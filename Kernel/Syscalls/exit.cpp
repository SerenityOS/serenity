/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/KSyms.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

void Process::sys$exit(int status)
{
    // FIXME: We have callers from kernel which don't acquire the big process lock.
    if (Thread::current()->previous_mode() == ExecutionMode::User) {
        VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    }

    with_mutable_protected_data([status](auto& protected_data) {
        protected_data.termination_status = status;
        protected_data.termination_signal = 0;
    });

    auto* current_thread = Thread::current();
    current_thread->set_profiling_suppressed();
    PerformanceManager::add_thread_exit_event(*current_thread);

    die();
    current_thread->die_if_needed();
    VERIFY_NOT_REACHED();
}

}
