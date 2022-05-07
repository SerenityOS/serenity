/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/PlainTimePrototype.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>

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

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.hour, hour_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.minute, minute_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.second, second_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.millisecond, millisecond_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.microsecond, microsecond_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.nanosecond, nanosecond_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.add, add, 1, attr);
    define_native_function(vm.names.subtract, subtract, 1, attr);
    define_native_function(vm.names.with, with, 1, attr);
    define_native_function(vm.names.until, until, 1, attr);
    define_native_function(vm.names.since, since, 1, attr);
    define_native_function(vm.names.round, round, 1, attr);
    define_native_function(vm.names.equals, equals, 1, attr);
    define_native_function(vm.names.toPlainDateTime, to_plain_date_time, 1, attr);
    define_native_function(vm.names.toZonedDateTime, to_zoned_date_time, 1, attr);
    define_native_function(vm.names.getISOFields, get_iso_fields, 0, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 0, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

// 4.3.3 get Temporal.PlainTime.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::calendar_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return temporalTime.[[Calendar]].
    return Value(&temporal_time->calendar());
}

// 4.3.4 get Temporal.PlainTime.prototype.hour, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.hour
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::hour_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISOHour]]).
    return Value(temporal_time->iso_hour());
}

// 4.3.5 get Temporal.PlainTime.prototype.minute, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.minute
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::minute_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISOMinute]]).
    return Value(temporal_time->iso_minute());
}

// 4.3.6 get Temporal.PlainTime.prototype.second, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.second
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::second_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISOSecond]]).
    return Value(temporal_time->iso_second());
}

// 4.3.7 get Temporal.PlainTime.prototype.millisecond, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.millisecond
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::millisecond_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISOMillisecond]]).
    return Value(temporal_time->iso_millisecond());
}

// 4.3.8 get Temporal.PlainTime.prototype.microsecond, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.microsecond
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::microsecond_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISOMicrosecond]]).
    return Value(temporal_time->iso_microsecond());
}

// 4.3.9 get Temporal.PlainTime.prototype.nanosecond, https://tc39.es/proposal-temporal/#sec-get-temporal.plaintime.prototype.nanosecond
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::nanosecond_getter)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return 𝔽(temporalTime.[[ISONanosecond]]).
    return Value(temporal_time->iso_nanosecond());
}

// 4.3.10 Temporal.PlainTime.prototype.add ( temporalDurationLike ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.add
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::add)
{
    auto temporal_duration_like = vm.argument(0);

    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return ? AddDurationToOrSubtractDurationFromPlainTime(add, temporalTime, temporalDurationLike).
    return TRY(add_duration_to_or_subtract_duration_from_plain_time(global_object, ArithmeticOperation::Add, *temporal_time, temporal_duration_like));
}

// 4.3.11 Temporal.PlainTime.prototype.subtract ( temporalDurationLike ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.subtract
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::subtract)
{
    auto temporal_duration_like = vm.argument(0);

    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return ? AddDurationToOrSubtractDurationFromPlainTime(subtract, temporalTime, temporalDurationLike).
    return TRY(add_duration_to_or_subtract_duration_from_plain_time(global_object, ArithmeticOperation::Subtract, *temporal_time, temporal_duration_like));
}

// 4.3.12 Temporal.PlainTime.prototype.with ( temporalTimeLike [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.with
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::with)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    auto temporal_time_like_argument = vm.argument(0);

    // 3. If Type(temporalTimeLike) is not Object, then
    if (!temporal_time_like_argument.is_object()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, temporal_time_like_argument.to_string_without_side_effects());
    }

    auto& temporal_time_like = temporal_time_like_argument.as_object();

    // 4. Perform ? RejectObjectWithCalendarOrTimeZone(temporalTimeLike).
    TRY(reject_object_with_calendar_or_time_zone(global_object, temporal_time_like));

    // 5. Let partialTime be ? ToPartialTime(temporalTimeLike).
    auto partial_time = TRY(to_partial_time(global_object, temporal_time_like));

    // 6. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 7. Let overflow be ? ToTemporalOverflow(options).
    auto overflow = TRY(to_temporal_overflow(global_object, options));

    // 8. If partialTime.[[Hour]] is not undefined, then
    //      a. Let hour be partialTime.[[Hour]].
    // 9. Else,
    //      a. Let hour be temporalTime.[[ISOHour]].
    auto hour = partial_time.hour.value_or(temporal_time->iso_hour());

    // 10. If partialTime.[[Minute]] is not undefined, then
    //      a. Let minute be partialTime.[[Minute]].
    // 11. Else,
    //      a. Let minute be temporalTime.[[ISOMinute]].
    auto minute = partial_time.minute.value_or(temporal_time->iso_minute());

    // 12. If partialTime.[[Second]] is not undefined, then
    //      a. Let second be partialTime.[[Second]].
    // 13. Else,
    //      a. Let second be temporalTime.[[ISOSecond]].
    auto second = partial_time.second.value_or(temporal_time->iso_second());

    // 14. If partialTime.[[Millisecond]] is not undefined, then
    //      a. Let millisecond be partialTime.[[Millisecond]].
    // 15. Else,
    //      a. Let millisecond be temporalTime.[[ISOMillisecond]].
    auto millisecond = partial_time.millisecond.value_or(temporal_time->iso_millisecond());

    // 16. If partialTime.[[Microsecond]] is not undefined, then
    //      a. Let microsecond be partialTime.[[Microsecond]].
    // 17. Else,
    //      a. Let microsecond be temporalTime.[[ISOMicrosecond]].
    auto microsecond = partial_time.microsecond.value_or(temporal_time->iso_microsecond());

    // 18. If partialTime.[[Nanosecond]] is not undefined, then
    //      a. Let nanosecond be partialTime.[[Nanosecond]].
    // 19. Else,
    //      a. Let nanosecond be temporalTime.[[ISONanosecond]].
    auto nanosecond = partial_time.nanosecond.value_or(temporal_time->iso_nanosecond());

    // 20. Let result be ? RegulateTime(hour, minute, second, millisecond, microsecond, nanosecond, overflow).
    auto result = TRY(regulate_time(global_object, hour, minute, second, millisecond, microsecond, nanosecond, overflow));

    // 21. Return ? CreateTemporalTime(result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]]).
    return TRY(create_temporal_time(global_object, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond));
}

