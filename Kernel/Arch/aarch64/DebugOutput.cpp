/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/DebugOutput.h>

namespace Kernel {

static DebugConsole const* s_debug_console;

void set_debug_console(DebugConsole const* debug_console)
{
    s_debug_console = debug_console;
}

void debug_output(char character)
{
    if (s_debug_console != nullptr)
        s_debug_console->write_character(character);
}

}
