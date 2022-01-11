/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTimeZone/TimeZone.h>

namespace TimeZone {

Optional<TimeZone> __attribute__((weak)) time_zone_from_string(StringView) { return {}; }
StringView __attribute__((weak)) time_zone_to_string(TimeZone) { return {}; }

Optional<StringView> canonicalize_time_zone(StringView time_zone)
{
    auto maybe_time_zone = time_zone_from_string(time_zone);
    if (!maybe_time_zone.has_value())
        return {};

    auto canonical_time_zone = time_zone_to_string(*maybe_time_zone);
    if (canonical_time_zone.is_one_of("Etc/UTC"sv, "Etc/GMT"sv))
        return "UTC"sv;

    return canonical_time_zone;
}

Optional<i64> __attribute__((weak)) get_time_zone_offset(TimeZone, AK::Time) { return {}; }

Optional<i64> get_time_zone_offset(StringView time_zone, AK::Time time)
{
    if (auto maybe_time_zone = time_zone_from_string(time_zone); maybe_time_zone.has_value())
        return get_time_zone_offset(*maybe_time_zone, time);
    return {};
}

}
