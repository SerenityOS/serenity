/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/DurationConstructor.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>

namespace JS::Temporal {

// 7 Temporal.Duration Objects, https://tc39.es/proposal-temporal/#sec-temporal-duration-objects
Duration::Duration(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, Object& prototype)
    : Object(prototype)
    , m_years(years)
    , m_months(months)
    , m_weeks(weeks)
    , m_days(days)
    , m_hours(hours)
    , m_minutes(minutes)
    , m_seconds(seconds)
    , m_milliseconds(milliseconds)
    , m_microseconds(microseconds)
    , m_nanoseconds(nanoseconds)
{
}

// 7.5.1 ToTemporalDuration ( item ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalduration
Duration* to_temporal_duration(GlobalObject& global_object, Value item)
{
    auto& vm = global_object.vm();

    Optional<TemporalDuration> result;

    // 1. If Type(item) is Object, then
    if (item.is_object()) {
        // a. If item has an [[InitializedTemporalDuration]] internal slot, then
        if (is<Duration>(item.as_object())) {
            // i. Return item.
            return &static_cast<Duration&>(item.as_object());
        }
        // b. Let result be ? ToTemporalDurationRecord(item).
        result = to_temporal_duration_record(global_object, item.as_object());
        if (vm.exception())
            return {};
    }
    // 2. Else,
    else {
        // a. Let string be ? ToString(item).
        auto string = item.to_string(global_object);
        if (vm.exception())
            return {};

        // b. Let result be ? ParseTemporalDurationString(string).
        result = parse_temporal_duration_string(global_object, string);
        if (vm.exception())
            return {};
    }

    // 3. Return ? CreateTemporalDuration(result.[[Years]], result.[[Months]], result.[[Weeks]], result.[[Days]], result.[[Hours]], result.[[Minutes]], result.[[Seconds]], result.[[Milliseconds]], result.[[Microseconds]], result.[[Nanoseconds]]).
    return create_temporal_duration(global_object, result->years, result->months, result->weeks, result->days, result->hours, result->minutes, result->seconds, result->milliseconds, result->microseconds, result->nanoseconds);
}

// 7.5.2 ToTemporalDurationRecord ( temporalDurationLike ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaldurationrecord
TemporalDuration to_temporal_duration_record(GlobalObject& global_object, Object& temporal_duration_like)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(temporalDurationLike) is Object.

    // 2. If temporalDurationLike has an [[InitializedTemporalDuration]] internal slot, then
    if (is<Duration>(temporal_duration_like)) {
        auto& duration = static_cast<Duration&>(temporal_duration_like);

        // a. Return the Record { [[Years]]: temporalDurationLike.[[Years]], [[Months]]: temporalDurationLike.[[Months]], [[Weeks]]: temporalDurationLike.[[Weeks]], [[Days]]: temporalDurationLike.[[Days]], [[Hours]]: temporalDurationLike.[[Hours]], [[Minutes]]: temporalDurationLike.[[Minutes]], [[Seconds]]: temporalDurationLike.[[Seconds]], [[Milliseconds]]: temporalDurationLike.[[Milliseconds]], [[Microseconds]]: temporalDurationLike.[[Microseconds]], [[Nanoseconds]]: temporalDurationLike.[[Nanoseconds]] }.
        return TemporalDuration { .years = duration.years(), .months = duration.months(), .weeks = duration.weeks(), .days = duration.days(), .hours = duration.hours(), .minutes = duration.minutes(), .seconds = duration.seconds(), .milliseconds = duration.milliseconds(), .microseconds = duration.microseconds(), .nanoseconds = duration.nanoseconds() };
    }

    // 3. Let result be a new Record with all the internal slots given in the Internal Slot column in Table 7.
    auto result = TemporalDuration {};

    // 4. Let any be false.
    auto any = false;

