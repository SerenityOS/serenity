/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <LibJS/Runtime/Temporal/ZonedDateTimeConstructor.h>

namespace JS::Temporal {

// 6 Temporal.ZonedDateTime Objects, https://tc39.es/proposal-temporal/#sec-temporal-zoneddatetime-objects
ZonedDateTime::ZonedDateTime(BigInt const& nanoseconds, Object& time_zone, Object& calendar, Object& prototype)
    : Object(prototype)
    , m_nanoseconds(nanoseconds)
    , m_time_zone(time_zone)
    , m_calendar(calendar)
{
}

void ZonedDateTime::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(&m_nanoseconds);
    visitor.visit(&m_time_zone);
    visitor.visit(&m_calendar);
}

// 6.5.3 CreateTemporalZonedDateTime ( epochNanoseconds, timeZone, calendar [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalzoneddatetime
ThrowCompletionOr<ZonedDateTime*> create_temporal_zoned_date_time(GlobalObject& global_object, BigInt const& epoch_nanoseconds, Object& time_zone, Object& calendar, FunctionObject const* new_target)
{
    // 1. Assert: Type(epochNanoseconds) is BigInt.
    // 3. Assert: Type(timeZone) is Object.
    // 4. Assert: Type(calendar) is Object.

    // 2. Assert: ! IsValidEpochNanoseconds(epochNanoseconds) is true.
    VERIFY(is_valid_epoch_nanoseconds(epoch_nanoseconds));

    // 5. If newTarget is not present, set it to %Temporal.ZonedDateTime%.
    if (!new_target)
        new_target = global_object.temporal_zoned_date_time_constructor();

    // 6. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.ZonedDateTime.prototype%", « [[InitializedTemporalZonedDateTime]], [[Nanoseconds]], [[TimeZone]], [[Calendar]] »).
    // 7. Set object.[[Nanoseconds]] to epochNanoseconds.
    // 8. Set object.[[TimeZone]] to timeZone.
    // 9. Set object.[[Calendar]] to calendar.
    auto* object = TRY(ordinary_create_from_constructor<ZonedDateTime>(global_object, *new_target, &GlobalObject::temporal_time_zone_prototype, epoch_nanoseconds, time_zone, calendar));

    // 10. Return object.
    return object;
}

// 6.5.5 AddZonedDateTime ( epochNanoseconds, timeZone, calendar, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-addzoneddatetime
ThrowCompletionOr<BigInt*> add_zoned_date_time(GlobalObject& global_object, BigInt const& epoch_nanoseconds, Value time_zone, Object& calendar, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, Object* options)
{
    // 1. If options is not present, set options to ! OrdinaryObjectCreate(null).
    if (!options)
        options = Object::create(global_object, nullptr);

    // 2. If all of years, months, weeks, and days are 0, then
    if (years == 0 && months == 0 && weeks == 0 && days == 0) {
        // a. Return ! AddInstant(epochNanoseconds, hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
        return MUST(add_instant(global_object, epoch_nanoseconds, hours, minutes, seconds, milliseconds, microseconds, nanoseconds));
    }

    // 3. Let instant be ! CreateTemporalInstant(epochNanoseconds).
    auto* instant = MUST(create_temporal_instant(global_object, epoch_nanoseconds));

    // 4. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY(builtin_time_zone_get_plain_date_time_for(global_object, time_zone, *instant, calendar));

    // 5. Let datePart be ? CreateTemporalDate(temporalDateTime.[[ISOYear]], temporalDateTime.[[ISOMonth]], temporalDateTime.[[ISODay]], calendar).
    auto* date_part = TRY(create_temporal_date(global_object, temporal_date_time->iso_year(), temporal_date_time->iso_month(), temporal_date_time->iso_day(), calendar));

    // 6. Let dateDuration be ? CreateTemporalDuration(years, months, weeks, days, 0, 0, 0, 0, 0, 0).
    auto* date_duration = TRY(create_temporal_duration(global_object, years, months, weeks, days, 0, 0, 0, 0, 0, 0));

    // 7. Let addedDate be ? CalendarDateAdd(calendar, datePart, dateDuration, options).
    auto* added_date = TRY(calendar_date_add(global_object, calendar, *date_part, *date_duration, options));

    // 8. Let intermediateDateTime be ? CreateTemporalDateTime(addedDate.[[ISOYear]], addedDate.[[ISOMonth]], addedDate.[[ISODay]], temporalDateTime.[[ISOHour]], temporalDateTime.[[ISOMinute]], temporalDateTime.[[ISOSecond]], temporalDateTime.[[ISOMillisecond]], temporalDateTime.[[ISOMicrosecond]], temporalDateTime.[[ISONanosecond]], calendar).
    auto* intermediate_date_time = TRY(create_temporal_date_time(global_object, added_date->iso_year(), added_date->iso_month(), added_date->iso_day(), temporal_date_time->iso_hour(), temporal_date_time->iso_minute(), temporal_date_time->iso_second(), temporal_date_time->iso_millisecond(), temporal_date_time->iso_microsecond(), temporal_date_time->iso_nanosecond(), calendar));

    // 9. Let intermediateInstant be ? BuiltinTimeZoneGetInstantFor(timeZone, intermediateDateTime, "compatible").
    auto* intermediate_instant = TRY(builtin_time_zone_get_instant_for(global_object, time_zone, *intermediate_date_time, "compatible"sv));

    // 10. Return ! AddInstant(intermediateInstant.[[Nanoseconds]], hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
    return MUST(add_instant(global_object, intermediate_instant->nanoseconds(), hours, minutes, seconds, milliseconds, microseconds, nanoseconds));
}

