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
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Array* Array::create(GlobalObject& global_object)
{
    auto& interpreter = global_object.interpreter();
    return interpreter.heap().allocate<Array>(*global_object.array_prototype());
}

Array::Array(Object& prototype)
    : Object(&prototype)
{
    put_native_property("length", length_getter, length_setter, Attribute::Configurable | Attribute::Writable);
}

Array::~Array()
{
}

Array* array_from(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!this_object->is_array()) {
        interpreter.throw_exception<TypeError>("Not an Array");
        return nullptr;
    }
    return static_cast<Array*>(this_object);
}

Value Array::length_getter(Interpreter& interpreter)
{
    auto* array = array_from(interpreter);
    if (!array)
        return {};
    return Value(array->length());
}

void Array::length_setter(Interpreter& interpreter, Value value)
{
    auto* array = array_from(interpreter);
    if (!array)
        return;
    auto length = value.to_number();
    if (length.is_nan() || length.is_infinity() || length.as_double() < 0) {
        interpreter.throw_exception<RangeError>("Invalid array length");
        return;
    }
    array->elements().resize(length.as_double());
}

}
