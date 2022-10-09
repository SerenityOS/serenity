/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MotionVector.h"

namespace Video::VP9 {

MotionVector::MotionVector(u32 row, u32 col)
    : m_row(row)
    , m_col(col)
{
}

MotionVector& MotionVector::operator=(i32 value)
{
    m_row = value;
    m_col = value;
    return *this;
}

MotionVector MotionVector::operator+(MotionVector const& other) const
{
    return MotionVector(this->row() + other.row(), this->col() + other.col());
}

}
