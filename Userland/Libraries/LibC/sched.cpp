/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <sched.h>
#include <syscall.h>

extern "C" {

int sched_yield()
{
    int rc = syscall(SC_yield);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int sched_get_priority_min([[maybe_unused]] int policy)
{
    return 0; // Idle
}

int sched_get_priority_max([[maybe_unused]] int policy)
{
    return 3; // High
}

int sched_setparam(pid_t pid, const struct sched_param* param)
{
    int rc = syscall(SC_sched_setparam, pid, param);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int sched_getparam(pid_t pid, struct sched_param* param)
{
    int rc = syscall(SC_sched_getparam, pid, param);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
