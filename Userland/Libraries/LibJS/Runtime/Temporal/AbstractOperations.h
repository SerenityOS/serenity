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

Object* get_options_object(GlobalObject&, Value options);
enum class OptionType {
    Boolean,
    String,
    Number
};
Value get_option(GlobalObject&, Object& options, String const& property, Vector<OptionType> const& types, Vector<StringView> const& values, Value fallback);

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
Optional<ISODateTime> parse_iso_date_time(GlobalObject&, String const& iso_string);
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
Optional<TemporalInstant> parse_temporal_instant_string(GlobalObject&, String const& iso_string);
struct TemporalTimeZone {
    bool z;
    Optional<String> offset;
    Optional<String> name;
};
Optional<TemporalTimeZone> parse_temporal_time_zone_string(GlobalObject&, String const& iso_string);

}
