/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/WeakPtr.h>
#include <LibCore/EventLoopImplementation.h>
#include <jni.h>

extern "C" struct ALooper;

namespace Ladybird {

class ALooperEventLoopManager : public Core::EventLoopManager {
public:
    ALooperEventLoopManager(JavaVM*, jobject timer_service);
    virtual ~ALooperEventLoopManager() override;
    virtual NonnullOwnPtr<Core::EventLoopImplementation> make_implementation() override;

    virtual int register_timer(Core::EventReceiver&, int milliseconds, bool should_reload, Core::TimerShouldFireWhenNotVisible) override;
    virtual bool unregister_timer(int timer_id) override;

    virtual void register_notifier(Core::Notifier&) override;
    virtual void unregister_notifier(Core::Notifier&) override;

    virtual void did_post_event() override;

    // FIXME: These APIs only exist for obscure use-cases inside SerenityOS. Try to get rid of them.
    virtual int register_signal(int, Function<void(int)>) override { return 0; }
    virtual void unregister_signal(int) override { }

private:
    JavaVM* m_vm { nullptr };
    jobject m_timer_service { nullptr };
    jmethodID m_register_timer { nullptr };
    jmethodID m_unregister_timer { nullptr };
    jclass m_timer_class { nullptr };
    jmethodID m_timer_constructor { nullptr };
};

struct TimerData {
    WeakPtr<Core::EventReceiver> receiver;
    Core::TimerShouldFireWhenNotVisible visibility;
};

struct EventLoopThreadData {
    static EventLoopThreadData& the();

    HashMap<long, TimerData> timers;
    HashTable<Core::Notifier*> notifiers;
    Core::ThreadEventQueue* thread_queue = nullptr;
};

class ALooperEventLoopImplementation : public Core::EventLoopImplementation {
public:
    static NonnullOwnPtr<ALooperEventLoopImplementation> create() { return adopt_own(*new ALooperEventLoopImplementation); }

    virtual ~ALooperEventLoopImplementation() override;

    virtual int exec() override;
    virtual size_t pump(PumpMode) override;
    virtual void quit(int) override;
    virtual void wake() override;
    virtual void post_event(Core::EventReceiver& receiver, NonnullOwnPtr<Core::Event>&&) override;

    // FIXME: These APIs only exist for obscure use-cases inside SerenityOS. Try to get rid of them.
    virtual void unquit() override { }
    virtual bool was_exit_requested() const override { return false; }
    virtual void notify_forked_and_in_child() override { }

    void poke();

    EventLoopThreadData& thread_data();

private:
    friend class ALooperEventLoopManager;

    ALooperEventLoopImplementation();

    static int looper_callback(int fd, int events, void* data);

    void register_notifier(Core::Notifier&);
    void unregister_notifier(Core::Notifier&);

    ALooper* m_event_loop { nullptr };
    int m_pipe[2] {};
    int m_exit_code { 0 };
    Atomic<bool> m_exit_requested { false };
    EventLoopThreadData* m_thread_data { nullptr };
};

}
