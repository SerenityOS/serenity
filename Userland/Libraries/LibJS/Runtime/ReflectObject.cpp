/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ReflectObject.h>

namespace JS {

static Object* get_target_object_from(GlobalObject& global_object, const String& name)
{
    auto& vm = global_object.vm();
    auto target = vm.argument(0);
    if (!target.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ReflectArgumentMustBeAnObject, name);
        return nullptr;
    }
    return static_cast<Object*>(&target.as_object());
}

static FunctionObject* get_target_function_from(GlobalObject& global_object, const String& name)
{
    auto& vm = global_object.vm();
    auto target = vm.argument(0);
    if (!target.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ReflectArgumentMustBeAFunction, name);
        return nullptr;
    }
    return &target.as_function();
}

ReflectObject::ReflectObject(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void ReflectObject::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.apply, apply, 3, attr);
    define_native_function(vm.names.construct, construct, 2, attr);
    define_native_function(vm.names.defineProperty, define_property, 3, attr);
    define_native_function(vm.names.deleteProperty, delete_property, 2, attr);
    define_native_function(vm.names.get, get, 2, attr);
    define_native_function(vm.names.getOwnPropertyDescriptor, get_own_property_descriptor, 2, attr);
    define_native_function(vm.names.getPrototypeOf, get_prototype_of, 1, attr);
    define_native_function(vm.names.has, has, 2, attr);
    define_native_function(vm.names.isExtensible, is_extensible, 1, attr);
    define_native_function(vm.names.ownKeys, own_keys, 1, attr);
    define_native_function(vm.names.preventExtensions, prevent_extensions, 1, attr);
    define_native_function(vm.names.set, set, 3, attr);
    define_native_function(vm.names.setPrototypeOf, set_prototype_of, 2, attr);

    // 28.1.14 Reflect [ @@toStringTag ], https://tc39.es/ecma262/#sec-reflect-@@tostringtag
    Object::define_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), vm.names.Reflect.as_string()), Attribute::Configurable);
}

ReflectObject::~ReflectObject()
{
}

// 28.1.1 Reflect.apply ( target, thisArgument, argumentsList ), https://tc39.es/ecma262/#sec-reflect.apply
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::apply)
{
    auto* target = get_target_function_from(global_object, "apply");
    if (!target)
        return {};
    auto this_arg = vm.argument(1);
    auto arguments = create_list_from_array_like(global_object, vm.argument(2));
    if (vm.exception())
        return {};
    return vm.call(*target, this_arg, move(arguments));
}

// 28.1.2 Reflect.construct ( target, argumentsList [ , newTarget ] ), https://tc39.es/ecma262/#sec-reflect.construct
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::construct)
{
    auto* target = get_target_function_from(global_object, "construct");
    if (!target)
        return {};
    auto arguments = create_list_from_array_like(global_object, vm.argument(1));
    if (vm.exception())
        return {};
    auto* new_target = target;
    if (vm.argument_count() > 2) {
        auto new_target_value = vm.argument(2);
        if (!new_target_value.is_function()
            || (is<NativeFunction>(new_target_value.as_object()) && !static_cast<NativeFunction&>(new_target_value.as_object()).has_constructor())) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ReflectBadNewTarget);
            return {};
        }
        new_target = &new_target_value.as_function();
    }
    return vm.construct(*target, *new_target, move(arguments));
}

// 28.1.3 Reflect.defineProperty ( target, propertyKey, attributes ), https://tc39.es/ecma262/#sec-reflect.defineproperty
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::define_property)
{
    auto* target = get_target_object_from(global_object, "defineProperty");
    if (!target)
        return {};
    auto property_key = vm.argument(1).to_property_key(global_object);
    if (vm.exception())
        return {};
    if (!vm.argument(2).is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ReflectBadDescriptorArgument);
        return {};
    }
    auto& descriptor = vm.argument(2).as_object();
    auto success = target->define_property(property_key, descriptor, false);
    if (vm.exception())
        return {};
    return Value(success);
}