    // 5. For each row of Table 7, except the header row, in table order, do
    for (auto& [internal_slot, property] : temporal_duration_like_properties<TemporalDuration, double>(vm)) {
        // a. Let prop be the Property value of the current row.

        // b. Let val be ? Get(temporalDurationLike, prop).
        auto value = temporal_duration_like.get(property);
        if (vm.exception())
            return {};

        // c. If val is undefined, then
        if (value.is_undefined()) {
            // i. Set result's internal slot whose name is the Internal Slot value of the current row to 0.
            result.*internal_slot = 0;
        }
        // d. Else,
        else {
            // i. Set any to true.
            any = true;

            // ii. Let val be ? ToNumber(val).
            value = value.to_number(global_object);
            if (vm.exception())
                return {};

            // iii. If ! IsIntegralNumber(val) is false, then
            if (!value.is_integral_number()) {
                // 1. Throw a RangeError exception.
                vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidDurationPropertyValueNonIntegral, property.as_string(), value.to_string_without_side_effects());
                return {};
            }

            // iv. Set result's internal slot whose name is the Internal Slot value of the current row to val.
            result.*internal_slot = value.as_double();
        }
    }

    // 6. If any is false, then
    if (!any) {
        // a. Throw a TypeError exception.
        vm.throw_exception<TypeError>(global_object, ErrorType::TemporalInvalidDurationLikeObject);
        return {};
    }

    // 7. Return result.
    return result;
}

// 7.5.3 DurationSign ( years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal-durationsign
i8 duration_sign(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds)
{
    // 1. For each value v of « years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds », do
    for (auto& v : { years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds }) {
        // a. If v < 0, return −1.
        if (v < 0)
            return -1;

        // b. If v > 0, return 1.
        if (v > 0)
            return 1;
    }

    // 2. Return 0.
    return 0;
}

// 7.5.4 IsValidDuration ( years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal-isvalidduration
bool is_valid_duration(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds)
{
    // 1. Let sign be ! DurationSign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
    auto sign = duration_sign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);

    // 2. For each value v of « years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds », do
    for (auto& v : { years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds }) {
        // a. If v is not finite, return false.
        if (!isfinite(v))
            return false;

        // b. If v < 0 and sign > 0, return false.
        if (v < 0 && sign > 0)
            return false;

        // c. If v > 0 and sign < 0, return false.
        if (v > 0 && sign < 0)
            return false;
    }

    // 3. Return true.
    return true;
}

// 7.5.6 ToPartialDuration ( temporalDurationLike ), https://tc39.es/proposal-temporal/#sec-temporal-topartialduration
PartialDuration to_partial_duration(GlobalObject& global_object, Value temporal_duration_like)
{
    auto& vm = global_object.vm();

    // 1. If Type(temporalDurationLike) is not Object, then
    if (!temporal_duration_like.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, temporal_duration_like.to_string_without_side_effects());
        return {};
    }

    // 2. Let result be the Record { [[Years]]: undefined, [[Months]]: undefined, [[Weeks]]: undefined, [[Days]]: undefined, [[Hours]]: undefined, [[Minutes]]: undefined, [[Seconds]]: undefined, [[Milliseconds]]: undefined, [[Microseconds]]: undefined, [[Nanoseconds]]: undefined }.
    auto result = PartialDuration {};

    // 3. Let any be false.
    auto any = false;

    // 4. For each row of Table 7, except the header row, in table order, do
    for (auto& [internal_slot, property] : temporal_duration_like_properties<PartialDuration, Optional<double>>(vm)) {
        // a. Let property be the Property value of the current row.

        // b. Let value be ? Get(temporalDurationLike, property).
        auto value = temporal_duration_like.as_object().get(property);
        if (vm.exception())
            return {};

        // c. If value is not undefined, then
        if (!value.is_undefined()) {
            // i. Set any to true.
            any = true;

            // ii. Set value to ? ToNumber(value).
            value = value.to_number(global_object);
            if (vm.exception())
                return {};

            // iii. If ! IsIntegralNumber(value) is false, then
            if (!value.is_integral_number()) {
                // 1. Throw a RangeError exception.
                vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidDurationPropertyValueNonIntegral, property.as_string(), value.to_string_without_side_effects());
                return {};
            }

            // iv. Set result's internal slot whose name is the Internal Slot value of the current row to value.
            result.*internal_slot = value.as_double();
        }
    }

    // 5. If any is false, then
    if (!any) {
        // a. Throw a TypeError exception.
        vm.throw_exception<TypeError>(global_object, ErrorType::TemporalInvalidDurationLikeObject);
        return {};
    }

    // 6. Return result.
    return result;
}

