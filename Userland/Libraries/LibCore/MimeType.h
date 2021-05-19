/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Optional.h>
#include <AK/HashMap.h>

namespace Core {

String collect_an_http_quoted_string(String const& original_input, GenericLexer& lexer, bool extract_value = false);

// https://mimesniff.spec.whatwg.org/#mime-type
class MimeType {
public:
    static Optional<MimeType> parse_from_string(String const&);

    MimeType() = default;

    const String& type() const { return m_type; }
    const String& subtype() const { return m_subtype; }
    const HashMap<StringView, StringView>& parameters() const { return m_parameters; }

    void set_parameter(StringView const& name, StringView const& value) { m_parameters.set(name, value); }

    // FIXME: Make this an internal slot.
    String essence() const;

    bool is_null() const { return m_type.is_null() && m_subtype.is_null() && m_parameters.is_empty(); }

    bool is_javascript_mime_type() const;

private:
    MimeType(String const& type, String const& subtype)
        : m_type(type)
        , m_subtype(subtype)
    {
    }

    String m_type;
    String m_subtype;
    HashMap<StringView, StringView> m_parameters; // FIXME: This needs to be ordered.
};

}
