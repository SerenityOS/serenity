/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/BigIntConstructor.h>
#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

BigIntConstructor::BigIntConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.BigInt.as_string(), *global_object.function_prototype())
{
}

void BigIntConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 21.2.2.3 BigInt.prototype, https://tc39.es/ecma262/#sec-bigint.prototype
    define_direct_property(vm.names.prototype, global_object.bigint_prototype(), 0);

    // TODO: Implement these functions below and uncomment this.
    // u8 attr = Attribute::Writable | Attribute::Configurable;
    // define_native_function(vm.names.asIntN, as_int_n, 2, attr);
    // define_native_function(vm.names.asUintN, as_uint_n, 2, attr);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

BigIntConstructor::~BigIntConstructor()
{
}

// 21.2.1.1 BigInt ( value ), https://tc39.es/ecma262/#sec-bigint-constructor-number-value
ThrowCompletionOr<Value> BigIntConstructor::call()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto value = vm.argument(0);

    // 2. Let prim be ? ToPrimitive(value, number).
    auto primitive = TRY(value.to_primitive(global_object, Value::PreferredType::Number));

    // 3. If Type(prim) is Number, return ? NumberToBigInt(prim).
    if (primitive.is_number())
        return TRY(number_to_bigint(global_object, primitive));

    // 4. Otherwise, return ? ToBigInt(value).
    return TRY(value.to_bigint(global_object));
}

// 21.2.1.1 BigInt ( value ), https://tc39.es/ecma262/#sec-bigint-constructor-number-value
ThrowCompletionOr<Object*> BigIntConstructor::construct(FunctionObject&)
{
    return vm().throw_completion<TypeError>(global_object(), ErrorType::NotAConstructor, "BigInt");
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
