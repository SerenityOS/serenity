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

#include <AK/Debug.h>
#include <AK/String.h>
#include <AK/TemporaryChange.h>
#include <LibJS/Heap/Heap.h>
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

PropertyDescriptor PropertyDescriptor::from_dictionary(VM& vm, const Object& object)
{
    PropertyAttributes attributes;
    if (object.has_property(vm.names.configurable)) {
        attributes.set_has_configurable();
        if (object.get(vm.names.configurable).value_or(Value(false)).to_boolean())
            attributes.set_configurable();
        if (vm.exception())
            return {};
    }
    if (object.has_property(vm.names.enumerable)) {
        attributes.set_has_enumerable();
        if (object.get(vm.names.enumerable).value_or(Value(false)).to_boolean())
            attributes.set_enumerable();
        if (vm.exception())
            return {};
    }
    if (object.has_property(vm.names.writable)) {
        attributes.set_has_writable();
        if (object.get(vm.names.writable).value_or(Value(false)).to_boolean())
            attributes.set_writable();
        if (vm.exception())
            return {};
    }
    PropertyDescriptor descriptor { attributes, object.get(vm.names.value), nullptr, nullptr };
    if (vm.exception())
        return {};
    auto getter = object.get(vm.names.get);
    if (vm.exception())
        return {};
    if (getter.is_function())
        descriptor.getter = &getter.as_function();
    auto setter = object.get(vm.names.set);
    if (vm.exception())
        return {};
    if (setter.is_function())
        descriptor.setter = &setter.as_function();
    return descriptor;
}

Object* Object::create_empty(GlobalObject& global_object)
{
    return global_object.heap().allocate<Object>(global_object, *global_object.new_object_shape());
}

Object::Object(GlobalObjectTag)
{
    // This is the global object
    m_shape = heap().allocate_without_global_object<Shape>(*this);
}

Object::Object(ConstructWithoutPrototypeTag, GlobalObject& global_object)
{
    m_shape = heap().allocate_without_global_object<Shape>(global_object);
}

Object::Object(Object& prototype)
{
    m_shape = prototype.global_object().empty_object_shape();
    set_prototype(&prototype);
}

Object::Object(Shape& shape)
    : m_shape(&shape)
{
    m_storage.resize(shape.property_count());
}

