/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    window.impl().did_set_location_href({}, new_href);
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
