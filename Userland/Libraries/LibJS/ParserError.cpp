/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibJS/ParserError.h>
#include <LibJS/Token.h>

namespace JS {

DeprecatedString ParserError::to_string() const
{
    if (!position.has_value())
        return message;
    return DeprecatedString::formatted("{} (line: {}, column: {})", message, position.value().line, position.value().column);
}

DeprecatedString ParserError::source_location_hint(StringView source, char const spacer, char const indicator) const
{
    if (!position.has_value())
        return {};
    // We need to modify the source to match what the lexer considers one line - normalizing
    // line terminators to \n is easier than splitting using all different LT characters.
    DeprecatedString source_string = source.replace("\r\n"sv, "\n"sv, ReplaceMode::All).replace("\r"sv, "\n"sv, ReplaceMode::All).replace(LINE_SEPARATOR_STRING, "\n"sv, ReplaceMode::All).replace(PARAGRAPH_SEPARATOR_STRING, "\n"sv, ReplaceMode::All);
    StringBuilder builder;
    builder.append(source_string.split_view('\n', SplitBehavior::KeepEmpty)[position.value().line - 1]);
    builder.append('\n');
    for (size_t i = 0; i < position.value().column - 1; ++i)
        builder.append(spacer);
    builder.append(indicator);
    return builder.build();
}

}
