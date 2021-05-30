/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/KSyms.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>

namespace Kernel {

void Process::sys$exit(int status)
{
    {
        ProtectedDataMutationScope scope { *this };
        m_termination_status = status;
        m_termination_signal = 0;
    }

    auto* current_thread = Thread::current();
    current_thread->set_profiling_suppressed();
    PerformanceManager::add_thread_exit_event(*current_thread);

    die();
    current_thread->die_if_needed();
    VERIFY_NOT_REACHED();
}

}
