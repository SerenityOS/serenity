/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/Now.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <time.h>

namespace JS::Temporal {

// 2 The Temporal.Now Object, https://tc39.es/proposal-temporal/#sec-temporal-now-object
Now::Now(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void Now::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 2.1.1 Temporal.Now [ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal-now-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.Now"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.timeZone, time_zone, 0, attr);
    define_native_function(vm.names.instant, instant, 0, attr);
    define_native_function(vm.names.plainDateTime, plain_date_time, 1, attr);
    define_native_function(vm.names.plainDateTimeISO, plain_date_time_iso, 0, attr);
    define_native_function(vm.names.zonedDateTime, zoned_date_time, 1, attr);
    define_native_function(vm.names.zonedDateTimeISO, zoned_date_time_iso, 0, attr);
    define_native_function(vm.names.plainDate, plain_date, 1, attr);
    define_native_function(vm.names.plainDateISO, plain_date_iso, 0, attr);
    define_native_function(vm.names.plainTimeISO, plain_time_iso, 0, attr);
}

// 2.2.1 Temporal.Now.timeZone ( ), https://tc39.es/proposal-temporal/#sec-temporal.now.timezone
JS_DEFINE_NATIVE_FUNCTION(Now::time_zone)
{
    // 1. Return ! SystemTimeZone().
    return system_time_zone(global_object);
}

// 2.2.2 Temporal.Now.instant ( ), https://tc39.es/proposal-temporal/#sec-temporal.now.instant
JS_DEFINE_NATIVE_FUNCTION(Now::instant)
{
    // 1. Return ! SystemInstant().
    return system_instant(global_object);
}

// 2.2.3 Temporal.Now.plainDateTime ( calendarLike [ , temporalTimeZoneLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.now.plaindatetime
JS_DEFINE_NATIVE_FUNCTION(Now::plain_date_time)
{
    auto calendar_like = vm.argument(0);
    auto temporal_time_zone_like = vm.argument(1);

    // 1. Return ? SystemDateTime(temporalTimeZoneLike, calendarLike).
    return TRY(system_date_time(global_object, temporal_time_zone_like, calendar_like));
}

// 2.2.4 Temporal.Now.plainDateTimeISO ( [ temporalTimeZoneLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.now.plaindatetimeiso
JS_DEFINE_NATIVE_FUNCTION(Now::plain_date_time_iso)
{
    auto temporal_time_zone_like = vm.argument(0);

    // 1, Let calendar be ! GetISO8601Calendar().
    auto* calendar = get_iso8601_calendar(global_object);

    // 2. Return ? SystemDateTime(temporalTimeZoneLike, calendar).
    return TRY(system_date_time(global_object, temporal_time_zone_like, calendar));
}

// 2.2.5 Temporal.Now.zonedDateTime ( calendarLike [ , temporalTimeZoneLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.now.zoneddatetime
JS_DEFINE_NATIVE_FUNCTION(Now::zoned_date_time)
{
    auto calendar_like = vm.argument(0);
    auto temporal_time_zone_like = vm.argument(1);

    // 1. Return ? SystemZonedDateTime(temporalTimeZoneLike, calendarLike).
    return TRY(system_zoned_date_time(global_object, temporal_time_zone_like, calendar_like));
}

// 2.2.6 Temporal.Now.zonedDateTimeISO ( [ temporalTimeZoneLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.now.zoneddatetimeiso
JS_DEFINE_NATIVE_FUNCTION(Now::zoned_date_time_iso)
{
    auto temporal_time_zone_like = vm.argument(0);

    // 1, Let calendar be ! GetISO8601Calendar().
    auto* calendar = get_iso8601_calendar(global_object);

    // 2. Return ? SystemZonedDateTime(temporalTimeZoneLike, calendar).
    return TRY(system_zoned_date_time(global_object, temporal_time_zone_like, calendar));
}

// 2.2.7 Temporal.Now.plainDate ( calendarLike [ , temporalTimeZoneLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.now.plaindate
JS_DEFINE_NATIVE_FUNCTION(Now::plain_date)
{
    auto calendar_like = vm.argument(0);
    auto temporal_time_zone_like = vm.argument(1);

    // 1. Let dateTime be ? SystemDateTime(temporalTimeZoneLike, calendarLike).
    auto* date_time = TRY(system_date_time(global_object, temporal_time_zone_like, calendar_like));

    // 2. Return ! CreateTemporalDate(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[Calendar]]).
    return TRY(create_temporal_date(global_object, date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), date_time->calendar()));
}

// 2.2.8 Temporal.Now.plainDateISO ( [ temporalTimeZoneLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.now.plaindateiso
JS_DEFINE_NATIVE_FUNCTION(Now::plain_date_iso)
{
    auto temporal_time_zone_like = vm.argument(0);

    // 1. Let calendar be ! GetISO8601Calendar().
    auto* calendar = get_iso8601_calendar(global_object);

    // 2. Let dateTime be ? SystemDateTime(temporalTimeZoneLike, calendar).
    auto* date_time = TRY(system_date_time(global_object, temporal_time_zone_like, calendar));

    // 3. Return ! CreateTemporalDate(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[Calendar]]).
    return TRY(create_temporal_date(global_object, date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), date_time->calendar()));
}

