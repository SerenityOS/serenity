/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>

namespace Shell {

String escape_token_for_double_quotes(const StringView token);
String escape_token_for_single_quotes(const StringView token);
String escape_token(const StringView token);
String unescape_token(const StringView token);

enum class SpecialCharacterEscapeMode {
    Untouched,
    Escaped,
    QuotedAsEscape,
    QuotedAsHex,
};

SpecialCharacterEscapeMode special_character_escape_mode(u32 c);

}
