/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Queue.h>
#include <LibWeb/HTML/EventLoop/Task.h>

namespace Web::HTML {

class TaskQueue {
public:
    explicit TaskQueue(HTML::EventLoop&);
    ~TaskQueue();

    bool is_empty() const { return m_tasks.is_empty(); }

    void add(NonnullOwnPtr<HTML::Task>);
    OwnPtr<HTML::Task> take_first_runnable() { return m_tasks.dequeue(); }

private:
    HTML::EventLoop& m_event_loop;

    Queue<NonnullOwnPtr<HTML::Task>> m_tasks;
};

}
