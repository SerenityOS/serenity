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
    enum class Type {
        // https://html.spec.whatwg.org/multipage/webappapis.html#window-event-loop
        Window,
        // https://html.spec.whatwg.org/multipage/webappapis.html#worker-event-loop
        Worker,
        // https://html.spec.whatwg.org/multipage/webappapis.html#worklet-event-loop
        Worklet,
    };

    EventLoop();
    ~EventLoop();

    Type type() const { return m_type; }

    TaskQueue& task_queue() { return m_task_queue; }
    TaskQueue const& task_queue() const { return m_task_queue; }

    void spin_until(Function<bool()> goal_condition);
    void process();

    Task const* currently_running_task() const { return m_currently_running_task; }

    JS::VM& vm() { return *m_vm; }
    JS::VM const& vm() const { return *m_vm; }

    void set_vm(JS::VM&);

private:
    Type m_type { Type::Window };

    TaskQueue m_task_queue;

    // https://html.spec.whatwg.org/multipage/webappapis.html#currently-running-task
    Task* m_currently_running_task { nullptr };

    JS::VM* m_vm { nullptr };
};

EventLoop& main_thread_event_loop();

}
