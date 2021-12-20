/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <poll.h>
#include <sys/time.h>
#include <syscall.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/poll.html
int poll(pollfd* fds, nfds_t nfds, int timeout_ms)
{
    timespec timeout;
    timespec* timeout_ts = &timeout;
    if (timeout_ms < 0)
        timeout_ts = nullptr;
    else
        timeout = { timeout_ms / 1000, (timeout_ms % 1000) * 1'000'000 };
    return ppoll(fds, nfds, timeout_ts, nullptr);
}

int ppoll(pollfd* fds, nfds_t nfds, const timespec* timeout, const sigset_t* sigmask)
{
    Syscall::SC_poll_params params { fds, nfds, timeout, sigmask };
    int rc = syscall(SC_poll, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
