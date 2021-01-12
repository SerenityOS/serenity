/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include <AK/LogStream.h>
#include <Kernel/API/Syscall.h>
#include <errno.h>
#include <sys/ptrace.h>

extern "C" {

int ptrace(int request, pid_t tid, void* addr, int data)
{

    // PT_PEEK needs special handling since the syscall wrapper
    // returns the peeked value as an int, which can be negative because of the cast.
    // When using PT_PEEK, the user can check if an error occurred
    // by looking at errno rather than the return value.

    u32 out_data;
    Syscall::SC_ptrace_peek_params peek_params;
    if (request == PT_PEEK) {
        peek_params.address = reinterpret_cast<u32*>(addr);
        peek_params.out_data = &out_data;
        addr = &peek_params;
    }

    Syscall::SC_ptrace_params params {
        request,
        tid,
        reinterpret_cast<u8*>(addr),
        data
    };
    int rc = syscall(SC_ptrace, &params);

    if (request == PT_PEEK) {
        if (rc < 0) {
            errno = -rc;
            return -1;
        }
        errno = 0;
        return static_cast<int>(out_data);
    }

    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
