/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>

namespace Web::MimeSniff {

// https://mimesniff.spec.whatwg.org/#mime-type
class MimeType {
public:
    static Optional<MimeType> from_string(StringView);

    MimeType(String type, String subtype);
    ~MimeType();

    String const& type() const { return m_type; }
    String const& subtype() const { return m_subtype; }
    OrderedHashMap<String, String> const& parameters() const { return m_parameters; }

    void set_parameter(String const& name, String const& value);

    String essence() const;

private:
    // https://mimesniff.spec.whatwg.org/#type
    // A MIME type’s type is a non-empty ASCII string.
    String m_type;

    // https://mimesniff.spec.whatwg.org/#subtype
    // A MIME type’s subtype is a non-empty ASCII string.
    String m_subtype;

    // https://mimesniff.spec.whatwg.org/#parameters
    // A MIME type’s parameters is an ordered map whose keys are ASCII strings and values are strings limited to HTTP quoted-string token code points. It is initially empty.
    OrderedHashMap<String, String> m_parameters;
};

}
