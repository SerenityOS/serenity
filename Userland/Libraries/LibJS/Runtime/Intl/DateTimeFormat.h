/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Object.h>
#include <LibUnicode/DateTimeFormat.h>

namespace JS::Intl {

class DateTimeFormat final
    : public Object
    , public Unicode::CalendarPattern {
    JS_OBJECT(DateTimeFormat, Object);

    using Patterns = Unicode::CalendarPattern;

public:
    enum class Style {
        Full,
        Long,
        Medium,
        Short,
    };

    static Vector<StringView> const& relevant_extension_keys(); // [[RelevantExtensionKeys]]

    DateTimeFormat(Object& prototype);
    virtual ~DateTimeFormat() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    String const& calendar() const { return m_calendar; }
    void set_calendar(String calendar) { m_calendar = move(calendar); }

    String const& numbering_system() const { return m_numbering_system; }
    void set_numbering_system(String numbering_system) { m_numbering_system = move(numbering_system); }

    bool has_hour_cycle() const { return m_hour_cycle.has_value(); }
    Unicode::HourCycle hour_cycle() const { return *m_hour_cycle; }
    StringView hour_cycle_string() const { return Unicode::hour_cycle_to_string(*m_hour_cycle); }
    void set_hour_cycle(StringView hour_cycle) { m_hour_cycle = Unicode::hour_cycle_from_string(hour_cycle); }
    void set_hour_cycle(Unicode::HourCycle hour_cycle) { m_hour_cycle = hour_cycle; }
    void clear_hour_cycle() { m_hour_cycle.clear(); }

    String const& time_zone() const { return m_time_zone; }
    void set_time_zone(String time_zone) { m_time_zone = move(time_zone); }

    bool has_date_style() const { return m_date_style.has_value(); }
    Style date_style() const { return *m_date_style; };
    StringView date_style_string() const { return style_to_string(*m_date_style); };
    void set_date_style(StringView style) { m_date_style = style_from_string(style); };

    bool has_time_style() const { return m_time_style.has_value(); }
    Style time_style() const { return *m_time_style; };
    StringView time_style_string() const { return style_to_string(*m_time_style); };
    void set_time_style(StringView style) { m_time_style = style_from_string(style); };

    String const& pattern() const { return Patterns::pattern; };
    void set_pattern(String pattern) { Patterns::pattern = move(pattern); }

    bool has_era() const { return Patterns::era.has_value(); }
    Unicode::CalendarPatternStyle era() const { return *Patterns::era; };
    StringView era_string() const { return Unicode::calendar_pattern_style_to_string(*Patterns::era); }

    bool has_year() const { return Patterns::year.has_value(); }
    Unicode::CalendarPatternStyle year() const { return *Patterns::year; };
    StringView year_string() const { return Unicode::calendar_pattern_style_to_string(*Patterns::year); }

    bool has_month() const { return Patterns::month.has_value(); }
    Unicode::CalendarPatternStyle month() const { return *Patterns::month; };
    StringView month_string() const { return Unicode::calendar_pattern_style_to_string(*Patterns::month); }

    bool has_weekday() const { return Patterns::weekday.has_value(); }
    Unicode::CalendarPatternStyle weekday() const { return *Patterns::weekday; };
    StringView weekday_string() const { return Unicode::calendar_pattern_style_to_string(*Patterns::weekday); }

    bool has_day() const { return Patterns::day.has_value(); }
    Unicode::CalendarPatternStyle day() const { return *Patterns::day; };
    StringView day_string() const { return Unicode::calendar_pattern_style_to_string(*Patterns::day); }

    bool has_day_period() const { return Patterns::day_period.has_value(); }
    Unicode::CalendarPatternStyle day_period() const { return *Patterns::day_period; };
    StringView day_period_string() const { return Unicode::calendar_pattern_style_to_string(*Patterns::day_period); }

    bool has_hour() const { return Patterns::hour.has_value(); }
    Unicode::CalendarPatternStyle hour() const { return *Patterns::hour; };
    StringView hour_string() const { return Unicode::calendar_pattern_style_to_string(*Patterns::hour); }

    bool has_minute() const { return Patterns::minute.has_value(); }
    Unicode::CalendarPatternStyle minute() const { return *Patterns::minute; };
    StringView minute_string() const { return Unicode::calendar_pattern_style_to_string(*Patterns::minute); }

    bool has_second() const { return Patterns::second.has_value(); }
    Unicode::CalendarPatternStyle second() const { return *Patterns::second; };
    StringView second_string() const { return Unicode::calendar_pattern_style_to_string(*Patterns::second); }

    bool has_fractional_second_digits() const { return Patterns::fractional_second_digits.has_value(); }
    u8 fractional_second_digits() const { return *Patterns::fractional_second_digits; };

    bool has_time_zone_name() const { return Patterns::time_zone_name.has_value(); }
    Unicode::CalendarPatternStyle time_zone_name() const { return *Patterns::time_zone_name; };
    StringView time_zone_name_string() const { return Unicode::calendar_pattern_style_to_string(*Patterns::time_zone_name); }

private:
    static Style style_from_string(StringView style);
    static StringView style_to_string(Style style);

    String m_locale;                           // [[Locale]]
    String m_calendar;                         // [[Calendar]]
    String m_numbering_system;                 // [[NumberingSystem]]
    Optional<Unicode::HourCycle> m_hour_cycle; // [[HourCycle]]
    String m_time_zone;                        // [[TimeZone]]
    Optional<Style> m_date_style;              // [[DateStyle]]
    Optional<Style> m_time_style;              // [[TimeStyle]]
};

}
