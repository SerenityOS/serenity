/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/BigInt.h>

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

}
