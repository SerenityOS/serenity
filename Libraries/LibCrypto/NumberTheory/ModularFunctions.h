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

#include <LibCrypto/BigInt/UnsignedBigInteger.h>

//#define NT_DEBUG

namespace Crypto {
namespace NumberTheory {

static auto ModularInverse(const UnsignedBigInteger& a_, const UnsignedBigInteger& b) -> UnsignedBigInteger
{
    if (b == 1)
        return { 1 };

    auto a = a_;
    auto u = a;
    if (a.words()[0] % 2 == 0)
        u = u.add(b);

    auto v = b;
    auto x = UnsignedBigInteger { 0 };
    auto d = b.sub(1);

    while (!(v == 1)) {
        while (v < u) {
            u = u.sub(v);
            d = d.add(x);
            while (u.words()[0] % 2 == 0) {
                if (d.words()[0] % 2 == 1) {
                    d = d.add(b);
                }
                u = u.divide(2).quotient;
                d = d.divide(2).quotient;
            }
        }
        v = v.sub(u);
        x = x.add(d);
        while (v.words()[0] % 2 == 0) {
            if (x.words()[0] % 2 == 1) {
                x = x.add(b);
            }
            v = v.divide(2).quotient;
            x = x.divide(2).quotient;
        }
    }
    return x.divide(b).remainder;
}

static auto ModularPower(const UnsignedBigInteger& b, const UnsignedBigInteger& e, const UnsignedBigInteger& m) -> UnsignedBigInteger
{
    if (m == 1)
        return 0;

    UnsignedBigInteger ep { e };
    UnsignedBigInteger base { b };
    UnsignedBigInteger exp { 1 };

    while (!(ep < 1)) {
#ifdef NT_DEBUG
        dbg() << ep.to_base10();
#endif
        if (ep.words()[0] % 2 == 1) {
            exp = exp.multiply(base).divide(m).remainder;
        }
        ep = ep.divide(2).quotient;
        base = base.multiply(base).divide(m).remainder;
    }
    return exp;
}

static auto GCD(const UnsignedBigInteger& a, const UnsignedBigInteger& b) -> UnsignedBigInteger
{
    UnsignedBigInteger a_ { a }, b_ { b };
    for (;;) {
        if (a_ == 0)
            return b_;
        b_ = b_.divide(a_).remainder;
        if (b_ == 0)
            return a_;
        a_ = a_.divide(b_).remainder;
    }
}

static auto LCM(const UnsignedBigInteger& a, const UnsignedBigInteger& b) -> UnsignedBigInteger
{
    auto temp = GCD(a, b);

    auto div = a.divide(temp);

#ifdef NT_DEBUG
    dbg() << "quot: " << div.quotient << " rem: " << div.remainder;
#endif
    return temp == 0 ? 0 : (a.divide(temp).quotient.multiply(b));
}

template<size_t test_count>
static bool MR_primality_test(UnsignedBigInteger n, const Vector<UnsignedBigInteger, test_count>& tests)
{
    auto prev = n.sub({ 1 });
    auto b = prev;
    auto r = 0;

    auto div_result = b.divide(2);
    while (div_result.quotient == 0) {
        div_result = b.divide(2);
        b = div_result.quotient;
        ++r;
    }

    for (size_t i = 0; i < tests.size(); ++i) {
        auto return_ = true;
        if (n < tests[i])
            continue;
        auto x = ModularPower(tests[i], b, n);
        if (x == 1 || x == prev)
            continue;
        for (auto d = r - 1; d != 0; --d) {
            x = ModularPower(x, 2, n);
            if (x == 1)
                return false;
            if (x == prev) {
                return_ = false;
                break;
            }
        }
        if (return_)
            return false;
    }

    return true;
}

static UnsignedBigInteger random_number(const UnsignedBigInteger& min, const UnsignedBigInteger& max)
{
    ASSERT(min < max);
    auto range = max.minus(min);
    UnsignedBigInteger base;
    // FIXME: Need a cryptographically secure rng
    auto size = range.trimmed_length() * sizeof(u32);
    u8 buf[size];
    arc4random_buf(buf, size);
    Vector<u32> vec;
    for (size_t i = 0; i < size / sizeof(u32); ++i) {
        vec.append(*(u32*)buf + i);
    }
    UnsignedBigInteger offset { move(vec) };
    return offset.add(min);
}

static bool is_probably_prime(const UnsignedBigInteger& p)
{
    if (p == 2 || p == 3 || p == 5)
        return true;
    if (p < 49)
        return true;

    Vector<UnsignedBigInteger, 256> tests;
    UnsignedBigInteger seven { 7 };
    for (size_t i = 0; i < tests.size(); ++i)
        tests.append(random_number(seven, p.sub(2)));

    return MR_primality_test(p, tests);
}

static UnsignedBigInteger random_big_prime(size_t bits)
{
    ASSERT(bits >= 33);
    UnsignedBigInteger min = UnsignedBigInteger::from_base10("6074001000").shift_left(bits - 33);
    UnsignedBigInteger max = UnsignedBigInteger { 1 }.shift_left(bits).sub(1);
    for (;;) {
        auto p = random_number(min, max);
        if (is_probably_prime(p))
            return p;
    }
}

}
}
