/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/BooleanPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BooleanPrototype::BooleanPrototype(GlobalObject& global_object)
    : BooleanObject(false, *global_object.object_prototype())
{
}

void BooleanPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    BooleanObject::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

BooleanPrototype::~BooleanPrototype()
{
}

// 20.3.3.2 Boolean.prototype.toString ( ), https://tc39.es/ecma262/#sec-boolean.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(BooleanPrototype::to_string)
{
    auto this_value = vm.this_value(global_object);
    if (this_value.is_boolean())
        return js_string(vm, this_value.as_bool() ? "true" : "false");
    if (!this_value.is_object() || !is<BooleanObject>(this_value.as_object())) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Boolean");
        return {};
    }

    bool bool_value = static_cast<BooleanObject const&>(this_value.as_object()).value_of().as_bool();
    return js_string(vm, bool_value ? "true" : "false");
}

// 20.3.3.3 Boolean.prototype.valueOf ( ), https://tc39.es/ecma262/#sec-boolean.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(BooleanPrototype::value_of)
{
    auto this_value = vm.this_value(global_object);
    if (this_value.is_boolean())
        return this_value;
    if (!this_value.is_object() || !is<BooleanObject>(this_value.as_object())) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Boolean");
        return {};
    }

    return static_cast<BooleanObject const&>(this_value.as_object()).value_of();
}
}
