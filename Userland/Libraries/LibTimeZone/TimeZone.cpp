/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibTimeZone/TimeZone.h>
#include <stdio.h>
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

class TimeZoneFile {
public:
    TimeZoneFile(char const* mode)
        : m_file(fopen("/etc/timezone", mode))
    {
        if (m_file)
            flockfile(m_file);
    }

    ~TimeZoneFile()
    {
        if (m_file) {
            funlockfile(m_file);
            fclose(m_file);
        }
    }

    ErrorOr<String> read_time_zone()
    {
        if (!m_file)
            return Error::from_string_literal("Could not open /etc/timezone");

        Array<u8, 128> buffer;
        size_t bytes = fread(buffer.data(), 1, buffer.size(), m_file);

        if (bytes == 0)
            return Error::from_string_literal("Could not read time zone from /etc/timezone");

        return String(buffer.span().slice(0, bytes)).trim_whitespace();
    }

    ErrorOr<void> write_time_zone(StringView time_zone)
    {
        if (!m_file)
            return Error::from_string_literal("Could not open /etc/timezone");

        auto bytes = fwrite(time_zone.characters_without_null_termination(), 1, time_zone.length(), m_file);
        if (bytes != time_zone.length())
            return Error::from_string_literal("Could not write new time zone to /etc/timezone");

        return {};
    }

private:
    FILE* m_file { nullptr };
};

StringView system_time_zone()
{
    TimeZoneFile time_zone_file("r");

    // FIXME: Propagate the error to existing callers.
    if (auto time_zone = time_zone_file.read_time_zone(); !time_zone.is_error())
        return canonicalize_time_zone(time_zone.value()).value_or("UTC"sv);

    return "UTC"sv;
}

StringView current_time_zone()
{
    return canonicalize_time_zone(tzname[0]).value_or("UTC"sv);
}

ErrorOr<void> change_time_zone([[maybe_unused]] StringView time_zone)
{
#ifdef __serenity__
    TimeZoneFile time_zone_file("w");

    if (auto new_time_zone = canonicalize_time_zone(time_zone); new_time_zone.has_value())
        return time_zone_file.write_time_zone(*new_time_zone);

    return Error::from_string_literal("Provided time zone is not supported");
#else
    // Do not even attempt to change the time zone of someone's host machine.
    return {};
#endif
}

Span<StringView const> __attribute__((weak)) all_time_zones()
{
#if !ENABLE_TIME_ZONE_DATA
    static constexpr auto utc = Array { "UTC"sv };
    return utc;
#else
    return {};
#endif
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

Optional<DaylightSavingsRule> __attribute__((weak)) daylight_savings_rule_from_string(StringView) { return {}; }
StringView __attribute__((weak)) daylight_savings_rule_to_string(DaylightSavingsRule) { return {}; }

Optional<Offset> __attribute__((weak)) get_time_zone_offset([[maybe_unused]] TimeZone time_zone, AK::Time)
{
#if !ENABLE_TIME_ZONE_DATA
    VERIFY(time_zone == TimeZone::UTC);
    return Offset {};
#else
    return {};
#endif
}

Optional<Offset> get_time_zone_offset(StringView time_zone, AK::Time time)
{
    if (auto maybe_time_zone = time_zone_from_string(time_zone); maybe_time_zone.has_value())
        return get_time_zone_offset(*maybe_time_zone, time);
    return {};
}

Optional<Array<NamedOffset, 2>> __attribute__((weak)) get_named_time_zone_offsets([[maybe_unused]] TimeZone time_zone, AK::Time)
{
#if !ENABLE_TIME_ZONE_DATA
    VERIFY(time_zone == TimeZone::UTC);

    NamedOffset utc_offset {};
    utc_offset.name = "UTC"sv;

    return Array { utc_offset, utc_offset };
#else
    return {};
#endif
}

Optional<Array<NamedOffset, 2>> get_named_time_zone_offsets(StringView time_zone, AK::Time time)
{
    if (auto maybe_time_zone = time_zone_from_string(time_zone); maybe_time_zone.has_value())
        return get_named_time_zone_offsets(*maybe_time_zone, time);
    return {};
}

Optional<Location> __attribute__((weak)) get_time_zone_location(TimeZone) { return {}; }

Optional<Location> get_time_zone_location(StringView time_zone)
{
    if (auto maybe_time_zone = time_zone_from_string(time_zone); maybe_time_zone.has_value())
        return get_time_zone_location(*maybe_time_zone);
    return {};
}

}
