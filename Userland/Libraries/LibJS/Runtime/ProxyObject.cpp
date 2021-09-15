/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PropertyDescriptor.h>
#include <LibJS/Runtime/ProxyObject.h>

namespace JS {

ProxyObject* ProxyObject::create(GlobalObject& global_object, Object& target, Object& handler)
{
    return global_object.heap().allocate<ProxyObject>(global_object, target, handler, *global_object.object_prototype());
}

ProxyObject::ProxyObject(Object& target, Object& handler, Object& prototype)
    : FunctionObject(prototype)
    , m_target(target)
    , m_handler(handler)
{
}

ProxyObject::~ProxyObject()
{
}

static Value property_name_to_value(VM& vm, PropertyName const& name)
{
    VERIFY(name.is_valid());
    if (name.is_symbol())
        return name.as_symbol();

    if (name.is_string())
        return js_string(vm, name.as_string());

    VERIFY(name.is_number());
    return js_string(vm, String::number(name.as_number()));
}

// 10.5.1 [[GetPrototypeOf]] ( ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-getprototypeof
Object* ProxyObject::internal_get_prototype_of() const
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Let handler be O.[[ProxyHandler]].

    // 2. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 3. Assert: Type(handler) is Object.
    // 4. Let target be O.[[ProxyTarget]].

    // 5. Let trap be ? GetMethod(handler, "getPrototypeOf").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.getPrototypeOf);
    if (vm.exception())
        return {};

    // 6. If trap is undefined, then
    if (!trap) {
        // a. Return ? target.[[GetPrototypeOf]]().
        return m_target.internal_get_prototype_of();
    }

    // 7. Let handlerProto be ? Call(trap, handler, « target »).
    auto handler_proto = vm.call(*trap, &m_handler, &m_target);
    if (vm.exception())
        return {};

    // 8. If Type(handlerProto) is neither Object nor Null, throw a TypeError exception.
    if (!handler_proto.is_object() && !handler_proto.is_null()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyGetPrototypeOfReturn);
        return {};
    }

    // 9. Let extensibleTarget be ? IsExtensible(target).
    auto extensible_target = m_target.is_extensible();
    if (vm.exception())
        return {};

    // 10. If extensibleTarget is true, return handlerProto.
    if (extensible_target)
        return handler_proto.is_null() ? nullptr : &handler_proto.as_object();

    // 11. Let targetProto be ? target.[[GetPrototypeOf]]().
    auto target_proto = m_target.internal_get_prototype_of();
    if (vm.exception())
        return {};

    // 12. If SameValue(handlerProto, targetProto) is false, throw a TypeError exception.
    if (!same_value(handler_proto, target_proto)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyGetPrototypeOfNonExtensible);
        return {};
    }

    // 13. Return handlerProto.
    return handler_proto.is_null() ? nullptr : &handler_proto.as_object();
}

// 10.5.2 [[SetPrototypeOf]] ( V ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-setprototypeof-v
bool ProxyObject::internal_set_prototype_of(Object* prototype)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Assert: Either Type(V) is Object or Type(V) is Null.
    // 2. Let handler be O.[[ProxyHandler]].

    // 3. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 4. Assert: Type(handler) is Object.
    // 5. Let target be O.[[ProxyTarget]].

    // 6. Let trap be ? GetMethod(handler, "setPrototypeOf").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.setPrototypeOf);
    if (vm.exception())
        return {};

    // 7. If trap is undefined, then
    if (!trap) {
        // a. Return ? target.[[SetPrototypeOf]](V).
        return m_target.internal_set_prototype_of(prototype);
    }

    // 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, V »)).
    auto trap_result = vm.call(*trap, &m_handler, &m_target, prototype);
    if (vm.exception())
        return {};

    // 9. If booleanTrapResult is false, return false.
    if (!trap_result.to_boolean())
        return false;

    // 10. Let extensibleTarget be ? IsExtensible(target).
    auto extensible_target = m_target.is_extensible();
    if (vm.exception())
        return {};

    // 11. If extensibleTarget is true, return true.
    if (extensible_target)
        return true;

    // 12. Let targetProto be ? target.[[GetPrototypeOf]]().
    auto* target_proto = m_target.internal_get_prototype_of();
    if (vm.exception())
        return {};

    // 13. If SameValue(V, targetProto) is false, throw a TypeError exception.
    if (!same_value(prototype, target_proto)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxySetPrototypeOfNonExtensible);
        return {};
    }

    // 14. Return true.
    return true;
}

