/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

class EventTarget {
    AK_MAKE_NONCOPYABLE(EventTarget);
    AK_MAKE_NONMOVABLE(EventTarget);

public:
    virtual ~EventTarget();

    void ref() { ref_event_target(); }
    void unref() { unref_event_target(); }

    void add_event_listener(FlyString const& event_name, RefPtr<EventListener>);
    void remove_event_listener(FlyString const& event_name, RefPtr<EventListener>);

    void remove_from_event_listener_list(NonnullRefPtr<EventListener>);

    virtual bool dispatch_event(NonnullRefPtr<Event>) = 0;
    ExceptionOr<bool> dispatch_event_binding(NonnullRefPtr<Event>);

    virtual JS::Object* create_wrapper(JS::GlobalObject&) = 0;
    Bindings::ScriptExecutionContext* script_execution_context() { return m_script_execution_context; }

    virtual EventTarget* get_parent(Event const&) { return nullptr; }

    struct EventListenerRegistration {
        FlyString event_name;
        NonnullRefPtr<EventListener> listener;
    };

    Vector<EventListenerRegistration>& listeners() { return m_listeners; }
    const Vector<EventListenerRegistration>& listeners() const { return m_listeners; }

    Function<void(Event const&)> activation_behaviour;

    // NOTE: These only exist for checkbox and radio input elements.
    Function<void()> legacy_pre_activation_behaviour;
    Function<void()> legacy_cancelled_activation_behaviour;

protected:
    explicit EventTarget(Bindings::ScriptExecutionContext&);

    virtual void ref_event_target() = 0;
    virtual void unref_event_target() = 0;

private:
    // FIXME: This should not be a raw pointer.
    Bindings::ScriptExecutionContext* m_script_execution_context { nullptr };

    Vector<EventListenerRegistration> m_listeners;
};

}