void Object::initialize(GlobalObject&)
{
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
        if (vm().exception())
            return false;
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

Value Object::get_own_property(const PropertyName& property_name, Value receiver) const
{
    VERIFY(property_name.is_valid());
    VERIFY(!receiver.is_empty());

    Value value_here;

    if (property_name.is_number()) {
        auto existing_property = m_indexed_properties.get(nullptr, property_name.as_number(), false);
        if (!existing_property.has_value())
            return {};
        value_here = existing_property.value().value.value_or(js_undefined());
    } else {
        auto metadata = shape().lookup(property_name.to_string_or_symbol());
        if (!metadata.has_value())
            return {};
        value_here = m_storage[metadata.value().offset].value_or(js_undefined());
    }

    VERIFY(!value_here.is_empty());
    if (value_here.is_accessor())
        return value_here.as_accessor().call_getter(receiver);
    if (value_here.is_native_property())
        return call_native_property_getter(value_here.as_native_property(), receiver);
    return value_here;
}

Value Object::get_own_properties(const Object& this_object, PropertyKind kind, bool only_enumerable_properties, GetOwnPropertyReturnType return_type) const
{
    auto* properties_array = Array::create(global_object());

    // FIXME: Support generic iterables
    if (is<StringObject>(this_object)) {
        auto str = static_cast<const StringObject&>(this_object).primitive_string().string();

        for (size_t i = 0; i < str.length(); ++i) {
            if (kind == PropertyKind::Key) {
                properties_array->define_property(i, js_string(vm(), String::number(i)));
            } else if (kind == PropertyKind::Value) {
                properties_array->define_property(i, js_string(vm(), String::formatted("{:c}", str[i])));
            } else {
                auto* entry_array = Array::create(global_object());
                entry_array->define_property(0, js_string(vm(), String::number(i)));
                entry_array->define_property(1, js_string(vm(), String::formatted("{:c}", str[i])));
                properties_array->define_property(i, entry_array);
            }
            if (vm().exception())
                return {};
        }

        return properties_array;
    }

    size_t property_index = 0;
    for (auto& entry : m_indexed_properties) {
        auto value_and_attributes = entry.value_and_attributes(const_cast<Object*>(&this_object));
        if (only_enumerable_properties && !value_and_attributes.attributes.is_enumerable())
            continue;

        if (kind == PropertyKind::Key) {
            properties_array->define_property(property_index, js_string(vm(), String::number(entry.index())));
        } else if (kind == PropertyKind::Value) {
            properties_array->define_property(property_index, value_and_attributes.value);
        } else {
            auto* entry_array = Array::create(global_object());
            entry_array->define_property(0, js_string(vm(), String::number(entry.index())));
            entry_array->define_property(1, value_and_attributes.value);
            properties_array->define_property(property_index, entry_array);
        }
        if (vm().exception())
            return {};

        ++property_index;
    }

    for (auto& it : this_object.shape().property_table_ordered()) {
        if (only_enumerable_properties && !it.value.attributes.is_enumerable())
            continue;

        if (return_type == GetOwnPropertyReturnType::StringOnly && it.key.is_symbol())
            continue;
        if (return_type == GetOwnPropertyReturnType::SymbolOnly && it.key.is_string())
            continue;

        if (kind == PropertyKind::Key) {
            properties_array->define_property(property_index, it.key.to_value(vm()));
        } else if (kind == PropertyKind::Value) {
            properties_array->define_property(property_index, this_object.get(it.key));
        } else {
            auto* entry_array = Array::create(global_object());
            entry_array->define_property(0, it.key.to_value(vm()));
            entry_array->define_property(1, this_object.get(it.key));
            properties_array->define_property(property_index, entry_array);
        }
        if (vm().exception())
            return {};

        ++property_index;
    }

    return properties_array;
}

Optional<PropertyDescriptor> Object::get_own_property_descriptor(const PropertyName& property_name) const
{
    VERIFY(property_name.is_valid());

    Value value;
    PropertyAttributes attributes;

    if (property_name.is_number()) {
        auto existing_value = m_indexed_properties.get(nullptr, property_name.as_number(), false);
        if (!existing_value.has_value())
            return {};
        value = existing_value.value().value;
        attributes = existing_value.value().attributes;
        attributes = default_attributes;
    } else {
        auto metadata = shape().lookup(property_name.to_string_or_symbol());
        if (!metadata.has_value())
            return {};
        value = m_storage[metadata.value().offset];
        if (vm().exception())
            return {};
        attributes = metadata.value().attributes;
    }

    PropertyDescriptor descriptor { attributes, {}, nullptr, nullptr };
    if (value.is_native_property()) {
        auto result = call_native_property_getter(value.as_native_property(), const_cast<Object*>(this));
        descriptor.value = result.value_or(js_undefined());
    } else if (value.is_accessor()) {
        auto& pair = value.as_accessor();
        if (pair.getter())
            descriptor.getter = pair.getter();
        if (pair.setter())
            descriptor.setter = pair.setter();
    } else {
        descriptor.value = value.value_or(js_undefined());
    }

    return descriptor;
}

Value Object::get_own_property_descriptor_object(const PropertyName& property_name) const
{
    VERIFY(property_name.is_valid());

    auto& vm = this->vm();
    auto descriptor_opt = get_own_property_descriptor(property_name);
    if (!descriptor_opt.has_value())
        return js_undefined();
    auto descriptor = descriptor_opt.value();

    auto* descriptor_object = Object::create_empty(global_object());
    descriptor_object->define_property(vm.names.enumerable, Value(descriptor.attributes.is_enumerable()));
    if (vm.exception())
        return {};
    descriptor_object->define_property(vm.names.configurable, Value(descriptor.attributes.is_configurable()));
    if (vm.exception())
        return {};
    if (descriptor.is_data_descriptor()) {
        descriptor_object->define_property(vm.names.value, descriptor.value.value_or(js_undefined()));
        if (vm.exception())
            return {};
        descriptor_object->define_property(vm.names.writable, Value(descriptor.attributes.is_writable()));
        if (vm.exception())
            return {};
    } else if (descriptor.is_accessor_descriptor()) {
        if (descriptor.getter) {
            descriptor_object->define_property(vm.names.get, Value(descriptor.getter));
            if (vm.exception())
                return {};
        }
        if (descriptor.setter) {
            descriptor_object->define_property(vm.names.set, Value(descriptor.setter));
            if (vm.exception())
                return {};
        }
    }
    return descriptor_object;
}

void Object::set_shape(Shape& new_shape)
{
    m_storage.resize(new_shape.property_count());
    m_shape = &new_shape;
}

bool Object::define_property(const StringOrSymbol& property_name, const Object& descriptor, bool throw_exceptions)
{
    auto& vm = this->vm();
    bool is_accessor_property = descriptor.has_property(vm.names.get) || descriptor.has_property(vm.names.set);
    PropertyAttributes attributes;
    if (descriptor.has_property(vm.names.configurable)) {
        attributes.set_has_configurable();
        if (descriptor.get(vm.names.configurable).value_or(Value(false)).to_boolean())
            attributes.set_configurable();
        if (vm.exception())
            return false;
    }
    if (descriptor.has_property(vm.names.enumerable)) {
        attributes.set_has_enumerable();
        if (descriptor.get(vm.names.enumerable).value_or(Value(false)).to_boolean())
            attributes.set_enumerable();
        if (vm.exception())
            return false;
    }

    if (is_accessor_property) {
        if (descriptor.has_property(vm.names.value) || descriptor.has_property(vm.names.writable)) {
            if (throw_exceptions)
                vm.throw_exception<TypeError>(global_object(), ErrorType::AccessorValueOrWritable);
            return false;
        }

        auto getter = descriptor.get(vm.names.get).value_or(js_undefined());
        if (vm.exception())
            return {};
        auto setter = descriptor.get(vm.names.set).value_or(js_undefined());
        if (vm.exception())
            return {};

        Function* getter_function { nullptr };
        Function* setter_function { nullptr };

        if (getter.is_function()) {
            getter_function = &getter.as_function();
        } else if (!getter.is_undefined()) {
            vm.throw_exception<TypeError>(global_object(), ErrorType::AccessorBadField, "get");
            return false;
        }

        if (setter.is_function()) {
            setter_function = &setter.as_function();
        } else if (!setter.is_undefined()) {
            vm.throw_exception<TypeError>(global_object(), ErrorType::AccessorBadField, "set");
            return false;
        }

#if OBJECT_DEBUG
        dbgln("Defining new property {} with accessor descriptor {{ attributes={}, getter={}, setter={} }}", property_name.to_display_string(), attributes, getter, setter);
#endif

        return define_property(property_name, Accessor::create(vm, getter_function, setter_function), attributes, throw_exceptions);
    }

    auto value = descriptor.get(vm.names.value);
    if (vm.exception())
        return {};
    if (descriptor.has_property(vm.names.writable)) {
        attributes.set_has_writable();
        if (descriptor.get(vm.names.writable).value_or(Value(false)).to_boolean())
            attributes.set_writable();
        if (vm.exception())
            return false;
    }
    if (vm.exception())
        return {};

#if OBJECT_DEBUG
    dbgln("Defining new property {} with data descriptor {{ attributes={}, value={} }}", property_name.to_display_string(), attributes, value);
#endif

    return define_property(property_name, value, attributes, throw_exceptions);
}

bool Object::define_property_without_transition(const PropertyName& property_name, Value value, PropertyAttributes attributes, bool throw_exceptions)
{
    TemporaryChange change(m_transitions_enabled, false);
    return define_property(property_name, value, attributes, throw_exceptions);
}

bool Object::define_property(const PropertyName& property_name, Value value, PropertyAttributes attributes, bool throw_exceptions)
{
    VERIFY(property_name.is_valid());

    if (property_name.is_number())
        return put_own_property_by_index(*this, property_name.as_number(), value, attributes, PutOwnPropertyMode::DefineProperty, throw_exceptions);

    if (property_name.is_string()) {
        i32 property_index = property_name.as_string().to_int().value_or(-1);
        if (property_index >= 0)
            return put_own_property_by_index(*this, property_index, value, attributes, PutOwnPropertyMode::DefineProperty, throw_exceptions);
    }
    return put_own_property(*this, property_name.to_string_or_symbol(), value, attributes, PutOwnPropertyMode::DefineProperty, throw_exceptions);
}

bool Object::define_accessor(const PropertyName& property_name, Function& getter_or_setter, bool is_getter, PropertyAttributes attributes, bool throw_exceptions)
{
    VERIFY(property_name.is_valid());

    Accessor* accessor { nullptr };
    auto property_metadata = shape().lookup(property_name.to_string_or_symbol());
    if (property_metadata.has_value()) {
        auto existing_property = get_direct(property_metadata.value().offset);
        if (existing_property.is_accessor())
            accessor = &existing_property.as_accessor();
    }
    if (!accessor) {
        accessor = Accessor::create(vm(), nullptr, nullptr);
        bool definition_success = define_property(property_name, accessor, attributes, throw_exceptions);
        if (vm().exception())
            return {};
        if (!definition_success)
            return false;
    }
    if (is_getter)
        accessor->set_getter(&getter_or_setter);
    else
        accessor->set_setter(&getter_or_setter);

    return true;
}

bool Object::put_own_property(Object& this_object, const StringOrSymbol& property_name, Value value, PropertyAttributes attributes, PutOwnPropertyMode mode, bool throw_exceptions)
{
    VERIFY(!(mode == PutOwnPropertyMode::Put && value.is_accessor()));

    if (value.is_accessor()) {
        auto& accessor = value.as_accessor();
        if (accessor.getter())
            attributes.set_has_getter();
        if (accessor.setter())
            attributes.set_has_setter();
    }

    // NOTE: We disable transitions during initialize(), this makes building common runtime objects significantly faster.
    //       Transitions are primarily interesting when scripts add properties to objects.
    if (!m_transitions_enabled && !m_shape->is_unique()) {
        m_shape->add_property_without_transition(property_name, attributes);
        m_storage.resize(m_shape->property_count());
        m_storage[m_shape->property_count() - 1] = value;
        return true;
    }

    auto metadata = shape().lookup(property_name);
    bool new_property = !metadata.has_value();

    if (!is_extensible() && new_property) {
#if OBJECT_DEBUG
        dbgln("Disallow define_property of non-extensible object");
#endif
        if (throw_exceptions && vm().in_strict_mode())
            vm().throw_exception<TypeError>(global_object(), ErrorType::NonExtensibleDefine, property_name.to_display_string());
        return false;
    }

    if (new_property) {
        if (!m_shape->is_unique() && shape().property_count() > 100) {
            // If you add more than 100 properties to an object, let's stop doing
            // transitions to avoid filling up the heap with shapes.
            ensure_shape_is_unique();
        }

        if (m_shape->is_unique()) {
            m_shape->add_property_to_unique_shape(property_name, attributes);
            m_storage.resize(m_shape->property_count());
        } else if (m_transitions_enabled) {
            set_shape(*m_shape->create_put_transition(property_name, attributes));
        } else {
            m_shape->add_property_without_transition(property_name, attributes);
            m_storage.resize(m_shape->property_count());
        }
        metadata = shape().lookup(property_name);
        VERIFY(metadata.has_value());
    }

    if (!new_property && mode == PutOwnPropertyMode::DefineProperty && !metadata.value().attributes.is_configurable() && attributes != metadata.value().attributes) {
#if OBJECT_DEBUG
        dbgln("Disallow reconfig of non-configurable property");
#endif
        if (throw_exceptions)
            vm().throw_exception<TypeError>(global_object(), ErrorType::DescChangeNonConfigurable, property_name.to_display_string());
        return false;
    }

    if (mode == PutOwnPropertyMode::DefineProperty && attributes != metadata.value().attributes) {
        if (m_shape->is_unique()) {
            m_shape->reconfigure_property_in_unique_shape(property_name, attributes);
        } else {
            set_shape(*m_shape->create_configure_transition(property_name, attributes));
        }
        metadata = shape().lookup(property_name);

#if OBJECT_DEBUG
        dbgln("Reconfigured property {}, new shape says offset is {} and my storage capacity is {}", property_name.to_display_string(), metadata.value().offset, m_storage.size());
#endif
    }

    auto value_here = m_storage[metadata.value().offset];
    if (!new_property && mode == PutOwnPropertyMode::Put && !value_here.is_accessor() && !metadata.value().attributes.is_writable()) {
#if OBJECT_DEBUG
        dbgln("Disallow write to non-writable property");
#endif
        return false;
    }

    if (value.is_empty())
        return true;

    if (value_here.is_native_property()) {
        call_native_property_setter(value_here.as_native_property(), &this_object, value);
    } else {
        m_storage[metadata.value().offset] = value;
    }
    return true;
}

bool Object::put_own_property_by_index(Object& this_object, u32 property_index, Value value, PropertyAttributes attributes, PutOwnPropertyMode mode, bool throw_exceptions)
{
    VERIFY(!(mode == PutOwnPropertyMode::Put && value.is_accessor()));

    auto existing_property = m_indexed_properties.get(nullptr, property_index, false);
    auto new_property = !existing_property.has_value();

    if (!is_extensible() && new_property) {
#if OBJECT_DEBUG
        dbgln("Disallow define_property of non-extensible object");
#endif
        if (throw_exceptions && vm().in_strict_mode())
            vm().throw_exception<TypeError>(global_object(), ErrorType::NonExtensibleDefine, property_index);
        return false;
    }

    if (value.is_accessor()) {
        auto& accessor = value.as_accessor();
        if (accessor.getter())
            attributes.set_has_getter();
        if (accessor.setter())
            attributes.set_has_setter();
    }

    PropertyAttributes existing_attributes = new_property ? 0 : existing_property.value().attributes;

    if (!new_property && mode == PutOwnPropertyMode::DefineProperty && !existing_attributes.is_configurable() && attributes != existing_attributes) {
#if OBJECT_DEBUG
        dbgln("Disallow reconfig of non-configurable property");
#endif
        if (throw_exceptions)
            vm().throw_exception<TypeError>(global_object(), ErrorType::DescChangeNonConfigurable, property_index);
        return false;
    }

    auto value_here = new_property ? Value() : existing_property.value().value;
    if (!new_property && mode == PutOwnPropertyMode::Put && !value_here.is_accessor() && !existing_attributes.is_writable()) {
#if OBJECT_DEBUG
        dbgln("Disallow write to non-writable property");
#endif
        return false;
    }

    if (value.is_empty())
        return true;

    if (value_here.is_native_property()) {
        call_native_property_setter(value_here.as_native_property(), &this_object, value);
    } else {
        m_indexed_properties.put(&this_object, property_index, value, attributes, mode == PutOwnPropertyMode::Put);
    }
    return true;
}

Value Object::delete_property(const PropertyName& property_name)
{
    VERIFY(property_name.is_valid());

    if (property_name.is_number())
        return Value(m_indexed_properties.remove(property_name.as_number()));

    if (property_name.is_string()) {
        i32 property_index = property_name.as_string().to_int().value_or(-1);
        if (property_index >= 0)
            return Value(m_indexed_properties.remove(property_index));
    }

    auto metadata = shape().lookup(property_name.to_string_or_symbol());
    if (!metadata.has_value())
        return Value(true);
    if (!metadata.value().attributes.is_configurable())
        return Value(false);

    size_t deleted_offset = metadata.value().offset;

    ensure_shape_is_unique();

    shape().remove_property_from_unique_shape(property_name.to_string_or_symbol(), deleted_offset);
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
        if (is<StringObject>(*this)) {
            auto& string = static_cast<const StringObject*>(this)->primitive_string().string();
            if (property_index < string.length())
                return js_string(heap(), string.substring(property_index, 1));
            return js_undefined();
        }
        if (static_cast<size_t>(property_index) < object->m_indexed_properties.array_like_size()) {
            auto result = object->m_indexed_properties.get(const_cast<Object*>(this), property_index);
            if (vm().exception())
                return {};
            if (result.has_value() && !result.value().value.is_empty())
                return result.value().value;
            return {};
        }
        object = object->prototype();
        if (vm().exception())
            return {};
    }
    return {};
}

