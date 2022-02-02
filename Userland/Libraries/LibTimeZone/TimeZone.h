/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Error.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <LibTimeZone/Forward.h>

namespace TimeZone {

enum class InDST {
    No,
    Yes,
};

struct Offset {
    i64 seconds { 0 };
    InDST in_dst { InDST::No };
};

struct NamedOffset : public Offset {
    String name;
};

struct Coordinate {
    constexpr float decimal_coordinate() const
    {
        return static_cast<float>(degrees) + (static_cast<float>(minutes) / 60.0f) + (static_cast<float>(seconds) / 3'600.0f);
    }

    i16 degrees { 0 };
    u8 minutes { 0 };
    u8 seconds { 0 };
};

struct Location {
    Coordinate latitude;
    Coordinate longitude;
};

StringView system_time_zone();
StringView current_time_zone();
ErrorOr<void> change_time_zone(StringView time_zone);
Span<StringView const> all_time_zones();

Optional<TimeZone> time_zone_from_string(StringView time_zone);
StringView time_zone_to_string(TimeZone time_zone);
Optional<StringView> canonicalize_time_zone(StringView time_zone);

Optional<DaylightSavingsRule> daylight_savings_rule_from_string(StringView daylight_savings_rule);
StringView daylight_savings_rule_to_string(DaylightSavingsRule daylight_savings_rule);

Optional<Offset> get_time_zone_offset(TimeZone time_zone, AK::Time time);
Optional<Offset> get_time_zone_offset(StringView time_zone, AK::Time time);

Optional<Array<NamedOffset, 2>> get_named_time_zone_offsets(TimeZone time_zone, AK::Time time);
Optional<Array<NamedOffset, 2>> get_named_time_zone_offsets(StringView time_zone, AK::Time time);

Optional<Location> get_time_zone_location(TimeZone time_zone);
Optional<Location> get_time_zone_location(StringView time_zone);

}
