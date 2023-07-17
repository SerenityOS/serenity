/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/WaitQueue.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel {

WorkQueue* g_io_work;
WorkQueue* g_ata_work;

UNMAP_AFTER_INIT void WorkQueue::initialize()
{
    g_io_work = new WorkQueue("IO WorkQueue Task"sv);
    g_ata_work = new WorkQueue("ATA WorkQueue Task"sv);
}

UNMAP_AFTER_INIT WorkQueue::WorkQueue(StringView name)
{
    auto [_, thread] = Process::create_kernel_process(name, [this] {
        while (!Process::current().is_dying()) {
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
        Process::current().sys$exit(0);
        VERIFY_NOT_REACHED();
    }).release_value_but_fixme_should_propagate_errors();
    m_thread = move(thread);
}

void WorkQueue::do_queue(WorkItem& item)
{
    m_items.with([&](auto& items) {
        items.append(item);
    });
    m_wait_queue.wake_one();
}

}
