/*
 * Copyright (c) 2023, Stephan Vedder <stephan.vedder@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#define COLLECT_RUNTIME_STATS 0

namespace JS::JIT {
struct RuntimeStats {
    u64 native_calls { 0 };

    static FlatPtr native_calls_offset() { return OFFSET_OF(RuntimeStats, native_calls); }
};

#if COLLECT_RUNTIME_STATS
extern RuntimeStats s_runtime_stats;
#endif
}
