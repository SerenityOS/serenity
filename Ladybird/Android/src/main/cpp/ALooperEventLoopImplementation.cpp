/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ALooperEventLoopImplementation.h"
#include "JNIHelpers.h"
#include <LibCore/EventLoop.h>
#include <LibCore/Notifier.h>
#include <LibCore/ThreadEventQueue.h>
#include <android/log.h>
#include <android/looper.h>
#include <fcntl.h>
#include <jni.h>

namespace Ladybird {

EventLoopThreadData& EventLoopThreadData::the()
{
    static thread_local EventLoopThreadData s_thread_data { {}, {}, &Core::ThreadEventQueue::current() };
    return s_thread_data;
}

static ALooperEventLoopImplementation& current_impl()
{
    return verify_cast<ALooperEventLoopImplementation>(Core::EventLoop::current().impl());
}

ALooperEventLoopManager::ALooperEventLoopManager(JavaVM* vm, jobject timer_service)
    : m_vm(vm)
    , m_timer_service(timer_service)
{
    JavaEnvironment env(m_vm);

    jclass timer_class = env.get()->FindClass("org/serenityos/ladybird/TimerExecutorService$Timer");
    if (!timer_class)
        TODO();
    m_timer_class = reinterpret_cast<jclass>(env.get()->NewGlobalRef(timer_class));
    env.get()->DeleteLocalRef(timer_class);

    m_timer_constructor = env.get()->GetMethodID(m_timer_class, "<init>", "(J)V");
    if (!m_timer_constructor)
        TODO();

    jclass timer_service_class = env.get()->GetObjectClass(m_timer_service);

    m_register_timer = env.get()->GetMethodID(timer_service_class, "registerTimer", "(Lorg/serenityos/ladybird/TimerExecutorService$Timer;ZJ)J");
    if (!m_register_timer)
        TODO();

    m_unregister_timer = env.get()->GetMethodID(timer_service_class, "unregisterTimer", "(J)Z");
    if (!m_unregister_timer)
        TODO();
    env.get()->DeleteLocalRef(timer_service_class);
}

ALooperEventLoopManager::~ALooperEventLoopManager()
{
    JavaEnvironment env(m_vm);

    env.get()->DeleteGlobalRef(m_timer_service);
    env.get()->DeleteGlobalRef(m_timer_class);
}

NonnullOwnPtr<Core::EventLoopImplementation> ALooperEventLoopManager::make_implementation()
{
    return ALooperEventLoopImplementation::create();
}

int ALooperEventLoopManager::register_timer(Core::EventReceiver& receiver, int milliseconds, bool should_reload, Core::TimerShouldFireWhenNotVisible visibility)
{
    JavaEnvironment env(m_vm);
    auto& thread_data = EventLoopThreadData::the();

    auto timer = env.get()->NewObject(m_timer_class, m_timer_constructor, reinterpret_cast<long>(&current_impl()));

    long millis = milliseconds;
    long timer_id = env.get()->CallLongMethod(m_timer_service, m_register_timer, timer, !should_reload, millis);

    // FIXME: Is there a race condition here? Maybe we should take a lock on the timers...
    thread_data.timers.set(timer_id, { receiver.make_weak_ptr(), visibility });

    return timer_id;
}

bool ALooperEventLoopManager::unregister_timer(int timer_id)
{
    if (auto timer = EventLoopThreadData::the().timers.take(timer_id); timer.has_value()) {
        JavaEnvironment env(m_vm);
        return env.get()->CallBooleanMethod(m_timer_service, m_unregister_timer, timer_id);
    }
    return false;
}

void ALooperEventLoopManager::register_notifier(Core::Notifier& notifier)
{
    EventLoopThreadData::the().notifiers.set(&notifier);
    current_impl().register_notifier(notifier);
}

void ALooperEventLoopManager::unregister_notifier(Core::Notifier& notifier)
{
    EventLoopThreadData::the().notifiers.remove(&notifier);
    current_impl().unregister_notifier(notifier);
}

void ALooperEventLoopManager::did_post_event()
{
    current_impl().poke();
}

ALooperEventLoopImplementation::ALooperEventLoopImplementation()
    : m_event_loop(ALooper_prepare(0))
    , m_thread_data(&EventLoopThreadData::the())
{
    auto ret = pipe2(m_pipe, O_CLOEXEC | O_NONBLOCK);
    VERIFY(ret == 0);

    ALooper_acquire(m_event_loop);

    ret = ALooper_addFd(m_event_loop, m_pipe[0], ALOOPER_POLL_CALLBACK, ALOOPER_EVENT_INPUT, &ALooperEventLoopImplementation::looper_callback, this);
    VERIFY(ret == 1);
}

ALooperEventLoopImplementation::~ALooperEventLoopImplementation()
{
    ALooper_removeFd(m_event_loop, m_pipe[0]);
    ALooper_release(m_event_loop);

    ::close(m_pipe[0]);
    ::close(m_pipe[1]);
}

EventLoopThreadData& ALooperEventLoopImplementation::thread_data()
{
    return *m_thread_data;
}

int ALooperEventLoopImplementation::exec()
{
    while (!m_exit_requested.load(MemoryOrder::memory_order_acquire))
        pump(PumpMode::WaitForEvents);
    return m_exit_code;
}

size_t ALooperEventLoopImplementation::pump(Core::EventLoopImplementation::PumpMode mode)
{
    auto num_events = Core::ThreadEventQueue::current().process();

    int timeout_ms = mode == Core::EventLoopImplementation::PumpMode::WaitForEvents ? -1 : 0;
    auto ret = ALooper_pollAll(timeout_ms, nullptr, nullptr, nullptr);

    // We don't expect any non-callback FDs to be ready
    VERIFY(ret <= 0);

    if (ret == ALOOPER_POLL_ERROR)
        m_exit_requested.store(true, MemoryOrder::memory_order_release);

    num_events += Core::ThreadEventQueue::current().process();
    return num_events;
}

void ALooperEventLoopImplementation::quit(int code)
{
    m_exit_code = code;
    m_exit_requested.store(true, MemoryOrder::memory_order_release);
    wake();
}

void ALooperEventLoopImplementation::wake()
{
    ALooper_wake(m_event_loop);
}

void ALooperEventLoopImplementation::post_event(Core::EventReceiver& receiver, NonnullOwnPtr<Core::Event>&& event)
{
    m_thread_event_queue.post_event(receiver, move(event));

    if (&m_thread_event_queue != &Core::ThreadEventQueue::current())
        wake();
}

int ALooperEventLoopImplementation::looper_callback(int fd, int events, void* data)
{
    auto& impl = *static_cast<ALooperEventLoopImplementation*>(data);
    (void)impl; // FIXME: Do we need to do anything with the instance here?

    if (events & ALOOPER_EVENT_INPUT) {
        int msg = 0;
        while (read(fd, &msg, sizeof(msg)) == sizeof(msg)) {
            // Do nothing, we don't actually care what the message was, just that it was posted
        }
    }
    return 1;
}

void ALooperEventLoopImplementation::poke()
{
    int msg = 0xCAFEBABE;
    (void)write(m_pipe[1], &msg, sizeof(msg));
}

static int notifier_callback(int fd, int, void* data)
{
    auto& notifier = *static_cast<Core::Notifier*>(data);

    VERIFY(fd == notifier.fd());

    Core::NotifierActivationEvent event(notifier.fd());
    notifier.dispatch_event(event);

    // Wake up from ALooper_pollAll, and service this event on the event queue
    current_impl().poke();

    return 1;
}

void ALooperEventLoopImplementation::register_notifier(Core::Notifier& notifier)
{
    auto event_flags = 0;
    switch (notifier.type()) {
    case Core::Notifier::Type::Read:
        event_flags = ALOOPER_EVENT_INPUT;
        break;
    case Core::Notifier::Type::Write:
        event_flags = ALOOPER_EVENT_OUTPUT;
        break;
    case Core::Notifier::Type::Exceptional:
    case Core::Notifier::Type::None:
        TODO();
    }

    auto ret = ALooper_addFd(m_event_loop, notifier.fd(), ALOOPER_POLL_CALLBACK, event_flags, &notifier_callback, &notifier);
    VERIFY(ret == 1);
}

void ALooperEventLoopImplementation::unregister_notifier(Core::Notifier& notifier)
{
    ALooper_removeFd(m_event_loop, notifier.fd());
}

}
