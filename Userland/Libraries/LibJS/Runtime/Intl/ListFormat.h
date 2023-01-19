/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Object.h>
#include <LibLocale/Locale.h>

namespace JS::Intl {

class ListFormat final : public Object {
    JS_OBJECT(ListFormat, Object);

public:
    enum class Type {
        Invalid,
        Conjunction,
        Disjunction,
        Unit,
    };

    virtual ~ListFormat() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    Type type() const { return m_type; }
    void set_type(StringView type);
    StringView type_string() const;

    ::Locale::Style style() const { return m_style; }
    void set_style(StringView style) { m_style = ::Locale::style_from_string(style); }
    StringView style_string() const { return ::Locale::style_to_string(m_style); }

private:
    explicit ListFormat(Object& prototype);

    String m_locale;                                   // [[Locale]]
    Type m_type { Type::Invalid };                     // [[Type]]
    ::Locale::Style m_style { ::Locale::Style::Long }; // [[Style]]
};

using Placeables = HashMap<StringView, Variant<PatternPartition, Vector<PatternPartition>>>;

ThrowCompletionOr<Vector<PatternPartition>> deconstruct_pattern(VM&, StringView pattern, Placeables);
ThrowCompletionOr<Vector<PatternPartition>> create_parts_from_list(VM&, ListFormat const&, Vector<DeprecatedString> const& list);
ThrowCompletionOr<DeprecatedString> format_list(VM&, ListFormat const&, Vector<DeprecatedString> const& list);
ThrowCompletionOr<Array*> format_list_to_parts(VM&, ListFormat const&, Vector<DeprecatedString> const& list);
ThrowCompletionOr<Vector<DeprecatedString>> string_list_from_iterable(VM&, Value iterable);

}
