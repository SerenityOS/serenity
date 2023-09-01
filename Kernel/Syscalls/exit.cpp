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
    auto* current_thread = Thread::current();
    current_thread->set_profiling_suppressed();
    PerformanceManager::add_thread_exit_event(*current_thread);

    die(status, {});
    current_thread->die_if_needed();
    VERIFY_NOT_REACHED();
}

}
