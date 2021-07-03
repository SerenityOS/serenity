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
    auto events_buffer = current_perf_events_buffer();
    if (!events_buffer) {
        if (!create_perf_events_buffer_if_needed())
            return ENOMEM;
        events_buffer = perf_events();
    }
    return events_buffer->append(type, arg1, arg2, nullptr);
}

}
