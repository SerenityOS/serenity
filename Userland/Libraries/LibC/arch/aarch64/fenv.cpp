/*
 * Copyright (c) 2024, Tom Finet <tom.codeninja@gmail.com>
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <fenv.h>

extern "C" {

// https://developer.arm.com/documentation/ddi0601/2025-12/AArch64-Registers/FPCR--Floating-point-Control-Register#fieldset_0-23_22
// RMode, bits [23:22]
enum class RoundingMode : u8 {
    RoundToNearest = 0b00,            // RN
    RoundTowardsPlusInfinity = 0b01,  // RP
    RoundTowardsMinusInfinity = 0b10, // RN
    RoundTowardsZero = 0b11,          // RZ
};

static constexpr size_t FPCR_RMODE_OFFSET = 22;
static constexpr u64 FPCR_RMODE_MASK = 0b11;

static RoundingMode rmode_from_feround(int c_rounding_mode)
{
    switch (c_rounding_mode) {
    case FE_TONEAREST:
        return RoundingMode::RoundToNearest;
    case FE_TOWARDZERO:
        return RoundingMode::RoundTowardsZero;
    case FE_DOWNWARD:
        return RoundingMode::RoundTowardsMinusInfinity;
    case FE_UPWARD:
        return RoundingMode::RoundTowardsPlusInfinity;
    }
    VERIFY_NOT_REACHED();
}

static int feround_from_rmode(RoundingMode frm)
{
    switch (frm) {
    case RoundingMode::RoundToNearest:
        return FE_TONEAREST;
    case RoundingMode::RoundTowardsPlusInfinity:
        return FE_UPWARD;
    case RoundingMode::RoundTowardsMinusInfinity:
        return FE_DOWNWARD;
    case RoundingMode::RoundTowardsZero:
        return FE_TOWARDZERO;
    }
    VERIFY_NOT_REACHED();
}

static RoundingMode get_rounding_mode()
{
    u64 fpcr = 0;
    asm volatile("mrs %0, fpcr" : "=r"(fpcr));
    return static_cast<RoundingMode>((fpcr >> FPCR_RMODE_OFFSET) & FPCR_RMODE_MASK);
}

static void set_rounding_mode(RoundingMode rmode)
{
    u64 fpcr = 0;
    asm volatile("mrs %0, fpcr" : "=r"(fpcr));

    fpcr &= ~(FPCR_RMODE_MASK << FPCR_RMODE_OFFSET);
    fpcr |= to_underlying(rmode) << FPCR_RMODE_OFFSET;

    asm volatile("msr fpcr, %0" ::"r"(fpcr));
}

int fegetenv(fenv_t* env)
{
    if (!env)
        return 1;

    asm volatile("mrs %0, fpcr" : "=r"(env->fpcr));
    asm volatile("mrs %0, fpsr" : "=r"(env->fpsr));

    return 0;
}

int fesetenv(fenv_t const* env)
{
    if (!env)
        return 1;

    asm volatile("msr fpcr, %0" ::"r"(env->fpcr));
    asm volatile("msr fpsr, %0" ::"r"(env->fpsr));

    return 0;
}

int feholdexcept(fenv_t* env)
{
    fegetenv(env);

    fenv_t current_env;
    fegetenv(&current_env);

    (void)env;
    TODO_AARCH64();

    fesetenv(&current_env);
    return 0;
}

int fesetexceptflag(fexcept_t const* except, int exceptions)
{
    if (!except)
        return 1;

    fenv_t current_env;
    fegetenv(&current_env);

    exceptions &= FE_ALL_EXCEPT;

    (void)exceptions;
    (void)except;
    TODO_AARCH64();

    fesetenv(&current_env);
    return 0;
}

int fegetround()
{
    return feround_from_rmode(get_rounding_mode());
}

int fesetround(int rounding_mode)
{
    if (rounding_mode < FE_TONEAREST || rounding_mode > FE_TOMAXMAGNITUDE)
        return 1;

    if (rounding_mode == FE_TOMAXMAGNITUDE)
        rounding_mode = FE_TONEAREST;

    set_rounding_mode(rmode_from_feround(rounding_mode));

    return 0;
}

int feclearexcept(int exceptions)
{
    exceptions &= FE_ALL_EXCEPT;

    fenv_t current_env;
    fegetenv(&current_env);

    (void)exceptions;
    TODO_AARCH64();

    fesetenv(&current_env);
    return 0;
}

int fetestexcept(int exceptions)
{
    (void)exceptions;
    TODO_AARCH64();
}

int feraiseexcept(int exceptions)
{
    fenv_t env;
    fegetenv(&env);

    exceptions &= FE_ALL_EXCEPT;

    (void)exceptions;
    TODO_AARCH64();
}
}
