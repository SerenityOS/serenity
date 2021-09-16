/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainDateTimePrototype.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>

namespace JS::Temporal {

// 5.3 Properties of the Temporal.PlainDateTime Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plaindatetime-prototype-object
PlainDateTimePrototype::PlainDateTimePrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void PlainDateTimePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 5.3.2 Temporal.PlainDateTime.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.PlainDateTime"), Attribute::Configurable);

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);
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
    define_native_accessor(vm.names.dayOfWeek, day_of_week_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.dayOfYear, day_of_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.weekOfYear, week_of_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInWeek, days_in_week_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInMonth, days_in_month_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInYear, days_in_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.monthsInYear, months_in_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.inLeapYear, in_leap_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.era, era_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.eraYear, era_year_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.withPlainTime, with_plain_time, 1, attr);
    define_native_function(vm.names.withPlainDate, with_plain_date, 1, attr);
    define_native_function(vm.names.withCalendar, with_calendar, 1, attr);
    define_native_function(vm.names.equals, equals, 1, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
    define_native_function(vm.names.toPlainDate, to_plain_date, 0, attr);
    define_native_function(vm.names.toPlainYearMonth, to_plain_year_month, 0, attr);
    define_native_function(vm.names.toPlainMonthDay, to_plain_month_day, 0, attr);
    define_native_function(vm.names.toPlainTime, to_plain_time, 0, attr);
    define_native_function(vm.names.getISOFields, get_iso_fields, 0, attr);
}

// 5.3.3 get Temporal.PlainDateTime.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::calendar_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return dateTime.[[Calendar]].
    return Value(&date_time->calendar());
}

// 5.3.4 get Temporal.PlainDateTime.prototype.year, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.year
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::year_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarYear(calendar, dateTime).
    return Value(TRY_OR_DISCARD(calendar_year(global_object, calendar, *date_time)));
}

// 5.3.5 get Temporal.PlainDateTime.prototype.month, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.month
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::month_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarMonth(calendar, dateTime).
    return Value(TRY_OR_DISCARD(calendar_month(global_object, calendar, *date_time)));
}

// 5.3.6 get Temporal.PlainDateTime.prototype.monthCode, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.monthcode
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::month_code_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarMonthCode(calendar, dateTime).
    return js_string(vm, TRY_OR_DISCARD(calendar_month_code(global_object, calendar, *date_time)));
}

// 5.3.7 get Temporal.PlainDateTime.prototype.day, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.day
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::day_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarDay(calendar, dateTime).
    return Value(TRY_OR_DISCARD(calendar_day(global_object, calendar, *date_time)));
}

// 5.3.8 get Temporal.PlainDateTime.prototype.hour, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.hour
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::hour_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return 𝔽(dateTime.[[ISOHour]]).
    return Value(date_time->iso_hour());
}

// 5.3.9 get Temporal.PlainDateTime.prototype.minute, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.minute
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::minute_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return 𝔽(dateTime.[[ISOMinute]]).
    return Value(date_time->iso_minute());
}

// 5.3.10 get Temporal.PlainDateTime.prototype.second, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.second
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::second_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return 𝔽(dateTime.[[ISOSecond]]).
    return Value(date_time->iso_second());
}

// 5.3.11 get Temporal.PlainDateTime.prototype.millisecond, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.millisecond
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::millisecond_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return 𝔽(dateTime.[[ISOMillisecond]]).
    return Value(date_time->iso_millisecond());
}

// 5.3.12 get Temporal.PlainDateTime.prototype.microsecond, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.microsecond
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::microsecond_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return 𝔽(dateTime.[[ISOMicrosecond]]).
    return Value(date_time->iso_microsecond());
}

// 5.3.13 get Temporal.PlainDateTime.prototype.nanosecond, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.nanosecond
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::nanosecond_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return 𝔽(dateTime.[[ISONanosecond]]).
    return Value(date_time->iso_nanosecond());
}

// 5.3.14 get Temporal.PlainDateTime.prototype.dayOfWeek, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.dayofweek
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::day_of_week_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarDayOfWeek(calendar, dateTime).
    return TRY_OR_DISCARD(calendar_day_of_week(global_object, calendar, *date_time));
}

