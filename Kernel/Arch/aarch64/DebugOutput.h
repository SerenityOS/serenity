/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/DebugOutput.h>

namespace Kernel {

struct DebugConsole {
    void (*write_character)(char);
};

void set_debug_console(DebugConsole const*);

}
