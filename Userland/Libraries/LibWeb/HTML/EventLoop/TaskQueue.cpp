/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/EventLoop/TaskQueue.h>

namespace Web::HTML {

TaskQueue::TaskQueue(HTML::EventLoop& event_loop)
    : m_event_loop(event_loop)
{
}

TaskQueue::~TaskQueue()
{
}

void TaskQueue::add(NonnullOwnPtr<Task> task)
{
    m_tasks.append(move(task));
    m_event_loop.schedule();
}

OwnPtr<Task> TaskQueue::take_first_runnable()
{
    for (size_t i = 0; i < m_tasks.size(); ++i) {
        if (m_tasks[i]->is_runnable())
            return m_tasks.take(i);
    }
    return nullptr;
}

bool TaskQueue::has_runnable_tasks() const
{
    for (auto& task : m_tasks) {
        if (task->is_runnable())
            return true;
    }
    return false;
}

}
