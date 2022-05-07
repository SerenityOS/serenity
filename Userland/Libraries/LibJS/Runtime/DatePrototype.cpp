/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Petróczi Zoltán <petroczizoltan@tutanota.com>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/DateConstants.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <AK/TypeCasts.h>
#include <LibCore/DateTime.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/DatePrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DateTimeFormat.h>
#include <LibJS/Runtime/Intl/DateTimeFormatConstructor.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Value.h>
#include <LibTimeZone/TimeZone.h>
#include <LibUnicode/DateTimeFormat.h>
#include <LibUnicode/Locale.h>

namespace JS {

DatePrototype::DatePrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void DatePrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.getDate, get_date, 0, attr);
    define_native_function(vm.names.getDay, get_day, 0, attr);
    define_native_function(vm.names.getFullYear, get_full_year, 0, attr);
    define_native_function(vm.names.getHours, get_hours, 0, attr);
    define_native_function(vm.names.getMilliseconds, get_milliseconds, 0, attr);
    define_native_function(vm.names.getMinutes, get_minutes, 0, attr);
    define_native_function(vm.names.getMonth, get_month, 0, attr);
    define_native_function(vm.names.getSeconds, get_seconds, 0, attr);
    define_native_function(vm.names.getTime, get_time, 0, attr);
    define_native_function(vm.names.getTimezoneOffset, get_timezone_offset, 0, attr);
    define_native_function(vm.names.getUTCDate, get_utc_date, 0, attr);
    define_native_function(vm.names.getUTCDay, get_utc_day, 0, attr);
    define_native_function(vm.names.getUTCFullYear, get_utc_full_year, 0, attr);
    define_native_function(vm.names.getUTCHours, get_utc_hours, 0, attr);
    define_native_function(vm.names.getUTCMilliseconds, get_utc_milliseconds, 0, attr);
    define_native_function(vm.names.getUTCMinutes, get_utc_minutes, 0, attr);
    define_native_function(vm.names.getUTCMonth, get_utc_month, 0, attr);
    define_native_function(vm.names.getUTCSeconds, get_utc_seconds, 0, attr);
    define_native_function(vm.names.setDate, set_date, 1, attr);
    define_native_function(vm.names.setFullYear, set_full_year, 3, attr);
    define_native_function(vm.names.setHours, set_hours, 4, attr);
    define_native_function(vm.names.setMilliseconds, set_milliseconds, 1, attr);
    define_native_function(vm.names.setMinutes, set_minutes, 3, attr);
    define_native_function(vm.names.setMonth, set_month, 2, attr);
    define_native_function(vm.names.setSeconds, set_seconds, 2, attr);
    define_native_function(vm.names.setTime, set_time, 1, attr);
    define_native_function(vm.names.setUTCDate, set_utc_date, 1, attr);
    define_native_function(vm.names.setUTCFullYear, set_utc_full_year, 3, attr);
    define_native_function(vm.names.setUTCHours, set_utc_hours, 4, attr);
    define_native_function(vm.names.setUTCMilliseconds, set_utc_milliseconds, 1, attr);
    define_native_function(vm.names.setUTCMinutes, set_utc_minutes, 3, attr);
    define_native_function(vm.names.setUTCMonth, set_utc_month, 2, attr);
    define_native_function(vm.names.setUTCSeconds, set_utc_seconds, 2, attr);
    define_native_function(vm.names.toDateString, to_date_string, 0, attr);
    define_native_function(vm.names.toISOString, to_iso_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 1, attr);
    define_native_function(vm.names.toLocaleDateString, to_locale_date_string, 0, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.toLocaleTimeString, to_locale_time_string, 0, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toTemporalInstant, to_temporal_instant, 0, attr);
    define_native_function(vm.names.toTimeString, to_time_string, 0, attr);
    define_native_function(vm.names.toUTCString, to_utc_string, 0, attr);

    define_native_function(vm.names.getYear, get_year, 0, attr);
    define_native_function(vm.names.setYear, set_year, 1, attr);

    // 21.4.4.45 Date.prototype [ @@toPrimitive ] ( hint ), https://tc39.es/ecma262/#sec-date.prototype-@@toprimitive
    define_native_function(*vm.well_known_symbol_to_primitive(), symbol_to_primitive, 1, Attribute::Configurable);

    // Aliases.
    define_native_function(vm.names.valueOf, get_time, 0, attr);

    // B.2.4.3 Date.prototype.toGMTString ( ), https://tc39.es/ecma262/#sec-date.prototype.togmtstring
    // The initial value of the "toGMTString" property is %Date.prototype.toUTCString%, defined in 21.4.4.43.
    define_direct_property(vm.names.toGMTString, get_without_side_effects(vm.names.toUTCString), attr);
}

// thisTimeValue ( value ), https://tc39.es/ecma262/#thistimevalue
ThrowCompletionOr<double> this_time_value(GlobalObject& global_object, Value value)
{
    // 1. If Type(value) is Object and value has a [[DateValue]] internal slot, then
    if (value.is_object() && is<Date>(value.as_object())) {
        // a. Return value.[[DateValue]].
        return static_cast<Date&>(value.as_object()).date_value();
    }

    // 2. Throw a TypeError exception.
    auto& vm = global_object.vm();
    return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Date");
}

// 21.4.4.2 Date.prototype.getDate ( ), https://tc39.es/ecma262/#sec-date.prototype.getdate
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_date)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return DateFromTime(LocalTime(t)).
    return Value(date_from_time(local_time(time)));
}

