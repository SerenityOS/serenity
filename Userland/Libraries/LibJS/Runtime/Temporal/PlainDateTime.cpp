/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainDateTimeConstructor.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>

namespace JS::Temporal {

// 5 Temporal.PlainDateTime Objects, https://tc39.es/proposal-temporal/#sec-temporal-plaindatetime-objects
PlainDateTime::PlainDateTime(i32 iso_year, u8 iso_month, u8 iso_day, u8 iso_hour, u8 iso_minute, u8 iso_second, u16 iso_millisecond, u16 iso_microsecond, u16 iso_nanosecond, Object& calendar, Object& prototype)
    : Object(prototype)
    , m_iso_year(iso_year)
    , m_iso_month(iso_month)
    , m_iso_day(iso_day)
    , m_iso_hour(iso_hour)
    , m_iso_minute(iso_minute)
    , m_iso_second(iso_second)
    , m_iso_millisecond(iso_millisecond)
    , m_iso_microsecond(iso_microsecond)
    , m_iso_nanosecond(iso_nanosecond)
    , m_calendar(calendar)
{
}

void PlainDateTime::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_calendar);
}

// 5.5.1 GetEpochFromISOParts ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-getepochfromisoparts
BigInt* get_epoch_from_iso_parts(GlobalObject& global_object, i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond)
{
    auto& vm = global_object.vm();

    // 1. Assert: year, month, day, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Assert: ! IsValidISODate(year, month, day) is true.
    VERIFY(is_valid_iso_date(year, month, day));

    // 3. Assert: ! IsValidTime(hour, minute, second, millisecond, microsecond, nanosecond) is true.
    VERIFY(is_valid_time(hour, minute, second, millisecond, microsecond, nanosecond));

    // 4. Let date be ! MakeDay(𝔽(year), 𝔽(month − 1), 𝔽(day)).
    auto date = make_day(global_object, Value(year), Value(month - 1), Value(day));

    // 5. Let time be ! MakeTime(𝔽(hour), 𝔽(minute), 𝔽(second), 𝔽(millisecond)).
    auto time = make_time(global_object, Value(hour), Value(minute), Value(second), Value(millisecond));

    // 6. Let ms be ! MakeDate(date, time).
    auto ms = make_date(date, time);

    // 7. Assert: ms is finite.
    VERIFY(ms.is_finite_number());

    // 8. Return ℝ(ms) × 10^6 + microsecond × 10^3 + nanosecond.
    return js_bigint(vm, Crypto::SignedBigInteger::create_from(static_cast<i64>(ms.as_double())).multiplied_by(Crypto::UnsignedBigInteger { 1'000'000 }).plus(Crypto::SignedBigInteger::create_from((i64)microsecond * 1000)).plus(Crypto::SignedBigInteger(nanosecond)));
}

// -864 * 10^19 - 864 * 10^14
const auto DATETIME_NANOSECONDS_MIN = "-8640086400000000000000"_sbigint;
// +864 * 10^19 + 864 * 10^14
const auto DATETIME_NANOSECONDS_MAX = "8640086400000000000000"_sbigint;

// 5.5.2 ISODateTimeWithinLimits ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-isodatetimewithinlimits
bool iso_date_time_within_limits(GlobalObject& global_object, i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond)
{
    // 1. Assert: year, month, day, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Let ns be ! GetEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond).
    auto ns = get_epoch_from_iso_parts(global_object, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);

    // 3. If ns ≤ -8.64 × 10^21 - 8.64 × 10^16, then
    if (ns->big_integer() <= DATETIME_NANOSECONDS_MIN) {
        // a. Return false.
        return false;
    }

    // 4. If ns ≥ 8.64 × 10^21 + 8.64 × 10^16, then
    if (ns->big_integer() >= DATETIME_NANOSECONDS_MAX) {
        // a. Return false.
        return false;
    }
    // 5. Return true.
    return true;
}

