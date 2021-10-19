/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/PlainTimePrototype.h>

namespace JS::Temporal {

// 4.3 Properties of the Temporal.PlainTime Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plaintime-prototype-object
PlainTimePrototype::PlainTimePrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void PlainTimePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 4.3.2 Temporal.PlainTime.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.PlainTime"), Attribute::Configurable);

    define_old_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);
    define_old_native_accessor(vm.names.hour, hour_getter, {}, Attribute::Configurable);
    define_old_native_accessor(vm.names.minute, minute_getter, {}, Attribute::Configurable);
    define_old_native_accessor(vm.names.second, second_getter, {}, Attribute::Configurable);
    define_old_native_accessor(vm.names.millisecond, millisecond_getter, {}, Attribute::Configurable);
    define_old_native_accessor(vm.names.microsecond, microsecond_getter, {}, Attribute::Configurable);
    define_old_native_accessor(vm.names.nanosecond, nanosecond_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_old_native_function(vm.names.with, with, 1, attr);
    define_old_native_function(vm.names.equals, equals, 1, attr);
    define_old_native_function(vm.names.toPlainDateTime, to_plain_date_time, 1, attr);
    define_old_native_function(vm.names.getISOFields, get_iso_fields, 0, attr);
    define_old_native_function(vm.names.toString, to_string, 0, attr);
    define_old_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_old_native_function(vm.names.toJSON, to_json, 0, attr);
    define_old_native_function(vm.names.valueOf, value_of, 0, attr);
}

// 4.3.3 get Temporal.PlainTime.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.calendar
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::calendar_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return temporalTime.[[Calendar]].
    return Value(&temporal_time->calendar());
}

// 4.3.4 get Temporal.PlainTime.prototype.hour, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.hour
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::hour_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISOHour]]).
    return Value(temporal_time->iso_hour());
}

// 4.3.5 get Temporal.PlainTime.prototype.minute, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.minute
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::minute_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISOMinute]]).
    return Value(temporal_time->iso_minute());
}

// 4.3.6 get Temporal.PlainTime.prototype.second, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.second
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::second_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISOSecond]]).
    return Value(temporal_time->iso_second());
}

// 4.3.7 get Temporal.PlainTime.prototype.millisecond, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.millisecond
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::millisecond_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISOMillisecond]]).
    return Value(temporal_time->iso_millisecond());
}

// 4.3.8 get Temporal.PlainTime.prototype.microsecond, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.microsecond
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::microsecond_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISOMicrosecond]]).
    return Value(temporal_time->iso_microsecond());
}

// 4.3.9 get Temporal.PlainTime.prototype.nanosecond, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.nanosecond
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::nanosecond_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISONanosecond]]).
    return Value(temporal_time->iso_nanosecond());
}

