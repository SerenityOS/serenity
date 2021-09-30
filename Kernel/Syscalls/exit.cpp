/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/KSyms.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

namespace Kernel {

void Process::sys$exit(int status)
{
    // FIXME: We have callers from kernel which don't acquire the big process lock.
    if (Thread::current()->previous_mode() == Thread::PreviousMode::UserMode) {
        VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    }

    {
        ProtectedDataMutationScope scope { *this };
        m_protected_values.termination_status = status;
        m_protected_values.termination_signal = 0;
    }

    auto* current_thread = Thread::current();
    current_thread->set_profiling_suppressed();
    PerformanceManager::add_thread_exit_event(*current_thread);

    die();
    current_thread->die_if_needed();
    VERIFY_NOT_REACHED();
}

}
