/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CountOnes.h>

namespace AK {

template<Integral T>
ALWAYS_INLINE constexpr size_t count_zeros(T val)
{
    return AK::count_ones<T>(~val);
}

}

using AK::count_zeros;
