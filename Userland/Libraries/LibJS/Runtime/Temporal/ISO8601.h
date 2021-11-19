/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
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
    Optional<StringView> time_fractional_part;
    Optional<StringView> calendar_name;
    Optional<StringView> utc_designator;
    Optional<StringView> time_zone_utc_offset_sign;
    Optional<StringView> time_zone_utc_offset_hour;
    Optional<StringView> time_zone_utc_offset_minute;
    Optional<StringView> time_zone_utc_offset_second;
    Optional<StringView> time_zone_utc_offset_fractional_part;
    Optional<StringView> time_zone_iana_name;
};

enum class Production {
    TemporalDateString,
    TemporalDateTimeString,
    TemporalMonthDayString,
    TemporalTimeString,
    TemporalTimeZoneString,
    TemporalYearMonthString,
    TemporalZonedDateTimeString,
    TemporalRelativeToString,
};

Optional<ParseResult> parse_iso8601(Production, StringView);

namespace Detail {

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

    [[nodiscard]] bool parse_decimal_digit();
    [[nodiscard]] bool parse_non_zero_digit();
    [[nodiscard]] bool parse_ascii_sign();
    [[nodiscard]] bool parse_sign();
    [[nodiscard]] bool parse_hour();
    [[nodiscard]] bool parse_minute_second();
    [[nodiscard]] bool parse_decimal_separator();
    [[nodiscard]] bool parse_date_time_separator();
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
    [[nodiscard]] bool parse_fractional_part();
    [[nodiscard]] bool parse_time_fractional_part();
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
    [[nodiscard]] bool parse_time_zone_utc_offset_name();
    [[nodiscard]] bool parse_time_zone_iana_name();
    [[nodiscard]] bool parse_time_zone_bracketed_name();
    [[nodiscard]] bool parse_time_zone_bracketed_annotation();
    [[nodiscard]] bool parse_time_zone_offset_required();
    [[nodiscard]] bool parse_time_zone_name_required();
    [[nodiscard]] bool parse_time_zone();
    [[nodiscard]] bool parse_calendar_name();
    [[nodiscard]] bool parse_calendar();
    [[nodiscard]] bool parse_time_spec();
    [[nodiscard]] bool parse_time();
    [[nodiscard]] bool parse_time_spec_separator();
    [[nodiscard]] bool parse_date_time();
    [[nodiscard]] bool parse_calendar_date_time();
    [[nodiscard]] bool parse_temporal_date_string();
    [[nodiscard]] bool parse_temporal_date_time_string();
    [[nodiscard]] bool parse_temporal_month_day_string();
    [[nodiscard]] bool parse_temporal_time_string();
    [[nodiscard]] bool parse_temporal_time_zone_identifier();
    [[nodiscard]] bool parse_temporal_time_zone_string();
    [[nodiscard]] bool parse_temporal_year_month_string();
    [[nodiscard]] bool parse_temporal_zoned_date_time_string();
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
