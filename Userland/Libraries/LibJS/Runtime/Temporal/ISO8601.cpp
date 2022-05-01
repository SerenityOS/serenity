/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibJS/Runtime/Temporal/ISO8601.h>

namespace JS::Temporal {

namespace Detail {

// https://tc39.es/proposal-temporal/#prod-DecimalDigits
bool ISO8601Parser::parse_decimal_digits()
{
    // DecimalDigits[Sep] ::
    //     DecimalDigit
    //     DecimalDigits[?Sep] DecimalDigit
    //     [+Sep] DecimalDigits[+Sep] NumericLiteralSeparator DecimalDigit
    // NOTE: Temporal exclusively uses the variant without a separator ([~Sep])
    if (!parse_decimal_digit())
        return false;
    while (parse_decimal_digit())
        ;
    return true;
}

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

// https://tc39.es/proposal-temporal/#prod-UnpaddedHour
bool ISO8601Parser::parse_unpadded_hour()
{
    // UnpaddedHour :
    //     DecimalDigit
    //     1 DecimalDigit
    //     20
    //     21
    //     22
    //     23
    StateTransaction transaction { *this };
    auto success = m_state.lexer.consume_specific("20"sv)
        || m_state.lexer.consume_specific("21"sv)
        || m_state.lexer.consume_specific("22"sv)
        || m_state.lexer.consume_specific("23"sv);
    if (!success) {
        // This could be either of the first two productions.
        if (m_state.lexer.consume_specific('1'))
            (void)parse_decimal_digit();
        else if (!parse_decimal_digit())
            return false;
    }
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

// https://tc39.es/proposal-temporal/#prod-DaysDesignator
bool ISO8601Parser::parse_days_designator()
{
    // DaysDesignator : one of
    //     D d
    return m_state.lexer.consume_specific('D')
        || m_state.lexer.consume_specific('d');
}

// https://tc39.es/proposal-temporal/#prod-HoursDesignator
bool ISO8601Parser::parse_hours_designator()
{
    // HoursDesignator : one of
    //     H h
    return m_state.lexer.consume_specific('H')
        || m_state.lexer.consume_specific('h');
}

// https://tc39.es/proposal-temporal/#prod-MinutesDesignator
bool ISO8601Parser::parse_minutes_designator()
{
    // MinutesDesignator : one of
    //     M m
    return m_state.lexer.consume_specific('M')
        || m_state.lexer.consume_specific('m');
}

// https://tc39.es/proposal-temporal/#prod-MonthsDesignator
bool ISO8601Parser::parse_months_designator()
{
    // MonthsDesignator : one of
    //     M m
    return m_state.lexer.consume_specific('M')
        || m_state.lexer.consume_specific('m');
}

// https://tc39.es/proposal-temporal/#prod-DurationDesignator
bool ISO8601Parser::parse_duration_designator()
{
    // DurationDesignator : one of
    //     P p
    return m_state.lexer.consume_specific('P')
        || m_state.lexer.consume_specific('p');
}

// https://tc39.es/proposal-temporal/#prod-SecondsDesignator
bool ISO8601Parser::parse_seconds_designator()
{
    // SecondsDesignator : one of
    //     S s
    return m_state.lexer.consume_specific('S')
        || m_state.lexer.consume_specific('s');
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

// https://tc39.es/proposal-temporal/#prod-TimeDesignator
bool ISO8601Parser::parse_time_designator()
{
    // TimeDesignator : one of
    //     T t
    return m_state.lexer.consume_specific('T')
        || m_state.lexer.consume_specific('t');
}

// https://tc39.es/proposal-temporal/#prod-WeeksDesignator
bool ISO8601Parser::parse_weeks_designator()
{
    // WeeksDesignator : one of
    //     W w
    return m_state.lexer.consume_specific('W')
        || m_state.lexer.consume_specific('w');
}

// https://tc39.es/proposal-temporal/#prod-YearsDesignator
bool ISO8601Parser::parse_years_designator()
{
    // YearsDesignator : one of
    //     Y y
    return m_state.lexer.consume_specific('Y')
        || m_state.lexer.consume_specific('y');
}

// https://tc39.es/proposal-temporal/#prod-UTCDesignator
bool ISO8601Parser::parse_utc_designator()
{
    // UTCDesignator : one of
    //     Z z
    StateTransaction transaction { *this };
    auto success = m_state.lexer.consume_specific('Z')
        || m_state.lexer.consume_specific('z');
    if (!success)
        return false;
    m_state.parse_result.utc_designator = transaction.parsed_string_view();
    transaction.commit();
    return true;
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
    // It is a Syntax Error if DateExtendedYear is "-000000" or "−000000" (U+2212 MINUS SIGN followed by 000000).
    if (transaction.parsed_string_view().is_one_of("-000000"sv, "−000000"sv))
        return false;
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

// https://tc39.es/proposal-temporal/#prod-TimeHourNotValidMonth
bool ISO8601Parser::parse_time_hour_not_valid_month()
{
    // TimeHourNotValidMonth : one of
    //     00 13 14 15 16 17 18 19 20 21 23
    StateTransaction transaction { *this };
    auto success = m_state.lexer.consume_specific("00"sv)
        || m_state.lexer.consume_specific("13"sv)
        || m_state.lexer.consume_specific("14"sv)
        || m_state.lexer.consume_specific("15"sv)
        || m_state.lexer.consume_specific("16"sv)
        || m_state.lexer.consume_specific("17"sv)
        || m_state.lexer.consume_specific("18"sv)
        || m_state.lexer.consume_specific("19"sv)
        || m_state.lexer.consume_specific("20"sv)
        || m_state.lexer.consume_specific("21"sv)
        || m_state.lexer.consume_specific("22"sv)
        || m_state.lexer.consume_specific("23"sv);
    if (!success)
        return false;
    m_state.parse_result.time_hour_not_valid_month = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeHourNotThirtyOneDayMonth
bool ISO8601Parser::parse_time_hour_not_thirty_one_day_month()
{
    // TimeHourNotThirtyOneDayMonth : one of
    //     02 04 06 09 11
    StateTransaction transaction { *this };
    auto success = m_state.lexer.consume_specific("02"sv)
        || m_state.lexer.consume_specific("04"sv)
        || m_state.lexer.consume_specific("06"sv)
        || m_state.lexer.consume_specific("09"sv)
        || m_state.lexer.consume_specific("11"sv);
    if (!success)
        return false;
    m_state.parse_result.time_hour_not_thirty_one_day_month = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeHourTwoOnly
bool ISO8601Parser::parse_time_hour_two_only()
{
    // TimeHourTwoOnly :
    //     02
    StateTransaction transaction { *this };
    if (!m_state.lexer.consume_specific("02"sv))
        return false;
    m_state.parse_result.time_hour_two_only = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeMinuteNotValidDay
bool ISO8601Parser::parse_time_minute_not_valid_day()
{
    // TimeMinuteNotValidDay :
    //     00
    //     32
    //     33
    //     34
    //     35
    //     36
    //     37
    //     38
    //     39
    //     4 DecimalDigit
    //     5 DecimalDigit
    //     60
    StateTransaction transaction { *this };
    if (m_state.lexer.consume_specific('4') || m_state.lexer.consume_specific('5')) {
        if (!parse_decimal_digit())
            return false;
    } else {
        auto success = m_state.lexer.consume_specific("00"sv)
            || m_state.lexer.consume_specific("32"sv)
            || m_state.lexer.consume_specific("33"sv)
            || m_state.lexer.consume_specific("34"sv)
            || m_state.lexer.consume_specific("35"sv)
            || m_state.lexer.consume_specific("36"sv)
            || m_state.lexer.consume_specific("37"sv)
            || m_state.lexer.consume_specific("38"sv)
            || m_state.lexer.consume_specific("39"sv)
            || m_state.lexer.consume_specific("60"sv);
        if (!success)
            return false;
    }
    m_state.parse_result.time_minute_not_valid_day = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeMinuteThirtyOnly
bool ISO8601Parser::parse_time_minute_thirty_only()
{
    // TimeMinuteThirtyOnly :
    //     30
    StateTransaction transaction { *this };
    if (!m_state.lexer.consume_specific("30"sv))
        return false;
    m_state.parse_result.time_minute_thirty_only = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeMinuteThirtyOneOnly
bool ISO8601Parser::parse_time_minute_thirty_one_only()
{
    // TimeMinuteThirtyOneOnly :
    //     31
    StateTransaction transaction { *this };
    if (!m_state.lexer.consume_specific("31"sv))
        return false;
    m_state.parse_result.time_minute_thirty_one_only = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeSecondNotValidMonth
bool ISO8601Parser::parse_time_second_not_valid_month()
{
    // TimeSecondNotValidMonth :
    //     00
    //     13
    //     14
    //     15
    //     16
    //     17
    //     18
    //     19
    //     2 DecimalDigit
    //     3 DecimalDigit
    //     4 DecimalDigit
    //     5 DecimalDigit
    //     60
    StateTransaction transaction { *this };
    if (m_state.lexer.consume_specific('2')
        || m_state.lexer.consume_specific('3')
        || m_state.lexer.consume_specific('4')
        || m_state.lexer.consume_specific('5')) {
        if (!parse_decimal_digit())
            return false;
    } else {
        auto success = m_state.lexer.consume_specific("00"sv)
            || m_state.lexer.consume_specific("13"sv)
            || m_state.lexer.consume_specific("14"sv)
            || m_state.lexer.consume_specific("15"sv)
            || m_state.lexer.consume_specific("16"sv)
            || m_state.lexer.consume_specific("17"sv)
            || m_state.lexer.consume_specific("18"sv)
            || m_state.lexer.consume_specific("19"sv)
            || m_state.lexer.consume_specific("60"sv);
        if (!success)
            return false;
    }
    m_state.parse_result.time_second_not_valid_month = transaction.parsed_string_view();
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

// https://tc39.es/proposal-temporal/#prod-Fraction
bool ISO8601Parser::parse_fraction()
{
    // Fraction :
    //     DecimalSeparator FractionalPart
    StateTransaction transaction { *this };
    if (!parse_decimal_separator())
        return false;
    if (!parse_fractional_part())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeFraction
bool ISO8601Parser::parse_time_fraction()
{
    // TimeFraction :
    //     Fraction
    StateTransaction transaction { *this };
    if (!parse_fraction())
        return false;
    m_state.parse_result.time_fraction = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneUTCOffsetSign
bool ISO8601Parser::parse_time_zone_utc_offset_sign()
{
    // TimeZoneUTCOffsetSign :
    //     Sign
    StateTransaction transaction { *this };
    if (!parse_sign())
        return false;
    m_state.parse_result.time_zone_utc_offset_sign = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneUTCOffsetHour
bool ISO8601Parser::parse_time_zone_utc_offset_hour()
{
    // TimeZoneUTCOffsetHour :
    //     Hour
    StateTransaction transaction { *this };
    if (!parse_hour())
        return false;
    m_state.parse_result.time_zone_utc_offset_hour = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneUTCOffsetMinute
bool ISO8601Parser::parse_time_zone_utc_offset_minute()
{
    // TimeZoneUTCOffsetMinute :
    //     MinuteSecond
    StateTransaction transaction { *this };
    if (!parse_minute_second())
        return false;
    m_state.parse_result.time_zone_utc_offset_minute = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneUTCOffsetSecond
bool ISO8601Parser::parse_time_zone_utc_offset_second()
{
    // TimeZoneUTCOffsetSecond :
    //     MinuteSecond
    StateTransaction transaction { *this };
    if (!parse_minute_second())
        return false;
    m_state.parse_result.time_zone_utc_offset_second = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneUTCOffsetFractionalPart
bool ISO8601Parser::parse_time_zone_utc_offset_fractional_part()
{
    // TimeZoneUTCOffsetFractionalPart :
    //     FractionalPart
    StateTransaction transaction { *this };
    if (!parse_fractional_part())
        return false;
    m_state.parse_result.time_zone_utc_offset_fractional_part = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneUTCOffsetFraction
bool ISO8601Parser::parse_time_zone_utc_offset_fraction()
{
    // TimeZoneUTCOffsetFraction :
    //     DecimalSeparator TimeZoneUTCOffsetFractionalPart
    StateTransaction transaction { *this };
    if (!parse_decimal_separator())
        return false;
    if (!parse_time_zone_utc_offset_fractional_part())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneNumericUTCOffset
bool ISO8601Parser::parse_time_zone_numeric_utc_offset()
{
    // TimeZoneNumericUTCOffset :
    //     TimeZoneUTCOffsetSign TimeZoneUTCOffsetHour
    //     TimeZoneUTCOffsetSign TimeZoneUTCOffsetHour : TimeZoneUTCOffsetMinute
    //     TimeZoneUTCOffsetSign TimeZoneUTCOffsetHour TimeZoneUTCOffsetMinute
    //     TimeZoneUTCOffsetSign TimeZoneUTCOffsetHour : TimeZoneUTCOffsetMinute : TimeZoneUTCOffsetSecond TimeZoneUTCOffsetFraction[opt]
    //     TimeZoneUTCOffsetSign TimeZoneUTCOffsetHour TimeZoneUTCOffsetMinute TimeZoneUTCOffsetSecond TimeZoneUTCOffsetFraction[opt]
    StateTransaction transaction { *this };
    if (!parse_time_zone_utc_offset_sign())
        return false;
    if (!parse_time_zone_utc_offset_hour())
        return false;
    if (m_state.lexer.consume_specific(':')) {
        if (!parse_time_zone_utc_offset_minute())
            return false;
        if (m_state.lexer.consume_specific(':')) {
            if (!parse_time_zone_utc_offset_second())
                return false;
            (void)parse_time_zone_utc_offset_fraction();
        }
    } else if (parse_time_zone_utc_offset_minute()) {
        if (parse_time_zone_utc_offset_second())
            (void)parse_time_zone_utc_offset_fraction();
    }
    m_state.parse_result.time_zone_numeric_utc_offset = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneUTCOffset
bool ISO8601Parser::parse_time_zone_utc_offset()
{
    // TimeZoneUTCOffset :
    //     TimeZoneNumericUTCOffset
    //     UTCDesignator
    return parse_time_zone_numeric_utc_offset()
        || parse_utc_designator();
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneNumericUTCOffsetNotAmbiguous
bool ISO8601Parser::parse_time_zone_numeric_utc_offset_not_ambiguous()
{
    // TimeZoneNumericUTCOffsetNotAmbiguous :
    //     + TimeZoneUTCOffsetHour
    //     U+2212 TimeZoneUTCOffsetHour
    //     TimeZoneUTCOffsetSign TimeZoneUTCOffsetHour : TimeZoneUTCOffsetMinute
    //     TimeZoneUTCOffsetSign TimeZoneUTCOffsetHour TimeZoneUTCOffsetMinute
    //     TimeZoneUTCOffsetSign TimeZoneUTCOffsetHour : TimeZoneUTCOffsetMinute : TimeZoneUTCOffsetSecond TimeZoneUTCOffsetFraction[opt]
    //     TimeZoneUTCOffsetSign TimeZoneUTCOffsetHour TimeZoneUTCOffsetMinute TimeZoneUTCOffsetSecond TimeZoneUTCOffsetFraction[opt]
    StateTransaction transaction { *this };
    if (m_state.lexer.consume_specific('+') || m_state.lexer.consume_specific("\xE2\x88\x92"sv)) {
        if (!parse_time_zone_utc_offset_hour())
            return false;
    } else {
        if (!parse_time_zone_utc_offset_sign())
            return false;
        if (!parse_time_zone_utc_offset_hour())
            return false;
        if (m_state.lexer.consume_specific(':')) {
            if (!parse_time_zone_utc_offset_minute())
                return false;
            if (m_state.lexer.consume_specific(':')) {
                if (!parse_time_zone_utc_offset_second())
                    return false;
                (void)parse_time_zone_utc_offset_fraction();
            }
        } else {
            if (!parse_time_zone_utc_offset_minute())
                return false;
            if (parse_time_zone_utc_offset_second())
                (void)parse_time_zone_utc_offset_fraction();
        }
    }
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneNumericUTCOffsetNotAmbiguousAllowedNegativeHour
bool ISO8601Parser::parse_time_zone_numeric_utc_offset_not_ambiguous_allowed_negative_hour()
{
    // TimeZoneNumericUTCOffsetNotAmbiguousAllowedNegativeHour :
    //     TimeZoneNumericUTCOffsetNotAmbiguous
    //     - TimeHourNotValidMonth
    StateTransaction transaction { *this };
    if (!parse_time_zone_numeric_utc_offset_not_ambiguous()) {
        if (!m_state.lexer.consume_specific('-'))
            return false;
        if (!parse_time_hour_not_valid_month())
            return false;
    }
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneUTCOffsetName
bool ISO8601Parser::parse_time_zone_utc_offset_name()
{
    // TimeZoneUTCOffsetName :
    //     Sign Hour
    //     Sign Hour : MinuteSecond
    //     Sign Hour MinuteSecond
    //     Sign Hour : MinuteSecond : MinuteSecond Fraction[opt]
    //     Sign Hour MinuteSecond MinuteSecond Fraction[opt]
    StateTransaction transaction { *this };
    if (!parse_sign())
        return false;
    if (!parse_hour())
        return false;
    if (m_state.lexer.consume_specific(':')) {
        if (!parse_minute_second())
            return false;
        if (m_state.lexer.consume_specific(':')) {
            if (!parse_minute_second())
                return false;
            (void)parse_fraction();
        }
    } else if (parse_minute_second()) {
        if (parse_minute_second())
            (void)parse_fraction();
    }
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TZLeadingChar
bool ISO8601Parser::parse_tz_leading_char()
{
    // TZLeadingChar :
    //     Alpha
    //     .
    //     _
    if (m_state.lexer.next_is(is_ascii_alpha)) {
        m_state.lexer.consume();
        return true;
    }
    return m_state.lexer.consume_specific('.')
        || m_state.lexer.consume_specific('_');
}

// https://tc39.es/proposal-temporal/#prod-TZChar
bool ISO8601Parser::parse_tz_char()
{
    // TZChar :
    //     Alpha
    //     .
    //     -
    //     _
    if (m_state.lexer.next_is(is_ascii_alpha)) {
        m_state.lexer.consume();
        return true;
    }
    return m_state.lexer.consume_specific('.')
        || m_state.lexer.consume_specific('-')
        || m_state.lexer.consume_specific('_');
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneIANANameComponent
bool ISO8601Parser::parse_time_zone_iana_component()
{
    // TimeZoneIANANameComponent :
    //     TZLeadingChar TZChar[opt] TZChar[opt] TZChar[opt] TZChar[opt] TZChar[opt] TZChar[opt] TZChar[opt] TZChar[opt] TZChar[opt] TZChar[opt] TZChar[opt] TZChar[opt] TZChar[opt] but not one of . or ..
    StateTransaction transaction { *this };
    if (!parse_tz_leading_char())
        return false;
    for (size_t i = 0; i < 13; ++i) {
        if (!parse_tz_char())
            break;
    }
    if (transaction.parsed_string_view().is_one_of("."sv, ".."sv))
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneIANANameTail
bool ISO8601Parser::parse_time_zone_iana_name_tail()
{
    // TimeZoneIANANameTail :
    //     TimeZoneIANANameComponent
    //     TimeZoneIANANameComponent / TimeZoneIANANameTail
    StateTransaction transaction { *this };
    if (!parse_time_zone_iana_component())
        return false;
    while (m_state.lexer.next_is('/')) {
        m_state.lexer.consume();
        if (!parse_time_zone_iana_component())
            return false;
    }
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneIANAName
bool ISO8601Parser::parse_time_zone_iana_name()
{
    // TimeZoneIANAName :
    //     Etc/GMT ASCIISign UnpaddedHour
    //     TimeZoneIANANameTail but not Etc/GMT ASCIISign UnpaddedHour
    auto parse_etc_gmt_with_offset = [this] {
        StateTransaction transaction { *this };
        if (!m_state.lexer.consume_specific("Etc/GMT"sv))
            return false;
        if (!parse_ascii_sign())
            return false;
        if (!parse_unpadded_hour())
            return false;
        transaction.commit();
        return true;
    };
    StateTransaction transaction { *this };
    if (parse_etc_gmt_with_offset()) {
        // no-op.
    } else if (!parse_time_zone_iana_name_tail()) {
        return false;
    }
    m_state.parse_result.time_zone_iana_name = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneBracketedName
bool ISO8601Parser::parse_time_zone_bracketed_name()
{
    // TimeZoneBracketedName :
    //     TimeZoneIANAName
    //     TimeZoneUTCOffsetName
    StateTransaction transaction { *this };
    if (parse_time_zone_iana_name()) {
        // no-op.
    } else if (!parse_time_zone_utc_offset_name()) {
        return false;
    }
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneBracketedAnnotation
bool ISO8601Parser::parse_time_zone_bracketed_annotation()
{
    // TimeZoneBracketedAnnotation :
    //     [ TimeZoneBracketedName ]
    StateTransaction transaction { *this };
    if (!m_state.lexer.consume_specific('['))
        return false;
    if (!parse_time_zone_bracketed_name())
        return false;
    if (!m_state.lexer.consume_specific(']'))
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneOffsetRequired
bool ISO8601Parser::parse_time_zone_offset_required()
{
    // TimeZoneOffsetRequired :
    //     TimeZoneUTCOffset TimeZoneBracketedAnnotation[opt]
    StateTransaction transaction { *this };
    if (!parse_time_zone_utc_offset())
        return false;
    (void)parse_time_zone_bracketed_annotation();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZoneNameRequired
bool ISO8601Parser::parse_time_zone_name_required()
{
    // TimeZoneNameRequired :
    //     TimeZoneUTCOffset[opt] TimeZoneBracketedAnnotation
    StateTransaction transaction { *this };
    (void)parse_time_zone_utc_offset();
    if (!parse_time_zone_bracketed_annotation())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TimeZone
bool ISO8601Parser::parse_time_zone()
{
    // TimeZone :
    //     TimeZoneUTCOffset TimeZoneBracketedAnnotation[opt]
    //     TimeZoneBracketedAnnotation
    StateTransaction transaction { *this };
    if (parse_time_zone_utc_offset())
        (void)parse_time_zone_bracketed_annotation();
    else if (!parse_time_zone_bracketed_annotation())
        return false;
    transaction.commit();
    return true;
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

// https://tc39.es/proposal-temporal/#prod-TimeHourMinuteBasicFormatNotAmbiguous
bool ISO8601Parser::parse_time_hour_minute_basic_format_not_ambiguous()
{
    // TimeHourMinuteBasicFormatNotAmbiguous :
    //     TimeHourNotValidMonth TimeMinute
    //     TimeHour TimeMinuteNotValidDay
    //     TimeHourNotThirtyOneDayMonth TimeMinuteThirtyOneOnly
    //     TimeHourTwoOnly TimeMinuteThirtyOnly
    {
        StateTransaction transaction { *this };
        if (parse_time_hour_not_valid_month() && parse_time_minute()) {
            transaction.commit();
            return true;
        }
    }
    {
        StateTransaction transaction { *this };
        if (parse_time_hour() && parse_time_minute_not_valid_day()) {
            transaction.commit();
            return true;
        }
    }
    {
        StateTransaction transaction { *this };
        if (parse_time_hour_not_thirty_one_day_month() && parse_time_minute_thirty_one_only()) {
            transaction.commit();
            return true;
        }
    }
    {
        StateTransaction transaction { *this };
        if (parse_time_hour_two_only() && parse_time_minute_thirty_only()) {
            transaction.commit();
            return true;
        }
    }
    return false;
}

// https://tc39.es/proposal-temporal/#prod-TimeSpecWithOptionalTimeZoneNotAmbiguous
bool ISO8601Parser::parse_time_spec_with_optional_time_zone_not_ambiguous()
{
    // TimeSpecWithOptionalTimeZoneNotAmbiguous :
    //     TimeHour TimeZoneNumericUTCOffsetNotAmbiguous[opt] TimeZoneBracketedAnnotation[opt]
    //     TimeHourNotValidMonth TimeZone
    //     TimeHour : TimeMinute TimeZone[opt]
    //     TimeHourMinuteBasicFormatNotAmbiguous TimeZoneBracketedAnnotation[opt]
    //     TimeHour TimeMinute TimeZoneNumericUTCOffsetNotAmbiguousAllowedNegativeHour TimeZoneBracketedAnnotation[opt]
    //     TimeHour : TimeMinute : TimeSecond TimeFraction[opt] TimeZone[opt]
    //     TimeHour TimeMinute TimeSecondNotValidMonth TimeZone[opt]
    //     TimeHour TimeMinute TimeSecond TimeFraction TimeZone[opt]
    // NOTE: Reverse order here because `TimeHour TimeZoneNumericUTCOffsetNotAmbiguous[opt] TimeZoneBracketedAnnotation[opt]` can
    // be a subset of `TimeHourNotValidMonth TimeZone`, so we'd not attempt to parse that but may not exhaust the input string.
    {
        StateTransaction transaction { *this };
        if (parse_time_hour_not_valid_month() && parse_time_zone()) {
            transaction.commit();
            return true;
        }
    }
    {
        StateTransaction transaction { *this };
        if (parse_time_hour()) {
            if (m_state.lexer.consume_specific(':')) {
                if (parse_time_minute()) {
                    if (m_state.lexer.consume_specific(':')) {
                        if (parse_time_second()) {
                            (void)parse_time_fraction();
                            (void)parse_time_zone();
                            transaction.commit();
                            return true;
                        }
                    } else {
                        (void)parse_time_zone();
                        transaction.commit();
                        return true;
                    }
                }
            } else if (parse_time_minute()) {
                if (parse_time_zone_numeric_utc_offset_not_ambiguous_allowed_negative_hour()) {
                    (void)parse_time_zone_bracketed_annotation();
                    transaction.commit();
                    return true;
                }
                {
                    StateTransaction sub_transaction { *this };
                    if (parse_time_second() && parse_time_fraction()) {
                        (void)parse_time_zone();
                        transaction.commit();
                        return true;
                    }
                }
                if (parse_time_second_not_valid_month()) {
                    (void)parse_time_zone();
                    transaction.commit();
                    return true;
                }
            } else {
                (void)parse_time_zone_numeric_utc_offset_not_ambiguous();
                (void)parse_time_zone_bracketed_annotation();
                transaction.commit();
                return true;
            }
        }
    }
    {
        StateTransaction transaction { *this };
        if (parse_time_hour_minute_basic_format_not_ambiguous()) {
            (void)parse_time_zone_bracketed_annotation();
            transaction.commit();
            return true;
        }
    }
    return false;
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

// https://tc39.es/proposal-temporal/#prod-CalendarTime
bool ISO8601Parser::parse_calendar_time()
{
    // CalendarTime :
    //     TimeDesignator TimeSpec TimeZone[opt] Calendar[opt]
    //     TimeSpec TimeZone[opt] Calendar
    //     TimeSpecWithOptionalTimeZoneNotAmbiguous
    {
        StateTransaction transaction { *this };
        if (parse_time_designator() && parse_time_spec()) {
            (void)parse_time_zone();
            (void)parse_calendar();
            transaction.commit();
            return true;
        }
    }
    {
        StateTransaction transaction { *this };
        if (parse_time_spec()) {
            (void)parse_time_zone();
            if (parse_calendar()) {
                transaction.commit();
                return true;
            }
        }
    }
    return parse_time_spec_with_optional_time_zone_not_ambiguous();
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

// https://tc39.es/proposal-temporal/#prod-CalendarDateTimeTimeRequired
bool ISO8601Parser::parse_calendar_date_time_time_required()
{
    // CalendarDateTimeTimeRequired :
    //     Date TimeSpecSeparator TimeZone[opt] Calendar[opt]
    StateTransaction transaction { *this };
    if (!parse_date())
        return false;
    if (!parse_time_spec_separator())
        return false;
    (void)parse_time_zone();
    (void)parse_calendar();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationWholeSeconds
bool ISO8601Parser::parse_duration_whole_seconds()
{
    // DurationWholeSeconds :
    //     DecimalDigits[~Sep]
    StateTransaction transaction { *this };
    if (!parse_decimal_digits())
        return false;
    m_state.parse_result.duration_whole_seconds = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationSecondsFraction
bool ISO8601Parser::parse_duration_seconds_fraction()
{
    // DurationSecondsFraction :
    //     TimeFraction
    StateTransaction transaction { *this };
    if (!parse_time_fraction())
        return false;
    m_state.parse_result.duration_seconds_fraction = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationSecondsPart
bool ISO8601Parser::parse_duration_seconds_part()
{
    // DurationSecondsPart :
    //     DurationWholeSeconds DurationSecondsFraction[opt] SecondsDesignator
    StateTransaction transaction { *this };
    if (!parse_duration_whole_seconds())
        return false;
    (void)parse_duration_seconds_fraction();
    if (!parse_seconds_designator())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationWholeMinutes
bool ISO8601Parser::parse_duration_whole_minutes()
{
    // DurationWholeMinutes :
    //     DecimalDigits[~Sep]
    StateTransaction transaction { *this };
    if (!parse_decimal_digits())
        return false;
    m_state.parse_result.duration_whole_minutes = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationMinutesFraction
bool ISO8601Parser::parse_duration_minutes_fraction()
{
    // DurationMinutesFraction :
    //     TimeFraction
    StateTransaction transaction { *this };
    if (!parse_time_fraction())
        return false;
    m_state.parse_result.duration_minutes_fraction = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationMinutesPart
bool ISO8601Parser::parse_duration_minutes_part()
{
    // DurationMinutesPart :
    //     DurationWholeMinutes DurationMinutesFraction[opt] MinutesDesignator DurationSecondsPart[opt]
    StateTransaction transaction { *this };
    if (!parse_duration_whole_minutes())
        return false;
    (void)parse_duration_minutes_fraction();
    if (!parse_minutes_designator())
        return false;
    (void)parse_duration_seconds_part();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationWholeHours
bool ISO8601Parser::parse_duration_whole_hours()
{
    // DurationWholeHours :
    //     DecimalDigits[~Sep]
    StateTransaction transaction { *this };
    if (!parse_decimal_digits())
        return false;
    m_state.parse_result.duration_whole_hours = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationHoursFraction
bool ISO8601Parser::parse_duration_hours_fraction()
{
    // DurationHoursFraction :
    //     TimeFraction
    StateTransaction transaction { *this };
    if (!parse_time_fraction())
        return false;
    m_state.parse_result.duration_hours_fraction = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationHoursPart
bool ISO8601Parser::parse_duration_hours_part()
{
    // DurationHoursPart :
    //     DurationWholeHours DurationHoursFraction[opt] HoursDesignator DurationMinutesPart
    //     DurationWholeHours DurationHoursFraction[opt] HoursDesignator DurationSecondsPart[opt]
    StateTransaction transaction { *this };
    if (!parse_duration_whole_hours())
        return false;
    (void)parse_duration_hours_fraction();
    if (!parse_hours_designator())
        return false;
    (void)(parse_duration_minutes_part()
        || parse_duration_seconds_part());
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationTime
bool ISO8601Parser::parse_duration_time()
{
    // DurationTime :
    //     TimeDesignator DurationHoursPart
    //     TimeDesignator DurationMinutesPart
    //     TimeDesignator DurationSecondsPart
    StateTransaction transaction { *this };
    if (!parse_time_designator())
        return false;
    auto success = parse_duration_hours_part()
        || parse_duration_minutes_part()
        || parse_duration_seconds_part();
    if (!success)
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationDays
bool ISO8601Parser::parse_duration_days()
{
    // DurationDays :
    //     DecimalDigits[~Sep]
    StateTransaction transaction { *this };
    if (!parse_decimal_digits())
        return false;
    m_state.parse_result.duration_days = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationDaysPart
bool ISO8601Parser::parse_duration_days_part()
{
    // DurationDaysPart :
    //     DurationDays DaysDesignator
    StateTransaction transaction { *this };
    if (!parse_duration_days())
        return false;
    if (!parse_days_designator())
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationWeeks
bool ISO8601Parser::parse_duration_weeks()
{
    // DurationWeeks :
    //     DecimalDigits[~Sep]
    StateTransaction transaction { *this };
    if (!parse_decimal_digits())
        return false;
    m_state.parse_result.duration_weeks = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationWeeksPart
bool ISO8601Parser::parse_duration_weeks_part()
{
    // DurationWeeksPart :
    //     DurationWeeks WeeksDesignator DurationDaysPart[opt]
    StateTransaction transaction { *this };
    if (!parse_duration_weeks())
        return false;
    if (!parse_weeks_designator())
        return false;
    (void)parse_duration_days_part();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationMonths
bool ISO8601Parser::parse_duration_months()
{
    // DurationMonths :
    //     DecimalDigits[~Sep]
    StateTransaction transaction { *this };
    if (!parse_decimal_digits())
        return false;
    m_state.parse_result.duration_months = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationMonthsPart
bool ISO8601Parser::parse_duration_months_part()
{
    // DurationMonthsPart :
    //     DurationMonths MonthsDesignator DurationWeeksPart
    //     DurationMonths MonthsDesignator DurationDaysPart[opt]
    StateTransaction transaction { *this };
    if (!parse_duration_months())
        return false;
    if (!parse_months_designator())
        return false;
    (void)(parse_duration_weeks_part()
        || parse_duration_days_part());
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationYears
bool ISO8601Parser::parse_duration_years()
{
    // DurationYears :
    //     DecimalDigits[~Sep]
    StateTransaction transaction { *this };
    if (!parse_decimal_digits())
        return false;
    m_state.parse_result.duration_years = transaction.parsed_string_view();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationYearsPart
bool ISO8601Parser::parse_duration_years_part()
{
    // DurationYearsPart :
    //     DurationYears YearsDesignator DurationMonthsPart
    //     DurationYears YearsDesignator DurationWeeksPart
    //     DurationYears YearsDesignator DurationDaysPart[opt]
    StateTransaction transaction { *this };
    if (!parse_duration_years())
        return false;
    if (!parse_years_designator())
        return false;
    (void)(parse_duration_months_part()
        || parse_duration_weeks_part()
        || parse_duration_days_part());
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-DurationDate
bool ISO8601Parser::parse_duration_date()
{
    // DurationDate :
    //     DurationYearsPart DurationTime[opt]
    //     DurationMonthsPart DurationTime[opt]
    //     DurationWeeksPart DurationTime[opt]
    //     DurationDaysPart DurationTime[opt]
    auto success = parse_duration_years_part()
        || parse_duration_months_part()
        || parse_duration_weeks_part()
        || parse_duration_days_part();
    if (!success)
        return false;
    (void)parse_duration_time();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-Duration
bool ISO8601Parser::parse_duration()
{
    // Duration :
    //     Sign[opt] DurationDesignator DurationDate
    //     Sign[opt] DurationDesignator DurationTime
    StateTransaction transaction { *this };
    (void)parse_sign();
    if (!parse_duration_designator())
        return false;
    auto success = parse_duration_date()
        || parse_duration_time();
    if (!success)
        return false;
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TemporalInstantString
bool ISO8601Parser::parse_temporal_instant_string()
{
    // TemporalInstantString :
    //     Date TimeSpecSeparator[opt] TimeZoneOffsetRequired
    StateTransaction transaction { *this };
    if (!parse_date())
        return false;
    (void)parse_time_spec_separator();
    if (!parse_time_zone_offset_required())
        return false;
    transaction.commit();
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

// https://tc39.es/proposal-temporal/#prod-TemporalDurationString
bool ISO8601Parser::parse_temporal_duration_string()
{
    // TemporalDurationString :
    //     Duration
    return parse_duration();
}

// https://tc39.es/proposal-temporal/#prod-TemporalMonthDayString
bool ISO8601Parser::parse_temporal_month_day_string()
{
    // TemporalMonthDayString :
    //     DateSpecMonthDay
    //     CalendarDateTime
    // NOTE: Reverse order here because `DateSpecMonthDay` can be a subset of `CalendarDateTime`,
    // so we'd not attempt to parse that but may not exhaust the input string.
    return parse_calendar_date_time()
        || parse_date_spec_month_day();
}

// https://tc39.es/proposal-temporal/#prod-TemporalTimeString
bool ISO8601Parser::parse_temporal_time_string()
{
    // TemporalTimeString :
    //     CalendarTime
    //     CalendarDateTimeTimeRequired
    // NOTE: Reverse order here because `Time` can be a subset of `CalendarDateTimeTimeRequired`,
    // so we'd not attempt to parse that but may not exhaust the input string.
    return parse_calendar_date_time_time_required()
        || parse_calendar_time();
}

// https://tc39.es/proposal-temporal/#prod-TemporalTimeZoneIdentifier
bool ISO8601Parser::parse_temporal_time_zone_identifier()
{
    // TemporalTimeZoneIdentifier :
    //     TimeZoneNumericUTCOffset
    //     TimeZoneIANAName
    return parse_time_zone_numeric_utc_offset()
        || parse_time_zone_iana_name();
}

// https://tc39.es/proposal-temporal/#prod-TemporalTimeZoneString
bool ISO8601Parser::parse_temporal_time_zone_string()
{
    // TemporalTimeZoneString :
    //     TemporalTimeZoneIdentifier
    //     Date TimeSpecSeparator[opt] TimeZone Calendar[opt]
    StateTransaction transaction { *this };
    if (!parse_temporal_time_zone_identifier()) {
        if (!parse_date())
            return false;
        (void)parse_time_spec_separator();
        if (!parse_time_zone())
            return false;
        (void)parse_calendar();
    }
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TemporalYearMonthString
bool ISO8601Parser::parse_temporal_year_month_string()
{
    // TemporalYearMonthString :
    //     DateSpecYearMonth
    //     CalendarDateTime
    // NOTE: Reverse order here because `DateSpecYearMonth` can be a subset of `CalendarDateTime`,
    // so we'd not attempt to parse that but may not exhaust the input string.
    return parse_calendar_date_time()
        || parse_date_spec_year_month();
}

// https://tc39.es/proposal-temporal/#prod-TemporalZonedDateTimeString
bool ISO8601Parser::parse_temporal_zoned_date_time_string()
{
    // TemporalZonedDateTimeString :
    //     Date TimeSpecSeparator[opt] TimeZoneNameRequired Calendar[opt]
    StateTransaction transaction { *this };
    if (!parse_date())
        return false;
    (void)parse_time_spec_separator();
    if (!parse_time_zone_name_required())
        return false;
    (void)parse_calendar();
    transaction.commit();
    return true;
}

// https://tc39.es/proposal-temporal/#prod-TemporalCalendarString
bool ISO8601Parser::parse_temporal_calendar_string()
{
    // TemporalCalendarString :
    //     CalendarName
    //     TemporalInstantString
    //     CalendarDateTime
    //     CalendarTime
    //     DateSpecYearMonth
    //     DateSpecMonthDay
    return parse_calendar_name()
        || parse_temporal_instant_string()
        || parse_calendar_date_time()
        || parse_date_spec_year_month()
        || parse_date_spec_month_day()
        || parse_calendar_time();
}

// https://tc39.es/proposal-temporal/#prod-TemporalRelativeToString
bool ISO8601Parser::parse_temporal_relative_to_string()
{
    // TemporalRelativeToString :
    //     TemporalDateTimeString
    return parse_temporal_date_time_string();
}

}

#define JS_ENUMERATE_ISO8601_PRODUCTION_PARSERS                                        \
    __JS_ENUMERATE(TemporalInstantString, parse_temporal_instant_string)               \
    __JS_ENUMERATE(TemporalDateString, parse_temporal_date_string)                     \
    __JS_ENUMERATE(TemporalDateTimeString, parse_temporal_date_time_string)            \
    __JS_ENUMERATE(TemporalDurationString, parse_temporal_duration_string)             \
    __JS_ENUMERATE(TemporalMonthDayString, parse_temporal_month_day_string)            \
    __JS_ENUMERATE(TemporalTimeString, parse_temporal_time_string)                     \
    __JS_ENUMERATE(TemporalTimeZoneString, parse_temporal_time_zone_string)            \
    __JS_ENUMERATE(TemporalYearMonthString, parse_temporal_year_month_string)          \
    __JS_ENUMERATE(TemporalZonedDateTimeString, parse_temporal_zoned_date_time_string) \
    __JS_ENUMERATE(TemporalCalendarString, parse_temporal_calendar_string)             \
    __JS_ENUMERATE(TemporalRelativeToString, parse_temporal_relative_to_string)

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
