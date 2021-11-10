/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/String.h>
#include <LibJS/Forward.h>

namespace JS {

class MarkupGenerator {
public:
    static String html_from_source(StringView);
    static String html_from_value(Value);
    static String html_from_error(Object&);

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
    static void array_to_html(const Array&, StringBuilder& output_html, HashTable<Object*>&);
    static void object_to_html(const Object&, StringBuilder& output_html, HashTable<Object*>&);
    static void function_to_html(const Object&, StringBuilder& output_html, HashTable<Object*>&);
    static void date_to_html(const Object&, StringBuilder& output_html, HashTable<Object*>&);
    static void error_to_html(const Object&, StringBuilder& output_html, HashTable<Object*>&);

    static String style_from_style_type(StyleType);
    static StyleType style_type_for_token(Token);
    static String open_style_type(StyleType type);
    static String wrap_string_in_style(String source, StyleType type);
};

}
