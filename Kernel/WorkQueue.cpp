/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/SpinLock.h>
#include <Kernel/WaitQueue.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

WorkQueue::WorkQueue(String&& name)
{
    RefPtr<Thread> thread;
    Process::create_kernel_process(thread, move(name), [this] {
        while (!m_destroying) {
            WorkItem* item;
            bool have_more;
            {
                ScopedSpinLock lock(m_lock);
                item = m_items.take_first();
                have_more = !m_items.is_empty();
            }
            if (item) {
                item->function();
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

WorkQueue::~WorkQueue()
{
    {
        ScopedSpinLock lock(m_lock);
        m_destroying = true;
    }
    m_wait_queue.wake_one();
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
