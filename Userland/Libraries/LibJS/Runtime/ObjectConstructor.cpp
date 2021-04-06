/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <mail@linusgroh.de>
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
    define_native_function(vm.names.is, is, 2, attr);
    define_native_function(vm.names.getOwnPropertyDescriptor, get_own_property_descriptor, 2, attr);
    define_native_function(vm.names.getOwnPropertyNames, get_own_property_names, 1, attr);
    define_native_function(vm.names.getPrototypeOf, get_prototype_of, 1, attr);
    define_native_function(vm.names.setPrototypeOf, set_prototype_of, 2, attr);
    define_native_function(vm.names.isExtensible, is_extensible, 1, attr);
    define_native_function(vm.names.preventExtensions, prevent_extensions, 1, attr);
    define_native_function(vm.names.freeze, freeze, 1, attr);
    define_native_function(vm.names.seal, seal, 1, attr);
    define_native_function(vm.names.keys, keys, 1, attr);
    define_native_function(vm.names.values, values, 1, attr);
    define_native_function(vm.names.entries, entries, 1, attr);
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
    auto* result = Array::create(global_object);
    for (auto& entry : object->indexed_properties())
        result->indexed_properties().append(js_string(vm, String::number(entry.index())));
    for (auto& it : object->shape().property_table_ordered()) {
        if (!it.key.is_string())
            continue;
        result->indexed_properties().append(js_string(vm, it.key.as_string()));
    }

    return result;
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
    if (!vm.argument(2).is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, "Descriptor argument");
        return {};
    }
    auto& object = vm.argument(0).as_object();
    auto property_key = StringOrSymbol::from_value(global_object, vm.argument(1));
    if (vm.exception())
        return {};
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

}
