/*
 * Copyright (c) 2019-2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Bitmap.h>
#include <AK/Checked.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/NeverDestroyed.h>
#include <LibRuntime/Mutex.h>
#include <LibRuntime/Serenity/PossiblyThrowingCallback.h>
#include <LibRuntime/System.h>
#include <mallocdefs.h>
#include <sys/internals.h>

namespace {
struct AtExitEntry {
    AtExitFunction method { nullptr };
    void* parameter { nullptr };
    void* dso_handle { nullptr };
};

// We'll re-allocate the region if it ends up being too small at runtime.
// Invariant: atexit_entry_region_capacity * sizeof(AtExitEntry) does not overflow.
size_t atexit_entry_region_capacity = PAGE_SIZE / sizeof(AtExitEntry);

size_t atexit_region_bytes(size_t capacity = atexit_entry_region_capacity)
{
    return PAGE_ROUND_UP(capacity * sizeof(AtExitEntry));
}

size_t atexit_next_capacity()
{
    size_t original_num_bytes = atexit_region_bytes();
    VERIFY(!Checked<size_t>::addition_would_overflow(original_num_bytes, PAGE_SIZE));
    return (original_num_bytes + PAGE_SIZE) / sizeof(AtExitEntry);
}

AtExitEntry* atexit_entries;
size_t atexit_entry_count = 0;
Runtime::Mutex atexit_mutex;

// The C++ compiler automagically registers the destructor of this object with __cxa_atexit.
// However, we can't control the order in which these destructors are run, so we might still want to access this data after the registered entry.
// Hence, we will call the destructor manually, when we know it is safe to do so.
NeverDestroyed<Bitmap> atexit_called_entries;

// During startup, it is sufficiently unlikely that the attacker can exploit any write primitive.
// We use this to avoid unnecessary syscalls to mprotect.
bool atexit_region_should_lock = false;

void lock_atexit_handlers()
{
    if (atexit_region_should_lock)
        MUST(Runtime::mprotect(atexit_entries, atexit_region_bytes(), Runtime::RegionAccess::Read));
}

void unlock_atexit_handlers()
{
    if (atexit_region_should_lock)
        MUST(Runtime::mprotect(atexit_entries, atexit_region_bytes(), Runtime::RegionAccess::ReadWrite));
}
}

extern "C" {

void __begin_atexit_locking()
{
    atexit_region_should_lock = true;
    lock_atexit_handlers();
}

int __cxa_atexit(AtExitFunction exit_function, void* parameter, void* dso_handle)
{
    Runtime::MutexLocker atexit_lock(atexit_mutex);

    // allocate initial atexit region
    if (!atexit_entries) {
        auto* region = MUST(Runtime::mmap(nullptr, atexit_region_bytes(), Runtime::RegionAccess::ReadWrite,
            Runtime::MMap::Private | Runtime::MMap::Anonymous, "atexit"sv, 0, 0, PAGE_SIZE));
        atexit_entries = reinterpret_cast<AtExitEntry*>(region);
    }

    // reallocate atexit region, increasing size by PAGE_SIZE
    if (atexit_entry_count >= atexit_entry_region_capacity) {
        size_t new_capacity = atexit_next_capacity();
        size_t new_atexit_region_size = atexit_region_bytes(new_capacity);
        dbgln_if(GLOBAL_DTORS_DEBUG, "__cxa_atexit: Growing exit handler region from {} entries to {} entries", atexit_entry_region_capacity, new_capacity);

        auto* region = MUST(Runtime::mmap(nullptr, new_atexit_region_size, Runtime::RegionAccess::ReadWrite,
            Runtime::MMap::Private | Runtime::MMap::Anonymous, "atexit"sv, 0, 0, PAGE_SIZE));
        auto* new_atexit_entries = reinterpret_cast<AtExitEntry*>(region);
        // Note: We must make sure to only copy initialized entries, as even touching uninitialized bytes will trigger UBSan.
        memcpy(new_atexit_entries, atexit_entries, atexit_entry_count * sizeof(AtExitEntry));
        if (auto unmap_result = Runtime::munmap(atexit_entries, atexit_region_bytes()); unmap_result.is_error()) {
            dbgln("__cxa_atexit failed to munmap old region: {}", unmap_result.release_error());
            // leak the old region on failure
        }
        atexit_entries = new_atexit_entries;
        atexit_entry_region_capacity = new_capacity;
    }

    unlock_atexit_handlers();
    atexit_entries[atexit_entry_count++] = { exit_function, parameter, dso_handle };
    lock_atexit_handlers();

    return 0;
}

void __cxa_finalize(void* dso_handle)
{
    // From the itanium abi, https://itanium-cxx-abi.github.io/cxx-abi/abi.html#dso-dtor-runtime-api
    //
    // When __cxa_finalize(d) is called, it should walk the termination function list, calling each in turn
    // if d matches __dso_handle for the termination function entry. If d == NULL, it should call all of them.
    // Multiple calls to __cxa_finalize shall not result in calling termination function entries multiple times;
    // the implementation may either remove entries or mark them finished.

    Runtime::MutexLocker atexit_lock(atexit_mutex);

    if (atexit_entry_count > atexit_called_entries->size())
        atexit_called_entries->grow(atexit_entry_count, false);

    ssize_t entry_index = atexit_entry_count;

    dbgln_if(GLOBAL_DTORS_DEBUG, "__cxa_finalize: {} entries in the finalizer list", entry_index);

    while (--entry_index >= 0) {
        auto& exit_entry = atexit_entries[entry_index];
        bool needs_calling = !atexit_called_entries->get(entry_index) && (!dso_handle || dso_handle == exit_entry.dso_handle);
        if (needs_calling) {
            dbgln_if(GLOBAL_DTORS_DEBUG, "__cxa_finalize: calling entry[{}] {:p}({:p}) dso: {:p}", entry_index, exit_entry.method, exit_entry.parameter, exit_entry.dso_handle);
            atexit_called_entries->set(entry_index, true);
            atexit_lock.unlock();
            Runtime::run_possibly_throwing_callback(exit_entry.method, exit_entry.parameter);
            atexit_lock.lock();
        }
    }
}

__attribute__((noreturn)) void __cxa_pure_virtual()
{
    VERIFY_NOT_REACHED();
}

} // extern "C"
