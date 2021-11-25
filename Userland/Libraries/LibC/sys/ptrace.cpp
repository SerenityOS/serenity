/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <sys/ptrace.h>
#include <syscall.h>

extern "C" {

long ptrace(int request, pid_t tid, void* addr, void* data)
{
    if (request == PT_PEEKBUF) {
        // PT_PEEKBUF cannot easily be correctly used through this function signature:
        // The amount of data to be copied is not available.
        // We could VERIFY() here, but to safeguard against ports that attempt to use
        // the same number, let's claim that the Kernel just doesn't know the command.
        // Use Core::System::ptrace_peekbuf instead.
        return EINVAL;
    }

    // PT_PEEK needs special handling since the syscall wrapper
    // returns the peeked value as an int, which can be negative because of the cast.
    // When using PT_PEEK, the user can check if an error occurred
    // by looking at errno rather than the return value.

    FlatPtr out_data;
    auto is_peek_type = request == PT_PEEK || request == PT_PEEKDEBUG;
    if (is_peek_type) {
        data = &out_data;
    }

    Syscall::SC_ptrace_params params {
        request,
        tid,
        addr,
        (FlatPtr)data
    };
    long rc = syscall(SC_ptrace, &params);

    if (is_peek_type) {
        if (rc < 0) {
            errno = -rc;
            return -1;
        }
        errno = 0;
        return static_cast<long>(out_data);
    }

    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
