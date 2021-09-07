/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/FinalizerTask.h>

namespace Kernel {

static void finalizer_task(void*)
{
    Thread::current()->set_priority(THREAD_PRIORITY_LOW);
    for (;;) {
        g_finalizer_wait_queue->wait_forever("FinalizerTask");

        if (g_finalizer_has_work.exchange(false, AK::MemoryOrder::memory_order_acq_rel) == true)
            Thread::finalize_dying_threads();
    }
};

UNMAP_AFTER_INIT void FinalizerTask::spawn()
{
    RefPtr<Thread> finalizer_thread;
    auto finalizer_process = Process::create_kernel_process(finalizer_thread, KString::must_create("FinalizerTask"), finalizer_task, nullptr);
    VERIFY(finalizer_process);
    g_finalizer = finalizer_thread;
}

}
