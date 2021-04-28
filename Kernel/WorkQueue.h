/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <Kernel/Forward.h>

namespace Kernel {

extern WorkQueue* g_io_work;

class WorkQueue {
    AK_MAKE_NONCOPYABLE(WorkQueue);
    AK_MAKE_NONMOVABLE(WorkQueue);

public:
    static void initialize();

    WorkQueue(const char*);

    void queue(void (*function)(void*), void* data = nullptr, void (*free_data)(void*) = nullptr)
    {
        auto* item = new WorkItem; // TODO: use a pool
        item->function = function;
        item->data = data;
        item->free_data = free_data;
        do_queue(item);
    }

    template<typename Function>
    void queue(Function function)
    {
        auto* item = new WorkItem; // TODO: use a pool
        item->function = [](void* f) {
            (*reinterpret_cast<Function*>(f))();
        };
        if constexpr (sizeof(Function) <= sizeof(item->inline_data)) {
            item->data = new (item->inline_data) Function(move(function));
            item->free_data = [](void* f) {
                reinterpret_cast<Function*>(f)->~Function();
            };

        } else {
            item->data = new Function(move(function));
            item->free_data = [](void* f) {
                delete reinterpret_cast<Function*>(f);
            };
        }
        do_queue(item);
    }

private:
    struct WorkItem {
        IntrusiveListNode<WorkItem> m_node;
        void (*function)(void*);
        void* data;
        void (*free_data)(void*);
        u8 inline_data[4 * sizeof(void*)];
    };

    void do_queue(WorkItem*);

    RefPtr<Thread> m_thread;
    WaitQueue m_wait_queue;
    IntrusiveList<WorkItem, RawPtr<WorkItem>, &WorkItem::m_node> m_items;
    SpinLock<u8> m_lock;
};

}
