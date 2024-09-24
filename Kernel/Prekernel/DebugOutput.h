/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CheckedFormatString.h>
#include <AK/FixedStringBuffer.h>
#include <AK/Format.h>
#include <AK/kmalloc.h>
#include <Kernel/Prekernel/Runtime.h>

void debug_write_string(StringView str);

template<typename... Parameters>
void write_debug_output(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::No, Parameters...> variadic_format_parameters { parameters... };
    auto message_buffer_or_error = FixedStringBuffer<128>::vformatted(fmtstr.view(), variadic_format_parameters);
    if (message_buffer_or_error.is_error()) {
        debug_write_string("PANIC: Failed to write message buffer:\n"sv);
        debug_write_string(fmtstr.view());
        halt();
    }

    auto message_buffer = message_buffer_or_error.release_value();
    debug_write_string(message_buffer.representable_view());
}
