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

#include <LibCrypto/NumberTheory/ModularFunctions.h>

namespace Crypto {
namespace NumberTheory {

UnsignedBigInteger ModularInverse(const UnsignedBigInteger& a_, const UnsignedBigInteger& b)
{
    if (b == 1)
        return { 1 };

    UnsignedBigInteger one { 1 };
    UnsignedBigInteger temp_1;
    UnsignedBigInteger temp_2;
    UnsignedBigInteger temp_3;
    UnsignedBigInteger temp_4;
    UnsignedBigInteger temp_plus;
    UnsignedBigInteger temp_minus;
    UnsignedBigInteger temp_quotient;
    UnsignedBigInteger temp_remainder;
    UnsignedBigInteger d;

    auto a = a_;
    auto u = a;
    if (a.words()[0] % 2 == 0) {
        // u += b
        UnsignedBigInteger::add_without_allocation(u, b, temp_plus);
        u.set_to(temp_plus);
    }

    auto v = b;
    UnsignedBigInteger x { 0 };

    // d = b - 1
    UnsignedBigInteger::subtract_without_allocation(b, one, d);

    while (!(v == 1)) {
        while (v < u) {
            // u -= v
            UnsignedBigInteger::subtract_without_allocation(u, v, temp_minus);
            u.set_to(temp_minus);

            // d += x
            UnsignedBigInteger::add_without_allocation(d, x, temp_plus);
            d.set_to(temp_plus);

            while (u.words()[0] % 2 == 0) {
                if (d.words()[0] % 2 == 1) {
                    // d += b
                    UnsignedBigInteger::add_without_allocation(d, b, temp_plus);
                    d.set_to(temp_plus);
                }

                // u /= 2
                UnsignedBigInteger::divide_u16_without_allocation(u, 2, temp_quotient, temp_remainder);
                u.set_to(temp_quotient);

                // d /= 2
                UnsignedBigInteger::divide_u16_without_allocation(d, 2, temp_quotient, temp_remainder);
                d.set_to(temp_quotient);
            }
        }

        // v -= u
        UnsignedBigInteger::subtract_without_allocation(v, u, temp_minus);
        v.set_to(temp_minus);

        // x += d
        UnsignedBigInteger::add_without_allocation(x, d, temp_plus);
        x.set_to(temp_plus);

        while (v.words()[0] % 2 == 0) {
            if (x.words()[0] % 2 == 1) {
                // x += b
                UnsignedBigInteger::add_without_allocation(x, b, temp_plus);
                x.set_to(temp_plus);
            }

            // v /= 2
            UnsignedBigInteger::divide_u16_without_allocation(v, 2, temp_quotient, temp_remainder);
            v.set_to(temp_quotient);

            // x /= 2
            UnsignedBigInteger::divide_u16_without_allocation(x, 2, temp_quotient, temp_remainder);
            x.set_to(temp_quotient);
        }
    }

    // x % b
    UnsignedBigInteger::divide_without_allocation(x, b, temp_1, temp_2, temp_3, temp_4, temp_quotient, temp_remainder);
    return temp_remainder;
}

UnsignedBigInteger ModularPower(const UnsignedBigInteger& b, const UnsignedBigInteger& e, const UnsignedBigInteger& m)
{
    if (m == 1)
        return 0;

    UnsignedBigInteger ep { e };
    UnsignedBigInteger base { b };
    UnsignedBigInteger exp { 1 };

    UnsignedBigInteger temp_1;
    UnsignedBigInteger temp_2;
    UnsignedBigInteger temp_3;
    UnsignedBigInteger temp_4;
    UnsignedBigInteger temp_multiply;
    UnsignedBigInteger temp_quotient;
    UnsignedBigInteger temp_remainder;

    while (!(ep < 1)) {
#ifdef NT_DEBUG
        dbg() << ep.to_base10();
#endif
        if (ep.words()[0] % 2 == 1) {
            // exp = (exp * base) % m;
            UnsignedBigInteger::multiply_without_allocation(exp, base, temp_1, temp_2, temp_3, temp_4, temp_multiply);
            UnsignedBigInteger::divide_without_allocation(temp_multiply, m, temp_1, temp_2, temp_3, temp_4, temp_quotient, temp_remainder);
            exp.set_to(temp_remainder);
        }

        // ep = ep / 2;
        UnsignedBigInteger::divide_u16_without_allocation(ep, 2, temp_quotient, temp_remainder);
        ep.set_to(temp_quotient);

        // base = (base * base) % m;
        UnsignedBigInteger::multiply_without_allocation(base, base, temp_1, temp_2, temp_3, temp_4, temp_multiply);
        UnsignedBigInteger::divide_without_allocation(temp_multiply, m, temp_1, temp_2, temp_3, temp_4, temp_quotient, temp_remainder);
        base.set_to(temp_remainder);
    }
    return exp;
}

static void GCD_without_allocation(
    const UnsignedBigInteger& a,
    const UnsignedBigInteger& b,
    UnsignedBigInteger& temp_a,
    UnsignedBigInteger& temp_b,
    UnsignedBigInteger& temp_1,
    UnsignedBigInteger& temp_2,
    UnsignedBigInteger& temp_3,
    UnsignedBigInteger& temp_4,
    UnsignedBigInteger& temp_quotient,
    UnsignedBigInteger& temp_remainder,
    UnsignedBigInteger& output)
{
    temp_a.set_to(a);
    temp_b.set_to(b);
    for (;;) {
        if (temp_a == 0) {
            output.set_to(temp_b);
            return;
        }

        // temp_b %= temp_a
        UnsignedBigInteger::divide_without_allocation(temp_b, temp_a, temp_1, temp_2, temp_3, temp_4, temp_quotient, temp_remainder);
        temp_b.set_to(temp_remainder);
        if (temp_b == 0) {
            output.set_to(temp_a);
            return;
        }

        // temp_a %= temp_b
        UnsignedBigInteger::divide_without_allocation(temp_a, temp_b, temp_1, temp_2, temp_3, temp_4, temp_quotient, temp_remainder);
        temp_a.set_to(temp_remainder);
    }
}

UnsignedBigInteger GCD(const UnsignedBigInteger& a, const UnsignedBigInteger& b)
{
    UnsignedBigInteger temp_a;
    UnsignedBigInteger temp_b;
    UnsignedBigInteger temp_1;
    UnsignedBigInteger temp_2;
    UnsignedBigInteger temp_3;
    UnsignedBigInteger temp_4;
    UnsignedBigInteger temp_quotient;
    UnsignedBigInteger temp_remainder;
    UnsignedBigInteger output;

    GCD_without_allocation(a, b, temp_a, temp_b, temp_1, temp_2, temp_3, temp_4, temp_quotient, temp_remainder, output);

    return output;
}

UnsignedBigInteger LCM(const UnsignedBigInteger& a, const UnsignedBigInteger& b)
{
    UnsignedBigInteger temp_a;
    UnsignedBigInteger temp_b;
    UnsignedBigInteger temp_1;
    UnsignedBigInteger temp_2;
    UnsignedBigInteger temp_3;
    UnsignedBigInteger temp_4;
    UnsignedBigInteger temp_quotient;
    UnsignedBigInteger temp_remainder;
    UnsignedBigInteger gcd_output;
    UnsignedBigInteger output { 0 };

    GCD_without_allocation(a, b, temp_a, temp_b, temp_1, temp_2, temp_3, temp_4, temp_quotient, temp_remainder, gcd_output);
    if (gcd_output == 0) {
#ifdef NT_DEBUG
        dbg() << "GCD is zero";
#endif
        return output;
    }

    // output = (a / gcd_output) * b
    UnsignedBigInteger::divide_without_allocation(a, gcd_output, temp_1, temp_2, temp_3, temp_4, temp_quotient, temp_remainder);
    UnsignedBigInteger::multiply_without_allocation(temp_quotient, b, temp_1, temp_2, temp_3, temp_4, output);

#ifdef NT_DEBUG
    dbg() << "quot: " << temp_quotient << " rem: " << temp_remainder << " out: " << output;
#endif

    return output;
}

static bool MR_primality_test(UnsignedBigInteger n, const Vector<UnsignedBigInteger, 256>& tests)
{
    // Written using Wikipedia:
    // https://en.wikipedia.org/wiki/Miller%E2%80%93Rabin_primality_test#Miller%E2%80%93Rabin_test
    ASSERT(!(n < 4));
    auto predecessor = n.minus({ 1 });
    auto d = predecessor;
    size_t r = 0;

    {
        auto div_result = d.divided_by(2);
        while (div_result.remainder == 0) {
            d = div_result.quotient;
            div_result = d.divided_by(2);
            ++r;
        }
    }
    if (r == 0) {
        // n - 1 is odd, so n was even. But there is only one even prime:
        return n == 2;
    }

    for (auto a : tests) {
        // Technically: ASSERT(2 <= a && a <= n - 2)
        ASSERT(a < n);
        auto x = ModularPower(a, d, n);
        if (x == 1 || x == predecessor)
            continue;
        bool skip_this_witness = false;
        // r − 1 iterations.
        for (size_t i = 0; i < r - 1; ++i) {
            x = ModularPower(x, 2, n);
            if (x == predecessor) {
                skip_this_witness = true;
                break;
            }
        }
        if (skip_this_witness)
            continue;
        return false; // "composite"
    }

    return true; // "probably prime"
}

UnsignedBigInteger random_number(const UnsignedBigInteger& min, const UnsignedBigInteger& max_excluded)
{
    ASSERT(min < max_excluded);
    auto range = max_excluded.minus(min);
    UnsignedBigInteger base;
    auto size = range.trimmed_length() * sizeof(u32) + 2;
    // "+2" is intentional (see below).
    // Also, if we're about to crash anyway, at least produce a nice error:
    ASSERT(size < 8 * MiB);
    u8 buf[size];
    AK::fill_with_random(buf, size);
    UnsignedBigInteger random { buf, size };
    // At this point, `random` is a large number, in the range [0, 256^size).
    // To get down to the actual range, we could just compute random % range.
    // This introduces "modulo bias". However, since we added 2 to `size`,
    // we know that the generated range is at least 65536 times as large as the
    // required range! This means that the modulo bias is only 0.0015%, if all
    // inputs are chosen adversarially. Let's hope this is good enough.
    auto divmod = random.divided_by(range);
    // The proper way to fix this is to restart if `divmod.quotient` is maximal.
    return divmod.remainder.plus(min);
}

bool is_probably_prime(const UnsignedBigInteger& p)
{
    // Is it a small number?
    if (p < 49) {
        u32 p_value = p.words()[0];
        // Is it a very small prime?
        if (p_value == 2 || p_value == 3 || p_value == 5 || p_value == 7)
            return true;
        // Is it the multiple of a very small prime?
        if (p_value % 2 == 0 || p_value % 3 == 0 || p_value % 5 == 0 || p_value % 7 == 0)
            return false;
        // Then it must be a prime, but not a very small prime, like 37.
        return true;
    }

    Vector<UnsignedBigInteger, 256> tests;
    // Make some good initial guesses that are guaranteed to find all primes < 2^64.
    tests.append(UnsignedBigInteger(2));
    tests.append(UnsignedBigInteger(3));
    tests.append(UnsignedBigInteger(5));
    tests.append(UnsignedBigInteger(7));
    tests.append(UnsignedBigInteger(11));
    tests.append(UnsignedBigInteger(13));
    UnsignedBigInteger seventeen { 17 };
    for (size_t i = tests.size(); i < 256; ++i) {
        tests.append(random_number(seventeen, p.minus(2)));
    }
    // Miller-Rabin's "error" is 8^-k. In adversarial cases, it's 4^-k.
    // With 200 random numbers, this would mean an error of about 2^-400.
    // So we don't need to worry too much about the quality of the random numbers.

    return MR_primality_test(p, tests);
}

UnsignedBigInteger random_big_prime(size_t bits)
{
    ASSERT(bits >= 33);
    UnsignedBigInteger min = UnsignedBigInteger::from_base10("6074001000").shift_left(bits - 33);
    UnsignedBigInteger max = UnsignedBigInteger { 1 }.shift_left(bits).minus(1);
    for (;;) {
        auto p = random_number(min, max);
        if ((p.words()[0] & 1) == 0) {
            // An even number is definitely not a large prime.
            continue;
        }
        if (is_probably_prime(p))
            return p;
    }
}

}
}
