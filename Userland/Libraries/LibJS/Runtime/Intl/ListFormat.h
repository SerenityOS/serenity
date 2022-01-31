/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Object.h>
#include <LibUnicode/Locale.h>

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

    ListFormat(Object& prototype);
    virtual ~ListFormat() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    Type type() const { return m_type; }
    void set_type(StringView type);
    StringView type_string() const;

    Unicode::Style style() const { return m_style; }
    void set_style(StringView style) { m_style = Unicode::style_from_string(style); }
    StringView style_string() const { return Unicode::style_to_string(m_style); }

private:
    String m_locale;                                 // [[Locale]]
    Type m_type { Type::Invalid };                   // [[Type]]
    Unicode::Style m_style { Unicode::Style::Long }; // [[Style]]
};

using Placeables = HashMap<StringView, Variant<PatternPartition, Vector<PatternPartition>>>;

Vector<PatternPartition> deconstruct_pattern(StringView pattern, Placeables placeables);
Vector<PatternPartition> create_parts_from_list(ListFormat const& list_format, Vector<String> const& list);
String format_list(ListFormat const& list_format, Vector<String> const& list);
Array* format_list_to_parts(GlobalObject& global_object, ListFormat const& list_format, Vector<String> const& list);
ThrowCompletionOr<Vector<String>> string_list_from_iterable(GlobalObject& global_object, Value iterable);

}