// 21.4.4.3 Date.prototype.getDay ( ), https://tc39.es/ecma262/#sec-date.prototype.getday
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_day)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return WeekDay(LocalTime(t)).
    return Value(week_day(local_time(time)));
}

// 21.4.4.4 Date.prototype.getFullYear ( ), https://tc39.es/ecma262/#sec-date.prototype.getfullyear
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_full_year)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return YearFromTime(LocalTime(t)).
    return Value(year_from_time(local_time(time)));
}

// 21.4.4.5 Date.prototype.getHours ( ), https://tc39.es/ecma262/#sec-date.prototype.gethours
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_hours)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return HourFromTime(LocalTime(t)).
    return Value(hour_from_time(local_time(time)));
}

// 21.4.4.6 Date.prototype.getMilliseconds ( ), https://tc39.es/ecma262/#sec-date.prototype.getmilliseconds
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_milliseconds)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return msFromTime(LocalTime(t)).
    return Value(ms_from_time(local_time(time)));
}

// 21.4.4.7 Date.prototype.getMinutes ( ), https://tc39.es/ecma262/#sec-date.prototype.getminutes
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_minutes)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return MinFromTime(LocalTime(t)).
    return Value(min_from_time(local_time(time)));
}

// 21.4.4.8 Date.prototype.getMonth ( ), https://tc39.es/ecma262/#sec-date.prototype.getmonth
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_month)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return MonthFromTime(LocalTime(t)).
    return Value(month_from_time(local_time(time)));
}

// 21.4.4.9 Date.prototype.getSeconds ( ), https://tc39.es/ecma262/#sec-date.prototype.getseconds
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_seconds)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return SecFromTime(LocalTime(t)).
    return Value(sec_from_time(local_time(time)));
}

// 21.4.4.10 Date.prototype.getTime ( ), https://tc39.es/ecma262/#sec-date.prototype.gettime
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_time)
{
    // 1. Return ? thisTimeValue(this value).
    return Value(TRY(this_time_value(global_object, vm.this_value(global_object))));
}

// 21.4.4.11 Date.prototype.getTimezoneOffset ( ), https://tc39.es/ecma262/#sec-date.prototype.gettimezoneoffset
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_timezone_offset)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return (t - LocalTime(t)) / msPerMinute.
    return Value((time - local_time(time)) / ms_per_minute);
}

// 21.4.4.12 Date.prototype.getUTCDate ( ), https://tc39.es/ecma262/#sec-date.prototype.getutcdate
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_date)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return DateFromTime(t).
    return Value(date_from_time(time));
}

// 21.4.4.13 Date.prototype.getUTCDay ( ), https://tc39.es/ecma262/#sec-date.prototype.getutcday
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_day)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return WeekDay(t).
    return Value(week_day(time));
}

// 21.4.4.14 Date.prototype.getUTCFullYear ( ), https://tc39.es/ecma262/#sec-date.prototype.getutcfullyear
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_full_year)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return YearFromTime(t).
    return Value(year_from_time(time));
}

// 21.4.4.15 Date.prototype.getUTCHours ( ), https://tc39.es/ecma262/#sec-date.prototype.getutchours
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_hours)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return HourFromTime(t).
    return Value(hour_from_time(time));
}

// 21.4.4.16 Date.prototype.getUTCMilliseconds ( ), https://tc39.es/ecma262/#sec-date.prototype.getutcmilliseconds
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_milliseconds)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return msFromTime(t).
    return Value(ms_from_time(time));
}

// 21.4.4.17 Date.prototype.getUTCMinutes ( ), https://tc39.es/ecma262/#sec-date.prototype.getutcminutes
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_minutes)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return MinFromTime(t).
    return Value(min_from_time(time));
}

// 21.4.4.18 Date.prototype.getUTCMonth ( ), https://tc39.es/ecma262/#sec-date.prototype.getutcmonth
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_month)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return MonthFromTime(t).
    return month_from_time(time);
}

// 21.4.4.19 Date.prototype.getUTCSeconds ( ), https://tc39.es/ecma262/#sec-date.prototype.getutcseconds
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_seconds)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return SecFromTime(t).
    return Value(sec_from_time(time));
}

static ThrowCompletionOr<double> argument_or_number(GlobalObject& global_object, size_t index, double fallback)
{
    auto& vm = global_object.vm();

    if (vm.argument_count() > index)
        return TRY(vm.argument(index).to_number(global_object)).as_double();

    return fallback;
}

static ThrowCompletionOr<Optional<double>> argument_or_empty(GlobalObject& global_object, size_t index)
{
    auto& vm = global_object.vm();

    if (vm.argument_count() > index)
        return TRY(vm.argument(index).to_number(global_object)).as_double();

    return Optional<double> {};
}

// 21.4.4.20 Date.prototype.setDate ( date ), https://tc39.es/ecma262/#sec-date.prototype.setdate
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_date)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto this_time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let dt be ? ToNumber(date).
    auto date = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If t is NaN, return NaN.
    if (isnan(this_time))
        return js_nan();

    // 4. Set t to LocalTime(t).
    auto time = local_time(this_time);

    // 5. Let newDate be MakeDate(MakeDay(YearFromTime(t), MonthFromTime(t), dt), TimeWithinDay(t)).
    auto year = year_from_time(time);
    auto month = month_from_time(time);

    auto day = make_day(year, month, date);
    auto new_date = make_date(day, time_within_day(time));

    // 6. Let u be TimeClip(UTC(newDate)).
    new_date = time_clip(utc_time(new_date));

    // 7. Set the [[DateValue]] internal slot of this Date object to u.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(new_date);

    // 8. Return u.
    return Value(new_date);
}

