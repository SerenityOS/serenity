/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PropertyDescriptor.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// 6.2.5.1 IsAccessorDescriptor ( Desc ), https://tc39.es/ecma262/#sec-isaccessordescriptor
bool PropertyDescriptor::is_accessor_descriptor() const
{
    // 1. If Desc is undefined, return false.

    // 2. If both Desc.[[Get]] and Desc.[[Set]] are absent, return false.
    if (!get.has_value() && !set.has_value())
        return false;

    // 3. Return true.
    return true;
}

// 6.2.5.2 IsDataDescriptor ( Desc ), https://tc39.es/ecma262/#sec-isdatadescriptor
bool PropertyDescriptor::is_data_descriptor() const
{
    // 1. If Desc is undefined, return false.

    // 2. If both Desc.[[Value]] and Desc.[[Writable]] are absent, return false.
    if (!value.has_value() && !writable.has_value())
        return false;

    // 3. Return true.
    return true;
}

// 6.2.5.3 IsGenericDescriptor ( Desc ), https://tc39.es/ecma262/#sec-isgenericdescriptor
bool PropertyDescriptor::is_generic_descriptor() const
{
    // 1. If Desc is undefined, return false.

    // 2. If IsAccessorDescriptor(Desc) and IsDataDescriptor(Desc) are both false, return true.
    if (!is_accessor_descriptor() && !is_data_descriptor())
        return true;

    // 3. Return false.
    return false;
}

// 6.2.5.4 FromPropertyDescriptor ( Desc ), https://tc39.es/ecma262/#sec-frompropertydescriptor
Value from_property_descriptor(GlobalObject& global_object, Optional<PropertyDescriptor> const& property_descriptor)
{
    if (!property_descriptor.has_value())
        return js_undefined();
    auto& vm = global_object.vm();
    auto* object = Object::create(global_object, global_object.object_prototype());
    if (property_descriptor->value.has_value())
        MUST(object->create_data_property_or_throw(vm.names.value, *property_descriptor->value));
    if (property_descriptor->writable.has_value())
        MUST(object->create_data_property_or_throw(vm.names.writable, Value(*property_descriptor->writable)));
    if (property_descriptor->get.has_value())
        MUST(object->create_data_property_or_throw(vm.names.get, *property_descriptor->get ? Value(*property_descriptor->get) : js_undefined()));
    if (property_descriptor->set.has_value())
        MUST(object->create_data_property_or_throw(vm.names.set, *property_descriptor->set ? Value(*property_descriptor->set) : js_undefined()));
    if (property_descriptor->enumerable.has_value())
        MUST(object->create_data_property_or_throw(vm.names.enumerable, Value(*property_descriptor->enumerable)));
    if (property_descriptor->configurable.has_value())
        MUST(object->create_data_property_or_throw(vm.names.configurable, Value(*property_descriptor->configurable)));
    return object;
}

// 6.2.5.5 ToPropertyDescriptor ( Obj ), https://tc39.es/ecma262/#sec-topropertydescriptor
PropertyDescriptor to_property_descriptor(GlobalObject& global_object, Value argument)
{
    auto& vm = global_object.vm();
    if (!argument.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, argument.to_string_without_side_effects());
        return {};
    }
    auto& object = argument.as_object();
    PropertyDescriptor descriptor;
    auto has_enumerable = object.has_property(vm.names.enumerable);
    if (vm.exception())
        return {};
    if (has_enumerable) {
        auto enumerable = TRY_OR_DISCARD(object.get(vm.names.enumerable));
        descriptor.enumerable = enumerable.to_boolean();
    }
    auto has_configurable = object.has_property(vm.names.configurable);
    if (vm.exception())
        return {};
    if (has_configurable) {
        auto configurable = TRY_OR_DISCARD(object.get(vm.names.configurable));
        descriptor.configurable = configurable.to_boolean();
    }
    auto has_value = object.has_property(vm.names.value);
    if (vm.exception())
        return {};
    if (has_value) {
        auto value = TRY_OR_DISCARD(object.get(vm.names.value));
        descriptor.value = value;
    }
    auto has_writable = object.has_property(vm.names.writable);
    if (vm.exception())
        return {};
    if (has_writable) {
        auto writable = TRY_OR_DISCARD(object.get(vm.names.writable));
        descriptor.writable = writable.to_boolean();
    }
    auto has_get = object.has_property(vm.names.get);
    if (vm.exception())
        return {};
    if (has_get) {
        auto getter = TRY_OR_DISCARD(object.get(vm.names.get));
        if (!getter.is_function() && !getter.is_undefined()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::AccessorBadField, "get");
            return {};
        }
        descriptor.get = getter.is_function() ? &getter.as_function() : nullptr;
    }
    auto has_set = object.has_property(vm.names.set);
    if (vm.exception())
        return {};
    if (has_set) {
        auto setter = TRY_OR_DISCARD(object.get(vm.names.set));
        if (!setter.is_function() && !setter.is_undefined()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::AccessorBadField, "set");
            return {};
        }
        descriptor.set = setter.is_function() ? &setter.as_function() : nullptr;
    }
    if (descriptor.get.has_value() || descriptor.set.has_value()) {
        if (descriptor.value.has_value() || descriptor.writable.has_value()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::AccessorValueOrWritable);
            return {};
        }
    }
    return descriptor;
}

// 6.2.5.6 CompletePropertyDescriptor ( Desc ), https://tc39.es/ecma262/#sec-completepropertydescriptor
void PropertyDescriptor::complete()
{
    if (is_generic_descriptor() || is_data_descriptor()) {
        if (!value.has_value())
            value = Value {};
        if (!writable.has_value())
            writable = false;
    } else {
        if (!get.has_value())
            get = nullptr;
        if (!set.has_value())
            set = nullptr;
    }
    if (!enumerable.has_value())
        enumerable = false;
    if (!configurable.has_value())
        configurable = false;
}

// Non-standard, just a convenient way to get from three Optional<bool> to PropertyAttributes.
PropertyAttributes PropertyDescriptor::attributes() const
{
    u8 attributes = 0;
    if (writable.value_or(false))
        attributes |= Attribute::Writable;
    if (enumerable.value_or(false))
        attributes |= Attribute::Enumerable;
    if (configurable.value_or(false))
        attributes |= Attribute::Configurable;
    return { attributes };
}

}
