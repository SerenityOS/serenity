/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CountTrailingZeros.h>

namespace AK {

template<Integral T>
ALWAYS_INLINE constexpr size_t count_trailing_ones(T val)
{
    return AK::count_trailing_zeros<T>(~val);
}

}

using AK::count_trailing_ones;
