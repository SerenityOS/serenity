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
    virtual ~PrimitiveString();

    String const& string() const { return m_string; }

    Vector<u16> const& utf16_string() const;
    Utf16View utf16_string_view() const;

private:
    virtual const char* class_name() const override { return "PrimitiveString"; }

    String m_string;
    mutable Vector<u16> m_utf16_string;
};

PrimitiveString* js_string(Heap&, Utf16View const&);
PrimitiveString* js_string(VM&, Utf16View const&);

PrimitiveString* js_string(Heap&, String);
PrimitiveString* js_string(VM&, String);

}