// 10.5.3 [[IsExtensible]] ( ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-isextensible
bool ProxyObject::internal_is_extensible() const
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Let handler be O.[[ProxyHandler]].

    // 2. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 3. Assert: Type(handler) is Object.
    // 4. Let target be O.[[ProxyTarget]].

    // 5. Let trap be ? GetMethod(handler, "isExtensible").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.isExtensible);
    if (vm.exception())
        return {};

    // 6. If trap is undefined, then
    if (!trap) {
        // a. Return ? IsExtensible(target).
        return m_target.is_extensible();
    }

    // 7. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target »)).
    auto trap_result = vm.call(*trap, &m_handler, &m_target);
    if (vm.exception())
        return {};

    // 8. Let targetResult be ? IsExtensible(target).
    auto target_result = m_target.is_extensible();
    if (vm.exception())
        return {};

    // 9. If SameValue(booleanTrapResult, targetResult) is false, throw a TypeError exception.
    if (trap_result.to_boolean() != target_result) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyIsExtensibleReturn);
        return {};
    }

    // 10. Return booleanTrapResult.
    return trap_result.to_boolean();
}

// 10.5.4 [[PreventExtensions]] ( ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-preventextensions
bool ProxyObject::internal_prevent_extensions()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Let handler be O.[[ProxyHandler]].

    // 2. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 3. Assert: Type(handler) is Object.
    // 4. Let target be O.[[ProxyTarget]].

    // 5. Let trap be ? GetMethod(handler, "preventExtensions").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.preventExtensions);
    if (vm.exception())
        return {};

    // 6. If trap is undefined, then
    if (!trap) {
        // a. Return ? target.[[PreventExtensions]]().
        return m_target.internal_prevent_extensions();
    }

    // 7. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target »)).
    auto trap_result = vm.call(*trap, &m_handler, &m_target);
    if (vm.exception())
        return {};

    // 8. If booleanTrapResult is true, then
    if (trap_result.to_boolean()) {
        // a. Let extensibleTarget be ? IsExtensible(target).
        auto extensible_target = m_target.is_extensible();
        if (vm.exception())
            return {};

        // b. If extensibleTarget is true, throw a TypeError exception.
        if (extensible_target) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ProxyPreventExtensionsReturn);
            return {};
        }
    }

    // 9. Return booleanTrapResult.
    return trap_result.to_boolean();
}

