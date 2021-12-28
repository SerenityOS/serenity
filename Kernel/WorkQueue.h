/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

extern WorkQueue* g_io_work;

class WorkQueue {
    AK_MAKE_NONCOPYABLE(WorkQueue);
    AK_MAKE_NONMOVABLE(WorkQueue);

public:
    static void initialize();

    void queue(void (*function)(void*), void* data = nullptr, void (*free_data)(void*) = nullptr)
    {
        auto* item = new WorkItem; // TODO: use a pool
        item->function = [function, data, free_data] {
            function(data);
            if (free_data)
                free_data(data);
        };
        do_queue(item);
    }

    template<typename Function>
    void queue(Function function)
    {
        auto* item = new WorkItem; // TODO: use a pool
        item->function = Function(function);
        do_queue(item);
    }

private:
    explicit WorkQueue(StringView);

    struct WorkItem {
    public:
        IntrusiveListNode<WorkItem> m_node;
        Function<void()> function;
    };

    void do_queue(WorkItem*);

    RefPtr<Thread> m_thread;
    WaitQueue m_wait_queue;
    SpinlockProtected<IntrusiveList<&WorkItem::m_node>> m_items;
};

}
