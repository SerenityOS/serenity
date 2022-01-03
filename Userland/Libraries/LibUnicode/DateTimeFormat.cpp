/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/StringBuilder.h>
#include <LibUnicode/DateTimeFormat.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/UnicodeSymbols.h>

namespace Unicode {

HourCycle hour_cycle_from_string(StringView hour_cycle)
{
    if (hour_cycle == "h11"sv)
        return Unicode::HourCycle::H11;
    else if (hour_cycle == "h12"sv)
        return Unicode::HourCycle::H12;
    else if (hour_cycle == "h23"sv)
        return Unicode::HourCycle::H23;
    else if (hour_cycle == "h24"sv)
        return Unicode::HourCycle::H24;
    VERIFY_NOT_REACHED();
}

StringView hour_cycle_to_string(HourCycle hour_cycle)
{
    switch (hour_cycle) {
    case HourCycle::H11:
        return "h11"sv;
    case HourCycle::H12:
        return "h12"sv;
    case HourCycle::H23:
        return "h23"sv;
    case HourCycle::H24:
        return "h24"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

CalendarPatternStyle calendar_pattern_style_from_string(StringView style)
{
    if (style == "narrow"sv)
        return CalendarPatternStyle::Narrow;
    if (style == "short"sv)
        return CalendarPatternStyle::Short;
    if (style == "long"sv)
        return CalendarPatternStyle::Long;
    if (style == "numeric"sv)
        return CalendarPatternStyle::Numeric;
    if (style == "2-digit"sv)
        return CalendarPatternStyle::TwoDigit;
    if (style == "shortOffset"sv)
        return CalendarPatternStyle::ShortOffset;
    if (style == "longOffset"sv)
        return CalendarPatternStyle::LongOffset;
    if (style == "shortGeneric"sv)
        return CalendarPatternStyle::ShortGeneric;
    if (style == "longGeneric"sv)
        return CalendarPatternStyle::LongGeneric;
    VERIFY_NOT_REACHED();
}

StringView calendar_pattern_style_to_string(CalendarPatternStyle style)
{
    switch (style) {
    case CalendarPatternStyle::Narrow:
        return "narrow"sv;
    case CalendarPatternStyle::Short:
        return "short"sv;
    case CalendarPatternStyle::Long:
        return "long"sv;
    case CalendarPatternStyle::Numeric:
        return "numeric"sv;
    case CalendarPatternStyle::TwoDigit:
        return "2-digit"sv;
    case CalendarPatternStyle::ShortOffset:
        return "shortOffset"sv;
    case CalendarPatternStyle::LongOffset:
        return "longOffset"sv;
    case CalendarPatternStyle::ShortGeneric:
        return "shortGeneric"sv;
    case CalendarPatternStyle::LongGeneric:
        return "longGeneric"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

// https://unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table
Vector<Unicode::HourCycle> get_regional_hour_cycles(StringView locale)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();

    if (auto hour_cycles = symbols.get_regional_hour_cycles(locale); !hour_cycles.is_empty())
        return hour_cycles;

    auto return_default_hour_cycles = [&]() { return symbols.get_regional_hour_cycles("001"sv); };

    auto language = parse_unicode_language_id(locale);
    if (!language.has_value())
        return return_default_hour_cycles();

    if (!language->region.has_value())
        language = add_likely_subtags(*language);
    if (!language.has_value() || !language->region.has_value())
        return return_default_hour_cycles();

    if (auto hour_cycles = symbols.get_regional_hour_cycles(*language->region); !hour_cycles.is_empty())
        return hour_cycles;

    return return_default_hour_cycles();
}

Optional<Unicode::HourCycle> get_default_regional_hour_cycle(StringView locale)
{
    if (auto hour_cycles = get_regional_hour_cycles(locale); !hour_cycles.is_empty())
        return hour_cycles.first();
    return {};
}

String combine_skeletons(StringView first, StringView second)
{
    // https://unicode.org/reports/tr35/tr35-dates.html#availableFormats_appendItems
    constexpr auto field_order = Array {
        "G"sv,       // Era
        "yYuUr"sv,   // Year
        "ML"sv,      // Month
        "dDFg"sv,    // Day
        "Eec"sv,     // Weekday
        "abB"sv,     // Period
        "hHKk"sv,    // Hour
        "m"sv,       // Minute
        "sSA"sv,     // Second
        "zZOvVXx"sv, // Zone
    };

    StringBuilder builder;

    auto append_from_skeleton = [&](auto skeleton, auto ch) {
        auto first_index = skeleton.find(ch);
        if (!first_index.has_value())
            return false;

        auto last_index = skeleton.find_last(ch);

        builder.append(skeleton.substring_view(*first_index, *last_index - *first_index + 1));
        return true;
    };

    for (auto fields : field_order) {
        for (auto ch : fields) {
            if (append_from_skeleton(first, ch))
                break;
            if (append_from_skeleton(second, ch))
                break;
        }
    }

    return builder.build();
}

Optional<CalendarFormat> get_calendar_format(StringView locale, StringView calendar, CalendarFormatType type)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();

    switch (type) {
    case CalendarFormatType::Date:
        return symbols.get_calendar_date_format(locale, calendar);
    case CalendarFormatType::Time:
        return symbols.get_calendar_time_format(locale, calendar);
    case CalendarFormatType::DateTime:
        return symbols.get_calendar_date_time_format(locale, calendar);
    default:
        VERIFY_NOT_REACHED();
    }
}

Vector<CalendarPattern> get_calendar_available_formats(StringView locale, StringView calendar)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();
    return symbols.get_calendar_available_formats(locale, calendar);
}

Optional<Unicode::CalendarRangePattern> get_calendar_default_range_format(StringView locale, StringView calendar)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();
    return symbols.get_calendar_default_range_format(locale, calendar);
}

Vector<Unicode::CalendarRangePattern> get_calendar_range_formats(StringView locale, StringView calendar, StringView skeleton)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();
    return symbols.get_calendar_range_formats(locale, calendar, skeleton);
}

Vector<Unicode::CalendarRangePattern> get_calendar_range12_formats(StringView locale, StringView calendar, StringView skeleton)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();
    return symbols.get_calendar_range12_formats(locale, calendar, skeleton);
}

Optional<StringView> get_calendar_era_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Unicode::Era value)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();
    return symbols.get_calendar_era_symbol(locale, calendar, style, value);
}

Optional<StringView> get_calendar_month_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Unicode::Month value)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();
    return symbols.get_calendar_month_symbol(locale, calendar, style, value);
}

Optional<StringView> get_calendar_weekday_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Unicode::Weekday value)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();
    return symbols.get_calendar_weekday_symbol(locale, calendar, style, value);
}

Optional<StringView> get_calendar_day_period_symbol(StringView locale, StringView calendar, CalendarPatternStyle style, Unicode::DayPeriod value)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();
    return symbols.get_calendar_day_period_symbol(locale, calendar, style, value);
}

Optional<StringView> get_calendar_day_period_symbol_for_hour(StringView locale, StringView calendar, CalendarPatternStyle style, u8 hour)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();
    return symbols.get_calendar_day_period_symbol_for_hour(locale, calendar, style, hour);
}

Optional<StringView> get_time_zone_name(StringView locale, StringView time_zone, CalendarPatternStyle style)
{
    static auto const& symbols = Detail::Symbols::ensure_loaded();
    return symbols.get_time_zone_name(locale, time_zone, style);
}

}
