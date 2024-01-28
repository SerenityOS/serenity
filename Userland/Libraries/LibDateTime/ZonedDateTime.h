/*
 * Copyright (c) 2023-2024, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Time.h>
#include <LibDateTime/Calendar.h>
#include <LibDateTime/LocalDateTime.h>
#include <LibTimeZone/Forward.h>
#include <LibTimeZone/TimeZone.h>

namespace DateTime {

// A date and time with an associated specific time zone.
// FIXME: Leap second handling is unclear.
class ZonedDateTime {
public:
    // Uses the current system time zone.
    static ZonedDateTime now();
    static ZonedDateTime now_in(TimeZone::TimeZone time_zone);

    TimeZone::Offset offset_to_utc() const;
    TimeZone::TimeZone time_zone() const { return m_time_zone; }

    ZonedDateTime in_time_zone(TimeZone::TimeZone new_time_zone) const;

    bool operator==(ZonedDateTime const&) const;

    template<Calendar C>
    C::OutputParts to_parts() const
    {
        return C::to_parts(*this);
    }

    template<Calendar C>
    static ErrorOr<ZonedDateTime> from_parts(C::InputParts const& parts, TimeZone::TimeZone time_zone)
    {
        return C::zoned_date_time_from_parts(parts, time_zone);
    }

    template<Calendar C>
    static ErrorOr<ZonedDateTime> from_parts(C::InputParts const& parts)
    {
        return C::zoned_date_time_from_parts(parts, current_time_zone());
    }

    // This is the offset from 1970-01-01T00:00:00+00:00.
    // FIXME: This does not respect leap seconds.
    Duration offset_to_utc_epoch() const
    {
        auto zone_offset = Duration::from_seconds(offset_to_utc().seconds);
        auto own_epoch_offset = zone_offset + m_unix_offset.offset_to_epoch();
        return own_epoch_offset;
    }

    // Drop the time zone information and return a local date time that represents the same in-timezone local time.
    LocalDateTime as_local_time() const;

    ErrorOr<String> format(StringView format_string) const;

    // For use by calendars only.
    template<Calendar T>
    ZonedDateTime(CalendarBadge<T>, UnixDateTime offset, TimeZone::TimeZone time_zone)
        : ZonedDateTime(offset, time_zone)
    {
    }

    constexpr static bool has_timezone = true;

private:
    friend class LocalDateTime;

    ZonedDateTime(UnixDateTime, TimeZone::TimeZone);

    static TimeZone::TimeZone current_time_zone();

    // The offset from the Unix epoch, in Unix time. This means:
    // - This value itself is not local to the time zone.
    // - This is not the offset from the time zone's epoch.
    // - Changing the time zone never changes this value.
    // FIXME: This does not account for leap seconds. Maybe it should?
    UnixDateTime m_unix_offset;
    TimeZone::TimeZone m_time_zone;
};

}

template<>
struct AK::Formatter<DateTime::ZonedDateTime> {
    Optional<StringView> format_string;

    void parse(TypeErasedFormatParams&, FormatParser&);

    ErrorOr<void> format(FormatBuilder& builder, DateTime::ZonedDateTime const& date_time)
    {
        auto formatted = TRY(date_time.format(this->format_string.value()));
        return builder.put_literal(formatted.bytes_as_string_view());
    }
};
