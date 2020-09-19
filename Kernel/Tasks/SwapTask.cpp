/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Process.h>
#include <Kernel/Tasks/SwapTask.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

static SwapTask* g_swap_task;

void SwapTask::spawn()
{
    ASSERT(!g_swap_task);
    g_swap_task = new SwapTask;

    RefPtr<Thread> swap_thread;
    Process::create_kernel_process(swap_thread, "SwapTask", [] {
        Thread::current()->set_priority(THREAD_PRIORITY_HIGH);
        g_swap_task->run();
    });
}

void SwapTask::run()
{
    dbg() << "SwapTask is running!";
    for (;;) {
        Thread::current()->wait_on(m_wait_queue, "SwapTask");
        
        bool swap_out_threshold_met = m_swap_out_threshold_met.exchange(false, AK::MemoryOrder::memory_order_acq_rel);
        u32 work_in_areas = m_have_work_in_areas.exchange(0, AK::MemoryOrder::memory_order_acq_rel);
        if (!work_in_areas && !swap_out_threshold_met)
            continue;

        LOCKER(m_lock);
        if (swap_out_threshold_met) {
            dbg() << "SwapTask: Swap-out threshold was met";
            MM.try_swap_out_pages(false);

            // Re-check m_have_work_in_areas and add any areas we may have swapped out in
            work_in_areas |= m_have_work_in_areas.exchange(0, AK::MemoryOrder::memory_order_acq_rel);
        }

        if (work_in_areas != 0) {
            for (u32 area_index = 0; area_index < 32; area_index++) {
                dbg() << "SwapTask: Have work to do in swap area " << area_index;
                MM.write_out_pending_swap_pages(area_index, true);
            }
        }
    }
}

void SwapTask::notify_swap_out_threshold_met()
{
    ASSERT(g_swap_task);
    bool expected = false;
    if (g_swap_task->m_swap_out_threshold_met.compare_exchange_strong(expected, true, AK::MemoryOrder::memory_order_acq_rel))
        g_swap_task->m_wait_queue.wake_one();
}

void SwapTask::notify_pending_swap_out(u32 area_index)
{
    ASSERT(g_swap_task);
    ASSERT(area_index < sizeof(area_index) * 8);
    if (!g_swap_task->m_have_work_in_areas.fetch_or(1u << area_index, AK::MemoryOrder::memory_order_acq_rel))
        g_swap_task->m_wait_queue.wake_one();
}

}
