/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

int sched_yield(void);

struct sched_param {
    int sched_priority;
};

#define SCHED_FIFO 0
#define SCHED_RR 1
#define SCHED_OTHER 2
#define SCHED_BATCH 3

int sched_get_priority_min(int policy);
int sched_get_priority_max(int policy);
int sched_setparam(pid_t pid, const struct sched_param* param);
int sched_getparam(pid_t pid, struct sched_param* param);

__END_DECLS
