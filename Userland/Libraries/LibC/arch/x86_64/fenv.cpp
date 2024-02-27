/*
 * Copyright (c) 2021-2024, Mițca Dumitru <dumitru0mitca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <fenv.h>

// This is the size of the floating point environment image in protected mode
static_assert(sizeof(__x87_floating_point_environment) == 28);

static u16 read_status_register()
{
    u16 status_register;
    asm volatile("fnstsw %0"
                 : "=m"(status_register));
    return status_register;
}

static u16 read_control_word()
{
    u16 control_word;
    asm volatile("fnstcw %0"
                 : "=m"(control_word));
    return control_word;
}

static void set_control_word(u16 new_control_word)
{
    asm volatile("fldcw %0" ::"m"(new_control_word));
}

static u32 read_mxcsr()
{
    u32 mxcsr;
    asm volatile("stmxcsr %0"
                 : "=m"(mxcsr));
    return mxcsr;
}

static void set_mxcsr(u32 new_mxcsr)
{
    asm volatile("ldmxcsr %0" ::"m"(new_mxcsr));
}

static constexpr u32 default_mxcsr_value = 0x1f80;

extern "C" {

int fegetenv(fenv_t* env)
{
    if (!env)
        return 1;

    asm volatile("fnstenv %0"
                 : "=m"(env->__x87_fpu_env)::"memory");

    env->__mxcsr = read_mxcsr();
    return 0;
}

int fesetenv(fenv_t const* env)
{
    if (!env)
        return 1;

    if (env == FE_DFL_ENV) {
        asm volatile("finit");
        set_mxcsr(default_mxcsr_value);
        return 0;
    }

    asm volatile("fldenv %0" ::"m"(env->__x87_fpu_env)
                 : "memory");

    set_mxcsr(env->__mxcsr);
    return 0;
}

int feholdexcept(fenv_t* env)
{
    fegetenv(env);

    fenv_t current_env;
    fegetenv(&current_env);

    current_env.__x87_fpu_env.__status_word &= ~FE_ALL_EXCEPT;
    current_env.__x87_fpu_env.__status_word &= ~(1 << 7);      // Clear the "Exception Status Summary" bit
    current_env.__x87_fpu_env.__control_word &= FE_ALL_EXCEPT; // Masking these bits stops the corresponding exceptions from being generated according to the Intel Programmer's Manual

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
    current_env.__x87_fpu_env.__status_word &= exceptions;
    current_env.__x87_fpu_env.__status_word &= ~(1 << 7); // Make sure exceptions don't get raised

    fesetenv(&current_env);
    return 0;
}

int fegetround()
{
    // There's no way to signal whether the SSE rounding mode and x87 ones are different, so we assume they're the same
    return (read_control_word() >> 10) & 3;
}

int fesetround(int rounding_mode)
{
    if (rounding_mode < FE_TONEAREST || rounding_mode > FE_TOMAXMAGNITUDE)
        return 1;

    if (rounding_mode == FE_TOMAXMAGNITUDE)
        rounding_mode = FE_TONEAREST;

    auto control_word = read_control_word();

    control_word &= ~(3 << 10);
    control_word |= rounding_mode << 10;

    set_control_word(control_word);

    auto mxcsr = read_mxcsr();

    mxcsr &= ~(3 << 13);
    mxcsr |= rounding_mode << 13;

    set_mxcsr(mxcsr);
    return 0;
}

int feclearexcept(int exceptions)
{
    exceptions &= FE_ALL_EXCEPT;

    fenv_t current_env;
    fegetenv(&current_env);

    current_env.__x87_fpu_env.__status_word &= ~exceptions;
    current_env.__x87_fpu_env.__status_word &= ~(1 << 7); // Clear the "Exception Status Summary" bit

    fesetenv(&current_env);
    return 0;
}

int fetestexcept(int exceptions)
{
    u16 status_register = read_status_register() & FE_ALL_EXCEPT;
    exceptions &= FE_ALL_EXCEPT;

    return status_register & exceptions;
}

int feraiseexcept(int exceptions)
{
    fenv_t env;
    fegetenv(&env);

    exceptions &= FE_ALL_EXCEPT;

    // While the order in which the exceptions is raised is unspecified, FE_OVERFLOW and FE_UNDERFLOW must be raised before FE_INEXACT, so handle that case in this branch
    if (exceptions & FE_INEXACT) {
        env.__x87_fpu_env.__status_word &= ((u16)exceptions & ~FE_INEXACT);
        fesetenv(&env);
        asm volatile("fwait"); // "raise" the exception by performing a floating point operation

        fegetenv(&env);
        env.__x87_fpu_env.__status_word &= FE_INEXACT;
        fesetenv(&env);
        asm volatile("fwait");

        return 0;
    }

    env.__x87_fpu_env.__status_word &= exceptions;
    fesetenv(&env);
    asm volatile("fwait");

    return 0;
}
}
