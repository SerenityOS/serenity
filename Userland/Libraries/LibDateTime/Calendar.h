/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Error.h>
#include <AK/String.h>
#include <LibTimeZone/TimeZone.h>

namespace DateTime {

class ZonedDateTime;
class LocalDateTime;

template<typename CalendarT>
constexpr bool IsCalendar = requires(CalendarT::InputParts input_parts, ZonedDateTime const& zoned_date_time, LocalDateTime const& local_date_time, TimeZone::TimeZone const& time_zone) {
    typename CalendarT::InputParts;
    typename CalendarT::OutputParts;

    {
        CalendarT::zoned_date_time_from_parts(input_parts, time_zone)
    } -> SameAs<ErrorOr<ZonedDateTime>>;
    {
        CalendarT::local_date_time_from_parts(input_parts)
    } -> SameAs<ErrorOr<LocalDateTime>>;
    {
        CalendarT::to_parts(zoned_date_time)
    } -> SameAs<typename CalendarT::OutputParts>;
    {
        CalendarT::format(zoned_date_time)
    } -> SameAs<ErrorOr<String>>;
    {
        CalendarT::to_parts(local_date_time)
    } -> SameAs<typename CalendarT::OutputParts>;
    {
        CalendarT::format(local_date_time)
    } -> SameAs<ErrorOr<String>>;

    // Must not have a default constructor; calendars should generally not be constructible.
    requires(!requires {
        CalendarT();
    });
};

// A calendar is responsible for creating a zoned date time from calendar-specific parts,
// as well as extracting those parts out of the zoned date time again.
// It can also format the date time with or without a format string.
// The syntax is calendar-specific, but should follow AK::Format conventions.
// InputParts and OutputParts types are distinct since many calendars provide a superset of the data
// they need for creating a date time as their output.
template<typename CalendarT>
concept Calendar = IsCalendar<CalendarT>;

template<Calendar CalendarT>
using CalendarBadge = Badge<CalendarT>;

}
