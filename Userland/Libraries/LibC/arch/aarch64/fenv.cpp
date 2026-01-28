/*
 * Copyright (c) 2024, Tom Finet <tom.codeninja@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <fenv.h>

static inline uint64_t read_fpcr()
{
    uint64_t value;
    asm volatile("mrs %0, fpcr"
        : "=r"(value));
    return value;
}

static inline void write_fpcr(uint64_t value)
{
    asm volatile("msr fpcr, %0"
        :
        : "r"(value));
}

static inline uint64_t read_fpsr()
{
    uint64_t value;
    asm volatile("mrs %0, fpsr"
        : "=r"(value));
    return value;
}

static inline void write_fpsr(uint64_t value)
{
    asm volatile("msr fpsr, %0"
        :
        : "r"(value));
}

static uint32_t serenity_to_arm_exceptions(int exceptions)
{
    uint32_t arm = 0;
    if (exceptions & FE_INVALID)
        arm |= (1u << 0);
    if (exceptions & FE_DIVBYZERO)
        arm |= (1u << 1);
    if (exceptions & FE_OVERFLOW)
        arm |= (1u << 2);
    if (exceptions & FE_UNDERFLOW)
        arm |= (1u << 3);
    if (exceptions & FE_INEXACT)
        arm |= (1u << 4);
    return arm;
}

static int arm_to_serenity_exceptions(uint32_t arm)
{
    int exceptions = 0;
    if (arm & (1u << 0))
        exceptions |= FE_INVALID;
    if (arm & (1u << 1))
        exceptions |= FE_DIVBYZERO;
    if (arm & (1u << 2))
        exceptions |= FE_OVERFLOW;
    if (arm & (1u << 3))
        exceptions |= FE_UNDERFLOW;
    if (arm & (1u << 4))
        exceptions |= FE_INEXACT;
    return exceptions;
}

extern "C" {

int fegetenv(fenv_t* env)
{
    if (!env)
        return 1;

    env->fpcr = static_cast<uint32_t>(read_fpcr());
    env->fpsr = static_cast<uint32_t>(read_fpsr());
    return 0;
}

int fesetenv(fenv_t const* env)
{
    if (!env)
        return 1;

    if (env == FE_DFL_ENV) {
        write_fpcr(0);
        write_fpsr(0);
    } else {
        write_fpcr(env->fpcr);
        write_fpsr(env->fpsr);
    }
    return 0;
}

int feholdexcept(fenv_t* env)
{
    if (fegetenv(env) != 0)
        return 1;

    uint32_t fpcr = env->fpcr;
    // Disable all exception traps by clearing bits 8-12 and 15
    fpcr &= ~(0x1f << 8);
    fpcr &= ~(1u << 15);
    write_fpcr(fpcr);

    // Clear exception flags
    write_fpsr(0);

    return 0;
}

int fesetexceptflag(fexcept_t const* except, int exceptions)
{
    if (!except)
        return 1;

    uint32_t arm_bits = serenity_to_arm_exceptions(exceptions);
    uint32_t fpsr = static_cast<uint32_t>(read_fpsr());
    fpsr &= ~arm_bits;
    fpsr |= (serenity_to_arm_exceptions(*except) & arm_bits);
    write_fpsr(fpsr);

    return 0;
}

int fegetround(void)
{
    uint32_t fpcr = static_cast<uint32_t>(read_fpcr());
    uint32_t arm_round = (fpcr >> 22) & 3;
    switch (arm_round) {
    case 0:
        return FE_TONEAREST;
    case 1:
        return FE_UPWARD;
    case 2:
        return FE_DOWNWARD;
    case 3:
        return FE_TOWARDZERO;
    }
    return FE_TONEAREST;
}

int fesetround(int rounding_mode)
{
    if (rounding_mode < FE_TONEAREST || rounding_mode > FE_TOMAXMAGNITUDE)
        return 1;

    if (rounding_mode == FE_TOMAXMAGNITUDE)
        rounding_mode = FE_TONEAREST;

    uint32_t arm_round;
    switch (rounding_mode) {
    case FE_TONEAREST:
        arm_round = 0;
        break;
    case FE_UPWARD:
        arm_round = 1;
        break;
    case FE_DOWNWARD:
        arm_round = 2;
        break;
    case FE_TOWARDZERO:
        arm_round = 3;
        break;
    default:
        return 1;
    }

    uint32_t fpcr = static_cast<uint32_t>(read_fpcr());
    fpcr &= ~(3u << 22);
    fpcr |= (arm_round << 22);
    write_fpcr(fpcr);

    return 0;
}

int feclearexcept(int exceptions)
{
    uint32_t arm_bits = serenity_to_arm_exceptions(exceptions);
    uint32_t fpsr = static_cast<uint32_t>(read_fpsr());
    fpsr &= ~arm_bits;
    write_fpsr(fpsr);
    return 0;
}

int fetestexcept(int exceptions)
{
    uint32_t fpsr = static_cast<uint32_t>(read_fpsr());
    return arm_to_serenity_exceptions(fpsr) & exceptions;
}

int feraiseexcept(int exceptions)
{
    uint32_t arm_bits = serenity_to_arm_exceptions(exceptions);
    uint32_t fpsr = static_cast<uint32_t>(read_fpsr());
    fpsr |= arm_bits;
    write_fpsr(fpsr);
    return 0;
}

int fegetexceptflag(fexcept_t* flagp, int exceptions)
{
    if (!flagp)
        return 1;
    uint32_t fpsr = static_cast<uint32_t>(read_fpsr());
    *flagp = static_cast<fexcept_t>(arm_to_serenity_exceptions(fpsr & serenity_to_arm_exceptions(exceptions)));
    return 0;
}

int feupdateenv(fenv_t const* envp)
{
    int exceptions = arm_to_serenity_exceptions(static_cast<uint32_t>(read_fpsr()));
    if (fesetenv(envp) != 0)
        return 1;
    feraiseexcept(exceptions);
    return 0;
}
}
