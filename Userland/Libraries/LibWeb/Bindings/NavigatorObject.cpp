/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FlyString.h>
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
    auto* languages = JS::Array::create(global_object);
    languages->indexed_properties().append(js_string(heap, "en-US"));

    define_property("appCodeName", js_string(heap, "Mozilla"));
    define_property("appName", js_string(heap, "Netscape"));
    define_property("appVersion", js_string(heap, "4.0"));
    define_property("language", languages->get(0));
    define_property("languages", languages);
    define_property("platform", js_string(heap, "SerenityOS"));
    define_property("product", js_string(heap, "Gecko"));

    define_native_property("userAgent", user_agent_getter, nullptr);
}

NavigatorObject::~NavigatorObject()
{
}

JS_DEFINE_NATIVE_GETTER(NavigatorObject::user_agent_getter)
{
    return JS::js_string(vm, ResourceLoader::the().user_agent());
}

}

}
