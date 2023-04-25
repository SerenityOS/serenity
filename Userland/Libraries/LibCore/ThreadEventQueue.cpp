/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibCore/DeferredInvocationContext.h>
#include <LibCore/EventLoopImplementation.h>
#include <LibCore/Object.h>
#include <LibCore/Promise.h>
#include <LibCore/ThreadEventQueue.h>
#include <LibThreading/Mutex.h>

namespace Core {

struct ThreadEventQueue::Private {
    struct QueuedEvent {
        AK_MAKE_NONCOPYABLE(QueuedEvent);

    public:
        QueuedEvent(Object& receiver, NonnullOwnPtr<Event> event)
            : receiver(receiver)
            , event(move(event))
        {
        }

        QueuedEvent(QueuedEvent&& other)
            : receiver(other.receiver)
            , event(move(other.event))
        {
        }

        ~QueuedEvent() = default;

        WeakPtr<Object> receiver;
        NonnullOwnPtr<Event> event;
    };

    Threading::Mutex mutex;
    Vector<QueuedEvent, 128> queued_events;
    Vector<NonnullRefPtr<Promise<NonnullRefPtr<Object>>>, 16> pending_promises;
    bool warned_promise_count { false };
};

static thread_local ThreadEventQueue* s_current_thread_event_queue;

ThreadEventQueue& ThreadEventQueue::current()
{
    if (!s_current_thread_event_queue) {
        // FIXME: Don't leak these.
        s_current_thread_event_queue = new ThreadEventQueue;
    }
    return *s_current_thread_event_queue;
}

ThreadEventQueue::ThreadEventQueue()
    : m_private(make<Private>())
{
}

ThreadEventQueue::~ThreadEventQueue() = default;

void ThreadEventQueue::post_event(Core::Object& receiver, NonnullOwnPtr<Core::Event> event)
{
    {
        Threading::MutexLocker lock(m_private->mutex);
        m_private->queued_events.empend(receiver, move(event));
    }
    Core::EventLoopManager::the().did_post_event();
}

void ThreadEventQueue::add_job(NonnullRefPtr<Promise<NonnullRefPtr<Object>>> promise)
{
    Threading::MutexLocker lock(m_private->mutex);
    m_private->pending_promises.append(move(promise));
}

size_t ThreadEventQueue::process()
{
    decltype(m_private->queued_events) events;
    {
        Threading::MutexLocker locker(m_private->mutex);
        events = move(m_private->queued_events);
        m_private->pending_promises.remove_all_matching([](auto& job) { return job->is_resolved() || job->is_canceled(); });
    }

    size_t processed_events = 0;
    for (size_t i = 0; i < events.size(); ++i) {
        auto& queued_event = events.at(i);
        auto receiver = queued_event.receiver.strong_ref();
        auto& event = *queued_event.event;

        if (!receiver) {
            switch (event.type()) {
            case Event::Quit:
                VERIFY_NOT_REACHED();
            default:
                // Receiver disappeared, drop the event on the floor.
                break;
            }
        } else if (event.type() == Event::Type::DeferredInvoke) {
            static_cast<DeferredInvocationEvent&>(event).m_invokee();
        } else {
            NonnullRefPtr<Object> protector(*receiver);
            receiver->dispatch_event(event);
        }
        ++processed_events;
    }

    {
        Threading::MutexLocker locker(m_private->mutex);
        if (m_private->pending_promises.size() > 30 && !m_private->warned_promise_count) {
            m_private->warned_promise_count = true;
            dbgln("ThreadEventQueue::process: Job queue wasn't designed for this load ({} promises)", m_private->pending_promises.size());
        }
    }
    return processed_events;
}

bool ThreadEventQueue::has_pending_events() const
{
    Threading::MutexLocker locker(m_private->mutex);
    return !m_private->queued_events.is_empty();
}

}
