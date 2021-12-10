/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/StringBuilder.h>
#include <LibUnicode/DateTimeFormat.h>
#include <LibUnicode/Locale.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeDateTimeFormat.h>
#endif

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
    default:
        VERIFY_NOT_REACHED();
    }
}

// https://unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table
Vector<Unicode::HourCycle> get_regional_hour_cycles([[maybe_unused]] StringView locale)
{
#if ENABLE_UNICODE_DATA
    if (auto hour_cycles = Detail::get_regional_hour_cycles(locale); !hour_cycles.is_empty())
        return hour_cycles;

    auto return_default_hour_cycles = []() {
        auto hour_cycles = Detail::get_regional_hour_cycles("001"sv);
        VERIFY(!hour_cycles.is_empty());
        return hour_cycles;
    };

    auto language = parse_unicode_language_id(locale);
    if (!language.has_value())
        return return_default_hour_cycles();

    if (!language->region.has_value())
        language = add_likely_subtags(*language);
    if (!language.has_value() || !language->region.has_value())
        return return_default_hour_cycles();

    if (auto hour_cycles = Detail::get_regional_hour_cycles(*language->region); !hour_cycles.is_empty())
        return hour_cycles;

    return return_default_hour_cycles();
#else
    return {};
#endif
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

Optional<CalendarFormat> get_calendar_format([[maybe_unused]] StringView locale, [[maybe_unused]] StringView calendar, [[maybe_unused]] CalendarFormatType type)
{
#if ENABLE_UNICODE_DATA
    switch (type) {
    case CalendarFormatType::Date:
        return Detail::get_calendar_date_format(locale, calendar);
    case CalendarFormatType::Time:
        return Detail::get_calendar_time_format(locale, calendar);
    case CalendarFormatType::DateTime:
        return Detail::get_calendar_date_time_format(locale, calendar);
    default:
        VERIFY_NOT_REACHED();
    }
#else
    return {};
#endif
}

Vector<CalendarPattern> get_calendar_available_formats([[maybe_unused]] StringView locale, [[maybe_unused]] StringView calendar)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_calendar_available_formats(locale, calendar);
#else
    return {};
#endif
}

Optional<Unicode::CalendarRangePattern> get_calendar_default_range_format([[maybe_unused]] StringView locale, [[maybe_unused]] StringView calendar)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_calendar_default_range_format(locale, calendar);
#else
    return {};
#endif
}

Vector<Unicode::CalendarRangePattern> get_calendar_range_formats([[maybe_unused]] StringView locale, [[maybe_unused]] StringView calendar, [[maybe_unused]] StringView skeleton)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_calendar_range_formats(locale, calendar, skeleton);
#else
    return {};
#endif
}

Vector<Unicode::CalendarRangePattern> get_calendar_range12_formats([[maybe_unused]] StringView locale, [[maybe_unused]] StringView calendar, [[maybe_unused]] StringView skeleton)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_calendar_range12_formats(locale, calendar, skeleton);
#else
    return {};
#endif
}

Optional<StringView> get_calendar_era_symbol([[maybe_unused]] StringView locale, [[maybe_unused]] StringView calendar, [[maybe_unused]] CalendarPatternStyle style, [[maybe_unused]] Unicode::Era value)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_calendar_era_symbol(locale, calendar, style, value);
#else
    return {};
#endif
}

Optional<StringView> get_calendar_month_symbol([[maybe_unused]] StringView locale, [[maybe_unused]] StringView calendar, [[maybe_unused]] CalendarPatternStyle style, [[maybe_unused]] Unicode::Month value)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_calendar_month_symbol(locale, calendar, style, value);
#else
    return {};
#endif
}

Optional<StringView> get_calendar_weekday_symbol([[maybe_unused]] StringView locale, [[maybe_unused]] StringView calendar, [[maybe_unused]] CalendarPatternStyle style, [[maybe_unused]] Unicode::Weekday value)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_calendar_weekday_symbol(locale, calendar, style, value);
#else
    return {};
#endif
}

Optional<StringView> get_calendar_day_period_symbol([[maybe_unused]] StringView locale, [[maybe_unused]] StringView calendar, [[maybe_unused]] CalendarPatternStyle style, [[maybe_unused]] Unicode::DayPeriod value)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_calendar_day_period_symbol(locale, calendar, style, value);
#else
    return {};
#endif
}

Optional<StringView> get_calendar_day_period_symbol_for_hour([[maybe_unused]] StringView locale, [[maybe_unused]] StringView calendar, [[maybe_unused]] CalendarPatternStyle style, [[maybe_unused]] u8 hour)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_calendar_day_period_symbol_for_hour(locale, calendar, style, hour);
#else
    return {};
#endif
}

Optional<StringView> get_time_zone_name([[maybe_unused]] StringView locale, [[maybe_unused]] StringView time_zone, [[maybe_unused]] CalendarPatternStyle style)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_time_zone_name(locale, time_zone, style);
#else
    return {};
#endif
}

}
