/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/CalendarConstructor.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Temporal {

// 12 Temporal.Calendar Objects, https://tc39.es/proposal-temporal/#sec-temporal-calendar-objects
Calendar::Calendar(String identifier, Object& prototype)
    : Object(prototype)
    , m_identifier(move(identifier))
{
}

// 12.1.1 CreateTemporalCalendar ( identifier [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalcalendar
Calendar* create_temporal_calendar(GlobalObject& global_object, String const& identifier, FunctionObject* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: ! IsBuiltinCalendar(identifier) is true.
    VERIFY(is_builtin_calendar(identifier));

    // 2. If newTarget is not provided, set newTarget to %Temporal.Calendar%.
    if (!new_target)
        new_target = global_object.temporal_calendar_constructor();

    // 3. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.Calendar.prototype%", « [[InitializedTemporalCalendar]], [[Identifier]] »).
    // 4. Set object.[[Identifier]] to identifier.
    auto* object = ordinary_create_from_constructor<Calendar>(global_object, *new_target, &GlobalObject::temporal_calendar_prototype, identifier);
    if (vm.exception())
        return {};

    // 5. Return object.
    return object;
}

// 12.1.2 IsBuiltinCalendar ( id ), https://tc39.es/proposal-temporal/#sec-temporal-isbuiltincalendar
// NOTE: This is the minimum IsBuiltinCalendar implementation for engines without ECMA-402.
bool is_builtin_calendar(String const& identifier)
{
    // 1. If id is not "iso8601", return false.
    if (identifier != "iso8601"sv)
        return false;

    // 2. Return true.
    return true;
}

// 12.1.30 IsISOLeapYear ( year ), https://tc39.es/proposal-temporal/#sec-temporal-isisoleapyear
bool is_iso_leap_year(i32 year)
{
    // 1. Assert: year is an integer.

    // 2. If year modulo 4 ≠ 0, return false.
    if (year % 4 != 0)
        return false;

    // 3. If year modulo 400 = 0, return true.
    if (year % 400 == 0)
        return true;

    // 4. If year modulo 100 = 0, return false.
    if (year % 100 == 0)
        return false;

    // 5. Return true.
    return true;
}

// 12.1.32 ISODaysInMonth ( year, month ), https://tc39.es/proposal-temporal/#sec-temporal-isodaysinmonth
i32 iso_days_in_month(i32 year, i32 month)
{
    // 1. Assert: year is an integer.

    // 2. Assert: month is an integer, month ≥ 1, and month ≤ 12.
    VERIFY(month >= 1 && month <= 12);

    // 3. If month is 1, 3, 5, 7, 8, 10, or 12, return 31.
    if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12)
        return 31;

    // 4. If month is 4, 6, 9, or 11, return 30.
    if (month == 4 || month == 6 || month == 9 || month == 11)
        return 30;

    // 5. If ! IsISOLeapYear(year) is true, return 29.
    if (is_iso_leap_year(year))
        return 29;

    // 6. Return 28.
    return 28;
}

}
