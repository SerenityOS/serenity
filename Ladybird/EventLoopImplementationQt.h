/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <LibCore/EventLoopImplementation.h>
#include <QEventLoop>
#include <QSocketNotifier>
#include <QTimer>

namespace Ladybird {

class EventLoopImplementationQt final : public Core::EventLoopImplementation {
public:
    static NonnullOwnPtr<EventLoopImplementationQt> create() { return adopt_own(*new EventLoopImplementationQt); }

    virtual ~EventLoopImplementationQt() override;

    virtual int exec() override;
    virtual size_t pump(PumpMode) override;
    virtual void quit(int) override;
    virtual void wake() override;

    virtual void deferred_invoke(Function<void()>) override;

    virtual int register_timer(Core::Object&, int milliseconds, bool should_reload, Core::TimerShouldFireWhenNotVisible) override;
    virtual bool unregister_timer(int timer_id) override;

    virtual void register_notifier(Core::Notifier&) override;
    virtual void unregister_notifier(Core::Notifier&) override;

    virtual void did_post_event() override;

    // FIXME: These APIs only exist for obscure use-cases inside SerenityOS. Try to get rid of them.
    virtual void unquit() override { }
    virtual bool was_exit_requested() const override { return false; }
    virtual void notify_forked_and_in_child() override { }
    virtual int register_signal(int, Function<void(int)>) override { return 0; }
    virtual void unregister_signal(int) override { }

    void set_main_loop() { m_main_loop = true; }

private:
    EventLoopImplementationQt();
    bool is_main_loop() const { return m_main_loop; }

    QEventLoop m_event_loop;
    QTimer m_process_core_events_timer;
    bool m_main_loop { false };
};

}
