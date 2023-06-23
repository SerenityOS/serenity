/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace WebContent {

enum class ConsoleExecutionMode {
    Regular,
    InCurrentScope,
};

struct StackFrame {
    DeprecatedString source_url;
    DeprecatedString function_name;
    size_t line { 0 };
    size_t column { 0 };
};

}

template<>
inline ErrorOr<void> IPC::encode<WebContent::StackFrame>(IPC::Encoder& encoder, WebContent::StackFrame const& frame)
{
    TRY(encoder.encode(frame.source_url));
    TRY(encoder.encode(frame.function_name));
    TRY(encoder.encode(frame.line));
    TRY(encoder.encode(frame.column));
    return {};
}

template<>
inline ErrorOr<WebContent::StackFrame> IPC::decode(IPC::Decoder& decoder)
{
    WebContent::StackFrame frame;
    frame.source_url = TRY(decoder.decode<DeprecatedString>());
    frame.function_name = TRY(decoder.decode<DeprecatedString>());
    frame.line = TRY(decoder.decode<size_t>());
    frame.column = TRY(decoder.decode<size_t>());
    return frame;
}
