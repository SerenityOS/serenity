/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Video::VP9 {

class MotionVector {
public:
    MotionVector() = default;
    MotionVector(u32 row, u32 col);

    u32 row() const { return m_row; }
    void set_row(u32 row) { m_row = row; }
    u32 col() const { return m_col; }
    void set_col(u32 col) { m_col = col; }

    MotionVector& operator=(i32 value);
    MotionVector operator+(MotionVector const& other) const;

private:
    u32 m_row { 0 };
    u32 m_col { 0 };
};

}
