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

Value Object::get_enumerable_own_properties(const Object& this_object, GetOwnPropertyMode kind) const
{
    auto* properties_array = Array::create(interpreter().global_object());

    // FIXME: Support generic iterables
    if (this_object.is_string_object()) {
        auto str = static_cast<const StringObject&>(this_object).primitive_string().string();

        for (size_t i = 0; i < str.length(); ++i) {
            if (kind == GetOwnPropertyMode::Key) {
                properties_array->put_by_index(i, js_string(interpreter(), String::number(i)));
            } else if (kind == GetOwnPropertyMode::Value) {
                properties_array->put_by_index(i, js_string(interpreter(), String::format("%c", str[i])));
            } else {
                auto* entry_array = Array::create(interpreter().global_object());
                entry_array->put_by_index(0, js_string(interpreter(), String::number(i)));
                entry_array->put_by_index(1, js_string(interpreter(), String::format("%c", str[i])));
                properties_array->put_by_index(i, entry_array);
            }
        }

        return properties_array;
    }

    size_t property_index = 0;
    for (size_t i = 0; i < m_elements.size(); ++i) {
        if (m_elements.at(i).is_empty())
            continue;

        if (kind == GetOwnPropertyMode::Key) {
            properties_array->put_by_index(property_index, js_string(interpreter(), String::number(i)));
        } else if (kind == GetOwnPropertyMode::Value) {
            properties_array->put_by_index(property_index, m_elements.at(i));
        } else {
            auto* entry_array = Array::create(interpreter().global_object());
            entry_array->put_by_index(0, js_string(interpreter(), String::number(i)));
            entry_array->put_by_index(1, m_elements.at(i));
            properties_array->put_by_index(property_index, entry_array);
        }

        ++property_index;
    }

    for (auto& it : this_object.shape().property_table_ordered()) {
        if (it.value.attributes & Attribute::Enumerable) {
            size_t offset = it.value.offset + property_index;

            if (kind == GetOwnPropertyMode::Key) {
                properties_array->put_by_index(offset, js_string(interpreter(), it.key));
            } else if (kind == GetOwnPropertyMode::Value) {
                properties_array->put_by_index(offset, this_object.get(it.key));
            } else {
                auto* entry_array = Array::create(interpreter().global_object());
                entry_array->put_by_index(0, js_string(interpreter(), it.key));
                entry_array->put_by_index(1, this_object.get(it.key));
                properties_array->put_by_index(offset, entry_array);
            }
        }
    }

    return properties_array;
}

void Object::set_shape(Shape& new_shape)
{
    m_storage.resize(new_shape.property_count());
    m_shape = &new_shape;
}

bool Object::put_own_property(Object& this_object, const FlyString& property_name, u8 attributes, Value value, PutOwnPropertyMode mode)
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
        return false;
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
        return false;
    }

    if (value.is_empty())
        return true;

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
        if (is_string_object()) {
            auto& string = static_cast<const StringObject*>(this)->primitive_string().string();
            if (property_index < (i32)string.length())
                return js_string(heap(), string.substring(property_index, 1));
            return js_undefined();
        }
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

bool Object::put_by_index(i32 property_index, Value value, u8 attributes)
{
    ASSERT(!value.is_empty());
    if (property_index < 0)
        return put(String::number(property_index), value, attributes);
    // FIXME: Implement some kind of sparse storage for arrays with huge indices.
    // Also: Take attributes into account here
    if (static_cast<size_t>(property_index) >= m_elements.size())
        m_elements.resize(property_index + 1);
    m_elements[property_index] = value;
    return true;
}

bool Object::put(const FlyString& property_name, Value value, u8 attributes)
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
                return true;
            }
        }
        object = object->prototype();
    }
    return put_own_property(*this, property_name, attributes, value, PutOwnPropertyMode::Put);
}

bool Object::put(PropertyName property_name, Value value, u8 attributes)
{
    if (property_name.is_number())
        return put_by_index(property_name.as_number(), value, attributes);
    return put(property_name.as_string(), value, attributes);
}

bool Object::put_native_function(const FlyString& property_name, AK::Function<Value(Interpreter&)> native_function, i32 length, u8 attributes)
{
    auto* function = NativeFunction::create(interpreter(), interpreter().global_object(), property_name, move(native_function));
    function->put("length", Value(length), Attribute::Configurable);
    return put(property_name, function, attributes);
}

bool Object::put_native_property(const FlyString& property_name, AK::Function<Value(Interpreter&)> getter, AK::Function<void(Interpreter&, Value)> setter, u8 attributes)
{
    return put(property_name, heap().allocate<NativeProperty>(move(getter), move(setter)), attributes);
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

bool Object::has_property(const FlyString& property_name) const
{
    const Object* object = this;
    while (object) {
        if (object->has_own_property(property_name))
            return true;
        object = object->prototype();
    }
    return false;
}

bool Object::has_own_property(const FlyString& property_name) const
{
    bool ok;
    i32 property_index = property_name.to_int(ok);
    if (ok && property_index >= 0) {
        if (is_string_object())
            return property_index < (i32)static_cast<const StringObject*>(this)->primitive_string().string().length();
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
        auto& interpreter = const_cast<Object*>(this)->interpreter();
        auto to_string_result = interpreter.call(to_string_function, const_cast<Object*>(this));
        if (to_string_result.is_object())
            interpreter.throw_exception<TypeError>("Cannot convert object to string");
        if (interpreter.exception())
            return {};
        return js_string(heap(), to_string_result.to_string());
    }
    return js_string(heap(), String::format("[object %s]", class_name()));
}

}
