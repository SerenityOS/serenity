/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/poll.h>
#include <signal.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

int poll(struct pollfd* fds, nfds_t nfds, int timeout);
int ppoll(struct pollfd* fds, nfds_t nfds, const struct timespec* timeout, sigset_t const* sigmask);

__END_DECLS
