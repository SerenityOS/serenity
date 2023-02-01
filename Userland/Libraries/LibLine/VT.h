/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibCore/Stream.h>
#include <LibLine/Style.h>

namespace Line {
namespace VT {

ErrorOr<void> save_cursor(AK::Stream&);
ErrorOr<void> restore_cursor(AK::Stream&);
ErrorOr<void> clear_to_end_of_line(AK::Stream&);
ErrorOr<void> clear_lines(size_t count_above, size_t count_below, AK::Stream&);
ErrorOr<void> move_relative(int x, int y, AK::Stream&);
ErrorOr<void> move_absolute(u32 x, u32 y, AK::Stream&);
ErrorOr<void> apply_style(Style const&, AK::Stream&, bool is_starting = true);

}
}
