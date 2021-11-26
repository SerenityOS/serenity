/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Utf16String.h>

namespace JS {

class PrimitiveString final : public Cell {
public:
    explicit PrimitiveString(String);
    explicit PrimitiveString(Utf16String);
    virtual ~PrimitiveString();

    PrimitiveString(PrimitiveString const&) = delete;
    PrimitiveString& operator=(PrimitiveString const&) = delete;

    String const& string() const;
    bool has_utf8_string() const { return m_has_utf8_string; }

    Utf16String const& utf16_string() const;
    Utf16View utf16_string_view() const;
    bool has_utf16_string() const { return m_has_utf16_string; }

private:
    virtual const char* class_name() const override { return "PrimitiveString"; }

    mutable String m_utf8_string;
    mutable bool m_has_utf8_string { false };

    mutable Utf16String m_utf16_string;
    mutable bool m_has_utf16_string { false };
};

PrimitiveString* js_string(Heap&, Utf16View const&);
PrimitiveString* js_string(VM&, Utf16View const&);

PrimitiveString* js_string(Heap&, Utf16String);
PrimitiveString* js_string(VM&, Utf16String);

PrimitiveString* js_string(Heap&, String);
PrimitiveString* js_string(VM&, String);

}