// 21.4.4.21 Date.prototype.setFullYear ( year [ , month [ , date ] ] ), https://tc39.es/ecma262/#sec-date.prototype.setfullyear
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_full_year)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto this_time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let y be ? ToNumber(year).
    auto year = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If t is NaN, set t to +0𝔽; otherwise, set t to LocalTime(t).
    double time = 0;
    if (!isnan(this_time))
        time = local_time(this_time);

    // 4. If month is not present, let m be MonthFromTime(t); otherwise, let m be ? ToNumber(month).
    auto month = TRY(argument_or_number(global_object, 1, month_from_time(time)));

    // 5. If date is not present, let dt be DateFromTime(t); otherwise, let dt be ? ToNumber(date).
    auto date = TRY(argument_or_number(global_object, 2, date_from_time(time)));

    // 6. Let newDate be MakeDate(MakeDay(y, m, dt), TimeWithinDay(t)).
    auto day = make_day(year, month, date);
    auto new_date = make_date(day, time_within_day(time));

    // 7. Let u be TimeClip(UTC(newDate)).
    new_date = time_clip(utc_time(new_date));

    // 8. Set the [[DateValue]] internal slot of this Date object to u.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(new_date);

    // 9. Return u.
    return Value(new_date);
}

// 21.4.4.22 Date.prototype.setHours ( hour [ , min [ , sec [ , ms ] ] ] ), https://tc39.es/ecma262/#sec-date.prototype.sethours
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_hours)
{
    // 1. Let t be LocalTime(? thisTimeValue(this value)).
    auto this_time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let h be ? ToNumber(hour).
    auto hour = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If min is present, let m be ? ToNumber(min).
    auto minute = TRY(argument_or_empty(global_object, 1));

    // 4. If sec is present, let s be ? ToNumber(sec).
    auto second = TRY(argument_or_empty(global_object, 2));

    // 5. If ms is present, let milli be ? ToNumber(ms).
    auto millisecond = TRY(argument_or_empty(global_object, 3));

    // 6. If t is NaN, return NaN.
    if (isnan(this_time))
        return js_nan();

    // 7. Set t to LocalTime(t).
    auto time = local_time(this_time);

    // 8. If min is not present, let m be MinFromTime(t).
    if (!minute.has_value())
        minute = min_from_time(time);

    // 9. If sec is not present, let s be SecFromTime(t).
    if (!second.has_value())
        second = sec_from_time(time);

    // 10. If ms is not present, let milli be msFromTime(t).
    if (!millisecond.has_value())
        millisecond = ms_from_time(time);

    // 11. Let date be MakeDate(Day(t), MakeTime(h, m, s, milli)).
    auto new_time = make_time(hour, *minute, *second, *millisecond);
    auto date = make_date(day(time), new_time);

    // 12. Let u be TimeClip(UTC(date)).
    date = time_clip(utc_time(date));

    // 13. Set the [[DateValue]] internal slot of this Date object to u.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(date);

    // 14. Return u.
    return Value(date);
}

// 21.4.4.23 Date.prototype.setMilliseconds ( ms ), https://tc39.es/ecma262/#sec-date.prototype.setmilliseconds
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_milliseconds)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto this_time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Set ms to ? ToNumber(ms).
    auto millisecond = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If t is NaN, return NaN.
    if (isnan(this_time))
        return js_nan();

    // 4. Set t to LocalTime(t).
    auto time = local_time(this_time);

    // 5. Let time be MakeTime(HourFromTime(t), MinFromTime(t), SecFromTime(t), ms).
    auto hour = hour_from_time(time);
    auto minute = min_from_time(time);
    auto second = sec_from_time(time);

    auto new_time = make_time(hour, minute, second, millisecond);

    // 6. Let u be TimeClip(UTC(MakeDate(Day(t), time))).
    auto date = make_date(day(time), new_time);
    date = time_clip(utc_time(date));

    // 7. Set the [[DateValue]] internal slot of this Date object to u.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(date);

    // 8. Return u.
    return Value(date);
}

// 21.4.4.24 Date.prototype.setMinutes ( min [ , sec [ , ms ] ] ), https://tc39.es/ecma262/#sec-date.prototype.setminutes
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_minutes)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto this_time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let m be ? ToNumber(min).
    auto minute = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If sec is present, let s be ? ToNumber(sec).
    auto second = TRY(argument_or_empty(global_object, 1));

    // 4. If ms is present, let milli be ? ToNumber(ms).
    auto millisecond = TRY(argument_or_empty(global_object, 2));

    // 5. If t is NaN, return NaN.
    if (isnan(this_time))
        return js_nan();

    // 6. Set t to LocalTime(t).
    auto time = local_time(this_time);

    // 7. If sec is not present, let s be SecFromTime(t).
    if (!second.has_value())
        second = sec_from_time(time);

    // 8. If ms is not present, let milli be msFromTime(t).
    if (!millisecond.has_value())
        millisecond = ms_from_time(time);

    // 9. Let date be MakeDate(Day(t), MakeTime(HourFromTime(t), m, s, milli)).
    auto hour = hour_from_time(time);

    auto new_time = make_time(hour, minute, *second, *millisecond);
    auto date = make_date(day(time), new_time);

    // 10. Let u be TimeClip(UTC(date)).
    date = time_clip(utc_time(date));

    // 11. Set the [[DateValue]] internal slot of this Date object to u.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(date);

    // 12. Return u.
    return Value(date);
}

