/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>

extern "C" {
void dbgputstr(char const*, size_t);
void kernelputstr(char const*, size_t);
void kernelcriticalputstr(char const*, size_t);
void dbgputchar(char);
void kernelearlyputstr(char const*, size_t);
void set_serial_debug_enabled(bool desired_state);
bool is_serial_debug_enabled();
}

void dbgputstr(StringView view);
