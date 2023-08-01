#pragma once

#include <AK/NonnullOwnPtr.h>
#include <LibCore/EventLoopImplementation.h>
#include <glib.h>

class EventLoopManagerGLib final : public Core::EventLoopManager {
public:
    virtual ~EventLoopManagerGLib() override;
    virtual NonnullOwnPtr<Core::EventLoopImplementation> make_implementation() override;

    virtual int register_timer(Core::Object&, int milliseconds, bool should_reload, Core::TimerShouldFireWhenNotVisible) override;
    virtual bool unregister_timer(int timer_id) override;

    virtual void register_notifier(Core::Notifier&) override;
    virtual void unregister_notifier(Core::Notifier&) override;

    virtual void did_post_event() override { }

    virtual int register_signal(int, Function<void(int)>) override
    {
        VERIFY_NOT_REACHED();
    }

    virtual void unregister_signal(int) override { }

private:
    // Some of these APIs should really be on EventLoopImplementation,
    // not EventLoopManager, welp.
    GMainContext* current_thread_context();
};

class EventLoopImplementationGLib final : public Core::EventLoopImplementation {
public:
    static NonnullOwnPtr<EventLoopImplementationGLib> create() { return adopt_own(*new EventLoopImplementationGLib); }

    virtual ~EventLoopImplementationGLib() override;

    virtual int exec() override;
    virtual size_t pump(PumpMode) override;
    virtual void quit(int) override;
    virtual void wake() override;
    virtual void post_event(Core::Object& receiver, NonnullOwnPtr<Core::Event>&&) override;

    virtual void unquit() override;
    virtual bool was_exit_requested() const override { return m_should_quit; }
    virtual void notify_forked_and_in_child() override { }

private:
    friend class EventLoopManagerGLib;
    EventLoopImplementationGLib();

    GMainContext* m_context { nullptr };
    GSource* m_thread_event_queue_source { nullptr };
    int m_exit_code { -1 };
    bool m_should_quit : 1 { false };
    bool m_owns_context : 1 { false };
};
