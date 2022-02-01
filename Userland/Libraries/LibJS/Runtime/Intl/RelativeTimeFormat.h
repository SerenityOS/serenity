/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Object.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/RelativeTimeFormat.h>

namespace JS::Intl {

class RelativeTimeFormat final : public Object {
    JS_OBJECT(RelativeTimeFormat, Object);

public:
    enum class Numeric {
        Always,
        Auto,
    };

    static constexpr auto relevant_extension_keys()
    {
        // 17.3.3 Internal slots, https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat-internal-slots
        // The value of the [[RelevantExtensionKeys]] internal slot is « "nu" ».
        return AK::Array { "nu"sv };
    }

    RelativeTimeFormat(Object& prototype);
    virtual ~RelativeTimeFormat() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    String const& data_locale() const { return m_data_locale; }
    void set_data_locale(String data_locale) { m_data_locale = move(data_locale); }

    String const& numbering_system() const { return m_numbering_system; }
    void set_numbering_system(String numbering_system) { m_numbering_system = move(numbering_system); }

    Unicode::Style style() const { return m_style; }
    void set_style(StringView style) { m_style = Unicode::style_from_string(style); }
    StringView style_string() const { return Unicode::style_to_string(m_style); }

    Numeric numeric() const { return m_numeric; }
    void set_numeric(StringView numeric);
    StringView numeric_string() const;

    NumberFormat& number_format() const { return *m_number_format; }
    void set_number_format(NumberFormat* number_format) { m_number_format = number_format; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    String m_locale;                                 // [[Locale]]
    String m_data_locale;                            // [[DataLocale]]
    String m_numbering_system;                       // [[NumberingSystem]]
    Unicode::Style m_style { Unicode::Style::Long }; // [[Style]]
    Numeric m_numeric { Numeric::Always };           // [[Numeric]]
    NumberFormat* m_number_format { nullptr };       // [[NumberFormat]]
};

struct PatternPartitionWithUnit : public PatternPartition {
    PatternPartitionWithUnit(StringView type, String value, StringView unit_string = {})
        : PatternPartition(type, move(value))
        , unit(unit_string)
    {
    }

    StringView unit;
};

ThrowCompletionOr<RelativeTimeFormat*> initialize_relative_time_format(GlobalObject& global_object, RelativeTimeFormat& relative_time_format, Value locales_value, Value options_value);
ThrowCompletionOr<Unicode::TimeUnit> singular_relative_time_unit(GlobalObject& global_object, StringView unit);
ThrowCompletionOr<Vector<PatternPartitionWithUnit>> partition_relative_time_pattern(GlobalObject& global_object, RelativeTimeFormat& relative_time_format, double value, StringView unit);
Vector<PatternPartitionWithUnit> make_parts_list(StringView pattern, StringView unit, Vector<PatternPartition> parts);
ThrowCompletionOr<String> format_relative_time(GlobalObject& global_object, RelativeTimeFormat& relative_time_format, double value, StringView unit);
ThrowCompletionOr<Array*> format_relative_time_to_parts(GlobalObject& global_object, RelativeTimeFormat& relative_time_format, double value, StringView unit);

}
