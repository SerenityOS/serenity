/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <fenv.h>

// TODO: Add tests for floating-point exception management.

static constexpr auto reset_rounding_mode = [] { fesetround(FE_TONEAREST); };

#if !ARCH(RISCV64)
#    define TOMAXMAGNITUDE_DECAYS_TO_TONEAREST
#endif

TEST_CASE(float_round_up)
{
    // Non-default rounding mode should not escape files with -frounding-math.
    ScopeGuard rounding_mode_guard = reset_rounding_mode;

    fesetround(FE_UPWARD);
    EXPECT_EQ(0.1f + 0.2f, 0.3f);
    EXPECT_EQ(0.1f + 0.3f, 0.40000004f);
    EXPECT_EQ(0.1f + 0.4f, 0.50000006f);
    EXPECT_EQ(-1.f + -0.1f, -1.0999999f);
    EXPECT_EQ(1.f + 0.1f, 1.1f);
}

TEST_CASE(float_round_down)
{
    ScopeGuard rounding_mode_guard = reset_rounding_mode;

    fesetround(FE_DOWNWARD);
    EXPECT_EQ(0.1f + 0.2f, 0.29999998f);
    EXPECT_EQ(0.1f + 0.3f, 0.4f);
    EXPECT_EQ(0.1f + 0.4f, 0.5f);
    EXPECT_EQ(-1.f + -0.1f, -1.1f);
    EXPECT_EQ(1.f + 0.1f, 1.0999999f);
}

TEST_CASE(float_round_to_zero)
{
    ScopeGuard rounding_mode_guard = reset_rounding_mode;

    fesetround(FE_TOWARDZERO);
    EXPECT_EQ(0.1f + 0.2f, 0.29999998f);
    EXPECT_EQ(0.1f + 0.3f, 0.4f);
    EXPECT_EQ(0.1f + 0.4f, 0.5f);
    EXPECT_EQ(-1.f + -0.1f, -1.0999999f);
    EXPECT_EQ(1.f + 0.1f, 1.0999999f);
}

TEST_CASE(float_round_to_nearest)
{
    ScopeGuard rounding_mode_guard = reset_rounding_mode;

    fesetround(FE_TONEAREST);
    EXPECT_EQ(0.1f + 0.2f, 0.3f);
    EXPECT_EQ(0.1f + 0.3f, 0.4f);
    EXPECT_EQ(0.1f + 0.4f, 0.5f);
    EXPECT_EQ(-1.f + -0.1f, -1.1f);
    EXPECT_EQ(1.f + 0.1f, 1.1f);
    EXPECT_EQ(1.f + 5.9604645e-08f, 1.f);
}

TEST_CASE(float_round_to_max_magnitude)
{
    ScopeGuard rounding_mode_guard = reset_rounding_mode;

    fesetround(FE_TOMAXMAGNITUDE);
    EXPECT_EQ(0.1f + 0.2f, 0.3f);
    EXPECT_EQ(0.1f + 0.3f, 0.4f);
    EXPECT_EQ(0.1f + 0.4f, 0.5f);
    EXPECT_EQ(-1.f + -0.1f, -1.1f);
    EXPECT_EQ(1.f + 0.1f, 1.1f);
#ifdef TOMAXMAGNITUDE_DECAYS_TO_TONEAREST
    EXPECT_EQ(1.f + 5.9604645e-08f, 1.f);
#else
    EXPECT_EQ(1.f + 5.9604645e-08f, 1.0000001f);
#endif
}

TEST_CASE(store_round_in_env)
{
    ScopeGuard rounding_mode_guard = reset_rounding_mode;

    fesetround(FE_DOWNWARD);
    fenv_t env;
    fegetenv(&env);
    fesetround(FE_UPWARD);
    // This result only happens under upward rounding.
    EXPECT_EQ(-1.f + -0.1f, -1.0999999f);
    fesetenv(&env);
    // ... and this only under downward rounding.
    EXPECT_EQ(-1.f + -0.1f, -1.1f);
}

TEST_CASE(save_restore_round)
{
    ScopeGuard rounding_mode_guard = reset_rounding_mode;

    fesetround(FE_DOWNWARD);
    auto rounding_mode = fegetround();
    EXPECT_EQ(rounding_mode, FE_DOWNWARD);

    fesetround(FE_UPWARD);
    EXPECT_EQ(fegetround(), FE_UPWARD);
    EXPECT_EQ(-1.f + -0.1f, -1.0999999f);
    fesetround(rounding_mode);
    EXPECT_EQ(-1.f + -0.1f, -1.1f);

    fesetround(FE_TOMAXMAGNITUDE);
#ifdef TOMAXMAGNITUDE_DECAYS_TO_TONEAREST
    // Max-magnitude rounding is not supported by x86, so we expect fesetround to decay it to some other rounding mode.
    EXPECT_EQ(fegetround(), FE_TONEAREST);
#else
    EXPECT_EQ(fegetround(), FE_TOMAXMAGNITUDE);
#endif
}
