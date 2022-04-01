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
void kernelearlyputstr(char const*, size_t);
int snprintf(char* buf, size_t, char const* fmt, ...) __attribute__((format(printf, 3, 4)));
void set_serial_debug(bool on_or_off);
int get_serial_debug();
}

void dbgputstr(StringView view);
