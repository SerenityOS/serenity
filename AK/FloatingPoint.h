/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <AK/Concepts.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
union FloatExtractor;

#ifdef AK_HAS_FLOAT_128
template<>
union FloatExtractor<f128> {
    static constexpr int mantissa_bits = 112;
    static constexpr unsigned __int128 mantissa_max = (((unsigned __int128)1) << 112) - 1;
    static constexpr int exponent_bias = 16383;
    static constexpr int exponent_bits = 15;
    static constexpr unsigned exponent_max = 32767;
    struct {
        unsigned __int128 mantissa : 112;
        unsigned exponent : 15;
        unsigned sign : 1;
    };
    f128 d;
};
// Validate that f128 and the FloatExtractor union are 128 bits.
static_assert(sizeof(f128) == 16);
static_assert(sizeof(FloatExtractor<f128>) == 16);
#endif

#ifdef AK_HAS_FLOAT_80
template<>
union FloatExtractor<f80> {
    static constexpr int mantissa_bits = 64;
    static constexpr unsigned long long mantissa_max = ~0u;
    static constexpr int exponent_bias = 16383;
    static constexpr int exponent_bits = 15;
    static constexpr unsigned exponent_max = 32767;
    struct {
        unsigned long long mantissa;
        unsigned exponent : 15;
        unsigned sign : 1;
    };
    f80 d;
};
#endif

template<>
union FloatExtractor<f64> {
    static constexpr int mantissa_bits = 52;
    static constexpr unsigned long long mantissa_max = (1ull << 52) - 1;
    static constexpr int exponent_bias = 1023;
    static constexpr int exponent_bits = 11;
    static constexpr unsigned exponent_max = 2047;
    struct {
        unsigned long long mantissa : 52;
        unsigned exponent : 11;
        unsigned sign : 1;
    };
    f64 d;
};

template<>
union FloatExtractor<f32> {
    static constexpr int mantissa_bits = 23;
    static constexpr unsigned mantissa_max = (1 << 23) - 1;
    static constexpr int exponent_bias = 127;
    static constexpr int exponent_bits = 8;
    static constexpr unsigned exponent_max = 255;
    struct {
        unsigned long long mantissa : 23;
        unsigned exponent : 8;
        unsigned sign : 1;
    };
    f32 d;
};

template<size_t S, size_t E, size_t M>
requires(S <= 1 && E >= 1 && M >= 1 && (S + E + M) <= 64) class FloatingPointBits final {
public:
    static const size_t signbit = S;
    static const size_t exponentbits = E;
    static const size_t mantissabits = M;

    template<typename T>
    requires(IsIntegral<T> && IsUnsigned<T> && sizeof(T) <= 8) constexpr FloatingPointBits(T bits)
        : m_bits(bits)
    {
    }

    constexpr FloatingPointBits(double value)
        : m_bits(bit_cast<u64>(value))
    {
    }

    constexpr FloatingPointBits(float value)
        : m_bits(bit_cast<u32>(value))
    {
    }

    double as_double() const
    requires(S == 1 && E == 11 && M == 52)
    {
        return bit_cast<double>(m_bits);
    }
    float as_float() const
    requires(S == 1 && E == 8 && M == 23)
    {
        return bit_cast<float>(static_cast<u32>(m_bits));
    }
    u64 bits() const { return m_bits; }

private:
    u64 m_bits;
};

typedef FloatingPointBits<1, 8, 23> SingleFloatingPointBits;
typedef FloatingPointBits<1, 11, 52> DoubleFloatingPointBits;

/**
 * Convert between two IEEE 754 floating point types in any arrangement of sign, exponent and mantissa bits.
 */
