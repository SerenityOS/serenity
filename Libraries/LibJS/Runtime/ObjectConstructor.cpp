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
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ObjectConstructor.h>
#include <LibJS/Runtime/Shape.h>

namespace JS {

ObjectConstructor::ObjectConstructor()
    : NativeFunction("Object", *interpreter().global_object().function_prototype())
{
    define_property("prototype", interpreter().global_object().object_prototype(), 0);
    define_property("length", Value(1), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function("defineProperty", define_property_, 3, attr);
    define_native_function("is", is, 2, attr);
    define_native_function("getOwnPropertyDescriptor", get_own_property_descriptor, 2, attr);
    define_native_function("getOwnPropertyNames", get_own_property_names, 1, attr);
    define_native_function("getPrototypeOf", get_prototype_of, 1, attr);
    define_native_function("setPrototypeOf", set_prototype_of, 2, attr);
    define_native_function("isExtensible", is_extensible, 1, attr);
    define_native_function("preventExtensions", prevent_extensions, 1, attr);
    define_native_function("keys", keys, 1, attr);
    define_native_function("values", values, 1, attr);
    define_native_function("entries", entries, 1, attr);
}

ObjectConstructor::~ObjectConstructor()
{
}

Value ObjectConstructor::call(Interpreter& interpreter)
{
    return Object::create_empty(interpreter, interpreter.global_object());
}

Value ObjectConstructor::construct(Interpreter& interpreter)
{
    return call(interpreter);
}

Value ObjectConstructor::get_own_property_names(Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return {};
    auto* object = interpreter.argument(0).to_object(interpreter);
    if (interpreter.exception())
        return {};
    auto* result = Array::create(interpreter.global_object());
    for (auto& entry : object->indexed_properties())
        result->indexed_properties().append(js_string(interpreter, String::number(entry.index())));
    for (auto& it : object->shape().property_table_ordered())
        result->indexed_properties().append(js_string(interpreter, it.key));

    return result;
}

Value ObjectConstructor::get_prototype_of(Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return {};
    auto* object = interpreter.argument(0).to_object(interpreter);
    if (interpreter.exception())
        return {};
    return object->prototype();
}

Value ObjectConstructor::set_prototype_of(Interpreter& interpreter)
{
    if (interpreter.argument_count() < 2)
        return {};
    auto* object = interpreter.argument(0).to_object(interpreter);
    if (interpreter.exception())
        return {};
    auto prototype_value = interpreter.argument(1);
    Object* prototype;
    if (prototype_value.is_null()) {
        prototype = nullptr;
    } else if (prototype_value.is_object()) {
        prototype = &prototype_value.as_object();
    } else {
        interpreter.throw_exception<TypeError>("Prototype must be null or object");
        return {};
    }
    if (!object->set_prototype(prototype)) {
        interpreter.throw_exception<TypeError>("Can't set prototype of non-extensible object");
        return {};
    }
    return object;
}

Value ObjectConstructor::is_extensible(Interpreter& interpreter)
{
    auto argument = interpreter.argument(0);
    if (!argument.is_object())
        return Value(false);
    return Value(argument.as_object().is_extensible());
}

Value ObjectConstructor::prevent_extensions(Interpreter& interpreter)
{
    auto argument = interpreter.argument(0);
    if (!argument.is_object())
        return argument;
    if (!argument.as_object().prevent_extensions()) {
        interpreter.throw_exception<TypeError>("Proxy preventExtensions handler returned false");
        return {};
    }
    return argument;
}

Value ObjectConstructor::get_own_property_descriptor(Interpreter& interpreter)
{
    auto* object = interpreter.argument(0).to_object(interpreter);
    if (interpreter.exception())
        return {};
    auto property_key = interpreter.argument(1).to_string(interpreter);
    if (interpreter.exception())
        return {};
    return object->get_own_property_descriptor(property_key);
}

Value ObjectConstructor::define_property_(Interpreter& interpreter)
{
    if (!interpreter.argument(0).is_object())
        return interpreter.throw_exception<TypeError>("Object argument is not an object");
    if (!interpreter.argument(2).is_object())
        return interpreter.throw_exception<TypeError>("Descriptor argument is not an object");
    auto& object = interpreter.argument(0).as_object();
    auto property_key = interpreter.argument(1).to_string(interpreter);
    if (interpreter.exception())
        return {};
    auto& descriptor = interpreter.argument(2).as_object();
    object.define_property(property_key, descriptor);
    return &object;
}

Value ObjectConstructor::is(Interpreter& interpreter)
{
    return Value(same_value(interpreter, interpreter.argument(0), interpreter.argument(1)));
}

Value ObjectConstructor::keys(Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return interpreter.throw_exception<TypeError>("Can't convert undefined to object");

    auto* obj_arg = interpreter.argument(0).to_object(interpreter);
    if (interpreter.exception())
        return {};

    return obj_arg->get_own_properties(*obj_arg, GetOwnPropertyMode::Key, Attribute::Enumerable);
}

Value ObjectConstructor::values(Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return interpreter.throw_exception<TypeError>("Can't convert undefined to object");

    auto* obj_arg = interpreter.argument(0).to_object(interpreter);
    if (interpreter.exception())
        return {};

    return obj_arg->get_own_properties(*obj_arg, GetOwnPropertyMode::Value, Attribute::Enumerable);
}

Value ObjectConstructor::entries(Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return interpreter.throw_exception<TypeError>("Can't convert undefined to object");

    auto* obj_arg = interpreter.argument(0).to_object(interpreter);
    if (interpreter.exception())
        return {};

    return obj_arg->get_own_properties(*obj_arg, GetOwnPropertyMode::KeyAndValue, Attribute::Enumerable);
}

}
