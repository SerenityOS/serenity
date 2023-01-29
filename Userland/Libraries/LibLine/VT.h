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

ErrorOr<void> save_cursor(Core::Stream::Stream&);
ErrorOr<void> restore_cursor(Core::Stream::Stream&);
ErrorOr<void> clear_to_end_of_line(Core::Stream::Stream&);
ErrorOr<void> clear_lines(size_t count_above, size_t count_below, Core::Stream::Stream&);
ErrorOr<void> move_relative(int x, int y, Core::Stream::Stream&);
ErrorOr<void> move_absolute(u32 x, u32 y, Core::Stream::Stream&);
ErrorOr<void> apply_style(Style const&, Core::Stream::Stream&, bool is_starting = true);

}
}