// 7.5.7 CreateTemporalDuration ( years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalduration
Duration* create_temporal_duration(GlobalObject& global_object, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, FunctionObject const* new_target)
{
    auto& vm = global_object.vm();

    // 1. If ! IsValidDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds) is false, throw a RangeError exception.
    if (!is_valid_duration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidDuration);
        return {};
    }

    // 2. If newTarget is not present, set it to %Temporal.Duration%.
    if (!new_target)
        new_target = global_object.temporal_duration_constructor();

    // 3. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.Duration.prototype%", « [[InitializedTemporalDuration]], [[Years]], [[Months]], [[Weeks]], [[Days]], [[Hours]], [[Minutes]], [[Seconds]], [[Milliseconds]], [[Microseconds]], [[Nanoseconds]] »).
    // 4. Set object.[[Years]] to years.
    // 5. Set object.[[Months]] to months.
    // 6. Set object.[[Weeks]] to weeks.
    // 7. Set object.[[Days]] to days.
    // 8. Set object.[[Hours]] to hours.
    // 9. Set object.[[Minutes]] to minutes.
    // 10. Set object.[[Seconds]] to seconds.
    // 11. Set object.[[Milliseconds]] to milliseconds.
    // 12. Set object.[[Microseconds]] to microseconds.
    // 13. Set object.[[Nanoseconds]] to nanoseconds.
    auto* object = ordinary_create_from_constructor<Duration>(global_object, *new_target, &GlobalObject::temporal_duration_prototype, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);
    if (vm.exception())
        return {};

    // 14. Return object.
    return object;
}

// 7.5.8 CreateNegatedTemporalDuration ( duration ), https://tc39.es/proposal-temporal/#sec-temporal-createnegatedtemporalduration
Duration* create_negated_temporal_duration(GlobalObject& global_object, Duration const& duration)
{
    // 1. Assert: Type(duration) is Object.
    // 2. Assert: duration has an [[InitializedTemporalDuration]] internal slot.

    // 3. Return ! CreateTemporalDuration(−duration.[[Years]], −duration.[[Months]], −duration.[[Weeks]], −duration.[[Days]], −duration.[[Hours]], −duration.[[Minutes]], −duration.[[Seconds]], −duration.[[Milliseconds]], −duration.[[Microseconds]], −duration.[[Nanoseconds]]).
    return create_temporal_duration(global_object, -duration.years(), -duration.months(), -duration.weeks(), -duration.days(), -duration.hours(), -duration.minutes(), -duration.seconds(), -duration.milliseconds(), -duration.microseconds(), -duration.nanoseconds());
}

