/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Runtime/BigIntConstructor.h>
#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

BigIntConstructor::BigIntConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.BigInt, *global_object.function_prototype())
{
}

void BigIntConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 21.2.2.3 BigInt.prototype, https://tc39.es/ecma262/#sec-bigint.prototype
    define_property(vm.names.prototype, global_object.bigint_prototype(), 0);

    define_property(vm.names.length, Value(1), Attribute::Configurable);

    // TODO: Implement these functions below and uncomment this.
    // u8 attr = Attribute::Writable | Attribute::Configurable;
    // define_native_function(vm.names.asIntN, as_int_n, 2, attr);
    // define_native_function(vm.names.asUintN, as_uint_n, 2, attr);
}

BigIntConstructor::~BigIntConstructor()
{
}

// 21.2.1.1 BigInt ( value ), https://tc39.es/ecma262/#sec-bigint-constructor-number-value
Value BigIntConstructor::call()
{
    auto primitive = vm().argument(0).to_primitive(global_object(), Value::PreferredType::Number);
    if (vm().exception())
        return {};
    if (primitive.is_number()) {
        if (!primitive.is_integral_number()) {
            vm().throw_exception<RangeError>(global_object(), ErrorType::BigIntIntArgument);
            return {};
        }
        return js_bigint(heap(), Crypto::SignedBigInteger { primitive.as_i32() });
    }
    auto* bigint = vm().argument(0).to_bigint(global_object());
    if (vm().exception())
        return {};
    return bigint;
}

// 21.2.1.1 BigInt ( value ), https://tc39.es/ecma262/#sec-bigint-constructor-number-value
Value BigIntConstructor::construct(Function&)
{
    vm().throw_exception<TypeError>(global_object(), ErrorType::NotAConstructor, "BigInt");
    return {};
}

// 21.2.2.1 BigInt.asIntN ( bits, bigint ), https://tc39.es/ecma262/#sec-bigint.asintn
JS_DEFINE_NATIVE_FUNCTION(BigIntConstructor::as_int_n)
{
    TODO();
}

// 21.2.2.2 BigInt.asUintN ( bits, bigint ), https://tc39.es/ecma262/#sec-bigint.asuintn
JS_DEFINE_NATIVE_FUNCTION(BigIntConstructor::as_uint_n)
{
    TODO();
}

}
