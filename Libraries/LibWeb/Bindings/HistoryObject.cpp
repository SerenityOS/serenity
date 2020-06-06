/*
 * Copyright (c) 2020, Luke Wilde <luke.wilde@live.co.uk>
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

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/HistoryObject.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Window.h>

namespace Web {
namespace Bindings {

HistoryObject::HistoryObject()
    : Object(interpreter().global_object().object_prototype())
{
    u8 attr = JS::Attribute::Writable | JS::Attribute::Enumerable;

    define_native_function("go", go, 0, attr);
    define_native_function("back", back, 0, attr);
    define_native_function("forward", forward, 0, attr);

    define_native_property("length", length_getter, nullptr, JS::Attribute::Configurable);
}

HistoryObject::~HistoryObject()
{
}

JS::Value HistoryObject::go(JS::Interpreter& interpreter)
{
    // FIXME: This method should be asynchronous.
    
    i32 delta = 0;

    if (interpreter.argument_count() > 0)
    {
        delta = interpreter.argument(0).to_i32(interpreter);
        if (interpreter.exception())
            return {};
    }

    auto& window = static_cast<WindowObject&>(interpreter.global_object());

    // FIXME: If document is not fully active, then throw a "SecurityError" DOMException.

    window.impl().did_call_history_navigation({}, delta);

    return JS::js_undefined();
}

JS::Value HistoryObject::back(JS::Interpreter& interpreter)
{
    // FIXME: This method should be asynchronous.

    auto& window = static_cast<WindowObject&>(interpreter.global_object());

    // FIXME: If document is not fully active, then throw a "SecurityError" DOMException.

    window.impl().did_call_history_navigation({}, -1);

    return JS::js_undefined();
}

JS::Value HistoryObject::forward(JS::Interpreter& interpreter)
{
    // FIXME: This method should be asynchronous.

    auto& window = static_cast<WindowObject&>(interpreter.global_object());

    // FIXME: If document is not fully active, then throw a "SecurityError" DOMException.

    window.impl().did_call_history_navigation({}, 1);

    return JS::js_undefined();
}

JS::Value HistoryObject::length_getter(JS::Interpreter& interpreter)
{
    auto& window = static_cast<WindowObject&>(interpreter.global_object());

    return JS::Value((i32)window.impl().num_history_entries({}));
}

}
}
