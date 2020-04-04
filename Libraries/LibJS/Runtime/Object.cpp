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
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NativeProperty.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

Object::Object()
{
    m_shape = interpreter().empty_object_shape();
    m_shape->set_prototype_without_transition(interpreter().object_prototype());
}

Object::~Object()
{
}

Object* Object::prototype()
{
    return shape().prototype();
}

const Object* Object::prototype() const
{
    return shape().prototype();
}

void Object::set_prototype(Object* new_prototype)
{
    m_shape = m_shape->create_prototype_transition(new_prototype);
}

bool Object::has_prototype(const Object* prototype) const
{
    for (auto* object = this->prototype(); object; object = object->prototype()) {
        if (object == prototype)
            return true;
    }
    return false;
}

Optional<Value> Object::get_own_property(const Object& this_object, const FlyString& property_name) const
{
    auto metadata = shape().lookup(property_name);
    if (!metadata.has_value())
        return {};

    auto value_here = m_storage[metadata.value().offset];
    if (value_here.is_object() && value_here.as_object().is_native_property()) {
        auto& native_property = static_cast<const NativeProperty&>(value_here.as_object());
        auto& interpreter = const_cast<Object*>(this)->interpreter();
        auto& call_frame = interpreter.push_call_frame();
        call_frame.this_value = const_cast<Object*>(&this_object);
        auto result = native_property.get(interpreter);
        interpreter.pop_call_frame();
        return result;
    }
    return value_here;
}

void Object::set_shape(Shape& new_shape)
{
    m_storage.resize(new_shape.property_count());
    m_shape = &new_shape;
}

bool Object::put_own_property(Object& this_object, const FlyString& property_name, Value value)
{
    auto metadata = shape().lookup(property_name);
    if (!metadata.has_value()) {
        auto* new_shape = m_shape->create_put_transition(property_name, 0);
        set_shape(*new_shape);
        metadata = shape().lookup(property_name);
        ASSERT(metadata.has_value());
    }

    auto value_here = m_storage[metadata.value().offset];
    if (value_here.is_object() && value_here.as_object().is_native_property()) {
        auto& native_property = static_cast<NativeProperty&>(value_here.as_object());
        auto& interpreter = const_cast<Object*>(this)->interpreter();
        auto& call_frame = interpreter.push_call_frame();
        call_frame.this_value = &this_object;
        native_property.set(interpreter, value);
        interpreter.pop_call_frame();
    } else {
        m_storage[metadata.value().offset] = value;
    }
    return true;
}

Optional<Value> Object::get(const FlyString& property_name) const
{
    const Object* object = this;
    while (object) {
        auto value = object->get_own_property(*this, property_name);
        if (value.has_value())
            return value.value();
        object = object->prototype();
    }
    return {};
}

void Object::put(const FlyString& property_name, Value value)
{
    // If there's a setter in the prototype chain, we go to the setter.
    // Otherwise, it goes in the own property storage.
    Object* object = this;
    while (object) {
        auto metadata = object->shape().lookup(property_name);
        if (metadata.has_value()) {
            auto value_here = object->m_storage[metadata.value().offset];
            if (value_here.is_object() && value_here.as_object().is_native_property()) {
                auto& native_property = static_cast<NativeProperty&>(value_here.as_object());
                auto& interpreter = const_cast<Object*>(this)->interpreter();
                auto& call_frame = interpreter.push_call_frame();
                call_frame.this_value = this;
                native_property.set(interpreter, value);
                interpreter.pop_call_frame();
                return;
            }
        }
        object = object->prototype();
    }
    put_own_property(*this, property_name, value);
}

void Object::put_native_function(const FlyString& property_name, AK::Function<Value(Interpreter&)> native_function, i32 length)
{
    auto* function = heap().allocate<NativeFunction>(move(native_function));
    function->put("length", Value(length));
    put(property_name, function);
}

void Object::put_native_property(const FlyString& property_name, AK::Function<Value(Interpreter&)> getter, AK::Function<void(Interpreter&, Value)> setter)
{
    put(property_name, heap().allocate<NativeProperty>(move(getter), move(setter)));
}

void Object::visit_children(Cell::Visitor& visitor)
{
    Cell::visit_children(visitor);
    visitor.visit(m_shape);
}

bool Object::has_own_property(const FlyString& property_name) const
{
    return shape().lookup(property_name).has_value();
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
