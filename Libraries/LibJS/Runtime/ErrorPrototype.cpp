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

#include <AK/Function.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

ErrorPrototype::ErrorPrototype()
    : Object(interpreter().global_object().object_prototype())
{
    put_native_property("name", name_getter, name_setter);
    put_native_property("message", message_getter, nullptr);
    put_native_function("toString", to_string);
}

ErrorPrototype::~ErrorPrototype()
{
}

Value ErrorPrototype::name_getter(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!this_object->is_error())
        return interpreter.throw_exception<TypeError>("Not an Error object");
    return js_string(interpreter, static_cast<const Error*>(this_object)->name());
}

void ErrorPrototype::name_setter(Interpreter& interpreter, Value value)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return;
    if (!this_object->is_error()) {
        interpreter.throw_exception<TypeError>("Not an Error object");
        return;
    }
    auto name = FlyString(value.to_string());
    static_cast<Error*>(this_object)->set_name(name);
}

Value ErrorPrototype::message_getter(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!this_object->is_error())
        return interpreter.throw_exception<TypeError>("Not an Error object");
    return js_string(interpreter, static_cast<const Error*>(this_object)->message());
}

Value ErrorPrototype::to_string(Interpreter& interpreter)
{
    if (!interpreter.this_value().is_object())
        return interpreter.throw_exception<TypeError>("Not an object");
    auto& this_object = interpreter.this_value().as_object();

    String name = "Error";
    auto object_name_property = this_object.get("name");
    if (object_name_property.has_value() && !object_name_property.value().is_undefined())
        name = object_name_property.value().to_string();

    String message = "";
    auto object_message_property = this_object.get("message");
    if (object_message_property.has_value() && !object_message_property.value().is_undefined())
        message = object_message_property.value().to_string();

    if (name.length() == 0)
        return js_string(interpreter, message);
    if (message.length() == 0)
        return js_string(interpreter, name);
    return js_string(interpreter, String::format("%s: %s", name.characters(), message.characters()));
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName) \
    PrototypeName::PrototypeName()                                            \
        : Object(interpreter().global_object().error_prototype())             \
    {                                                                         \
    }                                                                         \
    PrototypeName::~PrototypeName() {}                                        \
    const char* PrototypeName::class_name() const { return #PrototypeName; }

JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE

}
