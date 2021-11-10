/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::DOM {

struct EventInit {
    bool bubbles { false };
    bool cancelable { false };
    bool composed { false };
};

class Event
    : public RefCounted<Event>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::EventWrapper;

    enum Phase : u16 {
        None = 0,
        CapturingPhase = 1,
        AtTarget = 2,
        BubblingPhase = 3,
    };

    using TouchTargetList = Vector<RefPtr<EventTarget>>;

    struct PathEntry {
        RefPtr<EventTarget> invocation_target;
        bool invocation_target_in_shadow_tree { false };
        RefPtr<EventTarget> shadow_adjusted_target;
        RefPtr<EventTarget> related_target;
        TouchTargetList touch_target_list;
        bool root_of_closed_tree { false };
        bool slot_in_closed_tree { false };
        size_t index;
    };

    using Path = Vector<PathEntry>;

    static NonnullRefPtr<Event> create(const FlyString& event_name, EventInit const& event_init = {})
    {
        return adopt_ref(*new Event(event_name, event_init));
    }
    static NonnullRefPtr<Event> create_with_global_object(Bindings::WindowObject&, const FlyString& event_name, EventInit const& event_init)
    {
        return Event::create(event_name, event_init);
    }

    virtual ~Event() { }

    double time_stamp() const;

    const FlyString& type() const { return m_type; }
    void set_type(StringView type) { m_type = type; }

    RefPtr<EventTarget> target() const { return m_target; }
    void set_target(EventTarget* target) { m_target = target; }

    // NOTE: This is intended for the JS bindings.
    RefPtr<EventTarget> src_target() const { return target(); }

    RefPtr<EventTarget> related_target() const { return m_related_target; }
    void set_related_target(EventTarget* related_target) { m_related_target = related_target; }

    bool should_stop_propagation() const { return m_stop_propagation; }
    void set_stop_propagation(bool stop_propagation) { m_stop_propagation = stop_propagation; }

    bool should_stop_immediate_propagation() const { return m_stop_immediate_propagation; }
    void set_stop_immediate_propagation(bool stop_immediate_propagation) { m_stop_immediate_propagation = stop_immediate_propagation; }

    bool cancelled() const { return m_cancelled; }
    void set_cancelled(bool cancelled) { m_cancelled = cancelled; }

    bool in_passive_listener() const { return m_in_passive_listener; }
    void set_in_passive_listener(bool in_passive_listener) { m_in_passive_listener = in_passive_listener; }

    bool composed() const { return m_composed; }
    void set_composed(bool composed) { m_composed = composed; }

    bool initialized() const { return m_initialized; }
    void set_initialized(bool initialized) { m_initialized = initialized; }

    bool dispatched() const { return m_dispatch; }
    void set_dispatched(bool dispatched) { m_dispatch = dispatched; }

    void prevent_default() { set_cancelled_flag(); }
    bool default_prevented() const { return cancelled(); }

    u16 event_phase() const { return m_phase; }
    void set_phase(Phase phase) { m_phase = phase; }

    RefPtr<EventTarget> current_target() const { return m_current_target; }
    void set_current_target(EventTarget* current_target) { m_current_target = current_target; }

    bool return_value() const { return !m_cancelled; }
    void set_return_value(bool return_value)
    {
        if (!return_value)
            set_cancelled_flag();
    }

    void append_to_path(EventTarget&, RefPtr<EventTarget>, RefPtr<EventTarget>, TouchTargetList&, bool);
    Path& path() { return m_path; }
    const Path& path() const { return m_path; }
    void clear_path() { m_path.clear(); }

    void set_touch_target_list(TouchTargetList& touch_target_list) { m_touch_target_list = touch_target_list; }
    TouchTargetList& touch_target_list() { return m_touch_target_list; };
    void clear_touch_target_list() { m_touch_target_list.clear(); }

    bool bubbles() const { return m_bubbles; }
    void set_bubbles(bool bubbles) { m_bubbles = bubbles; }

    bool cancelable() const { return m_cancelable; }
    void set_cancelable(bool cancelable) { m_cancelable = cancelable; }

    bool is_trusted() const { return m_is_trusted; }
    void set_is_trusted(bool is_trusted) { m_is_trusted = is_trusted; }

    void stop_propagation() { m_stop_propagation = true; }

    bool cancel_bubble() const { return m_stop_propagation; }
    void set_cancel_bubble(bool cancel_bubble)
    {
        if (cancel_bubble)
            m_stop_propagation = true;
    }

    void stop_immediate_propagation()
    {
        m_stop_propagation = true;
        m_stop_immediate_propagation = true;
    }

    void init_event(const String&, bool, bool);

    void set_time_stamp(double time_stamp) { m_time_stamp = time_stamp; }

    NonnullRefPtrVector<EventTarget> composed_path() const;

protected:
    explicit Event(FlyString const& type)
        : m_type(type)
        , m_initialized(true)
    {
    }
    Event(FlyString const& type, EventInit const& event_init)
        : m_type(type)
        , m_bubbles(event_init.bubbles)
        , m_cancelable(event_init.cancelable)
        , m_composed(event_init.composed)
        , m_initialized(true)
    {
    }

    void initialize(const String&, bool, bool);

private:
    FlyString m_type;
    RefPtr<EventTarget> m_target;
    RefPtr<EventTarget> m_related_target;
    RefPtr<EventTarget> m_current_target;

    Phase m_phase { None };

    bool m_bubbles { false };
    bool m_cancelable { false };

    bool m_stop_propagation { false };
    bool m_stop_immediate_propagation { false };
    bool m_cancelled { false };
    bool m_in_passive_listener { false };
    bool m_composed { false };
    bool m_initialized { false };
    bool m_dispatch { false };

    bool m_is_trusted { true };

    Path m_path;
    TouchTargetList m_touch_target_list;

    double m_time_stamp { 0 };

    void set_cancelled_flag();
};

}
