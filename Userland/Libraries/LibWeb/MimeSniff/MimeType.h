/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>

namespace Web::MimeSniff {

bool is_javascript_mime_type_essence_match(DeprecatedString const&);

// https://mimesniff.spec.whatwg.org/#mime-type
class MimeType {
public:
    static Optional<MimeType> parse(StringView);

    MimeType(DeprecatedString type, DeprecatedString subtype);
    ~MimeType();

    DeprecatedString const& type() const { return m_type; }
    DeprecatedString const& subtype() const { return m_subtype; }
    OrderedHashMap<DeprecatedString, DeprecatedString> const& parameters() const { return m_parameters; }

    bool is_javascript() const;

    void set_parameter(DeprecatedString const& name, DeprecatedString const& value);

    DeprecatedString essence() const;
    DeprecatedString serialized() const;

private:
    // https://mimesniff.spec.whatwg.org/#type
    // A MIME type’s type is a non-empty ASCII string.
    DeprecatedString m_type;

    // https://mimesniff.spec.whatwg.org/#subtype
    // A MIME type’s subtype is a non-empty ASCII string.
    DeprecatedString m_subtype;

    // https://mimesniff.spec.whatwg.org/#parameters
    // A MIME type’s parameters is an ordered map whose keys are ASCII strings and values are strings limited to HTTP quoted-string token code points. It is initially empty.
    OrderedHashMap<DeprecatedString, DeprecatedString> m_parameters;
};

}
