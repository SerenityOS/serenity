/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/BigIntPrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BigIntPrototype::BigIntPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void BigIntPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);

    define_property(vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "BigInt"), Attribute::Configurable);
}

BigIntPrototype::~BigIntPrototype()
{
}

// thisBigIntValue, https://tc39.es/ecma262/#thisbigintvalue
static Value this_bigint_value(GlobalObject& global_object, Value value)
{
    if (value.is_bigint())
        return value;
    if (value.is_object() && is<BigIntObject>(value.as_object()))
        return static_cast<BigIntObject&>(value.as_object()).value_of();
    auto& vm = global_object.vm();
    vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "BigInt");
    return {};
}

JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::to_string)
{
    auto bigint_value = this_bigint_value(global_object, vm.this_value(global_object));
    if (vm.exception())
        return {};
    // FIXME: Support radix argument
    return js_string(vm, bigint_value.as_bigint().big_integer().to_base10());
}

JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::to_locale_string)
{
    return to_string(vm, global_object);
}

JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::value_of)
{
    return this_bigint_value(global_object, vm.this_value(global_object));
}

}
