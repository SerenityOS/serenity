/*
 * Copyright (c) 2024, Tom Finet <tom.codeninja@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/EnumBits.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <fenv.h>

static_assert(AssertSize<fenv_t, 4>());

// RISC-V F extension version 2.2
// Table 11.1 (frm rounding mode encoding)
enum class RoundingMode : u8 {
    // Round to Nearest, ties to Even
    RNE = 0b000,
    // Round towards Zero
    RTZ = 0b001,
    // Round Down (towards −∞)
    RDN = 0b010,
    // Round Up (towards +∞)
    RUP = 0b011,
    // Round to Nearest, ties to Max Magnitude
    RMM = 0b100,
    // Reserved for future use.
    Reserved5 = 0b101,
    Reserved6 = 0b110,
    // In instruction’s rm field, selects dynamic rounding mode; In Rounding Mode register, Invalid.
    DYN = 0b111,
};

static RoundingMode frm_from_feround(int c_rounding_mode)
{
    switch (c_rounding_mode) {
    case FE_TONEAREST:
        return RoundingMode::RNE;
    case FE_TOWARDZERO:
        return RoundingMode::RTZ;
    case FE_DOWNWARD:
        return RoundingMode::RDN;
    case FE_UPWARD:
        return RoundingMode::RUP;
    case FE_TOMAXMAGNITUDE:
        return RoundingMode::RMM;
    default:
        VERIFY_NOT_REACHED();
    }
}

static int feround_from_frm(RoundingMode frm)
{
    switch (frm) {
    case RoundingMode::RNE:
        return FE_TONEAREST;
    case RoundingMode::RTZ:
        return FE_TOWARDZERO;
    case RoundingMode::RDN:
        return FE_DOWNWARD;
    case RoundingMode::RUP:
        return FE_UPWARD;
    case RoundingMode::RMM:
        return FE_TOMAXMAGNITUDE;
    default:
        // DYN is invalid in the frm register and therefore should never appear here.
    case RoundingMode::DYN:
        VERIFY_NOT_REACHED();
    }
}

static RoundingMode get_rounding_mode()
{
    size_t rounding_mode;
    asm volatile("frrm %0"
                 : "=r"(rounding_mode));
    return static_cast<RoundingMode>(rounding_mode);
}

// Returns the old rounding mode, since we get that for free.
static RoundingMode set_rounding_mode(RoundingMode frm)
{
    size_t old_rounding_mode;
    size_t const new_rounding_mode = to_underlying(frm);
    asm volatile("fsrm %0, %1"
                 : "=r"(old_rounding_mode)
                 : "r"(new_rounding_mode));
    return static_cast<RoundingMode>(old_rounding_mode);
}

// Figure 11.2 (fflags)
enum class AccruedExceptions : u8 {
    None = 0,
    // Inexact
    NX = 1 << 0,
    // Underflow
    UF = 1 << 1,
    // Overflow
    OF = 1 << 2,
    // Divide by Zero
    DZ = 1 << 3,
    // Invalid Operation
    NV = 1 << 4,
    All = NX | UF | OF | DZ | NV,
};

AK_ENUM_BITWISE_OPERATORS(AccruedExceptions);

static AccruedExceptions fflags_from_fexcept(fexcept_t c_exceptions)
{
    AccruedExceptions exceptions = AccruedExceptions::None;
    if ((c_exceptions & FE_INEXACT) != 0)
        exceptions |= AccruedExceptions::NX;
    if ((c_exceptions & FE_UNDERFLOW) != 0)
        exceptions |= AccruedExceptions::UF;
    if ((c_exceptions & FE_OVERFLOW) != 0)
        exceptions |= AccruedExceptions::OF;
    if ((c_exceptions & FE_DIVBYZERO) != 0)
        exceptions |= AccruedExceptions::DZ;
    if ((c_exceptions & FE_INVALID) != 0)
        exceptions |= AccruedExceptions::NV;

    return exceptions;
}

static fexcept_t fexcept_from_fflags(AccruedExceptions fflags)
{
    fexcept_t c_exceptions = 0;
    if ((fflags & AccruedExceptions::NX) != AccruedExceptions::None)
        c_exceptions |= FE_INEXACT;
    if ((fflags & AccruedExceptions::UF) != AccruedExceptions::None)
        c_exceptions |= FE_UNDERFLOW;
    if ((fflags & AccruedExceptions::OF) != AccruedExceptions::None)
        c_exceptions |= FE_OVERFLOW;
    if ((fflags & AccruedExceptions::DZ) != AccruedExceptions::None)
        c_exceptions |= FE_DIVBYZERO;
    if ((fflags & AccruedExceptions::NV) != AccruedExceptions::None)
        c_exceptions |= FE_INVALID;

    return c_exceptions;
}

static AccruedExceptions get_accrued_exceptions()
{
    size_t fflags;
    asm volatile("frflags %0"
                 : "=r"(fflags));
    return static_cast<AccruedExceptions>(fflags);
}

// Returns the old exceptions, since we get them for free.
static AccruedExceptions set_accrued_exceptions(AccruedExceptions exceptions)
{
    size_t old_exceptions;
    size_t const new_exceptions = to_underlying(exceptions);
    asm volatile("fsflags %0, %1"
                 : "=r"(old_exceptions)
                 : "r"(new_exceptions));
    return static_cast<AccruedExceptions>(old_exceptions);
}

static void clear_accrued_exceptions(AccruedExceptions exceptions)
{
    asm volatile("csrc fcsr, %0" ::"r"(to_underlying(exceptions)));
}

extern "C" {

int fegetenv(fenv_t* env)
{
    if (!env)
        return 1;

    FlatPtr fcsr;
    asm volatile("csrr %0, fcsr"
                 : "=r"(fcsr));
    env->fcsr = fcsr;

    return 0;
}

int fesetenv(fenv_t const* env)
{
    if (!env)
        return 1;

    FlatPtr fcsr = env->fcsr;
    asm volatile("csrw fcsr, %0" ::"r"(fcsr));
    return 0;
}

int feholdexcept(fenv_t* env)
{
    fegetenv(env);

    // RISC-V does not have trapping floating point exceptions. Therefore, feholdexcept just clears fflags.
    clear_accrued_exceptions(AccruedExceptions::All);
    return 0;
}

int fesetexceptflag(fexcept_t const* except, int exceptions)
{
    if (!except)
        return 1;

    exceptions &= FE_ALL_EXCEPT;

    auto exceptions_to_set = fflags_from_fexcept(*except) & fflags_from_fexcept(exceptions);
    set_accrued_exceptions(exceptions_to_set);

    return 0;
}

int fegetround()
{
    auto rounding_mode = get_rounding_mode();
    return feround_from_frm(rounding_mode);
}

int fesetround(int rounding_mode)
{
    if (rounding_mode < FE_TONEAREST || rounding_mode > FE_TOMAXMAGNITUDE)
        return 1;

    auto frm = frm_from_feround(rounding_mode);
    set_rounding_mode(frm);

    return 0;
}

int feclearexcept(int exceptions)
{
    exceptions &= FE_ALL_EXCEPT;

    auto exception_clear_flag = fflags_from_fexcept(exceptions);
    // Use CSRRC to directly clear exception flags in fcsr which is faster.
    // Conveniently, the exception flags are the lower bits, so we don't need to shift anything around.
    clear_accrued_exceptions(exception_clear_flag);

    return 0;
}

int fetestexcept(int exceptions)
{
    auto fflags = get_accrued_exceptions();
    auto mask = fflags_from_fexcept(exceptions);
    return fexcept_from_fflags(fflags & mask);
}

int feraiseexcept(int exceptions)
{
    fenv_t env;
    fegetenv(&env);

    exceptions &= FE_ALL_EXCEPT;

    // RISC-V does not have trapping floating-point exceptions, so this function behaves as a simple exception setter.
    set_accrued_exceptions(fflags_from_fexcept(exceptions));

    return 0;
}
}