// 21.4.4.25 Date.prototype.setMonth ( month [ , date ] ), https://tc39.es/ecma262/#sec-date.prototype.setmonth
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_month)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto this_time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let m be ? ToNumber(month).
    auto month = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If date is present, let dt be ? ToNumber(date).
    auto date = TRY(argument_or_empty(global_object, 1));

    // 4. If t is NaN, return NaN.
    if (isnan(this_time))
        return js_nan();

    // 5. Set t to LocalTime(t).
    auto time = local_time(this_time);

    // 6. If date is not present, let dt be DateFromTime(t).
    if (!date.has_value())
        date = date_from_time(time);

    // 7. Let newDate be MakeDate(MakeDay(YearFromTime(t), m, dt), TimeWithinDay(t)).
    auto year = year_from_time(time);

    auto day = make_day(year, month, *date);
    auto new_date = make_date(day, time_within_day(time));

    // 8. Let u be TimeClip(UTC(newDate)).
    new_date = time_clip(utc_time(new_date));

    // 9. Set the [[DateValue]] internal slot of this Date object to u.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(new_date);

    // 10. Return u.
    return Value(new_date);
}

// 21.4.4.26 Date.prototype.setSeconds ( sec [ , ms ] ), https://tc39.es/ecma262/#sec-date.prototype.setseconds
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_seconds)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto this_time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let s be ? ToNumber(sec).
    auto second = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If ms is present, let milli be ? ToNumber(ms).
    auto millisecond = TRY(argument_or_empty(global_object, 1));

    // 4. If t is NaN, return NaN.
    if (isnan(this_time))
        return js_nan();

    // 5. Set t to LocalTime(t).
    auto time = local_time(this_time);

    // 6. If ms is not present, let milli be msFromTime(t).
    if (!millisecond.has_value())
        millisecond = ms_from_time(time);

    // 7. Let date be MakeDate(Day(t), MakeTime(HourFromTime(t), MinFromTime(t), s, milli)).
    auto hour = hour_from_time(time);
    auto minute = min_from_time(time);

    auto new_time = make_time(hour, minute, second, *millisecond);
    auto new_date = make_date(day(time), new_time);

    // 8. Let u be TimeClip(UTC(date)).
    new_date = time_clip(utc_time(new_date));

    // 9. Set the [[DateValue]] internal slot of this Date object to u.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(new_date);

    // 10. Return u.
    return Value(new_date);
}

// 21.4.4.27 Date.prototype.setTime ( time ), https://tc39.es/ecma262/#sec-date.prototype.settime
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_time)
{
    // 1. Perform ? thisTimeValue(this value).
    TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let t be ? ToNumber(time).
    auto time = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. Let v be TimeClip(t).
    time = time_clip(time);

    // 4. Set the [[DateValue]] internal slot of this Date object to v.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(time);

    // 5. Return v.
    return Value(time);
}

// 21.4.4.28 Date.prototype.setUTCDate ( date ), https://tc39.es/ecma262/#sec-date.prototype.setutcdate
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_utc_date)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let dt be ? ToNumber(date).
    auto date = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 4. Let newDate be MakeDate(MakeDay(YearFromTime(t), MonthFromTime(t), dt), TimeWithinDay(t)).
    auto year = year_from_time(time);
    auto month = month_from_time(time);

    auto day = make_day(year, month, date);
    auto new_date = make_date(day, time_within_day(time));

    // 5. Let v be TimeClip(newDate).
    new_date = time_clip(new_date);

    // 6. Set the [[DateValue]] internal slot of this Date object to v.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(new_date);

    // 7. Return v.
    return Value(new_date);
}

// 21.4.4.29 Date.prototype.setUTCFullYear ( year [ , month [ , date ] ] ), https://tc39.es/ecma262/#sec-date.prototype.setutcfullyear
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_utc_full_year)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto this_time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, set t to +0𝔽.
    double time = 0;
    if (!isnan(this_time))
        time = this_time;

    // 3. Let y be ? ToNumber(year).
    auto year = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 4. If month is not present, let m be MonthFromTime(t); otherwise, let m be ? ToNumber(month).
    auto month = TRY(argument_or_number(global_object, 1, month_from_time(time)));

    // 5. If date is not present, let dt be DateFromTime(t); otherwise, let dt be ? ToNumber(date).
    auto date = TRY(argument_or_number(global_object, 2, date_from_time(time)));

    // 6. Let newDate be MakeDate(MakeDay(y, m, dt), TimeWithinDay(t)).
    auto day = make_day(year, month, date);
    auto new_date = make_date(day, time_within_day(time));

    // 7. Let v be TimeClip(newDate).
    new_date = time_clip(new_date);

    // 8. Set the [[DateValue]] internal slot of this Date object to v.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(new_date);

    // 9. Return v.
    return Value(new_date);
}

