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
    static void spawn_or_show_error(Window* parent_window, StringView path, ReadonlySpan<ByteString> arguments, StringView working_directory = {});
    static void spawn_or_show_error(Window* parent_window, StringView path, ReadonlySpan<StringView> arguments, StringView working_directory = {});
    static void spawn_or_show_error(Window* parent_window, StringView path, ReadonlySpan<char const*> arguments = {}, StringView working_directory = {});
};

}
