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
#include <LibJS/Runtime/Accessor.h>
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

bool Object::set_prototype(Object* new_prototype)
{
    if (prototype() == new_prototype)
        return true;
    if (!m_is_extensible)
        return false;
    if (shape().is_unique()) {
        shape().set_prototype_without_transition(new_prototype);
        return true;
    }
    m_shape = m_shape->create_prototype_transition(new_prototype);
    return true;
}

bool Object::has_prototype(const Object* prototype) const
{
    for (auto* object = this->prototype(); object; object = object->prototype()) {
        if (object == prototype)
            return true;
    }
    return false;
}

bool Object::prevent_extensions()
{
    m_is_extensible = false;
    return true;
}

Value Object::get_own_property(const Object& this_object, PropertyName property_name) const
{
    Value value_here;

    if (property_name.is_number()) {
        auto existing_property = m_indexed_properties.get(nullptr, property_name.as_number(), false);
        if (!existing_property.has_value())
            return {};
        value_here = existing_property.value().value;
    } else {
        auto metadata = shape().lookup(property_name.as_string());
        if (!metadata.has_value())
            return {};
        value_here = m_storage[metadata.value().offset];
    }

    ASSERT(!value_here.is_empty());
    if (value_here.is_accessor()) {
        return value_here.as_accessor().call_getter(Value(const_cast<Object*>(this)));
    }
    if (value_here.is_object() && value_here.as_object().is_native_property())
        return call_native_property_getter(const_cast<Object*>(&this_object), value_here);
    return value_here;
}

Value Object::get_own_properties(const Object& this_object, GetOwnPropertyMode kind, PropertyAttributes attributes) const
{
    auto* properties_array = Array::create(interpreter().global_object());

    // FIXME: Support generic iterables
    if (this_object.is_string_object()) {
        auto str = static_cast<const StringObject&>(this_object).primitive_string().string();

        for (size_t i = 0; i < str.length(); ++i) {
            if (kind == GetOwnPropertyMode::Key) {
                properties_array->define_property(i, js_string(interpreter(), String::number(i)));
            } else if (kind == GetOwnPropertyMode::Value) {
                properties_array->define_property(i, js_string(interpreter(), String::format("%c", str[i])));
            } else {
                auto* entry_array = Array::create(interpreter().global_object());
                entry_array->define_property(0, js_string(interpreter(), String::number(i)));
                entry_array->define_property(1, js_string(interpreter(), String::format("%c", str[i])));
                properties_array->define_property(i, entry_array);
            }
        }

        return properties_array;
    }

    size_t property_index = 0;
    for (auto& entry : m_indexed_properties) {
        if (kind == GetOwnPropertyMode::Key) {
            properties_array->define_property(property_index, js_string(interpreter(), String::number(entry.index())));
        } else if (kind == GetOwnPropertyMode::Value) {
            properties_array->define_property(property_index, entry.value_and_attributes(const_cast<Object*>(&this_object)).value);
            if (interpreter().exception())
                return {};
        } else {
            auto* entry_array = Array::create(interpreter().global_object());
            entry_array->define_property(0, js_string(interpreter(), String::number(entry.index())));
            entry_array->define_property(1, entry.value_and_attributes(const_cast<Object*>(&this_object)).value);
            if (interpreter().exception())
                return {};
            properties_array->define_property(property_index, entry_array);
        }

        ++property_index;
    }

    for (auto& it : this_object.shape().property_table_ordered()) {
        if (it.value.attributes.bits() & attributes.bits()) {
            size_t offset = it.value.offset + property_index;

            if (kind == GetOwnPropertyMode::Key) {
                properties_array->define_property(offset, js_string(interpreter(), it.key));
            } else if (kind == GetOwnPropertyMode::Value) {
                properties_array->define_property(offset, this_object.get(it.key));
                if (interpreter().exception())
                    return {};
            } else {
                auto* entry_array = Array::create(interpreter().global_object());
                entry_array->define_property(0, js_string(interpreter(), it.key));
                entry_array->define_property(1, this_object.get(it.key));
                if (interpreter().exception())
                    return {};
                properties_array->define_property(offset, entry_array);
            }
        }
    }

    return properties_array;
}

