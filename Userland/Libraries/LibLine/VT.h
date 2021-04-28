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

void save_cursor();
void restore_cursor();
void clear_to_end_of_line();
void clear_lines(size_t count_above, size_t count_below = 0);
void move_relative(int x, int y);
void move_absolute(u32 x, u32 y);
void apply_style(const Style&, bool is_starting = true);

}
}
