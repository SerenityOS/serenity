/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <stdio.h>
#include <sys/select.h>
#include <sys/time.h>
#include <syscall.h>

extern "C" {

int select(int nfds, fd_set* readfds, fd_set* writefds, fd_set* exceptfds, timeval* timeout_tv)
{
    timespec* timeout_ts = nullptr;
    timespec timeout;
    if (timeout_tv) {
        timeout_ts = &timeout;
        TIMEVAL_TO_TIMESPEC(timeout_tv, timeout_ts);
    }
    return pselect(nfds, readfds, writefds, exceptfds, timeout_ts, nullptr);
}

int pselect(int nfds, fd_set* readfds, fd_set* writefds, fd_set* exceptfds, timespec const* timeout, sigset_t const* sigmask)
{
    Syscall::SC_select_params params { nfds, readfds, writefds, exceptfds, timeout, sigmask };
    int rc = syscall(SC_select, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
