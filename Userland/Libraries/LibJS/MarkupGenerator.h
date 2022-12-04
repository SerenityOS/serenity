/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/HashTable.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Error.h>

namespace JS {

class MarkupGenerator {
public:
    static DeprecatedString html_from_source(StringView);
    static DeprecatedString html_from_value(Value);
    static DeprecatedString html_from_error(Error const&, bool);

private:
    enum class StyleType {
        Invalid,
        String,
        Number,
        KeywordBold,
        Punctuation,
        Operator,
        Keyword,
        ControlKeyword,
        Identifier,
        ObjectType,
    };

    static void value_to_html(Value, StringBuilder& output_html, HashTable<Object*> seen_objects = {});
    static void array_to_html(Array const&, StringBuilder& output_html, HashTable<Object*>&);
    static void object_to_html(Object const&, StringBuilder& output_html, HashTable<Object*>&);
    static void function_to_html(Object const&, StringBuilder& output_html, HashTable<Object*>&);
    static void date_to_html(Object const&, StringBuilder& output_html, HashTable<Object*>&);
    static void error_to_html(Error const&, StringBuilder& output_html, bool in_promise);
    static void trace_to_html(TracebackFrame const&, StringBuilder& output_html);

    static DeprecatedString style_from_style_type(StyleType);
    static StyleType style_type_for_token(Token);
    static DeprecatedString open_style_type(StyleType type);
    static DeprecatedString wrap_string_in_style(DeprecatedString source, StyleType type);
};

}