// 5.3.15 get Temporal.PlainDateTime.prototype.dayOfYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.dayofyear
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::day_of_year_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarDayOfYear(calendar, dateTime).
    return TRY_OR_DISCARD(calendar_day_of_year(global_object, calendar, *date_time));
}

// 5.3.16 get Temporal.PlainDateTime.prototype.weekOfYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.weekofyear
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::week_of_year_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarWeekOfYear(calendar, dateTime).
    return TRY_OR_DISCARD(calendar_week_of_year(global_object, calendar, *date_time));
}

// 5.3.17 get Temporal.PlainDateTime.prototype.daysInWeek, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.daysinweek
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::days_in_week_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarDaysInWeek(calendar, dateTime).
    return TRY_OR_DISCARD(calendar_days_in_week(global_object, calendar, *date_time));
}

// 5.3.18 get Temporal.PlainDateTime.prototype.daysInMonth, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.daysinmonth
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::days_in_month_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarDaysInMonth(calendar, dateTime).
    return TRY_OR_DISCARD(calendar_days_in_month(global_object, calendar, *date_time));
}

// 5.3.19 get Temporal.PlainDateTime.prototype.daysInYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.daysinyear
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::days_in_year_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarDaysInYear(calendar, dateTime).
    return TRY_OR_DISCARD(calendar_days_in_year(global_object, calendar, *date_time));
}

// 5.3.20 get Temporal.PlainDateTime.prototype.monthsInYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.monthsinyear
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::months_in_year_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarMonthsInYear(calendar, dateTime).
    return TRY_OR_DISCARD(calendar_months_in_year(global_object, calendar, *date_time));
}

// 5.3.21 get Temporal.PlainDateTime.prototype.inLeapYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.inleapyear
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::in_leap_year_getter)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Return ? CalendarInLeapYear(calendar, dateTime).
    return TRY_OR_DISCARD(calendar_in_leap_year(global_object, calendar, *date_time));
}

// 15.6.6.2 get Temporal.PlainDateTime.prototype.era, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.era
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::era_getter)
{
    // 1. Let plainDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(plainDateTime, [[InitializedTemporalDateTime]]).
    auto* plain_date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be plainDateTime.[[Calendar]].
    auto& calendar = plain_date_time->calendar();

    // 4. Return ? CalendarEra(calendar, plainDateTime).
    return TRY_OR_DISCARD(calendar_era(global_object, calendar, *plain_date_time));
}

// 15.6.6.3 get Temporal.PlainDateTime.prototype.eraYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindatetime.prototype.erayear
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::era_year_getter)
{
    // 1. Let plainDateTime be the this value.
    // 2. Perform ? RequireInternalSlot(plainDateTime, [[InitializedTemporalDateTime]]).
    auto* plain_date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be plainDateTime.[[Calendar]].
    auto& calendar = plain_date_time->calendar();

    // 4. Return ? CalendarEraYear(calendar, plainDateTime).
    return TRY_OR_DISCARD(calendar_era_year(global_object, calendar, *plain_date_time));
}

// 5.3.23 Temporal.PlainDateTime.prototype.withPlainTime ( [ plainTimeLike ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.withplaintime
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::with_plain_time)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. If plainTimeLike is undefined, then
    if (vm.argument(0).is_undefined()) {
        // a. Return ? CreateTemporalDateTime(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], 0, 0, 0, 0, 0, 0, dateTime.[[Calendar]]).
        return create_temporal_date_time(global_object, date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), 0, 0, 0, 0, 0, 0, date_time->calendar());
    }

    // 4. Let plainTime be ? ToTemporalTime(plainTimeLike).
    auto* plain_time = TRY_OR_DISCARD(to_temporal_time(global_object, vm.argument(0)));

    // 5. Return ? CreateTemporalDateTime(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], plainTime.[[ISOHour]], plainTime.[[ISOMinute]], plainTime.[[ISOSecond]], plainTime.[[ISOMillisecond]], plainTime.[[ISOMicrosecond]], plainTime.[[ISONanosecond]], dateTime.[[Calendar]]).
    return create_temporal_date_time(global_object, date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), plain_time->iso_hour(), plain_time->iso_minute(), plain_time->iso_second(), plain_time->iso_millisecond(), plain_time->iso_microsecond(), plain_time->iso_nanosecond(), date_time->calendar());
}

