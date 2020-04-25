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
    put("prototype", interpreter().global_object().object_prototype());

    put_native_function("defineProperty", define_property, 3);
    put_native_function("is", is, 2);
    put_native_function("getOwnPropertyDescriptor", get_own_property_descriptor, 2);
    put_native_function("getOwnPropertyNames", get_own_property_names, 1);
    put_native_function("getPrototypeOf", get_prototype_of, 1);
    put_native_function("setPrototypeOf", set_prototype_of, 2);
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
    auto* object = interpreter.argument(0).to_object(interpreter.heap());
    if (interpreter.exception())
        return {};
    auto* result = Array::create(interpreter.global_object());
    for (size_t i = 0; i < object->elements().size(); ++i) {
        if (!object->elements()[i].is_empty())
            result->elements().append(js_string(interpreter, String::number(i)));
    }

    for (auto& it : object->shape().property_table())
        result->elements().append(js_string(interpreter, it.key));
    return result;
}

Value ObjectConstructor::get_prototype_of(Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return {};
    auto* object = interpreter.argument(0).to_object(interpreter.heap());
    if (interpreter.exception())
        return {};
    return object->prototype();
}

Value ObjectConstructor::set_prototype_of(Interpreter& interpreter)
{
    if (interpreter.argument_count() < 2)
        return {};
    auto* object = interpreter.argument(0).to_object(interpreter.heap());
    if (interpreter.exception())
        return {};
    object->set_prototype(&const_cast<Object&>(interpreter.argument(1).as_object()));
    return {};
}

Value ObjectConstructor::get_own_property_descriptor(Interpreter& interpreter)
{
    if (interpreter.argument_count() < 2)
        return interpreter.throw_exception<TypeError>("Object.getOwnPropertyDescriptor() needs 2 arguments");
    if (!interpreter.argument(0).is_object())
        return interpreter.throw_exception<TypeError>("Object argument is not an object");
    auto& object = interpreter.argument(0).as_object();
    auto metadata = object.shape().lookup(interpreter.argument(1).to_string());
    if (!metadata.has_value())
        return js_undefined();

    auto value = object.get(interpreter.argument(1).to_string()).value_or(js_undefined());
    if (interpreter.exception())
        return {};

    auto* descriptor = Object::create_empty(interpreter, interpreter.global_object());
    descriptor->put("configurable", Value(!!(metadata.value().attributes & Attribute::Configurable)));
    descriptor->put("enumerable", Value(!!(metadata.value().attributes & Attribute::Enumerable)));
    descriptor->put("writable", Value(!!(metadata.value().attributes & Attribute::Writable)));
    descriptor->put("value", value);
    return descriptor;
}

Value ObjectConstructor::define_property(Interpreter& interpreter)
{
    if (interpreter.argument_count() < 3)
        return interpreter.throw_exception<TypeError>("Object.defineProperty() needs 3 arguments");
    if (!interpreter.argument(0).is_object())
        return interpreter.throw_exception<TypeError>("Object argument is not an object");
    if (!interpreter.argument(2).is_object())
        return interpreter.throw_exception<TypeError>("Descriptor argument is not an object");
    auto& object = interpreter.argument(0).as_object();
    auto& descriptor = interpreter.argument(2).as_object();

    auto value = descriptor.get("value");
    u8 configurable = descriptor.get("configurable").value_or(Value(false)).to_boolean() * Attribute::Configurable;
    u8 enumerable = descriptor.get("enumerable").value_or(Value(false)).to_boolean() * Attribute::Enumerable;
    u8 writable = descriptor.get("writable").value_or(Value(false)).to_boolean() * Attribute::Writable;
    u8 attributes = configurable | enumerable | writable;

    dbg() << "Defining new property " << interpreter.argument(1).to_string() << " with descriptor { " << configurable << ", " << enumerable << ", " << writable << ", attributes=" << attributes << " }";

    object.put_own_property(object, interpreter.argument(1).to_string(), attributes, value, PutOwnPropertyMode::DefineProperty);
    return &object;
}

Value ObjectConstructor::is(Interpreter& interpreter)
{
    auto value1 = interpreter.argument(0);
    auto value2 = interpreter.argument(1);
    if (value1.is_nan() && value2.is_nan())
        return Value(true);
    if (value1.is_number() && value1.as_double() == 0 && value2.is_number() && value2.as_double() == 0) {
        if (value1.is_positive_zero() && value2.is_positive_zero())
            return Value(true);
        if (value1.is_negative_zero() && value2.is_negative_zero())
            return Value(true);
        return Value(false);
    }
    return typed_eq(interpreter, value1, value2);
}

}
