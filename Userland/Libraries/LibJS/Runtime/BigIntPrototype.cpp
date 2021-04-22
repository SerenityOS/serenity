/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
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

static BigIntObject* bigint_object_from(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!is<BigIntObject>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "BigInt");
        return nullptr;
    }
    return static_cast<BigIntObject*>(this_object);
}

JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::to_string)
{
    auto* bigint_object = bigint_object_from(vm, global_object);
    if (!bigint_object)
        return {};
    return js_string(vm, bigint_object->bigint().big_integer().to_base10());
}

JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::to_locale_string)
{
    return to_string(vm, global_object);
}

JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::value_of)
{
    auto* bigint_object = bigint_object_from(vm, global_object);
    if (!bigint_object)
        return {};
    return bigint_object->value_of();
}

}
