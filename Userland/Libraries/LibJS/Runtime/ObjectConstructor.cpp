/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/ObjectConstructor.h>
#include <LibJS/Runtime/ProxyObject.h>
#include <LibJS/Runtime/Shape.h>

namespace JS {

ObjectConstructor::ObjectConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Object.as_string(), *global_object.function_prototype())
{
}

void ObjectConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 20.1.2.19 Object.prototype, https://tc39.es/ecma262/#sec-object.prototype
    define_property(vm.names.prototype, global_object.object_prototype(), 0);

    define_property(vm.names.length, Value(1), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.defineProperty, define_property_, 3, attr);
    define_native_function(vm.names.defineProperties, define_properties, 2, attr);
    define_native_function(vm.names.is, is, 2, attr);
    define_native_function(vm.names.getOwnPropertyDescriptor, get_own_property_descriptor, 2, attr);
    define_native_function(vm.names.getOwnPropertyNames, get_own_property_names, 1, attr);
    define_native_function(vm.names.getOwnPropertySymbols, get_own_property_symbols, 1, attr);
    define_native_function(vm.names.getPrototypeOf, get_prototype_of, 1, attr);
    define_native_function(vm.names.setPrototypeOf, set_prototype_of, 2, attr);
    define_native_function(vm.names.isExtensible, is_extensible, 1, attr);
    define_native_function(vm.names.isFrozen, is_frozen, 1, attr);
    define_native_function(vm.names.isSealed, is_sealed, 1, attr);
    define_native_function(vm.names.preventExtensions, prevent_extensions, 1, attr);
    define_native_function(vm.names.freeze, freeze, 1, attr);
    define_native_function(vm.names.fromEntries, from_entries, 1, attr);
    define_native_function(vm.names.seal, seal, 1, attr);
    define_native_function(vm.names.keys, keys, 1, attr);
    define_native_function(vm.names.values, values, 1, attr);
    define_native_function(vm.names.entries, entries, 1, attr);
    define_native_function(vm.names.create, create, 2, attr);
    define_native_function(vm.names.hasOwn, has_own, 2, attr);
    define_native_function(vm.names.assign, assign, 2, attr);
}

ObjectConstructor::~ObjectConstructor()
{
}

// 20.1.1.1 Object ( [ value ] ), https://tc39.es/ecma262/#sec-object-value
Value ObjectConstructor::call()
{
    return construct(*this);
}

// 20.1.1.1 Object ( [ value ] ), https://tc39.es/ecma262/#sec-object-value
Value ObjectConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    if (&new_target != this)
        return ordinary_create_from_constructor<Object>(global_object, new_target, &GlobalObject::object_prototype);
    auto value = vm.argument(0);
    if (value.is_nullish())
        return Object::create(global_object, global_object.object_prototype());
    return value.to_object(global_object);
}

enum class GetOwnPropertyKeysType {
    String,
    Symbol,
};

// 20.1.2.11.1 GetOwnPropertyKeys ( O, type ), https://tc39.es/ecma262/#sec-getownpropertykeys
static Array* get_own_property_keys(GlobalObject& global_object, Value value, GetOwnPropertyKeysType type)
{
    auto& vm = global_object.vm();

    // 1. Let obj be ? ToObject(O).
    auto* object = value.to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Let keys be ? obj.[[OwnPropertyKeys]]().
    auto keys = object->internal_own_property_keys();
    if (vm.exception())
        return {};

    // 3. Let nameList be a new empty List.
    auto name_list = MarkedValueList { vm.heap() };

    // 4. For each element nextKey of keys, do
    for (auto& next_key : keys) {
        // a. If Type(nextKey) is Symbol and type is symbol or Type(nextKey) is String and type is string, then
        if ((next_key.is_symbol() && type == GetOwnPropertyKeysType::Symbol) || (next_key.is_string() && type == GetOwnPropertyKeysType::String)) {
            // i. Append nextKey as the last element of nameList.
            name_list.append(next_key);
        }
    }

    // 5. Return CreateArrayFromList(nameList).
    return Array::create_from(global_object, name_list);
}

// 20.1.2.10 Object.getOwnPropertyNames ( O ), https://tc39.es/ecma262/#sec-object.getownpropertynames
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::get_own_property_names)
{
    // 1. Return ? GetOwnPropertyKeys(O, string).
    return get_own_property_keys(global_object, vm.argument(0), GetOwnPropertyKeysType::String);
}

// 20.1.2.11 Object.getOwnPropertySymbols ( O ), https://tc39.es/ecma262/#sec-object.getownpropertysymbols
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::get_own_property_symbols)
{
    // 1. Return ? GetOwnPropertyKeys(O, symbol).
    return get_own_property_keys(global_object, vm.argument(0), GetOwnPropertyKeysType::Symbol);
}

