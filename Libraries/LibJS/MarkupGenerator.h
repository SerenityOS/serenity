/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/String.h>
#include <LibJS/Forward.h>

namespace JS {

class MarkupGenerator {
public:
    static String html_from_source(const StringView&);
    static String html_from_value(Value);

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
        Identifier
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
