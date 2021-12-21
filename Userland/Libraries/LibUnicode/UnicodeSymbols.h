/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibUnicode/Forward.h>

namespace Unicode::Detail {

struct Symbols {
    static Symbols const& ensure_loaded();

    // Loaded from UnicodeData.cpp:

    Optional<String> (*code_point_display_name)(u32) { nullptr };

    u32 (*canonical_combining_class)(u32 code_point) { nullptr };

    u32 (*simple_uppercase_mapping)(u32) { nullptr };
    u32 (*simple_lowercase_mapping)(u32) { nullptr };
    Span<SpecialCasing const* const> (*special_case_mapping)(u32 code_point) { nullptr };

    Optional<GeneralCategory> (*general_category_from_string)(StringView) { nullptr };
    bool (*code_point_has_general_category)(u32, GeneralCategory) { nullptr };

    Optional<Property> (*property_from_string)(StringView) { nullptr };
    bool (*code_point_has_property)(u32, Property) { nullptr };

    Optional<Script> (*script_from_string)(StringView) { nullptr };
    bool (*code_point_has_script)(u32, Script) { nullptr };
    bool (*code_point_has_script_extension)(u32, Script) { nullptr };

    // Loaded from UnicodeLocale.cpp:

    Optional<Locale> (*locale_from_string)(StringView) { nullptr };

    Optional<StringView> (*get_locale_language_mapping)(StringView, StringView) { nullptr };
    Optional<StringView> (*get_locale_territory_mapping)(StringView, StringView) { nullptr };
    Optional<StringView> (*get_locale_script_tag_mapping)(StringView, StringView) { nullptr };
    Optional<StringView> (*get_locale_long_currency_mapping)(StringView, StringView) { nullptr };
    Optional<StringView> (*get_locale_short_currency_mapping)(StringView, StringView) { nullptr };
    Optional<StringView> (*get_locale_narrow_currency_mapping)(StringView, StringView) { nullptr };
    Optional<StringView> (*get_locale_numeric_currency_mapping)(StringView, StringView) { nullptr };
    Optional<StringView> (*get_locale_key_mapping)(StringView, StringView) { nullptr };

    Optional<ListPatterns> (*get_locale_list_pattern_mapping)(StringView, StringView, StringView) { nullptr };

    Optional<StringView> (*resolve_language_alias)(StringView) { nullptr };
    Optional<StringView> (*resolve_territory_alias)(StringView) { nullptr };
    Optional<StringView> (*resolve_script_tag_alias)(StringView) { nullptr };
    Optional<StringView> (*resolve_variant_alias)(StringView) { nullptr };
    Optional<StringView> (*resolve_subdivision_alias)(StringView) { nullptr };

    void (*resolve_complex_language_aliases)(LanguageID&);

    Optional<LanguageID> (*add_likely_subtags)(LanguageID const&);
    Optional<String> (*resolve_most_likely_territory)(LanguageID const&);

    // Loaded from UnicodeNumberFormat.cpp:

    Optional<StringView> (*get_number_system_symbol)(StringView, StringView, NumericSymbol);
    Optional<NumberGroupings> (*get_number_system_groupings)(StringView, StringView);
    Optional<NumberFormat> (*get_standard_number_system_format)(StringView, StringView, StandardNumberFormatType);
    Vector<NumberFormat> (*get_compact_number_system_formats)(StringView, StringView, CompactNumberFormatType);
    Vector<NumberFormat> (*get_unit_formats)(StringView, StringView, Style);

    // Loaded from UnicodeDateTimeFormat.cpp:

    Vector<HourCycle> (*get_regional_hour_cycles)(StringView) { nullptr };

    Optional<CalendarFormat> (*get_calendar_date_format)(StringView, StringView) { nullptr };
    Optional<CalendarFormat> (*get_calendar_time_format)(StringView, StringView) { nullptr };
    Optional<CalendarFormat> (*get_calendar_date_time_format)(StringView, StringView) { nullptr };
    Vector<CalendarPattern> (*get_calendar_available_formats)(StringView, StringView) { nullptr };

    Optional<CalendarRangePattern> (*get_calendar_default_range_format)(StringView, StringView) { nullptr };
    Vector<CalendarRangePattern> (*get_calendar_range_formats)(StringView, StringView, StringView) { nullptr };
    Vector<CalendarRangePattern> (*get_calendar_range12_formats)(StringView, StringView, StringView) { nullptr };

    Optional<StringView> (*get_calendar_era_symbol)(StringView, StringView, CalendarPatternStyle, Era) { nullptr };
    Optional<StringView> (*get_calendar_month_symbol)(StringView, StringView, CalendarPatternStyle, Month) { nullptr };
    Optional<StringView> (*get_calendar_weekday_symbol)(StringView, StringView, CalendarPatternStyle, Weekday) { nullptr };
    Optional<StringView> (*get_calendar_day_period_symbol)(StringView, StringView, CalendarPatternStyle, DayPeriod) { nullptr };
    Optional<StringView> (*get_calendar_day_period_symbol_for_hour)(StringView, StringView, CalendarPatternStyle, u8) { nullptr };

    Optional<StringView> (*get_time_zone_name)(StringView, StringView, CalendarPatternStyle) { nullptr };

private:
    Symbols() = default;
};

}
