/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace JS::Temporal {

struct ParseResult {
    Optional<StringView> sign;
    Optional<StringView> date_year;
    Optional<StringView> date_month;
    Optional<StringView> date_day;
    Optional<StringView> time_hour;
    Optional<StringView> time_minute;
    Optional<StringView> time_second;
    Optional<StringView> time_fraction;
    Optional<StringView> time_hour_not_valid_month;
    Optional<StringView> time_hour_not_thirty_one_day_month;
    Optional<StringView> time_hour_two_only;
    Optional<StringView> time_minute_not_valid_day;
    Optional<StringView> time_minute_thirty_only;
    Optional<StringView> time_minute_thirty_one_only;
    Optional<StringView> time_second_not_valid_month;
    Optional<StringView> calendar_name;
    Optional<StringView> utc_designator;
    Optional<StringView> time_zone_numeric_utc_offset;
    Optional<StringView> time_zone_utc_offset_sign;
    Optional<StringView> time_zone_utc_offset_hour;
    Optional<StringView> time_zone_utc_offset_minute;
    Optional<StringView> time_zone_utc_offset_second;
    Optional<StringView> time_zone_utc_offset_fractional_part;
    Optional<StringView> time_zone_iana_name;
    Optional<StringView> duration_years;
    Optional<StringView> duration_months;
    Optional<StringView> duration_weeks;
    Optional<StringView> duration_days;
    Optional<StringView> duration_whole_hours;
    Optional<StringView> duration_hours_fraction;
    Optional<StringView> duration_whole_minutes;
    Optional<StringView> duration_minutes_fraction;
    Optional<StringView> duration_whole_seconds;
    Optional<StringView> duration_seconds_fraction;
};

enum class Production {
    TemporalInstantString,
    TemporalDateString,
    TemporalDateTimeString,
    TemporalDurationString,
    TemporalMonthDayString,
    TemporalTimeString,
    TemporalTimeZoneString,
    TemporalYearMonthString,
    TemporalZonedDateTimeString,
    TemporalCalendarString,
    TemporalRelativeToString,
};

Optional<ParseResult> parse_iso8601(Production, StringView);

namespace Detail {

// 13.33 ISO 8601 grammar, https://tc39.es/proposal-temporal/#sec-temporal-iso8601grammar
class ISO8601Parser {
public:
    explicit ISO8601Parser(StringView input)
        : m_input(input)
        , m_state({
              .lexer = GenericLexer { input },
              .parse_result = {},
          })
    {
    }

    [[nodiscard]] GenericLexer const& lexer() const { return m_state.lexer; }
    [[nodiscard]] ParseResult const& parse_result() const { return m_state.parse_result; }

    [[nodiscard]] bool parse_decimal_digits();
    [[nodiscard]] bool parse_decimal_digit();
    [[nodiscard]] bool parse_non_zero_digit();
    [[nodiscard]] bool parse_ascii_sign();
    [[nodiscard]] bool parse_sign();
    [[nodiscard]] bool parse_hour();
    [[nodiscard]] bool parse_minute_second();
    [[nodiscard]] bool parse_decimal_separator();
    [[nodiscard]] bool parse_days_designator();
    [[nodiscard]] bool parse_hours_designator();
    [[nodiscard]] bool parse_minutes_designator();
    [[nodiscard]] bool parse_months_designator();
    [[nodiscard]] bool parse_duration_designator();
    [[nodiscard]] bool parse_seconds_designator();
    [[nodiscard]] bool parse_date_time_separator();
    [[nodiscard]] bool parse_time_designator();
    [[nodiscard]] bool parse_weeks_designator();
    [[nodiscard]] bool parse_years_designator();
    [[nodiscard]] bool parse_utc_designator();
    [[nodiscard]] bool parse_date_year();
    [[nodiscard]] bool parse_date_month();
    [[nodiscard]] bool parse_date_day();
    [[nodiscard]] bool parse_date_spec_year_month();
    [[nodiscard]] bool parse_date_spec_month_day();
    [[nodiscard]] bool parse_date();
    [[nodiscard]] bool parse_time_hour();
    [[nodiscard]] bool parse_time_minute();
    [[nodiscard]] bool parse_time_second();
    [[nodiscard]] bool parse_time_hour_not_valid_month();
    [[nodiscard]] bool parse_time_hour_not_thirty_one_day_month();
    [[nodiscard]] bool parse_time_hour_two_only();
    [[nodiscard]] bool parse_time_minute_not_valid_day();
    [[nodiscard]] bool parse_time_minute_thirty_only();
    [[nodiscard]] bool parse_time_minute_thirty_one_only();
    [[nodiscard]] bool parse_time_second_not_valid_month();
    [[nodiscard]] bool parse_fractional_part();
    [[nodiscard]] bool parse_fraction();
    [[nodiscard]] bool parse_time_fraction();
    [[nodiscard]] bool parse_time_zone_utc_offset_sign();
    [[nodiscard]] bool parse_time_zone_utc_offset_hour();
    [[nodiscard]] bool parse_time_zone_utc_offset_minute();
    [[nodiscard]] bool parse_time_zone_utc_offset_second();
    [[nodiscard]] bool parse_time_zone_utc_offset_fractional_part();
    [[nodiscard]] bool parse_time_zone_utc_offset_fraction();
    [[nodiscard]] bool parse_time_zone_numeric_utc_offset();
    [[nodiscard]] bool parse_time_zone_utc_offset();
    [[nodiscard]] bool parse_time_zone_numeric_utc_offset_not_ambiguous();
    [[nodiscard]] bool parse_time_zone_numeric_utc_offset_not_ambiguous_allowed_negative_hour();
    [[nodiscard]] bool parse_time_zone_utc_offset_name();
    [[nodiscard]] bool parse_tz_leading_char();
    [[nodiscard]] bool parse_tz_char();
    [[nodiscard]] bool parse_time_zone_iana_component();
    [[nodiscard]] bool parse_time_zone_iana_name_tail();
    [[nodiscard]] bool parse_time_zone_iana_name();
    [[nodiscard]] bool parse_time_zone_bracketed_name();
    [[nodiscard]] bool parse_time_zone_bracketed_annotation();
    [[nodiscard]] bool parse_time_zone_offset_required();
    [[nodiscard]] bool parse_time_zone_name_required();
    [[nodiscard]] bool parse_time_zone();
    [[nodiscard]] bool parse_calendar_name();
    [[nodiscard]] bool parse_calendar();
    [[nodiscard]] bool parse_time_spec();
    [[nodiscard]] bool parse_time_hour_minute_basic_format_not_ambiguous();
    [[nodiscard]] bool parse_time_spec_with_optional_time_zone_not_ambiguous();
    [[nodiscard]] bool parse_time_spec_separator();
    [[nodiscard]] bool parse_date_time();
    [[nodiscard]] bool parse_calendar_time();
    [[nodiscard]] bool parse_calendar_date_time();
    [[nodiscard]] bool parse_calendar_date_time_time_required();
    [[nodiscard]] bool parse_duration_whole_seconds();
    [[nodiscard]] bool parse_duration_seconds_fraction();
    [[nodiscard]] bool parse_duration_seconds_part();
    [[nodiscard]] bool parse_duration_whole_minutes();
    [[nodiscard]] bool parse_duration_minutes_fraction();
    [[nodiscard]] bool parse_duration_minutes_part();
    [[nodiscard]] bool parse_duration_whole_hours();
    [[nodiscard]] bool parse_duration_hours_fraction();
    [[nodiscard]] bool parse_duration_hours_part();
    [[nodiscard]] bool parse_duration_time();
    [[nodiscard]] bool parse_duration_days();
    [[nodiscard]] bool parse_duration_days_part();
    [[nodiscard]] bool parse_duration_weeks();
    [[nodiscard]] bool parse_duration_weeks_part();
    [[nodiscard]] bool parse_duration_months();
    [[nodiscard]] bool parse_duration_months_part();
    [[nodiscard]] bool parse_duration_years();
    [[nodiscard]] bool parse_duration_years_part();
    [[nodiscard]] bool parse_duration_date();
    [[nodiscard]] bool parse_duration();
    [[nodiscard]] bool parse_temporal_instant_string();
    [[nodiscard]] bool parse_temporal_date_string();
    [[nodiscard]] bool parse_temporal_date_time_string();
    [[nodiscard]] bool parse_temporal_duration_string();
    [[nodiscard]] bool parse_temporal_month_day_string();
    [[nodiscard]] bool parse_temporal_time_string();
    [[nodiscard]] bool parse_temporal_time_zone_identifier();
    [[nodiscard]] bool parse_temporal_time_zone_string();
    [[nodiscard]] bool parse_temporal_year_month_string();
    [[nodiscard]] bool parse_temporal_zoned_date_time_string();
    [[nodiscard]] bool parse_temporal_calendar_string();
    [[nodiscard]] bool parse_temporal_relative_to_string();

private:
    struct State {
        GenericLexer lexer;
        ParseResult parse_result;
    };

    struct StateTransaction {
        explicit StateTransaction(ISO8601Parser& parser)
            : m_parser(parser)
            , m_saved_state(parser.m_state)
            , m_start_index(parser.m_state.lexer.tell())
        {
        }

        ~StateTransaction()
        {
            if (!m_commit)
                m_parser.m_state = move(m_saved_state);
        }

        void commit() { m_commit = true; }
        StringView parsed_string_view() const
        {
            return m_parser.m_input.substring_view(m_start_index, m_parser.m_state.lexer.tell() - m_start_index);
        }

    private:
        ISO8601Parser& m_parser;
        State m_saved_state;
        size_t m_start_index { 0 };
        bool m_commit { false };
    };

    StringView m_input;
    State m_state;
};

}

}
