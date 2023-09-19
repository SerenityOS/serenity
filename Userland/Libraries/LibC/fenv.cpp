/*
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/EnumBits.h>
#include <AK/Types.h>
#include <fenv.h>

#if ARCH(X86_64)

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

#elif ARCH(RISCV64)

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

#endif

extern "C" {

int fegetenv(fenv_t* env)
{
    if (!env)
        return 1;

#if ARCH(AARCH64)
    (void)env;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    asm volatile("csrr %0, fcsr"
                 : "=r"(env));
#elif ARCH(X86_64)
    asm volatile("fnstenv %0"
                 : "=m"(env->__x87_fpu_env)::"memory");

    env->__mxcsr = read_mxcsr();
#else
#    error Unknown architecture
#endif

    return 0;
}

int fesetenv(fenv_t const* env)
{
    if (!env)
        return 1;

#if ARCH(AARCH64)
    (void)env;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    asm volatile("csrw fcsr, %0" ::"r"(env));
#elif ARCH(X86_64)
    if (env == FE_DFL_ENV) {
        asm volatile("finit");
        set_mxcsr(default_mxcsr_value);
        return 0;
    }

    asm volatile("fldenv %0" ::"m"(env->__x87_fpu_env)
                 : "memory");

    set_mxcsr(env->__mxcsr);
#else
#    error Unknown architecture
#endif

    return 0;
}

int feholdexcept(fenv_t* env)
{
    fegetenv(env);

#if ARCH(AARCH64)
    (void)env;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    // RISC-V does not have trapping floating point exceptions. Therefore, feholdexcept just clears fflags.
    clear_accrued_exceptions(AccruedExceptions::All);
#elif ARCH(X86_64)
    fenv_t current_env;
    fegetenv(&current_env);

    current_env.__x87_fpu_env.__status_word &= ~FE_ALL_EXCEPT;
    current_env.__x87_fpu_env.__status_word &= ~(1 << 7);      // Clear the "Exception Status Summary" bit
    current_env.__x87_fpu_env.__control_word &= FE_ALL_EXCEPT; // Masking these bits stops the corresponding exceptions from being generated according to the Intel Programmer's Manual

    fesetenv(&current_env);
#else
#    error Unknown architecture
#endif

    return 0;
}

int feupdateenv(fenv_t const* env)
{
    auto currently_raised_exceptions = fetestexcept(FE_ALL_EXCEPT);

    fesetenv(env);
    feraiseexcept(currently_raised_exceptions);

    return 0;
}

int fegetexceptflag(fexcept_t* except, int exceptions)
{
    if (!except)
        return 1;
    *except = (uint16_t)fetestexcept(exceptions);
    return 0;
}
int fesetexceptflag(fexcept_t const* except, int exceptions)
{
    if (!except)
        return 1;

    exceptions &= FE_ALL_EXCEPT;

#if ARCH(AARCH64)
    (void)exceptions;
    (void)except;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    auto exceptions_to_set = fflags_from_fexcept(*except) & fflags_from_fexcept(exceptions);
    set_accrued_exceptions(exceptions_to_set);
#elif ARCH(X86_64)
    fenv_t current_env;
    fegetenv(&current_env);
    current_env.__x87_fpu_env.__status_word &= exceptions;
    current_env.__x87_fpu_env.__status_word &= ~(1 << 7); // Make sure exceptions don't get raised
    fesetenv(&current_env);
#else
#    error Unknown architecture
#endif

    return 0;
}

int fegetround()
{
#if ARCH(AARCH64)
    TODO_AARCH64();
#elif ARCH(RISCV64)
    auto rounding_mode = get_rounding_mode();
    return feround_from_frm(rounding_mode);
#elif ARCH(X86_64)
    // There's no way to signal whether the SSE rounding mode and x87 ones are different, so we assume they're the same
    return (read_status_register() >> 10) & 3;
#else
#    error Unknown architecture
#endif
}

int fesetround(int rounding_mode)
{
    if (rounding_mode < FE_TONEAREST || rounding_mode > FE_TOMAXMAGNITUDE)
        return 1;

#if ARCH(AARCH64)
    TODO_AARCH64();
#elif ARCH(RISCV64)
    auto frm = frm_from_feround(rounding_mode);
    set_rounding_mode(frm);
#elif ARCH(X86_64)
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
#else
#    error Unknown architecture
#endif

    return 0;
}

int feclearexcept(int exceptions)
{
    exceptions &= FE_ALL_EXCEPT;

#if ARCH(AARCH64)
    (void)exceptions;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    auto exception_clear_flag = fflags_from_fexcept(exceptions);
    // Use CSRRC to directly clear exception flags in fcsr which is faster.
    // Conveniently, the exception flags are the lower bits, so we don't need to shift anything around.
    clear_accrued_exceptions(exception_clear_flag);
#elif ARCH(X86_64)

    fenv_t current_env;
    fegetenv(&current_env);
    current_env.__x87_fpu_env.__status_word &= ~exceptions;
    current_env.__x87_fpu_env.__status_word &= ~(1 << 7); // Clear the "Exception Status Summary" bit
    fesetenv(&current_env);

#else
#    error Unknown architecture
#endif

    return 0;
}

int fetestexcept(int exceptions)
{
#if ARCH(AARCH64)
    (void)exceptions;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    auto fflags = get_accrued_exceptions();
    auto mask = fflags_from_fexcept(exceptions);
    return fexcept_from_fflags(fflags & mask);
#elif ARCH(X86_64)
    u16 status_register = read_status_register() & FE_ALL_EXCEPT;
    exceptions &= FE_ALL_EXCEPT;

    return status_register & exceptions;
#else
#    error Unknown architecture
#endif
}

int feraiseexcept(int exceptions)
{
    fenv_t env;
    fegetenv(&env);

    exceptions &= FE_ALL_EXCEPT;

#if ARCH(AARCH64)
    (void)exceptions;
    TODO_AARCH64();
#elif ARCH(RISCV64)
    // RISC-V does not have trapping floating-point exceptions, so this function behaves as a simple exception setter.
    set_accrued_exceptions(fflags_from_fexcept(exceptions));
#elif ARCH(X86_64)
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
#else
#    error Unknown architecture
#endif

    return 0;
}
}
