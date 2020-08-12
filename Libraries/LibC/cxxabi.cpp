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

#include <AK/Types.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/internals.h>

//#define GLOBAL_DTORS_DEBUG

extern "C" {

struct __exit_entry {
    AtExitFunction method;
    void* parameter;
    void* dso_handle;
    bool has_been_called;
};

static __exit_entry __exit_entries[1024] {};
static int __exit_entry_count = 0;

int __cxa_atexit(AtExitFunction exit_function, void* parameter, void* dso_handle)
{
    if (__exit_entry_count >= 1024)
        return -1;

    __exit_entries[__exit_entry_count++] = { exit_function, parameter, dso_handle, false };

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

    int entry_index = __exit_entry_count;

#ifdef GLOBAL_DTORS_DEBUG
    dbgprintf("__cxa_finalize: %d entries in the finalizer list\n", entry_index);
#endif

    while (--entry_index >= 0) {
        auto& exit_entry = __exit_entries[entry_index];
        bool needs_calling = !exit_entry.has_been_called && (!dso_handle || dso_handle == exit_entry.dso_handle);
        if (needs_calling) {
#ifdef GLOBAL_DTORS_DEBUG
            dbgprintf("__cxa_finalize: calling entry[%d] %p(%p) dso: %p\n", entry_index, exit_entry.method, exit_entry.parameter, exit_entry.dso_handle);
#endif
            exit_entry.method(exit_entry.parameter);
            exit_entry.has_been_called = true;
        }
    }
}

[[noreturn]] void __cxa_pure_virtual()
{
    ASSERT_NOT_REACHED();
}

extern u32 __stack_chk_guard;
u32 __stack_chk_guard = (u32)0xc6c7c8c9;

[[noreturn]] void __stack_chk_fail()
{
    ASSERT_NOT_REACHED();
}
} // extern "C"