// 10.5.5 [[GetOwnProperty]] ( P ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-getownproperty-p
Optional<PropertyDescriptor> ProxyObject::internal_get_own_property(const PropertyName& property_name) const
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_name.is_valid());

    // 2. Let handler be O.[[ProxyHandler]].

    // 3. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 4. Assert: Type(handler) is Object.
    // 5. Let target be O.[[ProxyTarget]].

    // 6. Let trap be ? GetMethod(handler, "getOwnPropertyDescriptor").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.getOwnPropertyDescriptor);
    if (vm.exception())
        return {};

    // 7. If trap is undefined, then
    if (!trap) {
        // a. Return ? target.[[GetOwnProperty]](P).
        return m_target.internal_get_own_property(property_name);
    }

    // 8. Let trapResultObj be ? Call(trap, handler, « target, P »).
    auto trap_result = vm.call(*trap, &m_handler, &m_target, property_name_to_value(vm, property_name));
    if (vm.exception())
        return {};

    // 9. If Type(trapResultObj) is neither Object nor Undefined, throw a TypeError exception.
    if (!trap_result.is_object() && !trap_result.is_undefined()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyGetOwnDescriptorReturn);
        return {};
    }

    // 10. Let targetDesc be ? target.[[GetOwnProperty]](P).
    auto target_descriptor = m_target.internal_get_own_property(property_name);
    if (vm.exception())
        return {};

    // 11. If trapResultObj is undefined, then
    if (trap_result.is_undefined()) {
        // a. If targetDesc is undefined, return undefined.
        if (!target_descriptor.has_value())
            return {};

        // b. If targetDesc.[[Configurable]] is false, throw a TypeError exception.
        if (!*target_descriptor->configurable) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ProxyGetOwnDescriptorNonConfigurable);
            return {};
        }

        // c. Let extensibleTarget be ? IsExtensible(target).
        auto extensible_target = m_target.is_extensible();
        if (vm.exception())
            return {};

        // d. If extensibleTarget is false, throw a TypeError exception.
        if (!extensible_target) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ProxyGetOwnDescriptorUndefinedReturn);
            return {};
        }

        // e. Return undefined.
        return {};
    }

    // 12. Let extensibleTarget be ? IsExtensible(target).
    auto extensible_target = m_target.is_extensible();
    if (vm.exception())
        return {};

    // 13. Let resultDesc be ? ToPropertyDescriptor(trapResultObj).
    auto result_desc = to_property_descriptor(global_object, trap_result);
    if (vm.exception())
        return {};

    // 14. Call CompletePropertyDescriptor(resultDesc).
    result_desc.complete();

    // 15. Let valid be IsCompatiblePropertyDescriptor(extensibleTarget, resultDesc, targetDesc).
    auto valid = is_compatible_property_descriptor(extensible_target, result_desc, target_descriptor);

    // 16. If valid is false, throw a TypeError exception.
    if (!valid) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyGetOwnDescriptorInvalidDescriptor);
        return {};
    }

    // 17. If resultDesc.[[Configurable]] is false, then
    if (!*result_desc.configurable) {
        // a. If targetDesc is undefined or targetDesc.[[Configurable]] is true, then
        if (!target_descriptor.has_value() || *target_descriptor->configurable) {
            // i. Throw a TypeError exception.
            vm.throw_exception<TypeError>(global_object, ErrorType::ProxyGetOwnDescriptorInvalidNonConfig);
            return {};
        }
        // b. If resultDesc has a [[Writable]] field and resultDesc.[[Writable]] is false, then
        if (result_desc.writable.has_value() && !*result_desc.writable) {
            // i. If targetDesc.[[Writable]] is true, throw a TypeError exception.
            if (*target_descriptor->writable) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ProxyGetOwnDescriptorNonConfigurableNonWritable);
                return {};
            }
        }
    }

    // 18. Return resultDesc.
    return result_desc;
}

