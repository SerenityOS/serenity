/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
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
    Optional<String> offset;
    Optional<String> name;
};

struct TemporalYearMonth {
    i32 year;
    u8 month;
    u8 day;
    Optional<String> calendar = {};
};

struct SecondsStringPrecision {
    Variant<StringView, u8> precision;
    String unit;
    u32 increment;
};

ThrowCompletionOr<MarkedValueList> iterable_to_list_of_type(GlobalObject&, Value items, Vector<OptionType> const& element_types);
ThrowCompletionOr<Object*> get_options_object(GlobalObject&, Value options);
ThrowCompletionOr<Value> get_option(GlobalObject&, Object const& options, PropertyName const& property, Vector<OptionType> const& types, Vector<StringView> const& values, Value fallback);
template<typename NumberType>
ThrowCompletionOr<Variant<String, NumberType>> get_string_or_number_option(GlobalObject&, Object const& options, PropertyName const& property, Vector<StringView> const& string_values, NumberType minimum, NumberType maximum, Value fallback);
ThrowCompletionOr<String> to_temporal_overflow(GlobalObject&, Object const& normalized_options);
ThrowCompletionOr<String> to_temporal_rounding_mode(GlobalObject&, Object const& normalized_options, String const& fallback);
ThrowCompletionOr<String> to_show_calendar_option(GlobalObject&, Object const& normalized_options);
ThrowCompletionOr<u64> to_temporal_rounding_increment(GlobalObject&, Object const& normalized_options, Optional<double> dividend, bool inclusive);
ThrowCompletionOr<SecondsStringPrecision> to_seconds_string_precision(GlobalObject&, Object const& normalized_options);
ThrowCompletionOr<String> to_largest_temporal_unit(GlobalObject&, Object const& normalized_options, Vector<StringView> const& disallowed_units, String const& fallback, Optional<String> auto_value);
ThrowCompletionOr<Optional<String>> to_smallest_temporal_unit(GlobalObject&, Object const& normalized_options, Vector<StringView> const& disallowed_units, Optional<String> fallback);
void validate_temporal_unit_range(GlobalObject&, StringView largest_unit, StringView smallest_unit);
String larger_of_two_temporal_units(StringView, StringView);
Optional<u16> maximum_temporal_duration_rounding_increment(StringView unit);
void reject_temporal_calendar_type(GlobalObject&, Object&);
String format_seconds_string_part(u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, Variant<StringView, u8> const& precision);
double constrain_to_range(double x, double minimum, double maximum);
BigInt* round_number_to_increment(GlobalObject&, BigInt const&, u64 increment, StringView rounding_mode);
Optional<ISODateTime> parse_iso_date_time(GlobalObject&, String const& iso_string);
Optional<TemporalInstant> parse_temporal_instant_string(GlobalObject&, String const& iso_string);
Optional<String> parse_temporal_calendar_string(GlobalObject&, String const& iso_string);
Optional<TemporalDate> parse_temporal_date_string(GlobalObject&, String const& iso_string);
Optional<ISODateTime> parse_temporal_date_time_string(GlobalObject&, String const& iso_string);
Optional<TemporalDuration> parse_temporal_duration_string(GlobalObject&, String const& iso_string);
Optional<TemporalTime> parse_temporal_time_string(GlobalObject&, String const& iso_string);
Optional<TemporalTimeZone> parse_temporal_time_zone_string(GlobalObject&, String const& iso_string);
Optional<TemporalYearMonth> parse_temporal_year_month_string(GlobalObject&, String const& iso_string);
double to_positive_integer(GlobalObject&, Value argument);
Object* prepare_temporal_fields(GlobalObject&, Object const& fields, Vector<String> const& field_names, Vector<StringView> const& required_fields);

// 13.46 ToIntegerThrowOnInfinity ( argument ), https://tc39.es/proposal-temporal/#sec-temporal-tointegerthrowoninfinity
template<typename... Args>
ThrowCompletionOr<double> to_integer_throw_on_infinity(GlobalObject& global_object, Value argument, ErrorType error_type, Args... args)
{
    auto& vm = global_object.vm();

    // 1. Let integer be ? ToIntegerOrInfinity(argument).
    auto integer = argument.to_integer_or_infinity(global_object);
    if (auto* exception = vm.exception())
        return throw_completion(exception->value());

    // 2. If integer is −∞ or +∞ , then
    if (Value(integer).is_infinity()) {
        // a. Throw a RangeError exception.
        return vm.template throw_completion<RangeError>(global_object, error_type, args...);
    }

    // 3. Return integer.
    return integer;
}

}
