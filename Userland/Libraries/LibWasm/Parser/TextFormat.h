/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/SourceLocation.h>
#include <LibWasm/Forward.h>
#include <LibWasm/Printer/Printer.h>
#include <LibWasm/Types.h>

namespace Wasm {
struct TextFormatParseError : public Error {
    TextFormatParseError(size_t line, size_t column, String error, SourceLocation location = SourceLocation::current())
        : Error(Error::from_string_literal("Text Format Parse Error"sv))
        , line(line)
        , column(column)
        , error(move(error))
        , location(location)
    {
    }

    TextFormatParseError(GenericLexer const& lexer, String error, SourceLocation location = SourceLocation::current());

    size_t line;
    size_t column;
    String error;
    SourceLocation location;
};

ErrorOr<void, TextFormatParseError> parse_and_generate_module_from_text_format(GenericLexer& lexer, OutputStream& output_stream);

}
