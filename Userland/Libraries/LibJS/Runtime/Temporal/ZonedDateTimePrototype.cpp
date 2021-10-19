/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <LibJS/Runtime/Temporal/ZonedDateTimePrototype.h>

namespace JS::Temporal {

// 6.3 Properties of the Temporal.ZonedDateTime Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-zoneddatetime-prototype-object
ZonedDateTimePrototype::ZonedDateTimePrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void ZonedDateTimePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 6.3.2 Temporal.ZonedDateTime.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.ZonedDateTime"), Attribute::Configurable);

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.timeZone, time_zone_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.year, year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.month, month_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.monthCode, month_code_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.day, day_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.hour, hour_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.minute, minute_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.second, second_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.millisecond, millisecond_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.microsecond, microsecond_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.nanosecond, nanosecond_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.epochSeconds, epoch_seconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.epochMilliseconds, epoch_milliseconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.epochMicroseconds, epoch_microseconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.epochNanoseconds, epoch_nanoseconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.dayOfWeek, day_of_week_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.dayOfYear, day_of_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.weekOfYear, week_of_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInWeek, days_in_week_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInMonth, days_in_month_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInYear, days_in_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.monthsInYear, months_in_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.inLeapYear, in_leap_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.offsetNanoseconds, offset_nanoseconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.offset, offset_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.era, era_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.eraYear, era_year_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.valueOf, value_of, 0, attr);
    define_native_function(vm.names.toInstant, to_instant, 0, attr);
    define_native_function(vm.names.toPlainDate, to_plain_date, 0, attr);
    define_native_function(vm.names.toPlainTime, to_plain_time, 0, attr);
    define_native_function(vm.names.toPlainDateTime, to_plain_date_time, 0, attr);
    define_native_function(vm.names.toPlainYearMonth, to_plain_year_month, 0, attr);
    define_native_function(vm.names.toPlainMonthDay, to_plain_month_day, 0, attr);
    define_native_function(vm.names.getISOFields, get_iso_fields, 0, attr);
}

// 6.3.3 get Temporal.ZonedDateTime.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.calendar
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::calendar_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return zonedDateTime.[[Calendar]].
    return Value(&zoned_date_time->calendar());
}

// 6.3.4 get Temporal.ZonedDateTime.prototype.timeZone, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.timezone
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::time_zone_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return zonedDateTime.[[TimeZone]].
    return Value(&zoned_date_time->time_zone());
}

// 6.3.5 get Temporal.ZonedDateTime.prototype.year, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.year
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::year_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarYear(calendar, temporalDateTime).
    return Value(TRY_OR_DISCARD(calendar_year(global_object, calendar, *temporal_date_time)));
}

// 6.3.6 get Temporal.ZonedDateTime.prototype.month, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.month
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::month_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarMonth(calendar, temporalDateTime).
    return Value(TRY_OR_DISCARD(calendar_month(global_object, calendar, *temporal_date_time)));
}

// 6.3.7 get Temporal.ZonedDateTime.prototype.monthCode, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.monthcode
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::month_code_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarMonthCode(calendar, temporalDateTime).
    return js_string(vm, TRY_OR_DISCARD(calendar_month_code(global_object, calendar, *temporal_date_time)));
}

// 6.3.8 get Temporal.ZonedDateTime.prototype.day, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.day
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::day_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarDay(calendar, temporalDateTime).
    return Value(TRY_OR_DISCARD(calendar_day(global_object, calendar, *temporal_date_time)));
}

// 6.3.9 get Temporal.ZonedDateTime.prototype.hour, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.hour
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::hour_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return 𝔽(temporalDateTime.[[ISOHour]]).
    return Value(temporal_date_time->iso_hour());
}

// 6.3.10 get Temporal.ZonedDateTime.prototype.minute, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.minute
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::minute_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return 𝔽(temporalDateTime.[[ISOMinute]]).
    return Value(temporal_date_time->iso_minute());
}

// 6.3.11 get Temporal.ZonedDateTime.prototype.second, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.second
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::second_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return 𝔽(temporalDateTime.[[ISOSecond]]).
    return Value(temporal_date_time->iso_second());
}

// 6.3.12 get Temporal.ZonedDateTime.prototype.millisecond, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.millisecond
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::millisecond_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return 𝔽(temporalDateTime.[[ISOMillisecond]]).
    return Value(temporal_date_time->iso_millisecond());
}

// 6.3.13 get Temporal.ZonedDateTime.prototype.microsecond, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.microsecond
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::microsecond_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return 𝔽(temporalDateTime.[[ISOMicrosecond]]).
    return Value(temporal_date_time->iso_microsecond());
}

