/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/DurationPrototype.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <math.h>

namespace JS::Temporal {

// 7.3 Properties of the Temporal.Duration Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-duration-prototype-object
DurationPrototype::DurationPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void DurationPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 7.3.2 Temporal.Duration.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.duration.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.Duration"), Attribute::Configurable);

    define_native_accessor(vm.names.years, years_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.months, months_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.weeks, weeks_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.days, days_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.hours, hours_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.minutes, minutes_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.seconds, seconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.milliseconds, milliseconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.microseconds, microseconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.nanoseconds, nanoseconds_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.sign, sign_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.blank, blank_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.with, with, 1, attr);
    define_native_function(vm.names.negated, negated, 0, attr);
    define_native_function(vm.names.abs, abs, 0, attr);
    define_native_function(vm.names.total, total, 1, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 0, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

// 7.3.3 get Temporal.Duration.prototype.years, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.years
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::years_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return duration.[[Years]].
    return Value(duration->years());
}

// 7.3.4 get Temporal.Duration.prototype.months, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.months
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::months_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return duration.[[Months]].
    return Value(duration->months());
}

// 7.3.5 get Temporal.Duration.prototype.weeks, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.weeks
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::weeks_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return duration.[[Weeks]].
    return Value(duration->weeks());
}

// 7.3.6 get Temporal.Duration.prototype.days, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.days
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::days_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return duration.[[Days]].
    return Value(duration->days());
}

// 7.3.7 get Temporal.Duration.prototype.hours, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.hours
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::hours_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return duration.[[Hours]].
    return Value(duration->hours());
}

// 7.3.8 get Temporal.Duration.prototype.minutes, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.minutes
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::minutes_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return duration.[[Minutes]].
    return Value(duration->minutes());
}

// 7.3.9 get Temporal.Duration.prototype.seconds, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.seconds
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::seconds_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return duration.[[Seconds]].
    return Value(duration->seconds());
}

// 7.3.10 get Temporal.Duration.prototype.milliseconds, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.milliseconds
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::milliseconds_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return duration.[[Milliseconds]].
    return Value(duration->milliseconds());
}

// 7.3.11 get Temporal.Duration.prototype.microseconds, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.microseconds
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::microseconds_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return duration.[[Microseconds]].
    return Value(duration->microseconds());
}

// 7.3.12 get Temporal.Duration.prototype.nanoseconds, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.nanoseconds
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::nanoseconds_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return duration.[[Nanoseconds]].
    return Value(duration->nanoseconds());
}

// 7.3.13 get Temporal.Duration.prototype.sign, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.sign
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::sign_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return ! DurationSign(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]]).
    return Value(duration_sign(duration->years(), duration->months(), duration->weeks(), duration->days(), duration->hours(), duration->minutes(), duration->seconds(), duration->milliseconds(), duration->microseconds(), duration->nanoseconds()));
}

// 7.3.14 get Temporal.Duration.prototype.blank, https://tc39.es/proposal-temporal/#sec-get-temporal.duration.prototype.blank
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::blank_getter)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Let sign be ! DurationSign(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]]).
    auto sign = duration_sign(duration->years(), duration->months(), duration->weeks(), duration->days(), duration->hours(), duration->minutes(), duration->seconds(), duration->milliseconds(), duration->microseconds(), duration->nanoseconds());

    // 4. If sign = 0, return true.
    if (sign == 0)
        return Value(true);

    // 5. Return false.
    return Value(false);
}