// 21.4.4.30 Date.prototype.setUTCHours ( hour [ , min [ , sec [ , ms ] ] ] ), https://tc39.es/ecma262/#sec-date.prototype.setutchours
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_utc_hours)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let h be ? ToNumber(hour).
    auto hour = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If min is present, let m be ? ToNumber(min).
    auto minute = TRY(argument_or_empty(global_object, 1));

    // 4. If sec is present, let s be ? ToNumber(sec).
    auto second = TRY(argument_or_empty(global_object, 2));

    // 5. If ms is present, let milli be ? ToNumber(ms).
    auto millisecond = TRY(argument_or_empty(global_object, 3));

    // 6. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 7. If min is not present, let m be MinFromTime(t).
    if (!minute.has_value())
        minute = min_from_time(time);

    // 8. If sec is not present, let s be SecFromTime(t).
    if (!second.has_value())
        second = sec_from_time(time);

    // 9. If ms is not present, let milli be msFromTime(t).
    if (!millisecond.has_value())
        millisecond = ms_from_time(time);

    // 10. Let date be MakeDate(Day(t), MakeTime(h, m, s, milli)).
    auto new_time = make_time(hour, *minute, *second, *millisecond);
    auto date = make_date(day(time), new_time);

    // 11. Let v be TimeClip(date).
    date = time_clip(date);

    // 12. Set the [[DateValue]] internal slot of this Date object to v.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(date);

    // 13. Return v.
    return Value(date);
}

// 21.4.4.31 Date.prototype.setUTCMilliseconds ( ms ), https://tc39.es/ecma262/#sec-date.prototype.setutcmilliseconds
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_utc_milliseconds)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Set ms to ? ToNumber(ms).
    auto millisecond = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 4. Let time be MakeTime(HourFromTime(t), MinFromTime(t), SecFromTime(t), ms).
    auto hour = hour_from_time(time);
    auto minute = min_from_time(time);
    auto second = sec_from_time(time);

    auto new_time = make_time(hour, minute, second, millisecond);

    // 5. Let v be TimeClip(MakeDate(Day(t), time)).
    auto date = make_date(day(time), new_time);
    date = time_clip(date);

    // 6. Set the [[DateValue]] internal slot of this Date object to v.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(date);

    // 7. Return v.
    return Value(date);
}

// 21.4.4.32 Date.prototype.setUTCMinutes ( min [ , sec [ , ms ] ] ), https://tc39.es/ecma262/#sec-date.prototype.setutcminutes
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_utc_minutes)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let m be ? ToNumber(min).
    auto minute = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If sec is present, let s be ? ToNumber(sec).
    auto second = TRY(argument_or_empty(global_object, 1));

    // 4. If ms is present, let milli be ? ToNumber(ms).
    auto millisecond = TRY(argument_or_empty(global_object, 2));

    // 5. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 6. If sec is not present, let s be SecFromTime(t).
    if (!second.has_value())
        second = sec_from_time(time);

    // 7. If ms is not present, let milli be msFromTime(t).
    if (!millisecond.has_value())
        millisecond = ms_from_time(time);

    // 8. Let date be MakeDate(Day(t), MakeTime(HourFromTime(t), m, s, milli)).
    auto hour = hour_from_time(time);

    auto new_time = make_time(hour, minute, *second, *millisecond);
    auto date = make_date(day(time), new_time);

    // 9. Let v be TimeClip(date).
    date = time_clip(date);

    // 10. Set the [[DateValue]] internal slot of this Date object to v.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(date);

    // 11. Return v.
    return Value(date);
}

// 21.4.4.33 Date.prototype.setUTCMonth ( month [ , date ] ), https://tc39.es/ecma262/#sec-date.prototype.setutcmonth
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_utc_month)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let m be ? ToNumber(month).
    auto month = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If date is present, let dt be ? ToNumber(date).
    auto date = TRY(argument_or_empty(global_object, 1));

    // 4. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 5. If date is not present, let dt be DateFromTime(t).
    if (!date.has_value())
        date = date_from_time(time);

    // 6. Let newDate be MakeDate(MakeDay(YearFromTime(t), m, dt), TimeWithinDay(t)).
    auto year = year_from_time(time);

    auto day = make_day(year, month, *date);
    auto new_date = make_date(day, time_within_day(time));

    // 7. Let v be TimeClip(newDate).
    new_date = time_clip(new_date);

    // 8. Set the [[DateValue]] internal slot of this Date object to v.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(new_date);

    // 9. Return v.
    return Value(new_date);
}

// 21.4.4.34 Date.prototype.setUTCSeconds ( sec [ , ms ] ), https://tc39.es/ecma262/#sec-date.prototype.setutcseconds
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_utc_seconds)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let s be ? ToNumber(sec).
    auto second = TRY(vm.argument(0).to_number(global_object)).as_double();

    // 3. If ms is present, let milli be ? ToNumber(ms).
    auto millisecond = TRY(argument_or_empty(global_object, 1));

    // 4. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 5. If ms is not present, let milli be msFromTime(t).
    if (!millisecond.has_value())
        millisecond = ms_from_time(time);

    // 6. Let date be MakeDate(Day(t), MakeTime(HourFromTime(t), MinFromTime(t), s, milli)).
    auto hour = hour_from_time(time);
    auto minute = min_from_time(time);

    auto new_time = make_time(hour, minute, second, *millisecond);
    auto new_date = make_date(day(time), new_time);

    // 7. Let v be TimeClip(date).
    new_date = time_clip(new_date);

    // 8. Set the [[DateValue]] internal slot of this Date object to v.
    auto* this_object = MUST(typed_this_object(global_object));
    this_object->set_date_value(new_date);

    // 9. Return v.
    return Value(new_date);
}

