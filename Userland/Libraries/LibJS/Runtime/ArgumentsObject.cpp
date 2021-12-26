/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ArgumentsObject.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ArgumentsObject::ArgumentsObject(GlobalObject& global_object, Environment& environment)
    : Object(*global_object.object_prototype())
    , m_environment(environment)
{
}

void ArgumentsObject::initialize(GlobalObject& global_object)
{
    Base::initialize(global_object);
    set_has_parameter_map();
    m_parameter_map = Object::create(global_object, nullptr);
}

ArgumentsObject::~ArgumentsObject()
{
}

void ArgumentsObject::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_environment);
    visitor.visit(m_parameter_map);
}

// 10.4.4.3 [[Get]] ( P, Receiver ), https://tc39.es/ecma262/#sec-arguments-exotic-objects-get-p-receiver
ThrowCompletionOr<Value> ArgumentsObject::internal_get(PropertyName const& property_name, Value receiver) const
{
    // 1. Let map be args.[[ParameterMap]].
    auto& map = *m_parameter_map;
    // 2. Let isMapped be ! HasOwnProperty(map, P).
    bool is_mapped = m_parameter_map->has_own_property(property_name);
    // 3. If isMapped is false, then
    if (!is_mapped) {
        // a. Return ? OrdinaryGet(args, P, Receiver).
        return Object::internal_get(property_name, receiver);
    }

    // FIXME: a. Assert: map contains a formal parameter mapping for P.

    // b. Return Get(map, P).
    return map.get(property_name);
}

// 10.4.4.4 [[Set]] ( P, V, Receiver ), https://tc39.es/ecma262/#sec-arguments-exotic-objects-set-p-v-receiver
ThrowCompletionOr<bool> ArgumentsObject::internal_set(PropertyName const& property_name, Value value, Value receiver)
{
    bool is_mapped = false;

    // 1. If SameValue(args, Receiver) is false, then
    if (!same_value(this, receiver)) {
        // a. Let isMapped be false.
        is_mapped = false;
    } else {
        // a. Let map be args.[[ParameterMap]].
        // b. Let isMapped be ! HasOwnProperty(map, P).
        is_mapped = parameter_map().has_own_property(property_name);
    }

    // 3. If isMapped is true, then
    if (is_mapped) {
        // a. Let setStatus be Set(map, P, V, false).
        auto set_status = m_parameter_map->set(property_name, value, Object::ShouldThrowExceptions::No);
        // b. Assert: setStatus is true because formal parameters mapped by argument objects are always writable.
        VERIFY(set_status);
    }

    // 4. Return ? OrdinarySet(args, P, V, Receiver).
    return Object::internal_set(property_name, value, receiver);
}

// 10.4.4.5 [[Delete]] ( P ), https://tc39.es/ecma262/#sec-arguments-exotic-objects-delete-p
ThrowCompletionOr<bool> ArgumentsObject::internal_delete(PropertyName const& property_name)
{
    // 1. Let map be args.[[ParameterMap]].
    auto& map = parameter_map();

    // 2. Let isMapped be ! HasOwnProperty(map, P).
    bool is_mapped = map.has_own_property(property_name);

    // 3. Let result be ? OrdinaryDelete(args, P).
    bool result = TRY(Object::internal_delete(property_name));

    // 4. If result is true and isMapped is true, then
    if (result && is_mapped) {
        // a. Call map.[[Delete]](P).
        MUST(map.internal_delete(property_name));
    }

    // 5. Return result.
    return result;
}

// 10.4.4.1 [[GetOwnProperty]] ( P ), https://tc39.es/ecma262/#sec-arguments-exotic-objects-getownproperty-p
ThrowCompletionOr<Optional<PropertyDescriptor>> ArgumentsObject::internal_get_own_property(PropertyName const& property_name) const
{
    // 1. Let desc be OrdinaryGetOwnProperty(args, P).
    auto desc = MUST(Object::internal_get_own_property(property_name));

    // 2. If desc is undefined, return desc.
    if (!desc.has_value())
        return desc;
    // 3. Let map be args.[[ParameterMap]].
    // 4. Let isMapped be ! HasOwnProperty(map, P).
    bool is_mapped = m_parameter_map->has_own_property(property_name);
    // 5. If isMapped is true, then
    if (is_mapped) {
        // a. Set desc.[[Value]] to Get(map, P).
        desc->value = m_parameter_map->get(property_name);
    }
    // 6. Return desc.
    return desc;
}

// 10.4.4.2 [[DefineOwnProperty]] ( P, Desc ), https://tc39.es/ecma262/#sec-arguments-exotic-objects-defineownproperty-p-desc
ThrowCompletionOr<bool> ArgumentsObject::internal_define_own_property(PropertyName const& property_name, PropertyDescriptor const& descriptor)
{
    // 1. Let map be args.[[ParameterMap]].
    auto& map = parameter_map();

    // 2. Let isMapped be HasOwnProperty(map, P).
    bool is_mapped = map.has_own_property(property_name);

    // 3. Let newArgDesc be Desc.
    auto new_arg_desc = descriptor;

    // 4. If isMapped is true and IsDataDescriptor(Desc) is true, then
    if (is_mapped && descriptor.is_data_descriptor()) {
        // a. If Desc.[[Value]] is not present and Desc.[[Writable]] is present and its value is false, then
        if (!descriptor.value.has_value() && descriptor.writable.has_value() && descriptor.writable == false) {
            // i. Set newArgDesc to a copy of Desc.
            new_arg_desc = descriptor;
            // ii. Set newArgDesc.[[Value]] to Get(map, P).
            new_arg_desc.value = map.get(property_name);
        }
    }

    // 5. Let allowed be ? OrdinaryDefineOwnProperty(args, P, newArgDesc).
    bool allowed = TRY(Object::internal_define_own_property(property_name, new_arg_desc));

    // 6. If allowed is false, return false.
    if (!allowed)
        return false;

    // 7. If isMapped is true, then
    if (is_mapped) {
        // a. If IsAccessorDescriptor(Desc) is true, then
        if (descriptor.is_accessor_descriptor()) {
            // i. Call map.[[Delete]](P).
            MUST(map.internal_delete(property_name));
        } else {
            // i. If Desc.[[Value]] is present, then
            if (descriptor.value.has_value()) {
                // 1. Let setStatus be Set(map, P, Desc.[[Value]], false).
                bool set_status = map.set(property_name, descriptor.value.value(), Object::ShouldThrowExceptions::No);
                // 2. Assert: setStatus is true because formal parameters mapped by argument objects are always writable.
                VERIFY(set_status);
            }
            // ii. If Desc.[[Writable]] is present and its value is false, then
            if (descriptor.writable == false) {
                // 1. Call map.[[Delete]](P).
                MUST(map.internal_delete(property_name));
            }
        }
    }

    // 8. Return true.
    return true;
}

}
