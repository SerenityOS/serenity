/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BigInt::BigInt(Crypto::SignedBigInteger big_integer)
    : m_big_integer(move(big_integer))
{
    VERIFY(!m_big_integer.is_invalid());
}

BigInt::~BigInt()
{
}

BigInt* js_bigint(Heap& heap, Crypto::SignedBigInteger big_integer)
{
    return heap.allocate_without_global_object<BigInt>(move(big_integer));
}

BigInt* js_bigint(VM& vm, Crypto::SignedBigInteger big_integer)
{
    return js_bigint(vm.heap(), move(big_integer));
}

// 21.2.1.1.1 NumberToBigInt ( number ), https://tc39.es/ecma262/#sec-numbertobigint
ThrowCompletionOr<BigInt*> number_to_bigint(GlobalObject& global_object, Value number)
{
    VERIFY(number.is_number());
    auto& vm = global_object.vm();

    // 1. If IsIntegralNumber(number) is false, throw a RangeError exception.
    if (!number.is_integral_number())
        return vm.throw_completion<RangeError>(global_object, ErrorType::BigIntFromNonIntegral);

    // 2. Return the BigInt value that represents ℝ(number).
    return js_bigint(vm, Crypto::SignedBigInteger::create_from((i64)number.as_double()));
}

}
