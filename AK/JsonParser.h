/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/JsonValue.h>

namespace AK {

class JsonParser : private GenericLexer {
public:
    explicit JsonParser(StringView input)
        : GenericLexer(input)
    {
    }

    Optional<JsonValue> parse();

private:
    Optional<JsonValue> parse_helper();

    String consume_and_unescape_string();
    Optional<JsonValue> parse_array();
    Optional<JsonValue> parse_object();
    Optional<JsonValue> parse_number();
    Optional<JsonValue> parse_string();
    Optional<JsonValue> parse_false();
    Optional<JsonValue> parse_true();
    Optional<JsonValue> parse_null();

    String m_last_string_starting_with_character[256];
};

}

using AK::JsonParser;
