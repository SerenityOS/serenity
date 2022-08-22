/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/TypeCasts.h>
#include <AK/Variant.h>
#include <AK/Weakable.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/CallbackType.h>
#include <LibWeb/Bindings/CrossOriginAbstractOperations.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/GlobalEventHandlers.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/WindowEventHandlers.h>

namespace Web {
namespace Bindings {

// https://html.spec.whatwg.org/#timerhandler
using TimerHandler = Variant<CallbackType, String>;

class WindowObject
    : public JS::GlobalObject
    , public Weakable<WindowObject> {
    JS_OBJECT(WindowObject, JS::GlobalObject);

public:
    explicit WindowObject(JS::Realm&, HTML::Window&);
    virtual void initialize_global_object(JS::Realm&) override;
    virtual ~WindowObject() override = default;

    HTML::Window& impl() { return *m_impl; }
    const HTML::Window& impl() const { return *m_impl; }

    HTML::Origin origin() const;

    LocationObject* location_object() { return m_location_object; }
    LocationObject const* location_object() const { return m_location_object; }

    JS::Object* web_prototype(String const& class_name) { return m_prototypes.get(class_name).value_or(nullptr); }
    JS::NativeFunction* web_constructor(String const& class_name) { return m_constructors.get(class_name).value_or(nullptr); }

    template<typename T>
    JS::Object& ensure_web_prototype(String const& class_name)
    {
        auto it = m_prototypes.find(class_name);
        if (it != m_prototypes.end())
            return *it->value;
        auto& realm = *associated_realm();
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
        auto& realm = *associated_realm();
        auto* constructor = heap().allocate<T>(realm, realm);
        m_constructors.set(class_name, constructor);
        define_direct_property(class_name, JS::Value(constructor), JS::Attribute::Writable | JS::Attribute::Configurable);
        return *constructor;
    }

    virtual JS::ThrowCompletionOr<bool> internal_set_prototype_of(JS::Object* prototype) override;

    CrossOriginPropertyDescriptorMap const& cross_origin_property_descriptor_map() const { return m_cross_origin_property_descriptor_map; }
    CrossOriginPropertyDescriptorMap& cross_origin_property_descriptor_map() { return m_cross_origin_property_descriptor_map; }

private:
    virtual void visit_edges(Visitor&) override;

    JS_DECLARE_NATIVE_FUNCTION(top_getter);

    JS_DECLARE_NATIVE_FUNCTION(document_getter);

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

    NonnullRefPtr<HTML::Window> m_impl;

    LocationObject* m_location_object { nullptr };

    HashMap<String, JS::Object*> m_prototypes;
    HashMap<String, JS::NativeFunction*> m_constructors;

    // [[CrossOriginPropertyDescriptorMap]], https://html.spec.whatwg.org/multipage/browsers.html#crossoriginpropertydescriptormap
    CrossOriginPropertyDescriptorMap m_cross_origin_property_descriptor_map;
};

}
}