// 6.3.14 get Temporal.ZonedDateTime.prototype.nanosecond, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.nanosecond
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::nanosecond_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return 𝔽(temporalDateTime.[[ISONanosecond]]).
    return Value(temporal_date_time->iso_nanosecond());
}

// 6.3.15 get Temporal.ZonedDateTime.prototype.epochSeconds, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.epochseconds
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::epoch_seconds_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let ns be zonedDateTime.[[Nanoseconds]].
    auto& ns = zoned_date_time->nanoseconds();

    // 4. Let s be RoundTowardsZero(ℝ(ns) / 10^9).
    auto s = ns.big_integer().divided_by(Crypto::UnsignedBigInteger { 1'000'000'000 }).quotient;

    // 5. Return 𝔽(s).
    return Value((double)s.to_base(10).to_int<i64>().value());
}

// 6.3.16 get Temporal.ZonedDateTime.prototype.epochMilliseconds, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.epochmilliseconds
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::epoch_milliseconds_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let ns be zonedDateTime.[[Nanoseconds]].
    auto& ns = zoned_date_time->nanoseconds();

    // 4. Let ms be RoundTowardsZero(ℝ(ns) / 10^6).
    auto ms = ns.big_integer().divided_by(Crypto::UnsignedBigInteger { 1'000'000 }).quotient;

    // 5. Return 𝔽(ms).
    return Value((double)ms.to_base(10).to_int<i64>().value());
}

// 6.3.17 get Temporal.ZonedDateTime.prototype.epochMicroseconds, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.epochmicroseconds
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::epoch_microseconds_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let ns be zonedDateTime.[[Nanoseconds]].
    auto& ns = zoned_date_time->nanoseconds();

    // 4. Let µs be RoundTowardsZero(ℝ(ns) / 10^3).
    auto us = ns.big_integer().divided_by(Crypto::UnsignedBigInteger { 1'000 }).quotient;

    // 5. Return ℤ(µs).
    return js_bigint(vm, move(us));
}

// 6.3.18 get Temporal.ZonedDateTime.prototype.epochNanoseconds, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.epochnanoseconds
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::epoch_nanoseconds_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return zonedDateTime.[[Nanoseconds]].
    return &zoned_date_time->nanoseconds();
}

// 6.3.19 get Temporal.ZonedDateTime.prototype.dayOfWeek, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.dayofweek
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::day_of_week_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarDayOfWeek(calendar, temporalDateTime).
    return TRY_OR_DISCARD(calendar_day_of_week(global_object, calendar, *temporal_date_time));
}

// 6.3.20 get Temporal.ZonedDateTime.prototype.dayOfYear, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.dayofyear
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::day_of_year_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarDayOfYear(calendar, temporalDateTime).
    return TRY_OR_DISCARD(calendar_day_of_year(global_object, calendar, *temporal_date_time));
}

// 6.3.21 get Temporal.ZonedDateTime.prototype.weekOfYear, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.weekofyear
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::week_of_year_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarWeekOfYear(calendar, temporalDateTime).
    return TRY_OR_DISCARD(calendar_week_of_year(global_object, calendar, *temporal_date_time));
}

// 6.3.23 get Temporal.ZonedDateTime.prototype.daysInWeek, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.daysinweek
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::days_in_week_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarDaysInWeek(calendar, temporalDateTime).
    return TRY_OR_DISCARD(calendar_days_in_week(global_object, calendar, *temporal_date_time));
}

// 6.3.24 get Temporal.ZonedDateTime.prototype.daysInMonth, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.daysinmonth
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::days_in_month_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarDaysInMonth(calendar, temporalDateTime).
    return TRY_OR_DISCARD(calendar_days_in_month(global_object, calendar, *temporal_date_time));
}

// 6.3.25 get Temporal.ZonedDateTime.prototype.daysInYear, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.daysinyear
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::days_in_year_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarDaysInYear(calendar, temporalDateTime).
    return TRY_OR_DISCARD(calendar_days_in_year(global_object, calendar, *temporal_date_time));
}

// 6.3.26 get Temporal.ZonedDateTime.prototype.monthsInYear, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.monthsinyear
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::months_in_year_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarMonthsInYear(calendar, temporalDateTime).
    return TRY_OR_DISCARD(calendar_months_in_year(global_object, calendar, *temporal_date_time));
}

// 6.3.27 get Temporal.ZonedDateTime.prototype.inLeapYear, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.inleapyear
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::in_leap_year_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarInLeapYear(calendar, temporalDateTime).
    return TRY_OR_DISCARD(calendar_in_leap_year(global_object, calendar, *temporal_date_time));
}