// 4.3.13 Temporal.PlainTime.prototype.until ( other [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.until
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::until)
{
    auto other = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return ? DifferenceTemporalPlainTime(since, temporalTime, other, options).
    // FIXME: until, not since (spec issue, see https://github.com/tc39/proposal-temporal/pull/2183)
    return TRY(difference_temporal_plain_time(global_object, DifferenceOperation::Until, *temporal_time, other, options));
}

// 4.3.14 Temporal.PlainTime.prototype.since ( other [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.since
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::since)
{
    auto other = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return ? DifferenceTemporalPlainTime(until, temporalTime, other, options).
    // FIXME: since, not until (spec issue, see https://github.com/tc39/proposal-temporal/pull/2183)
    return TRY(difference_temporal_plain_time(global_object, DifferenceOperation::Since, *temporal_time, other, options));
}

// 4.3.15 Temporal.PlainTime.prototype.round ( roundTo ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.round
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::round)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. If roundTo is undefined, then
    if (vm.argument(0).is_undefined()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalMissingOptionsObject);
    }

    Object* round_to;

    // 4. If Type(roundTo) is String, then
    if (vm.argument(0).is_string()) {
        // a. Let paramString be roundTo.

        // b. Set roundTo to OrdinaryObjectCreate(null).
        round_to = Object::create(global_object, nullptr);

        // c. Perform ! CreateDataPropertyOrThrow(roundTo, "smallestUnit", paramString).
        MUST(round_to->create_data_property_or_throw(vm.names.smallestUnit, vm.argument(0)));
    }
    // 5. Else,
    else {
        // a. Set roundTo to ? GetOptionsObject(roundTo).
        round_to = TRY(get_options_object(global_object, vm.argument(0)));
    }

    // 6. Let smallestUnit be ? ToSmallestTemporalUnit(roundTo, « "year", "month", "week", "day" », undefined).
    auto smallest_unit_value = TRY(to_smallest_temporal_unit(global_object, *round_to, { "year"sv, "month"sv, "week"sv, "day"sv }, {}));

    // 7. If smallestUnit is undefined, throw a RangeError exception.
    if (!smallest_unit_value.has_value())
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, vm.names.undefined.as_string(), "smallestUnit");

    // NOTE: At this point smallest_unit_value can only be a string
    auto& smallest_unit = *smallest_unit_value;

    // 8. Let roundingMode be ? ToTemporalRoundingMode(roundTo, "halfExpand").
    auto rounding_mode = TRY(to_temporal_rounding_mode(global_object, *round_to, "halfExpand"));

    double maximum;

    // 9. If smallestUnit is "hour", then
    if (smallest_unit == "hour"sv) {
        // a. Let maximum be 24.
        maximum = 24;
    }
    // 10. Else if smallestUnit is "minute" or "second", then
    else if (smallest_unit == "minute"sv || smallest_unit == "second"sv) {
        // a. Let maximum be 60.
        maximum = 60;
    }
    // 11. Else,
    else {
        // a. Let maximum be 1000.
        maximum = 1000;
    }

    // 12. Let roundingIncrement be ? ToTemporalRoundingIncrement(roundTo, maximum, false).
    auto rounding_increment = TRY(to_temporal_rounding_increment(global_object, *round_to, maximum, false));

    // 13. Let result be ! RoundTime(temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], roundingIncrement, smallestUnit, roundingMode).
    auto result = round_time(temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), rounding_increment, smallest_unit, rounding_mode);

    // 14. Return ? CreateTemporalTime(result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]]).
    return TRY(create_temporal_time(global_object, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond));
}

