/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibWeb/HTML/EventLoop/TaskQueue.h>

namespace Web::HTML {

class EventLoop {
public:
    EventLoop();
    ~EventLoop();

    TaskQueue& task_queue() { return m_task_queue; }
    TaskQueue const& task_queue() const { return m_task_queue; }

    void spin_until(Function<bool()> goal_condition);

    Task const* currently_running_task() const { return m_currently_running_task; }

private:
    TaskQueue m_task_queue;

    // https://html.spec.whatwg.org/multipage/webappapis.html#currently-running-task
    Task* m_currently_running_task { nullptr };
};

EventLoop& main_thread_event_loop();

}
