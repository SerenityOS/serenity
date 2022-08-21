/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Utf16String.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class PrimitiveString final : public Cell {
public:
    explicit PrimitiveString(PrimitiveString&, PrimitiveString&);
    explicit PrimitiveString(String);
    explicit PrimitiveString(Utf16String);
    virtual ~PrimitiveString();

    PrimitiveString(PrimitiveString const&) = delete;
    PrimitiveString& operator=(PrimitiveString const&) = delete;

    bool is_empty() const;

    String const& string() const;
    bool has_utf8_string() const { return m_has_utf8_string; }

    Utf16String const& utf16_string() const;
    Utf16View utf16_string_view() const;
    bool has_utf16_string() const { return m_has_utf16_string; }

    Optional<Value> get(VM&, PropertyKey const&) const;

private:
    virtual StringView class_name() const override { return "PrimitiveString"sv; }
    virtual void visit_edges(Cell::Visitor&) override;

    void resolve_rope_if_needed() const;

    mutable bool m_is_rope { false };
    mutable bool m_has_utf8_string { false };
    mutable bool m_has_utf16_string { false };

    mutable PrimitiveString* m_lhs { nullptr };
    mutable PrimitiveString* m_rhs { nullptr };

    mutable String m_utf8_string;

    mutable Utf16String m_utf16_string;
};

PrimitiveString* js_string(Heap&, Utf16View const&);
PrimitiveString* js_string(VM&, Utf16View const&);

PrimitiveString* js_string(Heap&, Utf16String);
PrimitiveString* js_string(VM&, Utf16String);

PrimitiveString* js_string(Heap&, String);
PrimitiveString* js_string(VM&, String);

PrimitiveString* js_rope_string(VM&, PrimitiveString&, PrimitiveString&);

}
