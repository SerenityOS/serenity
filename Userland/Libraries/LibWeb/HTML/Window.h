/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/IDAllocator.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <AK/TypeCasts.h>
#include <AK/URL.h>
#include <LibJS/Heap/Heap.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/AnimationFrameCallbackDriver.h>
#include <LibWeb/HTML/CrossOrigin/CrossOriginPropertyDescriptorMap.h>
#include <LibWeb/HTML/GlobalEventHandlers.h>
#include <LibWeb/HTML/WindowEventHandlers.h>

namespace Web::HTML {

class IdleCallback;

// https://html.spec.whatwg.org/#timerhandler
using TimerHandler = Variant<JS::Handle<WebIDL::CallbackType>, String>;

class Window final
    : public DOM::EventTarget
    , public HTML::GlobalEventHandlers
    , public HTML::WindowEventHandlers {
    WEB_PLATFORM_OBJECT(Window, DOM::EventTarget);

public:
    static JS::NonnullGCPtr<Window> create(JS::Realm&);

    ~Window();

    virtual bool dispatch_event(DOM::Event&) override;

    Page* page();
    Page const* page() const;

    // https://html.spec.whatwg.org/multipage/window-object.html#concept-document-window
    DOM::Document const& associated_document() const { return *m_associated_document; }
    DOM::Document& associated_document() { return *m_associated_document; }
    void set_associated_document(DOM::Document&);

    // https://html.spec.whatwg.org/multipage/window-object.html#window-bc
    HTML::BrowsingContext const* browsing_context() const;
    HTML::BrowsingContext* browsing_context();

    JS::ThrowCompletionOr<size_t> document_tree_child_browsing_context_count() const;

    void alert_impl(String const&);
    bool confirm_impl(String const&);
    String prompt_impl(String const&, String const&);
    i32 request_animation_frame_impl(WebIDL::CallbackType& js_callback);
    void cancel_animation_frame_impl(i32);
    bool has_animation_frame_callbacks() const { return m_animation_frame_callback_driver.has_callbacks(); }

    i32 set_timeout_impl(TimerHandler, i32 timeout, JS::MarkedVector<JS::Value> arguments);
    i32 set_interval_impl(TimerHandler, i32 timeout, JS::MarkedVector<JS::Value> arguments);
    void clear_timeout_impl(i32);
    void clear_interval_impl(i32);

    void queue_microtask_impl(WebIDL::CallbackType& callback);

    int inner_width() const;
    int inner_height() const;

    void did_set_location_href(Badge<Bindings::LocationObject>, AK::URL const& new_href);
    void did_call_location_reload(Badge<Bindings::LocationObject>);
    void did_call_location_replace(Badge<Bindings::LocationObject>, String url);

    void deallocate_timer_id(Badge<Timer>, i32);

    HighResolutionTime::Performance& performance();

    Crypto::Crypto& crypto() { return *m_crypto; }

    CSS::Screen& screen();

    DOM::Event* current_event() { return m_current_event.ptr(); }
    DOM::Event const* current_event() const { return m_current_event.ptr(); }
    void set_current_event(DOM::Event* event);

    CSS::CSSStyleDeclaration* get_computed_style_impl(DOM::Element&) const;
    JS::NonnullGCPtr<CSS::MediaQueryList> match_media_impl(String);
    Optional<CSS::MediaFeatureValue> query_media_feature(CSS::MediaFeatureID) const;

    float scroll_x() const;
    float scroll_y() const;

    void fire_a_page_transition_event(FlyString const& event_name, bool persisted);

    float device_pixel_ratio() const;

    int screen_x() const;
    int screen_y() const;

    Selection::Selection* get_selection_impl();

    JS::NonnullGCPtr<HTML::Storage> local_storage();
    JS::NonnullGCPtr<HTML::Storage> session_storage();

    Window* parent();

    DOM::ExceptionOr<void> post_message_impl(JS::Value, String const& target_origin);

    String name() const;
    void set_name(String const&);

    void start_an_idle_period();

    u32 request_idle_callback_impl(WebIDL::CallbackType& callback);
    void cancel_idle_callback_impl(u32);

    AnimationFrameCallbackDriver& animation_frame_callback_driver() { return m_animation_frame_callback_driver; }

    // https://html.spec.whatwg.org/multipage/interaction.html#transient-activation
    bool has_transient_activation() const;

private:
    explicit Window(JS::Realm&);
    virtual void initialize(JS::Realm&) override;

    virtual void visit_edges(Cell::Visitor&) override;

    // ^HTML::GlobalEventHandlers
    virtual DOM::EventTarget& global_event_handlers_to_event_target(FlyString const&) override { return *this; }

    // ^HTML::WindowEventHandlers
    virtual DOM::EventTarget& window_event_handlers_to_event_target() override { return *this; }

    enum class Repeat {
        Yes,
        No,
    };
    i32 run_timer_initialization_steps(TimerHandler handler, i32 timeout, JS::MarkedVector<JS::Value> arguments, Repeat repeat, Optional<i32> previous_id = {});

    void invoke_idle_callbacks();

    // https://html.spec.whatwg.org/multipage/window-object.html#concept-document-window
    JS::GCPtr<DOM::Document> m_associated_document;

    JS::GCPtr<DOM::Event> m_current_event;

    IDAllocator m_timer_id_allocator;
    HashMap<int, JS::NonnullGCPtr<Timer>> m_timers;

    JS::GCPtr<HighResolutionTime::Performance> m_performance;
    JS::GCPtr<Crypto::Crypto> m_crypto;
    JS::GCPtr<CSS::Screen> m_screen;

    AnimationFrameCallbackDriver m_animation_frame_callback_driver;

    // https://w3c.github.io/requestidlecallback/#dfn-list-of-idle-request-callbacks
    NonnullRefPtrVector<IdleCallback> m_idle_request_callbacks;
    // https://w3c.github.io/requestidlecallback/#dfn-list-of-runnable-idle-callbacks
    NonnullRefPtrVector<IdleCallback> m_runnable_idle_callbacks;
    // https://w3c.github.io/requestidlecallback/#dfn-idle-callback-identifier
    u32 m_idle_callback_identifier = 0;

public:
    HTML::Origin origin() const;

    Bindings::LocationObject* location_object() { return m_location_object; }
    Bindings::LocationObject const* location_object() const { return m_location_object; }

    JS::Object* web_prototype(String const& class_name) { return m_prototypes.get(class_name).value_or(nullptr); }
    JS::NativeFunction* web_constructor(String const& class_name) { return m_constructors.get(class_name).value_or(nullptr); }

    JS::Object& cached_web_prototype(String const& class_name);

    template<typename T>
    JS::Object& ensure_web_prototype(String const& class_name)
    {
        auto it = m_prototypes.find(class_name);
        if (it != m_prototypes.end())
            return *it->value;
        auto& realm = shape().realm();
        auto* prototype = heap().allocate<T>(realm, realm);
        m_prototypes.set(class_name, prototype);
        return *prototype;
    }

    template<typename T>
    JS::NativeFunction& ensure_web_constructor(String const& class_name)
    {
        auto it = m_constructors.find(class_name);
        if (it != m_constructors.end())
            return *it->value;
        auto& realm = shape().realm();
        auto* constructor = heap().allocate<T>(realm, realm);
        m_constructors.set(class_name, constructor);
        define_direct_property(class_name, JS::Value(constructor), JS::Attribute::Writable | JS::Attribute::Configurable);
        return *constructor;
    }

    virtual JS::ThrowCompletionOr<bool> internal_set_prototype_of(JS::Object* prototype) override;

    CrossOriginPropertyDescriptorMap const& cross_origin_property_descriptor_map() const { return m_cross_origin_property_descriptor_map; }
    CrossOriginPropertyDescriptorMap& cross_origin_property_descriptor_map() { return m_cross_origin_property_descriptor_map; }

private:
    JS_DECLARE_NATIVE_FUNCTION(length_getter);
    JS_DECLARE_NATIVE_FUNCTION(top_getter);

    JS_DECLARE_NATIVE_FUNCTION(document_getter);

    JS_DECLARE_NATIVE_FUNCTION(frame_element_getter);

    JS_DECLARE_NATIVE_FUNCTION(location_getter);
    JS_DECLARE_NATIVE_FUNCTION(location_setter);

    JS_DECLARE_NATIVE_FUNCTION(name_getter);
    JS_DECLARE_NATIVE_FUNCTION(name_setter);

    JS_DECLARE_NATIVE_FUNCTION(performance_getter);
    JS_DECLARE_NATIVE_FUNCTION(performance_setter);

    JS_DECLARE_NATIVE_FUNCTION(history_getter);
    JS_DECLARE_NATIVE_FUNCTION(screen_getter);

    JS_DECLARE_NATIVE_FUNCTION(event_getter);
    JS_DECLARE_NATIVE_FUNCTION(event_setter);

    JS_DECLARE_NATIVE_FUNCTION(inner_width_getter);
    JS_DECLARE_NATIVE_FUNCTION(inner_height_getter);

    JS_DECLARE_NATIVE_FUNCTION(parent_getter);

    JS_DECLARE_NATIVE_FUNCTION(device_pixel_ratio_getter);

    JS_DECLARE_NATIVE_FUNCTION(scroll_x_getter);
    JS_DECLARE_NATIVE_FUNCTION(scroll_y_getter);
    JS_DECLARE_NATIVE_FUNCTION(scroll);
    JS_DECLARE_NATIVE_FUNCTION(scroll_by);

    JS_DECLARE_NATIVE_FUNCTION(screen_x_getter);
    JS_DECLARE_NATIVE_FUNCTION(screen_y_getter);
    JS_DECLARE_NATIVE_FUNCTION(screen_left_getter);
    JS_DECLARE_NATIVE_FUNCTION(screen_top_getter);

    JS_DECLARE_NATIVE_FUNCTION(post_message);

    JS_DECLARE_NATIVE_FUNCTION(local_storage_getter);
    JS_DECLARE_NATIVE_FUNCTION(session_storage_getter);
    JS_DECLARE_NATIVE_FUNCTION(origin_getter);

    JS_DECLARE_NATIVE_FUNCTION(alert);
    JS_DECLARE_NATIVE_FUNCTION(confirm);
    JS_DECLARE_NATIVE_FUNCTION(prompt);
    JS_DECLARE_NATIVE_FUNCTION(set_interval);
    JS_DECLARE_NATIVE_FUNCTION(set_timeout);
    JS_DECLARE_NATIVE_FUNCTION(clear_interval);
    JS_DECLARE_NATIVE_FUNCTION(clear_timeout);
    JS_DECLARE_NATIVE_FUNCTION(request_animation_frame);
    JS_DECLARE_NATIVE_FUNCTION(cancel_animation_frame);
    JS_DECLARE_NATIVE_FUNCTION(atob);
    JS_DECLARE_NATIVE_FUNCTION(btoa);

    JS_DECLARE_NATIVE_FUNCTION(get_computed_style);
    JS_DECLARE_NATIVE_FUNCTION(match_media);
    JS_DECLARE_NATIVE_FUNCTION(get_selection);

    JS_DECLARE_NATIVE_FUNCTION(queue_microtask);

    JS_DECLARE_NATIVE_FUNCTION(request_idle_callback);
    JS_DECLARE_NATIVE_FUNCTION(cancel_idle_callback);

    JS_DECLARE_NATIVE_FUNCTION(crypto_getter);

#define __ENUMERATE(attribute, event_name)          \
    JS_DECLARE_NATIVE_FUNCTION(attribute##_getter); \
    JS_DECLARE_NATIVE_FUNCTION(attribute##_setter);
    ENUMERATE_GLOBAL_EVENT_HANDLERS(__ENUMERATE);
    ENUMERATE_WINDOW_EVENT_HANDLERS(__ENUMERATE);
#undef __ENUMERATE

    Bindings::LocationObject* m_location_object { nullptr };

    HashMap<String, JS::Object*> m_prototypes;
    HashMap<String, JS::NativeFunction*> m_constructors;

    // [[CrossOriginPropertyDescriptorMap]], https://html.spec.whatwg.org/multipage/browsers.html#crossoriginpropertydescriptormap
    CrossOriginPropertyDescriptorMap m_cross_origin_property_descriptor_map;
};

void run_animation_frame_callbacks(DOM::Document&, double now);

}
