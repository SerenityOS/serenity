/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS::Temporal {

enum class OptionType {
    Boolean,
    String,
    Number
};

struct ISODateTime {
    i32 year;
    i32 month;
    i32 day;
    i32 hour;
    i32 minute;
    i32 second;
    i32 millisecond;
    i32 microsecond;
    i32 nanosecond;
    Optional<String> calendar;
};

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

struct TemporalTimeZone {
    bool z;
    Optional<String> offset;
    Optional<String> name;
};

Object* get_options_object(GlobalObject&, Value options);
Value get_option(GlobalObject&, Object& options, String const& property, Vector<OptionType> const& types, Vector<StringView> const& values, Value fallback);
String to_temporal_rounding_mode(GlobalObject&, Object& normalized_options, String const& fallback);
u64 to_temporal_rounding_increment(GlobalObject&, Object& normalized_options, Optional<double> dividend, bool inclusive);
Optional<String> to_smallest_temporal_unit(GlobalObject&, Object& normalized_options, Vector<StringView> const& disallowed_units, Optional<String> fallback);
BigInt* round_number_to_increment(GlobalObject&, BigInt const&, u64 increment, String const& rounding_mode);
Optional<ISODateTime> parse_iso_date_time(GlobalObject&, String const& iso_string);
Optional<TemporalInstant> parse_temporal_instant_string(GlobalObject&, String const& iso_string);
Optional<TemporalTimeZone> parse_temporal_time_zone_string(GlobalObject&, String const& iso_string);

}
