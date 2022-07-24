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
    return THREAD_PRIORITY_MIN;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sched_get_priority_max.html
int sched_get_priority_max([[maybe_unused]] int policy)
{
    return THREAD_PRIORITY_MAX;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sched_setparam.html
int sched_setparam(pid_t pid, const struct sched_param* param)
{
    Syscall::SC_scheduler_parameters_params parameters {
        .pid_or_tid = pid,
        .mode = Syscall::SchedulerParametersMode::Process,
        .parameters = *param,
    };
    int rc = syscall(SC_scheduler_set_parameters, &parameters);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sched_getparam.html
int sched_getparam(pid_t pid, struct sched_param* param)
{
    Syscall::SC_scheduler_parameters_params parameters {
        .pid_or_tid = pid,
        .mode = Syscall::SchedulerParametersMode::Process,
        .parameters = {},
    };
    int rc = syscall(SC_scheduler_get_parameters, &parameters);
    if (rc == 0)
        *param = parameters.parameters;
    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
