/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Object.h>
#include <LibLocale/DateTimeFormat.h>

namespace JS::Intl {

class DateTimeFormat final
    : public Object
    , public ::Locale::CalendarPattern {
    JS_OBJECT(DateTimeFormat, Object);
    JS_DECLARE_ALLOCATOR(DateTimeFormat);

    using Patterns = ::Locale::CalendarPattern;

public:
    enum class Style {
        Full,
        Long,
        Medium,
        Short,
    };

    static constexpr auto relevant_extension_keys()
    {
        // 11.2.3 Internal slots, https://tc39.es/ecma402/#sec-intl.datetimeformat-internal-slots
        // The value of the [[RelevantExtensionKeys]] internal slot is « "ca", "hc", "nu" ».
        return AK::Array { "ca"sv, "hc"sv, "nu"sv };
    }

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
    ::Locale::HourCycle hour_cycle() const { return *m_hour_cycle; }
    StringView hour_cycle_string() const { return ::Locale::hour_cycle_to_string(*m_hour_cycle); }
    void set_hour_cycle(::Locale::HourCycle hour_cycle) { m_hour_cycle = hour_cycle; }
    void clear_hour_cycle() { m_hour_cycle.clear(); }

    String const& time_zone() const { return m_time_zone; }
    void set_time_zone(String time_zone) { m_time_zone = move(time_zone); }

    bool has_date_style() const { return m_date_style.has_value(); }
    Style date_style() const { return *m_date_style; }
    StringView date_style_string() const { return style_to_string(*m_date_style); }
    void set_date_style(StringView style) { m_date_style = style_from_string(style); }

    bool has_time_style() const { return m_time_style.has_value(); }
    Style time_style() const { return *m_time_style; }
    StringView time_style_string() const { return style_to_string(*m_time_style); }
    void set_time_style(StringView style) { m_time_style = style_from_string(style); }

    String const& pattern() const { return Patterns::pattern; }
    void set_pattern(String pattern) { Patterns::pattern = move(pattern); }

    ReadonlySpan<::Locale::CalendarRangePattern> range_patterns() const { return m_range_patterns.span(); }
    void set_range_patterns(Vector<::Locale::CalendarRangePattern> range_patterns) { m_range_patterns = move(range_patterns); }

    bool has_era() const { return Patterns::era.has_value(); }
    ::Locale::CalendarPatternStyle era() const { return *Patterns::era; }
    StringView era_string() const { return ::Locale::calendar_pattern_style_to_string(*Patterns::era); }

    bool has_year() const { return Patterns::year.has_value(); }
    ::Locale::CalendarPatternStyle year() const { return *Patterns::year; }
    StringView year_string() const { return ::Locale::calendar_pattern_style_to_string(*Patterns::year); }

    bool has_month() const { return Patterns::month.has_value(); }
    ::Locale::CalendarPatternStyle month() const { return *Patterns::month; }
    StringView month_string() const { return ::Locale::calendar_pattern_style_to_string(*Patterns::month); }

    bool has_weekday() const { return Patterns::weekday.has_value(); }
    ::Locale::CalendarPatternStyle weekday() const { return *Patterns::weekday; }
    StringView weekday_string() const { return ::Locale::calendar_pattern_style_to_string(*Patterns::weekday); }

    bool has_day() const { return Patterns::day.has_value(); }
    ::Locale::CalendarPatternStyle day() const { return *Patterns::day; }
    StringView day_string() const { return ::Locale::calendar_pattern_style_to_string(*Patterns::day); }

    bool has_day_period() const { return Patterns::day_period.has_value(); }
    ::Locale::CalendarPatternStyle day_period() const { return *Patterns::day_period; }
    StringView day_period_string() const { return ::Locale::calendar_pattern_style_to_string(*Patterns::day_period); }

    bool has_hour() const { return Patterns::hour.has_value(); }
    ::Locale::CalendarPatternStyle hour() const { return *Patterns::hour; }
    StringView hour_string() const { return ::Locale::calendar_pattern_style_to_string(*Patterns::hour); }

    bool has_minute() const { return Patterns::minute.has_value(); }
    ::Locale::CalendarPatternStyle minute() const { return *Patterns::minute; }
    StringView minute_string() const { return ::Locale::calendar_pattern_style_to_string(*Patterns::minute); }

    bool has_second() const { return Patterns::second.has_value(); }
    ::Locale::CalendarPatternStyle second() const { return *Patterns::second; }
    StringView second_string() const { return ::Locale::calendar_pattern_style_to_string(*Patterns::second); }

    bool has_fractional_second_digits() const { return Patterns::fractional_second_digits.has_value(); }
    u8 fractional_second_digits() const { return *Patterns::fractional_second_digits; }

    bool has_time_zone_name() const { return Patterns::time_zone_name.has_value(); }
    ::Locale::CalendarPatternStyle time_zone_name() const { return *Patterns::time_zone_name; }
    StringView time_zone_name_string() const { return ::Locale::calendar_pattern_style_to_string(*Patterns::time_zone_name); }

    NativeFunction* bound_format() const { return m_bound_format; }
    void set_bound_format(NativeFunction* bound_format) { m_bound_format = bound_format; }

private:
    DateTimeFormat(Object& prototype);

    static Style style_from_string(StringView style);
    static StringView style_to_string(Style style);

    virtual void visit_edges(Visitor&) override;

    String m_locale;                                         // [[Locale]]
    String m_calendar;                                       // [[Calendar]]
    String m_numbering_system;                               // [[NumberingSystem]]
    Optional<::Locale::HourCycle> m_hour_cycle;              // [[HourCycle]]
    String m_time_zone;                                      // [[TimeZone]]
    Optional<Style> m_date_style;                            // [[DateStyle]]
    Optional<Style> m_time_style;                            // [[TimeStyle]]
    Vector<::Locale::CalendarRangePattern> m_range_patterns; // [[RangePatterns]]
    GCPtr<NativeFunction> m_bound_format;                    // [[BoundFormat]]

    String m_data_locale;
};

// Table 8: Record returned by ToLocalTime, https://tc39.es/ecma402/#table-datetimeformat-tolocaltime-record
// Note: [[InDST]] is not included here - it is handled by LibUnicode / LibTimeZone.
struct LocalTime {
    AK::UnixDateTime time_since_epoch() const
    {
        return AK::UnixDateTime::from_unix_time_parts(year, month + 1, day + 1, hour, minute, second, millisecond);
    }