// 6.3.28 get Temporal.ZonedDateTime.prototype.offsetNanoseconds, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.offsetnanoseconds
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::offset_nanoseconds_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Return 𝔽(? GetOffsetNanosecondsFor(timeZone, instant)).
    return Value(TRY_OR_DISCARD(get_offset_nanoseconds_for(global_object, &time_zone, *instant)));
}

// 6.3.29 get Temporal.ZonedDateTime.prototype.offset, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.offset
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::offset_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 4. Return ? BuiltinTimeZoneGetOffsetStringFor(zonedDateTime.[[TimeZone]], instant).
    auto offset_string = TRY_OR_DISCARD(builtin_time_zone_get_offset_string_for(global_object, &zoned_date_time->time_zone(), *instant));
    return js_string(vm, move(offset_string));
}

// 15.6.10.2 get Temporal.ZonedDateTime.prototype.era, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.era
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::era_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let plainDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* plain_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarEra(calendar, plainDateTime).
    return TRY_OR_DISCARD(calendar_era(global_object, calendar, *plain_date_time));
}

// 15.6.10.3 get Temporal.ZonedDateTime.prototype.eraYear, https://tc39.es/proposal-temporal/#sec-get-temporal.zoneddatetime.prototype.erayear
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::era_year_getter)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let plainDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* plain_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CalendarEraYear(calendar, plainDateTime).
    return TRY_OR_DISCARD(calendar_era_year(global_object, calendar, *plain_date_time));
}

// 6.3.44 Temporal.ZonedDateTime.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.prototype.valueof
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.ZonedDateTime", "a primitive value");
    return {};
}

// 6.3.46 Temporal.ZonedDateTime.prototype.toInstant ( ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.prototype.toinstant
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::to_instant)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    return MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));
}

// 6.3.47 Temporal.ZonedDateTime.prototype.toPlainDate ( ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.prototype.toplaindate
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::to_plain_date)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CreateTemporalDate(temporalDateTime.[[ISOYear]], temporalDateTime.[[ISOMonth]], temporalDateTime.[[ISODay]], calendar).
    return TRY_OR_DISCARD(create_temporal_date(global_object, temporal_date_time->iso_year(), temporal_date_time->iso_month(), temporal_date_time->iso_day(), calendar));
}

// 6.3.48 Temporal.ZonedDateTime.prototype.toPlainTime ( ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.prototype.toplaintime
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::to_plain_time)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, zonedDateTime.[[Calendar]]).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Return ? CreateTemporalTime(temporalDateTime.[[ISOHour]], temporalDateTime.[[ISOMinute]], temporalDateTime.[[ISOSecond]], temporalDateTime.[[ISOMillisecond]], temporalDateTime.[[ISOMicrosecond]], temporalDateTime.[[ISONanosecond]]).
    return TRY_OR_DISCARD(create_temporal_time(global_object, temporal_date_time->iso_hour(), temporal_date_time->iso_minute(), temporal_date_time->iso_second(), temporal_date_time->iso_millisecond(), temporal_date_time->iso_microsecond(), temporal_date_time->iso_nanosecond()));
}

// 6.3.49 Temporal.ZonedDateTime.prototype.toPlainDateTime ( ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.prototype.toplaindatetime
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::to_plain_date_time)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Return ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, zonedDateTime.[[Calendar]]).
    return TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, zoned_date_time->calendar()));
}

// 6.3.50 Temporal.ZonedDateTime.prototype.toPlainYearMonth ( ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.prototype.toplainyearmonth
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::to_plain_year_month)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Let fieldNames be ? CalendarFields(calendar, « "monthCode", "year" »).
    auto field_names = TRY_OR_DISCARD(calendar_fields(global_object, calendar, { "monthCode"sv, "year"sv }));

    // 8. Let fields be ? PrepareTemporalFields(temporalDateTime, fieldNames, «»).
    auto* fields = TRY_OR_DISCARD(prepare_temporal_fields(global_object, *temporal_date_time, field_names, {}));

    // 9. Return ? YearMonthFromFields(calendar, fields).
    return TRY_OR_DISCARD(year_month_from_fields(global_object, calendar, *fields));
}