// 4.3.12 Temporal.PlainTime.prototype.with ( temporalTimeLike [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.with
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::with)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    auto temporal_time_like_argument = vm.argument(0);

    // 3. If Type(temporalTimeLike) is not Object, then
    if (!temporal_time_like_argument.is_object()) {
        // a. Throw a TypeError exception.
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, temporal_time_like_argument.to_string_without_side_effects());
        return {};
    }

    auto& temporal_time_like = temporal_time_like_argument.as_object();

    // 4. Perform ? RejectTemporalCalendarType(temporalTimeLike).
    TRY_OR_DISCARD(reject_temporal_calendar_type(global_object, temporal_time_like));

    // 5. Let calendarProperty be ? Get(temporalTimeLike, "calendar").
    auto calendar_property = TRY_OR_DISCARD(temporal_time_like.get(vm.names.calendar));

    // 6. If calendarProperty is not undefined, then
    if (!calendar_property.is_undefined()) {
        // a. Throw a TypeError exception.
        vm.throw_exception<TypeError>(global_object, ErrorType::TemporalPlainTimeWithArgumentMustNotHave, "calendar");
        return {};
    }

    // 7. Let timeZoneProperty be ? Get(temporalTimeLike, "timeZone").
    auto time_zone_property = TRY_OR_DISCARD(temporal_time_like.get(vm.names.timeZone));

    // 8. If timeZoneProperty is not undefined, then
    if (!time_zone_property.is_undefined()) {
        // a. Throw a TypeError exception.
        vm.throw_exception<TypeError>(global_object, ErrorType::TemporalPlainTimeWithArgumentMustNotHave, "timeZone");
        return {};
    }

    // 9. Let partialTime be ? ToPartialTime(temporalTimeLike).
    auto partial_time = TRY_OR_DISCARD(to_partial_time(global_object, temporal_time_like));

    // 10. Set options to ? GetOptionsObject(options).
    auto* options = TRY_OR_DISCARD(get_options_object(global_object, vm.argument(1)));

    // 11. Let overflow be ? ToTemporalOverflow(options).
    auto overflow = TRY_OR_DISCARD(to_temporal_overflow(global_object, *options));

    // 12. If partialTime.[[Hour]] is not undefined, then
    //      a. Let hour be partialTime.[[Hour]].
    // 13. Else,
    //      a. Let hour be temporalTime.[[ISOHour]].
    auto hour = partial_time.hour.value_or(temporal_time->iso_hour());

    // 14. If partialTime.[[Minute]] is not undefined, then
    //      a. Let minute be partialTime.[[Minute]].
    // 15. Else,
    //      a. Let minute be temporalTime.[[ISOMinute]].
    auto minute = partial_time.minute.value_or(temporal_time->iso_minute());

    // 16. If partialTime.[[Second]] is not undefined, then
    //      a. Let second be partialTime.[[Second]].
    // 17. Else,
    //      a. Let second be temporalTime.[[ISOSecond]].
    auto second = partial_time.second.value_or(temporal_time->iso_second());

    // 18. If partialTime.[[Millisecond]] is not undefined, then
    //      a. Let millisecond be partialTime.[[Millisecond]].
    // 19. Else,
    //      a. Let millisecond be temporalTime.[[ISOMillisecond]].
    auto millisecond = partial_time.millisecond.value_or(temporal_time->iso_millisecond());

    // 20. If partialTime.[[Microsecond]] is not undefined, then
    //      a. Let microsecond be partialTime.[[Microsecond]].
    // 21. Else,
    //      a. Let microsecond be temporalTime.[[ISOMicrosecond]].
    auto microsecond = partial_time.microsecond.value_or(temporal_time->iso_microsecond());

    // 22. If partialTime.[[Nanosecond]] is not undefined, then
    //      a. Let nanosecond be partialTime.[[Nanosecond]].
    // 23. Else,
    //      a. Let nanosecond be temporalTime.[[ISONanosecond]].
    auto nanosecond = partial_time.nanosecond.value_or(temporal_time->iso_nanosecond());

    // 24. Let result be ? RegulateTime(hour, minute, second, millisecond, microsecond, nanosecond, overflow).
    auto result = TRY_OR_DISCARD(regulate_time(global_object, hour, minute, second, millisecond, microsecond, nanosecond, overflow));

    // 25. Return ? CreateTemporalTime(result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]]).
    return TRY_OR_DISCARD(create_temporal_time(global_object, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond));
}

// 4.3.16 Temporal.PlainTime.prototype.equals ( other ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.equals
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::equals)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Set other to ? ToTemporalTime(other).
    auto* other = TRY_OR_DISCARD(to_temporal_time(global_object, vm.argument(0)));

    // 4. If temporalTime.[[ISOHour]] ≠ other.[[ISOHour]], return false.
    if (temporal_time->iso_hour() != other->iso_hour())
        return Value(false);

    // 5. If temporalTime.[[ISOMinute]] ≠ other.[[ISOMinute]], return false.
    if (temporal_time->iso_minute() != other->iso_minute())
        return Value(false);

    // 6. If temporalTime.[[ISOSecond]] ≠ other.[[ISOSecond]], return false.
    if (temporal_time->iso_second() != other->iso_second())
        return Value(false);

    // 7. If temporalTime.[[ISOMillisecond]] ≠ other.[[ISOMillisecond]], return false.
    if (temporal_time->iso_millisecond() != other->iso_millisecond())
        return Value(false);

    // 8. If temporalTime.[[ISOMicrosecond]] ≠ other.[[ISOMicrosecond]], return false.
    if (temporal_time->iso_microsecond() != other->iso_microsecond())
        return Value(false);

    // 9. If temporalTime.[[ISONanosecond]] ≠ other.[[ISONanosecond]], return false.
    if (temporal_time->iso_nanosecond() != other->iso_nanosecond())
        return Value(false);

    // 10. Return true.
    return Value(true);
}

// 4.3.17 Temporal.PlainTime.prototype.toPlainDateTime ( temporalDate ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.toplaindatetime
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::to_plain_date_time)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Set temporalDate to ? ToTemporalDate(temporalDate).
    auto* temporal_date = TRY_OR_DISCARD(to_temporal_date(global_object, vm.argument(0)));

    // 4. Return ? CreateTemporalDateTime(temporalDate.[[ISOYear]], temporalDate.[[ISOMonth]], temporalDate.[[ISODay]], temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], temporalDate.[[Calendar]]).
    return TRY_OR_DISCARD(create_temporal_date_time(global_object, temporal_date->iso_year(), temporal_date->iso_month(), temporal_date->iso_day(), temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), temporal_date->calendar()));
}

