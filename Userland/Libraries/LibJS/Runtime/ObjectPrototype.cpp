/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/String.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/ObjectPrototype.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

ObjectPrototype::ObjectPrototype(GlobalObject& global_object)
    : Object(Object::ConstructWithoutPrototypeTag::Tag, global_object)
{
}

void ObjectPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    // This must be called after the constructor has returned, so that the below code
    // can find the ObjectPrototype through normal paths.
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.hasOwnProperty, has_own_property, 1, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
    define_native_function(vm.names.propertyIsEnumerable, property_is_enumerable, 1, attr);
    define_native_function(vm.names.isPrototypeOf, is_prototype_of, 1, attr);

    // Annex B
    define_native_function(vm.names.__defineGetter__, define_getter, 2, attr);
    define_native_function(vm.names.__defineSetter__, define_setter, 2, attr);
    define_native_function(vm.names.__lookupGetter__, lookup_getter, 1, attr);
    define_native_function(vm.names.__lookupSetter__, lookup_setter, 1, attr);
    define_native_accessor(vm.names.__proto__, proto_getter, proto_setter, Attribute::Configurable);
}

ObjectPrototype::~ObjectPrototype()
{
}

// 20.1.3.2 Object.prototype.hasOwnProperty ( V ), https://tc39.es/ecma262/#sec-object.prototype.hasownproperty
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::has_own_property)
{
    auto property_key = vm.argument(0).to_property_key(global_object);
    if (vm.exception())
        return {};
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    return Value(this_object->has_own_property(property_key));
}

// 20.1.3.6 Object.prototype.toString ( ), https://tc39.es/ecma262/#sec-object.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::to_string)
{
    auto this_value = vm.this_value(global_object);

    if (this_value.is_undefined())
        return js_string(vm, "[object Undefined]");
    if (this_value.is_null())
        return js_string(vm, "[object Null]");

    auto* this_object = this_value.to_object(global_object);
    VERIFY(this_object);

    String tag;
    auto to_string_tag = this_object->get(vm.well_known_symbol_to_string_tag());

    if (to_string_tag.is_string()) {
        tag = to_string_tag.as_string().string();
    } else if (this_object->is_array()) {
        tag = "Array";
    } else if (this_object->is_function()) {
        tag = "Function";
    } else if (is<Error>(this_object)) {
        tag = "Error";
    } else if (is<BooleanObject>(this_object)) {
        tag = "Boolean";
    } else if (is<NumberObject>(this_object)) {
        tag = "Number";
    } else if (is<StringObject>(this_object)) {
        tag = "String";
    } else if (is<Date>(this_object)) {
        tag = "Date";
    } else if (is<RegExpObject>(this_object)) {
        tag = "RegExp";
    } else {
        tag = "Object";
    }

    return js_string(vm, String::formatted("[object {}]", tag));
}

// 20.1.3.5 Object.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] ), https://tc39.es/ecma262/#sec-object.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::to_locale_string)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    return this_object->invoke(vm.names.toString.as_string());
}

// 20.1.3.7 Object.prototype.valueOf ( ), https://tc39.es/ecma262/#sec-object.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::value_of)
{
    return vm.this_value(global_object).to_object(global_object);
}

// 20.1.3.4 Object.prototype.propertyIsEnumerable ( V ), https://tc39.es/ecma262/#sec-object.prototype.propertyisenumerable
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::property_is_enumerable)
{
    auto property_key = vm.argument(0).to_property_key(global_object);
    if (vm.exception())
        return {};
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    auto property_descriptor = this_object->get_own_property_descriptor(property_key);
    if (!property_descriptor.has_value())
        return Value(false);
    return Value(property_descriptor.value().attributes.is_enumerable());
}

// 20.1.3.3 Object.prototype.isPrototypeOf ( V ), https://tc39.es/ecma262/#sec-object.prototype.isprototypeof
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::is_prototype_of)
{
    auto object_argument = vm.argument(0);
    if (!object_argument.is_object())
        return Value(false);
    auto* object = &object_argument.as_object();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    for (;;) {
        object = object->prototype();
        if (!object)
            return Value(false);
        if (same_value(this_object, object))
            return Value(true);
    }
}

