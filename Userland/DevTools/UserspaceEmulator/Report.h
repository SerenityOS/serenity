/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Format.h>

extern bool g_report_to_debug;

template<typename... Ts>
void reportln(const StringView& format, Ts... args)
{
    if (g_report_to_debug) {
        YAK::VariadicFormatParams variadic_format_params { args... };
        YAK::vdbgln(format, variadic_format_params);
    } else {
        warnln(format, args...);
    }
}