Value Object::get(const PropertyName& property_name, Value receiver) const
{
    VERIFY(property_name.is_valid());

    if (property_name.is_number())
        return get_by_index(property_name.as_number());

    if (property_name.is_string()) {
        auto property_string = property_name.to_string();
        i32 property_index = property_string.to_int().value_or(-1);
        if (property_index >= 0)
            return get_by_index(property_index);
    }

    const Object* object = this;
    while (object) {
        if (receiver.is_empty())
            receiver = Value(const_cast<Object*>(this));
        auto value = object->get_own_property(property_name, receiver);
        if (vm().exception())
            return {};
        if (!value.is_empty())
            return value;
        object = object->prototype();
        if (vm().exception())
            return {};
    }
    return {};
}

bool Object::put_by_index(u32 property_index, Value value)
{
    VERIFY(!value.is_empty());

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
            if (value_here.value.is_native_property()) {
                // FIXME: Why doesn't put_by_index() receive the receiver value from put()?!
                auto receiver = this;
                call_native_property_setter(value_here.value.as_native_property(), receiver, value);
                return true;
            }
        }
        object = object->prototype();
        if (vm().exception())
            return {};
    }
    return put_own_property_by_index(*this, property_index, value, default_attributes, PutOwnPropertyMode::Put);
}

