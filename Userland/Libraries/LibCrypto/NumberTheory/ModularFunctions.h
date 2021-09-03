/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Random.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>

namespace Crypto {
namespace NumberTheory {

UnsignedBigInteger ModularInverse(const UnsignedBigInteger& a_, const UnsignedBigInteger& b);
UnsignedBigInteger ModularPower(const UnsignedBigInteger& b, const UnsignedBigInteger& e, const UnsignedBigInteger& m);

// Note: This function _will_ generate extremely huge numbers, and in doing so,
//       it will allocate and free a lot of memory!
//       Please use |ModularPower| if your use-case is modexp.
template<typename IntegerType>
static IntegerType Power(const IntegerType& b, const IntegerType& e)
{
    IntegerType ep { e };
    IntegerType base { b };
    IntegerType exp { 1 };

    while (!(ep < IntegerType { 1 })) {
        if (ep.words()[0] % 2 == 1)
            exp.set_to(exp.multiplied_by(base));

        // ep = ep / 2;
        ep.set_to(ep.divided_by(IntegerType { 2 }).quotient);

        // base = base * base
        base.set_to(base.multiplied_by(base));
    }

    return exp;
}

UnsignedBigInteger GCD(const UnsignedBigInteger& a, const UnsignedBigInteger& b);
UnsignedBigInteger LCM(const UnsignedBigInteger& a, const UnsignedBigInteger& b);

UnsignedBigInteger random_number(const UnsignedBigInteger& min, const UnsignedBigInteger& max_excluded);
bool is_probably_prime(const UnsignedBigInteger& p);
UnsignedBigInteger random_big_prime(size_t bits);

}
}
