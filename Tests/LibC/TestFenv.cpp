/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <fenv.h>

// TODO: Add tests for floating-point exception management.

// This error happens even with float literals; e.g. -1.099999f becomes -1.099998f.
// However, it is enough to distinguish the different rounding errors we test for.
static constexpr double acceptable_float_error = 0.000001;

TEST_CASE(float_round_up)
{
    fesetround(FE_UPWARD);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.2f, 0.3f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.3f, 0.4f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.4f, 0.5f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(-1.f + -0.1f, -1.099999f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(1.f + 0.1f, 1.1f, acceptable_float_error);
}

TEST_CASE(float_round_down)
{
    fesetround(FE_DOWNWARD);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.2f, 0.299999f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.3f, 0.4f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.4f, 0.5f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(-1.f + -0.1f, -1.1f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(1.f + 0.1f, 1.099999f, acceptable_float_error);
}

TEST_CASE(float_round_to_zero)
{
    fesetround(FE_TOWARDZERO);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.2f, 0.299999f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.3f, 0.4f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.4f, 0.5f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(-1.f + -0.1f, -1.099999f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(1.f + 0.1f, 1.099999f, acceptable_float_error);
}

TEST_CASE(float_round_to_nearest)
{
    fesetround(FE_TONEAREST);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.2f, 0.3, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.3f, 0.4, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.4f, 0.5, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(-1.f + -0.1f, -1.1f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(1.f + 0.1f, 1.1f, acceptable_float_error);
}

// FIXME: Figure out some tests to distinguish nearest from max-magnitude on supported platforms.
TEST_CASE(float_round_to_max_magnitude)
{
    fesetround(FE_TOMAXMAGNITUDE);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.2f, 0.3f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.3f, 0.4f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(0.1f + 0.4f, 0.5f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(-1.f + -0.1f, -1.1f, acceptable_float_error);
    EXPECT_APPROXIMATE_WITH_ERROR(1.f + 0.1f, 1.1f, acceptable_float_error);
}

TEST_CASE(store_round_in_env)
{
    fesetround(FE_DOWNWARD);
    fenv_t env;
    fegetenv(&env);
    fesetround(FE_UPWARD);
    // This result only happens under upward rounding.
    EXPECT_APPROXIMATE_WITH_ERROR(-1.f + -0.1f, -1.099999f, acceptable_float_error);
    fesetenv(&env);
    // ... and this only under downward rounding.
    EXPECT_APPROXIMATE_WITH_ERROR(-1.f + -0.1f, -1.1f, acceptable_float_error);
}

TEST_CASE(save_restore_round)
{
    fesetround(FE_DOWNWARD);
    auto rounding_mode = fegetround();
    EXPECT_EQ(rounding_mode, FE_DOWNWARD);

    fesetround(FE_UPWARD);
    EXPECT_EQ(fegetround(), FE_UPWARD);
    EXPECT_APPROXIMATE_WITH_ERROR(-1.f + -0.1f, -1.099999f, acceptable_float_error);
    fesetround(rounding_mode);
    EXPECT_APPROXIMATE_WITH_ERROR(-1.f + -0.1f, -1.1f, acceptable_float_error);

#if ARCH(X86_64)
    // Max-magnitude rounding is not supported by x86, so we expect fesetround to decay it to some other rounding mode.
    fesetround(FE_TOMAXMAGNITUDE);
    EXPECT_NE(fegetround(), FE_TOMAXMAGNITUDE);
#endif
}