Value Object::get_own_property_descriptor(PropertyName property_name) const
{
    Value value;
    PropertyAttributes attributes;

    if (property_name.is_number()) {
        auto existing_value = m_indexed_properties.get(nullptr, property_name.as_number(), false);
        if (!existing_value.has_value())
            return js_undefined();
        value = existing_value.value().value;
        attributes = existing_value.value().attributes;
        attributes = default_attributes;
    } else {
        auto metadata = shape().lookup(property_name.as_string());
        if (!metadata.has_value())
            return js_undefined();
        value = m_storage[metadata.value().offset];
        if (interpreter().exception())
            return {};
        attributes = metadata.value().attributes;
    }

    auto* descriptor = Object::create_empty(interpreter(), interpreter().global_object());
    descriptor->define_property("enumerable", Value(attributes.is_enumerable()));
    descriptor->define_property("configurable", Value(attributes.is_configurable()));
    if (value.is_object() && value.as_object().is_native_property()) {
        auto result = call_native_property_getter(const_cast<Object*>(this), value);
        descriptor->define_property("value", result);
        descriptor->define_property("writable", Value(attributes.is_writable()));
    } else if (value.is_accessor()) {
        auto& pair = value.as_accessor();
        if (pair.getter())
            descriptor->define_property("get", pair.getter());
        if (pair.setter())
            descriptor->define_property("set", pair.setter());
    } else {
        descriptor->define_property("value", value.value_or(js_undefined()));
        descriptor->define_property("writable", Value(attributes.is_writable()));
    }
    return descriptor;
}

void Object::set_shape(Shape& new_shape)
{
    m_storage.resize(new_shape.property_count());
    m_shape = &new_shape;
}

bool Object::define_property(const FlyString& property_name, const Object& descriptor, bool throw_exceptions)
{
    bool is_accessor_property = descriptor.has_property("get") || descriptor.has_property("set");
    PropertyAttributes attributes;
    if (descriptor.has_property("configurable")) {
        if (interpreter().exception())
            return false;
        attributes.set_has_configurable();
        if (descriptor.get("configurable").value_or(Value(false)).to_boolean())
            attributes.set_configurable();
        if (interpreter().exception())
            return false;
    }
    if (descriptor.has_property("enumerable")) {
        if (interpreter().exception())
            return false;
        attributes.set_has_enumerable();
        if (descriptor.get("enumerable").value_or(Value(false)).to_boolean())
            attributes.set_enumerable();
        if (interpreter().exception())
            return false;
    }

    if (is_accessor_property) {
        if (descriptor.has_property("value") || descriptor.has_property("writable")) {
            if (throw_exceptions)
                interpreter().throw_exception<TypeError>("Accessor property descriptors cannot specify a value or writable key");
            return false;
        }

        auto getter = descriptor.get("get").value_or(js_undefined());
        if (interpreter().exception())
            return {};
        auto setter = descriptor.get("set").value_or(js_undefined());
        if (interpreter().exception())
            return {};

        Function* getter_function { nullptr };
        Function* setter_function { nullptr };

        if (getter.is_function()) {
            getter_function = &getter.as_function();
        } else if (!getter.is_undefined()) {
            interpreter().throw_exception<TypeError>("Accessor descriptor's 'get' field must be a function or undefined");
            return false;
        }

        if (setter.is_function()) {
            setter_function = &setter.as_function();
        } else if (!setter.is_undefined()) {
            interpreter().throw_exception<TypeError>("Accessor descriptor's 'set' field must be a function or undefined");
            return false;
        }

        dbg() << "Defining new property " << property_name << " with accessor descriptor { attributes=" << attributes << ", "
              << "getter=" << getter.to_string_without_side_effects() << ", "
              << "setter=" << setter.to_string_without_side_effects() << "}";

        return define_property(property_name, Accessor::create(interpreter(), getter_function, setter_function), attributes, throw_exceptions);
    }

    auto value = descriptor.get("value");
    if (interpreter().exception())
        return {};
    if (descriptor.has_property("writable")) {
        if (interpreter().exception())
            return false;
        attributes.set_has_writable();
        if (descriptor.get("writable").value_or(Value(false)).to_boolean())
            attributes.set_writable();
        if (interpreter().exception())
            return false;
    }
    if (interpreter().exception())
        return {};

    dbg() << "Defining new property " << property_name << " with data descriptor { attributes=" << attributes
          << ", value=" << (value.is_empty() ? "<empty>" : value.to_string_without_side_effects()) << " }";

    return define_property(property_name, value, attributes, throw_exceptions);
}