// 21.4.4.35 Date.prototype.toDateString ( ), https://tc39.es/ecma262/#sec-date.prototype.todatestring
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_date_string)
{
    // 1. Let O be this Date object.
    // 2. Let tv be ? thisTimeValue(O).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 3. If tv is NaN, return "Invalid Date".
    if (isnan(time))
        return js_string(vm, "Invalid Date"sv);

    // 4. Let t be LocalTime(tv).
    // 5. Return DateString(t).
    return js_string(vm, date_string(local_time(time)));
}

// 21.4.4.36 Date.prototype.toISOString ( ), https://tc39.es/ecma262/#sec-date.prototype.toisostring
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_iso_string)
{
    auto* this_object = TRY(typed_this_object(global_object));

    if (!Value(this_object->date_value()).is_finite_number())
        return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidTimeValue);

    auto string = this_object->iso_date_string();
    return js_string(vm, move(string));
}

// 21.4.4.37 Date.prototype.toJSON ( key ), https://tc39.es/ecma262/#sec-date.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_json)
{
    auto this_value = vm.this_value(global_object);

    auto time_value = TRY(this_value.to_primitive(global_object, Value::PreferredType::Number));

    if (time_value.is_number() && !time_value.is_finite_number())
        return js_null();

    return TRY(this_value.invoke(global_object, vm.names.toISOString));
}

static ThrowCompletionOr<Intl::DateTimeFormat*> construct_date_time_format(GlobalObject& global_object, Value locales, Value options)
{
    auto* date_time_format = TRY(construct(global_object, *global_object.intl_date_time_format_constructor(), locales, options));
    return static_cast<Intl::DateTimeFormat*>(date_time_format);
}

// 21.4.4.38 Date.prototype.toLocaleDateString ( [ reserved1 [ , reserved2 ] ] ), https://tc39.es/ecma262/#sec-date.prototype.tolocaledatestring
// 19.4.2 Date.prototype.toLocaleDateString ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sup-date.prototype.tolocaledatestring
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_locale_date_string)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let x be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If x is NaN, return "Invalid Date".
    if (isnan(time))
        return js_string(vm, "Invalid Date"sv);

    // 3. Let options be ? ToDateTimeOptions(options, "date", "date").
    options = Value(TRY(Intl::to_date_time_options(global_object, options, Intl::OptionRequired::Date, Intl::OptionDefaults::Date)));

    // 4. Let dateFormat be ? Construct(%DateTimeFormat%, « locales, options »).
    auto* date_format = TRY(construct_date_time_format(global_object, locales, options));

    // 5. Return ? FormatDateTime(dateFormat, x).
    auto formatted = TRY(Intl::format_date_time(global_object, *date_format, time));
    return js_string(vm, move(formatted));
}

// 21.4.4.39 Date.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] ), https://tc39.es/ecma262/#sec-date.prototype.tolocalestring
// 19.4.1 Date.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sup-date.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_locale_string)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let x be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If x is NaN, return "Invalid Date".
    if (isnan(time))
        return js_string(vm, "Invalid Date"sv);

    // 3. Let options be ? ToDateTimeOptions(options, "any", "all").
    options = Value(TRY(Intl::to_date_time_options(global_object, options, Intl::OptionRequired::Any, Intl::OptionDefaults::All)));

    // 4. Let dateFormat be ? Construct(%DateTimeFormat%, « locales, options »).
    auto* date_format = TRY(construct_date_time_format(global_object, locales, options));

    // 5. Return ? FormatDateTime(dateFormat, x).
    auto formatted = TRY(Intl::format_date_time(global_object, *date_format, time));
    return js_string(vm, move(formatted));
}

// 21.4.4.40 Date.prototype.toLocaleTimeString ( [ reserved1 [ , reserved2 ] ] ), https://tc39.es/ecma262/#sec-date.prototype.tolocaletimestring
// 19.4.3 Date.prototype.toLocaleTimeString ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sup-date.prototype.tolocaletimestring
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_locale_time_string)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let x be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If x is NaN, return "Invalid Date".
    if (isnan(time))
        return js_string(vm, "Invalid Date"sv);

    // 3. Let options be ? ToDateTimeOptions(options, "time", "time").
    options = Value(TRY(Intl::to_date_time_options(global_object, options, Intl::OptionRequired::Time, Intl::OptionDefaults::Time)));

    // 4. Let timeFormat be ? Construct(%DateTimeFormat%, « locales, options »).
    auto* time_format = TRY(construct_date_time_format(global_object, locales, options));

    // 5. Return ? FormatDateTime(dateFormat, x).
    auto formatted = TRY(Intl::format_date_time(global_object, *time_format, time));
    return js_string(vm, move(formatted));
}

// 21.4.4.41 Date.prototype.toString ( ), https://tc39.es/ecma262/#sec-date.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_string)
{
    // 1. Let tv be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Return ToDateString(tv).
    return js_string(vm, JS::to_date_string(time));
}

