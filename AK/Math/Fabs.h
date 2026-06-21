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
constexpr T fabs(T x)
{
    // Both GCC and Clang inline fabs by default, so this is just a cmath like wrapper
    CALL_BUILTIN(fabs, x);
}

}

#include <AK/Math/UndefMacros.h>
