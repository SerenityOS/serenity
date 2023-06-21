/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FloatingPoint.h>
#include <LibTest/TestCase.h>
#include <math.h>

TEST_CASE(f16_1_5_10_to_native_float)
{
    auto within_approximate = [](u16 lhs, float rhs) -> bool {
        auto f32_lhs = convert_to_native_float(FloatingPointBits<1, 5, 10>(lhs));
        return fabsf(f32_lhs - rhs) <= 0.00001f;
    };

    EXPECT(within_approximate(0x0000, 0.f));
    EXPECT(within_approximate(0x03FF, 0.000061f));
    EXPECT(within_approximate(0x3CEF, 1.23339f));
    EXPECT(within_approximate(0xBC00, -1.f));
    EXPECT(within_approximate(0xA266, -0.0125f));

    float result;
    result = convert_to_native_float(FloatingPointBits<1, 5, 10>(0xFC01u));
    EXPECT(isnan(result));

    result = convert_to_native_float(FloatingPointBits<1, 5, 10>(0x7C00u));
    EXPECT(isinf(result));
}

TEST_CASE(float_to_double_roundtrips)
{
    auto roundtrip = [](float floatvalue1) {
        auto doublevalue = convert_from_native_float<DoubleFloatingPointBits>(floatvalue1).as_double();
        auto floatbits = convert_from_native_double<SingleFloatingPointBits>(doublevalue);
        auto floatvalue2 = convert_to_native_float(floatbits);

        EXPECT_APPROXIMATE(floatvalue1, floatvalue2);
    };

    roundtrip(-1.0f);
    roundtrip(-0.1f);
    roundtrip(0.0f);
    roundtrip(0.000001f);
    roundtrip(0.1f);
    roundtrip(1.0f);
    roundtrip(3.141592f);
    roundtrip(16777216.0f);
    roundtrip(33554432.0f);

    roundtrip(1 / 0.0f);
    roundtrip(1 / -0.0f);
    roundtrip(0 / 0.0f);
}

TEST_CASE(normalize_denormalize)
{
    // Go from denormalized float to normalized double
    auto denormalized_float = 6.709679e-39f;
    auto denormalized_float_bits = SingleFloatingPointBits(denormalized_float);
    auto normalized_double = convert_to_native_double(denormalized_float_bits);
    EXPECT_APPROXIMATE(denormalized_float, normalized_double);

    // Go back from normalized double to denormalized float
    auto normalized_double_bits = DoubleFloatingPointBits(normalized_double);
    auto reconstructed_denormalized_float = convert_to_native_float(normalized_double_bits);
    EXPECT_APPROXIMATE(denormalized_float, reconstructed_denormalized_float);
}

TEST_CASE(large_exponent)
{
    // Make sure we support at least 62 bits of exponent
    auto large_exponent_float = convert_from_native_double<FloatingPointBits<1, 62, 1>>(1.0);
    auto converted_double = convert_to_native_double(large_exponent_float);
    EXPECT_APPROXIMATE(converted_double, 1.0);
}

TEST_CASE(large_mantissa)
{
    // Make sure we support at least 62 bits of mantissa
    auto large_exponent_float = convert_from_native_double<FloatingPointBits<1, 1, 62>>(1.0);
    auto converted_double = convert_to_native_double(large_exponent_float);
    EXPECT_APPROXIMATE(converted_double, 1.0);
}
