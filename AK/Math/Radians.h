/*
 * Copyright (c) 2021-2026, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Math/Constants.h>

namespace AK {

template<FloatingPoint T>
constexpr T to_radians(T degrees)
{
    return degrees * AK::Pi<T> / 180;
}

template<FloatingPoint T>
constexpr T to_degrees(T radians)
{
    return radians * 180 / AK::Pi<T>;
}

}
