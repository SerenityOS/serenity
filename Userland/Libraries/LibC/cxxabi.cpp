/*
 * Copyright (c) 2019-2020, Andrew Kaster <andrewdkaster@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Debug.h>
#include <AK/LogStream.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/internals.h>
#include <sys/mman.h>

extern "C" {

struct AtExitEntry {
    AtExitFunction method { nullptr };
    void* parameter { nullptr };
    void* dso_handle { nullptr };
    bool has_been_called { false };
};

static constexpr size_t max_atexit_entry_count = PAGE_SIZE / sizeof(AtExitEntry);

static AtExitEntry* atexit_entries;
static size_t atexit_entry_count = 0;

static void lock_atexit_handlers()
{
    if (mprotect(atexit_entries, PAGE_SIZE, PROT_READ) < 0) {
        perror("lock_atexit_handlers");
        _exit(1);
    }
}

static void unlock_atexit_handlers()
{
    if (mprotect(atexit_entries, PAGE_SIZE, PROT_READ | PROT_WRITE) < 0) {
        perror("unlock_atexit_handlers");
        _exit(1);
    }
}

int __cxa_atexit(AtExitFunction exit_function, void* parameter, void* dso_handle)
{
    if (atexit_entry_count >= max_atexit_entry_count)
        return -1;

    if (!atexit_entries) {
        atexit_entries = (AtExitEntry*)mmap(nullptr, PAGE_SIZE, PROT_READ, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
        if (atexit_entries == MAP_FAILED) {
            perror("__cxa_atexit mmap");
            _exit(1);
        }
    }

    unlock_atexit_handlers();
    atexit_entries[atexit_entry_count++] = { exit_function, parameter, dso_handle, false };
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

    ssize_t entry_index = atexit_entry_count;

    dbgln_if(GLOBAL_DTORS_DEBUG, "__cxa_finalize: {} entries in the finalizer list", entry_index);

    while (--entry_index >= 0) {
        auto& exit_entry = atexit_entries[entry_index];
        bool needs_calling = !exit_entry.has_been_called && (!dso_handle || dso_handle == exit_entry.dso_handle);
        if (needs_calling) {
            dbgln_if(GLOBAL_DTORS_DEBUG, "__cxa_finalize: calling entry[{}] {:p}({:p}) dso: {:p}", entry_index, exit_entry.method, exit_entry.parameter, exit_entry.dso_handle);
            exit_entry.method(exit_entry.parameter);
            unlock_atexit_handlers();
            exit_entry.has_been_called = true;
            lock_atexit_handlers();
        }
    }
}

[[noreturn]] void __cxa_pure_virtual()
{
    ASSERT_NOT_REACHED();
}

} // extern "C"
