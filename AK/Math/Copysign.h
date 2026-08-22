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
constexpr T copysign(T x, T y)
{
    CALL_BUILTIN(copysign, x, y);
}

#include <AK/Math/UndefMacros.h>

}
