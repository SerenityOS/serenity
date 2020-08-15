/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Random.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>

//#define NT_DEBUG

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