bool Object::put(const PropertyName& property_name, Value value, Value receiver)
{
    VERIFY(property_name.is_valid());

    if (property_name.is_number())
        return put_by_index(property_name.as_number(), value);

    VERIFY(!value.is_empty());

    if (property_name.is_string()) {
        auto& property_string = property_name.as_string();
        i32 property_index = property_string.to_int().value_or(-1);
        if (property_index >= 0)
            return put_by_index(property_index, value);
    }

    auto string_or_symbol = property_name.to_string_or_symbol();

    if (receiver.is_empty())
        receiver = Value(this);

    // If there's a setter in the prototype chain, we go to the setter.
    // Otherwise, it goes in the own property storage.
    Object* object = this;
    while (object) {
        auto metadata = object->shape().lookup(string_or_symbol);
        if (metadata.has_value()) {
            auto value_here = object->m_storage[metadata.value().offset];
            if (value_here.is_accessor()) {
                value_here.as_accessor().call_setter(receiver, value);
                return true;
            }
            if (value_here.is_native_property()) {
                call_native_property_setter(value_here.as_native_property(), receiver, value);
                return true;
            }
        }
        object = object->prototype();
        if (vm().exception())
            return false;
    }
    return put_own_property(*this, string_or_symbol, value, default_attributes, PutOwnPropertyMode::Put);
}

