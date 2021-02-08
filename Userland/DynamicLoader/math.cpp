/*
 * Copyright (c) 2020, the SerenityOS developers.
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

// TODO: Is there a compiler flag we can use to still get these math functions? (and compile with -nostdlib)
// FIXME: What do we do with this regarding copyright stuff?
// code for math functions taken from here:
// https://gitlab.incom.co/CM-Shield/u-boot/commit/aa7839b39c2ee77f9ab8c393c56b8d812507dbb7
// https://github.com/zayac/qemu-arm/blob/master/qemu/roms/ipxe/src/libgcc/__udivmoddi4.c
// https://code.woboq.org/llvm/compiler-rt/lib/builtins/divdi3.c.html

#include "math.h"

extern "C" {

union overlay64 {
    u64 longw;
    struct {
        u32 lower;
        u32 higher;
    } words;
};

u64 __ashldi3(u64 num, unsigned int shift)
{
    union overlay64 output;

    output.longw = num;
    if (shift >= 32) {
        output.words.higher = output.words.lower << (shift - 32);
        output.words.lower = 0;
    } else {
        if (!shift)
            return num;
        output.words.higher = (output.words.higher << shift) | (output.words.lower >> (32 - shift));
        output.words.lower = output.words.lower << shift;
    }
    return output.longw;
}

u64 __lshrdi3(u64 num, unsigned int shift)
{
    union overlay64 output;

    output.longw = num;
    if (shift >= 32) {
        output.words.lower = output.words.higher >> (shift - 32);
        output.words.higher = 0;
    } else {
        if (!shift)
            return num;
        output.words.lower = output.words.lower >> shift | (output.words.higher << (32 - shift));
        output.words.higher = output.words.higher >> shift;
    }
    return output.longw;
}

#define MAX_32BIT_UINT ((((u64)1) << 32) - 1)

static u64 _64bit_divide(u64 dividend, u64 divider, u64* rem_p)
{
    u64 result = 0;

    /*
	 * If divider is zero - let the rest of the system care about the
	 * exception.
	 */
    if (!divider)
        return 1 / (u32)divider;

    /* As an optimization, let's not use 64 bit division unless we must. */
    if (dividend <= MAX_32BIT_UINT) {
        if (divider > MAX_32BIT_UINT) {
            result = 0;
            if (rem_p)
                *rem_p = divider;
        } else {
            result = (u32)dividend / (u32)divider;
            if (rem_p)
                *rem_p = (u32)dividend % (u32)divider;
        }
        return result;
    }

    while (divider <= dividend) {
        u64 locald = divider;
        u64 limit = __lshrdi3(dividend, 1);
        int shifts = 0;

        while (locald <= limit) {
            shifts++;
            locald = locald + locald;
        }
        result |= __ashldi3(1, shifts);
        dividend -= locald;
    }

    if (rem_p)
        *rem_p = dividend;

    return result;
}

u64 __udivdi3(u64 num, u64 den)
{
    return _64bit_divide(num, den, nullptr);
}

u64 __umoddi3(u64 num, u64 den)
{
    u64 v = 0;

    _64bit_divide(num, den, &v);
    return v;
}

uint64_t __udivmoddi4(uint64_t num, uint64_t den, uint64_t* rem_p)
{
    uint64_t quot = 0, qbit = 1;

    if (den == 0) {
        return 1 / ((unsigned)den); /* Intentional divide by zero, without
				 triggering a compiler warning which
				 would abort the build */
    }

    /* Left-justify denominator and count shift */
    while ((int64_t)den >= 0) {
        den <<= 1;
        qbit <<= 1;
    }

    while (qbit) {
        if (den <= num) {
            num -= den;
            quot += qbit;
        }
        den >>= 1;
        qbit >>= 1;
    }

    if (rem_p)
        *rem_p = num;

    return quot;
}
int64_t __divdi3(int64_t a, int64_t b)
{
    const int bits_in_dword_m1 = (int)(sizeof(int64_t) * 8) - 1;
    int64_t s_a = a >> bits_in_dword_m1;                   // s_a = a < 0 ? -1 : 0
    int64_t s_b = b >> bits_in_dword_m1;                   // s_b = b < 0 ? -1 : 0
    a = (a ^ s_a) - s_a;                                   // negate if s_a == -1
    b = (b ^ s_b) - s_b;                                   // negate if s_b == -1
    s_a ^= s_b;                                            // sign of quotient
    return (__udivmoddi4(a, b, (uint64_t*)0) ^ s_a) - s_a; // negate if s_a == -1
}
int64_t __moddi3(int64_t a, int64_t b)
{
    const int bits_in_dword_m1 = (int)(sizeof(int64_t) * 8) - 1;
    int64_t s = b >> bits_in_dword_m1; // s = b < 0 ? -1 : 0
    b = (b ^ s) - s;                   // negate if s == -1
    s = a >> bits_in_dword_m1;         // s = a < 0 ? -1 : 0
    a = (a ^ s) - s;                   // negate if s == -1
    uint64_t r;
    __udivmoddi4(a, b, &r);
    return ((int64_t)r ^ s) - s; // negate if s == -1
}
}
