/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
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

    static constexpr auto relevant_extension_keys()
    {
        // 11.3.3 Internal slots, https://tc39.es/ecma402/#sec-intl.datetimeformat-internal-slots
        // The value of the [[RelevantExtensionKeys]] internal slot is « "ca", "hc", "nu" ».
        return AK::Array { "ca"sv, "hc"sv, "nu"sv };
    }

    DateTimeFormat(Object& prototype);
    virtual ~DateTimeFormat() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    String const& data_locale() const { return m_data_locale; }
    void set_data_locale(String data_locale) { m_data_locale = move(data_locale); }

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

    Span<Unicode::CalendarRangePattern const> range_patterns() const { return m_range_patterns.span(); };
    void set_range_patterns(Vector<Unicode::CalendarRangePattern> range_patterns) { m_range_patterns = move(range_patterns); }

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

    NativeFunction* bound_format() const { return m_bound_format; }
    void set_bound_format(NativeFunction* bound_format) { m_bound_format = bound_format; }

private:
    static Style style_from_string(StringView style);
    static StringView style_to_string(Style style);

    virtual void visit_edges(Visitor&) override;

    String m_locale;                                        // [[Locale]]
    String m_calendar;                                      // [[Calendar]]
    String m_numbering_system;                              // [[NumberingSystem]]
    Optional<Unicode::HourCycle> m_hour_cycle;              // [[HourCycle]]
    String m_time_zone;                                     // [[TimeZone]]
    Optional<Style> m_date_style;                           // [[DateStyle]]
    Optional<Style> m_time_style;                           // [[TimeStyle]]
    Vector<Unicode::CalendarRangePattern> m_range_patterns; // [[RangePatterns]]
    NativeFunction* m_bound_format { nullptr };             // [[BoundFormat]]

    String m_data_locale;
};

enum class OptionRequired {
    Any,
    Date,
    Time,
};

enum class OptionDefaults {
    All,
    Date,
    Time,
};

// Table 5: Record returned by ToLocalTime, https://tc39.es/ecma402/#table-datetimeformat-tolocaltime-record
struct LocalTime {
    int weekday { 0 };     // [[Weekday]]
    Unicode::Era era {};   // [[Era]]
    i32 year { 0 };        // [[Year]]
    Value related_year {}; // [[RelatedYear]]
    Value year_name {};    // [[YearName]]
    u8 month { 0 };        // [[Month]]
    u8 day { 0 };          // [[Day]]
    u8 hour { 0 };         // [[Hour]]
    u8 minute { 0 };       // [[Minute]]
    u8 second { 0 };       // [[Second]]
    u16 millisecond { 0 }; // [[Millisecond]]
    bool in_dst { false }; // [[InDST]]
};

struct PatternPartitionWithSource : public PatternPartition {
    static Vector<PatternPartitionWithSource> create_from_parent_list(Vector<PatternPartition> partitions)
    {
        Vector<PatternPartitionWithSource> result;
        result.ensure_capacity(partitions.size());

        for (auto& partition : partitions) {
            PatternPartitionWithSource partition_with_source {};
            partition_with_source.type = partition.type;
            partition_with_source.value = move(partition.value);
            result.append(move(partition_with_source));
        }

        return result;
    }

    StringView source;
};

ThrowCompletionOr<DateTimeFormat*> initialize_date_time_format(GlobalObject& global_object, DateTimeFormat& date_time_format, Value locales_value, Value options_value);
ThrowCompletionOr<Object*> to_date_time_options(GlobalObject& global_object, Value options_value, OptionRequired, OptionDefaults);
Optional<Unicode::CalendarPattern> date_time_style_format(StringView data_locale, DateTimeFormat& date_time_format);
Optional<Unicode::CalendarPattern> basic_format_matcher(Unicode::CalendarPattern const& options, Vector<Unicode::CalendarPattern> formats);
Optional<Unicode::CalendarPattern> best_fit_format_matcher(Unicode::CalendarPattern const& options, Vector<Unicode::CalendarPattern> formats);
ThrowCompletionOr<Vector<PatternPartition>> format_date_time_pattern(GlobalObject& global_object, DateTimeFormat& date_time_format, Vector<PatternPartition> pattern_parts, Value time, Unicode::CalendarPattern const* range_format_options);
ThrowCompletionOr<Vector<PatternPartition>> partition_date_time_pattern(GlobalObject& global_object, DateTimeFormat& date_time_format, Value time);
ThrowCompletionOr<String> format_date_time(GlobalObject& global_object, DateTimeFormat& date_time_format, Value time);
ThrowCompletionOr<Array*> format_date_time_to_parts(GlobalObject& global_object, DateTimeFormat& date_time_format, Value time);
ThrowCompletionOr<Vector<PatternPartitionWithSource>> partition_date_time_range_pattern(GlobalObject& global_object, DateTimeFormat& date_time_format, Value start, Value end);
ThrowCompletionOr<String> format_date_time_range(GlobalObject& global_object, DateTimeFormat& date_time_format, Value start, Value end);
ThrowCompletionOr<Array*> format_date_time_range_to_parts(GlobalObject& global_object, DateTimeFormat& date_time_format, Value start, Value end);
ThrowCompletionOr<LocalTime> to_local_time(GlobalObject& global_object, double time, StringView calendar, StringView time_zone);

template<typename Callback>
ThrowCompletionOr<void> for_each_calendar_field(GlobalObject& global_object, Unicode::CalendarPattern& pattern, Callback&& callback)
{
    auto& vm = global_object.vm();

    constexpr auto narrow_short_long = AK::Array { "narrow"sv, "short"sv, "long"sv };
    constexpr auto two_digit_numeric = AK::Array { "2-digit"sv, "numeric"sv };
    constexpr auto two_digit_numeric_narrow_short_long = AK::Array { "2-digit"sv, "numeric"sv, "narrow"sv, "short"sv, "long"sv };
    constexpr auto short_long = AK::Array { "short"sv, "long"sv };

    // Table 4: Components of date and time formats, https://tc39.es/ecma402/#table-datetimeformat-components
    TRY(callback(pattern.weekday, vm.names.weekday, narrow_short_long));
    TRY(callback(pattern.era, vm.names.era, narrow_short_long));
    TRY(callback(pattern.year, vm.names.year, two_digit_numeric));
    TRY(callback(pattern.month, vm.names.month, two_digit_numeric_narrow_short_long));
    TRY(callback(pattern.day, vm.names.day, two_digit_numeric));
    TRY(callback(pattern.day_period, vm.names.dayPeriod, narrow_short_long));
    TRY(callback(pattern.hour, vm.names.hour, two_digit_numeric));
    TRY(callback(pattern.minute, vm.names.minute, two_digit_numeric));
    TRY(callback(pattern.second, vm.names.second, two_digit_numeric));
    TRY(callback(pattern.fractional_second_digits, vm.names.fractionalSecondDigits, Empty {}));
    TRY(callback(pattern.time_zone_name, vm.names.timeZoneName, short_long));

    return {};
}

}
