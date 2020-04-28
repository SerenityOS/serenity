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
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Uint8ClampedArray.h>

namespace JS {

Uint8ClampedArray* Uint8ClampedArray::create(GlobalObject& global_object, i32 length)
{
    ASSERT(length >= 0);
    auto& interpreter = global_object.interpreter();
    return interpreter.heap().allocate<Uint8ClampedArray>(length, *global_object.array_prototype());
}

Uint8ClampedArray::Uint8ClampedArray(i32 length, Object& prototype)
    : Object(&prototype)
    , m_length(length)
{
    ASSERT(m_length >= 0);
    put_native_property("length", length_getter, nullptr);
    m_data = new u8[m_length];
}

Uint8ClampedArray::~Uint8ClampedArray()
{
    ASSERT(m_data);
    delete[] m_data;
    m_data = nullptr;
}

Value Uint8ClampedArray::length_getter(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (StringView(this_object->class_name()) != "Uint8ClampedArray")
        return interpreter.throw_exception<TypeError>("Not a Uint8ClampedArray");
    return Value(static_cast<const Uint8ClampedArray*>(this_object)->length());
}

void Uint8ClampedArray::put_by_index(i32 property_index, Value value, u8)
{
    // FIXME: Use attributes
    ASSERT(property_index >= 0);
    ASSERT(property_index < m_length);
    m_data[property_index] = clamp(value.to_i32(), 0, 255);
}

Value Uint8ClampedArray::get_by_index(i32 property_index) const
{
    ASSERT(property_index >= 0);
    ASSERT(property_index < m_length);
    return Value((i32)m_data[property_index]);
}

}
