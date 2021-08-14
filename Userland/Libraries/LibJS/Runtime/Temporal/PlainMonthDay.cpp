/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainMonthDay.h>
#include <LibJS/Runtime/Temporal/PlainMonthDayConstructor.h>

namespace JS::Temporal {

// 10 Temporal.PlainMonthDay Objects, https://tc39.es/proposal-temporal/#sec-temporal-plainmonthday-objects
PlainMonthDay::PlainMonthDay(u8 iso_month, u8 iso_day, i32 iso_year, Object& calendar, Object& prototype)
    : Object(prototype)
    , m_iso_year(iso_year)
    , m_iso_month(iso_month)
    , m_iso_day(iso_day)
    , m_calendar(calendar)
{
}

void PlainMonthDay::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_calendar);
}

// 10.5.2 CreateTemporalMonthDay ( isoMonth, isoDay, calendar, referenceISOYear [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalmonthday
PlainMonthDay* create_temporal_month_day(GlobalObject& global_object, u8 iso_month, u8 iso_day, Object& calendar, i32 reference_iso_year, FunctionObject* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: isoMonth, isoDay, and referenceISOYear are integers.
    // 2. Assert: Type(calendar) is Object.

    // 3. If ! IsValidISODate(referenceISOYear, isoMonth, isoDay) is false, throw a RangeError exception.
    if (!is_valid_iso_date(reference_iso_year, iso_month, iso_day)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainMonthDay);
        return {};
    }

    // 4. If newTarget is not present, set it to %Temporal.PlainMonthDay%.
    if (!new_target)
        new_target = global_object.temporal_plain_month_day_constructor();

    // 5. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.PlainMonthDay.prototype%", « [[InitializedTemporalMonthDay]], [[ISOMonth]], [[ISODay]], [[ISOYear]], [[Calendar]] »).
    // 6. Set object.[[ISOMonth]] to isoMonth.
    // 7. Set object.[[ISODay]] to isoDay.
    // 8. Set object.[[Calendar]] to calendar.
    // 9. Set object.[[ISOYear]] to referenceISOYear.
    auto* object = ordinary_create_from_constructor<PlainMonthDay>(global_object, *new_target, &GlobalObject::temporal_plain_month_day_prototype, iso_month, iso_day, reference_iso_year, calendar);
    if (vm.exception())
        return {};

    // 10. Return object.
    return object;
}

}
