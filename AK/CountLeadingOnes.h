/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CountLeadingZeros.h>

namespace AK {

template<Integral T>
ALWAYS_INLINE constexpr size_t count_leading_ones(T val)
{
    return AK::count_leading_zeros<T>(~val);
}

}

using AK::count_leading_ones;
