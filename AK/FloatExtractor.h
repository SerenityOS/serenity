/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

template<typename T>
union FloatExtractor;

#if ARCH(I386) || ARCH(X86_64)
// This assumes long double is 80 bits, which is true with GCC on Intel platforms
template<>
union FloatExtractor<long double> {
    static const int mantissa_bits = 64;
    static const unsigned long long mantissa_max = ~0u;
    static const int exponent_bias = 16383;
    static const int exponent_bits = 15;
    static const unsigned exponent_max = 32767;
    struct {
        unsigned long long mantissa;
        unsigned exponent : 15;
        unsigned sign : 1;
    };
    long double d;
};
#endif

template<>
union FloatExtractor<double> {
    static const int mantissa_bits = 52;
    static const unsigned long long mantissa_max = (1ull << 52) - 1;
    static const int exponent_bias = 1023;
    static const int exponent_bits = 11;
    static const unsigned exponent_max = 2047;
    struct {
        unsigned long long mantissa : 52;
        unsigned exponent : 11;
        unsigned sign : 1;
    };
    double d;
};

template<>
union FloatExtractor<float> {
    static const int mantissa_bits = 23;
    static const unsigned mantissa_max = (1 << 23) - 1;
    static const int exponent_bias = 127;
    static const int exponent_bits = 8;
    static const unsigned exponent_max = 255;
    struct {
        unsigned long long mantissa : 23;
        unsigned exponent : 8;
        unsigned sign : 1;
    };
    float d;
};