// 7.3.15 Temporal.Duration.prototype.with ( temporalDurationLike ), https://tc39.es/proposal-temporal/#sec-temporal.duration.prototype.with
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::with)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Let temporalDurationLike be ? ToPartialDuration(temporalDurationLike).
    auto temporal_duration_like = TRY(to_partial_duration(global_object, vm.argument(0)));

    // 4. If temporalDurationLike.[[Years]] is not undefined, then
    //     a. Let years be temporalDurationLike.[[Years]].
    // 5. Else,
    //     a. Let years be duration.[[Years]].
    auto years = temporal_duration_like.years.value_or(duration->years());

    // 6. If temporalDurationLike.[[Months]] is not undefined, then
    //     a. Let months be temporalDurationLike.[[Months]].
    // 7. Else,
    //     a. Let months be duration.[[Months]].
    auto months = temporal_duration_like.months.value_or(duration->months());

    // 8. If temporalDurationLike.[[Weeks]] is not undefined, then
    //     a. Let weeks be temporalDurationLike.[[Weeks]].
    // 9. Else,
    //     a. Let weeks be duration.[[Weeks]].
    auto weeks = temporal_duration_like.weeks.value_or(duration->weeks());

    // 10. If temporalDurationLike.[[Days]] is not undefined, then
    //     a. Let days be temporalDurationLike.[[Days]].
    // 11. Else,
    //     a. Let days be duration.[[Days]].
    auto days = temporal_duration_like.days.value_or(duration->days());

    // 12. If temporalDurationLike.[[Hours]] is not undefined, then
    //     a. Let hours be temporalDurationLike.[[Hours]].
    // 13. Else,
    //     a. Let hours be duration.[[Hours]].
    auto hours = temporal_duration_like.hours.value_or(duration->hours());

    // 14. If temporalDurationLike.[[Minutes]] is not undefined, then
    //     a. Let minutes be temporalDurationLike.[[Minutes]].
    // 15. Else,
    //     a. Let minutes be duration.[[Minutes]].
    auto minutes = temporal_duration_like.minutes.value_or(duration->minutes());

    // 16. If temporalDurationLike.[[Seconds]] is not undefined, then
    //     a. Let seconds be temporalDurationLike.[[Seconds]].
    // 17. Else,
    //     a. Let seconds be duration.[[Seconds]].
    auto seconds = temporal_duration_like.seconds.value_or(duration->seconds());

    // 18. If temporalDurationLike.[[Milliseconds]] is not undefined, then
    //     a. Let milliseconds be temporalDurationLike.[[Milliseconds]].
    // 19. Else,
    //     a. Let milliseconds be duration.[[Milliseconds]].
    auto milliseconds = temporal_duration_like.milliseconds.value_or(duration->milliseconds());

    // 20. If temporalDurationLike.[[Microseconds]] is not undefined, then
    //     a. Let microseconds be temporalDurationLike.[[Microseconds]].
    // 21. Else,
    //     a. Let microseconds be duration.[[Microseconds]].
    auto microseconds = temporal_duration_like.microseconds.value_or(duration->microseconds());

    // 22. If temporalDurationLike.[[Nanoseconds]] is not undefined, then
    //     a. Let nanoseconds be temporalDurationLike.[[Nanoseconds]].
    // 23. Else,
    //     a. Let nanoseconds be duration.[[Nanoseconds]].
    auto nanoseconds = temporal_duration_like.nanoseconds.value_or(duration->nanoseconds());

    // 24. Return ? CreateTemporalDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
    return TRY(create_temporal_duration(global_object, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds));
}

// 7.3.16 Temporal.Duration.prototype.negated ( ), https://tc39.es/proposal-temporal/#sec-temporal.duration.prototype.negated
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::negated)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return ! CreateNegatedTemporalDuration(duration).
    return create_negated_temporal_duration(global_object, *duration);
}

// 7.3.17 Temporal.Duration.prototype.abs ( ), https://tc39.es/proposal-temporal/#sec-temporal.duration.prototype.abs
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::abs)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return ! CreateTemporalDuration(abs(duration.[[Years]]), abs(duration.[[Months]]), abs(duration.[[Weeks]]), abs(duration.[[Days]]), abs(duration.[[Hours]]), abs(duration.[[Minutes]]), abs(duration.[[Seconds]]), abs(duration.[[Milliseconds]]), abs(duration.[[Microseconds]]), abs(duration.[[Nanoseconds]])).
    return TRY(create_temporal_duration(global_object, fabs(duration->years()), fabs(duration->months()), fabs(duration->weeks()), fabs(duration->days()), fabs(duration->hours()), fabs(duration->minutes()), fabs(duration->seconds()), fabs(duration->milliseconds()), fabs(duration->microseconds()), fabs(duration->nanoseconds())));
}

