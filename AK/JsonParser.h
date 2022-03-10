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

    ErrorOr<JsonValue> parse();

private:
    ErrorOr<JsonValue> parse_helper();

    ErrorOr<String> consume_and_unescape_string();
    ErrorOr<JsonValue> parse_array();
    ErrorOr<JsonValue> parse_object();
    ErrorOr<JsonValue> parse_number();
    ErrorOr<JsonValue> parse_string();
    ErrorOr<JsonValue> parse_false();
    ErrorOr<JsonValue> parse_true();
    ErrorOr<JsonValue> parse_null();
};

}

using AK::JsonParser;
