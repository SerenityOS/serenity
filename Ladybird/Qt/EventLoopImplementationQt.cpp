/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EventLoopImplementationQt.h"
#include "EventLoopImplementationQtEventTarget.h"
#include <AK/IDAllocator.h>
#include <LibCore/Event.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/Notifier.h>
#include <LibCore/ThreadEventQueue.h>
#include <QCoreApplication>
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
    if (is_main_loop())
        return QCoreApplication::exec();
    return m_event_loop.exec();
}

size_t EventLoopImplementationQt::pump(PumpMode mode)
{
    auto result = Core::ThreadEventQueue::current().process();
    auto qt_mode = mode == PumpMode::WaitForEvents ? QEventLoop::WaitForMoreEvents : QEventLoop::AllEvents;
    if (is_main_loop())
        QCoreApplication::processEvents(qt_mode);
    else
        m_event_loop.processEvents(qt_mode);
    result += Core::ThreadEventQueue::current().process();
    return result;
}

void EventLoopImplementationQt::quit(int code)
{
    if (is_main_loop())
        QCoreApplication::exit(code);
    else
        m_event_loop.exit(code);
}

void EventLoopImplementationQt::wake()
{
    if (!is_main_loop())
        m_event_loop.wakeUp();
}

void EventLoopImplementationQt::post_event(Core::EventReceiver& receiver, NonnullOwnPtr<Core::Event>&& event)
{
    m_thread_event_queue.post_event(receiver, move(event));
    if (&m_thread_event_queue != &Core::ThreadEventQueue::current())
        wake();
}

static void qt_timer_fired(int timer_id, Core::TimerShouldFireWhenNotVisible should_fire_when_not_visible, Core::EventReceiver& object)
{
    if (should_fire_when_not_visible == Core::TimerShouldFireWhenNotVisible::No) {
        if (!object.is_visible_for_timer_purposes())
            return;
    }
    Core::TimerEvent event(timer_id);
    object.dispatch_event(event);
}

int EventLoopManagerQt::register_timer(Core::EventReceiver& object, int milliseconds, bool should_reload, Core::TimerShouldFireWhenNotVisible should_fire_when_not_visible)
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
        qt_timer_fired(timer_id, should_fire_when_not_visible, *object);
    });
    timer->start();
    thread_data.timers.set(timer_id, move(timer));
    return timer_id;
}

bool EventLoopManagerQt::unregister_timer(int timer_id)
{
    auto& thread_data = ThreadData::the();
    thread_data.timer_id_allocator.deallocate(timer_id);
    return thread_data.timers.remove(timer_id);
}

static void qt_notifier_activated(Core::Notifier& notifier)
{
    Core::NotifierActivationEvent event(notifier.fd());
    notifier.dispatch_event(event);
}

void EventLoopManagerQt::register_notifier(Core::Notifier& notifier)
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
    QObject::connect(socket_notifier, &QSocketNotifier::activated, [&notifier] {
        qt_notifier_activated(notifier);
    });

    ThreadData::the().notifiers.set(&notifier, move(socket_notifier));
}

void EventLoopManagerQt::unregister_notifier(Core::Notifier& notifier)
{
    ThreadData::the().notifiers.remove(&notifier);
}

void EventLoopManagerQt::did_post_event()
{
    QCoreApplication::postEvent(m_main_thread_event_target.ptr(), new QtEventLoopManagerEvent(QtEventLoopManagerEvent::process_event_queue_event_type()));
}

bool EventLoopManagerQt::event_target_received_event(Badge<EventLoopImplementationQtEventTarget>, QEvent* event)
{
    if (event->type() == QtEventLoopManagerEvent::process_event_queue_event_type()) {
        Core::ThreadEventQueue::current().process();
        return true;
    }
    return false;
}

EventLoopManagerQt::EventLoopManagerQt()
    : m_main_thread_event_target(make<EventLoopImplementationQtEventTarget>())
{
}

EventLoopManagerQt::~EventLoopManagerQt() = default;

NonnullOwnPtr<Core::EventLoopImplementation> EventLoopManagerQt::make_implementation()
{
    return adopt_own(*new EventLoopImplementationQt);
}

}