// 10.5.6 [[DefineOwnProperty]] ( P, Desc ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-defineownproperty-p-desc
bool ProxyObject::internal_define_own_property(PropertyName const& property_name, PropertyDescriptor const& property_descriptor)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_name.is_valid());

    // 2. Let handler be O.[[ProxyHandler]].

    // 3. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 4. Assert: Type(handler) is Object.
    // 5. Let target be O.[[ProxyTarget]].

    // 6. Let trap be ? GetMethod(handler, "defineProperty").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.defineProperty);
    if (vm.exception())
        return {};

    // 7. If trap is undefined, then
    if (!trap) {
        // a. Return ? target.[[DefineOwnProperty]](P, Desc).
        return m_target.internal_define_own_property(property_name, property_descriptor);
    }

    // 8. Let descObj be FromPropertyDescriptor(Desc).
    auto descriptor_object = from_property_descriptor(global_object, property_descriptor);

    // 9. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P, descObj »)).
    auto trap_result = vm.call(*trap, &m_handler, &m_target, property_name_to_value(vm, property_name), descriptor_object);
    if (vm.exception())
        return {};

    // 10. If booleanTrapResult is false, return false.
    if (!trap_result.to_boolean())
        return false;

    // 11. Let targetDesc be ? target.[[GetOwnProperty]](P).
    auto target_descriptor = m_target.internal_get_own_property(property_name);
    if (vm.exception())
        return {};

    // 12. Let extensibleTarget be ? IsExtensible(target).
    auto extensible_target = m_target.is_extensible();
    if (vm.exception())
        return {};

    // 14. Else, let settingConfigFalse be false.
    bool setting_config_false = false;

    // 13. If Desc has a [[Configurable]] field and if Desc.[[Configurable]] is false, then
    if (property_descriptor.configurable.has_value() && !*property_descriptor.configurable) {
        // a. Let settingConfigFalse be true.
        setting_config_false = true;
    }

    // 15. If targetDesc is undefined, then
    if (!target_descriptor.has_value()) {
        // a. If extensibleTarget is false, throw a TypeError exception.
        if (!extensible_target) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ProxyDefinePropNonExtensible);
            return {};
        }
        // b. If settingConfigFalse is true, throw a TypeError exception.
        if (setting_config_false) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ProxyDefinePropNonConfigurableNonExisting);
            return {};
        }
    }
    // 16. Else,
    else {
        // a. If IsCompatiblePropertyDescriptor(extensibleTarget, Desc, targetDesc) is false, throw a TypeError exception.
        if (!is_compatible_property_descriptor(extensible_target, property_descriptor, target_descriptor)) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ProxyDefinePropIncompatibleDescriptor);
            return {};
        }
        // b. If settingConfigFalse is true and targetDesc.[[Configurable]] is true, throw a TypeError exception.
        if (setting_config_false && *target_descriptor->configurable) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ProxyDefinePropExistingConfigurable);
            return {};
        }
        // c. If IsDataDescriptor(targetDesc) is true, targetDesc.[[Configurable]] is false, and targetDesc.[[Writable]] is true, then
        if (target_descriptor->is_data_descriptor() && !*target_descriptor->configurable && *target_descriptor->writable) {
            // i. If Desc has a [[Writable]] field and Desc.[[Writable]] is false, throw a TypeError exception.
            if (property_descriptor.writable.has_value() && !*property_descriptor.writable) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ProxyDefinePropNonWritable);
                return {};
            }
        }
    }

    // 17. Return true.
    return true;
}

// 10.5.7 [[HasProperty]] ( P ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-hasproperty-p
bool ProxyObject::internal_has_property(PropertyName const& property_name) const
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_name.is_valid());

    // 2. Let handler be O.[[ProxyHandler]].

    // 3. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 4. Assert: Type(handler) is Object.
    // 5. Let target be O.[[ProxyTarget]].

    // 6. Let trap be ? GetMethod(handler, "has").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.has);
    if (vm.exception())
        return {};

    // 7. If trap is undefined, then
    if (!trap) {
        // a. Return ? target.[[HasProperty]](P).
        return m_target.internal_has_property(property_name);
    }

    // 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P »)).
    auto trap_result = vm.call(*trap, &m_handler, &m_target, property_name_to_value(vm, property_name));
    if (vm.exception())
        return {};

    // 9. If booleanTrapResult is false, then
    if (!trap_result.to_boolean()) {
        // a. Let targetDesc be ? target.[[GetOwnProperty]](P).
        auto target_descriptor = m_target.internal_get_own_property(property_name);
        if (vm.exception())
            return {};

        // b. If targetDesc is not undefined, then
        if (target_descriptor.has_value()) {
            // i. If targetDesc.[[Configurable]] is false, throw a TypeError exception.
            if (!*target_descriptor->configurable) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ProxyHasExistingNonConfigurable);
                return {};
            }

            // ii. Let extensibleTarget be ? IsExtensible(target).
            auto extensible_target = m_target.is_extensible();
            if (vm.exception())
                return {};

            // iii. If extensibleTarget is false, throw a TypeError exception.
            if (!extensible_target) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ProxyHasExistingNonExtensible);
                return false;
            }
        }
    }

    // 10. Return booleanTrapResult.
    return trap_result.to_boolean();
}

