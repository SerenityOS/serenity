/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Queue.h>
#include <LibWeb/HTML/EventLoop/Task.h>

namespace Web::HTML {

class TaskQueue {
public:
    explicit TaskQueue(HTML::EventLoop&);
    ~TaskQueue();

    bool is_empty() const { return m_tasks.is_empty(); }

    bool has_runnable_tasks() const;

    void add(NonnullOwnPtr<HTML::Task>);
    OwnPtr<HTML::Task> take_first_runnable();

    void enqueue(NonnullOwnPtr<HTML::Task> task) { add(move(task)); }
    OwnPtr<HTML::Task> dequeue()
    {
        if (m_tasks.is_empty())
            return {};
        return m_tasks.take_first();
    }

private:
    HTML::EventLoop& m_event_loop;

    Vector<NonnullOwnPtr<HTML::Task>> m_tasks;
};

}