// 7.3.21 Temporal.Duration.prototype.total ( options ), https://tc39.es/proposal-temporal/#sec-temporal.duration.prototype.total
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::total)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. If options is undefined, throw a TypeError exception.
    if (vm.argument(0).is_undefined())
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalMissingOptionsObject);

    // 4. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(0)));

    // 5. Let relativeTo be ? ToRelativeTemporalObject(options).
    auto relative_to = TRY(to_relative_temporal_object(global_object, *options));

    // 6. Let unit be ? ToTemporalDurationTotalUnit(options).
    auto unit = TRY(to_temporal_duration_total_unit(global_object, *options));

    // 7. Let unbalanceResult be ? UnbalanceDurationRelative(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], duration.[[Days]], unit, relativeTo).
    auto unbalance_result = TRY(unbalance_duration_relative(global_object, duration->years(), duration->months(), duration->weeks(), duration->days(), unit, relative_to));

    // 8. Let intermediate be undefined.
    ZonedDateTime* intermediate = nullptr;

    // 9. If relativeTo has an [[InitializedTemporalZonedDateTime]] internal slot, then
    if (relative_to.is_object() && is<ZonedDateTime>(relative_to.as_object())) {
        // a. Set intermediate to ? MoveRelativeZonedDateTime(relativeTo, unbalanceResult.[[Years]], unbalanceResult.[[Months]], unbalanceResult.[[Weeks]], 0).
        intermediate = TRY(move_relative_zoned_date_time(global_object, static_cast<ZonedDateTime&>(relative_to.as_object()), unbalance_result.years, unbalance_result.months, unbalance_result.weeks, 0));
    }

    // 10. Let balanceResult be ? BalanceDuration(unbalanceResult.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]], unit, intermediate).
    auto balance_result = TRY(balance_duration(global_object, unbalance_result.days, duration->hours(), duration->minutes(), duration->seconds(), duration->milliseconds(), duration->microseconds(), *js_bigint(vm, Crypto::SignedBigInteger::create_from(duration->nanoseconds())), unit, intermediate));

    // 11. Let roundResult be ? RoundDuration(unbalanceResult.[[Years]], unbalanceResult.[[Months]], unbalanceResult.[[Weeks]], balanceResult.[[Days]], balanceResult.[[Hours]], balanceResult.[[Minutes]], balanceResult.[[Seconds]], balanceResult.[[Milliseconds]], balanceResult.[[Microseconds]], balanceResult.[[Nanoseconds]], 1, unit, "trunc", relativeTo).
    auto round_result = TRY(round_duration(global_object, unbalance_result.years, unbalance_result.months, unbalance_result.weeks, balance_result.days, balance_result.hours, balance_result.minutes, balance_result.seconds, balance_result.milliseconds, balance_result.microseconds, balance_result.nanoseconds, 1, unit, "trunc"sv, relative_to.is_object() ? &relative_to.as_object() : nullptr));

    double whole;

    // 12. If unit is "year", then
    if (unit == "year"sv) {
        // a. Let whole be roundResult.[[Years]].
        whole = round_result.years;
    }

    // 13. If unit is "month", then
    if (unit == "month"sv) {
        // a. Let whole be roundResult.[[Months]].
        whole = round_result.months;
    }

    // 14. If unit is "week", then
    if (unit == "week"sv) {
        // a. Let whole be roundResult.[[Weeks]].
        whole = round_result.weeks;
    }

    // 15. If unit is "day", then
    if (unit == "day"sv) {
        // a. Let whole be roundResult.[[Days]].
        whole = round_result.days;
    }

    // 16. If unit is "hour", then
    if (unit == "hour"sv) {
        // a. Let whole be roundResult.[[Hours]].
        whole = round_result.hours;
    }

    // 17. If unit is "minute", then
    if (unit == "minute"sv) {
        // a. Let whole be roundResult.[[Minutes]].
        whole = round_result.minutes;
    }

    // 18. If unit is "second", then
    if (unit == "second"sv) {
        // a. Let whole be roundResult.[[Seconds]].
        whole = round_result.seconds;
    }

    // 19. If unit is "millisecond", then
    if (unit == "millisecond"sv) {
        // a. Let whole be roundResult.[[Milliseconds]].
        whole = round_result.milliseconds;
    }

    // 20. If unit is "microsecond", then
    if (unit == "microsecond"sv) {
        // a. Let whole be roundResult.[[Microseconds]].
        whole = round_result.microseconds;
    }

    // 21. If unit is "nanosecond", then
    if (unit == "nanosecond"sv) {
        // a. Let whole be roundResult.[[Nanoseconds]].
        whole = round_result.nanoseconds;
    }

    // 22. Return whole + roundResult.[[Remainder]].
    return whole + round_result.remainder;
}

