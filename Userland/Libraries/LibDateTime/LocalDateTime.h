/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Time.h>
#include <LibCore/DateTime.h>
#include <LibDateTime/Calendar.h>
#include <LibTimeZone/TimeZone.h>

namespace DateTime {

class ZonedDateTime;

// A date and time without a time zone.
// This is useful for ephermal time calculations within time zones that can disregard them.
// For instance, GUI can produce and consume local time.
// It is *wrong* to use this class for anything permanent or connected to other systems, like:
// - NTP communication
// - System time control
// - Permanent storage, e.g. events in a calendar.
// This class also has no knowledge of leap seconds, since accounting for leap seconds correctly requires time zone knowledge.
class LocalDateTime {
public:
    // Current time in the current time zone.
    static LocalDateTime now();

    // Offset from the Unix epoch 1970-01-01T00:00:00.
    // The timezone in which this offset applies is just as unspecified as in the class in general!
    Duration offset_to_local_epoch() const { return m_offset.offset_to_epoch(); }

    bool operator==(LocalDateTime const&) const;
    int operator<=>(LocalDateTime const&) const;
    Duration operator-(LocalDateTime const&) const;

    LocalDateTime operator+(Duration const& duration) const { return { m_offset + duration }; }
    LocalDateTime const& operator+=(Duration const& duration)
    {
        this->m_offset += duration;
        return *this;
    }

    template<Calendar C>
    C::OutputParts to_parts() const
    {
        return C::to_parts(*this);
    }

    template<Calendar C>
    static ErrorOr<LocalDateTime> from_parts(C::InputParts const& parts)
    {
        return C::local_date_time_from_parts(parts);
    }

    ErrorOr<String> format(StringView format_string) const;

    // For use by calendars only.
    template<Calendar T>
    LocalDateTime(CalendarBadge<T>, UnixDateTime offset)
        : LocalDateTime(offset)
    {
    }

    // FIXME: Remove this API together with Core::DateTime.
    explicit LocalDateTime(Core::DateTime const& legacy_date_time)
        : LocalDateTime(UnixDateTime::from_unix_timespec({ .tv_sec = legacy_date_time.timestamp() }))
    {
    }

    constexpr static bool has_timezone = false;

private:
    friend class ZonedDateTime;

    LocalDateTime(UnixDateTime offset)
        : m_offset(offset)
    {
    }

    UnixDateTime m_offset;
};

}

template<>
struct AK::Formatter<DateTime::LocalDateTime> {
    Optional<StringView> format_string;

    void parse(TypeErasedFormatParams&, FormatParser&);

    ErrorOr<void> format(FormatBuilder& builder, DateTime::LocalDateTime const& date_time)
    {
        auto formatted = TRY(date_time.format(this->format_string.value()));
        return builder.put_literal(formatted.bytes_as_string_view());
    }
};
