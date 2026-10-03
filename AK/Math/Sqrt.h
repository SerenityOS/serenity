/*
 * Copyright (c) 2021-2026, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>

#include <AK/Math/Macros.h>

namespace AK {

template<FloatingPoint T>
constexpr T sqrt(T x)
{
    CONSTEXPR_STATE(sqrt, x);

#if ARCH(X86_64)
    if constexpr (IsSame<T, float>) {
        float res;
        asm("sqrtss %1, %0"
            : "=x"(res)
            : "x"(x));
        return res;
    }
    if constexpr (IsSame<T, double>) {
        double res;
        asm("sqrtsd %1, %0"
            : "=x"(res)
            : "x"(x));
        return res;
    }
    T res;
    asm("fsqrt"
        : "=t"(res)
        : "0"(x));
    return res;
#elif ARCH(AARCH64)
    AARCH64_INSTRUCTION(fsqrt, x);
#elif ARCH(RISCV64)
    if constexpr (IsSame<T, float>) {
        float res;
        asm("fsqrt.s %0, %1"
            : "=f"(res)
            : "f"(x));
        return res;
    }
    if constexpr (IsSame<T, double>) {
        double res;
        asm("fsqrt.d %0, %1"
            : "=f"(res)
            : "f"(x));
        return res;
    }
#else
#    ifndef AK_OS_SERENITY
    CALL_BUILTIN(sqrt, x);
#    endif
#endif
    // TODO: Add implementation for this function.
    //       This *should* only affect long double on non-x86
    TODO();
}

#include <AK/Math/UndefMacros.h>

}
