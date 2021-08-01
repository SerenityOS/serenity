/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

class PrimitiveString final : public Cell {
public:
    explicit PrimitiveString(String);
    explicit PrimitiveString(Vector<u16>);
    virtual ~PrimitiveString();

    PrimitiveString(PrimitiveString const&) = delete;
    PrimitiveString& operator=(PrimitiveString const&) = delete;

    String const& string() const;

    Vector<u16> const& utf16_string() const;
    Utf16View utf16_string_view() const;

private:
    virtual const char* class_name() const override { return "PrimitiveString"; }

    mutable String m_utf8_string;
    mutable bool m_has_utf8_string { false };

    mutable Vector<u16> m_utf16_string;
    mutable bool m_has_utf16_string { false };
};

PrimitiveString* js_string(Heap&, Utf16View const&);
PrimitiveString* js_string(VM&, Utf16View const&);

PrimitiveString* js_string(Heap&, Vector<u16>);
PrimitiveString* js_string(VM&, Vector<u16>);

PrimitiveString* js_string(Heap&, String);
PrimitiveString* js_string(VM&, String);

}
