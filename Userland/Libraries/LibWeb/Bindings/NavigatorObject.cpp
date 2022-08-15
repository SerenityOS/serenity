/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/NavigatorObject.h>
#include <LibWeb/Bindings/NavigatorPrototype.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web {
namespace Bindings {

NavigatorObject::NavigatorObject(JS::Realm& realm)
    : Object(static_cast<WindowObject&>(realm.global_object()).ensure_web_prototype<NavigatorPrototype>("Navigator"))
{
}

void NavigatorObject::initialize(JS::Realm& realm)
{
    auto& heap = this->heap();
    auto* languages = MUST(JS::Array::create(realm, 0));
    languages->indexed_properties().append(js_string(heap, "en-US"));

    // FIXME: All of these should be in Navigator's prototype and be native accessors
    u8 attr = JS::Attribute::Configurable | JS::Attribute::Writable | JS::Attribute::Enumerable;
    define_direct_property("appCodeName", js_string(heap, "Mozilla"), attr);
    define_direct_property("appName", js_string(heap, "Netscape"), attr);
    define_direct_property("appVersion", js_string(heap, "4.0"), attr);
    define_direct_property("language", languages->get_without_side_effects(0), attr);
    define_direct_property("languages", languages, attr);
    define_direct_property("platform", js_string(heap, "SerenityOS"), attr);
    define_direct_property("product", js_string(heap, "Gecko"), attr);

    define_native_accessor("userAgent", user_agent_getter, {}, JS::Attribute::Configurable | JS::Attribute::Enumerable);
    define_native_accessor("cookieEnabled", cookie_enabled_getter, {}, JS::Attribute::Configurable | JS::Attribute::Enumerable);

    define_native_function("javaEnabled", java_enabled, 0, JS::Attribute::Configurable | JS::Attribute::Enumerable);

    // FIXME: Reflect actual connectivity status.
    define_direct_property("onLine", JS::Value(true), attr);
}

JS_DEFINE_NATIVE_FUNCTION(NavigatorObject::user_agent_getter)
{
    return JS::js_string(vm, ResourceLoader::the().user_agent());
}

JS_DEFINE_NATIVE_FUNCTION(NavigatorObject::cookie_enabled_getter)
{
    // No way of disabling cookies right now :^)
    return JS::Value(true);
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-javaenabled
JS_DEFINE_NATIVE_FUNCTION(NavigatorObject::java_enabled)
{
    // The NavigatorPlugins mixin's javaEnabled() method steps are to return false.
    return JS::Value(false);
}

}

}
