/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibJS/Runtime/Temporal/ISO8601.h>

namespace JS::Temporal {

namespace Detail {

// https://tc39.es/proposal-temporal/#prod-DecimalDigit
bool ISO8601Parser::parse_decimal_digit()
{
    // DecimalDigit : one of
    //     0 1 2 3 4 5 6 7 8 9
    if (m_state.lexer.next_is(is_ascii_digit)) {
        m_state.lexer.consume();
        return true;
    }
    return false;
}

// https://tc39.es/proposal-temporal/#prod-NonZeroDigit
bool ISO8601Parser::parse_non_zero_digit()
{
    // NonZeroDigit : one of
    //     1 2 3 4 5 6 7 8 9
    if (m_state.lexer.next_is(is_ascii_digit) && !m_state.lexer.next_is('0')) {
        m_state.lexer.consume();
        return true;
    }
    return false;
}

// https://tc39.es/proposal-temporal/#prod-ASCIISign
bool ISO8601Parser::parse_ascii_sign()
{
    // ASCIISign : one of
    //     + -
    return m_state.lexer.consume_specific('+')
        || m_state.lexer.consume_specific('-');
}

// https://tc39.es/proposal-temporal/#prod-Sign
bool ISO8601Parser::parse_sign()
{
    // Sign :
    //     ASCIISign
    //     U+2212
    StateTransaction transaction { *this };
    auto success = parse_ascii_sign()
        || m_state.lexer.consume_specific("\xE2\x88\x92"sv);
    if (!success)
        return false;
    m_state.parse_result.sign = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-Hour
bool ISO8601Parser::parse_hour()
{
    // Hour :
    //     0 DecimalDigit
    //     1 DecimalDigit
    //     20
    //     21
    //     22
    //     23
    StateTransaction transaction { *this };
    if (m_state.lexer.consume_specific('0') || m_state.lexer.consume_specific('1')) {
        if (!parse_decimal_digit())
            return false;
    } else {
        auto success = m_state.lexer.consume_specific("20"sv)
            || m_state.lexer.consume_specific("21"sv)
            || m_state.lexer.consume_specific("22"sv)
            || m_state.lexer.consume_specific("23"sv);
        if (!success)
            return false;
    }
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-MinuteSecond
bool ISO8601Parser::parse_minute_second()
{
    // MinuteSecond :
    //     0 DecimalDigit
    //     1 DecimalDigit
    //     2 DecimalDigit
    //     3 DecimalDigit
    //     4 DecimalDigit
    //     5 DecimalDigit
    StateTransaction transaction { *this };
    auto success = m_state.lexer.consume_specific('0')
        || m_state.lexer.consume_specific('1')
        || m_state.lexer.consume_specific('2')
        || m_state.lexer.consume_specific('3')
        || m_state.lexer.consume_specific('4')
        || m_state.lexer.consume_specific('5');
    if (!success)
        return false;
    if (!parse_decimal_digit())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DecimalSeparator
bool ISO8601Parser::parse_decimal_separator()
{
    // DecimalSeparator : one of
    //     . ,
    return m_state.lexer.consume_specific('.')
        || m_state.lexer.consume_specific(',');
}

// https://tc39.es/proposal-temporal/#prod-DateTimeSeparator
bool ISO8601Parser::parse_date_time_separator()
{
    // DateTimeSeparator :
    //     <SP>
    //     T
    //     t
    return m_state.lexer.consume_specific(' ')
        || m_state.lexer.consume_specific('T')
        || m_state.lexer.consume_specific('t');
}

// https://tc39.es/proposal-temporal/#prod-DateYear
bool ISO8601Parser::parse_date_year()
{
    // DateFourDigitYear :
    //     DecimalDigit DecimalDigit DecimalDigit DecimalDigit
    // DateExtendedYear :
    //     Sign DecimalDigit DecimalDigit DecimalDigit DecimalDigit DecimalDigit DecimalDigit
    // DateYear :
    //     DateFourDigitYear
    //     DateExtendedYear
    StateTransaction transaction { *this };
    if (parse_sign()) {
        for (size_t i = 0; i < 6; ++i) {
            if (!parse_decimal_digit())
                return false;
        }
    } else {
        for (size_t i = 0; i < 4; ++i) {
            if (!parse_decimal_digit())
                return false;
        }
    }
    m_state.parse_result.date_year = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DateMonth
bool ISO8601Parser::parse_date_month()
{
    // DateMonth :
    //     0 NonZeroDigit
    //     10
    //     11
    //     12
    StateTransaction transaction { *this };
    if (m_state.lexer.consume_specific('0')) {
        if (!parse_non_zero_digit())
            return false;
    } else {
        auto success = m_state.lexer.consume_specific("10"sv)
            || m_state.lexer.consume_specific("11"sv)
            || m_state.lexer.consume_specific("12"sv);
        if (!success)
            return false;
    }
    m_state.parse_result.date_month = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DateDay
bool ISO8601Parser::parse_date_day()
{
    // DateDay :
    //     0 NonZeroDigit
    //     1 DecimalDigit
    //     2 DecimalDigit
    //     30
    //     31
    StateTransaction transaction { *this };
    if (m_state.lexer.consume_specific('0')) {
        if (!parse_non_zero_digit())
            return false;
    } else if (m_state.lexer.consume_specific('1') || m_state.lexer.consume_specific('2')) {
        if (!parse_decimal_digit())
            return false;
    } else {
        auto success = m_state.lexer.consume_specific("30"sv)
            || m_state.lexer.consume_specific("31"sv);
        if (!success)
            return false;
    }
    m_state.parse_result.date_day = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DateSpecYearMonth
bool ISO8601Parser::parse_date_spec_year_month()
{
    // DateSpecYearMonth :
    //     DateYear -[opt] DateMonth
    StateTransaction transaction { *this };
    if (!parse_date_year())
        return false;
    m_state.lexer.consume_specific('-');
    if (!parse_date_month())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DateSpecMonthDay
bool ISO8601Parser::parse_date_spec_month_day()
{
    // TwoDashes :
    //     --
    // DateSpecMonthDay :
    //     TwoDashes[opt] DateMonth -[opt] DateDay
    StateTransaction transaction { *this };
    m_state.lexer.consume_specific("--"sv);
    if (!parse_date_month())
        return false;
    m_state.lexer.consume_specific('-');
    if (!parse_date_day())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-Date
bool ISO8601Parser::parse_date()
{
    // Date :
    //     DateYear - DateMonth - DateDay
    //     DateYear DateMonth DateDay
    StateTransaction transaction { *this };
    if (!parse_date_year())
        return false;
    auto with_dashes = m_state.lexer.consume_specific('-');
    if (!parse_date_month())
        return false;
    if (with_dashes && !m_state.lexer.consume_specific('-'))
        return false;
    if (!parse_date_day())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeHour
bool ISO8601Parser::parse_time_hour()
{
    // TimeHour :
    //     Hour
    StateTransaction transaction { *this };
    if (!parse_hour())
        return false;
    m_state.parse_result.time_hour = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeMinute
bool ISO8601Parser::parse_time_minute()
{
    // TimeMinute :
    //     MinuteSecond
    StateTransaction transaction { *this };
    if (!parse_minute_second())
        return false;
    m_state.parse_result.time_minute = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeSecond
bool ISO8601Parser::parse_time_second()
{
    // TimeSecond :
    //     MinuteSecond
    //     60
    StateTransaction transaction { *this };
    auto success = parse_minute_second()
        || m_state.lexer.consume_specific("60"sv);
    if (!success)
        return false;
    m_state.parse_result.time_second = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-FractionalPart
bool ISO8601Parser::parse_fractional_part()
{
    // FractionalPart :
    //     DecimalDigit DecimalDigit[opt] DecimalDigit[opt] DecimalDigit[opt] DecimalDigit[opt] DecimalDigit[opt] DecimalDigit[opt] DecimalDigit[opt] DecimalDigit[opt]
    if (!parse_decimal_digit())
        return false;
    for (size_t i = 0; i < 8; ++i) {
        if (!parse_decimal_digit())
            break;
    }
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeFractionalPart
bool ISO8601Parser::parse_time_fractional_part()
{
    // TimeFractionalPart :
    //     FractionalPart
    StateTransaction transaction { *this };
    if (!parse_fractional_part())
        return false;
    m_state.parse_result.time_fractional_part = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-Fraction
bool ISO8601Parser::parse_fraction()
{
    // Fraction :
    //     DecimalSeparator TimeFractionalPart
    StateTransaction transaction { *this };
    if (!parse_decimal_separator())
        return false;
    if (!parse_time_fractional_part())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeFraction
bool ISO8601Parser::parse_time_fraction()
{
    // TimeFraction :
    //     Fraction
    return parse_fraction();
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneOffsetRequired
bool ISO8601Parser::parse_time_zone_offset_required()
{
    // TimeZoneOffsetRequired :
    //     TimeZoneUTCOffset TimeZoneBracketedAnnotation[opt]
    return false;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneNameRequired
bool ISO8601Parser::parse_time_zone_name_required()
{
    // TimeZoneNameRequired :
    //     TimeZoneUTCOffset[opt] TimeZoneBracketedAnnotation
    return false;
}

// https://tc39.es/proposal-temporal/#prod-TimeZone
bool ISO8601Parser::parse_time_zone()
{
    // TimeZone :
    //     TimeZoneOffsetRequired
    //     TimeZoneNameRequired
    return parse_time_zone_offset_required()
        || parse_time_zone_name_required();
}

// https://tc39.es/proposal-temporal/#prod-CalendarName
bool ISO8601Parser::parse_calendar_name()
{
    // CalChar :
    //     Alpha
    //     DecimalDigit
    // CalendarNameComponent :
    //     CalChar CalChar CalChar CalChar[opt] CalChar[opt] CalChar[opt] CalChar[opt] CalChar[opt]
    // CalendarNameTail :
    //     CalendarNameComponent
    //     CalendarNameComponent - CalendarNameTail
    // CalendarName :
    //     CalendarNameTail
    auto parse_calendar_name_component = [&] {
        for (size_t i = 0; i < 8; ++i) {
            if (!m_state.lexer.next_is(is_ascii_alphanumeric))
                return i > 2;
            m_state.lexer.consume();
        }
        return true;
    };
    StateTransaction transaction { *this };
    do {
        if (!parse_calendar_name_component())
            return false;
    } while (m_state.lexer.consume_specific('-'));
    m_state.parse_result.calendar_name = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-Calendar
bool ISO8601Parser::parse_calendar()
{
    // Calendar :
    //     [u-ca= CalendarName ]
    StateTransaction transaction { *this };
    if (!m_state.lexer.consume_specific("[u-ca="sv))
        return false;
    if (!parse_calendar_name())
        return false;
    if (!m_state.lexer.consume_specific(']'))
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeSpec
bool ISO8601Parser::parse_time_spec()
{
    // TimeSpec :
    //     TimeHour
    //     TimeHour : TimeMinute
    //     TimeHour TimeMinute
    //     TimeHour : TimeMinute : TimeSecond TimeFraction[opt]
    //     TimeHour TimeMinute TimeSecond TimeFraction[opt]
    StateTransaction transaction { *this };
    if (!parse_time_hour())
        return false;
    if (m_state.lexer.consume_specific(':')) {
        if (!parse_time_minute())
            return false;
        if (m_state.lexer.consume_specific(':')) {
            if (!parse_time_second())
                return false;
            (void)parse_time_fraction();
        }
    } else if (parse_time_minute()) {
        if (parse_time_second())
            (void)parse_time_fraction();
    }
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-Time
bool ISO8601Parser::parse_time()
{
    // Time :
    //     TimeSpec TimeZone[opt]
    if (!parse_time_spec())
        return false;
    (void)parse_time_zone();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeSpecSeparator
bool ISO8601Parser::parse_time_spec_separator()
{
    // TimeSpecSeparator :
    //     DateTimeSeparator TimeSpec
    StateTransaction transaction { *this };
    if (!parse_date_time_separator())
        return false;
    if (!parse_time_spec())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DateTime
bool ISO8601Parser::parse_date_time()
{
    // DateTime :
    //     Date TimeSpecSeparator[opt] TimeZone[opt]
    if (!parse_date())
        return false;
    (void)parse_time_spec_separator();
    (void)parse_time_zone();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-CalendarDateTime
bool ISO8601Parser::parse_calendar_date_time()
{
    // CalendarDateTime :
    //     DateTime Calendar[opt]
    if (!parse_date_time())
        return false;
    (void)parse_calendar();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TemporalDateString
bool ISO8601Parser::parse_temporal_date_string()
{
    // TemporalDateString :
    //     CalendarDateTime
    return parse_calendar_date_time();
}

// https://tc39.es/proposal-temporal/#prod-TemporalDateTimeString
bool ISO8601Parser::parse_temporal_date_time_string()
{
    // TemporalDateTimeString :
    //     CalendarDateTime
    return parse_calendar_date_time();
}

// https://tc39.es/proposal-temporal/#prod-TemporalMonthDayString
bool ISO8601Parser::parse_temporal_month_day_string()
{
    // TemporalMonthDayString :
    //     DateSpecMonthDay
    //     DateTime
    // NOTE: Reverse order here because `DateSpecMonthDay` can be a subset of `DateTime`,
    // so we'd not attempt to parse that but may not exhaust the input string.
    return parse_date_time()
        || parse_date_spec_month_day();
}

// https://tc39.es/proposal-temporal/#prod-TemporalTimeString
bool ISO8601Parser::parse_temporal_time_string()
{
    // TemporalTimeString :
    //     Time
    //     DateTime
    // NOTE: Reverse order here because `Time` can be a subset of `DateTime`,
    // so we'd not attempt to parse that but may not exhaust the input string.
    return parse_date_time()
        || parse_time();
}

// https://tc39.es/proposal-temporal/#prod-TemporalYearMonthString
bool ISO8601Parser::parse_temporal_year_month_string()
{
    // TemporalYearMonthString :
    //     DateSpecYearMonth
    //     DateTime
    // NOTE: Reverse order here because `DateSpecYearMonth` can be a subset of `DateTime`,
    // so we'd not attempt to parse that but may not exhaust the input string.
    return parse_date_time()
        || parse_date_spec_year_month();
}

}

#define JS_ENUMERATE_ISO8601_PRODUCTION_PARSERS                             \
    __JS_ENUMERATE(TemporalDateString, parse_temporal_date_string)          \
    __JS_ENUMERATE(TemporalDateTimeString, parse_temporal_date_time_string) \
    __JS_ENUMERATE(TemporalMonthDayString, parse_temporal_month_day_string) \
    __JS_ENUMERATE(TemporalTimeString, parse_temporal_time_string)          \
    __JS_ENUMERATE(TemporalYearMonthString, parse_temporal_year_month_string)

Optional<ParseResult> parse_iso8601(Production production, StringView input)
{
    auto parser = Detail::ISO8601Parser { input };

    switch (production) {
#define __JS_ENUMERATE(ProductionName, parse_production) \
    case Production::ProductionName:                     \
        if (!parser.parse_production())                  \
            return {};                                   \
        break;
        JS_ENUMERATE_ISO8601_PRODUCTION_PARSERS
#undef __JS_ENUMERATE
    default:
        VERIFY_NOT_REACHED();
    }

    // If we parsed successfully but didn't reach the end, the string doesn't match the given production.
    if (!parser.lexer().is_eof())
        return {};

    return parser.parse_result();
}

}
