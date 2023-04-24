/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EventLoopImplementationQt.h"
#include <AK/IDAllocator.h>
#include <LibCore/Event.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>
#include <LibCore/ThreadEventQueue.h>
#include <QTimer>

namespace Ladybird {

struct ThreadData;
static thread_local ThreadData* s_thread_data;

struct ThreadData {
    static ThreadData& the()
    {
        if (!s_thread_data) {
            // FIXME: Don't leak this.
            s_thread_data = new ThreadData;
        }
        return *s_thread_data;
    }

    IDAllocator timer_id_allocator;
    HashMap<int, NonnullOwnPtr<QTimer>> timers;
    HashMap<Core::Notifier*, NonnullOwnPtr<QSocketNotifier>> notifiers;
};

EventLoopImplementationQt::EventLoopImplementationQt()
{
}

EventLoopImplementationQt::~EventLoopImplementationQt() = default;

int EventLoopImplementationQt::exec()
{
    // NOTE: We don't use QEventLoop::exec() here since it wouldn't process the Core::ThreadEventQueue.
    while (!m_exit_code.has_value()) {
        pump(PumpMode::WaitForEvents);
    }
    return m_exit_code.value();
}

size_t EventLoopImplementationQt::pump(PumpMode mode)
{
    bool result = Core::ThreadEventQueue::current().process() != 0;
    if (mode == PumpMode::WaitForEvents)
        result |= m_event_loop.processEvents(QEventLoop::WaitForMoreEvents);
    else
        result |= m_event_loop.processEvents();
    Core::ThreadEventQueue::current().process();
    return result;
}

void EventLoopImplementationQt::quit(int code)
{
    m_exit_code = code;
}

void EventLoopImplementationQt::wake()
{
    m_event_loop.wakeUp();
}

void EventLoopImplementationQt::deferred_invoke(Function<void()> function)
{
    VERIFY(function);
    QTimer::singleShot(0, [function = move(function)] {
        function();
    });
}

int EventLoopImplementationQt::register_timer(Core::Object& object, int milliseconds, bool should_reload, Core::TimerShouldFireWhenNotVisible should_fire_when_not_visible)
{
    auto& thread_data = ThreadData::the();
    auto timer = make<QTimer>();
    timer->setInterval(milliseconds);
    timer->setSingleShot(!should_reload);
    auto timer_id = thread_data.timer_id_allocator.allocate();
    auto weak_object = object.make_weak_ptr();
    QObject::connect(timer, &QTimer::timeout, [timer_id, should_fire_when_not_visible, weak_object = move(weak_object)] {
        auto object = weak_object.strong_ref();
        if (!object)
            return;
        if (should_fire_when_not_visible == Core::TimerShouldFireWhenNotVisible::No) {
            if (!object->is_visible_for_timer_purposes())
                return;
        }
        Core::ThreadEventQueue::current().post_event(*object, make<Core::TimerEvent>(timer_id));
    });
    timer->start();
    thread_data.timers.set(timer_id, move(timer));
    return timer_id;
}

bool EventLoopImplementationQt::unregister_timer(int timer_id)
{
    auto& thread_data = ThreadData::the();
    thread_data.timer_id_allocator.deallocate(timer_id);
    return thread_data.timers.remove(timer_id);
}

void EventLoopImplementationQt::register_notifier(Core::Notifier& notifier)
{
    QSocketNotifier::Type type;
    switch (notifier.type()) {
    case Core::Notifier::Type::Read:
        type = QSocketNotifier::Read;
        break;
    case Core::Notifier::Type::Write:
        type = QSocketNotifier::Write;
        break;
    default:
        TODO();
    }
    auto socket_notifier = make<QSocketNotifier>(notifier.fd(), type);
    QObject::connect(socket_notifier, &QSocketNotifier::activated, [fd = notifier.fd(), &notifier] {
        Core::ThreadEventQueue::current().post_event(notifier, make<Core::NotifierActivationEvent>(fd));
    });

    ThreadData::the().notifiers.set(&notifier, move(socket_notifier));
}

void EventLoopImplementationQt::unregister_notifier(Core::Notifier& notifier)
{
    ThreadData::the().notifiers.remove(&notifier);
}

}
