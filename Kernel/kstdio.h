/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>

extern "C" {
void dbgputstr(const char*, size_t);
void kernelputstr(const char*, size_t);
void kernelcriticalputstr(const char*, size_t);
void kernelearlyputstr(const char*, size_t);
int snprintf(char* buf, size_t, const char* fmt, ...) __attribute__((format(printf, 3, 4)));
void set_serial_debug(bool on_or_off);
int get_serial_debug();
}

void dbgputstr(StringView view);
