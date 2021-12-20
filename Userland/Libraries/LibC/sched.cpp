/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <sched.h>
#include <syscall.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sched_yield.html
int sched_yield()
{
    int rc = syscall(SC_yield);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sched_get_priority_min.html
int sched_get_priority_min([[maybe_unused]] int policy)
{
    return 0; // Idle
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sched_get_priority_max.html
int sched_get_priority_max([[maybe_unused]] int policy)
{
    return 3; // High
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sched_setparam.html
int sched_setparam(pid_t pid, const struct sched_param* param)
{
    int rc = syscall(SC_sched_setparam, pid, param);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sched_getparam.html
int sched_getparam(pid_t pid, struct sched_param* param)
{
    int rc = syscall(SC_sched_getparam, pid, param);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
