/*
 * Copyright (c) 2019-2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <LibC/bits/pthread_integration.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/internals.h>
#include <sys/mman.h>
#include <unistd.h>

extern "C" {

struct AtExitEntry {
    AtExitFunction method { nullptr };
    void* parameter { nullptr };
    void* dso_handle { nullptr };
    bool has_been_called { false };
};

// We'll re-allocate the region if it ends up being too small at runtime
static size_t atexit_entry_region_size = 2 * PAGE_SIZE;

static AtExitEntry* atexit_entries;
static size_t atexit_entry_count = 0;
static pthread_mutex_t atexit_mutex = __PTHREAD_MUTEX_INITIALIZER;

static void lock_atexit_handlers()
{
    if (mprotect(atexit_entries, atexit_entry_region_size, PROT_READ) < 0) {
        perror("lock_atexit_handlers");
        _exit(1);
    }
}

static void unlock_atexit_handlers()
{
    if (mprotect(atexit_entries, atexit_entry_region_size, PROT_READ | PROT_WRITE) < 0) {
        perror("unlock_atexit_handlers");
        _exit(1);
    }
}

int __cxa_atexit(AtExitFunction exit_function, void* parameter, void* dso_handle)
{
    __pthread_mutex_lock(&atexit_mutex);

    // allocate initial atexit region
    if (!atexit_entries) {
        atexit_entries = (AtExitEntry*)mmap(nullptr, atexit_entry_region_size, PROT_READ, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
        if (atexit_entries == MAP_FAILED) {
            __pthread_mutex_unlock(&atexit_mutex);
            perror("__cxa_atexit mmap");
            _exit(1);
        }
    }

    // reallocate atexit region, increasing size by PAGE_SIZE
    if ((atexit_entry_count) >= (atexit_entry_region_size / sizeof(AtExitEntry))) {
        if (Checked<size_t>::addition_would_overflow(atexit_entry_region_size, PAGE_SIZE)) {
            __pthread_mutex_unlock(&atexit_mutex);
            return -1;
        }
        dbgln_if(GLOBAL_DTORS_DEBUG, "__cxa_atexit: Growing exit handler region from {} to {}", atexit_entry_region_size, atexit_entry_region_size + PAGE_SIZE);
        size_t new_atexit_region_size = atexit_entry_region_size + PAGE_SIZE;

        auto* new_atexit_entries = (AtExitEntry*)mmap(nullptr, new_atexit_region_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
        if (new_atexit_entries == MAP_FAILED) {
            __pthread_mutex_unlock(&atexit_mutex);
            perror("__cxa_atexit mmap (new size)");
            return -1;
        }
        memcpy(new_atexit_entries, atexit_entries, atexit_entry_region_size);
        if (munmap(atexit_entries, atexit_entry_region_size) < 0) {
            perror("__cxa_atexit munmap old region");
            // leak the old region on failure
        }
        atexit_entries = new_atexit_entries;
        atexit_entry_region_size = new_atexit_region_size;
    }

    unlock_atexit_handlers();
    atexit_entries[atexit_entry_count++] = { exit_function, parameter, dso_handle, false };
    lock_atexit_handlers();

    __pthread_mutex_unlock(&atexit_mutex);

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

    __pthread_mutex_lock(&atexit_mutex);

    ssize_t entry_index = atexit_entry_count;

    dbgln_if(GLOBAL_DTORS_DEBUG, "__cxa_finalize: {} entries in the finalizer list", entry_index);

    while (--entry_index >= 0) {
        auto& exit_entry = atexit_entries[entry_index];
        bool needs_calling = !exit_entry.has_been_called && (!dso_handle || dso_handle == exit_entry.dso_handle);
        if (needs_calling) {
            dbgln_if(GLOBAL_DTORS_DEBUG, "__cxa_finalize: calling entry[{}] {:p}({:p}) dso: {:p}", entry_index, exit_entry.method, exit_entry.parameter, exit_entry.dso_handle);
            unlock_atexit_handlers();
            exit_entry.has_been_called = true;
            lock_atexit_handlers();
            __pthread_mutex_unlock(&atexit_mutex);
            exit_entry.method(exit_entry.parameter);
            __pthread_mutex_lock(&atexit_mutex);
        }
    }

    __pthread_mutex_unlock(&atexit_mutex);
}

__attribute__((noreturn)) void __cxa_pure_virtual()
{
    VERIFY_NOT_REACHED();
}

} // extern "C"
