/*
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Types.h>
#include <fenv.h>

// This is the size of the floating point envinronment image in protected mode
static_assert(sizeof(__x87_floating_point_environment) == 28);

static u16 read_status_register()
{
    u16 status_register;
    asm volatile("fstsw %0"
                 : "=m"(status_register));
    return status_register;
}

static u16 read_control_word()
{
    u16 control_word;
    asm volatile("fstcw %0"
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

    asm volatile("fstenv %0"
                 : "=m"(env->__x87_fpu_env)::"memory");

    env->__mxcsr = read_mxcsr();

    return 0;
}

int fesetenv(const fenv_t* env)
{
    if (!env)
        return 1;

    if (env == FE_DFL_ENV) {
        asm volatile("finit");
        set_mxcsr(default_mxcsr_value);
        return 0;
    }

    asm volatile("fldenv %0" ::"m"(env)
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

int feupdateenv(const fenv_t* env)
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
int fesetexceptflag(const fexcept_t* except, int exceptions)
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
    return (read_status_register() >> 10) & 3;
}

int fesetround(int rounding_mode)
{
    if (rounding_mode < FE_TONEAREST || rounding_mode > FE_TOWARDZERO)
        return 1;

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
