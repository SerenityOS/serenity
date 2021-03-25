/*
 * Copyright (c) 2021, The SerenityOS developers.
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
        IntrusiveListNode m_node;
        void (*function)(void*);
        void* data;
        void (*free_data)(void*);
        u8 inline_data[4 * sizeof(void*)];
    };

    void do_queue(WorkItem*);

    RefPtr<Thread> m_thread;
    WaitQueue m_wait_queue;
    IntrusiveList<WorkItem, &WorkItem::m_node> m_items;
    SpinLock<u8> m_lock;
};

}