bool Object::define_native_function(const StringOrSymbol& property_name, AK::Function<Value(VM&, GlobalObject&)> native_function, i32 length, PropertyAttributes attribute)
{
    auto& vm = this->vm();
    String function_name;
    if (property_name.is_string()) {
        function_name = property_name.as_string();
    } else {
        function_name = String::formatted("[{}]", property_name.as_symbol()->description());
    }
    auto* function = NativeFunction::create(global_object(), function_name, move(native_function));
    function->define_property_without_transition(vm.names.length, Value(length), Attribute::Configurable);
    if (vm.exception())
        return {};
    function->define_property_without_transition(vm.names.name, js_string(vm.heap(), function_name), Attribute::Configurable);
    if (vm.exception())
        return {};
    return define_property(property_name, function, attribute);
}

bool Object::define_native_property(const StringOrSymbol& property_name, AK::Function<Value(VM&, GlobalObject&)> getter, AK::Function<void(VM&, GlobalObject&, Value)> setter, PropertyAttributes attribute)
{
    return define_property(property_name, heap().allocate_without_global_object<NativeProperty>(move(getter), move(setter)), attribute);
}

void Object::visit_edges(Cell::Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(m_shape);

    for (auto& value : m_storage)
        visitor.visit(value);

    m_indexed_properties.for_each_value([&visitor](auto& value) {
        visitor.visit(value);
    });
}

