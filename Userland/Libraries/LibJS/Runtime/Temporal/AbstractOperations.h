/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/ISO8601.h>

namespace JS::Temporal {

enum class OptionType {
    Boolean,
    String,
    Number
};

struct ISODateTime {
    i32 year;
    u8 month;
    u8 day;
    u8 hour;
    u8 minute;
    u8 second;
    u16 millisecond;
    u16 microsecond;
    u16 nanosecond;
    Optional<String> calendar = {};
};

struct TemporalInstant {
    i32 year;
    u8 month;
    u8 day;
    u8 hour;
    u8 minute;
    u8 second;
    u16 millisecond;
    u16 microsecond;
    u16 nanosecond;
    Optional<String> time_zone_offset;
};

struct TemporalDate {
    i32 year;
    u8 month;
    u8 day;
    Optional<String> calendar;
};

struct TemporalTime {
    u8 hour;
    u8 minute;
    u8 second;
    u16 millisecond;
    u16 microsecond;
    u16 nanosecond;
    Optional<String> calendar = {};
};

struct TemporalTimeZone {
    bool z;
    Optional<String> offset_string;
    Optional<String> name;
};

struct TemporalYearMonth {
    i32 year;
    u8 month;
    u8 day;
    Optional<String> calendar = {};
};

struct TemporalMonthDay {
    Optional<i32> year;
    u8 month;
    u8 day;
    Optional<String> calendar = {};
};

struct TemporalZonedDateTime {
    ISODateTime date_time;
    TemporalTimeZone time_zone;
};

struct SecondsStringPrecision {
    Variant<StringView, u8> precision;
    String unit;
    u32 increment;
};

ThrowCompletionOr<MarkedVector<Value>> iterable_to_list_of_type(GlobalObject&, Value items, Vector<OptionType> const& element_types);
ThrowCompletionOr<Object*> get_options_object(GlobalObject&, Value options);
ThrowCompletionOr<Value> get_option(GlobalObject&, Object const& options, PropertyKey const& property, Vector<OptionType> const& types, Vector<StringView> const& values, Value fallback);
template<typename NumberType>
ThrowCompletionOr<Variant<String, NumberType>> get_string_or_number_option(GlobalObject&, Object const& options, PropertyKey const& property, Vector<StringView> const& string_values, NumberType minimum, NumberType maximum, Value fallback);
ThrowCompletionOr<String> to_temporal_overflow(GlobalObject&, Object const& normalized_options);
ThrowCompletionOr<String> to_temporal_disambiguation(GlobalObject&, Object const& normalized_options);
ThrowCompletionOr<String> to_temporal_rounding_mode(GlobalObject&, Object const& normalized_options, String const& fallback);
StringView negate_temporal_rounding_mode(String const& rounding_mode);
ThrowCompletionOr<String> to_temporal_offset(GlobalObject&, Object const& normalized_options, String const& fallback);
ThrowCompletionOr<String> to_show_calendar_option(GlobalObject&, Object const& normalized_options);
ThrowCompletionOr<String> to_show_time_zone_name_option(GlobalObject&, Object const& normalized_options);
ThrowCompletionOr<String> to_show_offset_option(GlobalObject&, Object const& normalized_options);
ThrowCompletionOr<u64> to_temporal_rounding_increment(GlobalObject&, Object const& normalized_options, Optional<double> dividend, bool inclusive);
ThrowCompletionOr<u64> to_temporal_date_time_rounding_increment(GlobalObject&, Object const& normalized_options, StringView smallest_unit);
ThrowCompletionOr<SecondsStringPrecision> to_seconds_string_precision(GlobalObject&, Object const& normalized_options);
ThrowCompletionOr<Optional<String>> to_largest_temporal_unit(GlobalObject&, Object const& normalized_options, Vector<StringView> const& disallowed_units, Optional<String> fallback, Optional<String> auto_value = {});
ThrowCompletionOr<Optional<String>> to_smallest_temporal_unit(GlobalObject&, Object const& normalized_options, Vector<StringView> const& disallowed_units, Optional<String> fallback);
ThrowCompletionOr<String> to_temporal_duration_total_unit(GlobalObject& global_object, Object const& normalized_options);
ThrowCompletionOr<Value> to_relative_temporal_object(GlobalObject&, Object const& options);
ThrowCompletionOr<void> validate_temporal_unit_range(GlobalObject&, StringView largest_unit, StringView smallest_unit);
StringView larger_of_two_temporal_units(StringView, StringView);
ThrowCompletionOr<Object*> merge_largest_unit_option(GlobalObject&, Object& options, String largest_unit);
Optional<u16> maximum_temporal_duration_rounding_increment(StringView unit);
ThrowCompletionOr<void> reject_object_with_calendar_or_time_zone(GlobalObject&, Object&);
String format_seconds_string_part(u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, Variant<StringView, u8> const& precision);
double sign(double);
double sign(Crypto::SignedBigInteger const&);
double constrain_to_range(double x, double minimum, double maximum);
i64 round_number_to_increment(double, u64 increment, StringView rounding_mode);
BigInt* round_number_to_increment(GlobalObject&, BigInt const&, u64 increment, StringView rounding_mode);
ThrowCompletionOr<ISODateTime> parse_iso_date_time(GlobalObject&, ParseResult const& parse_result);
ThrowCompletionOr<TemporalInstant> parse_temporal_instant_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<TemporalZonedDateTime> parse_temporal_zoned_date_time_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<String> parse_temporal_calendar_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<TemporalDate> parse_temporal_date_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<ISODateTime> parse_temporal_date_time_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<TemporalDuration> parse_temporal_duration_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<TemporalMonthDay> parse_temporal_month_day_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<TemporalZonedDateTime> parse_temporal_relative_to_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<TemporalTime> parse_temporal_time_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<TemporalTimeZone> parse_temporal_time_zone_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<TemporalYearMonth> parse_temporal_year_month_string(GlobalObject&, String const& iso_string);
ThrowCompletionOr<double> to_positive_integer(GlobalObject&, Value argument);
ThrowCompletionOr<Object*> prepare_temporal_fields(GlobalObject&, Object const& fields, Vector<String> const& field_names, Vector<StringView> const& required_fields);
ThrowCompletionOr<Object*> prepare_partial_temporal_fields(GlobalObject&, Object const& fields, Vector<String> const& field_names);

// 13.46 ToIntegerThrowOnInfinity ( argument ), https://tc39.es/proposal-temporal/#sec-temporal-tointegerthrowoninfinity
template<typename... Args>
ThrowCompletionOr<double> to_integer_throw_on_infinity(GlobalObject& global_object, Value argument, ErrorType error_type, Args... args)
{
    auto& vm = global_object.vm();

    // 1. Let integer be ? ToIntegerOrInfinity(argument).
    auto integer = TRY(argument.to_integer_or_infinity(global_object));

    // 2. If integer is −∞ or +∞ , then
    if (Value(integer).is_infinity()) {
        // a. Throw a RangeError exception.
        return vm.template throw_completion<RangeError>(global_object, error_type, args...);
    }

    // 3. Return integer.
    return integer;
}

// 13.47 ToIntegerWithoutRounding ( argument ), https://tc39.es/proposal-temporal/#sec-temporal-tointegerwithoutrounding
template<typename... Args>
ThrowCompletionOr<double> to_integer_without_rounding(GlobalObject& global_object, Value argument, ErrorType error_type, Args... args)
{
    auto& vm = global_object.vm();

    // 1. Let number be ? ToNumber(argument).
    auto number = TRY(argument.to_number(global_object));

    // 2. If number is NaN, +0𝔽, or −0𝔽 return 0.
    if (number.is_nan() || number.is_positive_zero() || number.is_negative_zero())
        return 0;

    // 3. If ! IsIntegralNumber(number) is false, throw a RangeError exception.
    if (!number.is_integral_number())
        return vm.template throw_completion<RangeError>(global_object, error_type, args...);

    // 4. Return ℝ(number).
    return number.as_double();
}

}
