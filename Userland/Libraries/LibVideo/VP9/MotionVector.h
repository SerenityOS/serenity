/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace Video::VP9 {

struct MotionVector {

    constexpr MotionVector operator+(MotionVector const& other) const
    {
        return { this->row + other.row, this->column + other.column };
    }
    constexpr MotionVector& operator+=(MotionVector const& other)
    {
        *this = *this + other;
        return *this;
    }

    constexpr MotionVector operator*(i32 scalar) const
    {
        return { this->row * scalar, this->column * scalar };
    }
    constexpr MotionVector& operator*=(i32 scalar)
    {
        *this = *this * scalar;
        return *this;
    }

    constexpr bool operator==(MotionVector const& other) const = default;

    i32 row;
    i32 column;
};

static_assert(IsPOD<MotionVector>);

}