bool Object::has_property(const PropertyName& property_name) const
{
    const Object* object = this;
    while (object) {
        if (object->has_own_property(property_name))
            return true;
        object = object->prototype();
        if (vm().exception())
            return false;
    }
    return false;
}

bool Object::has_own_property(const PropertyName& property_name) const
{
    VERIFY(property_name.is_valid());

    auto has_indexed_property = [&](u32 index) -> bool {
        if (is<StringObject>(*this))
            return index < static_cast<const StringObject*>(this)->primitive_string().string().length();
        return m_indexed_properties.has_index(index);
    };

    if (property_name.is_number())
        return has_indexed_property(property_name.as_number());

    if (property_name.is_string()) {
        i32 property_index = property_name.as_string().to_int().value_or(-1);
        if (property_index >= 0)
            return has_indexed_property(property_index);
    }

    return shape().lookup(property_name.to_string_or_symbol()).has_value();
}

Value Object::ordinary_to_primitive(Value::PreferredType preferred_type) const
{
    VERIFY(preferred_type == Value::PreferredType::String || preferred_type == Value::PreferredType::Number);

    auto& vm = this->vm();

    Vector<FlyString, 2> method_names;
    if (preferred_type == Value::PreferredType::String)
        method_names = { vm.names.toString, vm.names.valueOf };
    else
        method_names = { vm.names.valueOf, vm.names.toString };

    for (auto& method_name : method_names) {
        auto method = get(method_name);
        if (vm.exception())
            return {};
        if (method.is_function()) {
            auto result = vm.call(method.as_function(), const_cast<Object*>(this));
            if (!result.is_object())
                return result;
        }
    }
    vm.throw_exception<TypeError>(global_object(), ErrorType::Convert, "object", preferred_type == Value::PreferredType::String ? "string" : "number");
    return {};
}

