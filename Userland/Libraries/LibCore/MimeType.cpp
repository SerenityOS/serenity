/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <LibCore/MimeType.h>
#include <ctype.h>

namespace Core {

// Code points from: https://mimesniff.spec.whatwg.org/#http-token-code-point
static bool consists_of_only_http_token_code_points(StringView const& string)
{
    for (char c : string) {
        if (!isalnum(c) && c != '!' && c != '#' && c != '$' && c != '%' && c != '&' && c != '\'' && c != '*' && c != '+' && c != '-' && c != '.' && c != '^' && c != '_' && c != '`' && c != '|' && c != '~')
            return false;
    }

    return true;
}

// Code points from: https://mimesniff.spec.whatwg.org/#http-quoted-string-token-code-point
static bool consists_of_only_http_quoted_string_token_code_points(StringView const& string)
{
    for (char c : string) {
        // FIXME: Maybe don't cast to u16? Utf8View?
        if (c != '\t' && !(c >= ' ' && c <= '~') && !((u8)c >= (u8)0x80 && (u16)c <= (u16)0xFF))
            return false;
    }

    return true;
}

// https://fetch.spec.whatwg.org/#http-whitespace
static bool is_http_whitespace(char c)
{
    return c == '\t' || c == ' ' || c == '\n' || c == '\r';
}

// https://fetch.spec.whatwg.org/#collect-an-http-quoted-string
String collect_an_http_quoted_string(String const& original_input, GenericLexer& lexer, bool extract_value)
{
    size_t start_position = lexer.tell();
    StringBuilder quoted_value;

    VERIFY(lexer.peek() == '"');
    lexer.ignore();

    for (;;) {
        quoted_value.append(lexer.consume_while([](char c) {
            return c != '"' && c != '\\';
        }));

        if (lexer.is_eof())
            break;

        auto quote_or_backslash = lexer.peek();
        lexer.ignore();

        if (quote_or_backslash == '\\') {
            if (lexer.is_eof()) {
                quoted_value.append('\\');
                break;
            }

            quoted_value.append(lexer.consume());
        } else {
            VERIFY(quote_or_backslash == '"');
            break;
        }
    }

    if (extract_value)
        return quoted_value.to_string();
    // FIXME: Is this inclusive?
    return original_input.substring(start_position, lexer.tell());
}

// https://mimesniff.spec.whatwg.org/#parse-a-mime-type
Optional<MimeType> MimeType::parse_from_string(String const& input)
{
    // FIXME: This is not the right trim. trim_whitespace uses isspace, which includes 0x0b VERTICAL TAB and 0x0C FEED, which is not excluded by HTTP whitespace.
    auto trimmed_input = input.trim_whitespace();
    GenericLexer lexer(trimmed_input);

    auto type = lexer.consume_while([](char c) {
        return c != '/';
    });

    // NOTE: This is out of order of the spec, but saves us from doing an otherwise useless loop below.
    if (lexer.is_eof())
        return {};

    if (!consists_of_only_http_token_code_points(type))
        return {};

    // This skips the '/'.
    lexer.ignore();

    auto subtype = lexer.consume_while([](char c) {
        return c != ';';
    });

    // FIXME: This is not the right trim. trim_whitespace uses isspace, which includes 0x0b VERTICAL TAB and 0x0C FEED, which is not excluded by HTTP whitespace.
    subtype = subtype.trim_whitespace(TrimMode::Right);

    if (subtype.is_empty() || !consists_of_only_http_token_code_points(subtype))
        return {};

    MimeType mime_type { type.to_string().to_lowercase(), subtype.to_string().to_lowercase() };

    while (!lexer.is_eof()) {
        // This skips a ';'.
        lexer.ignore();
        lexer.ignore_while([](char c) {
            return is_http_whitespace(c);
        });

        String parameter_name = lexer.consume_while([](char c) {
            return c != ';' && c != '=';
        });

        parameter_name = parameter_name.to_lowercase();

        if (!lexer.is_eof()) {
            if (lexer.peek() == ';')
                continue;

            // This skips a '=';
            lexer.ignore();
        }

        if (lexer.is_eof())
            break;

        String parameter_value;

        if (lexer.peek() == '"') {
            parameter_value = collect_an_http_quoted_string(input, lexer, true);
            lexer.ignore_while([](char c) {
                return c != ';';
            });
        } else {
            parameter_value = lexer.consume_while([](char c) {
                return c != ';';
            });

            // FIXME: This is not the right trim. trim_whitespace uses isspace, which includes 0x0b VERTICAL TAB and 0x0C FEED, which is not excluded by HTTP whitespace.
            parameter_value = parameter_value.trim_whitespace(TrimMode::Right);

            if (parameter_value.is_empty())
                continue;
        }

        dbgln("parser param name: {} value: {}", parameter_name, parameter_value);

        if (!parameter_name.is_empty() && consists_of_only_http_token_code_points(parameter_name) && consists_of_only_http_quoted_string_token_code_points(parameter_value) && !mime_type.m_parameters.contains(parameter_name))
            mime_type.m_parameters.set(parameter_name, parameter_value);

        for (auto& parameter : mime_type.m_parameters) {
            dbgln("parameter name: {} value: {}", parameter.key, parameter.value);
        }
    }

    return mime_type;
}

// https://mimesniff.spec.whatwg.org/#mime-type-essence
String MimeType::essence() const
{
    StringBuilder builder;
    builder.append(m_type);
    builder.append('/');
    builder.append(m_subtype);
    return builder.to_string();
}

// https://mimesniff.spec.whatwg.org/#javascript-mime-type
bool MimeType::is_javascript_mime_type() const
{
    return essence().is_one_of("application/ecmascript", "application/javascript", "application/x-ecmascript", "application/x-javascript", "text/ecmascript", "text/javascript", "text/javascript1.0", "text/javascript1.1", "text/javascript1.2", "text/javascript1.3", "text/javascript1.4", "text/javascript1.5", "text/jscript", "text/livescript", "text/x-ecmascript", "text/x-javascript");
}

}
