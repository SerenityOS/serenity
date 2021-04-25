/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/KSyms.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>

namespace Kernel {

void Process::sys$exit(int status)
{
    {
        ProtectedDataMutationScope scope { *this };
        m_termination_status = status;
        m_termination_signal = 0;
    }

    if (auto* event_buffer = current_perf_events_buffer()) {
        [[maybe_unused]] auto rc = event_buffer->append(PERF_EVENT_THREAD_EXIT, Thread::current()->tid().value(), 0, nullptr);
    }

    die();
    Thread::current()->die_if_needed();
    VERIFY_NOT_REACHED();
}

}
