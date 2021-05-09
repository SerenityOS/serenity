/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <sys/ptrace.h>
#include <syscall.h>

extern "C" {

int ptrace(int request, pid_t tid, void* addr, int data)
{
    // PT_PEEK needs special handling since the syscall wrapper
    // returns the peeked value as an int, which can be negative because of the cast.
    // When using PT_PEEK, the user can check if an error occurred
    // by looking at errno rather than the return value.

    u32 out_data;
    Syscall::SC_ptrace_peek_params peek_params;
    auto is_peek_type = request == PT_PEEK || request == PT_PEEKDEBUG;
    if (is_peek_type) {
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

    if (is_peek_type) {
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
