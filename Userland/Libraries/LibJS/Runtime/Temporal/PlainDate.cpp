/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateConstructor.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>

namespace JS::Temporal {

// 3 Temporal.PlainDate Objects, https://tc39.es/proposal-temporal/#sec-temporal-plaindate-objects
PlainDate::PlainDate(i32 year, i32 month, i32 day, Object& calendar, Object& prototype)
    : Object(prototype)
    , m_iso_year(year)
    , m_iso_month(month)
    , m_iso_day(day)
    , m_calendar(calendar)
{
}

void PlainDate::visit_edges(Visitor& visitor)
{
    visitor.visit(&m_calendar);
}

// 3.5.1 CreateTemporalDate ( isoYear, isoMonth, isoDay, calendar [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporaldate
PlainDate* create_temporal_date(GlobalObject& global_object, i32 iso_year, i32 iso_month, i32 iso_day, Object& calendar, FunctionObject* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: isoYear is an integer.
    // 2. Assert: isoMonth is an integer.
    // 3. Assert: isoDay is an integer.
    // 4. Assert: Type(calendar) is Object.

    // 5. If ! IsValidISODate(isoYear, isoMonth, isoDay) is false, throw a RangeError exception.
    if (!is_valid_iso_date(iso_year, iso_month, iso_day)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
        return {};
    }

    // 6. If ! ISODateTimeWithinLimits(isoYear, isoMonth, isoDay, 12, 0, 0, 0, 0, 0) is false, throw a RangeError exception.
    if (!iso_date_time_within_limits(global_object, iso_year, iso_month, iso_day, 12, 0, 0, 0, 0, 0)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
        return {};
    }

    // 7. If newTarget is not present, set it to %Temporal.PlainDate%.
    if (!new_target)
        new_target = global_object.temporal_plain_date_constructor();

    // 8. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.PlainDate.prototype%", « [[InitializedTemporalDate]], [[ISOYear]], [[ISOMonth]], [[ISODay]], [[Calendar]] »).
    // 9. Set object.[[ISOYear]] to isoYear.
    // 10. Set object.[[ISOMonth]] to isoMonth.
    // 11. Set object.[[ISODay]] to isoDay.
    // 12. Set object.[[Calendar]] to calendar.
    auto* object = ordinary_create_from_constructor<PlainDate>(global_object, *new_target, &GlobalObject::temporal_plain_date_prototype, iso_year, iso_month, iso_day, calendar);
    if (vm.exception())
        return {};

    return object;
}

// 3.5.4 RegulateISODate ( year, month, day, overflow ), https://tc39.es/proposal-temporal/#sec-temporal-regulateisodate
Optional<TemporalDate> regulate_iso_date(GlobalObject& global_object, double year, double month, double day, String const& overflow)
{
    auto& vm = global_object.vm();
    // 1. Assert: year, month, and day are integers.
    VERIFY(year == trunc(year) && month == trunc(month) && day == trunc(day));
    // 2. Assert: overflow is either "constrain" or "reject".
    // NOTE: Asserted by the VERIFY_NOT_REACHED at the end

    // 3. If overflow is "reject", then
    if (overflow == "reject"sv) {
        // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
        // This does not change the exposed behaviour as the call to IsValidISODate will immediately check that these values are valid ISO
        // values (for years: -273975 - 273975, for months: 1 - 12, for days: 1 - 31) all of which are subsets of this check.
        if (!AK::is_within_range<i32>(year) || !AK::is_within_range<i32>(month) || !AK::is_within_range<i32>(day)) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
            return {};
        }
        auto y = static_cast<i32>(year);
        auto m = static_cast<i32>(month);
        auto d = static_cast<i32>(day);
        // a. If ! IsValidISODate(year, month, day) is false, throw a RangeError exception.
        if (is_valid_iso_date(y, m, d)) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
            return {};
        }
        // b. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day }.
        return TemporalDate { .year = y, .month = m, .day = d, .calendar = {} };
    }
    // 4. If overflow is "constrain", then
    else if (overflow == "constrain"sv) {
        // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat this double as normal integer from this point onwards. This
        // does not change the exposed behaviour as the parent's call to CreateTemporalDate will immediately check that this value is a valid
        // ISO value for years: -273975 - 273975, which is a subset of this check.
        if (!AK::is_within_range<i32>(year)) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
            return {};
        }
        auto y = static_cast<i32>(year);

        // a. Set month to ! ConstrainToRange(month, 1, 12).
        month = constrain_to_range(month, 1, 12);
        // b. Set day to ! ConstrainToRange(day, 1, ! ISODaysInMonth(year, month)).
        day = constrain_to_range(day, 1, iso_days_in_month(y, month));

        // c. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day }.
        return TemporalDate { .year = y, .month = static_cast<i32>(month), .day = static_cast<i32>(day), .calendar = {} };
    }
    VERIFY_NOT_REACHED();
}

// 3.5.5 IsValidISODate ( year, month, day ), https://tc39.es/proposal-temporal/#sec-temporal-isvalidisodate
bool is_valid_iso_date(i32 year, i32 month, i32 day)
{
    // 1. Assert: year, month, and day are integers.

    // 2. If month < 1 or month > 12, then
    if (month < 1 || month > 12) {
        // a. Return false.
        return false;
    }

    // 3. Let daysInMonth be ! ISODaysInMonth(year, month).
    auto days_in_month = iso_days_in_month(year, month);

    // 4. If day < 1 or day > daysInMonth, then
    if (day < 1 || day > days_in_month) {
        // a. Return false.
        return false;
    }

    // 5. Return true.
    return true;
}

}