// 5.3.24 Temporal.PlainDateTime.prototype.withPlainDate ( plainDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.withplaindate
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::with_plain_date)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let plainDate be ? ToTemporalDate(plainDateLike).
    auto* plain_date = to_temporal_date(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. Let calendar be ? ConsolidateCalendars(dateTime.[[Calendar]], plainDate.[[Calendar]]).
    auto* calendar = TRY_OR_DISCARD(consolidate_calendars(global_object, date_time->calendar(), plain_date->calendar()));

    // 5. Return ? CreateTemporalDateTime(plainDate.[[ISOYear]], plainDate.[[ISOMonth]], plainDate.[[ISODay]], dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]], calendar).
    return create_temporal_date_time(global_object, plain_date->iso_year(), plain_date->iso_month(), plain_date->iso_day(), date_time->iso_hour(), date_time->iso_minute(), date_time->iso_second(), date_time->iso_millisecond(), date_time->iso_microsecond(), date_time->iso_nanosecond(), *calendar);
}

// 5.3.25 Temporal.PlainDateTime.prototype.withCalendar ( calendar ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.withcalendar
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::with_calendar)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be ? ToTemporalCalendar(calendar).
    auto* calendar = TRY_OR_DISCARD(to_temporal_calendar(global_object, vm.argument(0)));
    if (vm.exception())
        return {};

    // 4. Return ? CreateTemporalDateTime(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]], calendar).
    return create_temporal_date_time(global_object, date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), date_time->iso_hour(), date_time->iso_minute(), date_time->iso_second(), date_time->iso_millisecond(), date_time->iso_microsecond(), date_time->iso_nanosecond(), *calendar);
}

// 5.3.31 Temporal.PlainDateTime.prototype.equals ( other ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.equals
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::equals)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Set other to ? ToTemporalDateTime(other).
    auto* other = to_temporal_date_time(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. Let result be ! CompareISODateTime(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]], other.[[ISOYear]], other.[[ISOMonth]], other.[[ISODay]], other.[[ISOHour]], other.[[ISOMinute]], other.[[ISOSecond]], other.[[ISOMillisecond]], other.[[ISOMicrosecond]], other.[[ISONanosecond]]).
    auto result = compare_iso_date_time(date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), date_time->iso_hour(), date_time->iso_minute(), date_time->iso_second(), date_time->iso_millisecond(), date_time->iso_microsecond(), date_time->iso_nanosecond(), other->iso_year(), other->iso_month(), other->iso_day(), other->iso_hour(), other->iso_minute(), other->iso_second(), other->iso_millisecond(), other->iso_microsecond(), other->iso_nanosecond());

    // 5. If result is not 0, return false.
    if (result != 0)
        return Value(false);

    // 6. Return ? CalendarEquals(dateTime.[[Calendar]], other.[[Calendar]]).
    return Value(TRY_OR_DISCARD(calendar_equals(global_object, date_time->calendar(), other->calendar())));
}

// 5.3.35 Temporal.PlainDateTime.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainDateTime", "a primitive value");
    return {};
}

// 5.3.37 Temporal.PlainDateTime.prototype.toPlainDate ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.toplaindate
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::to_plain_date)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return ? CreateTemporalDate(dateTime.[[ISOYear]], dateTime.[[ISOMonth]], dateTime.[[ISODay]], dateTime.[[Calendar]]).
    return create_temporal_date(global_object, date_time->iso_year(), date_time->iso_month(), date_time->iso_day(), date_time->calendar());
}

// 5.3.38 Temporal.PlainDateTime.prototype.toPlainYearMonth ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.toplainyearmonth
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::to_plain_year_month)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Let fieldNames be ? CalendarFields(calendar, « "monthCode", "year" »).
    auto field_names = TRY_OR_DISCARD(calendar_fields(global_object, calendar, { "monthCode"sv, "year"sv }));

    // 5. Let fields be ? PrepareTemporalFields(dateTime, fieldNames, «»).
    auto* fields = TRY_OR_DISCARD(prepare_temporal_fields(global_object, *date_time, field_names, {}));

    // 6. Return ? YearMonthFromFields(calendar, fields).
    return TRY_OR_DISCARD(year_month_from_fields(global_object, calendar, *fields));
}