// 10.5.8 [[Get]] ( P, Receiver ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-get-p-receiver
Value ProxyObject::internal_get(PropertyName const& property_name, Value receiver) const
{
    VERIFY(!receiver.is_empty());

    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_name.is_valid());

    // 2. Let handler be O.[[ProxyHandler]].

    // 3. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 4. Assert: Type(handler) is Object.
    // 5. Let target be O.[[ProxyTarget]].

    // NOTE: We need to protect ourselves from a Proxy with the handler's prototype set to the
    // Proxy itself, which would by default bounce between these functions indefinitely and lead to
    // a stack overflow when the Proxy's (p) or Proxy handler's (h) Object::get() is called and the
    // handler doesn't have a `get` trap:
    //
    // 1. p -> ProxyObject::internal_get()  <- you are here
    // 2. h -> Value::get_method()
    // 3. h -> Value::get()
    // 4. h -> Object::internal_get()
    // 5. h -> Object::internal_get_prototype_of() (result is p)
    // 6. goto 1
    //
    // In JS code: `h = {}; p = new Proxy({}, h); h.__proto__ = p; p.foo // or h.foo`
    if (vm.did_reach_stack_space_limit()) {
        vm.throw_exception<Error>(global_object, ErrorType::CallStackSizeExceeded);
        return {};
    }

    // 6. Let trap be ? GetMethod(handler, "get").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.get);
    if (vm.exception())
        return {};

    // 7. If trap is undefined, then
    if (!trap) {
        // a. Return ? target.[[Get]](P, Receiver).
        return m_target.internal_get(property_name, receiver);
    }

    // 8. Let trapResult be ? Call(trap, handler, « target, P, Receiver »).
    auto trap_result = vm.call(*trap, &m_handler, &m_target, property_name_to_value(vm, property_name), receiver);
    if (vm.exception())
        return {};

    // 9. Let targetDesc be ? target.[[GetOwnProperty]](P).
    auto target_descriptor = m_target.internal_get_own_property(property_name);
    if (vm.exception())
        return {};

    // 10. If targetDesc is not undefined and targetDesc.[[Configurable]] is false, then
    if (target_descriptor.has_value() && !*target_descriptor->configurable) {
        // a. If IsDataDescriptor(targetDesc) is true and targetDesc.[[Writable]] is false, then
        if (target_descriptor->is_data_descriptor() && !*target_descriptor->writable) {
            // i. If SameValue(trapResult, targetDesc.[[Value]]) is false, throw a TypeError exception.
            if (!same_value(trap_result, *target_descriptor->value)) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ProxyGetImmutableDataProperty);
                return {};
            }
        }
        // b. If IsAccessorDescriptor(targetDesc) is true and targetDesc.[[Get]] is undefined, then
        if (target_descriptor->is_accessor_descriptor() && !*target_descriptor->get) {
            // i. If trapResult is not undefined, throw a TypeError exception.
            if (!trap_result.is_undefined()) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ProxyGetNonConfigurableAccessor);
                return {};
            }
        }
    }

    // 11. Return trapResult.
    return trap_result;
}