// 6.3.51 Temporal.ZonedDateTime.prototype.toPlainMonthDay ( ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.prototype.toplainmonthday
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::to_plain_month_day)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 4. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 5. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 6. Let temporalDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* temporal_date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 7. Let fieldNames be ? CalendarFields(calendar, « "day", "monthCode" »).
    auto field_names = TRY_OR_DISCARD(calendar_fields(global_object, calendar, { "day"sv, "monthCode"sv }));

    // 8. Let fields be ? PrepareTemporalFields(temporalDateTime, fieldNames, «»).
    auto* fields = TRY_OR_DISCARD(prepare_temporal_fields(global_object, *temporal_date_time, field_names, {}));

    // 9. Return ? MonthDayFromFields(calendar, fields).
    return TRY_OR_DISCARD(month_day_from_fields(global_object, calendar, *fields));
}

// 6.3.52 Temporal.ZonedDateTime.prototype.getISOFields ( ), https://tc39.es/proposal-temporal/#sec-temporal.zoneddatetime.prototype.getisofields
JS_DEFINE_OLD_NATIVE_FUNCTION(ZonedDateTimePrototype::get_iso_fields)
{
    // 1. Let zonedDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(zonedDateTime, [[InitializedTemporalZonedDateTime]]).
    auto* zoned_date_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let fields be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* fields = Object::create(global_object, global_object.object_prototype());

    // 4. Let timeZone be zonedDateTime.[[TimeZone]].
    auto& time_zone = zoned_date_time->time_zone();

    // 5. Let instant be ! CreateTemporalInstant(zonedDateTime.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, zoned_date_time->nanoseconds()));

    // 6. Let calendar be zonedDateTime.[[Calendar]].
    auto& calendar = zoned_date_time->calendar();

    // 7. Let dateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    auto* date_time = TRY_OR_DISCARD(builtin_time_zone_get_plain_date_time_for(global_object, &time_zone, *instant, calendar));

    // 8. Let offset be ? BuiltinTimeZoneGetOffsetStringFor(timeZone, instant).
    auto offset = TRY_OR_DISCARD(builtin_time_zone_get_offset_string_for(global_object, &time_zone, *instant));

    // 9. Perform ! CreateDataPropertyOrThrow(fields, "calendar", calendar).
    MUST(fields->create_data_property_or_throw(vm.names.calendar, Value(&calendar)));

    // 10. Perform ! CreateDataPropertyOrThrow(fields, "isoDay", dateTime.[[ISODay]]).
    MUST(fields->create_data_property_or_throw(vm.names.isoDay, Value(date_time->iso_day())));

    // 11. Perform ! CreateDataPropertyOrThrow(fields, "isoHour", dateTime.[[ISOHour]]).
    MUST(fields->create_data_property_or_throw(vm.names.isoHour, Value(date_time->iso_hour())));

    // 12. Perform ! CreateDataPropertyOrThrow(fields, "isoMicrosecond", dateTime.[[ISOMicrosecond]]).
    MUST(fields->create_data_property_or_throw(vm.names.isoMicrosecond, Value(date_time->iso_microsecond())));

    // 13. Perform ! CreateDataPropertyOrThrow(fields, "isoMillisecond", dateTime.[[ISOMillisecond]]).
    MUST(fields->create_data_property_or_throw(vm.names.isoMillisecond, Value(date_time->iso_millisecond())));

    // 14. Perform ! CreateDataPropertyOrThrow(fields, "isoMinute", dateTime.[[ISOMinute]]).
    MUST(fields->create_data_property_or_throw(vm.names.isoMinute, Value(date_time->iso_minute())));

    // 15. Perform ! CreateDataPropertyOrThrow(fields, "isoMonth", dateTime.[[ISOMonth]]).
    MUST(fields->create_data_property_or_throw(vm.names.isoMonth, Value(date_time->iso_month())));

    // 16. Perform ! CreateDataPropertyOrThrow(fields, "isoNanosecond", dateTime.[[ISONanosecond]]).
    MUST(fields->create_data_property_or_throw(vm.names.isoNanosecond, Value(date_time->iso_nanosecond())));

    // 17. Perform ! CreateDataPropertyOrThrow(fields, "isoSecond", dateTime.[[ISOSecond]]).
    MUST(fields->create_data_property_or_throw(vm.names.isoSecond, Value(date_time->iso_second())));

    // 18. Perform ! CreateDataPropertyOrThrow(fields, "isoYear", dateTime.[[ISOYear]]).
    MUST(fields->create_data_property_or_throw(vm.names.isoYear, Value(date_time->iso_year())));

    // 19. Perform ! CreateDataPropertyOrThrow(fields, "offset", offset).
    MUST(fields->create_data_property_or_throw(vm.names.offset, js_string(vm, offset)));

    // 20. Perform ! CreateDataPropertyOrThrow(fields, "timeZone", timeZone).
    MUST(fields->create_data_property_or_throw(vm.names.timeZone, Value(&time_zone)));

    // 21. Return fields.
    return fields;
}

}
