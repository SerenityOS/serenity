/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#ifdef KERNEL
#    include <Kernel/UnixTypes.h>
#else
#    include <time.h>
#endif

namespace Kernel {

inline bool time_page_supports(clockid_t clock_id)
{
    return clock_id == CLOCK_REALTIME_COARSE || clock_id == CLOCK_MONOTONIC_COARSE;
}

struct TimePage {
    u32 volatile update1;
    struct timespec clocks[CLOCK_ID_COUNT];
    u32 volatile update2;
};

}
