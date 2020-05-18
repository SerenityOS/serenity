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
#include <LibJS/Interpreter.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Window.h>

namespace Web {
namespace Bindings {

LocationObject::LocationObject()
    : Object(interpreter().global_object().object_prototype())
{
    put_native_property("href", href_getter, href_setter);
    put_native_property("hostname", hostname_getter, nullptr);
    put_native_property("pathname", pathname_getter, nullptr);
    put_native_property("hash", hash_getter, nullptr);
    put_native_property("search", search_getter, nullptr);
}

LocationObject::~LocationObject()
{
}

JS::Value LocationObject::href_getter(JS::Interpreter& interpreter)
{
    auto& window = static_cast<WindowObject&>(interpreter.global_object());
    return JS::js_string(interpreter, window.impl().document().url().to_string());
}

void LocationObject::href_setter(JS::Interpreter&, JS::Value)
{
    // FIXME: Navigate to a new URL
}

JS::Value LocationObject::pathname_getter(JS::Interpreter& interpreter)
{
    auto& window = static_cast<WindowObject&>(interpreter.global_object());
    return JS::js_string(interpreter, window.impl().document().url().path());
}

JS::Value LocationObject::hostname_getter(JS::Interpreter& interpreter)
{
    auto& window = static_cast<WindowObject&>(interpreter.global_object());
    return JS::js_string(interpreter, window.impl().document().url().host());
}

JS::Value LocationObject::hash_getter(JS::Interpreter& interpreter)
{
    auto& window = static_cast<WindowObject&>(interpreter.global_object());
    return JS::js_string(interpreter, window.impl().document().url().fragment());
}

JS::Value LocationObject::search_getter(JS::Interpreter& interpreter)
{
    auto& window = static_cast<WindowObject&>(interpreter.global_object());
    return JS::js_string(interpreter, window.impl().document().url().query());
}

}

}
