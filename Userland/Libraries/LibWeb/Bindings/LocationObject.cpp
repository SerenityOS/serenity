/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FlyString.h>
#include <AK/StringBuilder.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Window.h>

namespace Web {
namespace Bindings {

LocationObject::LocationObject(JS::GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void LocationObject::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);
    u8 attr = JS::Attribute::Writable | JS::Attribute::Enumerable;
    define_native_property("href", href_getter, href_setter, attr);
    define_native_property("host", host_getter, nullptr, attr);
    define_native_property("hostname", hostname_getter, nullptr, attr);
    define_native_property("pathname", pathname_getter, nullptr, attr);
    define_native_property("hash", hash_getter, nullptr, attr);
    define_native_property("search", search_getter, nullptr, attr);
    define_native_property("protocol", protocol_getter, nullptr, attr);

    define_native_function("reload", reload, 0, JS::Attribute::Enumerable);
}

LocationObject::~LocationObject()
{
}

JS_DEFINE_NATIVE_GETTER(LocationObject::href_getter)
{
    auto& window = static_cast<WindowObject&>(global_object);
    return JS::js_string(vm, window.impl().document().url().to_string());
}

JS_DEFINE_NATIVE_SETTER(LocationObject::href_setter)
{
    auto& window = static_cast<WindowObject&>(global_object);
    auto new_href = value.to_string(global_object);
    if (vm.exception())
        return;
    auto href_url = window.impl().document().complete_url(new_href);
    if (!href_url.is_valid()) {
        vm.throw_exception<JS::URIError>(global_object, String::formatted("Invalid URL '{}'", new_href));
        return;
    }
    window.impl().did_set_location_href({}, href_url);
}

JS_DEFINE_NATIVE_GETTER(LocationObject::pathname_getter)
{
    auto& window = static_cast<WindowObject&>(global_object);
    return JS::js_string(vm, window.impl().document().url().path());
}

JS_DEFINE_NATIVE_GETTER(LocationObject::hostname_getter)
{
    auto& window = static_cast<WindowObject&>(global_object);
    return JS::js_string(vm, window.impl().document().url().host());
}

JS_DEFINE_NATIVE_GETTER(LocationObject::host_getter)
{
    auto& window = static_cast<WindowObject&>(global_object);
    auto url = window.impl().document().url();
    StringBuilder builder;
    builder.append(url.host());
    builder.append(':');
    builder.appendf("%u", url.port());
    return JS::js_string(vm, builder.to_string());
}

JS_DEFINE_NATIVE_GETTER(LocationObject::hash_getter)
{
    auto& window = static_cast<WindowObject&>(global_object);
    auto fragment = window.impl().document().url().fragment();
    if (!fragment.length())
        return JS::js_string(vm, "");
    StringBuilder builder;
    builder.append('#');
    builder.append(fragment);
    return JS::js_string(vm, builder.to_string());
}

JS_DEFINE_NATIVE_GETTER(LocationObject::search_getter)
{
    auto& window = static_cast<WindowObject&>(global_object);
    auto query = window.impl().document().url().query();
    if (!query.length())
        return JS::js_string(vm, "");
    StringBuilder builder;
    builder.append('?');
    builder.append(query);
    return JS::js_string(vm, builder.to_string());
}

JS_DEFINE_NATIVE_GETTER(LocationObject::protocol_getter)
{
    auto& window = static_cast<WindowObject&>(global_object);
    StringBuilder builder;
    builder.append(window.impl().document().url().protocol());
    builder.append(':');
    return JS::js_string(vm, builder.to_string());
}

JS_DEFINE_NATIVE_FUNCTION(LocationObject::reload)
{
    auto& window = static_cast<WindowObject&>(global_object);
    window.impl().did_call_location_reload({});
    return JS::js_undefined();
}

}

}