// 4.3.19 Temporal.PlainTime.prototype.getISOFields ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.getisofields
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::get_iso_fields)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Let fields be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* fields = Object::create(global_object, global_object.object_prototype());

    // 4. Perform ! CreateDataPropertyOrThrow(fields, "calendar", temporalTime.[[Calendar]]).
    MUST(fields->create_data_property_or_throw(vm.names.calendar, Value(&temporal_time->calendar())));

    // 5. Perform ! CreateDataPropertyOrThrow(fields, "isoHour", 𝔽(temporalTime.[[ISOHour]])).
    MUST(fields->create_data_property_or_throw(vm.names.isoHour, Value(temporal_time->iso_hour())));

    // 6. Perform ! CreateDataPropertyOrThrow(fields, "isoMicrosecond", 𝔽(temporalTime.[[ISOMicrosecond]])).
    MUST(fields->create_data_property_or_throw(vm.names.isoMicrosecond, Value(temporal_time->iso_microsecond())));

    // 7. Perform ! CreateDataPropertyOrThrow(fields, "isoMillisecond", 𝔽(temporalTime.[[ISOMillisecond]])).
    MUST(fields->create_data_property_or_throw(vm.names.isoMillisecond, Value(temporal_time->iso_millisecond())));

    // 8. Perform ! CreateDataPropertyOrThrow(fields, "isoMinute", 𝔽(temporalTime.[[ISOMinute]])).
    MUST(fields->create_data_property_or_throw(vm.names.isoMinute, Value(temporal_time->iso_minute())));

    // 9. Perform ! CreateDataPropertyOrThrow(fields, "isoNanosecond", 𝔽(temporalTime.[[ISONanosecond]])).
    MUST(fields->create_data_property_or_throw(vm.names.isoNanosecond, Value(temporal_time->iso_nanosecond())));

    // 10. Perform ! CreateDataPropertyOrThrow(fields, "isoSecond", 𝔽(temporalTime.[[ISOSecond]])).
    MUST(fields->create_data_property_or_throw(vm.names.isoSecond, Value(temporal_time->iso_second())));

    // 11. Return fields.
    return fields;
}

// 4.3.20 Temporal.PlainTime.prototype.toString ( [ options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.tostring
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::to_string)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Set options to ? GetOptionsObject(options).
    auto* options = TRY_OR_DISCARD(get_options_object(global_object, vm.argument(0)));

    // 4. Let precision be ? ToSecondsStringPrecision(options).
    auto precision = TRY_OR_DISCARD(to_seconds_string_precision(global_object, *options));

    // 5. Let roundingMode be ? ToTemporalRoundingMode(options, "trunc").
    auto rounding_mode = TRY_OR_DISCARD(to_temporal_rounding_mode(global_object, *options, "trunc"sv));

    // 6. Let roundResult be ! RoundTime(temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], precision.[[Increment]], precision.[[Unit]], roundingMode).
    auto round_result = round_time(temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), precision.increment, precision.unit, rounding_mode);

    // 7. Return ! TemporalTimeToString(roundResult.[[Hour]], roundResult.[[Minute]], roundResult.[[Second]], roundResult.[[Millisecond]], roundResult.[[Microsecond]], roundResult.[[Nanosecond]], precision.[[Precision]]).
    auto string = temporal_time_to_string(round_result.hour, round_result.minute, round_result.second, round_result.millisecond, round_result.microsecond, round_result.nanosecond, precision.precision);
    return js_string(vm, move(string));
}

// 4.3.21 Temporal.PlainTime.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.tolocalestring
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::to_locale_string)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return ! TemporalTimeToString(temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], "auto").
    auto string = temporal_time_to_string(temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), "auto"sv);
    return js_string(vm, move(string));
}

// 4.3.22 Temporal.PlainTime.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.tojson
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::to_json)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY_OR_DISCARD(typed_this_object(global_object));

    // 3. Return ! TemporalTimeToString(temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], "auto").
    auto string = temporal_time_to_string(temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), "auto"sv);
    return js_string(vm, move(string));
}

// 4.3.23 Temporal.PlainTime.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.valueof
JS_DEFINE_OLD_NATIVE_FUNCTION(PlainTimePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainTime", "a primitive value");
    return {};
}

}
