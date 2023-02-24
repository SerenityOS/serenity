/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

extern WorkQueue* g_io_work;
extern WorkQueue* g_ata_work;

class WorkQueue {
    AK_MAKE_NONCOPYABLE(WorkQueue);
    AK_MAKE_NONMOVABLE(WorkQueue);

public:
    static void initialize();

    ErrorOr<void> try_queue(void (*function)(void*), void* data = nullptr, void (*free_data)(void*) = nullptr)
    {
        auto item = new (nothrow) WorkItem; // TODO: use a pool
        if (!item)
            return Error::from_errno(ENOMEM);
        item->function = [function, data, free_data] {
            function(data);
            if (free_data)
                free_data(data);
        };
        do_queue(*item);
        return {};
    }

    template<typename Function>
    ErrorOr<void> try_queue(Function function)
    {
        auto item = new (nothrow) WorkItem; // TODO: use a pool
        if (!item)
            return Error::from_errno(ENOMEM);
        item->function = Function(function);
        do_queue(*item);
        return {};
    }

private:
    explicit WorkQueue(StringView);

    struct WorkItem {
    public:
        IntrusiveListNode<WorkItem> m_node;
        Function<void()> function;
    };

    void do_queue(WorkItem&);

    RefPtr<Thread> m_thread;
    WaitQueue m_wait_queue;
    SpinlockProtected<IntrusiveList<&WorkItem::m_node>, LockRank::None> m_items {};
};

}
