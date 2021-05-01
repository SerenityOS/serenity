/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$perf_event(int type, FlatPtr arg1, FlatPtr arg2)
{
    if (!create_perf_events_buffer_if_needed())
        return ENOMEM;
    return perf_events()->append(type, arg1, arg2, nullptr);
}

}
