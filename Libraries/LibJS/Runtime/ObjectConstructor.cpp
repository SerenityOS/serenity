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
#include <LibJS/Runtime/ObjectConstructor.h>

namespace JS {

ObjectConstructor::ObjectConstructor()
{
    put("prototype", interpreter().object_prototype());

    put_native_function("getOwnPropertyNames", get_own_property_names);
    put_native_function("getPrototypeOf", get_prototype_of);
    put_native_function("setPrototypeOf", set_prototype_of);
}

ObjectConstructor::~ObjectConstructor()
{
}

Value ObjectConstructor::call(Interpreter& interpreter)
{
    return interpreter.heap().allocate<Object>();
}

Value ObjectConstructor::get_own_property_names(Interpreter& interpreter)
{
    if (interpreter.call_frame().arguments.size() < 1)
        return {};
    auto* object = interpreter.call_frame().arguments[0].to_object(interpreter.heap());
    if (interpreter.exception())
        return {};
    auto* result = interpreter.heap().allocate<Array>();
    if (object->is_array()) {
        auto* array = static_cast<const Array*>(object);
        for (i32 i = 0; i < array->length(); ++i)
            result->push(js_string(interpreter.heap(), String::number(i)));
    }
    for (auto& it : object->own_properties())
        result->push(js_string(interpreter.heap(), it.key));
    return result;
}

Value ObjectConstructor::get_prototype_of(Interpreter& interpreter)
{
    if (interpreter.call_frame().arguments.size() < 1)
        return {};
    auto* object = interpreter.call_frame().arguments[0].to_object(interpreter.heap());
    if (interpreter.exception())
        return {};
    return object->prototype();
}

Value ObjectConstructor::set_prototype_of(Interpreter& interpreter)
{
    if (interpreter.call_frame().arguments.size() < 2)
        return {};
    if (!interpreter.call_frame().arguments[1].is_object())
        return {};
    auto* object = interpreter.call_frame().arguments[0].to_object(interpreter.heap());
    if (interpreter.exception())
        return {};
    object->set_prototype(const_cast<Object*>(interpreter.call_frame().arguments[1].as_object()));
    return {};
}

}