// 21.4.4.41.1 TimeString ( tv ), https://tc39.es/ecma262/#sec-timestring
String time_string(double time)
{
    // 1. Let hour be ToZeroPaddedDecimalString(ℝ(HourFromTime(tv)), 2).
    auto hour = hour_from_time(time);

    // 2. Let minute be ToZeroPaddedDecimalString(ℝ(MinFromTime(tv)), 2).
    auto minute = min_from_time(time);

    // 3. Let second be ToZeroPaddedDecimalString(ℝ(SecFromTime(tv)), 2).
    auto second = sec_from_time(time);

    // 4. Return the string-concatenation of hour, ":", minute, ":", second, the code unit 0x0020 (SPACE), and "GMT".
    return String::formatted("{:02}:{:02}:{:02} GMT", hour, minute, second);
}

// 21.4.4.41.2 DateString ( tv ), https://tc39.es/ecma262/#sec-datestring
String date_string(double time)
{
    // 1. Let weekday be the Name of the entry in Table 62 with the Number WeekDay(tv).
    auto weekday = short_day_names[week_day(time)];

    // 2. Let month be the Name of the entry in Table 63 with the Number MonthFromTime(tv).
    auto month = short_month_names[month_from_time(time)];

    // 3. Let day be ToZeroPaddedDecimalString(ℝ(DateFromTime(tv)), 2).
    auto day = date_from_time(time);

    // 4. Let yv be YearFromTime(tv).
    auto year = year_from_time(time);

    // 5. If yv is +0𝔽 or yv > +0𝔽, let yearSign be the empty String; otherwise, let yearSign be "-".
    auto year_sign = year >= 0 ? ""sv : "-"sv;

    // 6. Let paddedYear be ToZeroPaddedDecimalString(abs(ℝ(yv)), 4).
    // 7. Return the string-concatenation of weekday, the code unit 0x0020 (SPACE), month, the code unit 0x0020 (SPACE), day, the code unit 0x0020 (SPACE), yearSign, and paddedYear.
    return String::formatted("{} {} {:02} {}{:04}", weekday, month, day, year_sign, abs(year));
}

// 21.4.4.41.3 TimeZoneString ( tv ), https://tc39.es/ecma262/#sec-timezoneestring
String time_zone_string(double time)
{
    // 1. Let offset be LocalTZA(tv, true).
    auto offset = local_tza(time, true);

    StringView offset_sign;

    // 2. If offset is +0𝔽 or offset > +0𝔽, then
    if (offset >= 0) {
        // a. Let offsetSign be "+".
        offset_sign = "+"sv;
        // b. Let absOffset be offset.
    }
    // 3. Else,
    else {
        // a. Let offsetSign be "-".
        offset_sign = "-"sv;
        // b. Let absOffset be -offset.
        offset *= -1;
    }

    // 4. Let offsetMin be ToZeroPaddedDecimalString(ℝ(MinFromTime(absOffset)), 2).
    auto offset_min = min_from_time(offset);

    // 5. Let offsetHour be ToZeroPaddedDecimalString(ℝ(HourFromTime(absOffset)), 2).
    auto offset_hour = hour_from_time(offset);

    // 6. Let tzName be an implementation-defined string that is either the empty String or the string-concatenation of the code unit 0x0020 (SPACE), the code unit 0x0028 (LEFT PARENTHESIS), an implementation-defined timezone name, and the code unit 0x0029 (RIGHT PARENTHESIS).
    auto tz_name = TimeZone::current_time_zone();

    // Most implementations seem to prefer the long-form display name of the time zone. Not super important, but we may as well match that behavior.
    if (auto maybe_offset = TimeZone::get_time_zone_offset(tz_name, AK::Time::from_milliseconds(time)); maybe_offset.has_value()) {
        if (auto long_name = Unicode::get_time_zone_name(Unicode::default_locale(), tz_name, Unicode::CalendarPatternStyle::Long, maybe_offset->in_dst); long_name.has_value())
            tz_name = long_name.release_value();
    }

    // 7. Return the string-concatenation of offsetSign, offsetHour, offsetMin, and tzName.
    return String::formatted("{}{:02}{:02} ({})", offset_sign, offset_hour, offset_min, tz_name);
}

// 21.4.4.41.4 ToDateString ( tv ), https://tc39.es/ecma262/#sec-todatestring
String to_date_string(double time)
{
    // 1. If tv is NaN, return "Invalid Date".
    if (Value(time).is_nan())
        return "Invalid Date"sv;

    // 2. Let t be LocalTime(tv).
    time = local_time(time);

    // 3. Return the string-concatenation of DateString(t), the code unit 0x0020 (SPACE), TimeString(t), and TimeZoneString(tv).
    return String::formatted("{} {}{}", date_string(time), time_string(time), time_zone_string(time));
}

// 14.1.1 Date.prototype.toTemporalInstant ( ), https://tc39.es/proposal-temporal/#sec-date.prototype.totemporalinstant
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_temporal_instant)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto t = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. Let ns be ? NumberToBigInt(t) × ℤ(10^6).
    auto* ns = TRY(number_to_bigint(global_object, Value(t)));
    ns = js_bigint(vm, ns->big_integer().multiplied_by(Crypto::UnsignedBigInteger { 1'000'000 }));

    // 3. Return ! CreateTemporalInstant(ns).
    return MUST(Temporal::create_temporal_instant(global_object, *ns));
}

// 21.4.4.42 Date.prototype.toTimeString ( ), https://tc39.es/ecma262/#sec-date.prototype.totimestring
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_time_string)
{
    // 1. Let O be this Date object.
    // 2. Let tv be ? thisTimeValue(O).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 3. If tv is NaN, return "Invalid Date".
    if (isnan(time))
        return js_string(vm, "Invalid Date"sv);

    // 4. Let t be LocalTime(tv).
    // 5. Return the string-concatenation of TimeString(t) and TimeZoneString(tv).
    auto string = String::formatted("{}{}", time_string(local_time(time)), time_zone_string(time));
    return js_string(vm, move(string));
}

