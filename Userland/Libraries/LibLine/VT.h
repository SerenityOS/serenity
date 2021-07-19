/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibLine/Style.h>

namespace Line {
namespace VT {

void save_cursor(OutputStream&);
void restore_cursor(OutputStream&);
void clear_to_end_of_line(OutputStream&);
void clear_lines(size_t count_above, size_t count_below, OutputStream&);
void move_relative(int x, int y, OutputStream&);
void move_absolute(u32 x, u32 y, OutputStream&);
void apply_style(const Style&, OutputStream&, bool is_starting = true);

}
}