// 28.1.4 Reflect.deleteProperty ( target, propertyKey ), https://tc39.es/ecma262/#sec-reflect.deleteproperty
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::delete_property)
{
    auto* target = get_target_object_from(global_object, "deleteProperty");
    if (!target)
        return {};
    auto property_key = vm.argument(1).to_property_key(global_object);
    if (vm.exception())
        return {};
    return Value(target->delete_property(property_key));
}

// 28.1.5 Reflect.get ( target, propertyKey [ , receiver ] ), https://tc39.es/ecma262/#sec-reflect.get
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::get)
{
    auto* target = get_target_object_from(global_object, "get");
    if (!target)
        return {};
    auto property_key = vm.argument(1).to_property_key(global_object);
    if (vm.exception())
        return {};
    Value receiver = {};
    if (vm.argument_count() > 2)
        receiver = vm.argument(2);
    return target->get(property_key, receiver).value_or(js_undefined());
}

// 28.1.6 Reflect.getOwnPropertyDescriptor ( target, propertyKey ), https://tc39.es/ecma262/#sec-reflect.getownpropertydescriptor
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::get_own_property_descriptor)
{
    auto* target = get_target_object_from(global_object, "getOwnPropertyDescriptor");
    if (!target)
        return {};
    auto property_key = vm.argument(1).to_property_key(global_object);
    if (vm.exception())
        return {};
    return target->get_own_property_descriptor_object(property_key);
}

// 28.1.7 Reflect.getPrototypeOf ( target ), https://tc39.es/ecma262/#sec-reflect.getprototypeof
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::get_prototype_of)
{
    auto* target = get_target_object_from(global_object, "getPrototypeOf");
    if (!target)
        return {};
    return target->prototype();
}

// 28.1.8 Reflect.has ( target, propertyKey ), https://tc39.es/ecma262/#sec-reflect.has
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::has)
{
    auto* target = get_target_object_from(global_object, "has");
    if (!target)
        return {};
    auto property_key = vm.argument(1).to_property_key(global_object);
    if (vm.exception())
        return {};
    return Value(target->has_property(property_key));
}

// 28.1.9 Reflect.isExtensible ( target ), https://tc39.es/ecma262/#sec-reflect.isextensible
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::is_extensible)
{
    auto* target = get_target_object_from(global_object, "isExtensible");
    if (!target)
        return {};
    return Value(target->is_extensible());
}

// 28.1.10 Reflect.ownKeys ( target ), https://tc39.es/ecma262/#sec-reflect.ownkeys
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::own_keys)
{
    auto* target = get_target_object_from(global_object, "ownKeys");
    if (!target)
        return {};
    return Array::create_from(global_object, target->get_own_properties(PropertyKind::Key));
}

// 28.1.11 Reflect.preventExtensions ( target ), https://tc39.es/ecma262/#sec-reflect.preventextensions
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::prevent_extensions)
{
    auto* target = get_target_object_from(global_object, "preventExtensions");
    if (!target)
        return {};
    return Value(target->prevent_extensions());
}

// 28.1.12 Reflect.set ( target, propertyKey, V [ , receiver ] ), https://tc39.es/ecma262/#sec-reflect.set
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::set)
{
    auto* target = get_target_object_from(global_object, "set");
    if (!target)
        return {};
    auto property_key = vm.argument(1).to_property_key(global_object);
    if (vm.exception())
        return {};
    auto value = vm.argument(2);
    Value receiver = {};
    if (vm.argument_count() > 3)
        receiver = vm.argument(3);
    return Value(target->put(property_key, value, receiver));
}

// 28.1.13 Reflect.setPrototypeOf ( target, proto ), https://tc39.es/ecma262/#sec-reflect.setprototypeof
JS_DEFINE_NATIVE_FUNCTION(ReflectObject::set_prototype_of)
{
    auto* target = get_target_object_from(global_object, "setPrototypeOf");
    if (!target)
        return {};
    auto prototype_value = vm.argument(1);
    if (!prototype_value.is_object() && !prototype_value.is_null()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectPrototypeWrongType);
        return {};
    }
    Object* prototype = nullptr;
    if (!prototype_value.is_null())
        prototype = const_cast<Object*>(&prototype_value.as_object());
    return Value(target->set_prototype(prototype));
}

}