// 2.2.9 Temporal.Now.plainTimeISO ( [ temporalTimeZoneLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.now.plaintimeiso
JS_DEFINE_NATIVE_FUNCTION(Now::plain_time_iso)
{
    auto temporal_time_zone_like = vm.argument(0);

    // 1. Let calendar be ! GetISO8601Calendar().
    auto* calendar = get_iso8601_calendar(global_object);

    // 2. Let dateTime be ? SystemDateTime(temporalTimeZoneLike, calendar).
    auto* date_time = TRY(system_date_time(global_object, temporal_time_zone_like, calendar));

    // 3. Return ! CreateTemporalTime(dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]]).
    return MUST(create_temporal_time(global_object, date_time->iso_hour(), date_time->iso_minute(), date_time->iso_second(), date_time->iso_millisecond(), date_time->iso_microsecond(), date_time->iso_nanosecond()));
}

// 2.3.1 SystemTimeZone ( ), https://tc39.es/proposal-temporal/#sec-temporal-systemtimezone
TimeZone* system_time_zone(GlobalObject& global_object)
{
    // 1. Let identifier be ! DefaultTimeZone().
    auto identifier = default_time_zone();

    // 2. Return ! CreateTemporalTimeZone(identifier).
    return MUST(create_temporal_time_zone(global_object, identifier));
}

// 2.3.2 SystemUTCEpochNanoseconds ( ), https://tc39.es/proposal-temporal/#sec-temporal-systemutcepochnanoseconds
BigInt* system_utc_epoch_nanoseconds(GlobalObject& global_object)
{
    // 1. Let ns be the approximate current UTC date and time, in nanoseconds since the epoch.
    struct timespec now;
    clock_gettime(CLOCK_REALTIME, &now);
    Checked<i64> ns_timestamp;
    ns_timestamp += now.tv_sec;
    ns_timestamp *= 1'000'000'000;
    ns_timestamp += now.tv_nsec;
    if (ns_timestamp.has_overflow()) {
        // TODO: Deal with this before 2262-04-21T00:47:16Z.
        VERIFY_NOT_REACHED();
    }
    auto ns = Crypto::SignedBigInteger::create_from(ns_timestamp.value());

    // 2. Set ns to the result of clamping ns between −8.64 × 10^21 and 8.64 × 10^21.
    // Uhh, these don't even fit in an i64... ¯\_(ツ)_/¯

    // 3. Return ℤ(ns).
    return js_bigint(global_object.heap(), move(ns));
}

// 2.3.3 SystemInstant ( ), https://tc39.es/proposal-temporal/#sec-temporal-systeminstant
Instant* system_instant(GlobalObject& global_object)
{
    // 1. Let ns be ! SystemUTCEpochNanoseconds().
    auto* ns = system_utc_epoch_nanoseconds(global_object);

    // 2. Return ! CreateTemporalInstant(ns).
    return MUST(create_temporal_instant(global_object, *ns));
}

// 2.3.4 SystemDateTime ( temporalTimeZoneLike, calendarLike ), https://tc39.es/proposal-temporal/#sec-temporal-systemdatetime
ThrowCompletionOr<PlainDateTime*> system_date_time(GlobalObject& global_object, Value temporal_time_zone_like, Value calendar_like)
{
    Object* time_zone;

    // 1. If temporalTimeZoneLike is undefined, then
    if (temporal_time_zone_like.is_undefined()) {
        // a. Let timeZone be ! SystemTimeZone().
        time_zone = system_time_zone(global_object);
    }
    // 2. Else,
    else {
        // a. Let timeZone be ? ToTemporalTimeZone(temporalTimeZoneLike).
        time_zone = TRY(to_temporal_time_zone(global_object, temporal_time_zone_like));
    }

    // 3. Let calendar be ? ToTemporalCalendar(calendarLike).
    auto* calendar = TRY(to_temporal_calendar(global_object, calendar_like));

    // 4. Let instant be ! SystemInstant().
    auto* instant = system_instant(global_object);

    // 5. Return ? BuiltinTimeZoneGetPlainDateTimeFor(timeZone, instant, calendar).
    return builtin_time_zone_get_plain_date_time_for(global_object, time_zone, *instant, *calendar);
}

// 2.3.5 SystemZonedDateTime ( temporalTimeZoneLike, calendarLike ), https://tc39.es/proposal-temporal/#sec-temporal-systemzoneddatetime
ThrowCompletionOr<ZonedDateTime*> system_zoned_date_time(GlobalObject& global_object, Value temporal_time_zone_like, Value calendar_like)
{
    Object* time_zone;

    // 1. If temporalTimeZoneLike is undefined, then
    if (temporal_time_zone_like.is_undefined()) {
        // a. Let timeZone be ! SystemTimeZone().
        time_zone = system_time_zone(global_object);
    }
    // 2. Else,
    else {
        // a. Let timeZone be ? ToTemporalTimeZone(temporalTimeZoneLike).
        time_zone = TRY(to_temporal_time_zone(global_object, temporal_time_zone_like));
    }

    // 3. Let calendar be ? ToTemporalCalendar(calendarLike).
    auto* calendar = TRY(to_temporal_calendar(global_object, calendar_like));

    // 4. Let ns be ! SystemUTCEpochNanoseconds().
    auto* ns = system_utc_epoch_nanoseconds(global_object);

    // 5. Return ? CreateTemporalZonedDateTime(ns, timeZone, calendar).
    return create_temporal_zoned_date_time(global_object, *ns, *time_zone, *calendar);
}

}
