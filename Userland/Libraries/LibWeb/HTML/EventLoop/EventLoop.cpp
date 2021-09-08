/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>

namespace Web::HTML {

EventLoop::EventLoop()
{
}

EventLoop::~EventLoop()
{
}

EventLoop& main_thread_event_loop()
{
    return static_cast<Bindings::WebEngineCustomData*>(Bindings::main_thread_vm().custom_data())->event_loop;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#spin-the-event-loop
void EventLoop::spin_until([[maybe_unused]] Function<bool()> goal_condition)
{
    // FIXME: 1. Let task be the event loop's currently running task.

    // FIXME: 2. Let task source be task's source.

    // FIXME: 3. Let old stack be a copy of the JavaScript execution context stack.

    // FIXME: 4. Empty the JavaScript execution context stack.

    // FIXME: 5. Perform a microtask checkpoint.

    // FIXME: 6. In parallel:

    // FIXME:    1. Wait until the condition goal is met.

    // FIXME:    2. Queue a task on task source to:

    // FIXME:       1. Replace the JavaScript execution context stack with old stack.

    // FIXME:       2. Perform any steps that appear after this spin the event loop instance in the original algorithm.

    // FIXME: 7. Stop task, allowing whatever algorithm that invoked it to resume.
}

}
