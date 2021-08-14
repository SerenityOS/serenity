/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

#define CLOCKS_PER_SEC 1000

struct timespec {
    time_t tv_sec;
    long tv_nsec;
};

typedef int clockid_t;

enum {
    CLOCK_REALTIME,
#define CLOCK_REALTIME CLOCK_REALTIME
    CLOCK_MONOTONIC,
#define CLOCK_MONOTONIC CLOCK_MONOTONIC
    CLOCK_MONOTONIC_RAW,
#define CLOCK_MONOTONIC_RAW CLOCK_MONOTONIC_RAW
    CLOCK_REALTIME_COARSE,
#define CLOCK_REALTIME_COARSE CLOCK_REALTIME_COARSE
    CLOCK_MONOTONIC_COARSE,
#define CLOCK_MONOTONIC_COARSE CLOCK_MONOTONIC_COARSE
    CLOCK_ID_COUNT,
};

#define TIMER_ABSTIME 99

#ifdef __cplusplus
}
#endif