    int weekday { 0 };     // [[Weekday]]
    ::Locale::Era era {};  // [[Era]]
    i32 year { 0 };        // [[Year]]
    Value related_year {}; // [[RelatedYear]]
    Value year_name {};    // [[YearName]]
    u8 month { 0 };        // [[Month]]
    u8 day { 0 };          // [[Day]]
    u8 hour { 0 };         // [[Hour]]
    u8 minute { 0 };       // [[Minute]]
    u8 second { 0 };       // [[Second]]
    u16 millisecond { 0 }; // [[Millisecond]]
};

Optional<::Locale::CalendarPattern> date_time_style_format(StringView data_locale, DateTimeFormat& date_time_format);
Optional<::Locale::CalendarPattern> basic_format_matcher(::Locale::CalendarPattern const& options, Vector<::Locale::CalendarPattern> formats);
Optional<::Locale::CalendarPattern> best_fit_format_matcher(::Locale::CalendarPattern const& options, Vector<::Locale::CalendarPattern> formats);
ThrowCompletionOr<Vector<PatternPartition>> format_date_time_pattern(VM&, DateTimeFormat&, Vector<PatternPartition> pattern_parts, double time, ::Locale::CalendarPattern const* range_format_options);
ThrowCompletionOr<Vector<PatternPartition>> partition_date_time_pattern(VM&, DateTimeFormat&, double time);
ThrowCompletionOr<String> format_date_time(VM&, DateTimeFormat&, double time);
ThrowCompletionOr<NonnullGCPtr<Array>> format_date_time_to_parts(VM&, DateTimeFormat&, double time);
ThrowCompletionOr<Vector<PatternPartitionWithSource>> partition_date_time_range_pattern(VM&, DateTimeFormat&, double start, double end);
ThrowCompletionOr<String> format_date_time_range(VM&, DateTimeFormat&, double start, double end);
ThrowCompletionOr<NonnullGCPtr<Array>> format_date_time_range_to_parts(VM&, DateTimeFormat&, double start, double end);
ThrowCompletionOr<LocalTime> to_local_time(VM&, Crypto::SignedBigInteger const& epoch_ns, StringView calendar, StringView time_zone);

template<typename Callback>
ThrowCompletionOr<void> for_each_calendar_field(VM& vm, ::Locale::CalendarPattern& pattern, Callback&& callback)
{
    constexpr auto narrow_short_long = AK::Array { "narrow"sv, "short"sv, "long"sv };
    constexpr auto two_digit_numeric = AK::Array { "2-digit"sv, "numeric"sv };
    constexpr auto two_digit_numeric_narrow_short_long = AK::Array { "2-digit"sv, "numeric"sv, "narrow"sv, "short"sv, "long"sv };
    constexpr auto time_zone = AK::Array { "short"sv, "long"sv, "shortOffset"sv, "longOffset"sv, "shortGeneric"sv, "longGeneric"sv };

    // Table 6: Components of date and time formats, https://tc39.es/ecma402/#table-datetimeformat-components
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
    TRY(callback(pattern.time_zone_name, vm.names.timeZoneName, time_zone));

    return {};
}

}