// 10.5.9 [[Set]] ( P, V, Receiver ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-set-p-v-receiver
bool ProxyObject::internal_set(PropertyName const& property_name, Value value, Value receiver)
{
    VERIFY(!value.is_empty());
    VERIFY(!receiver.is_empty());

    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_name.is_valid());

    // 2. Let handler be O.[[ProxyHandler]].

    // 3. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 4. Assert: Type(handler) is Object.
    // 5. Let target be O.[[ProxyTarget]].

    // 6. Let trap be ? GetMethod(handler, "set").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.set);
    if (vm.exception())
        return {};

    // 7. If trap is undefined, then
    if (!trap) {
        // a. Return ? target.[[Set]](P, V, Receiver).
        return m_target.internal_set(property_name, value, receiver);
    }

    // 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P, V, Receiver »)).
    auto trap_result = vm.call(*trap, &m_handler, &m_target, property_name_to_value(vm, property_name), value, receiver);
    if (vm.exception())
        return {};

    // 9. If booleanTrapResult is false, return false.
    if (!trap_result.to_boolean())
        return false;

    // 10. Let targetDesc be ? target.[[GetOwnProperty]](P).
    auto target_descriptor = m_target.internal_get_own_property(property_name);
    if (vm.exception())
        return {};

    // 11. If targetDesc is not undefined and targetDesc.[[Configurable]] is false, then
    if (target_descriptor.has_value() && !*target_descriptor->configurable) {
        // a. If IsDataDescriptor(targetDesc) is true and targetDesc.[[Writable]] is false, then
        if (target_descriptor->is_data_descriptor() && !*target_descriptor->writable) {
            // i. If SameValue(V, targetDesc.[[Value]]) is false, throw a TypeError exception.
            if (!same_value(value, *target_descriptor->value)) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ProxySetImmutableDataProperty);
                return {};
            }
        }
        // b. If IsAccessorDescriptor(targetDesc) is true, then
        if (target_descriptor->is_accessor_descriptor()) {
            // i. If targetDesc.[[Set]] is undefined, throw a TypeError exception.
            if (!*target_descriptor->set) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ProxySetNonConfigurableAccessor);
                return {};
            }
        }
    }

    // 12. Return true.
    return true;
}

// 10.5.10 [[Delete]] ( P ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-delete-p
bool ProxyObject::internal_delete(PropertyName const& property_name)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_name.is_valid());

    // 2. Let handler be O.[[ProxyHandler]].

    // 3. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 4. Assert: Type(handler) is Object.
    // 5. Let target be O.[[ProxyTarget]].

    // 6. Let trap be ? GetMethod(handler, "deleteProperty").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.deleteProperty);
    if (vm.exception())
        return {};

    // 7. If trap is undefined, then
    if (!trap) {
        // a. Return ? target.[[Delete]](P).
        return m_target.internal_delete(property_name);
    }

    // 8. Let booleanTrapResult be ! ToBoolean(? Call(trap, handler, « target, P »)).
    auto trap_result = vm.call(*trap, &m_handler, &m_target, property_name_to_value(vm, property_name));
    if (vm.exception())
        return {};

    // 9. If booleanTrapResult is false, return false.
    if (!trap_result.to_boolean())
        return false;

    // 10. Let targetDesc be ? target.[[GetOwnProperty]](P).
    auto target_descriptor = m_target.internal_get_own_property(property_name);
    if (vm.exception())
        return {};

    // 11. If targetDesc is undefined, return true.
    if (!target_descriptor.has_value())
        return true;

    // 12. If targetDesc.[[Configurable]] is false, throw a TypeError exception.
    if (!*target_descriptor->configurable) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyDeleteNonConfigurable);
        return {};
    }

    // 13. Let extensibleTarget be ? IsExtensible(target).
    auto extensible_target = m_target.is_extensible();
    if (vm.exception())
        return {};

    // 14. If extensibleTarget is false, throw a TypeError exception.
    if (!extensible_target) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyDeleteNonExtensible);
        return {};
    }

    // 15. Return true.
    return true;
}

