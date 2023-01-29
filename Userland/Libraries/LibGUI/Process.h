/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Process.h>
#include <LibGUI/Forward.h>

namespace GUI {

struct Process {
    static void spawn_or_show_error(Window* parent_window, StringView path, Span<DeprecatedString const> arguments);
    static void spawn_or_show_error(Window* parent_window, StringView path, Span<StringView const> arguments);
    static void spawn_or_show_error(Window* parent_window, StringView path, Span<char const* const> arguments = {});
};

}
