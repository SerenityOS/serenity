/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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
        return "Numeric"sv;
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

}
