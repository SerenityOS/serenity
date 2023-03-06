/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/IDAllocator.h>
#include <AK/RefPtr.h>
#include <AK/TypeCasts.h>
#include <AK/URL.h>
#include <LibJS/Heap/Heap.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/WindowGlobalMixin.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/AnimationFrameCallbackDriver.h>
#include <LibWeb/HTML/CrossOrigin/CrossOriginPropertyDescriptorMap.h>
#include <LibWeb/HTML/GlobalEventHandlers.h>
#include <LibWeb/HTML/MimeType.h>
#include <LibWeb/HTML/Plugin.h>
#include <LibWeb/HTML/Scripting/ImportMap.h>
#include <LibWeb/HTML/WindowEventHandlers.h>
#include <LibWeb/HTML/WindowOrWorkerGlobalScope.h>

namespace Web::HTML {

class IdleCallback;

// https://html.spec.whatwg.org/#timerhandler
using TimerHandler = Variant<JS::Handle<WebIDL::CallbackType>, DeprecatedString>;

class Window final
    : public DOM::EventTarget
    , public HTML::GlobalEventHandlers
    , public HTML::WindowEventHandlers
    , public WindowOrWorkerGlobalScopeMixin
    , public Bindings::WindowGlobalMixin {
    WEB_PLATFORM_OBJECT(Window, DOM::EventTarget);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Window>> create(JS::Realm&);

    ~Window();

    using WindowOrWorkerGlobalScopeMixin::btoa;

    // ^DOM::EventTarget
    virtual bool dispatch_event(DOM::Event&) override;

    // ^WindowOrWorkerGlobalScopeMixin
    virtual Bindings::PlatformObject& this_impl() override { return *this; }
    virtual Bindings::PlatformObject const& this_impl() const override { return *this; }

    Page* page();
    Page const* page() const;

    // https://html.spec.whatwg.org/multipage/window-object.html#concept-document-window
    DOM::Document const& associated_document() const { return *m_associated_document; }
    DOM::Document& associated_document() { return *m_associated_document; }
    void set_associated_document(DOM::Document&);

    // https://html.spec.whatwg.org/multipage/window-object.html#window-bc
    HTML::BrowsingContext const* browsing_context() const;
    HTML::BrowsingContext* browsing_context();

    size_t document_tree_child_browsing_context_count() const;

    ImportMap const& import_map() const { return m_import_map; }

    bool import_maps_allowed() const { return m_import_maps_allowed; }
    void set_import_maps_allowed(bool import_maps_allowed) { m_import_maps_allowed = import_maps_allowed; }

    WebIDL::ExceptionOr<JS::GCPtr<HTML::WindowProxy>> open_impl(StringView url, StringView target, StringView features);
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

    void did_set_location_href(Badge<HTML::Location>, AK::URL const& new_href);
    void did_call_location_reload(Badge<HTML::Location>);
    void did_call_location_replace(Badge<HTML::Location>, DeprecatedString url);

    void deallocate_timer_id(Badge<Timer>, i32);

    HighResolutionTime::Performance& performance();

    Crypto::Crypto& crypto() { return *m_crypto; }

    CSS::Screen& screen();

    DOM::Event* current_event() { return m_current_event.ptr(); }
    DOM::Event const* current_event() const { return m_current_event.ptr(); }
    void set_current_event(DOM::Event* event);

    CSS::CSSStyleDeclaration* get_computed_style_impl(DOM::Element&) const;
    JS::NonnullGCPtr<CSS::MediaQueryList> match_media_impl(DeprecatedString);
    Optional<CSS::MediaFeatureValue> query_media_feature(CSS::MediaFeatureID) const;

    float scroll_x() const;
    float scroll_y() const;

    void fire_a_page_transition_event(DeprecatedFlyString const& event_name, bool persisted);

    float device_pixel_ratio() const;

    int screen_x() const;
    int screen_y() const;

    int outer_width() const;
    int outer_height() const;

    JS::NonnullGCPtr<HTML::Storage> local_storage();
    JS::NonnullGCPtr<HTML::Storage> session_storage();

    WebIDL::ExceptionOr<JS::Value> structured_clone_impl(JS::VM& vm, JS::Value);

    void start_an_idle_period();

    u32 request_idle_callback_impl(WebIDL::CallbackType& callback);
    void cancel_idle_callback_impl(u32);

    AnimationFrameCallbackDriver& animation_frame_callback_driver() { return m_animation_frame_callback_driver; }

    // https://html.spec.whatwg.org/multipage/interaction.html#transient-activation
    bool has_transient_activation() const;

    WebIDL::ExceptionOr<void> initialize_web_interfaces(Badge<WindowEnvironmentSettingsObject>);

    Vector<JS::NonnullGCPtr<Plugin>> pdf_viewer_plugin_objects();
    Vector<JS::NonnullGCPtr<MimeType>> pdf_viewer_mime_type_objects();

    // JS API functions
    JS::NonnullGCPtr<WindowProxy> window() const;
    JS::NonnullGCPtr<WindowProxy> self() const;
    JS::NonnullGCPtr<DOM::Document const> document() const;
    String name() const;
    void set_name(String const&);
    JS::NonnullGCPtr<Location> location() const;
    JS::NonnullGCPtr<History> history() const;

    JS::NonnullGCPtr<WindowProxy> frames() const;
    u32 length() const;
    JS::GCPtr<WindowProxy const> top() const;
    JS::GCPtr<WindowProxy const> parent() const;
    JS::GCPtr<DOM::Element const> frame_element() const;
    WebIDL::ExceptionOr<JS::GCPtr<HTML::WindowProxy>> open(Optional<String> const& url, Optional<String> const& target, Optional<String> const& features);

    JS::NonnullGCPtr<Navigator> navigator() const;

    void alert(String const& message = {});
    bool confirm(Optional<String> const& message);
    Optional<String> prompt(Optional<String> const& message, Optional<String> const& default_);

    void post_message(JS::Value message, String const&);

private:
    explicit Window(JS::Realm&);

    virtual void visit_edges(Cell::Visitor&) override;

    // ^HTML::GlobalEventHandlers
    virtual DOM::EventTarget& global_event_handlers_to_event_target(DeprecatedFlyString const&) override { return *this; }

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

    // https://html.spec.whatwg.org/multipage/webappapis.html#concept-window-import-map
    ImportMap m_import_map;

    // https://html.spec.whatwg.org/multipage/webappapis.html#import-maps-allowed
    bool m_import_maps_allowed { true };

    JS::GCPtr<HighResolutionTime::Performance> m_performance;
    JS::GCPtr<Crypto::Crypto> m_crypto;
    JS::GCPtr<CSS::Screen> m_screen;
    JS::GCPtr<HTML::Navigator> m_navigator;

    AnimationFrameCallbackDriver m_animation_frame_callback_driver;

    // https://w3c.github.io/requestidlecallback/#dfn-list-of-idle-request-callbacks
    Vector<NonnullRefPtr<IdleCallback>> m_idle_request_callbacks;
    // https://w3c.github.io/requestidlecallback/#dfn-list-of-runnable-idle-callbacks
    Vector<NonnullRefPtr<IdleCallback>> m_runnable_idle_callbacks;
    // https://w3c.github.io/requestidlecallback/#dfn-idle-callback-identifier
    u32 m_idle_callback_identifier = 0;

    // https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewer-plugin-objects
    Vector<JS::NonnullGCPtr<Plugin>> m_pdf_viewer_plugin_objects;

    // https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewer-mime-type-objects
    Vector<JS::NonnullGCPtr<MimeType>> m_pdf_viewer_mime_type_objects;

public:
    virtual JS::ThrowCompletionOr<bool> internal_set_prototype_of(JS::Object* prototype) override;

    CrossOriginPropertyDescriptorMap const& cross_origin_property_descriptor_map() const { return m_cross_origin_property_descriptor_map; }
    CrossOriginPropertyDescriptorMap& cross_origin_property_descriptor_map() { return m_cross_origin_property_descriptor_map; }

private:
    JS_DECLARE_NATIVE_FUNCTION(location_setter);

    JS_DECLARE_NATIVE_FUNCTION(performance_getter);
    JS_DECLARE_NATIVE_FUNCTION(performance_setter);

    JS_DECLARE_NATIVE_FUNCTION(screen_getter);
    JS_DECLARE_NATIVE_FUNCTION(screen_setter);

    JS_DECLARE_NATIVE_FUNCTION(event_getter);
    JS_DECLARE_NATIVE_FUNCTION(event_setter);

    JS_DECLARE_NATIVE_FUNCTION(inner_width_getter);
    JS_DECLARE_NATIVE_FUNCTION(inner_height_getter);

    JS_DECLARE_NATIVE_FUNCTION(device_pixel_ratio_getter);

    JS_DECLARE_NATIVE_FUNCTION(scroll_x_getter);
    JS_DECLARE_NATIVE_FUNCTION(scroll_y_getter);
    JS_DECLARE_NATIVE_FUNCTION(scroll);
    JS_DECLARE_NATIVE_FUNCTION(scroll_by);

    JS_DECLARE_NATIVE_FUNCTION(screen_x_getter);
    JS_DECLARE_NATIVE_FUNCTION(screen_y_getter);
    JS_DECLARE_NATIVE_FUNCTION(screen_left_getter);
    JS_DECLARE_NATIVE_FUNCTION(screen_top_getter);

    JS_DECLARE_NATIVE_FUNCTION(outer_width_getter);
    JS_DECLARE_NATIVE_FUNCTION(outer_height_getter);

    JS_DECLARE_NATIVE_FUNCTION(structured_clone);

    JS_DECLARE_NATIVE_FUNCTION(local_storage_getter);
    JS_DECLARE_NATIVE_FUNCTION(session_storage_getter);

    JS_DECLARE_NATIVE_FUNCTION(set_interval);
    JS_DECLARE_NATIVE_FUNCTION(set_timeout);
    JS_DECLARE_NATIVE_FUNCTION(clear_interval);
    JS_DECLARE_NATIVE_FUNCTION(clear_timeout);
    JS_DECLARE_NATIVE_FUNCTION(request_animation_frame);
    JS_DECLARE_NATIVE_FUNCTION(cancel_animation_frame);
    JS_DECLARE_NATIVE_FUNCTION(atob);
    JS_DECLARE_NATIVE_FUNCTION(focus);

    JS_DECLARE_NATIVE_FUNCTION(get_computed_style);
    JS_DECLARE_NATIVE_FUNCTION(match_media);
    JS_DECLARE_NATIVE_FUNCTION(get_selection);

    JS_DECLARE_NATIVE_FUNCTION(queue_microtask);

    JS_DECLARE_NATIVE_FUNCTION(request_idle_callback);
    JS_DECLARE_NATIVE_FUNCTION(cancel_idle_callback);

    JS_DECLARE_NATIVE_FUNCTION(crypto_getter);

    HTML::Location* m_location { nullptr };

    // [[CrossOriginPropertyDescriptorMap]], https://html.spec.whatwg.org/multipage/browsers.html#crossoriginpropertydescriptormap
    CrossOriginPropertyDescriptorMap m_cross_origin_property_descriptor_map;
};

void run_animation_frame_callbacks(DOM::Document&, double now);

}