// 20.1.2.12 Object.getPrototypeOf ( O ), https://tc39.es/ecma262/#sec-object.getprototypeof
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::get_prototype_of)
{
    // 1. Let obj be ? ToObject(O).
    auto* object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};

    // 2. Return ? obj.[[GetPrototypeOf]]().
    return object->internal_get_prototype_of();
}

// 20.1.2.21 Object.setPrototypeOf ( O, proto ), https://tc39.es/ecma262/#sec-object.setprototypeof
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::set_prototype_of)
{
    auto proto = vm.argument(1);

    // 1. Set O to ? RequireObjectCoercible(O).
    auto object = require_object_coercible(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 2. If Type(proto) is neither Object nor Null, throw a TypeError exception.
    if (!proto.is_object() && !proto.is_null()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectPrototypeWrongType);
        return {};
    }

    // 3. If Type(O) is not Object, return O.
    if (!object.is_object())
        return object;

    // 4. Let status be ? O.[[SetPrototypeOf]](proto).
    auto status = object.as_object().internal_set_prototype_of(proto.is_null() ? nullptr : &proto.as_object());
    if (vm.exception())
        return {};

    // 5. If status is false, throw a TypeError exception.
    if (!status) {
        // FIXME: Improve/contextualize error message
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectSetPrototypeOfReturnedFalse);
        return {};
    }

    // 6. Return O.
    return object;
}

// 20.1.2.14 Object.isExtensible ( O ), https://tc39.es/ecma262/#sec-object.isextensible
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::is_extensible)
{
    auto argument = vm.argument(0);
    if (!argument.is_object())
        return Value(false);
    return Value(argument.as_object().is_extensible());
}

// 20.1.2.15 Object.isFrozen ( O ), https://tc39.es/ecma262/#sec-object.isfrozen
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::is_frozen)
{
    auto argument = vm.argument(0);
    if (!argument.is_object())
        return Value(true);
    return Value(argument.as_object().test_integrity_level(Object::IntegrityLevel::Frozen));
}

// 20.1.2.16 Object.isSealed ( O ), https://tc39.es/ecma262/#sec-object.issealed
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::is_sealed)
{
    auto argument = vm.argument(0);
    if (!argument.is_object())
        return Value(true);
    return Value(argument.as_object().test_integrity_level(Object::IntegrityLevel::Sealed));
}

// 20.1.2.18 Object.preventExtensions ( O ), https://tc39.es/ecma262/#sec-object.preventextensions
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::prevent_extensions)
{
    auto argument = vm.argument(0);
    if (!argument.is_object())
        return argument;
    auto status = argument.as_object().internal_prevent_extensions();
    if (vm.exception())
        return {};
    if (!status) {
        // FIXME: Improve/contextualize error message
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectPreventExtensionsReturnedFalse);
        return {};
    }
    return argument;
}

// 20.1.2.6 Object.freeze ( O ), https://tc39.es/ecma262/#sec-object.freeze
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::freeze)
{
    auto argument = vm.argument(0);
    if (!argument.is_object())
        return argument;
    auto status = argument.as_object().set_integrity_level(Object::IntegrityLevel::Frozen);
    if (vm.exception())
        return {};
    if (!status) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectFreezeFailed);
        return {};
    }
    return argument;
}

// 20.1.2.7 Object.fromEntries ( iterable ), https://tc39.es/ecma262/#sec-object.fromentries
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::from_entries)
{
    auto iterable = require_object_coercible(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    auto* object = Object::create(global_object, global_object.object_prototype());

    get_iterator_values(global_object, iterable, [&](Value iterator_value) {
        if (vm.exception())
            return IterationDecision::Break;
        if (!iterator_value.is_object()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, String::formatted("Iterator value {}", iterator_value.to_string_without_side_effects()));
            return IterationDecision::Break;
        }
        auto key = iterator_value.as_object().get(0);
        if (vm.exception())
            return IterationDecision::Break;
        auto value = iterator_value.as_object().get(1);
        if (vm.exception())
            return IterationDecision::Break;
        auto property_key = key.to_property_key(global_object);
        if (vm.exception())
            return IterationDecision::Break;
        object->define_property(property_key, value);
        if (vm.exception())
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
    if (vm.exception())
        return {};
    return object;
}

// 20.1.2.20 Object.seal ( O ), https://tc39.es/ecma262/#sec-object.seal
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::seal)
{
    auto argument = vm.argument(0);
    if (!argument.is_object())
        return argument;
    auto status = argument.as_object().set_integrity_level(Object::IntegrityLevel::Sealed);
    if (vm.exception())
        return {};
    if (!status) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectSealFailed);
        return {};
    }
    return argument;
}

