/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoopImplementation.h>
#include <LibCore/ThreadEventQueue.h>

namespace Core {

EventLoopImplementation::EventLoopImplementation()
    : m_thread_event_queue(ThreadEventQueue::current())
{
}

EventLoopImplementation::~EventLoopImplementation() = default;

void EventLoopImplementation::post_event(Object& receiver, NonnullOwnPtr<Event>&& event)
{
    m_thread_event_queue.post_event(receiver, move(event));

    // Wake up this EventLoopImplementation if this is a cross-thread event posting.
    if (&ThreadEventQueue::current() != &m_thread_event_queue)
        wake();
}

}
