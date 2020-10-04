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

#include <AK/HashTable.h>
#include <AK/StringBuilder.h>
#include <LibJS/Lexer.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

String MarkupGenerator::html_from_source(const StringView& source)
{
    StringBuilder builder;
    size_t source_cursor = 0;

    auto lexer = Lexer(source);
    for (auto token = lexer.next(); token.type() != TokenType::Eof; token = lexer.next()) {
        auto length = token.value().length();
        auto start = token.line_column() - 1;

        if (start > source_cursor) {
            builder.append(source.substring_view(source_cursor, start - source_cursor));
        }

        builder.append(wrap_string_in_style(token.value(), style_type_for_token(token)));
        source_cursor = start + length;
    }

    if (source_cursor < source.length())
        builder.append(source.substring_view(source_cursor, source.length() - source_cursor));

    return builder.to_string();
}

String MarkupGenerator::html_from_value(Value value)
{
    StringBuilder output_html;
    value_to_html(value, output_html);
    return output_html.to_string();
}

void MarkupGenerator::value_to_html(Value value, StringBuilder& output_html, HashTable<Object*> seen_objects)
{
    if (value.is_empty()) {
        output_html.append("&lt;empty&gt;");
        return;
    }

    if (value.is_object()) {
        if (seen_objects.contains(&value.as_object())) {
            // FIXME: Maybe we should only do this for circular references,
            //        not for all reoccurring objects.
            output_html.appendff("&lt;already printed Object {:p}&gt;", &value.as_object());
            return;
        }
        seen_objects.set(&value.as_object());
    }

    if (value.is_array())
        return array_to_html(static_cast<const Array&>(value.as_object()), output_html, seen_objects);

    if (value.is_object()) {
        auto& object = value.as_object();
        if (object.is_function())
            return function_to_html(object, output_html, seen_objects);
        if (object.is_date())
            return date_to_html(object, output_html, seen_objects);
        if (object.is_error())
            return error_to_html(object, output_html, seen_objects);
        return object_to_html(object, output_html, seen_objects);
    }

    if (value.is_string())
        output_html.append(open_style_type(StyleType::String));
    else if (value.is_number())
        output_html.append(open_style_type(StyleType::Number));
    else if (value.is_boolean() || value.is_nullish())
        output_html.append(open_style_type(StyleType::KeywordBold));

    if (value.is_string())
        output_html.append('"');
    output_html.append(escape_html_entities(value.to_string_without_side_effects()));
    if (value.is_string())
        output_html.append('"');

    output_html.append("</span>");
}

void MarkupGenerator::array_to_html(const Array& array, StringBuilder& html_output, HashTable<Object*>& seen_objects)
{
    html_output.append(wrap_string_in_style("[ ", StyleType::Punctuation));
    bool first = true;
    for (auto it = array.indexed_properties().begin(false); it != array.indexed_properties().end(); ++it) {
        if (!first)
            html_output.append(wrap_string_in_style(", ", StyleType::Punctuation));
        first = false;
        // FIXME: Exception check
        value_to_html(it.value_and_attributes(const_cast<Array*>(&array)).value, html_output, seen_objects);
    }
    html_output.append(wrap_string_in_style(" ]", StyleType::Punctuation));
}