template<typename To, typename From>
constexpr To float_to_float(From const input)
{
    constexpr u64 from_exponent_nonnumber = (1ull << From::exponentbits) - 1;
    constexpr u64 from_exponent_bias = (1ull << (From::exponentbits - 1)) - 1;
    constexpr u64 to_exponent_nonnumber = (1ull << To::exponentbits) - 1;
    constexpr u64 to_exponent_bias = (1ull << (To::exponentbits - 1)) - 1;
    constexpr u64 to_exponent_max = (1ull << To::exponentbits) - 2;

    // Deconstruct input bits to float components
    u64 from_sign = (input.bits() >> (From::exponentbits + From::mantissabits)) & From::signbit;
    u64 from_exponent = (input.bits() >> From::mantissabits) & ((1ull << From::exponentbits) - 1);
    u64 from_mantissa = input.bits() & ((1ull << From::mantissabits) - 1);

    u64 to_sign = from_sign & To::signbit;
    u64 to_exponent;
    u64 to_mantissa;
    auto target_value = [&to_sign, &to_exponent, &to_mantissa]() {
        return To((to_sign << (To::exponentbits + To::mantissabits)) | (to_exponent << To::mantissabits) | to_mantissa);
    };

    auto shift_mantissa = [](u64 mantissa) -> u64 {
        if constexpr (From::mantissabits < To::mantissabits)
            return mantissa << (To::mantissabits - From::mantissabits);
        else
            return mantissa >> (From::mantissabits - To::mantissabits);
    };

    // If target is unsigned and source is negative, clamp to 0 or keep NaN
    if constexpr (To::signbit == 0) {
        if (from_sign == 1) {
            if (from_exponent == from_exponent_nonnumber && from_mantissa > 0) {
                to_exponent = to_exponent_nonnumber;
                to_mantissa = 1;
            } else {
                to_exponent = 0;
                to_mantissa = 0;
            }
            return target_value();
        }
    }

    // If the source floating point is denormalized;
    if (from_exponent == 0) {
        // If the source mantissa is 0, the value is +/-0
        if (from_mantissa == 0) {
            to_exponent = 0;
            to_mantissa = 0;
            return target_value();
        }

        // If the source has more exponent bits than the target, then the largest possible
        // source mantissa still cannot be represented in the target denormalized value.
        if constexpr (From::exponentbits > To::exponentbits) {
            to_exponent = 0;
            to_mantissa = 0;
            return target_value();
        }

        // If the source and target have the same number of exponent bits, we only need to
        // shift the mantissa.
        if constexpr (From::exponentbits == To::exponentbits) {
            to_exponent = 0;
            to_mantissa = shift_mantissa(from_mantissa);
            return target_value();
        }

        // The target has more exponent bits, so our denormalized value can be represented
        // as a normalized value in the target floating point. Normalized values have an
        // implicit leading 1, so we shift the mantissa left until we find our explicit
        // leading 1 which is then dropped.
        int adjust_exponent = -1;
        to_mantissa = from_mantissa;
        do {
            ++adjust_exponent;
            to_mantissa <<= 1;
        } while ((to_mantissa & (1ull << From::mantissabits)) == 0);
        to_exponent = to_exponent_bias - from_exponent_bias - adjust_exponent;

        // Drop the most significant bit from the mantissa
        to_mantissa &= (1ull << From::mantissabits) - 1;
        to_mantissa = shift_mantissa(to_mantissa);
        return target_value();
    }

    // If the source is NaN or +/-Inf, keep it that way
    if (from_exponent == from_exponent_nonnumber) {
        to_exponent = to_exponent_nonnumber;
        to_mantissa = (from_mantissa == 0) ? 0 : 1;
        return target_value();
    }

    // Determine the target exponent
    to_exponent = to_exponent_bias - from_exponent_bias + from_exponent;

    // If the calculated exponent exceeds the target's capacity, clamp both the exponent and the
    // mantissa to their maximum values.
    if (to_exponent > to_exponent_max) {
        to_exponent = to_exponent_max;
        to_mantissa = (1ull << To::mantissabits) - 1;
        return target_value();
    }

    // If the new exponent is less than 1, we can only represent this value as a denormalized number
    if (to_exponent < 1) {
        to_exponent = 0;

        // Add a leading 1 and shift the mantissa right
        int adjust_exponent = 1 - to_exponent_bias - from_exponent + from_exponent_bias;
        to_mantissa = ((1ull << From::mantissabits) | from_mantissa) >> adjust_exponent;
        to_mantissa = shift_mantissa(to_mantissa);
        return target_value();
    }

    // New exponent fits; shift the mantissa to fit as well
    to_mantissa = shift_mantissa(from_mantissa);
    return target_value();
}

template<typename O>
constexpr O convert_from_native_double(double input) { return float_to_float<O>(DoubleFloatingPointBits(input)); }

template<typename O>
constexpr O convert_from_native_float(float input) { return float_to_float<O>(SingleFloatingPointBits(input)); }

template<typename I>
constexpr double convert_to_native_double(I input) { return float_to_float<DoubleFloatingPointBits>(input).as_double(); }

template<typename I>
constexpr float convert_to_native_float(I input) { return float_to_float<SingleFloatingPointBits>(input).as_float(); }

}

#if USING_AK_GLOBALLY
using AK::DoubleFloatingPointBits;
using AK::FloatExtractor;
using AK::FloatingPointBits;
using AK::SingleFloatingPointBits;

using AK::convert_from_native_double;
using AK::convert_from_native_float;
using AK::convert_to_native_double;
using AK::convert_to_native_float;
using AK::float_to_float;
#endif