// 21.4.4.43 Date.prototype.toUTCString ( ), https://tc39.es/ecma262/#sec-date.prototype.toutcstring
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_utc_string)
{
    // 1. Let O be this Date object.
    // 2. Let tv be ? thisTimeValue(O).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 3. If tv is NaN, return "Invalid Date".
    if (isnan(time))
        return js_string(vm, "Invalid Date"sv);

    // 4. Let weekday be the Name of the entry in Table 62 with the Number WeekDay(tv).
    auto weekday = short_day_names[week_day(time)];

    // 5. Let month be the Name of the entry in Table 63 with the Number MonthFromTime(tv).
    auto month = short_month_names[month_from_time(time)];

    // 6. Let day be ToZeroPaddedDecimalString(ℝ(DateFromTime(tv)), 2).
    auto day = date_from_time(time);

    // 7. Let yv be YearFromTime(tv).
    auto year = year_from_time(time);

    // 8. If yv is +0𝔽 or yv > +0𝔽, let yearSign be the empty String; otherwise, let yearSign be "-".
    auto year_sign = year >= 0 ? ""sv : "-"sv;

    // 9. Let paddedYear be ToZeroPaddedDecimalString(abs(ℝ(yv)), 4).
    // 10. Return the string-concatenation of weekday, ",", the code unit 0x0020 (SPACE), day, the code unit 0x0020 (SPACE), month, the code unit 0x0020 (SPACE), yearSign, paddedYear, the code unit 0x0020 (SPACE), and TimeString(tv).
    auto string = String::formatted("{}, {:02} {} {}{:04} {}", weekday, day, month, year_sign, abs(year), time_string(time));
    return js_string(vm, move(string));
}

// 21.4.4.45 Date.prototype [ @@toPrimitive ] ( hint ), https://tc39.es/ecma262/#sec-date.prototype-@@toprimitive
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::symbol_to_primitive)
{
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, this_value.to_string_without_side_effects());
    auto hint_value = vm.argument(0);
    if (!hint_value.is_string())
        return vm.throw_completion<TypeError>(global_object, ErrorType::InvalidHint, hint_value.to_string_without_side_effects());
    auto& hint = hint_value.as_string().string();
    Value::PreferredType try_first;
    if (hint == "string" || hint == "default")
        try_first = Value::PreferredType::String;
    else if (hint == "number")
        try_first = Value::PreferredType::Number;
    else
        return vm.throw_completion<TypeError>(global_object, ErrorType::InvalidHint, hint);
    return TRY(this_value.as_object().ordinary_to_primitive(try_first));
}

// B.2.4.1 Date.prototype.getYear ( ), https://tc39.es/ecma262/#sec-date.prototype.getyear
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_year)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, return NaN.
    if (isnan(time))
        return js_nan();

    // 3. Return YearFromTime(LocalTime(t)) - 1900𝔽.
    return Value(year_from_time(local_time(time)) - 1900);
}

// B.2.4.2 Date.prototype.setYear ( year ), https://tc39.es/ecma262/#sec-date.prototype.setyear
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::set_year)
{
    // 1. Let t be ? thisTimeValue(this value).
    auto this_time = TRY(this_time_value(global_object, vm.this_value(global_object)));

    // 2. If t is NaN, set t to +0𝔽; otherwise, set t to LocalTime(t).
    double time = 0;
    if (!isnan(this_time))
        time = local_time(this_time);

    // 3. Let y be ? ToNumber(year).
    auto year = TRY(vm.argument(0).to_number(global_object)).as_double();

    auto* this_object = MUST(typed_this_object(global_object));

    // 4. If y is NaN, then
    if (isnan(year)) {
        // a. Set the [[DateValue]] internal slot of this Date object to NaN.
        this_object->set_date_value(NAN);

        // b. Return NaN.
        return js_nan();
    }

    // 5. Let yi be ! ToIntegerOrInfinity(y).
    auto year_integer = to_integer_or_infinity(year);

    // 6. If 0 ≤ yi ≤ 99, let yyyy be 1900𝔽 + 𝔽(yi).
    if (0 <= year_integer && year_integer <= 99)
        year = 1900 + year_integer;
    // 7. Else, let yyyy be y.

    // 8. Let d be MakeDay(yyyy, MonthFromTime(t), DateFromTime(t)).
    auto day = make_day(year, month_from_time(time), date_from_time(time));

    // 9. Let date be UTC(MakeDate(d, TimeWithinDay(t))).
    auto date = utc_time(make_date(day, time_within_day(time)));

    // 10. Set the [[DateValue]] internal slot of this Date object to TimeClip(date).
    auto new_date = time_clip(date);
    this_object->set_date_value(new_date);

    // 11. Return the value of the [[DateValue]] internal slot of this Date object.
    return Value(new_date);
}

// B.2.4.3 Date.prototype.toGMTString ( ), https://tc39.es/ecma262/#sec-date.prototype.togmtstring
JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_gmt_string)
{
    // NOTE: The toUTCString method is preferred. The toGMTString method is provided principally for compatibility with old code.
    return to_utc_string(vm, global_object);
}

}
