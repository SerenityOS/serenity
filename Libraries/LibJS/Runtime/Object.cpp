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
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NativeProperty.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

Object* Object::create_empty(Interpreter&, GlobalObject& global_object)
{
    return global_object.heap().allocate<Object>(global_object.object_prototype());
}

Object::Object(Object* prototype)
{
    if (prototype) {
        m_shape = interpreter().global_object().empty_object_shape();
        set_prototype(prototype);
    } else {
        m_shape = interpreter().heap().allocate<Shape>();
    }
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
    if (prototype() == new_prototype)
        return;
    if (shape().is_unique()) {
        shape().set_prototype_without_transition(new_prototype);
        return;
    }
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

Value Object::get_own_property(const Object& this_object, const FlyString& property_name) const
{
    auto metadata = shape().lookup(property_name);
    if (!metadata.has_value())
        return {};

    auto value_here = m_storage[metadata.value().offset];
    ASSERT(!value_here.is_empty());
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

void Object::put_own_property(Object& this_object, const FlyString& property_name, u8 attributes, Value value, PutOwnPropertyMode mode)
{
    auto metadata = shape().lookup(property_name);
    bool new_property = !metadata.has_value();

    if (new_property) {
        if (m_shape->is_unique()) {
            m_shape->add_property_to_unique_shape(property_name, attributes);
            m_storage.resize(m_shape->property_count());
        } else {
            set_shape(*m_shape->create_put_transition(property_name, attributes));
        }
        metadata = shape().lookup(property_name);
        ASSERT(metadata.has_value());
    }

    if (!new_property && mode == PutOwnPropertyMode::DefineProperty && !(metadata.value().attributes & Attribute::Configurable) && attributes != metadata.value().attributes) {
        dbg() << "Disallow reconfig of non-configurable property";
        interpreter().throw_exception<TypeError>(String::format("Cannot redefine property '%s'", property_name.characters()));
        return;
    }

    if (mode == PutOwnPropertyMode::DefineProperty && attributes != metadata.value().attributes) {
        if (m_shape->is_unique()) {
            m_shape->reconfigure_property_in_unique_shape(property_name, attributes);
        } else {
            set_shape(*m_shape->create_configure_transition(property_name, attributes));
        }
        metadata = shape().lookup(property_name);

        dbg() << "Reconfigured property " << property_name << ", new shape says offset is " << metadata.value().offset << " and my storage capacity is " << m_storage.size();
    }

    if (!new_property && mode == PutOwnPropertyMode::Put && !(metadata.value().attributes & Attribute::Writable)) {
        dbg() << "Disallow write to non-writable property";
        return;
    }

    if (value.is_empty())
        return;

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
}

Value Object::delete_property(PropertyName property_name)
{
    ASSERT(property_name.is_valid());
    if (property_name.is_number()) {
        if (property_name.as_number() < static_cast<i32>(elements().size())) {
            elements()[property_name.as_number()] = {};
            return Value(true);
        }
        return Value(true);
    }
    auto metadata = shape().lookup(property_name.as_string());
    if (!metadata.has_value())
        return Value(true);
    if (!(metadata.value().attributes & Attribute::Configurable))
        return Value(false);

    size_t deleted_offset = metadata.value().offset;

    ensure_shape_is_unique();

    shape().remove_property_from_unique_shape(property_name.as_string(), deleted_offset);
    m_storage.remove(deleted_offset);
    return Value(true);
}

void Object::ensure_shape_is_unique()
{
    if (shape().is_unique())
        return;

    m_shape = m_shape->create_unique_clone();
}

Value Object::get_by_index(i32 property_index) const
{
    if (property_index < 0)
        return get(String::number(property_index));

    const Object* object = this;
    while (object) {
        if (static_cast<size_t>(property_index) < object->m_elements.size()) {
            auto value = object->m_elements[property_index];
            if (value.is_empty())
                return {};
            return value;
        }
        object = object->prototype();
    }
    return {};
}

Value Object::get(const FlyString& property_name) const
{
    bool ok;
    i32 property_index = property_name.to_int(ok);
    if (ok && property_index >= 0)
        return get_by_index(property_index);

    const Object* object = this;
    while (object) {
        auto value = object->get_own_property(*this, property_name);
        if (!value.is_empty())
            return value;
        object = object->prototype();
    }
    return {};
}

Value Object::get(PropertyName property_name) const
{
    if (property_name.is_number())
        return get_by_index(property_name.as_number());
    return get(property_name.as_string());
}

void Object::put_by_index(i32 property_index, Value value, u8 attributes)
{
    ASSERT(!value.is_empty());
    if (property_index < 0)
        return put(String::number(property_index), value, attributes);
    // FIXME: Implement some kind of sparse storage for arrays with huge indices.
    // Also: Take attributes into account here
    if (static_cast<size_t>(property_index) >= m_elements.size())
        m_elements.resize(property_index + 1);
    m_elements[property_index] = value;
}

void Object::put(const FlyString& property_name, Value value, u8 attributes)
{
    ASSERT(!value.is_empty());
    bool ok;
    i32 property_index = property_name.to_int(ok);
    if (ok && property_index >= 0)
        return put_by_index(property_index, value, attributes);

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
    put_own_property(*this, property_name, attributes, value, PutOwnPropertyMode::Put);
}

void Object::put(PropertyName property_name, Value value, u8 attributes)
{
    if (property_name.is_number())
        return put_by_index(property_name.as_number(), value, attributes);
    return put(property_name.as_string(), value, attributes);
}

void Object::put_native_function(const FlyString& property_name, AK::Function<Value(Interpreter&)> native_function, i32 length, u8 attributes)
{
    auto* function = NativeFunction::create(interpreter(), interpreter().global_object(), property_name, move(native_function));
    function->put("length", Value(length), Attribute::Configurable);
    put(property_name, function, attributes);
}

void Object::put_native_property(const FlyString& property_name, AK::Function<Value(Interpreter&)> getter, AK::Function<void(Interpreter&, Value)> setter, u8 attributes)
{
    put(property_name, heap().allocate<NativeProperty>(move(getter), move(setter)), attributes);
}

void Object::visit_children(Cell::Visitor& visitor)
{
    Cell::visit_children(visitor);
    visitor.visit(m_shape);

    for (auto& value : m_storage)
        visitor.visit(value);

    for (auto& value : m_elements)
        visitor.visit(value);
}

bool Object::has_own_property(const FlyString& property_name) const
{
    bool ok;
    i32 property_index = property_name.to_int(ok);
    if (ok && property_index >= 0) {
        if (static_cast<size_t>(property_index) >= m_elements.size())
            return false;
        return !m_elements[property_index].is_empty();
    }
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
    auto to_string_property = get("toString");
    if (!to_string_property.is_empty()
        && to_string_property.is_object()
        && to_string_property.as_object().is_function()) {
        auto& to_string_function = static_cast<Function&>(to_string_property.as_object());
        return const_cast<Object*>(this)->interpreter().call(&to_string_function, const_cast<Object*>(this));
    }
    return js_string(heap(), String::format("[object %s]", class_name()));
}

}
