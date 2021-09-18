/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
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

    enum class Style {
        Invalid,
        Narrow,
        Short,
        Long,
    };

    ListFormat(Object& prototype);
    virtual ~ListFormat() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    Type type() const { return m_type; }
    void set_type(StringView type);
    StringView type_string() const;

    Style style() const { return m_style; }
    void set_style(StringView style);
    StringView style_string() const;

private:
    String m_locale;                  // [[Locale]]
    Type m_type { Type::Invalid };    // [[Type]]
    Style m_style { Style::Invalid }; // [[Style]]
};

using Placeables = HashMap<StringView, Variant<PatternPartition, Vector<PatternPartition>>>;

Vector<PatternPartition> deconstruct_pattern(StringView pattern, Placeables placeables);
Vector<PatternPartition> create_parts_from_list(ListFormat const& list_format, Vector<String> const& list);
String format_list(ListFormat const& list_format, Vector<String> const& list);
Array* format_list_to_parts(GlobalObject& global_object, ListFormat const& list_format, Vector<String> const& list);
ThrowCompletionOr<Vector<String>> string_list_from_iterable(GlobalObject& global_object, Value iterable);

}
