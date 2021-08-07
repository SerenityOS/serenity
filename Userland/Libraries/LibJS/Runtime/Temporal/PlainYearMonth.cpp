/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>
#include <LibJS/Runtime/Temporal/PlainYearMonthConstructor.h>

namespace JS::Temporal {

// 9 Temporal.PlainYearMonth Objects, https://tc39.es/proposal-temporal/#sec-temporal-plainyearmonth-objects
PlainYearMonth::PlainYearMonth(i32 iso_year, u8 iso_month, u8 iso_day, Object& calendar, Object& prototype)
    : Object(prototype)
    , m_iso_year(iso_year)
    , m_iso_month(iso_month)
    , m_iso_day(iso_day)
    , m_calendar(calendar)
{
}

void PlainYearMonth::visit_edges(Visitor& visitor)
{
    visitor.visit(&m_calendar);
}

// 9.5.5 BalanceISOYearMonth ( year, month ), https://tc39.es/proposal-temporal/#sec-temporal-balanceisoyearmonth
ISOYearMonth balance_iso_year_month(i32 year, i32 month)
{
    // 1. Assert: year and month are integers.

    // 2. Set year to year + floor((month - 1) / 12).
    year += (month - 1) / 12;

    // 3. Set month to (month − 1) modulo 12 + 1.
    month = (month - 1) % 12 + 1;

    // 4. Return the new Record { [[Year]]: year, [[Month]]: month }.
    return ISOYearMonth { .year = year, .month = static_cast<u8>(month) };
}

// 9.5.4 ISOYearMonthWithinLimits ( year, month ), https://tc39.es/proposal-temporal/#sec-temporal-isoyearmonthwithinlimits
bool iso_year_month_within_limits(i32 year, u8 month)
{
    // 1. Assert: year and month are integers.

    // 2. If year < −271821 or year > 275760, then
    if (year < -271821 || year > 275760) {
        // a. Return false.
        return false;
    }

    // 3. If year is −271821 and month < 4, then
    if (year == -271821 && month < 4) {
        // a. Return false.
        return false;
    }

    // 4. If year is 275760 and month > 9, then
    if (year == 275760 && month > 9) {
        // a. Return false.
        return false;
    }

    // 5. Return true.
    return true;
}

// 9.5.7 CreateTemporalYearMonth ( isoYear, isoMonth, calendar, referenceISODay [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalyearmonth
PlainYearMonth* create_temporal_year_month(GlobalObject& global_object, i32 iso_year, u8 iso_month, Object& calendar, u8 reference_iso_day, FunctionObject* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: isoYear, isoMonth, and referenceISODay are integers.
    // 2. Assert: Type(calendar) is Object.

    // 3. If ! IsValidISODate(isoYear, isoMonth, referenceISODay) is false, throw a RangeError exception.
    if (!is_valid_iso_date(iso_year, iso_month, reference_iso_day)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);
        return {};
    }

    // 4. If ! ISOYearMonthWithinLimits(isoYear, isoMonth) is false, throw a RangeError exception.
    if (!iso_year_month_within_limits(iso_year, iso_month)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);
        return {};
    }

    // 5. If newTarget is not present, set it to %Temporal.PlainYearMonth%.
    if (!new_target)
        new_target = global_object.temporal_plain_year_month_constructor();

    // 6. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.PlainYearMonth.prototype%", « [[InitializedTemporalYearMonth]], [[ISOYear]], [[ISOMonth]], [[ISODay]], [[Calendar]] »).
    // 7. Set object.[[ISOYear]] to isoYear.
    // 8. Set object.[[ISOMonth]] to isoMonth.
    // 9. Set object.[[Calendar]] to calendar.
    // 10. Set object.[[ISODay]] to referenceISODay.
    auto* object = ordinary_create_from_constructor<PlainYearMonth>(global_object, *new_target, &GlobalObject::temporal_plain_year_month_prototype, iso_year, iso_month, reference_iso_day, calendar);
    if (vm.exception())
        return {};

    // 11. Return object.
    return object;
}

}