Value Object::invoke_internal(const StringOrSymbol& property_name, Optional<MarkedValueList> arguments)
{
    auto& vm = this->vm();
    auto property = get(property_name).value_or(js_undefined());
    if (vm.exception())
        return {};
    if (!property.is_function()) {
        vm.throw_exception<TypeError>(global_object(), ErrorType::NotAFunction, property.to_string_without_side_effects());
        return {};
    }
    return vm.call(property.as_function(), this, move(arguments));
}

Value Object::call_native_property_getter(NativeProperty& property, Value this_value) const
{
    auto& vm = this->vm();
    CallFrame call_frame;
    call_frame.current_node = property.vm().current_node();
    call_frame.is_strict_mode = vm.in_strict_mode();
    call_frame.this_value = this_value;
    vm.push_call_frame(call_frame, global_object());
    if (vm.exception())
        return {};
    auto result = property.get(vm, global_object());
    vm.pop_call_frame();
    return result;
}

void Object::call_native_property_setter(NativeProperty& property, Value this_value, Value setter_value) const
{
    auto& vm = this->vm();
    CallFrame call_frame;
    call_frame.current_node = property.vm().current_node();
    call_frame.is_strict_mode = vm.in_strict_mode();
    call_frame.this_value = this_value;
    vm.push_call_frame(call_frame, global_object());
    if (vm.exception())
        return;
    property.set(vm, global_object(), setter_value);
    vm.pop_call_frame();
}

}
