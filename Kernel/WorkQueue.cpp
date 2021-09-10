/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/WaitQueue.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

WorkQueue* g_io_work;

UNMAP_AFTER_INIT void WorkQueue::initialize()
{
    g_io_work = new (nothrow) WorkQueue("IO WorkQueue");
}

UNMAP_AFTER_INIT WorkQueue::WorkQueue(StringView name)
{
    RefPtr<Thread> thread;
    auto name_kstring = KString::try_create(name);
    if (name_kstring.is_error())
        TODO();
    Process::create_kernel_process(thread, name_kstring.release_value(), [this] {
        for (;;) {
            WorkItem* item;
            bool have_more;
            {
                SpinlockLocker lock(m_lock);
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

void WorkQueue::do_queue(WorkItem* item)
{
    {
        SpinlockLocker lock(m_lock);
        m_items.append(*item);
    }
    m_wait_queue.wake_one();
}

}