// 6.5.7 NanosecondsToDays ( nanoseconds, relativeTo ), https://tc39.es/proposal-temporal/#sec-temporal-nanosecondstodays
ThrowCompletionOr<NanosecondsToDaysResult> nanoseconds_to_days(GlobalObject& global_object, BigInt const& nanoseconds_bigint, Value relative_to_value)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(nanoseconds) is BigInt.

    // 2. Set nanoseconds to ℝ(nanoseconds).
    auto nanoseconds = nanoseconds_bigint.big_integer();

    // 3. Let sign be ! ℝ(Sign(𝔽(nanoseconds))).
    i8 sign;
    if (nanoseconds == Crypto::UnsignedBigInteger { 0 })
        sign = 0;
    else if (nanoseconds.is_negative())
        sign = -1;
    else
        sign = 1;

    // 4. Let dayLengthNs be 8.64 × 10^13.
    auto day_length_ns = "86400000000000"_sbigint;

    // 5. If sign is 0, then
    if (sign == 0) {
        // a. Return the Record { [[Days]]: 0, [[Nanoseconds]]: 0, [[DayLength]]: dayLengthNs }.
        return NanosecondsToDaysResult { .days = 0, .nanoseconds = make_handle(js_bigint(vm, { 0 })), .day_length = day_length_ns.to_double() };
    }

    // 6. If Type(relativeTo) is not Object or relativeTo does not have an [[InitializedTemporalZonedDateTime]] internal slot, then
    if (!relative_to_value.is_object() || !is<ZonedDateTime>(relative_to_value.as_object())) {
        // a. Return the Record { [[Days]]: the integral part of nanoseconds / dayLengthNs, [[Nanoseconds]]: (abs(nanoseconds) modulo dayLengthNs) × sign, [[DayLength]]: dayLengthNs }.
        return NanosecondsToDaysResult {
            .days = nanoseconds.divided_by(day_length_ns).quotient.to_double(),
            .nanoseconds = make_handle(js_bigint(vm, Crypto::SignedBigInteger { nanoseconds.unsigned_value() }.divided_by(day_length_ns).remainder.multiplied_by(Crypto::SignedBigInteger { sign }))),
            .day_length = day_length_ns.to_double()
        };
    }

    auto& relative_to = static_cast<ZonedDateTime&>(relative_to_value.as_object());

    // 7. Let startNs be ℝ(relativeTo.[[Nanoseconds]]).
    auto& start_ns = relative_to.nanoseconds().big_integer();

    // 8. Let startInstant be ! CreateTemporalInstant(ℤ(startNs)).
    auto* start_instant = MUST(create_temporal_instant(global_object, *js_bigint(vm, start_ns)));

    // 9. Let startDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(relativeTo.[[TimeZone]], startInstant, relativeTo.[[Calendar]]).
    auto* start_date_time = TRY(builtin_time_zone_get_plain_date_time_for(global_object, &relative_to.time_zone(), *start_instant, relative_to.calendar()));

    // 10. Let endNs be startNs + nanoseconds.
    auto end_ns = start_ns.plus(nanoseconds);

    // 11. Let endInstant be ! CreateTemporalInstant(ℤ(endNs)).
    auto* end_instant = MUST(create_temporal_instant(global_object, *js_bigint(vm, end_ns)));

    // 12. Let endDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(relativeTo.[[TimeZone]], endInstant, relativeTo.[[Calendar]]).
    auto* end_date_time = TRY(builtin_time_zone_get_plain_date_time_for(global_object, &relative_to.time_zone(), *end_instant, relative_to.calendar()));

    // 13. Let dateDifference be ? DifferenceISODateTime(startDateTime.[[ISOYear]], startDateTime.[[ISOMonth]], startDateTime.[[ISODay]], startDateTime.[[ISOHour]], startDateTime.[[ISOMinute]], startDateTime.[[ISOSecond]], startDateTime.[[ISOMillisecond]], startDateTime.[[ISOMicrosecond]], startDateTime.[[ISONanosecond]], endDateTime.[[ISOYear]], endDateTime.[[ISOMonth]], endDateTime.[[ISODay]], endDateTime.[[ISOHour]], endDateTime.[[ISOMinute]], endDateTime.[[ISOSecond]], endDateTime.[[ISOMillisecond]], endDateTime.[[ISOMicrosecond]], endDateTime.[[ISONanosecond]], relativeTo.[[Calendar]], "day").
    auto date_difference = TRY(difference_iso_date_time(global_object, start_date_time->iso_year(), start_date_time->iso_month(), start_date_time->iso_day(), start_date_time->iso_hour(), start_date_time->iso_minute(), start_date_time->iso_second(), start_date_time->iso_millisecond(), start_date_time->iso_microsecond(), start_date_time->iso_nanosecond(), end_date_time->iso_year(), end_date_time->iso_month(), end_date_time->iso_day(), end_date_time->iso_hour(), end_date_time->iso_minute(), end_date_time->iso_second(), end_date_time->iso_millisecond(), end_date_time->iso_microsecond(), end_date_time->iso_nanosecond(), relative_to.calendar(), "day"sv));

    // 14. Let days be dateDifference.[[Days]].
    auto days = date_difference.days;

    // 15. Let intermediateNs be ℝ(? AddZonedDateTime(ℤ(startNs), relativeTo.[[TimeZone]], relativeTo.[[Calendar]], 0, 0, 0, days, 0, 0, 0, 0, 0, 0)).
    auto intermediate_ns = TRY(add_zoned_date_time(global_object, *js_bigint(vm, start_ns), &relative_to.time_zone(), relative_to.calendar(), 0, 0, 0, days, 0, 0, 0, 0, 0, 0))->big_integer();

    // 16. If sign is 1, then
    if (sign == 1) {
        // a. Repeat, while days > 0 and intermediateNs > endNs,
        while (days > 0 && intermediate_ns > end_ns) {
            // i. Set days to days − 1.
            days--;

            // ii. Set intermediateNs to ℝ(? AddZonedDateTime(ℤ(startNs), relativeTo.[[TimeZone]], relativeTo.[[Calendar]], 0, 0, 0, days, 0, 0, 0, 0, 0, 0)).
            intermediate_ns = TRY(add_zoned_date_time(global_object, *js_bigint(vm, start_ns), &relative_to.time_zone(), relative_to.calendar(), 0, 0, 0, days, 0, 0, 0, 0, 0, 0))->big_integer();
        }
    }

    // 17. Set nanoseconds to endNs − intermediateNs.
    nanoseconds = end_ns.minus(intermediate_ns);

    // 18. Let done be false.
    // 19. Repeat, while done is false,
    while (true) {
        // a. Let oneDayFartherNs be ℝ(? AddZonedDateTime(ℤ(intermediateNs), relativeTo.[[TimeZone]], relativeTo.[[Calendar]], 0, 0, 0, sign, 0, 0, 0, 0, 0, 0)).
        auto one_day_farther_ns = TRY(add_zoned_date_time(global_object, *js_bigint(vm, intermediate_ns), &relative_to.time_zone(), relative_to.calendar(), 0, 0, 0, sign, 0, 0, 0, 0, 0, 0))->big_integer();

        // b. Set dayLengthNs to oneDayFartherNs − intermediateNs.
        day_length_ns = one_day_farther_ns.minus(intermediate_ns);

        // c. If (nanoseconds − dayLengthNs) × sign ≥ 0, then
        if (nanoseconds.minus(day_length_ns).multiplied_by(Crypto::SignedBigInteger { sign }) >= "0"_sbigint) {
            // i. Set nanoseconds to nanoseconds − dayLengthNs.
            nanoseconds = nanoseconds.minus(day_length_ns);

            // ii. Set intermediateNs to oneDayFartherNs.
            intermediate_ns = move(one_day_farther_ns);

            // iii. Set days to days + sign.
            days += sign;
        }
        // d. Else,
        else {
            // i. Set done to true.
            break;
        }
    }

    // 20. Return the Record { [[Days]]: days, [[Nanoseconds]]: nanoseconds, [[DayLength]]: abs(dayLengthNs) }.
    return NanosecondsToDaysResult { .days = days, .nanoseconds = make_handle(js_bigint(vm, move(nanoseconds))), .day_length = fabs(day_length_ns.to_double()) };
}

}
