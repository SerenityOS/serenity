/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
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

static Function* get_target_function_from(GlobalObject& global_object, const String& name)
{
    auto& vm = global_object.vm();
    auto target = vm.argument(0);
    if (!target.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ReflectArgumentMustBeAFunction, name);
        return nullptr;
    }
    return &target.as_function();
}

static void prepare_arguments_list(GlobalObject& global_object, Value value, MarkedValueList& arguments)
{
    auto& vm = global_object.vm();
    if (!value.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ReflectBadArgumentsList);
        return;
    }
    auto& arguments_list = value.as_object();
    auto length = length_of_array_like(global_object, arguments_list);
    if (vm.exception())
        return;
    for (size_t i = 0; i < length; ++i) {
        auto element = arguments_list.get(String::number(i));
        if (vm.exception())
            return;
        arguments.append(element.value_or(js_undefined()));
    }
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
}

ReflectObject::~ReflectObject()
{
}

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::apply)
{
    auto* target = get_target_function_from(global_object, "apply");
    if (!target)
        return {};
    auto this_arg = vm.argument(1);
    MarkedValueList arguments(vm.heap());
    prepare_arguments_list(global_object, vm.argument(2), arguments);
    if (vm.exception())
        return {};
    return vm.call(*target, this_arg, move(arguments));
}

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::construct)
{
    auto* target = get_target_function_from(global_object, "construct");
    if (!target)
        return {};
    MarkedValueList arguments(vm.heap());
    prepare_arguments_list(global_object, vm.argument(1), arguments);
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
    return vm.construct(*target, *new_target, move(arguments), global_object);
}

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

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::delete_property)
{
    auto* target = get_target_object_from(global_object, "deleteProperty");
    if (!target)
        return {};

    auto property_key = vm.argument(1);
    auto property_name = PropertyName::from_value(global_object, property_key);
    if (vm.exception())
        return {};
    auto property_key_number = property_key.to_number(global_object);
    if (vm.exception())
        return {};
    if (property_key_number.is_finite_number()) {
        auto property_key_as_double = property_key_number.as_double();
        if (property_key_as_double >= 0 && (i32)property_key_as_double == property_key_as_double)
            property_name = PropertyName(property_key_as_double);
    }
    return Value(target->delete_property(property_name));
}

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::get)
{
    auto* target = get_target_object_from(global_object, "get");
    if (!target)
        return {};
    auto property_key = PropertyName::from_value(global_object, vm.argument(1));
    if (vm.exception())
        return {};
    Value receiver = {};
    if (vm.argument_count() > 2)
        receiver = vm.argument(2);
    return target->get(property_key, receiver).value_or(js_undefined());
}

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::get_own_property_descriptor)
{
    auto* target = get_target_object_from(global_object, "getOwnPropertyDescriptor");
    if (!target)
        return {};
    auto property_key = PropertyName::from_value(global_object, vm.argument(1));
    if (vm.exception())
        return {};
    return target->get_own_property_descriptor_object(property_key);
}

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::get_prototype_of)
{
    auto* target = get_target_object_from(global_object, "getPrototypeOf");
    if (!target)
        return {};
    return target->prototype();
}

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::has)
{
    auto* target = get_target_object_from(global_object, "has");
    if (!target)
        return {};
    auto property_key = PropertyName::from_value(global_object, vm.argument(1));
    if (vm.exception())
        return {};
    return Value(target->has_property(property_key));
}

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::is_extensible)
{
    auto* target = get_target_object_from(global_object, "isExtensible");
    if (!target)
        return {};
    return Value(target->is_extensible());
}

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::own_keys)
{
    auto* target = get_target_object_from(global_object, "ownKeys");
    if (!target)
        return {};
    return Array::create_from(global_object, target->get_own_properties(PropertyKind::Key));
}

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::prevent_extensions)
{
    auto* target = get_target_object_from(global_object, "preventExtensions");
    if (!target)
        return {};
    return Value(target->prevent_extensions());
}

JS_DEFINE_NATIVE_FUNCTION(ReflectObject::set)
{
    auto* target = get_target_object_from(global_object, "set");
    if (!target)
        return {};
    auto property_key = vm.argument(1).to_string(global_object);
    if (vm.exception())
        return {};
    auto value = vm.argument(2);
    Value receiver = {};
    if (vm.argument_count() > 3)
        receiver = vm.argument(3);
    return Value(target->put(property_key, value, receiver));
}

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