// 5.5.5 BalanceISODateTime ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-balanceisodatetime
ISODateTime balance_iso_date_time(i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, i64 nanosecond)
{
    // NOTE: The only use of this AO is in BuiltinTimeZoneGetPlainDateTimeFor, where we know that all values
    // but `nanosecond` are in their usual range, hence why that's the only outlier here. The range for that
    // is -86400000000000 to 86400000000999, so an i32 is not enough.

    // 1. Assert: year, month, day, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Let balancedTime be ! BalanceTime(hour, minute, second, millisecond, microsecond, nanosecond).
    auto balanced_time = balance_time(hour, minute, second, millisecond, microsecond, nanosecond);

    // 3. Let balancedDate be ! BalanceISODate(year, month, day + balancedTime.[[Days]]).
    auto balanced_date = balance_iso_date(year, month, day + balanced_time.days);

    // 4. Return the Record { [[Year]]: balancedDate.[[Year]], [[Month]]: balancedDate.[[Month]], [[Day]]: balancedDate.[[Day]], [[Hour]]: balancedTime.[[Hour]], [[Minute]]: balancedTime.[[Minute]], [[Second]]: balancedTime.[[Second]], [[Millisecond]]: balancedTime.[[Millisecond]], [[Microsecond]]: balancedTime.[[Microsecond]], [[Nanosecond]]: balancedTime.[[Nanosecond]] }.
    return ISODateTime { .year = balanced_date.year, .month = balanced_date.month, .day = balanced_date.day, .hour = balanced_time.hour, .minute = balanced_time.minute, .second = balanced_time.second, .millisecond = balanced_time.millisecond, .microsecond = balanced_time.microsecond, .nanosecond = balanced_time.nanosecond };
}

// 5.5.6 CreateTemporalDateTime ( isoYear, isoMonth, isoDay, hour, minute, second, millisecond, microsecond, nanosecond, calendar [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporaldatetime
PlainDateTime* create_temporal_date_time(GlobalObject& global_object, i32 iso_year, u8 iso_month, u8 iso_day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, Object& calendar, FunctionObject* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: isoYear, isoMonth, isoDay, hour, minute, second, millisecond, microsecond, and nanosecond are integers.
    // 2. Assert: Type(calendar) is Object.

    // 3. If ! IsValidISODate(isoYear, isoMonth, isoDay) is false, throw a RangeError exception.
    if (!is_valid_iso_date(iso_year, iso_month, iso_day)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 4. If ! IsValidTime(hour, minute, second, millisecond, microsecond, nanosecond) is false, throw a RangeError exception.
    if (!is_valid_time(hour, minute, second, millisecond, microsecond, nanosecond)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 5. If ! ISODateTimeWithinLimits(isoYear, isoMonth, isoDay, hour, minute, second, millisecond, microsecond, nanosecond) is false, then
    if (!iso_date_time_within_limits(global_object, iso_year, iso_month, iso_day, hour, minute, second, millisecond, microsecond, nanosecond)) {
        // a. Throw a RangeError exception.
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
        return {};
    }

    // 6. If newTarget is not present, set it to %Temporal.PlainDateTime%.
    if (!new_target)
        new_target = global_object.temporal_plain_date_time_constructor();

    // 7. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.PlainDateTime.prototype%", « [[InitializedTemporalDateTime]], [[ISOYear]], [[ISOMonth]], [[ISODay]], [[ISOHour]], [[ISOMinute]], [[ISOSecond]], [[ISOMillisecond]], [[ISOMicrosecond]], [[ISONanosecond]], [[Calendar]] »).
    // 8. Set object.[[ISOYear]] to isoYear.
    // 9. Set object.[[ISOMonth]] to isoMonth.
    // 10. Set object.[[ISODay]] to isoDay.
    // 11. Set object.[[ISOHour]] to hour.
    // 12. Set object.[[ISOMinute]] to minute.
    // 13. Set object.[[ISOSecond]] to second.
    // 14. Set object.[[ISOMillisecond]] to millisecond.
    // 15. Set object.[[ISOMicrosecond]] to microsecond.
    // 16. Set object.[[ISONanosecond]] to nanosecond.
    // 17. Set object.[[Calendar]] to calendar.
    auto* object = ordinary_create_from_constructor<PlainDateTime>(global_object, *new_target, &GlobalObject::temporal_plain_date_prototype, iso_year, iso_month, iso_day, hour, minute, second, millisecond, microsecond, nanosecond, calendar);
    if (vm.exception())
        return {};

    // 18. Return object.
    return object;
}

}
