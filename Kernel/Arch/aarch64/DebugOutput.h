/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
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