// 5.3.39 Temporal.PlainDateTime.prototype.toPlainMonthDay ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.toplainmonthday
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::to_plain_month_day)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be dateTime.[[Calendar]].
    auto& calendar = date_time->calendar();

    // 4. Let fieldNames be ? CalendarFields(calendar, « "day", "monthCode" »).
    auto field_names = TRY_OR_DISCARD(calendar_fields(global_object, calendar, { "day"sv, "monthCode"sv }));

    // 5. Let fields be ? PrepareTemporalFields(dateTime, fieldNames, «»).
    auto* fields = TRY_OR_DISCARD(prepare_temporal_fields(global_object, *date_time, field_names, {}));

    // 6. Return ? MonthDayFromFields(calendar, fields).
    return TRY_OR_DISCARD(month_day_from_fields(global_object, calendar, *fields));
}

// 5.3.40 Temporal.PlainDateTime.prototype.toPlainTime ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.toplaintime
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::to_plain_time)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return ? CreateTemporalTime(dateTime.[[ISOHour]], dateTime.[[ISOMinute]], dateTime.[[ISOSecond]], dateTime.[[ISOMillisecond]], dateTime.[[ISOMicrosecond]], dateTime.[[ISONanosecond]]).
    return TRY_OR_DISCARD(create_temporal_time(global_object, date_time->iso_hour(), date_time->iso_minute(), date_time->iso_second(), date_time->iso_millisecond(), date_time->iso_microsecond(), date_time->iso_nanosecond()));
}

// 5.3.41 Temporal.PlainDateTime.prototype.getISOFields ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindatetime.prototype.getisofields
JS_DEFINE_NATIVE_FUNCTION(PlainDateTimePrototype::get_iso_fields)
{
    // 1. Let dateTime be the this value.
    // 2. Perform ? RequireInternalSlot(dateTime, [[InitializedTemporalDateTime]]).
    auto* date_time = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let fields be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* fields = Object::create(global_object, global_object.object_prototype());

    // 4. Perform ! CreateDataPropertyOrThrow(fields, "calendar", dateTime.[[Calendar]]).
    fields->create_data_property_or_throw(vm.names.calendar, Value(&date_time->calendar()));

    // 5. Perform ! CreateDataPropertyOrThrow(fields, "isoDay", 𝔽(dateTime.[[ISODay]])).
    fields->create_data_property_or_throw(vm.names.isoDay, Value(date_time->iso_day()));

    // 6. Perform ! CreateDataPropertyOrThrow(fields, "isoHour", 𝔽(dateTime.[[ISOHour]])).
    fields->create_data_property_or_throw(vm.names.isoHour, Value(date_time->iso_hour()));

    // 7. Perform ! CreateDataPropertyOrThrow(fields, "isoMicrosecond", 𝔽(dateTime.[[ISOMicrosecond]])).
    fields->create_data_property_or_throw(vm.names.isoMicrosecond, Value(date_time->iso_microsecond()));

    // 8. Perform ! CreateDataPropertyOrThrow(fields, "isoMillisecond", 𝔽(dateTime.[[ISOMillisecond]])).
    fields->create_data_property_or_throw(vm.names.isoMillisecond, Value(date_time->iso_millisecond()));

    // 9. Perform ! CreateDataPropertyOrThrow(fields, "isoMinute", 𝔽(dateTime.[[ISOMinute]])).
    fields->create_data_property_or_throw(vm.names.isoMinute, Value(date_time->iso_minute()));

    // 10. Perform ! CreateDataPropertyOrThrow(fields, "isoMonth", 𝔽(dateTime.[[ISOMonth]])).
    fields->create_data_property_or_throw(vm.names.isoMonth, Value(date_time->iso_month()));

    // 11. Perform ! CreateDataPropertyOrThrow(fields, "isoNanosecond", 𝔽(dateTime.[[ISONanosecond]])).
    fields->create_data_property_or_throw(vm.names.isoNanosecond, Value(date_time->iso_nanosecond()));

    // 12. Perform ! CreateDataPropertyOrThrow(fields, "isoSecond", 𝔽(dateTime.[[ISOSecond]])).
    fields->create_data_property_or_throw(vm.names.isoSecond, Value(date_time->iso_second()));

    // 13. Perform ! CreateDataPropertyOrThrow(fields, "isoYear", 𝔽(dateTime.[[ISOYear]])).
    fields->create_data_property_or_throw(vm.names.isoYear, Value(date_time->iso_year()));

    // 14. Return fields.
    return fields;
}

}
