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

private:
    TaskQueue m_task_queue;
};

EventLoop& main_thread_event_loop();

}
