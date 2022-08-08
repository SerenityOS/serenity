/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/IDAllocator.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/CSS/MediaQueryList.h>
#include <LibWeb/CSS/Screen.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/HTML/AnimationFrameCallbackDriver.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/GlobalEventHandlers.h>
#include <LibWeb/HTML/WindowEventHandlers.h>

namespace Web::HTML {

class IdleCallback;

class Window final
    : public RefCounted<Window>
    , public Weakable<Window>
    , public DOM::EventTarget
    , public HTML::GlobalEventHandlers
    , public HTML::WindowEventHandlers {
public:
    static NonnullRefPtr<Window> create();
    static NonnullRefPtr<Window> create_with_document(DOM::Document&);
    ~Window();

    using RefCounted::ref;
    using RefCounted::unref;

    virtual void ref_event_target() override { RefCounted::ref(); }
    virtual void unref_event_target() override { RefCounted::unref(); }
    virtual bool dispatch_event(DOM::Event&) override;
    virtual JS::Object* create_wrapper(JS::Realm&) override;

    Page* page();
    Page const* page() const;

    // https://html.spec.whatwg.org/multipage/window-object.html#concept-document-window
    DOM::Document const& associated_document() const { return *m_associated_document; }
    DOM::Document& associated_document() { return *m_associated_document; }
    void set_associated_document(DOM::Document&);

    // https://html.spec.whatwg.org/multipage/window-object.html#window-bc
    HTML::BrowsingContext const* browsing_context() const { return m_associated_document->browsing_context(); }
    HTML::BrowsingContext* browsing_context() { return m_associated_document->browsing_context(); }

    void alert(String const&);
    bool confirm(String const&);
    String prompt(String const&, String const&);
    i32 request_animation_frame(Bindings::CallbackType& js_callback);
    void cancel_animation_frame(i32);
    bool has_animation_frame_callbacks() const { return m_animation_frame_callback_driver.has_callbacks(); }

    i32 set_timeout(Bindings::TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments);
    i32 set_interval(Bindings::TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments);
    void clear_timeout(i32);
    void clear_interval(i32);

    void queue_microtask(Bindings::CallbackType& callback);

    int inner_width() const;
    int inner_height() const;

    void did_set_location_href(Badge<Bindings::LocationObject>, AK::URL const& new_href);
    void did_call_location_reload(Badge<Bindings::LocationObject>);
    void did_call_location_replace(Badge<Bindings::LocationObject>, String url);

    Bindings::WindowObject* wrapper() { return m_wrapper; }
    Bindings::WindowObject const* wrapper() const { return m_wrapper; }

    void set_wrapper(Badge<Bindings::WindowObject>, Bindings::WindowObject&);

    void deallocate_timer_id(Badge<Timer>, i32);

    HighResolutionTime::Performance& performance() { return *m_performance; }

    Crypto::Crypto& crypto() { return *m_crypto; }

    CSS::Screen& screen() { return *m_screen; }

    DOM::Event* current_event() { return m_current_event.cell(); }
    DOM::Event const* current_event() const { return m_current_event.cell(); }
    void set_current_event(DOM::Event* event);

    CSS::CSSStyleDeclaration* get_computed_style(DOM::Element&) const;
    NonnullRefPtr<CSS::MediaQueryList> match_media(String);
    Optional<CSS::MediaFeatureValue> query_media_feature(CSS::MediaFeatureID) const;

    float scroll_x() const;
    float scroll_y() const;

    void fire_a_page_transition_event(FlyString const& event_name, bool persisted);

    float device_pixel_ratio() const;

    int screen_x() const;
    int screen_y() const;

    Selection::Selection* get_selection();

    RefPtr<HTML::Storage> local_storage();
    RefPtr<HTML::Storage> session_storage();

    Window* parent();

    DOM::ExceptionOr<void> post_message(JS::Value, String const& target_origin);

    String name() const;
    void set_name(String const&);

    void start_an_idle_period();

    u32 request_idle_callback(Bindings::CallbackType& callback);
    void cancel_idle_callback(u32);

    AnimationFrameCallbackDriver& animation_frame_callback_driver() { return m_animation_frame_callback_driver; }

private:
    Window();
    explicit Window(DOM::Document&);

    // ^HTML::GlobalEventHandlers
    virtual DOM::EventTarget& global_event_handlers_to_event_target(FlyString const&) override { return *this; }

    // ^HTML::WindowEventHandlers
    virtual DOM::EventTarget& window_event_handlers_to_event_target() override { return *this; }

    enum class Repeat {
        Yes,
        No,
    };
    i32 run_timer_initialization_steps(Bindings::TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments, Repeat repeat, Optional<i32> previous_id = {});

    void invoke_idle_callbacks();

    // https://html.spec.whatwg.org/multipage/window-object.html#concept-document-window
    WeakPtr<DOM::Document> m_associated_document;

    WeakPtr<Bindings::WindowObject> m_wrapper;

    IDAllocator m_timer_id_allocator;
    HashMap<int, NonnullRefPtr<Timer>> m_timers;

    NonnullOwnPtr<HighResolutionTime::Performance> m_performance;
    NonnullRefPtr<Crypto::Crypto> m_crypto;
    NonnullOwnPtr<CSS::Screen> m_screen;
    JS::Handle<DOM::Event> m_current_event;

    AnimationFrameCallbackDriver m_animation_frame_callback_driver;

    // https://w3c.github.io/requestidlecallback/#dfn-list-of-idle-request-callbacks
    NonnullRefPtrVector<IdleCallback> m_idle_request_callbacks;
    // https://w3c.github.io/requestidlecallback/#dfn-list-of-runnable-idle-callbacks
    NonnullRefPtrVector<IdleCallback> m_runnable_idle_callbacks;
    // https://w3c.github.io/requestidlecallback/#dfn-idle-callback-identifier
    u32 m_idle_callback_identifier = 0;
};

void run_animation_frame_callbacks(DOM::Document&, double now);

}
