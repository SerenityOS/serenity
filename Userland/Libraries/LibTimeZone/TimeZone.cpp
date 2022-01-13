/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTimeZone/TimeZone.h>
#include <time.h>

namespace TimeZone {

// NOTE: Without ENABLE_TIME_ZONE_DATA LibTimeZone operates in a UTC-only mode and only recognizes
//       the 'UTC' time zone, which is slightly more useful than a bunch of dummy functions that
//       can't do anything. When we build with time zone data, these weakly linked functions are
//       replaced with their proper counterparts.

#if !ENABLE_TIME_ZONE_DATA
enum class TimeZone : u16 {
    UTC,
};
#endif

StringView current_time_zone()
{
    static bool initialized_time_zone = false;
    if (!initialized_time_zone) {
        initialized_time_zone = true;
        tzset();
    }

    return tzname[0];
}

Optional<TimeZone> __attribute__((weak)) time_zone_from_string([[maybe_unused]] StringView time_zone)
{
#if !ENABLE_TIME_ZONE_DATA
    if (time_zone.equals_ignoring_case("UTC"sv))
        return TimeZone::UTC;
#endif
    return {};
}

StringView __attribute__((weak)) time_zone_to_string([[maybe_unused]] TimeZone time_zone)
{
#if !ENABLE_TIME_ZONE_DATA
    VERIFY(time_zone == TimeZone::UTC);
    return "UTC"sv;
#else
    return {};
#endif
}

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

Optional<i64> __attribute__((weak)) get_time_zone_offset([[maybe_unused]] TimeZone time_zone, AK::Time)
{
#if !ENABLE_TIME_ZONE_DATA
    VERIFY(time_zone == TimeZone::UTC);
    return 0;
#else
    return {};
#endif
}

Optional<i64> get_time_zone_offset(StringView time_zone, AK::Time time)
{
    if (auto maybe_time_zone = time_zone_from_string(time_zone); maybe_time_zone.has_value())
        return get_time_zone_offset(*maybe_time_zone, time);
    return {};
}

}
