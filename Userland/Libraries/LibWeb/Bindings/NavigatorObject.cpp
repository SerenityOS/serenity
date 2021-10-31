/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/NavigatorObject.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web {
namespace Bindings {

NavigatorObject::NavigatorObject(JS::GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void NavigatorObject::initialize(JS::GlobalObject& global_object)
{
    auto& heap = this->heap();
    auto* languages = MUST(JS::Array::create(global_object, 0));
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

    // FIXME: Reflect actual connectivity status.
    define_direct_property("onLine", JS::Value(true), attr);
}

NavigatorObject::~NavigatorObject()
{
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

}

}
