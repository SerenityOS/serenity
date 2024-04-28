/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/IDAllocator.h>
#include <LibCore/Event.h>
#include <LibCore/Notifier.h>
#include <LibCore/ThreadEventQueue.h>

#import <Application/EventLoopImplementation.h>
#import <System/Cocoa.h>
#import <System/CoreFoundation.h>

namespace Ladybird {

struct ThreadData {
    static ThreadData& the()
    {
        static thread_local ThreadData s_thread_data;
        return s_thread_data;
    }

    Core::Notifier& notifier_by_fd(int fd)
    {
        for (auto notifier : notifiers) {
            if (notifier.key->fd() == fd)
                return *notifier.key;
        }

        // If we didn't have a notifier for the provided FD, it should have been unregistered.
        VERIFY_NOT_REACHED();
    }

    IDAllocator timer_id_allocator;
    HashMap<int, CFRunLoopTimerRef> timers;
    HashMap<Core::Notifier*, CFRunLoopSourceRef> notifiers;
};

static void post_application_event()
{
    auto* event = [NSEvent otherEventWithType:NSEventTypeApplicationDefined
                                     location:NSMakePoint(0, 0)
                                modifierFlags:0
                                    timestamp:0
                                 windowNumber:0
                                      context:nil
                                      subtype:0
                                        data1:0
                                        data2:0];

    [NSApp postEvent:event atStart:NO];
}

NonnullOwnPtr<Core::EventLoopImplementation> CFEventLoopManager::make_implementation()
{
    return CFEventLoopImplementation::create();
}

intptr_t CFEventLoopManager::register_timer(Core::EventReceiver& receiver, int interval_milliseconds, bool should_reload, Core::TimerShouldFireWhenNotVisible should_fire_when_not_visible)
{
    auto& thread_data = ThreadData::the();

    auto timer_id = thread_data.timer_id_allocator.allocate();
    auto weak_receiver = receiver.make_weak_ptr();

    auto interval_seconds = static_cast<double>(interval_milliseconds) / 1000.0;
    auto first_fire_time = CFAbsoluteTimeGetCurrent() + interval_seconds;

    auto* timer = CFRunLoopTimerCreateWithHandler(
        kCFAllocatorDefault, first_fire_time, should_reload ? interval_seconds : 0, 0, 0,
        ^(CFRunLoopTimerRef) {
            auto receiver = weak_receiver.strong_ref();
            if (!receiver) {
                return;
            }

            if (should_fire_when_not_visible == Core::TimerShouldFireWhenNotVisible::No) {
                if (!receiver->is_visible_for_timer_purposes()) {
                    return;
                }
            }

            Core::TimerEvent event;
            receiver->dispatch_event(event);
        });

    CFRunLoopAddTimer(CFRunLoopGetCurrent(), timer, kCFRunLoopDefaultMode);
    thread_data.timers.set(timer_id, timer);

    return timer_id;
}

void CFEventLoopManager::unregister_timer(intptr_t timer_id)
{
    auto& thread_data = ThreadData::the();
    thread_data.timer_id_allocator.deallocate(static_cast<int>(timer_id));

    auto timer = thread_data.timers.take(static_cast<int>(timer_id));
    VERIFY(timer.has_value());
    CFRunLoopTimerInvalidate(*timer);
    CFRelease(*timer);
}

static void socket_notifier(CFSocketRef socket, CFSocketCallBackType notification_type, CFDataRef, void const*, void*)
{
    auto& notifier = ThreadData::the().notifier_by_fd(CFSocketGetNative(socket));

    // This socket callback is not quite re-entrant. If Core::Notifier::dispatch_event blocks, e.g.
    // to wait upon a Core::Promise, this socket will not receive any more notifications until that
    // promise is resolved or rejected. So we mark this socket as able to receive more notifications
    // before dispatching the event, which allows it to be triggered again.
    CFSocketEnableCallBacks(socket, notification_type);

    Core::NotifierActivationEvent event(notifier.fd(), notifier.type());
    notifier.dispatch_event(event);

    // This manual process of enabling the callbacks also seems to require waking the event loop,
    // otherwise it hangs indefinitely in any ongoing pump(PumpMode::WaitForEvents) invocation.
    post_application_event();
}

void CFEventLoopManager::register_notifier(Core::Notifier& notifier)
{
    auto notification_type = kCFSocketNoCallBack;

    switch (notifier.type()) {
    case Core::Notifier::Type::Read:
        notification_type = kCFSocketReadCallBack;
        break;
    case Core::Notifier::Type::Write:
        notification_type = kCFSocketWriteCallBack;
        break;
    default:
        TODO();
        break;
    }

    CFSocketContext context { .version = 0, .info = nullptr, .retain = nullptr, .release = nullptr, .copyDescription = nullptr };
    auto* socket = CFSocketCreateWithNative(kCFAllocatorDefault, notifier.fd(), notification_type, &socket_notifier, &context);

    CFOptionFlags sockopt = CFSocketGetSocketFlags(socket);
    sockopt &= ~kCFSocketAutomaticallyReenableReadCallBack;
    sockopt &= ~kCFSocketCloseOnInvalidate;
    CFSocketSetSocketFlags(socket, sockopt);

    auto* source = CFSocketCreateRunLoopSource(kCFAllocatorDefault, socket, 0);
    CFRunLoopAddSource(CFRunLoopGetCurrent(), source, kCFRunLoopDefaultMode);

    CFRelease(socket);

    ThreadData::the().notifiers.set(&notifier, source);
}

void CFEventLoopManager::unregister_notifier(Core::Notifier& notifier)
{
    if (auto source = ThreadData::the().notifiers.take(&notifier); source.has_value()) {
        CFRunLoopRemoveSource(CFRunLoopGetCurrent(), *source, kCFRunLoopDefaultMode);
        CFRelease(*source);
    }
}

void CFEventLoopManager::did_post_event()
{
    post_application_event();
}

NonnullOwnPtr<CFEventLoopImplementation> CFEventLoopImplementation::create()
{
    return adopt_own(*new CFEventLoopImplementation);
}

int CFEventLoopImplementation::exec()
{
    [NSApp run];
    return m_exit_code;
}

size_t CFEventLoopImplementation::pump(PumpMode mode)
{
    auto* wait_until = mode == PumpMode::WaitForEvents ? [NSDate distantFuture] : [NSDate distantPast];

    auto* event = [NSApp nextEventMatchingMask:NSEventMaskAny
                                     untilDate:wait_until
                                        inMode:NSDefaultRunLoopMode
                                       dequeue:YES];

    while (event) {
        [NSApp sendEvent:event];

        event = [NSApp nextEventMatchingMask:NSEventMaskAny
                                   untilDate:nil
                                      inMode:NSDefaultRunLoopMode
                                     dequeue:YES];
    }

    return 0;
}

void CFEventLoopImplementation::quit(int exit_code)
{
    m_exit_code = exit_code;
    [NSApp stop:nil];
}

void CFEventLoopImplementation::wake()
{
    CFRunLoopWakeUp(CFRunLoopGetCurrent());
}

void CFEventLoopImplementation::post_event(Core::EventReceiver& receiver, NonnullOwnPtr<Core::Event>&& event)
{
    m_thread_event_queue.post_event(receiver, move(event));

    if (&m_thread_event_queue != &Core::ThreadEventQueue::current())
        wake();
}

}