// 7.5.10 TotalDurationNanoseconds ( days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, offsetShift ), https://tc39.es/proposal-temporal/#sec-temporal-totaldurationnanoseconds
BigInt* total_duration_nanoseconds(GlobalObject& global_object, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, BigInt const& nanoseconds, double offset_shift)
{
    auto& vm = global_object.vm();

    // 1. Assert: offsetShift is an integer.
    VERIFY(offset_shift == trunc(offset_shift));

    // 2. Set nanoseconds to ℝ(nanoseconds).
    auto result_nanoseconds = nanoseconds.big_integer();

    // TODO: Add a way to create SignedBigIntegers from doubles with full precision and remove this restriction
    VERIFY(AK::is_within_range<i64>(days) && AK::is_within_range<i64>(hours) && AK::is_within_range<i64>(minutes) && AK::is_within_range<i64>(seconds) && AK::is_within_range<i64>(milliseconds) && AK::is_within_range<i64>(microseconds));

    // 3. If days ≠ 0, then
    if (days != 0) {
        // a. Set nanoseconds to nanoseconds − offsetShift.
        result_nanoseconds = result_nanoseconds.minus(Crypto::SignedBigInteger::create_from(offset_shift));
    }
    // 4. Set hours to ℝ(hours) + ℝ(days) × 24.
    auto total_hours = Crypto::SignedBigInteger::create_from(hours).plus(Crypto::SignedBigInteger::create_from(days).multiplied_by(Crypto::UnsignedBigInteger(24)));
    // 5. Set minutes to ℝ(minutes) + hours × 60.
    auto total_minutes = Crypto::SignedBigInteger::create_from(minutes).plus(total_hours.multiplied_by(Crypto::UnsignedBigInteger(60)));
    // 6. Set seconds to ℝ(seconds) + minutes × 60.
    auto total_seconds = Crypto::SignedBigInteger::create_from(seconds).plus(total_minutes.multiplied_by(Crypto::UnsignedBigInteger(60)));
    // 7. Set milliseconds to ℝ(milliseconds) + seconds × 1000.
    auto total_milliseconds = Crypto::SignedBigInteger::create_from(milliseconds).plus(total_seconds.multiplied_by(Crypto::UnsignedBigInteger(1000)));
    // 8. Set microseconds to ℝ(microseconds) + milliseconds × 1000.
    auto total_microseconds = Crypto::SignedBigInteger::create_from(microseconds).plus(total_milliseconds.multiplied_by(Crypto::UnsignedBigInteger(1000)));
    // 9. Return nanoseconds + microseconds × 1000.
    return js_bigint(vm, result_nanoseconds.plus(total_microseconds.multiplied_by(Crypto::UnsignedBigInteger(1000))));
}