void MarkupGenerator::object_to_html(const Object& object, StringBuilder& html_output, HashTable<Object*>& seen_objects)
{
    html_output.append(wrap_string_in_style("{ ", StyleType::Punctuation));
    bool first = true;
    for (auto& entry : object.indexed_properties()) {
        if (!first)
            html_output.append(wrap_string_in_style(", ", StyleType::Punctuation));
        first = false;
        html_output.append(wrap_string_in_style(String::number(entry.index()), StyleType::Number));
        html_output.append(wrap_string_in_style(": ", StyleType::Punctuation));
        // FIXME: Exception check
        value_to_html(entry.value_and_attributes(const_cast<Object*>(&object)).value, html_output, seen_objects);
    }

    if (!object.indexed_properties().is_empty() && object.shape().property_count())
        html_output.append(wrap_string_in_style(", ", StyleType::Punctuation));

    size_t index = 0;
    for (auto& it : object.shape().property_table_ordered()) {
        html_output.append(wrap_string_in_style(String::formatted("\"{}\"", escape_html_entities(it.key.to_display_string())), StyleType::String));
        html_output.append(wrap_string_in_style(": ", StyleType::Punctuation));
        value_to_html(object.get_direct(it.value.offset), html_output, seen_objects);
        if (index != object.shape().property_count() - 1)
            html_output.append(wrap_string_in_style(", ", StyleType::Punctuation));
        ++index;
    }

    html_output.append(wrap_string_in_style(" }", StyleType::Punctuation));
}

void MarkupGenerator::function_to_html(const Object& function, StringBuilder& html_output, HashTable<Object*>&)
{
    html_output.appendff("[{}]", function.class_name());
}

void MarkupGenerator::date_to_html(const Object& date, StringBuilder& html_output, HashTable<Object*>&)
{
    html_output.appendff("Date {}", static_cast<const JS::Date&>(date).string());
}

void MarkupGenerator::error_to_html(const Object& object, StringBuilder& html_output, HashTable<Object*>&)
{
    auto& error = static_cast<const Error&>(object);
    html_output.append(wrap_string_in_style(String::formatted("[{}]", error.name()), StyleType::Invalid));
    if (!error.message().is_empty()) {
        html_output.appendff(": {}", escape_html_entities(error.message()));
    }
}

String MarkupGenerator::style_from_style_type(StyleType type)
{
    switch (type) {
    case StyleType::Invalid:
        return "color: red;";
    case StyleType::String:
        return "color: -libweb-palette-syntax-string;";
    case StyleType::Number:
        return "color: -libweb-palette-syntax-number;";
    case StyleType::KeywordBold:
        return "color: -libweb-palette-syntax-keyword; font-weight: bold;";
    case StyleType::Punctuation:
        return "color: -libweb-palette-syntax-punctuation;";
    case StyleType::Operator:
        return "color: -libweb-palette-syntax-operator;";
    case StyleType::Keyword:
        return "color: -libweb-palette-syntax-keyword;";
    case StyleType::ControlKeyword:
        return "color: -libweb-palette-syntax-control-keyword;";
    case StyleType::Identifier:
        return "color: -libweb-palette-syntax-identifier;";
    default:
        ASSERT_NOT_REACHED();
    }
}

MarkupGenerator::StyleType MarkupGenerator::style_type_for_token(Token token)
{
    switch (token.category()) {
    case TokenCategory::Invalid:
        return StyleType::Invalid;
    case TokenCategory::Number:
        return StyleType::Number;
    case TokenCategory::String:
        return StyleType::String;
    case TokenCategory::Punctuation:
        return StyleType::Punctuation;
    case TokenCategory::Operator:
        return StyleType::Operator;
    case TokenCategory::Keyword:
        switch (token.type()) {
        case TokenType::BoolLiteral:
        case TokenType::NullLiteral:
            return StyleType::KeywordBold;
        default:
            return StyleType::Keyword;
        }
    case TokenCategory::ControlKeyword:
        return StyleType::ControlKeyword;
    case TokenCategory::Identifier:
        return StyleType::Identifier;
    default:
        dbgln("Unknown style type for token {}", token.name());
        ASSERT_NOT_REACHED();
    }
}

String MarkupGenerator::open_style_type(StyleType type)
{
    return String::formatted("<span style=\"{}\">", style_from_style_type(type));
}

String MarkupGenerator::wrap_string_in_style(String source, StyleType type)
{
    return String::formatted("<span style=\"{}\">{}</span>", style_from_style_type(type), escape_html_entities(source));
}

}
