/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/SIMD.h>
#include <AK/SIMDFormat.h>
#include <AK/SIMDMath.h>

// See the comment in <AK/SIMDMath.h>
#pragma GCC diagnostic ignored "-Wpsabi"

TEST_CASE(exp)
{
    AK::SIMD::f32x4 v = { .2f, .4f, .6f, .8f };
    auto result = AK::SIMD::exp(v);

    EXPECT_APPROXIMATE(result[0], 1.22140276f);
    EXPECT_APPROXIMATE(result[1], 1.49182470f);
    EXPECT_APPROXIMATE(result[2], 1.82211880f);
    EXPECT_APPROXIMATE(result[3], 2.22554093f);
}

TEST_CASE(exp_approximate)
{
    AK::SIMD::f32x4 v = { .2f, .4f, .6f, .8f };
    auto result = AK::SIMD::exp_approximate(v);
    constexpr float accuracy = .001f;

    EXPECT(fabsf(result[0] - 1.22140276f) <= accuracy);
    EXPECT(fabsf(result[1] - 1.49182470f) <= accuracy);
    EXPECT(fabsf(result[2] - 1.82211880f) <= accuracy);
    EXPECT(fabsf(result[3] - 2.22554093f) <= accuracy);
}

TEST_CASE(clamp)
{
    AK::SIMD::f32x4 v1 = { .2f, .4f, .6f, .8f };
    v1 = AK::SIMD::clamp(v1, .4f, .6f);
    EXPECT(v1[0] == v1[1] && v1[1] == .4f);
    EXPECT(v1[2] == v1[3] && v1[3] == .6f);

    AK::SIMD::i32x4 v2 = { -10, 0, 0, 10 };
    v2 = AK::SIMD::clamp(v2, -5, 5);
    EXPECT(v2[0] == -5);
    EXPECT(v2[1] == 0);
    EXPECT(v2[2] == 0);
    EXPECT(v2[3] == 5);
}

TEST_CASE(format)
{
    AK::SIMD::f32x4 v1 = { .2f, .4f, .6f, .8f };
    EXPECT(MUST(String::formatted("{}", v1)) == "{0.2, 0.4, 0.6, 0.8}"sv);
}