// 7.5.11 BalanceDuration ( days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, largestUnit [ , relativeTo ] ), https://tc39.es/proposal-temporal/#sec-temporal-balanceduration
Optional<BalancedDuration> balance_duration(GlobalObject& global_object, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, BigInt const& nanoseconds, String const& largest_unit, Object* relative_to)
{
    auto& vm = global_object.vm();
    // 1. If relativeTo is not present, set relativeTo to undefined.

    Crypto::SignedBigInteger total_nanoseconds;
    // 2. If Type(relativeTo) is Object and relativeTo has an [[InitializedTemporalZonedDateTime]] internal slot, then
    if (relative_to && is<ZonedDateTime>(*relative_to)) {
        // a. Let endNs be ? AddZonedDateTime(relativeTo.[[Nanoseconds]], relativeTo.[[TimeZone]], relativeTo.[[Calendar]], 0, 0, 0, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
        TODO();
        if (vm.exception())
            return {};
        // b. Set nanoseconds to endNs − relativeTo.[[Nanoseconds]].
    }
    // 3. Else,
    else {
        // a. Set nanoseconds to ℤ(! TotalDurationNanoseconds(days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0)).
        total_nanoseconds = total_duration_nanoseconds(global_object, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0)->big_integer();
    }

    // 4. If largestUnit is one of "year", "month", "week", or "day", then
    if (largest_unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv)) {
        // a. Let result be ? NanosecondsToDays(nanoseconds, relativeTo).
        TODO();
        if (vm.exception())
            return {};

        // b. Set days to result.[[Days]].

        // c. Set nanoseconds to result.[[Nanoseconds]].
    }
    // 5. Else,
    else {
        // a. Set days to 0.
        days = 0;
    }
    // 6. Set hours, minutes, seconds, milliseconds, and microseconds to 0.
    hours = 0;
    minutes = 0;
    seconds = 0;
    milliseconds = 0;
    microseconds = 0;

    // 7. Set nanoseconds to ℝ(nanoseconds).
    double result_nanoseconds = total_nanoseconds.to_double();

    // 8. If nanoseconds < 0, let sign be −1; else, let sign be 1.
    i8 sign = total_nanoseconds.is_negative() ? -1 : 1;

    // 9. Set nanoseconds to abs(nanoseconds).
    total_nanoseconds = Crypto::SignedBigInteger(total_nanoseconds.unsigned_value());
    result_nanoseconds = fabs(result_nanoseconds);

    // 10. If largestUnit is "year", "month", "week", "day", or "hour", then
    if (largest_unit.is_one_of("year"sv, "month"sv, "day"sv, "hour"sv)) {
        // a. Set microseconds to floor(nanoseconds / 1000).
        auto nanoseconds_division_result = total_nanoseconds.divided_by(Crypto::UnsignedBigInteger(1000));
        // b. Set nanoseconds to nanoseconds modulo 1000.
        result_nanoseconds = nanoseconds_division_result.remainder.to_double();
        // c. Set milliseconds to floor(microseconds / 1000).
        auto microseconds_division_result = nanoseconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(1000));
        // d. Set microseconds to microseconds modulo 1000.
        microseconds = microseconds_division_result.remainder.to_double();
        // e. Set seconds to floor(milliseconds / 1000).
        auto milliseconds_division_result = microseconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(1000));
        // f. Set milliseconds to milliseconds modulo 1000.
        milliseconds = milliseconds_division_result.remainder.to_double();
        // g. Set minutes to floor(seconds / 60).
        auto seconds_division_result = milliseconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(60));
        // h. Set seconds to seconds modulo 60.
        seconds = seconds_division_result.remainder.to_double();
        // i. Set hours to floor(minutes / 60).
        auto minutes_division_result = milliseconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(60));
        hours = minutes_division_result.quotient.to_double();
        // j. Set minutes to minutes modulo 60.
        minutes = minutes_division_result.remainder.to_double();
    }
    // 11. Else if largestUnit is "minute", then
    else if (largest_unit == "minute"sv) {
        // a. Set microseconds to floor(nanoseconds / 1000).
        auto nanoseconds_division_result = total_nanoseconds.divided_by(Crypto::UnsignedBigInteger(1000));
        // b. Set nanoseconds to nanoseconds modulo 1000.
        result_nanoseconds = nanoseconds_division_result.remainder.to_double();
        // c. Set milliseconds to floor(microseconds / 1000).
        auto microseconds_division_result = nanoseconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(1000));
        // d. Set microseconds to microseconds modulo 1000.
        microseconds = microseconds_division_result.remainder.to_double();
        // e. Set seconds to floor(milliseconds / 1000).
        auto milliseconds_division_result = microseconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(1000));
        // f. Set milliseconds to milliseconds modulo 1000.
        milliseconds = milliseconds_division_result.remainder.to_double();
        // g. Set minutes to floor(seconds / 60).
        auto seconds_division_result = milliseconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(60));
        minutes = seconds_division_result.quotient.to_double();
        // h. Set seconds to seconds modulo 60.
        seconds = seconds_division_result.remainder.to_double();
    }
    // 12. Else if largestUnit is "second", then
    else if (largest_unit == "second"sv) {
        // a. Set microseconds to floor(nanoseconds / 1000).
        auto nanoseconds_division_result = total_nanoseconds.divided_by(Crypto::UnsignedBigInteger(1000));
        // b. Set nanoseconds to nanoseconds modulo 1000.
        result_nanoseconds = nanoseconds_division_result.remainder.to_double();
        // c. Set milliseconds to floor(microseconds / 1000).
        auto microseconds_division_result = nanoseconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(1000));
        // d. Set microseconds to microseconds modulo 1000.
        microseconds = microseconds_division_result.remainder.to_double();
        // e. Set seconds to floor(milliseconds / 1000).
        auto milliseconds_division_result = microseconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(1000));
        seconds = milliseconds_division_result.quotient.to_double();
        // f. Set milliseconds to milliseconds modulo 1000.
        milliseconds = milliseconds_division_result.remainder.to_double();
    }
    // 13. Else if largestUnit is "millisecond", then
    else if (largest_unit == "millisecond"sv) {
        // a. Set microseconds to floor(nanoseconds / 1000).
        auto nanoseconds_division_result = total_nanoseconds.divided_by(Crypto::UnsignedBigInteger(1000));
        // b. Set nanoseconds to nanoseconds modulo 1000.
        result_nanoseconds = nanoseconds_division_result.remainder.to_double();
        // c. Set milliseconds to floor(microseconds / 1000).
        auto microseconds_division_result = nanoseconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(1000));
        milliseconds = microseconds_division_result.quotient.to_double();
        // d. Set microseconds to microseconds modulo 1000.
        microseconds = microseconds_division_result.remainder.to_double();
    }
    // 14. Else if largestUnit is "microsecond", then
    else if (largest_unit == "microsecond"sv) {
        // a. Set microseconds to floor(nanoseconds / 1000).
        auto nanoseconds_division_result = total_nanoseconds.divided_by(Crypto::UnsignedBigInteger(1000));
        microseconds = nanoseconds_division_result.quotient.to_double();
        // b. Set nanoseconds to nanoseconds modulo 1000.
        result_nanoseconds = nanoseconds_division_result.remainder.to_double();
    }
    // 15. Else,
    else {
        // a. Assert: largestUnit is "nanosecond".
        VERIFY(largest_unit == "nanosecond"sv);
    }
    // 16. Return the Record { [[Days]]: 𝔽(days), [[Hours]]: 𝔽(hours × sign), [[Minutes]]: 𝔽(minutes × sign), [[Seconds]]: 𝔽(seconds × sign), [[Milliseconds]]: 𝔽(milliseconds × sign), [[Microseconds]]: 𝔽(microseconds × sign), [[Nanoseconds]]: 𝔽(nanoseconds × sign) }.
    return BalancedDuration { .days = days, .hours = hours * sign, .minutes = minutes * sign, .seconds = seconds * sign, .milliseconds = milliseconds * sign, .microseconds = microseconds * sign, .nanoseconds = result_nanoseconds * sign };
}