// 7.3.22 Temporal.Duration.prototype.toString ( [ options ] ), https://tc39.es/proposal-temporal/#sec-temporal.duration.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::to_string)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(0)));

    // 4. Let precision be ? ToSecondsStringPrecision(options).
    auto precision = TRY(to_seconds_string_precision(global_object, *options));

    // 5. If precision.[[Unit]] is "minute", throw a RangeError exception.
    if (precision.unit == "minute"sv)
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, "minute"sv, "smallestUnit"sv);

    // 6. Let roundingMode be ? ToTemporalRoundingMode(options, "trunc").
    auto rounding_mode = TRY(to_temporal_rounding_mode(global_object, *options, "trunc"sv));

    // 7. Let result be ? RoundDuration(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]], precision.[[Increment]], precision.[[Unit]], roundingMode).
    auto result = TRY(round_duration(global_object, duration->years(), duration->months(), duration->weeks(), duration->days(), duration->hours(), duration->minutes(), duration->seconds(), duration->milliseconds(), duration->microseconds(), duration->nanoseconds(), precision.increment, precision.unit, rounding_mode));

    // 8. Return ! TemporalDurationToString(result.[[Years]], result.[[Months]], result.[[Weeks]], result.[[Days]], result.[[Hours]], result.[[Minutes]], result.[[Seconds]], result.[[Milliseconds]], result.[[Microseconds]], result.[[Nanoseconds]], precision.[[Precision]]).
    return js_string(vm, temporal_duration_to_string(result.years, result.months, result.weeks, result.days, result.hours, result.minutes, result.seconds, result.milliseconds, result.microseconds, result.nanoseconds, precision.precision));
}

// 7.3.23 Temporal.Duration.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.duration.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::to_json)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return ! TemporalDurationToString(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]], "auto").
    return js_string(vm, temporal_duration_to_string(duration->years(), duration->months(), duration->weeks(), duration->days(), duration->hours(), duration->minutes(), duration->seconds(), duration->milliseconds(), duration->microseconds(), duration->nanoseconds(), "auto"sv));
}

// 7.3.24 Temporal.Duration.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.duration.prototype.tolocalestring
// NOTE: This is the minimum toLocaleString implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::to_locale_string)
{
    // 1. Let duration be the this value.
    // 2. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration = TRY(typed_this_object(global_object));

    // 3. Return ! TemporalDurationToString(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]], "auto").
    return js_string(vm, temporal_duration_to_string(duration->years(), duration->months(), duration->weeks(), duration->days(), duration->hours(), duration->minutes(), duration->seconds(), duration->milliseconds(), duration->microseconds(), duration->nanoseconds(), "auto"sv));
}

// 7.3.25 Temporal.Duration.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.duration.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(DurationPrototype::value_of)
{
    // 1. Throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object, ErrorType::Convert, "Temporal.Duration", "a primitive value");
}

}
