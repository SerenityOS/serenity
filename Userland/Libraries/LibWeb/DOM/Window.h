/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/IDAllocator.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/CSS/Screen.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::DOM {

class Window final
    : public RefCounted<Window>
    , public EventTarget {
public:
    static NonnullRefPtr<Window> create_with_document(Document&);
    ~Window();

    using RefCounted::ref;
    using RefCounted::unref;

    virtual void ref_event_target() override { RefCounted::ref(); }
    virtual void unref_event_target() override { RefCounted::unref(); }
    virtual bool dispatch_event(NonnullRefPtr<Event>) override;
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    Page* page();
    Page const* page() const;

    const Document& document() const { return m_document; }
    Document& document() { return m_document; }

    void alert(const String&);
    bool confirm(const String&);
    String prompt(const String&, const String&);
    i32 request_animation_frame(JS::FunctionObject&);
    void cancel_animation_frame(i32);

    i32 set_timeout(JS::FunctionObject&, i32);
    i32 set_interval(JS::FunctionObject&, i32);
    void clear_timeout(i32);
    void clear_interval(i32);

    int inner_width() const;
    int inner_height() const;

    void did_set_location_href(Badge<Bindings::LocationObject>, const URL& new_href);
    void did_call_location_reload(Badge<Bindings::LocationObject>);

    Bindings::WindowObject* wrapper() { return m_wrapper; }
    const Bindings::WindowObject* wrapper() const { return m_wrapper; }

    void set_wrapper(Badge<Bindings::WindowObject>, Bindings::WindowObject&);

    i32 allocate_timer_id(Badge<Timer>);
    void deallocate_timer_id(Badge<Timer>, i32);
    void timer_did_fire(Badge<Timer>, Timer&);

    HighResolutionTime::Performance& performance() { return *m_performance; }

    CSS::Screen& screen() { return *m_screen; }

    const Event* current_event() const { return m_current_event; }
    void set_current_event(Event* event) { m_current_event = event; }

private:
    explicit Window(Document&);

    Document& m_document;
    WeakPtr<Bindings::WindowObject> m_wrapper;

    IDAllocator m_timer_id_allocator;
    HashMap<int, NonnullRefPtr<Timer>> m_timers;

    NonnullOwnPtr<HighResolutionTime::Performance> m_performance;
    NonnullRefPtr<CSS::Screen> m_screen;
    RefPtr<Event> m_current_event;
};

}