// 7.5.20 ToLimitedTemporalDuration ( temporalDurationLike, disallowedFields ),https://tc39.es/proposal-temporal/#sec-temporal-tolimitedtemporalduration
Optional<TemporalDuration> to_limited_temporal_duration(GlobalObject& global_object, Value temporal_duration_like, Vector<StringView> const& disallowed_fields)
{
    auto& vm = global_object.vm();

    Optional<TemporalDuration> duration;

    // 1. If Type(temporalDurationLike) is not Object, then
    if (!temporal_duration_like.is_object()) {
        // a. Let str be ? ToString(temporalDurationLike).
        auto str = temporal_duration_like.to_string(global_object);
        if (vm.exception())
            return {};

        // b. Let duration be ? ParseTemporalDurationString(str).
        duration = parse_temporal_duration_string(global_object, str);
        if (vm.exception())
            return {};
    }
    // 2. Else,
    else {
        // a. Let duration be ? ToTemporalDurationRecord(temporalDurationLike).
        duration = to_temporal_duration_record(global_object, temporal_duration_like.as_object());
        if (vm.exception())
            return {};
    }

    // 3. If ! IsValidDuration(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]]) is false, throw a RangeError exception.
    if (!is_valid_duration(duration->years, duration->months, duration->weeks, duration->days, duration->hours, duration->minutes, duration->seconds, duration->milliseconds, duration->microseconds, duration->nanoseconds)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidDuration);
        return {};
    }

    // 4. For each row of Table 7, except the header row, in table order, do
    for (auto& [internal_slot, property] : temporal_duration_like_properties<TemporalDuration, double>(vm)) {
        // a. Let prop be the Property value of the current row.

        // b. Let value be duration's internal slot whose name is the Internal Slot value of the current row.
        auto value = (*duration).*internal_slot;

        // If value is not 0 and disallowedFields contains prop, then
        if (value != 0 && disallowed_fields.contains_slow(property.as_string())) {
            // i. Throw a RangeError exception.
            vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidDurationPropertyValueNonZero, property.as_string(), value);
            return {};
        }
    }

    // 5. Return duration.
    return duration;
}

}
