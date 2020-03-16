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

#include <AK/String.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NativeProperty.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

Object::Object()
{
    set_prototype(interpreter().object_prototype());
}

Object::~Object()
{
}

Value Object::get(String property_name) const
{
    const Object* object = this;
    while (object) {
        auto value = object->m_properties.get(property_name);
        if (value.has_value()) {
            if (value.value().is_object() && value.value().as_object()->is_native_property())
                return static_cast<const NativeProperty*>(value.value().as_object())->get(const_cast<Object*>(this));
            return value.value();
        }
        object = object->prototype();
    }
    return js_undefined();
}

void Object::put(String property_name, Value value)
{
    m_properties.set(property_name, move(value));
}

void Object::put_native_function(String property_name, AK::Function<Value(Object*, Vector<Value>)> native_function)
{
    put(property_name, heap().allocate<NativeFunction>(move(native_function)));
}

void Object::put_native_property(String property_name, AK::Function<Value(Object*)> getter, AK::Function<void(Object*, Value)> setter)
{
    put(property_name, heap().allocate<NativeProperty>(move(getter), move(setter)));
}

void Object::visit_children(Cell::Visitor& visitor)
{
    Cell::visit_children(visitor);
    if (m_prototype)
        visitor.visit(m_prototype);
    for (auto& it : m_properties)
        visitor.visit(it.value);
}

bool Object::has_own_property(const String& property_name) const
{
    return m_properties.get(property_name).has_value();
}

Value Object::to_primitive(PreferredType preferred_type) const
{
    Value result = js_undefined();

    switch (preferred_type) {
    case PreferredType::Default:
    case PreferredType::Number: {
        result = value_of();
        if (result.is_object()) {
            result = to_string();
        }
        break;
    }
    case PreferredType::String: {
        result = to_string();
        if (result.is_object())
            result = value_of();
        break;
    }
    }

    ASSERT(!result.is_object());
    return result;
}

Value Object::to_string() const
{
    return js_string(heap(), String::format("[object %s]", class_name()));
}

}
