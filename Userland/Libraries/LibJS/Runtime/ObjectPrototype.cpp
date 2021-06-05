/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/String.h>
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
}

ObjectPrototype::~ObjectPrototype()
{
}

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

JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::to_string)
{
    auto this_value = vm.this_value(global_object);

    if (this_value.is_undefined())
        return js_string(vm, "[object Undefined]");
    if (this_value.is_null())
        return js_string(vm, "[object Null]");

    auto* this_object = this_value.to_object(global_object);
    if (!this_object)
        return {};

    String tag;
    auto to_string_tag = this_object->get(global_object.vm().well_known_symbol_to_string_tag());

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

JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::to_locale_string)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    return this_object->invoke(vm.names.toString);
}

JS_DEFINE_NATIVE_FUNCTION(ObjectPrototype::value_of)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    return this_object->value_of();
}

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

}
