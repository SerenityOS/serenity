/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <bits/pthread_cancel.h>
#include <errno.h>
#include <poll.h>
#include <sys/time.h>
#include <syscall.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/poll.html
int poll(pollfd* fds, nfds_t nfds, int timeout_ms)
{
    __pthread_maybe_cancel();

    timespec timeout;
    timespec* timeout_ts = &timeout;
    if (timeout_ms < 0)
        timeout_ts = nullptr;
    else
        timeout = { timeout_ms / 1000, (timeout_ms % 1000) * 1'000'000 };
    return ppoll(fds, nfds, timeout_ts, nullptr);
}

int ppoll(pollfd* fds, nfds_t nfds, timespec const* timeout, sigset_t const* sigmask)
{
    Syscall::SC_poll_params params { fds, nfds, timeout, sigmask };
    int rc = syscall(SC_poll, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
