/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>
#include <Kernel/SpinLock.h>
#include <Kernel/WaitQueue.h>
#include <Kernel/WorkQueue.h>
#include <Kernel/TimerQueue.h>

#define WORKQUEUE_DEBUG_LONG_FUNCTIONS 1

namespace Kernel {

WorkQueue* g_io_work;

void WorkQueue::initialize()
{
    g_io_work = new WorkQueue("IO WorkQueue");
}

WorkQueue::WorkQueue(const char* name)
    : m_name(name)
{
    RefPtr<Thread> thread;
    Process::create_kernel_process(thread, name, [this] {
        for (;;) {
            WorkItem* item;
            bool have_more;
            {
                ScopedSpinLock lock(m_lock);
                item = m_items.take_first();
                have_more = !m_items.is_empty();
            }
            if (item) {
#if WORKQUEUE_DEBUG_LONG_FUNCTIONS
                auto deadline = TimeManagement::the().monotonic_time();
                deadline += Time::from_seconds(2);
                auto timer = TimerQueue::the().add_timer_without_id(CLOCK_MONOTONIC_COARSE, deadline, [this]() {
                    dbgln("WorkQueue[{}] function has taken more than 2 seconds!", m_name);
                    dbgln("{}", m_thread->backtrace());
                });
#endif
                item->function(item->data);
#if WORKQUEUE_DEBUG_LONG_FUNCTIONS
                TimerQueue::the().cancel_timer(timer.release_nonnull());
#endif
                if (item->free_data)
                    item->free_data(item->data);
                delete item;

                if (have_more)
                    continue;
            }
            [[maybe_unused]] auto result = m_wait_queue.wait_on({});
        }
    });
    // If we can't create the thread we're in trouble...
    m_thread = thread.release_nonnull();
}

void WorkQueue::do_queue(WorkItem* item)
{
    {
        ScopedSpinLock lock(m_lock);
        m_items.append(*item);
    }
    m_wait_queue.wake_one();
}

}
