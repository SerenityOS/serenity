/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <AK/StringBuilder.h>
#include <AK/TypeCasts.h>
#include <LibJS/Lexer.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/DatePrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

String MarkupGenerator::html_from_source(StringView source)
{
    StringBuilder builder;
    auto lexer = Lexer(source);
    for (auto token = lexer.next(); token.type() != TokenType::Eof; token = lexer.next()) {
        builder.append(token.trivia());
        builder.append(wrap_string_in_style(token.value(), style_type_for_token(token)));
    }
    return builder.to_string();
}

String MarkupGenerator::html_from_value(Value value)
{
    StringBuilder output_html;
    value_to_html(value, output_html);
    return output_html.to_string();
}

String MarkupGenerator::html_from_error(Error const& object, bool in_promise)
{
    StringBuilder output_html;
    error_to_html(object, output_html, in_promise);
    return output_html.to_string();
}

void MarkupGenerator::value_to_html(Value value, StringBuilder& output_html, HashTable<Object*> seen_objects)
{
    if (value.is_empty()) {
        output_html.append("&lt;empty&gt;"sv);
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

    if (value.is_object()) {
        auto& object = value.as_object();
        if (is<Array>(object))
            return array_to_html(static_cast<Array const&>(object), output_html, seen_objects);
        output_html.append(wrap_string_in_style(object.class_name(), StyleType::ObjectType));
        if (object.is_function())
            return function_to_html(object, output_html, seen_objects);
        if (is<Date>(object))
            return date_to_html(object, output_html, seen_objects);
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

    output_html.append("</span>"sv);
}

void MarkupGenerator::array_to_html(Array const& array, StringBuilder& html_output, HashTable<Object*>& seen_objects)
{
    html_output.append(wrap_string_in_style("[ ", StyleType::Punctuation));
    bool first = true;
    for (auto it = array.indexed_properties().begin(false); it != array.indexed_properties().end(); ++it) {
        if (!first)
            html_output.append(wrap_string_in_style(", ", StyleType::Punctuation));
        first = false;
        // FIXME: Exception check
        value_to_html(array.get(it.index()).release_value(), html_output, seen_objects);
    }
    html_output.append(wrap_string_in_style(" ]", StyleType::Punctuation));
}

void MarkupGenerator::object_to_html(Object const& object, StringBuilder& html_output, HashTable<Object*>& seen_objects)
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
        value_to_html(object.get(entry.index()).release_value(), html_output, seen_objects);
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

void MarkupGenerator::function_to_html(Object const& function, StringBuilder& html_output, HashTable<Object*>&)
{
    html_output.appendff("[{}]", function.class_name());
}

void MarkupGenerator::date_to_html(Object const& date, StringBuilder& html_output, HashTable<Object*>&)
{
    html_output.appendff("Date {}", to_date_string(static_cast<Date const&>(date).date_value()));
}

void MarkupGenerator::trace_to_html(TracebackFrame const& traceback_frame, StringBuilder& html_output)
{
    auto function_name = escape_html_entities(traceback_frame.function_name);
    auto [line, column, _] = traceback_frame.source_range.start;
    auto get_filename_from_path = [&](StringView filename) -> StringView {
        auto last_slash_index = filename.find_last('/');
        return last_slash_index.has_value() ? filename.substring_view(*last_slash_index + 1) : filename;
    };
    auto filename = escape_html_entities(get_filename_from_path(traceback_frame.source_range.filename));
    auto trace = String::formatted("at {} ({}:{}:{})", function_name, filename, line, column);

    html_output.appendff("&nbsp;&nbsp;{}<br>", trace);
}

void MarkupGenerator::error_to_html(Error const& error, StringBuilder& html_output, bool in_promise)
{
    auto& vm = error.vm();
    auto name = error.get_without_side_effects(vm.names.name).value_or(js_undefined());
    auto message = error.get_without_side_effects(vm.names.message).value_or(js_undefined());
    auto name_string = name.to_string_without_side_effects();
    auto message_string = message.to_string_without_side_effects();
    auto uncaught_message = String::formatted("Uncaught {}[{}]: ", in_promise ? "(in promise) " : "", name_string);

    html_output.append(wrap_string_in_style(uncaught_message, StyleType::Invalid));
    html_output.appendff("{}<br>", message_string.is_empty() ? "\"\"" : escape_html_entities(message_string));

    for (size_t i = 0; i < error.traceback().size() - min(error.traceback().size(), 3); i++) {
        auto& traceback_frame = error.traceback().at(i);
        trace_to_html(traceback_frame, html_output);
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
    case StyleType::ObjectType:
        return "padding: 2px; background-color: #ddf; color: black; font-weight: bold;";
    default:
        VERIFY_NOT_REACHED();
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
        VERIFY_NOT_REACHED();
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