// 10.5.11 [[OwnPropertyKeys]] ( ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-ownpropertykeys
MarkedValueList ProxyObject::internal_own_property_keys() const
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Let handler be O.[[ProxyHandler]].

    // 2. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return MarkedValueList { heap() };
    }

    // 3. Assert: Type(handler) is Object.
    // 4. Let target be O.[[ProxyTarget]].

    // 5. Let trap be ? GetMethod(handler, "ownKeys").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.ownKeys);
    if (vm.exception())
        return MarkedValueList { heap() };

    // 6. If trap is undefined, then
    if (!trap) {
        // a. Return ? target.[[OwnPropertyKeys]]().
        return m_target.internal_own_property_keys();
    }

    // 7. Let trapResultArray be ? Call(trap, handler, « target »).
    auto trap_result_array = vm.call(*trap, &m_handler, &m_target);
    if (vm.exception())
        return MarkedValueList { heap() };

    // 8. Let trapResult be ? CreateListFromArrayLike(trapResultArray, « String, Symbol »).
    HashTable<StringOrSymbol> unique_keys;
    auto throw_completion_or_trap_result = create_list_from_array_like(global_object, trap_result_array, [&](auto value) -> ThrowCompletionOr<void> {
        auto& vm = global_object.vm();
        if (!value.is_string() && !value.is_symbol())
            return vm.throw_completion<TypeError>(global_object, ErrorType::ProxyOwnPropertyKeysNotStringOrSymbol);
        auto property_key = value.to_property_key(global_object);
        VERIFY(!vm.exception());
        unique_keys.set(property_key, AK::HashSetExistingEntryBehavior::Keep);
        return {};
    });
    // TODO: This becomes a lot nicer once this function returns a ThrowCompletionOr as well.
    if (throw_completion_or_trap_result.is_throw_completion())
        return MarkedValueList { heap() };
    auto trap_result = throw_completion_or_trap_result.release_value();

    // 9. If trapResult contains any duplicate entries, throw a TypeError exception.
    if (unique_keys.size() != trap_result.size()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyOwnPropertyKeysDuplicates);
        return MarkedValueList { heap() };
    }

    // 10. Let extensibleTarget be ? IsExtensible(target).
    auto extensible_target = m_target.is_extensible();
    if (vm.exception())
        return MarkedValueList { heap() };

    // 11. Let targetKeys be ? target.[[OwnPropertyKeys]]().
    auto target_keys = m_target.internal_own_property_keys();
    if (vm.exception())
        return MarkedValueList { heap() };

    // 12. Assert: targetKeys is a List whose elements are only String and Symbol values.
    // 13. Assert: targetKeys contains no duplicate entries.

    // 14. Let targetConfigurableKeys be a new empty List.
    auto target_configurable_keys = MarkedValueList { heap() };

    // 15. Let targetNonconfigurableKeys be a new empty List.
    auto target_nonconfigurable_keys = MarkedValueList { heap() };

    // 16. For each element key of targetKeys, do
    for (auto& key : target_keys) {
        // a. Let desc be ? target.[[GetOwnProperty]](key).
        auto descriptor = m_target.internal_get_own_property(PropertyName::from_value(global_object, key));

        // b. If desc is not undefined and desc.[[Configurable]] is false, then
        if (descriptor.has_value() && !*descriptor->configurable) {
            // i. Append key as an element of targetNonconfigurableKeys.
            target_nonconfigurable_keys.append(key);
        }
        // c. Else,
        else {
            // i. Append key as an element of targetConfigurableKeys.
            target_configurable_keys.append(key);
        }
    }

    // 17. If extensibleTarget is true and targetNonconfigurableKeys is empty, then
    if (extensible_target && target_nonconfigurable_keys.is_empty()) {
        // a. Return trapResult.
        return trap_result;
    }

    // 18. Let uncheckedResultKeys be a List whose elements are the elements of trapResult.
    auto unchecked_result_keys = MarkedValueList { heap() };
    unchecked_result_keys.extend(trap_result);

    // 19. For each element key of targetNonconfigurableKeys, do
    for (auto& key : target_nonconfigurable_keys) {
        // a. If key is not an element of uncheckedResultKeys, throw a TypeError exception.
        if (!unchecked_result_keys.contains_slow(key)) {
            vm.throw_exception<TypeError>(global_object, ErrorType::FixmeAddAnErrorString);
            return MarkedValueList { heap() };
        }

        // b. Remove key from uncheckedResultKeys.
        unchecked_result_keys.remove_first_matching([&](auto& value) {
            return same_value(value, key);
        });
    }

    // 20. If extensibleTarget is true, return trapResult.
    if (extensible_target)
        return trap_result;

    // 21. For each element key of targetConfigurableKeys, do
    for (auto& key : target_configurable_keys) {
        // a. If key is not an element of uncheckedResultKeys, throw a TypeError exception.
        if (!unchecked_result_keys.contains_slow(key)) {
            vm.throw_exception<TypeError>(global_object, ErrorType::FixmeAddAnErrorString);
            return MarkedValueList { heap() };
        }

        // b. Remove key from uncheckedResultKeys.
        unchecked_result_keys.remove_first_matching([&](auto& value) {
            return same_value(value, key);
        });
    }

    // 22. If uncheckedResultKeys is not empty, throw a TypeError exception.
    if (!unchecked_result_keys.is_empty()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::FixmeAddAnErrorString);
        return MarkedValueList { heap() };
    }

    // 23. Return trapResult.
    return trap_result;
}

