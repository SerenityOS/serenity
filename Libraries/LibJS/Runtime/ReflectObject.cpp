/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ReflectObject.h>

namespace JS {

static Object* get_target_object_from(Interpreter& interpreter, const String& name)
{
    auto target = interpreter.argument(0);
    if (!target.is_object()) {
        interpreter.throw_exception<TypeError>(String::format("First argument of Reflect.%s() must be an object", name.characters()));
        return nullptr;
    }
    return static_cast<Object*>(&target.as_object());
}

static Function* get_target_function_from(Interpreter& interpreter, const String& name)
{
    auto target = interpreter.argument(0);
    if (!target.is_function()) {
        interpreter.throw_exception<TypeError>(String::format("First argument of Reflect.%s() must be a function", name.characters()));
        return nullptr;
    }
    return &target.as_function();
}

static void prepare_arguments_list(Interpreter& interpreter, Value value, MarkedValueList* arguments)
{
    if (!value.is_object()) {
        interpreter.throw_exception<TypeError>("Arguments list must be an object");
        return;
    }
    auto& arguments_list = value.as_object();
    auto length_property = arguments_list.get("length");
    if (interpreter.exception())
        return;
    auto length = length_property.to_size_t(interpreter);
    if (interpreter.exception())
        return;
    for (size_t i = 0; i < length; ++i) {
        auto element = arguments_list.get(String::number(i));
        if (interpreter.exception())
            return;
        arguments->append(element.value_or(js_undefined()));
    }
}

ReflectObject::ReflectObject()
    : Object(interpreter().global_object().object_prototype())
{
    u8 attr = Attribute::Writable | Attribute::Configurable;
    put_native_function("apply", apply, 3, attr);
    put_native_function("construct", construct, 2, attr);
    put_native_function("defineProperty", define_property, 3, attr);
    put_native_function("deleteProperty", delete_property, 2, attr);
    put_native_function("get", get, 2, attr);
    put_native_function("getOwnPropertyDescriptor", get_own_property_descriptor, 2, attr);
    put_native_function("getPrototypeOf", get_prototype_of, 1, attr);
    put_native_function("has", has, 2, attr);
    put_native_function("isExtensible", is_extensible, 1, attr);
    put_native_function("ownKeys", own_keys, 1, attr);
    put_native_function("preventExtensions", prevent_extensions, 1, attr);
    put_native_function("set", set, 3, attr);
    put_native_function("setPrototypeOf", set_prototype_of, 2, attr);
}

ReflectObject::~ReflectObject()
{
}

Value ReflectObject::apply(Interpreter& interpreter)
{
    auto* target = get_target_function_from(interpreter, "apply");
    if (!target)
        return {};
    auto this_arg = interpreter.argument(1);
    MarkedValueList arguments(interpreter.heap());
    prepare_arguments_list(interpreter, interpreter.argument(2), &arguments);
    if (interpreter.exception())
        return {};
    return interpreter.call(*target, this_arg, move(arguments));
}

Value ReflectObject::construct(Interpreter& interpreter)
{
    auto* target = get_target_function_from(interpreter, "construct");
    if (!target)
        return {};
    MarkedValueList arguments(interpreter.heap());
    prepare_arguments_list(interpreter, interpreter.argument(1), &arguments);
    if (interpreter.exception())
        return {};
    auto* new_target = target;
    if (interpreter.argument_count() > 2) {
        auto new_target_value = interpreter.argument(2);
        if (!new_target_value.is_function()
            || (new_target_value.as_object().is_native_function() && !static_cast<NativeFunction&>(new_target_value.as_object()).has_constructor())) {
            interpreter.throw_exception<TypeError>("Optional third argument of Reflect.construct() must be a constructor");
            return {};
        }
        new_target = &new_target_value.as_function();
    }
    return interpreter.construct(*target, *new_target, move(arguments));
}

Value ReflectObject::define_property(Interpreter& interpreter)
{
    auto* target = get_target_object_from(interpreter, "defineProperty");
    if (!target)
        return {};
    if (!interpreter.argument(2).is_object())
        return interpreter.throw_exception<TypeError>("Descriptor argument is not an object");
    auto property_key = interpreter.argument(1).to_string(interpreter);
    if (interpreter.exception())
        return {};
    auto& descriptor = interpreter.argument(2).as_object();
    auto success = target->define_property(property_key, descriptor, false);
    return Value(success);
}

Value ReflectObject::delete_property(Interpreter& interpreter)
{
    auto* target = get_target_object_from(interpreter, "deleteProperty");
    if (!target)
        return {};

    auto property_key = interpreter.argument(1);
    auto property_name = PropertyName(property_key.to_string(interpreter));
    if (interpreter.exception())
        return {};
    auto property_key_number = property_key.to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (property_key_number.is_finite_number()) {
        auto property_key_as_double = property_key_number.as_double();
        if (property_key_as_double >= 0 && (i32)property_key_as_double == property_key_as_double)
            property_name = PropertyName(property_key_as_double);
    }
    return target->delete_property(property_name);
}

Value ReflectObject::get(Interpreter& interpreter)
{
    // FIXME: There's a third argument, receiver, for getters - use it once we have those.
    auto* target = get_target_object_from(interpreter, "get");
    if (!target)
        return {};
    auto property_key = interpreter.argument(1).to_string(interpreter);
    if (interpreter.exception())
        return {};
    return target->get(property_key).value_or(js_undefined());
}

Value ReflectObject::get_own_property_descriptor(Interpreter& interpreter)
{
    auto* target = get_target_object_from(interpreter, "getOwnPropertyDescriptor");
    if (!target)
        return {};
    auto property_key = interpreter.argument(1).to_string(interpreter);
    if (interpreter.exception())
        return {};
    return target->get_own_property_descriptor(property_key);
}

Value ReflectObject::get_prototype_of(Interpreter& interpreter)
{
    auto* target = get_target_object_from(interpreter, "getPrototypeOf");
    if (!target)
        return {};
    return target->prototype();
}

Value ReflectObject::has(Interpreter& interpreter)
{
    auto* target = get_target_object_from(interpreter, "has");
    if (!target)
        return {};
    auto property_key = interpreter.argument(1).to_string(interpreter);
    if (interpreter.exception())
        return {};
    return Value(target->has_property(property_key));
}

Value ReflectObject::is_extensible(Interpreter&)
{
    // FIXME: For this to be useful we need one of these:
    // Object.seal(), Object.freeze(), Reflect.preventExtensions()
    // For now we just return true, as that's always the case.
    return Value(true);
}

Value ReflectObject::own_keys(Interpreter& interpreter)
{
    auto* target = get_target_object_from(interpreter, "ownKeys");
    if (!target)
        return {};
    return target->get_own_properties(*target, GetOwnPropertyMode::Key);
}

Value ReflectObject::prevent_extensions(Interpreter&)
{
    // FIXME: Implement me :^)
    ASSERT_NOT_REACHED();
}

Value ReflectObject::set(Interpreter& interpreter)
{
    // FIXME: There's a fourth argument, receiver, for setters - use it once we have those.
    auto* target = get_target_object_from(interpreter, "set");
    if (!target)
        return {};
    auto property_key = interpreter.argument(1).to_string(interpreter);
    if (interpreter.exception())
        return {};
    auto value = interpreter.argument(2);
    return Value(target->put(property_key, value));
}

Value ReflectObject::set_prototype_of(Interpreter& interpreter)
{
    auto* target = get_target_object_from(interpreter, "setPrototypeOf");
    if (!target)
        return {};
    auto prototype_value = interpreter.argument(1);
    if (!prototype_value.is_object() && !prototype_value.is_null()) {
        interpreter.throw_exception<TypeError>("Prototype must be an object or null");
        return {};
    }
    Object* prototype = nullptr;
    if (!prototype_value.is_null())
        prototype = const_cast<Object*>(&prototype_value.as_object());
    target->set_prototype(prototype);
    // FIXME: Needs to return false for prototype chain cycles and non-extensible objects (don't have those yet).
    return Value(true);
}

}