bool Object::define_property(PropertyName property_name, Value value, PropertyAttributes attributes, bool throw_exceptions)
{
    if (property_name.is_number())
        return put_own_property_by_index(*this, property_name.as_number(), value, attributes, PutOwnPropertyMode::DefineProperty, throw_exceptions);
    bool ok;
    i32 property_index = property_name.as_string().to_int(ok);
    if (ok && property_index >= 0)
        return put_own_property_by_index(*this, property_index, value, attributes, PutOwnPropertyMode::DefineProperty, throw_exceptions);
    return put_own_property(*this, property_name.as_string(), value, attributes, PutOwnPropertyMode::DefineProperty, throw_exceptions);
}

bool Object::put_own_property(Object& this_object, const FlyString& property_name, Value value, PropertyAttributes attributes, PutOwnPropertyMode mode, bool throw_exceptions)
{
    ASSERT(!(mode == PutOwnPropertyMode::Put && value.is_accessor()));

    if (!is_extensible()) {
        dbg() << "Disallow define_property of non-extensible object";
        if (throw_exceptions && interpreter().in_strict_mode())
            interpreter().throw_exception<TypeError>(String::format("Cannot define property %s on non-extensible object", property_name.characters()));
        return false;
    }

    if (value.is_accessor()) {
        auto& accessor = value.as_accessor();
        if (accessor.getter())
            attributes.set_has_getter();
        if (accessor.setter())
            attributes.set_has_setter();
    }

    auto metadata = shape().lookup(property_name);
    bool new_property = !metadata.has_value();

    if (new_property) {
        if (!m_shape->is_unique() && shape().property_count() > 100) {
            // If you add more than 100 properties to an object, let's stop doing
            // transitions to avoid filling up the heap with shapes.
            ensure_shape_is_unique();
        }

        if (m_shape->is_unique()) {
            m_shape->add_property_to_unique_shape(property_name, attributes);
            m_storage.resize(m_shape->property_count());
        } else {
            set_shape(*m_shape->create_put_transition(property_name, attributes));
        }
        metadata = shape().lookup(property_name);
        ASSERT(metadata.has_value());
    }

    if (!new_property && mode == PutOwnPropertyMode::DefineProperty && !metadata.value().attributes.is_configurable() && attributes != metadata.value().attributes) {
        dbg() << "Disallow reconfig of non-configurable property";
        if (throw_exceptions)
            interpreter().throw_exception<TypeError>(String::format("Cannot change attributes of non-configurable property '%s'", property_name.characters()));
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

    auto value_here = m_storage[metadata.value().offset];
    if (!new_property && mode == PutOwnPropertyMode::Put && !value_here.is_accessor() && !metadata.value().attributes.is_writable()) {
        dbg() << "Disallow write to non-writable property";
        return false;
    }

    if (value.is_empty())
        return true;

    if (value_here.is_object() && value_here.as_object().is_native_property()) {
        call_native_property_setter(const_cast<Object*>(&this_object), value_here, value);
    } else {
        m_storage[metadata.value().offset] = value;
    }
    return true;
}

bool Object::put_own_property_by_index(Object& this_object, u32 property_index, Value value, PropertyAttributes attributes, PutOwnPropertyMode mode, bool throw_exceptions)
{
    ASSERT(!(mode == PutOwnPropertyMode::Put && value.is_accessor()));

    if (!is_extensible()) {
        dbg() << "Disallow define_property of non-extensible object";
        if (throw_exceptions && interpreter().in_strict_mode())
            interpreter().throw_exception<TypeError>(String::format("Cannot define property %d on non-extensible object", property_index));
        return false;
    }

    if (value.is_accessor()) {
        auto& accessor = value.as_accessor();
        if (accessor.getter())
            attributes.set_has_getter();
        if (accessor.setter())
            attributes.set_has_setter();
    }

    auto existing_property = m_indexed_properties.get(nullptr, property_index, false);
    auto new_property = !existing_property.has_value();
    PropertyAttributes existing_attributes = new_property ? 0 : existing_property.value().attributes;

    if (!new_property && mode == PutOwnPropertyMode::DefineProperty && !existing_attributes.is_configurable() && attributes != existing_attributes) {
        dbg() << "Disallow reconfig of non-configurable property";
        if (throw_exceptions)
            interpreter().throw_exception<TypeError>(String::format("Cannot change attributes of non-configurable property %d", property_index));
        return false;
    }

    auto value_here = new_property ? Value() : existing_property.value().value;
    if (!new_property && mode == PutOwnPropertyMode::Put && !value_here.is_accessor() && !existing_attributes.is_writable()) {
        dbg() << "Disallow write to non-writable property";
        return false;
    }

    if (value.is_empty())
        return true;

    if (value_here.is_object() && value_here.as_object().is_native_property()) {
        call_native_property_setter(const_cast<Object*>(&this_object), value_here, value);
    } else {
        m_indexed_properties.put(&this_object, property_index, value, attributes, mode == PutOwnPropertyMode::Put);
    }
    return true;
}

Value Object::delete_property(PropertyName property_name)
{
    ASSERT(property_name.is_valid());
    if (property_name.is_number())
        return Value(m_indexed_properties.remove(property_name.as_number()));
    auto metadata = shape().lookup(property_name.as_string());
    if (!metadata.has_value())
        return Value(true);
    if (!metadata.value().attributes.is_configurable())
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

Value Object::get_by_index(u32 property_index) const
{
    const Object* object = this;
    while (object) {
        if (is_string_object()) {
            auto& string = static_cast<const StringObject*>(this)->primitive_string().string();
            if (property_index < string.length())
                return js_string(heap(), string.substring(property_index, 1));
            return js_undefined();
        }
        if (static_cast<size_t>(property_index) < object->m_indexed_properties.array_like_size()) {
            auto result = object->m_indexed_properties.get(const_cast<Object*>(this), property_index);
            if (interpreter().exception())
                return {};
            if (result.has_value() && !result.value().value.is_empty())
                return result.value().value;
            return {};
        }
        object = object->prototype();
    }
    return {};
}

Value Object::get(PropertyName property_name) const
{
    if (property_name.is_number())
        return get_by_index(property_name.as_number());

    auto property_string = property_name.to_string();
    bool ok;
    i32 property_index = property_string.to_int(ok);
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

bool Object::put_by_index(u32 property_index, Value value)
{
    ASSERT(!value.is_empty());

    // If there's a setter in the prototype chain, we go to the setter.
    // Otherwise, it goes in the own property storage.
    Object* object = this;
    while (object) {
        auto existing_value = object->m_indexed_properties.get(nullptr, property_index, false);
        if (existing_value.has_value()) {
            auto value_here = existing_value.value();
            if (value_here.value.is_accessor()) {
                value_here.value.as_accessor().call_setter(object, value);
                return true;
            }
            if (value_here.value.is_object() && value_here.value.as_object().is_native_property()) {
                call_native_property_setter(const_cast<Object*>(this), value_here.value, value);
                return true;
            }
        }
        object = object->prototype();
    }
    return put_own_property_by_index(*this, property_index, value, default_attributes, PutOwnPropertyMode::Put);
}

bool Object::put(PropertyName property_name, Value value)
{
    if (property_name.is_number())
        return put_by_index(property_name.as_number(), value);

    ASSERT(!value.is_empty());

    auto property_string = property_name.to_string();
    bool ok;
    i32 property_index = property_string.to_int(ok);
    if (ok && property_index >= 0)
        return put_by_index(property_index, value);

    // If there's a setter in the prototype chain, we go to the setter.
    // Otherwise, it goes in the own property storage.
    Object* object = this;
    while (object) {
        auto metadata = object->shape().lookup(property_string);
        if (metadata.has_value()) {
            auto value_here = object->m_storage[metadata.value().offset];
            if (value_here.is_accessor()) {
                value_here.as_accessor().call_setter(Value(this), value);
                return true;
            }
            if (value_here.is_object() && value_here.as_object().is_native_property()) {
                call_native_property_setter(const_cast<Object*>(this), value_here, value);
                return true;
            }
        }
        object = object->prototype();
    }
    return put_own_property(*this, property_string, value, default_attributes, PutOwnPropertyMode::Put);
}

bool Object::define_native_function(const FlyString& property_name, AK::Function<Value(Interpreter&)> native_function, i32 length, PropertyAttributes attribute)
{
    auto* function = NativeFunction::create(interpreter(), interpreter().global_object(), property_name, move(native_function));
    function->define_property("length", Value(length), Attribute::Configurable);
    function->define_property("name", js_string(heap(), property_name), Attribute::Configurable);
    return define_property(property_name, function, attribute);
}

bool Object::define_native_property(const FlyString& property_name, AK::Function<Value(Interpreter&)> getter, AK::Function<void(Interpreter&, Value)> setter, PropertyAttributes attribute)
{
    return define_property(property_name, heap().allocate<NativeProperty>(move(getter), move(setter)), attribute);
}

void Object::visit_children(Cell::Visitor& visitor)
{
    Cell::visit_children(visitor);
    visitor.visit(m_shape);

    for (auto& value : m_storage)
        visitor.visit(value);

    for (auto& value : m_indexed_properties.values_unordered())
        visitor.visit(value.value);
}

bool Object::has_property(PropertyName property_name) const
{
    const Object* object = this;
    while (object) {
        if (object->has_own_property(property_name))
            return true;
        object = object->prototype();
    }
    return false;
}

bool Object::has_own_property(PropertyName property_name) const
{
    auto has_indexed_property = [&](u32 index) -> bool {
        if (is_string_object())
            return index < static_cast<const StringObject*>(this)->primitive_string().string().length();
        return m_indexed_properties.has_index(index);
    };

    if (property_name.is_number())
        return has_indexed_property(property_name.as_number());

    bool ok;
    i32 property_index = property_name.as_string().to_int(ok);
    if (ok && property_index >= 0)
        return has_indexed_property(property_index);

    return shape().lookup(property_name.as_string()).has_value();
}

Value Object::to_primitive(Value::PreferredType preferred_type) const
{
    Value result = js_undefined();

    switch (preferred_type) {
    case Value::PreferredType::Default:
    case Value::PreferredType::Number: {
        result = value_of();
        if (result.is_object()) {
            result = to_string();
        }
        break;
    }
    case Value::PreferredType::String: {
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
    if (to_string_property.is_function()) {
        auto& to_string_function = to_string_property.as_function();
        auto& interpreter = const_cast<Object*>(this)->interpreter();
        auto to_string_result = interpreter.call(to_string_function, const_cast<Object*>(this));
        if (to_string_result.is_object())
            interpreter.throw_exception<TypeError>("Cannot convert object to string");
        if (interpreter.exception())
            return {};
        auto* string = to_string_result.to_primitive_string(interpreter);
        if (interpreter.exception())
            return {};
        return string;
    }
    return js_string(heap(), String::format("[object %s]", class_name()));
}

Value Object::invoke(const FlyString& property_name, Optional<MarkedValueList> arguments)
{
    auto& interpreter = this->interpreter();
    auto property = get(property_name).value_or(js_undefined());
    if (interpreter.exception())
        return {};
    if (!property.is_function()) {
        interpreter.throw_exception<TypeError>(String::format("%s is not a function", property.to_string_without_side_effects().characters()));
        return {};
    }
    return interpreter.call(property.as_function(), this, move(arguments));
}

Value Object::call_native_property_getter(Object* this_object, Value property) const
{
    ASSERT(property.is_object());
    ASSERT(property.as_object().is_native_property());
    auto& native_property = static_cast<NativeProperty&>(property.as_object());
    auto& call_frame = interpreter().push_call_frame();
    call_frame.this_value = this_object;
    auto result = native_property.get(interpreter());
    interpreter().pop_call_frame();
    return result;
}

void Object::call_native_property_setter(Object* this_object, Value property, Value value) const
{
    ASSERT(property.is_object());
    ASSERT(property.as_object().is_native_property());
    auto& native_property = static_cast<NativeProperty&>(property.as_object());
    auto& call_frame = interpreter().push_call_frame();
    call_frame.this_value = this_object;
    native_property.set(interpreter(), value);
    interpreter().pop_call_frame();
}

}
