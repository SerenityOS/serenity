/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/WaitQueue.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

WorkQueue* g_io_work;

UNMAP_AFTER_INIT void WorkQueue::initialize()
{
    g_io_work = new WorkQueue("IO WorkQueue");
}

UNMAP_AFTER_INIT WorkQueue::WorkQueue(StringView name)
{
    RefPtr<Thread> thread;
    auto name_kstring = KString::try_create(name);
    if (name_kstring.is_error())
        TODO();
    (void)Process::create_kernel_process(thread, name_kstring.release_value(), [this] {
        for (;;) {
            WorkItem* item;
            bool have_more;
            m_items.with([&](auto& items) {
                item = items.take_first();
                have_more = !items.is_empty();
            });
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
    m_items.with([&](auto& items) {
        items.append(*item);
    });
    m_wait_queue.wake_one();
}

}
