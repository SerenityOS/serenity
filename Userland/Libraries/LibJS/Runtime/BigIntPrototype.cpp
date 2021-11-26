/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/BigIntPrototype.h>
#include <LibJS/Runtime/Completion.h>
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

    // 21.2.3.5 BigInt.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-bigint.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.BigInt.as_string()), Attribute::Configurable);
}

BigIntPrototype::~BigIntPrototype()
{
}

// thisBigIntValue ( value ), https://tc39.es/ecma262/#thisbigintvalue
static ThrowCompletionOr<BigInt*> this_bigint_value(GlobalObject& global_object, Value value)
{
    if (value.is_bigint())
        return &value.as_bigint();
    if (value.is_object() && is<BigIntObject>(value.as_object()))
        return &static_cast<BigIntObject&>(value.as_object()).bigint();
    auto& vm = global_object.vm();
    return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "BigInt");
}

// 21.2.3.3 BigInt.prototype.toString ( [ radix ] ), https://tc39.es/ecma262/#sec-bigint.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::to_string)
{
    auto* bigint = TRY(this_bigint_value(global_object, vm.this_value(global_object)));
    double radix = 10;
    if (!vm.argument(0).is_undefined()) {
        radix = TRY(vm.argument(0).to_integer_or_infinity(global_object));
        if (radix < 2 || radix > 36)
            return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidRadix);
    }
    return js_string(vm, bigint->big_integer().to_base(radix));
}

// 21.2.3.2 BigInt.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] ), https://tc39.es/ecma262/#sec-bigint.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::to_locale_string)
{
    return to_string(vm, global_object);
}

// 21.2.3.4 BigInt.prototype.valueOf ( ), https://tc39.es/ecma262/#sec-bigint.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(BigIntPrototype::value_of)
{
    return TRY(this_bigint_value(global_object, vm.this_value(global_object)));
}

}
