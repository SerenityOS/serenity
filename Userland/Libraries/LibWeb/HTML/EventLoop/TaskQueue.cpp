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

TaskQueue::~TaskQueue() = default;

void TaskQueue::add(NonnullOwnPtr<Task> task)
{
    m_tasks.append(move(task));
    m_event_loop.schedule();
}

OwnPtr<Task> TaskQueue::take_first_runnable()
{
    if (m_event_loop.execution_paused())
        return nullptr;

    for (size_t i = 0; i < m_tasks.size(); ++i) {
        if (m_tasks[i]->is_runnable())
            return m_tasks.take(i);
    }
    return nullptr;
}

bool TaskQueue::has_runnable_tasks() const
{
    if (m_event_loop.execution_paused())
        return false;

    for (auto& task : m_tasks) {
        if (task->is_runnable())
            return true;
    }
    return false;
}

void TaskQueue::remove_tasks_matching(Function<bool(HTML::Task const&)> filter)
{
    m_tasks.remove_all_matching([&](auto& task) {
        return filter(*task);
    });
}

ErrorOr<Vector<NonnullOwnPtr<Task>>> TaskQueue::take_tasks_matching(Function<bool(HTML::Task const&)> filter)
{
    Vector<NonnullOwnPtr<Task>> matching_tasks;

    for (size_t i = 0; i < m_tasks.size();) {
        auto& task = m_tasks.at(i);

        if (filter(*task)) {
            TRY(matching_tasks.try_append(move(task)));
            m_tasks.remove(i);
        } else {
            ++i;
        }
    }

    return matching_tasks;
}

Task const* TaskQueue::last_added_task() const
{
    if (m_tasks.is_empty())
        return nullptr;
    return m_tasks.last();
}

}