// 4.3.16 Temporal.PlainTime.prototype.equals ( other ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.equals
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::equals)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Set other to ? ToTemporalTime(other).
    auto* other = TRY(to_temporal_time(global_object, vm.argument(0)));

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
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::to_plain_date_time)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Set temporalDate to ? ToTemporalDate(temporalDate).
    auto* temporal_date = TRY(to_temporal_date(global_object, vm.argument(0)));

    // 4. Return ? CreateTemporalDateTime(temporalDate.[[ISOYear]], temporalDate.[[ISOMonth]], temporalDate.[[ISODay]], temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], temporalDate.[[Calendar]]).
    return TRY(create_temporal_date_time(global_object, temporal_date->iso_year(), temporal_date->iso_month(), temporal_date->iso_day(), temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), temporal_date->calendar()));
}

// 4.3.18 Temporal.PlainTime.prototype.toZonedDateTime ( item ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.tozoneddatetime
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::to_zoned_date_time)
{
    auto item = vm.argument(0);

    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. If Type(item) is not Object, then
    if (!item.is_object()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, item);
    }

    // 4. Let temporalDateLike be ? Get(item, "plainDate").
    auto temporal_date_like = TRY(item.as_object().get(vm.names.plainDate));

    // 5. If temporalDateLike is undefined, then
    if (temporal_date_like.is_undefined()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::MissingRequiredProperty, vm.names.plainDate.as_string());
    }

    // 6. Let temporalDate be ? ToTemporalDate(temporalDateLike).
    auto* temporal_date = TRY(to_temporal_date(global_object, temporal_date_like));

    // 7. Let temporalTimeZoneLike be ? Get(item, "timeZone").
    auto temporal_time_zone_like = TRY(item.as_object().get(vm.names.timeZone));

    // 8. If temporalTimeZoneLike is undefined, then
    if (temporal_time_zone_like.is_undefined()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::MissingRequiredProperty, vm.names.timeZone.as_string());
    }

    // 9. Let timeZone be ? ToTemporalTimeZone(temporalTimeZoneLike).
    auto* time_zone = TRY(to_temporal_time_zone(global_object, temporal_time_zone_like));

    // 10. Let temporalDateTime be ? CreateTemporalDateTime(temporalDate.[[ISOYear]], temporalDate.[[ISOMonth]], temporalDate.[[ISODay]], temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], temporalDate.[[Calendar]]).
    auto* temporal_date_time = TRY(create_temporal_date_time(global_object, temporal_date->iso_year(), temporal_date->iso_month(), temporal_date->iso_day(), temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), temporal_date->calendar()));

    // 11. Let instant be ? BuiltinTimeZoneGetInstantFor(timeZone, temporalDateTime, "compatible").
    auto* instant = TRY(builtin_time_zone_get_instant_for(global_object, time_zone, *temporal_date_time, "compatible"sv));

    // 12. Return ! CreateTemporalZonedDateTime(instant.[[Nanoseconds]], timeZone, temporalDate.[[Calendar]]).
    return MUST(create_temporal_zoned_date_time(global_object, instant->nanoseconds(), *time_zone, temporal_date->calendar()));
}

// 4.3.19 Temporal.PlainTime.prototype.getISOFields ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.getisofields
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::get_iso_fields)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Let fields be OrdinaryObjectCreate(%Object.prototype%).
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
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::to_string)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(0)));

    // 4. Let precision be ? ToSecondsStringPrecision(options).
    auto precision = TRY(to_seconds_string_precision(global_object, *options));

    // 5. Let roundingMode be ? ToTemporalRoundingMode(options, "trunc").
    auto rounding_mode = TRY(to_temporal_rounding_mode(global_object, *options, "trunc"sv));

    // 6. Let roundResult be ! RoundTime(temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], precision.[[Increment]], precision.[[Unit]], roundingMode).
    auto round_result = round_time(temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), precision.increment, precision.unit, rounding_mode);

    // 7. Return ! TemporalTimeToString(roundResult.[[Hour]], roundResult.[[Minute]], roundResult.[[Second]], roundResult.[[Millisecond]], roundResult.[[Microsecond]], roundResult.[[Nanosecond]], precision.[[Precision]]).
    auto string = temporal_time_to_string(round_result.hour, round_result.minute, round_result.second, round_result.millisecond, round_result.microsecond, round_result.nanosecond, precision.precision);
    return js_string(vm, move(string));
}

// 4.3.21 Temporal.PlainTime.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::to_locale_string)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return ! TemporalTimeToString(temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], "auto").
    auto string = temporal_time_to_string(temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), "auto"sv);
    return js_string(vm, move(string));
}

// 4.3.22 Temporal.PlainTime.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::to_json)
{
    // 1. Let temporalTime be the this value.
    // 2. Perform ? RequireInternalSlot(temporalTime, [[InitializedTemporalTime]]).
    auto* temporal_time = TRY(typed_this_object(global_object));

    // 3. Return ! TemporalTimeToString(temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], "auto").
    auto string = temporal_time_to_string(temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), "auto"sv);
    return js_string(vm, move(string));
}

// 4.3.23 Temporal.PlainTime.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaintime.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainTimePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainTime", "a primitive value");
}

}
