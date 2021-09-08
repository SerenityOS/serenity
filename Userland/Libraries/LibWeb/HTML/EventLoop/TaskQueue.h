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
    TaskQueue();
    ~TaskQueue();

    bool is_empty() const { return m_tasks.is_empty(); }

    void add(NonnullOwnPtr<HTML::Task>);
    OwnPtr<HTML::Task> take_first_runnable() { return m_tasks.dequeue(); }

private:
    Queue<NonnullOwnPtr<HTML::Task>> m_tasks;
};

}
