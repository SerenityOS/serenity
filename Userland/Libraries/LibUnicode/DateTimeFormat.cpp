/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibUnicode/DateTimeFormat.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeDateTimeFormat.h>
#endif

namespace Unicode {

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
