/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MV.h"

namespace Video::VP9 {

MV::MV(u32 row, u32 col)
    : m_row(row)
    , m_col(col)
{
}

MV& MV::operator=(MV const& other)
{
    if (this == &other)
        return *this;
    m_row = other.row();
    m_col = other.col();
    return *this;
}

MV& MV::operator=(i32 value)
{
    m_row = value;
    m_col = value;
    return *this;
}

MV MV::operator+(MV const& other) const
{
    return MV(this->row() + other.row(), this->col() + other.col());
}

}
