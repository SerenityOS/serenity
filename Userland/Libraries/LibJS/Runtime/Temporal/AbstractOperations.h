/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>
#include <LibJS/Forward.h>
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

// FIXME: Use more narrow types for most of these (u8/u16)
struct TemporalInstant {
    i32 year;
    i32 month;
    i32 day;
    i32 hour;
    i32 minute;
    i32 second;
    i32 millisecond;
    i32 microsecond;
    i32 nanosecond;
    Optional<String> time_zone_offset;
};

// FIXME: Use more narrow type for month/day (u8)
struct TemporalDate {
    i32 year;
    i32 month;
    i32 day;
    Optional<String> calendar;
};

struct TemporalTimeZone {
    bool z;
    Optional<String> offset;
    Optional<String> name;
};

MarkedValueList iterable_to_list_of_type(GlobalObject&, Value items, Vector<OptionType> const& element_types);
Object* get_options_object(GlobalObject&, Value options);
Value get_option(GlobalObject&, Object& options, String const& property, Vector<OptionType> const& types, Vector<StringView> const& values, Value fallback);
Optional<String> to_temporal_overflow(GlobalObject&, Object& normalized_options);
Optional<String> to_temporal_rounding_mode(GlobalObject&, Object& normalized_options, String const& fallback);
u64 to_temporal_rounding_increment(GlobalObject&, Object& normalized_options, Optional<double> dividend, bool inclusive);
Optional<String> to_smallest_temporal_unit(GlobalObject&, Object& normalized_options, Vector<StringView> const& disallowed_units, Optional<String> fallback);
double constrain_to_range(double x, double minimum, double maximum);
BigInt* round_number_to_increment(GlobalObject&, BigInt const&, u64 increment, String const& rounding_mode);
Optional<ISODateTime> parse_iso_date_time(GlobalObject&, String const& iso_string);
Optional<TemporalInstant> parse_temporal_instant_string(GlobalObject&, String const& iso_string);
Optional<String> parse_temporal_calendar_string(GlobalObject&, String const& iso_string);
Optional<TemporalDate> parse_temporal_date_string(GlobalObject&, String const& iso_string);
Optional<TemporalDuration> parse_temporal_duration_string(GlobalObject&, String const& iso_string);
Optional<TemporalTimeZone> parse_temporal_time_zone_string(GlobalObject&, String const& iso_string);
double to_positive_integer_or_infinity(GlobalObject&, Value argument);
Object* prepare_temporal_fields(GlobalObject&, Object& fields, Vector<String> field_names, Vector<StringView> required_fields);

}