// B.2.2.2 Object.prototype.__defineGetter__ ( P, getter ), https://tc39.es/ecma262/#sec-object.prototype.__defineGetter__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::define_getter)
{
    auto object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    auto getter = vm.argument(1);
    if (!getter.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, getter.to_string_without_side_effects());
        return {};
    }

    auto key = vm.argument(0).to_property_key(global_object);
    if (vm.exception())
        return {};

    auto descriptor = Object::create(global_object, global_object.object_prototype());
    descriptor->define_property(vm.names.get, getter);
    descriptor->define_property(vm.names.enumerable, Value(true));
    descriptor->define_property(vm.names.configurable, Value(true));

    auto success = object->define_property(key, *descriptor);
    if (vm.exception())
        return {};
    if (!success) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectDefinePropertyReturnedFalse);
        return {};
    }
    return js_undefined();
}

// B.2.2.3 Object.prototype.__defineSetter__ ( P, getter ), https://tc39.es/ecma262/#sec-object.prototype.__defineSetter__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::define_setter)
{
    auto object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    auto setter = vm.argument(1);
    if (!setter.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, setter.to_string_without_side_effects());
        return {};
    }

    auto key = vm.argument(0).to_property_key(global_object);
    if (vm.exception())
        return {};

    auto descriptor = Object::create(global_object, global_object.object_prototype());
    descriptor->define_property(vm.names.set, setter);
    descriptor->define_property(vm.names.enumerable, Value(true));
    descriptor->define_property(vm.names.configurable, Value(true));

    auto success = object->define_property(key, *descriptor);
    if (vm.exception())
        return {};
    if (!success) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectDefinePropertyReturnedFalse);
        return {};
    }
    return js_undefined();
}

// B.2.2.4 Object.prototype.__lookupGetter__ ( P ), https://tc39.es/ecma262/#sec-object.prototype.__lookupGetter__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::lookup_getter)
{
    auto object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    auto key = vm.argument(0).to_property_key(global_object);
    if (vm.exception())
        return {};

    while (object) {
        auto desc = object->get_own_property_descriptor(key);
        if (vm.exception())
            return {};
        if (desc.has_value())
            return desc->getter ?: js_undefined();
        object = object->prototype();
        if (vm.exception())
            return {};
    }

    return js_undefined();
}

// B.2.2.5 Object.prototype.__lookupSetter__ ( P ), https://tc39.es/ecma262/#sec-object.prototype.__lookupSetter__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::lookup_setter)
{
    auto* object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};

    auto key = vm.argument(0).to_property_key(global_object);
    if (vm.exception())
        return {};

    while (object) {
        auto desc = object->get_own_property_descriptor(key);
        if (vm.exception())
            return {};
        if (desc.has_value())
            return desc->setter ?: js_undefined();
        object = object->prototype();
        if (vm.exception())
            return {};
    }

    return js_undefined();
}

// B.2.2.1.1 get Object.prototype.__proto__, https://tc39.es/ecma262/#sec-get-object.prototype.__proto__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::proto_getter)
{
    auto object = vm.this_value(global_object).to_object(global_object);
    if (vm.exception())
        return {};
    auto proto = object->prototype();
    if (vm.exception())
        return {};
    return proto;
}

// B.2.2.1.2 set Object.prototype.__proto__, https://tc39.es/ecma262/#sec-set-object.prototype.__proto__
JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::proto_setter)
{
    auto object = require_object_coercible(global_object, vm.this_value(global_object));
    if (vm.exception())
        return {};

    auto proto = vm.argument(0);
    if (!proto.is_object() && !proto.is_null())
        return js_undefined();

    if (!object.is_object())
        return js_undefined();

    auto status = object.as_object().set_prototype(proto.is_object() ? &proto.as_object() : nullptr);
    if (vm.exception())
        return {};
    if (!status) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ObjectSetPrototypeOfReturnedFalse);
        return {};
    }
    return js_undefined();
}

}