// 10.5.12 [[Call]] ( thisArgument, argumentsList ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-call-thisargument-argumentslist
Value ProxyObject::call()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();
    auto this_argument = vm.this_value(global_object);

    // A Proxy exotic object only has a [[Call]] internal method if the initial value of its [[ProxyTarget]] internal slot is an object that has a [[Call]] internal method.
    if (!is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, Value(this).to_string_without_side_effects());
        return {};
    }

    // 1. Let handler be O.[[ProxyHandler]].

    // 2. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 3. Assert: Type(handler) is Object.
    // 4. Let target be O.[[ProxyTarget]].

    // 5. Let trap be ? GetMethod(handler, "apply").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.apply);
    if (vm.exception())
        return {};

    // 6. If trap is undefined, then
    if (!trap) {
        // a. Return ? Call(target, thisArgument, argumentsList).
        return static_cast<FunctionObject&>(m_target).call();
    }

    // 7. Let argArray be ! CreateArrayFromList(argumentsList).
    auto arguments_array = Array::create(global_object, 0);
    vm.for_each_argument([&](auto& argument) {
        arguments_array->indexed_properties().append(argument);
    });

    // 8. Return ? Call(trap, handler, « target, thisArgument, argArray »).
    return vm.call(*trap, &m_handler, &m_target, this_argument, arguments_array);
}

// 10.5.13 [[Construct]] ( argumentsList, newTarget ), https://tc39.es/ecma262/#sec-proxy-object-internal-methods-and-internal-slots-construct-argumentslist-newtarget
Value ProxyObject::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // A Proxy exotic object only has a [[Construct]] internal method if the initial value of its [[ProxyTarget]] internal slot is an object that has a [[Construct]] internal method.
    if (!is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, Value(this).to_string_without_side_effects());
        return {};
    }

    // 1. Let handler be O.[[ProxyHandler]].

    // 2. If handler is null, throw a TypeError exception.
    if (m_is_revoked) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyRevoked);
        return {};
    }

    // 3. Assert: Type(handler) is Object.
    // 4. Let target be O.[[ProxyTarget]].
    // 5. Assert: IsConstructor(target) is true.

    // 6. Let trap be ? GetMethod(handler, "construct").
    auto trap = Value(&m_handler).get_method(global_object, vm.names.construct);
    if (vm.exception())
        return {};

    // 7. If trap is undefined, then
    if (!trap) {
        // a. Return ? Construct(target, argumentsList, newTarget).
        return static_cast<FunctionObject&>(m_target).construct(new_target);
    }

    // 8. Let argArray be ! CreateArrayFromList(argumentsList).
    auto arguments_array = Array::create(global_object, 0);
    vm.for_each_argument([&](auto& argument) {
        arguments_array->indexed_properties().append(argument);
    });

    // 9. Let newObj be ? Call(trap, handler, « target, argArray, newTarget »).
    auto result = vm.call(*trap, &m_handler, &m_target, arguments_array, &new_target);

    // 10. If Type(newObj) is not Object, throw a TypeError exception.
    if (!result.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ProxyConstructBadReturnType);
        return {};
    }

    // 11. Return newObj.
    return result;
}

void ProxyObject::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_target);
    visitor.visit(&m_handler);
}

const FlyString& ProxyObject::name() const
{
    VERIFY(is_function());
    return static_cast<FunctionObject&>(m_target).name();
}

FunctionEnvironment* ProxyObject::create_environment(FunctionObject& function_being_invoked)
{
    VERIFY(is_function());
    return static_cast<FunctionObject&>(m_target).create_environment(function_being_invoked);
}

}
