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
#include <AK/Function.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Window.h>

namespace Web {
namespace Bindings {

WindowObject::WindowObject(Window& impl)
    : m_impl(impl)
{
    put_native_property("document", document_getter, document_setter);
    put_native_function("alert", alert);
    put_native_function("setInterval", set_interval);
    put_native_function("requestAnimationFrame", request_animation_frame);
    put_native_function("cancelAnimationFrame", cancel_animation_frame);
}

WindowObject::~WindowObject()
{
}

static Window* impl_from(JS::Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object) {
        dbg() << "this_object is null";
        ASSERT_NOT_REACHED();
        return nullptr;
    }
    if (StringView("WindowObject") != this_object->class_name()) {
        interpreter.throw_exception<JS::Error>("TypeError", "That's not a WindowObject, bro.");
        dbg() << "this_object class_name is '" << this_object->class_name() << "'";
        return nullptr;
    }
    return &static_cast<WindowObject*>(this_object)->impl();
}

JS::Value WindowObject::alert(JS::Interpreter& interpreter)
{
    dbg() << "alert entry";
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    dbg() << "alert2 entry";
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.size() < 1)
        return {};
    impl->alert(arguments[0].to_string());
    return {};
}

JS::Value WindowObject::set_interval(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.size() < 2)
        return {};
    auto* callback_object = arguments[0].to_object(interpreter.heap());
    if (!callback_object)
        return {};
    if (!callback_object->is_function())
        return interpreter.throw_exception<JS::Error>("TypeError", "Not a function");
    impl->set_interval(*static_cast<JS::Function*>(callback_object), arguments[1].to_i32());
    return {};
}

JS::Value WindowObject::request_animation_frame(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.size() < 1)
        return {};
    auto* callback_object = arguments[0].to_object(interpreter.heap());
    if (!callback_object)
        return {};
    if (!callback_object->is_function())
        return interpreter.throw_exception<JS::Error>("TypeError", "Not a function");
    return JS::Value(impl->request_animation_frame(*static_cast<JS::Function*>(callback_object)));
}


JS::Value WindowObject::cancel_animation_frame(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.size() < 1)
        return {};
    impl->cancel_animation_frame(arguments[0].to_i32());
    return {};
}

JS::Value WindowObject::document_getter(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    return wrap(interpreter.heap(), impl->document());
}

void WindowObject::document_setter(JS::Interpreter&, JS::Value)
{
    // FIXME: Figure out what we should do here. Just ignore attempts to set window.document for now.
}

}
}
