/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Sections.h>
#include <Kernel/Tasks/FinalizerTask.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

static constexpr StringView finalizer_task_name = "Finalizer Task"sv;

static void finalizer_task(void*)
{
    Thread::current()->set_priority(THREAD_PRIORITY_LOW);
    while (!Process::current().is_dying()) {
        MUST(g_finalizer_wait_queue->wait_until(g_finalizer_has_work, [](bool& has_work) -> bool {
            if (!has_work)
                return false;
            has_work = false;
            return true;
        }));
        Thread::finalize_dying_threads();
    }
    Process::current().sys$exit(0);
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT void FinalizerTask::spawn()
{
    auto [_, finalizer_thread] = MUST(Process::create_kernel_process(finalizer_task_name, finalizer_task, nullptr));
    g_finalizer = move(finalizer_thread);
}

}
