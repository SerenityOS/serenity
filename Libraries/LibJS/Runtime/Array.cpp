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

namespace JS {

Array::Array()
{
    set_prototype(interpreter().array_prototype());
    put_native_property(
        "length",
        [this](Object*) {
            return Value(length());
        },
        [](Object*, Value) {
            ASSERT_NOT_REACHED();
        });
}

Array::~Array()
{
}

void Array::push(Value value)
{
    m_elements.append(value);
}

void Array::visit_children(Cell::Visitor& visitor)
{
    Object::visit_children(visitor);
    for (auto& element : m_elements)
        visitor.visit(element);
}

Optional<Value> Array::get_own_property(const FlyString& property_name) const
{
    bool ok;
    i32 index = property_name.to_int(ok);
    if (ok) {
        if (index >= 0 && index < length())
            return m_elements[index];
    }
    return Object::get_own_property(property_name);
}

bool Array::put_own_property(const FlyString& property_name, Value value)
{
    bool ok;
    i32 index = property_name.to_int(ok);
    if (ok && index >= 0) {
        if (index >= length())
            m_elements.resize(index + 1);
        m_elements[index] = value;
        return true;
    }
    return Object::put_own_property(property_name, value);
}

}
