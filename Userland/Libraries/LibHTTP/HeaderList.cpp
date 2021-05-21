/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/GenericLexer.h>
#include <LibHTTP/HeaderList.h>

namespace HTTP {

// https://fetch.spec.whatwg.org/#header-list-contains
bool HeaderList::contains(const String& name) const
{
    for (auto& header : m_list) {
        if (header.name.equals_ignoring_case(name))
            return true;
    }
    return false;
}

Optional<Header> HeaderList::first_header_with_name(String const& name) const
{
    for (auto& header : m_list) {
        if (header.name.equals_ignoring_case(name))
            return header;
    }
    return {};
}

// https://fetch.spec.whatwg.org/#concept-header-list-get
String HeaderList::get(String const& name) const
{
    if (!contains(name))
        return {};

    StringBuilder builder;
    bool first_name_match = true;

    for (auto& header : m_list) {
        if (!header.name.equals_ignoring_case(name))
            continue;

        if (!first_name_match)
            builder.append(", ");

        first_name_match = false;
        builder.append(header.value);
    }

    return builder.to_string();
}

// https://fetch.spec.whatwg.org/#concept-header-list-append
void HeaderList::append(String const& name, String const& value)
{
    String name_to_use = name;
    if (auto first_header = first_header_with_name(name); first_header.has_value()) {
        // This is to keep the same casing across all headers with the same name.
        name_to_use = first_header.value().name;
    }

    m_list.append({ name_to_use, value });
}

// https://fetch.spec.whatwg.org/#concept-header-list-delete
void HeaderList::remove(String const& name)
{
    m_list.remove_all_matching([&name](auto& header) {
        return header.name.equals_ignoring_case(name);
    });
}

// https://fetch.spec.whatwg.org/#concept-header-list-set
void HeaderList::set(String const& name, String const& value)
{
    if (auto first_header = first_header_with_name(name); first_header.has_value()) {
        auto& header = first_header.value();
        header.name = name;
        header.value = value;

        m_list.remove_all_matching([&](auto& header_in_list) {
            return &header_in_list != &header && header_in_list.name.equals_ignoring_case(name);
        });

        return;
    }

    append(name, value);
}

// https://fetch.spec.whatwg.org/#concept-header-list-combine
void HeaderList::combine(String const& name, String const& value)
{
    if (auto first_header = first_header_with_name(name); first_header.has_value()) {
        auto& header = first_header.value();
        header.value = String::formatted("{}, {}", name, value);
        return;
    }

    append(name, value);
}

// https://fetch.spec.whatwg.org/#concept-header-list-get-decode-split
Vector<String> HeaderList::get_decode_and_split(String const& name) const
{
    auto initial_value = get(name);
    if (initial_value.is_null())
        return {};

    // FIXME: Let input be the result of isomorphic decoding initialValue. Once fixed, replace "initial_value" below with "input".
    GenericLexer lexer(initial_value);
    Vector<String> values;
    StringBuilder value;

    while (!lexer.is_eof()) {
        value.append(lexer.consume_while([](char c) {
           return c != '"' && c != ',';
        }));

        if (!lexer.is_eof()) {
            if (lexer.peek() == '"') {
                value.append(Core::collect_an_http_quoted_string(name, lexer));
                if (!lexer.is_eof())
                    continue;
            } else {
                VERIFY(lexer.peek() == ',');
                lexer.ignore();
            }
        }

        // FIXME: This is the wrong trim, it should only trim tabs and spaces
        values.append(value.to_string().trim_whitespace());
    }

    return values;
}

// https://fetch.spec.whatwg.org/#concept-header-extract-mime-type
Optional<Core::MimeType> HeaderList::extract_mime_type() const
{
    String charset;
    String essence;
    Core::MimeType mime_type;

    auto values = get_decode_and_split("Content-Type");
    dbgln("empty? {}", values.is_empty());
    for (auto& header : m_list) {
        dbgln("name: {} value: {}", header.name, header.value);
    }
    for (auto& value : values) {
        dbgln("ct value: {}", value);
    }
    if (values.is_empty())
        return {};

    for (auto& value : values) {
        auto temporary_mime_type = Core::MimeType::parse_from_string(value);
        dbgln("has mime type? {}", temporary_mime_type.has_value());
        if (!temporary_mime_type.has_value() || temporary_mime_type.value().essence() == "*/*")
            continue;
        mime_type = temporary_mime_type.value();
        dbgln("essence: {} parameter length: {}", mime_type.essence(), mime_type.parameters().size());

        for (auto& parameter : mime_type.parameters()) {
            dbgln("parameter name: {} value: {}", parameter.key, parameter.value);
        }

        if (mime_type.essence() != essence) {
            charset = {};
            auto character_parameter_iterator = mime_type.parameters().find("charset");
            if (character_parameter_iterator != mime_type.parameters().end())
                charset = character_parameter_iterator->value;
            dbgln("charset? {}", charset);
            essence = mime_type.essence();
        } else {
            if (!charset.is_null()) {
                auto character_parameter_iterator = mime_type.parameters().find("charset");
                if (character_parameter_iterator == mime_type.parameters().end())
                    mime_type.set_parameter("charset", charset);
            }
        }
    }

    if (mime_type.is_null())
        return {};
    return mime_type;
}

// https://fetch.spec.whatwg.org/#determine-nosniff
bool HeaderList::determine_nosniff() const
{
    auto values = get_decode_and_split("X-Content-Type-Options");
    if (values.is_empty())
        return false;

    if (values[0].equals_ignoring_case("nosniff"))
        return true;

    return false;
}

// https://fetch.spec.whatwg.org/#extract-header-list-values
Vector<String> HeaderList::extract_header_list_values(String const& name) const
{
    if (!contains(name))
        return {};

    // FIXME
    return {};
}

}
