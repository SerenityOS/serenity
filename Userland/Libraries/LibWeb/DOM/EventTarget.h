/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Noncopyable.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibWeb/DOM/DOMEventListener.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/EventHandler.h>

namespace Web::DOM {

class EventTarget {
    AK_MAKE_NONCOPYABLE(EventTarget);
    AK_MAKE_NONMOVABLE(EventTarget);

public:
    virtual ~EventTarget();

    void ref() { ref_event_target(); }
    void unref() { unref_event_target(); }

    virtual bool is_focusable() const { return false; }

    void add_event_listener(FlyString const& type, RefPtr<IDLEventListener> callback, Variant<AddEventListenerOptions, bool> const& options);
    void remove_event_listener(FlyString const& type, RefPtr<IDLEventListener> callback, Variant<EventListenerOptions, bool> const& options);

    // NOTE: These are for internal use only. They operate as though addEventListener(type, callback) was called instead of addEventListener(type, callback, options).
    void add_event_listener_without_options(FlyString const& type, RefPtr<IDLEventListener> callback);
    void remove_event_listener_without_options(FlyString const& type, RefPtr<IDLEventListener> callback);

    virtual bool dispatch_event(NonnullRefPtr<Event>);
    ExceptionOr<bool> dispatch_event_binding(NonnullRefPtr<Event>);

    virtual JS::Object* create_wrapper(JS::GlobalObject&) = 0;

    virtual EventTarget* get_parent(const Event&) { return nullptr; }

    void add_an_event_listener(NonnullRefPtr<DOMEventListener>);
    void remove_an_event_listener(DOMEventListener&);
    void remove_from_event_listener_list(DOMEventListener&);

    auto& event_listener_list() { return m_event_listener_list; }
    auto const& event_listener_list() const { return m_event_listener_list; }

    Function<void(const Event&)> activation_behavior;

    // NOTE: These only exist for checkbox and radio input elements.
    virtual void legacy_pre_activation_behavior() { }
    virtual void legacy_cancelled_activation_behavior() { }

    Bindings::CallbackType* event_handler_attribute(FlyString const& name);
    void set_event_handler_attribute(FlyString const& name, Optional<Bindings::CallbackType>);

    // https://dom.spec.whatwg.org/#eventtarget-activation-behavior
    virtual void run_activation_behavior() { }

protected:
    EventTarget();

    virtual void ref_event_target() = 0;
    virtual void unref_event_target() = 0;

    void element_event_handler_attribute_changed(FlyString const& local_name, String const& value);

private:
    Vector<NonnullRefPtr<DOMEventListener>> m_event_listener_list;

    // https://html.spec.whatwg.org/multipage/webappapis.html#event-handler-map
    // Spec Note: The order of the entries of event handler map could be arbitrary. It is not observable through any algorithms that operate on the map.
    HashMap<FlyString, HTML::EventHandler> m_event_handler_map;

    enum class IsAttribute {
        No,
        Yes,
    };

    Bindings::CallbackType* get_current_value_of_event_handler(FlyString const& name);
    void activate_event_handler(FlyString const& name, HTML::EventHandler& event_handler, IsAttribute is_attribute);
    void deactivate_event_handler(FlyString const& name);
    JS::ThrowCompletionOr<void> process_event_handler_for_event(FlyString const& name, Event& event);
};

}
