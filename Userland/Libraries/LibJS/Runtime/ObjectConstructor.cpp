/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ObjectConstructor.h>
#include <LibJS/Runtime/ProxyObject.h>
#include <LibJS/Runtime/Shape.h>

namespace JS {

ObjectConstructor::ObjectConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Object, *global_object.function_prototype())
{
}

void ObjectConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    define_property(vm.names.prototype, global_object.object_prototype(), 0);
    define_property(vm.names.length, Value(1), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.defineProperty, define_property_, 3, attr);
    define_native_function(vm.names.defineProperties, define_properties, 2, attr);
    define_native_function(vm.names.is, is, 2, attr);
    define_native_function(vm.names.getOwnPropertyDescriptor, get_own_property_descriptor, 2, attr);
    define_native_function(vm.names.getOwnPropertyNames, get_own_property_names, 1, attr);
    define_native_function(vm.names.getPrototypeOf, get_prototype_of, 1, attr);
    define_native_function(vm.names.setPrototypeOf, set_prototype_of, 2, attr);
    define_native_function(vm.names.isExtensible, is_extensible, 1, attr);
    define_native_function(vm.names.isFrozen, is_frozen, 1, attr);
    define_native_function(vm.names.isSealed, is_sealed, 1, attr);
    define_native_function(vm.names.preventExtensions, prevent_extensions, 1, attr);
    define_native_function(vm.names.freeze, freeze, 1, attr);
    define_native_function(vm.names.seal, seal, 1, attr);
    define_native_function(vm.names.keys, keys, 1, attr);
    define_native_function(vm.names.values, values, 1, attr);
    define_native_function(vm.names.entries, entries, 1, attr);
    define_native_function(vm.names.create, create, 2, attr);
    define_native_function(vm.names.hasOwn, has_own, 2, attr);
}

ObjectConstructor::~ObjectConstructor()
{
}

Value ObjectConstructor::call()
{
    auto value = vm().argument(0);
    if (value.is_nullish())
        return Object::create_empty(global_object());
    return value.to_object(global_object());
}

Value ObjectConstructor::construct(Function&)
{
    return call();
}

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::get_own_property_names)
{
    if (!vm.argument_count())
        return {};
    auto* object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    return Array::create_from(global_object, object->get_own_properties(PropertyKind::Key, false, GetOwnPropertyReturnType::StringOnly));
}

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::get_prototype_of)
{
    if (!vm.argument_count())
        return {};
    auto* object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    return object->prototype();
}

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::set_prototype_of)
{
    if (vm.argument_count() < 2) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectSetPrototypeOfTwoArgs);
        return {};
    }
    auto* object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    auto prototype_value = vm.argument(1);
    Object* prototype;
    if (prototype_value.is_null()) {
        prototype = nullptr;
    } else if (prototype_value.is_object()) {
        prototype = &prototype_value.as_object();
    } else {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectPrototypeWrongType);
        return {};
    }
    if (!object->set_prototype(prototype)) {
        if (!vm.exception())
            vm.throw_exception<TypeError>(global_object, ErrorType::ObjectSetPrototypeOfReturnedFalse);
        return {};
    }
    return object;
}

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::is_extensible)
{
    auto argument = vm.argument(0);
    if (!argument.is_object())
        return Value(false);
    return Value(argument.as_object().is_extensible());
}

// 20.1.2.15 Object.isFrozen, https://tc39.es/ecma262/#sec-object.isfrozen
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::is_frozen)
{
    auto argument = vm.argument(0);
    if (!argument.is_object())
        return Value(true);
    return Value(argument.as_object().test_integrity_level(Object::IntegrityLevel::Frozen));
}

// 20.1.2.16 Object.isSealed, https://tc39.es/ecma262/#sec-object.issealed
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::is_sealed)
{
    auto argument = vm.argument(0);
    if (!argument.is_object())
        return Value(true);
    return Value(argument.as_object().test_integrity_level(Object::IntegrityLevel::Sealed));
}

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::prevent_extensions)
{
    auto argument = vm.argument(0);
    if (!argument.is_object())
        return argument;
    auto status = argument.as_object().prevent_extensions();
    if (vm.exception())
        return {};
    if (!status) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectPreventExtensionsReturnedFalse);
        return {};
    }
    return argument;
}

// 20.1.2.6 Object.freeze, https://tc39.es/ecma262/#sec-object.freeze
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

// 20.1.2.20 Object.seal, https://tc39.es/ecma262/#sec-object.seal
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

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::get_own_property_descriptor)
{
    auto* object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    auto property_key = PropertyName::from_value(global_object, vm.argument(1));
    if (vm.exception())
        return {};
    return object->get_own_property_descriptor_object(property_key);
}

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::define_property_)
{
    if (!vm.argument(0).is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, "Object argument");
        return {};
    }
    auto property_key = vm.argument(1).to_property_key(global_object);
    if (vm.exception())
        return {};
    if (!vm.argument(2).is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, "Descriptor argument");
        return {};
    }
    auto& object = vm.argument(0).as_object();
    auto& descriptor = vm.argument(2).as_object();
    if (!object.define_property(property_key, descriptor)) {
        if (!vm.exception()) {
            if (AK::is<ProxyObject>(object)) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ObjectDefinePropertyReturnedFalse);
            } else {
                vm.throw_exception<TypeError>(global_object, ErrorType::NonExtensibleDefine, property_key.to_display_string());
            }
        }
        return {};
    }
    return &object;
}

// 20.1.2.3 Object.defineProperties, https://tc39.es/ecma262/#sec-object.defineproperties
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::define_properties)
{
    if (!vm.argument(0).is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, "Object argument");
        return {};
    }
    auto& object = vm.argument(0).as_object();
    auto properties = vm.argument(1);
    object.define_properties(properties);
    if (vm.exception())
        return {};
    return &object;
}

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::is)
{
    return Value(same_value(vm.argument(0), vm.argument(1)));
}

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::keys)
{
    if (!vm.argument_count()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ConvertUndefinedToObject);
        return {};
    }

    auto* obj_arg = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};

    return Array::create_from(global_object, obj_arg->get_enumerable_own_property_names(PropertyKind::Key));
}

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::values)
{
    if (!vm.argument_count()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ConvertUndefinedToObject);
        return {};
    }
    auto* obj_arg = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};

    return Array::create_from(global_object, obj_arg->get_enumerable_own_property_names(PropertyKind::Value));
}

JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::entries)
{
    if (!vm.argument_count()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ConvertUndefinedToObject);
        return {};
    }
    auto* obj_arg = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};

    return Array::create_from(global_object, obj_arg->get_enumerable_own_property_names(PropertyKind::KeyAndValue));
}

// 20.1.2.2 Object.create, https://tc39.es/ecma262/#sec-object.create
JS_DEFINE_NATIVE_FUNCTION(ObjectConstructor::create)
{
    auto prototype_value = vm.argument(0);
    auto properties = vm.argument(1);

    Object* prototype;
    if (prototype_value.is_null()) {
        prototype = nullptr;
    } else if (prototype_value.is_object()) {
        prototype = &prototype_value.as_object();
    } else {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectPrototypeWrongType);
        return {};
    }

    auto* object = Object::create_empty(global_object);
    object->set_prototype(prototype);

    if (!properties.is_undefined()) {
        object->define_properties(properties);
        if (vm.exception())
            return {};
    }
    return object;
}

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

}