// 20.1.2.8 Object.getOwnPropertyDescriptor ( O, P ), https://tc39.es/ecma262/#sec-object.getownpropertydescriptor
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::get_own_property_descriptor)
{
    auto* object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    auto key = vm.argument(1).to_property_key(global_object);
    if (vm.exception())
        return {};
    auto descriptor = object->internal_get_own_property(key);
    if (vm.exception())
        return {};
    return from_property_descriptor(global_object, descriptor);
}

// 20.1.2.4 Object.defineProperty ( O, P, Attributes ), https://tc39.es/ecma262/#sec-object.defineproperty
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::define_property_)
{
    if (!vm.argument(0).is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, vm.argument(0).to_string_without_side_effects());
        return {};
    }
    auto key = vm.argument(1).to_property_key(global_object);
    if (vm.exception())
        return {};
    auto descriptor = to_property_descriptor(global_object, vm.argument(2));
    if (vm.exception())
        return {};
    vm.argument(0).as_object().define_property_or_throw(key, descriptor);
    if (vm.exception())
        return {};
    return vm.argument(0);
}

// 20.1.2.3 Object.defineProperties ( O, Properties ), https://tc39.es/ecma262/#sec-object.defineproperties
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::define_properties)
{
    auto object = vm.argument(0);
    auto properties = vm.argument(1);

    // 1. If Type(O) is not Object, throw a TypeError exception.
    if (!object.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, "Object argument");
        return {};
    }

    // 2. Return ? ObjectDefineProperties(O, Properties).
    return object.as_object().define_properties(properties);
}

// 20.1.2.13 Object.is ( value1, value2 ), https://tc39.es/ecma262/#sec-object.is
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::is)
{
    return Value(same_value(vm.argument(0), vm.argument(1)));
}

// 20.1.2.17 Object.keys ( O ), https://tc39.es/ecma262/#sec-object.keys
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::keys)
{
    auto* object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    auto name_list = object->enumerable_own_property_names(PropertyKind::Key);
    if (vm.exception())
        return {};
    return Array::create_from(global_object, name_list);
}

// 20.1.2.22 Object.values ( O ), https://tc39.es/ecma262/#sec-object.values
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::values)
{
    auto* object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    auto name_list = object->enumerable_own_property_names(PropertyKind::Value);
    if (vm.exception())
        return {};
    return Array::create_from(global_object, name_list);
}

// 20.1.2.5 Object.entries ( O ), https://tc39.es/ecma262/#sec-object.entries
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::entries)
{
    auto* object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    auto name_list = object->enumerable_own_property_names(PropertyKind::KeyAndValue);
    if (vm.exception())
        return {};
    return Array::create_from(global_object, name_list);
}

// 20.1.2.2 Object.create ( O, Properties ), https://tc39.es/ecma262/#sec-object.create
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::create)
{
    auto proto = vm.argument(0);
    auto properties = vm.argument(1);

    // 1. If Type(O) is neither Object nor Null, throw a TypeError exception.
    if (!proto.is_object() && !proto.is_null()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectPrototypeWrongType);
        return {};
    }

    // 2. Let obj be ! OrdinaryObjectCreate(O).
    auto* object = Object::create(global_object, proto.is_null() ? nullptr : &proto.as_object());

    // 3. If Properties is not undefined, then
    if (!properties.is_undefined()) {
        // a. Return ? ObjectDefineProperties(obj, Properties).
        return object->define_properties(properties);
    }

    // 4. Return obj.
    return object;
}

// 1 Object.hasOwn ( O, P ), https://tc39.es/proposal-accessible-object-hasownproperty/#sec-object.hasown
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::has_own)
{
    auto* object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    auto property_key = vm.argument(1).to_property_key(global_object);
    if (vm.exception())
        return {};
    return Value(object->has_own_property(property_key));
}

// 20.1.2.1 Object.assign ( target, ...sources ), https://tc39.es/ecma262/#sec-object.assign
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::assign)
{
    auto* to = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    if (vm.argument_count() == 1)
        return to;
    for (size_t i = 1; i < vm.argument_count(); ++i) {
        auto next_source = vm.argument(i);
        if (next_source.is_nullish())
            continue;
        auto from = next_source.to_object(global_object);
        VERIFY(!vm.exception());
        auto keys = from->internal_own_property_keys();
        if (vm.exception())
            return {};
        for (auto& next_key : keys) {
            auto property_name = PropertyName::from_value(global_object, next_key);
            auto property_descriptor = from->internal_get_own_property(property_name);
            if (!property_descriptor.has_value() || !*property_descriptor->enumerable)
                continue;
            auto prop_value = from->get(property_name);
            if (vm.exception())
                return {};
            to->set(property_name, prop_value, true);
            if (vm.exception())
                return {};
        }
    }
    return to;
}

}
