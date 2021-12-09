/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/DurationConstructor.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
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
ThrowCompletionOr<Duration*> to_temporal_duration(GlobalObject& global_object, Value item)
{
    TemporalDuration result;

    // 1. If Type(item) is Object, then
    if (item.is_object()) {
        // a. If item has an [[InitializedTemporalDuration]] internal slot, then
        if (is<Duration>(item.as_object())) {
            // i. Return item.
            return &static_cast<Duration&>(item.as_object());
        }
        // b. Let result be ? ToTemporalDurationRecord(item).
        result = TRY(to_temporal_duration_record(global_object, item.as_object()));
    }
    // 2. Else,
    else {
        // a. Let string be ? ToString(item).
        auto string = TRY(item.to_string(global_object));

        // b. Let result be ? ParseTemporalDurationString(string).
        result = TRY(parse_temporal_duration_string(global_object, string));
    }

    // 3. Return ? CreateTemporalDuration(result.[[Years]], result.[[Months]], result.[[Weeks]], result.[[Days]], result.[[Hours]], result.[[Minutes]], result.[[Seconds]], result.[[Milliseconds]], result.[[Microseconds]], result.[[Nanoseconds]]).
    return create_temporal_duration(global_object, result.years, result.months, result.weeks, result.days, result.hours, result.minutes, result.seconds, result.milliseconds, result.microseconds, result.nanoseconds);
}

// 7.5.2 ToTemporalDurationRecord ( temporalDurationLike ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaldurationrecord
ThrowCompletionOr<TemporalDuration> to_temporal_duration_record(GlobalObject& global_object, Object const& temporal_duration_like)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(temporalDurationLike) is Object.

    // 2. If temporalDurationLike has an [[InitializedTemporalDuration]] internal slot, then
    if (is<Duration>(temporal_duration_like)) {
        auto& duration = static_cast<Duration const&>(temporal_duration_like);

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
        auto value = TRY(temporal_duration_like.get(property));

        // c. If val is undefined, then
        if (value.is_undefined()) {
            // i. Set result's internal slot whose name is the Internal Slot value of the current row to 0.
            result.*internal_slot = 0;
        }
        // d. Else,
        else {
            // i. Set any to true.
            any = true;

            // ii. Let val be 𝔽(? ToIntegerWithoutRounding(val)).
            value = Value(TRY(to_integer_without_rounding(global_object, value, ErrorType::TemporalInvalidDurationPropertyValueNonIntegral, property.as_string(), value.to_string_without_side_effects())));

            // iii. Set result's internal slot whose name is the Internal Slot value of the current row to val.
            result.*internal_slot = value.as_double();
        }
    }

    // 6. If any is false, then
    if (!any) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalInvalidDurationLikeObject);
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

// 7.5.5 DefaultTemporalLargestUnit ( years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds ), https://tc39.es/proposal-temporal/#sec-temporal-defaulttemporallargestunit
StringView default_temporal_largest_unit(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds)
{
    // 1. If years is not zero, return "year".
    if (years != 0)
        return "year"sv;

    // 2. If months is not zero, return "month".
    if (months != 0)
        return "month"sv;

    // 3. If weeks is not zero, return "week".
    if (weeks != 0)
        return "week"sv;

    // 4. If days is not zero, return "day".
    if (days != 0)
        return "day"sv;

    // 5. If hours is not zero, return "hour".
    if (hours != 0)
        return "hour"sv;

    // 6. If minutes is not zero, return "minute".
    if (minutes != 0)
        return "minute"sv;

    // 7. If seconds is not zero, return "second".
    if (seconds != 0)
        return "second"sv;

    // 8. If milliseconds is not zero, return "millisecond".
    if (milliseconds != 0)
        return "millisecond"sv;

    // 9. If microseconds is not zero, return "microsecond".
    if (microseconds != 0)
        return "microsecond"sv;

    // 10. Return "nanosecond".
    return "nanosecond"sv;
}

// 7.5.6 ToPartialDuration ( temporalDurationLike ), https://tc39.es/proposal-temporal/#sec-temporal-topartialduration
ThrowCompletionOr<PartialDuration> to_partial_duration(GlobalObject& global_object, Value temporal_duration_like)
{
    auto& vm = global_object.vm();

    // 1. If Type(temporalDurationLike) is not Object, then
    if (!temporal_duration_like.is_object()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, temporal_duration_like.to_string_without_side_effects());
    }

    // 2. Let result be the Record { [[Years]]: undefined, [[Months]]: undefined, [[Weeks]]: undefined, [[Days]]: undefined, [[Hours]]: undefined, [[Minutes]]: undefined, [[Seconds]]: undefined, [[Milliseconds]]: undefined, [[Microseconds]]: undefined, [[Nanoseconds]]: undefined }.
    auto result = PartialDuration {};

    // 3. Let any be false.
    auto any = false;

    // 4. For each row of Table 7, except the header row, in table order, do
    for (auto& [internal_slot, property] : temporal_duration_like_properties<PartialDuration, Optional<double>>(vm)) {
        // a. Let property be the Property value of the current row.

        // b. Let value be ? Get(temporalDurationLike, property).
        auto value = TRY(temporal_duration_like.as_object().get(property));

        // c. If value is not undefined, then
        if (!value.is_undefined()) {
            // i. Set any to true.
            any = true;

            // ii. Set value to 𝔽(? ToIntegerWithoutRounding(value)).
            value = Value(TRY(to_integer_without_rounding(global_object, value, ErrorType::TemporalInvalidDurationPropertyValueNonIntegral, property.as_string(), value.to_string_without_side_effects())));

            // iii. Set result's internal slot whose name is the Internal Slot value of the current row to value.
            result.*internal_slot = value.as_double();
        }
    }

    // 5. If any is false, then
    if (!any) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalInvalidDurationLikeObject);
    }

    // 6. Return result.
    return result;
}

// 7.5.7 CreateTemporalDuration ( years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalduration
ThrowCompletionOr<Duration*> create_temporal_duration(GlobalObject& global_object, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, FunctionObject const* new_target)
{
    auto& vm = global_object.vm();

    // 1. If ! IsValidDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds) is false, throw a RangeError exception.
    if (!is_valid_duration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDuration);

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
    auto* object = TRY(ordinary_create_from_constructor<Duration>(global_object, *new_target, &GlobalObject::temporal_duration_prototype, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds));

    // 14. Return object.
    return object;
}

// 7.5.8 CreateNegatedTemporalDuration ( duration ), https://tc39.es/proposal-temporal/#sec-temporal-createnegatedtemporalduration
Duration* create_negated_temporal_duration(GlobalObject& global_object, Duration const& duration)
{
    // 1. Assert: Type(duration) is Object.
    // 2. Assert: duration has an [[InitializedTemporalDuration]] internal slot.

    // 3. Return ! CreateTemporalDuration(−duration.[[Years]], −duration.[[Months]], −duration.[[Weeks]], −duration.[[Days]], −duration.[[Hours]], −duration.[[Minutes]], −duration.[[Seconds]], −duration.[[Milliseconds]], −duration.[[Microseconds]], −duration.[[Nanoseconds]]).
    return MUST(create_temporal_duration(global_object, -duration.years(), -duration.months(), -duration.weeks(), -duration.days(), -duration.hours(), -duration.minutes(), -duration.seconds(), -duration.milliseconds(), -duration.microseconds(), -duration.nanoseconds()));
}

// 7.5.9 CalculateOffsetShift ( relativeTo, y, mon, w, d, h, min, s, ms, mus, ns ), https://tc39.es/proposal-temporal/#sec-temporal-calculateoffsetshift
ThrowCompletionOr<double> calculate_offset_shift(GlobalObject& global_object, Value relative_to_value, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds)
{
    // 1. If Type(relativeTo) is not Object or relativeTo does not have an [[InitializedTemporalZonedDateTime]] internal slot, return 0.
    if (!relative_to_value.is_object() || !is<ZonedDateTime>(relative_to_value.as_object()))
        return 0.0;

    auto& relative_to = static_cast<ZonedDateTime&>(relative_to_value.as_object());

    // 2. Let instant be ! CreateTemporalInstant(relativeTo.[[Nanoseconds]]).
    auto* instant = MUST(create_temporal_instant(global_object, relative_to.nanoseconds()));

    // 3. Let offsetBefore be ? GetOffsetNanosecondsFor(relativeTo.[[TimeZone]], instant).
    auto offset_before = TRY(get_offset_nanoseconds_for(global_object, &relative_to.time_zone(), *instant));

    // 4. Let after be ? AddZonedDateTime(relativeTo.[[Nanoseconds]], relativeTo.[[TimeZone]], relativeTo.[[Calendar]], y, mon, w, d, h, min, s, ms, mus, ns).
    auto* after = TRY(add_zoned_date_time(global_object, relative_to.nanoseconds(), &relative_to.time_zone(), relative_to.calendar(), years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds));

    // 5. Let instantAfter be ! CreateTemporalInstant(after).
    auto* instant_after = MUST(create_temporal_instant(global_object, *after));

    // 6. Let offsetAfter be ? GetOffsetNanosecondsFor(relativeTo.[[TimeZone]], instantAfter).
    auto offset_after = TRY(get_offset_nanoseconds_for(global_object, &relative_to.time_zone(), *instant_after));

    // 7. Return offsetAfter − offsetBefore.
    return offset_after - offset_before;
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
ThrowCompletionOr<BalancedDuration> balance_duration(GlobalObject& global_object, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, BigInt const& nanoseconds, String const& largest_unit, Object* relative_to)
{
    auto& vm = global_object.vm();

    // 1. If relativeTo is not present, set relativeTo to undefined.

    Crypto::SignedBigInteger total_nanoseconds;
    // 2. If Type(relativeTo) is Object and relativeTo has an [[InitializedTemporalZonedDateTime]] internal slot, then
    if (relative_to && is<ZonedDateTime>(*relative_to)) {
        auto& relative_to_zoned_date_time = static_cast<ZonedDateTime&>(*relative_to);

        // a. Let endNs be ? AddZonedDateTime(relativeTo.[[Nanoseconds]], relativeTo.[[TimeZone]], relativeTo.[[Calendar]], 0, 0, 0, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
        auto* end_ns = TRY(add_zoned_date_time(global_object, relative_to_zoned_date_time.nanoseconds(), &relative_to_zoned_date_time.time_zone(), relative_to_zoned_date_time.calendar(), 0, 0, 0, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds.big_integer().to_double()));

        // b. Set nanoseconds to endNs − relativeTo.[[Nanoseconds]].
        total_nanoseconds = end_ns->big_integer().minus(relative_to_zoned_date_time.nanoseconds().big_integer());
    }
    // 3. Else,
    else {
        // a. Set nanoseconds to ℤ(! TotalDurationNanoseconds(days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0)).
        total_nanoseconds = total_duration_nanoseconds(global_object, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0)->big_integer();
    }

    // 4. If largestUnit is one of "year", "month", "week", or "day", then
    if (largest_unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv)) {
        // a. Let result be ? NanosecondsToDays(nanoseconds, relativeTo).
        auto result = TRY(nanoseconds_to_days(global_object, *js_bigint(vm, total_nanoseconds), relative_to ?: js_undefined()));

        // b. Set days to result.[[Days]].
        days = result.days;

        // c. Set nanoseconds to result.[[Nanoseconds]].
        total_nanoseconds = result.nanoseconds->big_integer();
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
    if (largest_unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv, "hour"sv)) {
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
        auto minutes_division_result = seconds_division_result.quotient.divided_by(Crypto::UnsignedBigInteger(60));
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

// 7.5.12 UnbalanceDurationRelative ( years, months, weeks, days, largestUnit, relativeTo ), https://tc39.es/proposal-temporal/#sec-temporal-unbalancedurationrelative
ThrowCompletionOr<UnbalancedDuration> unbalance_duration_relative(GlobalObject& global_object, double years, double months, double weeks, double days, String const& largest_unit, Value relative_to)
{
    auto& vm = global_object.vm();

    // 1. If largestUnit is "year", or years, months, weeks, and days are all 0, then
    if (largest_unit == "year"sv || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
        // a. Return the Record { [[Years]]: years, [[Months]]: months, [[Weeks]]: weeks, [[Days]]: days }.
        return UnbalancedDuration { .years = years, .months = months, .weeks = weeks, .days = days };
    }

    // 2. Let sign be ! DurationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0).
    auto sign = duration_sign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);

    // 3. Assert: sign ≠ 0.
    VERIFY(sign != 0);

    // 4. Let oneYear be ! CreateTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0).
    auto* one_year = MUST(create_temporal_duration(global_object, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0));

    // 5. Let oneMonth be ! CreateTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0).
    auto* one_month = MUST(create_temporal_duration(global_object, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0));

    // 6. Let oneWeek be ! CreateTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0).
    auto* one_week = MUST(create_temporal_duration(global_object, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0));

    Object* calendar;

    // 7. If relativeTo is not undefined, then
    if (!relative_to.is_undefined()) {
        // a. Set relativeTo to ? ToTemporalDate(relativeTo).
        auto* relative_to_plain_date = TRY(to_temporal_date(global_object, relative_to));
        relative_to = relative_to_plain_date;

        // b. Let calendar be relativeTo.[[Calendar]].
        calendar = &relative_to_plain_date->calendar();
    }
    // 8. Else,
    else {
        // a. Let calendar be undefined.
        calendar = nullptr;
    }

    // 9. If largestUnit is "month", then
    if (largest_unit == "month"sv) {
        // a. If calendar is undefined, then
        if (!calendar) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalMissingStartingPoint, "months");
        }

        // b. Let dateAdd be ? GetMethod(calendar, "dateAdd").
        auto* date_add = TRY(Value(calendar).get_method(global_object, vm.names.dateAdd));

        // c. Let dateUntil be ? GetMethod(calendar, "dateUntil").
        auto* date_until = TRY(Value(calendar).get_method(global_object, vm.names.dateUntil));

        // d. Repeat, while years ≠ 0,
        while (years != 0) {
            // i. Let addOptions be ! OrdinaryObjectCreate(null).
            auto* add_options = Object::create(global_object, nullptr);

            // ii. Let newRelativeTo be ? CalendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd).
            auto* new_relative_to = TRY(calendar_date_add(global_object, *calendar, relative_to, *one_year, add_options, date_add));

            // iii. Let untilOptions be ! OrdinaryObjectCreate(null).
            auto* until_options = Object::create(global_object, nullptr);

            // iv. Perform ! CreateDataPropertyOrThrow(untilOptions, "largestUnit", "month").
            MUST(until_options->create_data_property_or_throw(vm.names.largestUnit, js_string(vm, "month"sv)));

            // v. Let untilResult be ? CalendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil).
            auto* until_result = TRY(calendar_date_until(global_object, *calendar, relative_to, new_relative_to, *until_options, date_until));

            // vi. Let oneYearMonths be untilResult.[[Months]].
            auto one_year_months = until_result->months();

            // vii. Set relativeTo to newRelativeTo.
            relative_to = new_relative_to;

            // viii. Set years to years − sign.
            years -= sign;

            // ix. Set months to months + oneYearMonths.
            months += one_year_months;
        }
    }
    // 10. Else if largestUnit is "week", then
    else if (largest_unit == "week"sv) {
        // a. If calendar is undefined, then
        if (!calendar) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalMissingStartingPoint, "weeks");
        }

        // b. Repeat, while years ≠ 0,
        while (years != 0) {
            // i. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneYear).
            auto move_result = TRY(move_relative_date(global_object, *calendar, verify_cast<PlainDate>(relative_to.as_object()), *one_year));

            // ii. Set relativeTo to moveResult.[[RelativeTo]].
            relative_to = move_result.relative_to.cell();

            // iii. Set days to days + moveResult.[[Days]].
            days += move_result.days;

            // iv. Set years to years − sign.
            years -= sign;
        }

        // c. Repeat, while months ≠ 0,
        while (months != 0) {
            // i. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneMonth).
            auto move_result = TRY(move_relative_date(global_object, *calendar, verify_cast<PlainDate>(relative_to.as_object()), *one_month));

            // ii. Set relativeTo to moveResult.[[RelativeTo]].
            relative_to = move_result.relative_to.cell();

            // iii. Set days to days + moveResult.[[Days]].
            days += move_result.days;

            // iv. Set months to months − sign.
            months -= sign;
        }
    }
    // 11. Else,
    else {
        // a. If any of years, months, and weeks are not zero, then
        if (years != 0 || months != 0 || weeks != 0) {
            // i. If calendar is undefined, then
            if (!calendar) {
                // i. Throw a RangeError exception.
                return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalMissingStartingPoint, "calendar units");
            }

            // ii. Repeat, while years ≠ 0,
            while (years != 0) {
                // 1. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneYear).
                auto move_result = TRY(move_relative_date(global_object, *calendar, verify_cast<PlainDate>(relative_to.as_object()), *one_year));

                // 2. Set relativeTo to moveResult.[[RelativeTo]].
                relative_to = move_result.relative_to.cell();

                // 3. Set days to days + moveResult.[[Days]].
                days += move_result.days;

                // 4. Set years to years − sign.
                years -= sign;
            }

            // iii. Repeat, while months ≠ 0,
            while (months != 0) {
                // 1. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneMonth).
                auto move_result = TRY(move_relative_date(global_object, *calendar, verify_cast<PlainDate>(relative_to.as_object()), *one_month));

                // 2. Set relativeTo to moveResult.[[RelativeTo]].
                relative_to = move_result.relative_to.cell();

                // 3. Set days to days +moveResult.[[Days]].
                days += move_result.days;

                // 4. Set months to months − sign.
                months -= sign;
            }

            // iv. Repeat, while weeks ≠ 0,
            while (weeks != 0) {
                // 1. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneWeek).
                auto move_result = TRY(move_relative_date(global_object, *calendar, verify_cast<PlainDate>(relative_to.as_object()), *one_week));

                // 2. Set relativeTo to moveResult.[[RelativeTo]].
                relative_to = move_result.relative_to.cell();

                // 3. Set days to days + moveResult.[[Days]].
                days += move_result.days;

                // 4. Set weeks to weeks − sign.
                weeks -= sign;
            }
        }
    }

    // 12. Return the Record { [[Years]]: years, [[Months]]: months, [[Weeks]]: weeks, [[Days]]: days }.
    return UnbalancedDuration { .years = years, .months = months, .weeks = weeks, .days = days };
}

// 7.5.13 BalanceDurationRelative ( years, months, weeks, days, largestUnit, relativeTo ), https://tc39.es/proposal-temporal/#sec-temporal-balancedurationrelative
ThrowCompletionOr<RelativeBalancedDuration> balance_duration_relative(GlobalObject& global_object, double years, double months, double weeks, double days, String const& largest_unit, Value relative_to_value)
{
    auto& vm = global_object.vm();

    // 1. If largestUnit is not one of "year", "month", or "week", or years, months, weeks, and days are all 0, then
    if (!largest_unit.is_one_of("year"sv, "month"sv, "week"sv) || (years == 0 && months == 0 && weeks == 0 && days == 0)) {
        // a. Return the Record { [[Years]]: years, [[Months]]: months, [[Weeks]]: weeks, [[Days]]: days }.
        return RelativeBalancedDuration { .years = years, .months = months, .weeks = weeks, .days = days };
    }

    // 2. Let sign be ! DurationSign(years, months, weeks, days, 0, 0, 0, 0, 0, 0).
    auto sign = duration_sign(years, months, weeks, days, 0, 0, 0, 0, 0, 0);

    // 3. Assert: sign ≠ 0.
    VERIFY(sign != 0);

    // 4. Let oneYear be ! CreateTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0).
    auto* one_year = MUST(create_temporal_duration(global_object, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0));

    // 5. Let oneMonth be ! CreateTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0).
    auto* one_month = MUST(create_temporal_duration(global_object, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0));

    // 6. Let oneWeek be ! CreateTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0).
    auto* one_week = MUST(create_temporal_duration(global_object, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0));

    // 7. Set relativeTo to ? ToTemporalDate(relativeTo).
    auto* relative_to = TRY(to_temporal_date(global_object, relative_to_value));

    // 8. Let calendar be relativeTo.[[Calendar]].
    auto& calendar = relative_to->calendar();

    // 9. If largestUnit is "year", then
    if (largest_unit == "year"sv) {
        // a. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneYear).
        auto move_result = TRY(move_relative_date(global_object, calendar, *relative_to, *one_year));

        // b. Set relativeTo to moveResult.[[RelativeTo]].
        relative_to = move_result.relative_to.cell();

        // c. Let oneYearDays be moveResult.[[Days]].
        auto one_year_days = move_result.days;

        // d. Repeat, while abs(days) ≥ abs(oneYearDays),
        while (fabs(days) >= fabs(one_year_days)) {
            // i. Set days to days − oneYearDays.
            days -= one_year_days;

            // ii. Set years to years + sign.
            years += sign;

            // iii. Set moveResult to ? MoveRelativeDate(calendar, relativeTo, oneYear).
            move_result = TRY(move_relative_date(global_object, calendar, *relative_to, *one_year));

            // iv. Set relativeTo to moveResult.[[RelativeTo]].
            relative_to = move_result.relative_to.cell();

            // v. Set oneYearDays to moveResult.[[Days]].
            one_year_days = move_result.days;
        }

        // e. Set moveResult to ? MoveRelativeDate(calendar, relativeTo, oneMonth).
        move_result = TRY(move_relative_date(global_object, calendar, *relative_to, *one_month));

        // f. Set relativeTo to moveResult.[[RelativeTo]].
        relative_to = move_result.relative_to.cell();

        // g. Let oneMonthDays be moveResult.[[Days]].
        auto one_month_days = move_result.days;

        // h. Repeat, while abs(days) ≥ abs(oneMonthDays),
        while (fabs(days) >= fabs(one_month_days)) {
            // i. Set days to days − oneMonthDays.
            days -= one_month_days;

            // ii. Set months to months + sign.
            months += sign;

            // iii. Set moveResult to ? MoveRelativeDate(calendar, relativeTo, oneMonth).
            move_result = TRY(move_relative_date(global_object, calendar, *relative_to, *one_month));

            // iv. Set relativeTo to moveResult.[[RelativeTo]].
            relative_to = move_result.relative_to.cell();

            // v. Set oneMonthDays to moveResult.[[Days]].
            one_month_days = move_result.days;
        }

        // i. Let dateAdd be ? GetMethod(calendar, "dateAdd").
        auto* date_add = TRY(Value(&calendar).get_method(global_object, vm.names.dateAdd));

        // j. Let addOptions be ! OrdinaryObjectCreate(null).
        auto* add_options = Object::create(global_object, nullptr);

        // k. Let newRelativeTo be ? CalendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd).
        auto* new_relative_to = TRY(calendar_date_add(global_object, calendar, relative_to, *one_year, add_options, date_add));

        // l. Let dateUntil be ? GetMethod(calendar, "dateUntil").
        auto* date_until = TRY(Value(&calendar).get_method(global_object, vm.names.dateUntil));

        // m. Let untilOptions be ! OrdinaryObjectCreate(null).
        auto* until_options = Object::create(global_object, nullptr);

        // n. Perform ! CreateDataPropertyOrThrow(untilOptions, "largestUnit", "month").
        MUST(until_options->create_data_property_or_throw(vm.names.largestUnit, js_string(vm, "month"sv)));

        // o. Let untilResult be ? CalendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil).
        auto* until_result = TRY(calendar_date_until(global_object, calendar, relative_to, new_relative_to, *until_options, date_until));

        // p. Let oneYearMonths be untilResult.[[Months]].
        auto one_year_months = until_result->months();

        // q. Repeat, while abs(months) ≥ abs(oneYearMonths),
        while (fabs(months) >= fabs(one_year_months)) {
            // i. Set months to months − oneYearMonths.
            months -= one_year_months;

            // ii. Set years to years + sign.
            years += sign;

            // iii. Set relativeTo to newRelativeTo.
            relative_to = new_relative_to;

            // iv. Set addOptions to ! OrdinaryObjectCreate(null).
            add_options = Object::create(global_object, nullptr);

            // v. Set newRelativeTo to ? CalendarDateAdd(calendar, relativeTo, oneYear, addOptions, dateAdd).
            new_relative_to = TRY(calendar_date_add(global_object, calendar, relative_to, *one_year, add_options, date_add));

            // vi. Set untilOptions to ! OrdinaryObjectCreate(null).
            until_options = Object::create(global_object, nullptr);

            // vii. Perform ! CreateDataPropertyOrThrow(untilOptions, "largestUnit", "month").
            MUST(until_options->create_data_property_or_throw(vm.names.largestUnit, js_string(vm, "month"sv)));

            // viii. Set untilResult to ? CalendarDateUntil(calendar, relativeTo, newRelativeTo, untilOptions, dateUntil).
            until_result = TRY(calendar_date_until(global_object, calendar, relative_to, new_relative_to, *until_options, date_until));

            // ix. Set oneYearMonths to untilResult.[[Months]].
            one_year_months = until_result->months();
        }
    }
    // 10. Else if largestUnit is "month", then
    else if (largest_unit == "month"sv) {
        // a. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneMonth).
        auto move_result = TRY(move_relative_date(global_object, calendar, *relative_to, *one_month));

        // b. Set relativeTo to moveResult.[[RelativeTo]].
        relative_to = move_result.relative_to.cell();

        // c. Let oneMonthDays be moveResult.[[Days]].
        auto one_month_days = move_result.days;

        // d. Repeat, while abs(days) ≥ abs(oneMonthDays),
        while (fabs(days) >= fabs(one_month_days)) {
            // i. Set days to days − oneMonthDays.
            days -= one_month_days;

            // ii. Set months to months + sign.
            months += sign;

            // iii. Set moveResult to ? MoveRelativeDate(calendar, relativeTo, oneMonth).
            move_result = TRY(move_relative_date(global_object, calendar, *relative_to, *one_month));

            // iv. Set relativeTo to moveResult.[[RelativeTo]].
            relative_to = move_result.relative_to.cell();

            // v. Set oneMonthDays to moveResult.[[Days]].
            one_month_days = move_result.days;
        }
    }
    // 11. Else,
    else {
        // a. Assert: largestUnit is "week".
        VERIFY(largest_unit == "week"sv);

        // b. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneWeek).
        auto move_result = TRY(move_relative_date(global_object, calendar, *relative_to, *one_week));

        // c. Set relativeTo to moveResult.[[RelativeTo]].
        relative_to = move_result.relative_to.cell();

        // d. Let oneWeekDays be moveResult.[[Days]].
        auto one_week_days = move_result.days;

        // e. Repeat, while abs(days) ≥ abs(oneWeekDays),
        while (fabs(days) >= fabs(one_week_days)) {
            // i. Set days to days − oneWeekDays.
            days -= one_week_days;

            // ii. Set weeks to weeks + sign.
            weeks += sign;

            // iii. Set moveResult to ? MoveRelativeDate(calendar, relativeTo, oneWeek).
            move_result = TRY(move_relative_date(global_object, calendar, *relative_to, *one_week));

            // iv. Set relativeTo to moveResult.[[RelativeTo]].
            relative_to = move_result.relative_to.cell();

            // v. Set oneWeekDays to moveResult.[[Days]].
            one_week_days = move_result.days;
        }
    }

    // 12. Return the Record { [[Years]]: years, [[Months]]: months, [[Weeks]]: weeks, [[Days]]: days }.
    return RelativeBalancedDuration { .years = years, .months = months, .weeks = weeks, .days = days };
}

// 7.5.14 AddDuration ( y1, mon1, w1, d1, h1, min1, s1, ms1, mus1, ns1, y2, mon2, w2, d2, h2, min2, s2, ms2, mus2, ns2, relativeTo ), https://tc39.es/proposal-temporal/#sec-temporal-addduration
ThrowCompletionOr<TemporalDuration> add_duration(GlobalObject& global_object, double years1, double months1, double weeks1, double days1, double hours1, double minutes1, double seconds1, double milliseconds1, double microseconds1, double nanoseconds1, double years2, double months2, double weeks2, double days2, double hours2, double minutes2, double seconds2, double milliseconds2, double microseconds2, double nanoseconds2, Value relative_to_value)
{
    auto& vm = global_object.vm();

    // FIXME: 1. Assert: y1, mon1, w1, d1, h1, min1, s1, ms1, mus1, ns1, y2, mon2, w2, d2, h2, min2, s2, ms2, mus2, ns2 are integer Number values.

    // 2. Let largestUnit1 be ! DefaultTemporalLargestUnit(y1, mon1, w1, d1, h1, min1, s1, ms1, mus1).
    auto largest_unit1 = default_temporal_largest_unit(years1, months1, weeks1, days1, hours1, minutes1, seconds1, milliseconds1, microseconds1);

    // 3. Let largestUnit2 be ! DefaultTemporalLargestUnit(y2, mon2, w2, d2, h2, min2, s2, ms2, mus2).
    auto largest_unit2 = default_temporal_largest_unit(years2, months2, weeks2, days2, hours2, minutes2, seconds2, milliseconds2, microseconds2);

    // 4. Let largestUnit be ! LargerOfTwoTemporalUnits(largestUnit1, largestUnit2).
    auto largest_unit = larger_of_two_temporal_units(largest_unit1, largest_unit2);

    double years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds;

    // 5. If relativeTo is undefined, then
    if (relative_to_value.is_undefined()) {
        // a. If largestUnit is one of "year", "month", or "week", then
        if (largest_unit.is_one_of("year"sv, "month"sv, "week"sv)) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalMissingStartingPoint, "year, month or week");
        }

        // b. Let result be ! BalanceDuration(d1 + d2, h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit).
        // FIXME: Narrowing conversion from 'double' to 'i64'
        auto* added_nanoseconds_bigint = js_bigint(vm, Crypto::SignedBigInteger::create_from(nanoseconds1 + nanoseconds2));
        auto result = MUST(balance_duration(global_object, days1 + days2, hours1 + hours2, minutes1 + minutes2, seconds1 + seconds2, milliseconds1 + milliseconds2, microseconds1 + microseconds2, *added_nanoseconds_bigint, largest_unit));

        // c. Let years be 0.
        years = 0;

        // d. Let months be 0.
        months = 0;

        // e. Let weeks be 0.
        weeks = 0;

        // f. Let days be result.[[Days]].
        days = result.days;

        // g. Let hours be result.[[Hours]].
        hours = result.hours;

        // h. Let minutes be result.[[Minutes]].
        minutes = result.minutes;

        // i. Let seconds be result.[[Seconds]].
        seconds = result.seconds;

        // j. Let milliseconds be result.[[Milliseconds]].
        milliseconds = result.milliseconds;

        // k. Let microseconds be result.[[Microseconds]].
        microseconds = result.microseconds;

        // l. Let nanoseconds be result.[[Nanoseconds]].
        nanoseconds = result.nanoseconds;
    }
    // 6. Else if relativeTo has an [[InitializedTemporalDate]] internal slot, then
    else if (is<PlainDate>(relative_to_value.as_object())) {
        auto& relative_to = static_cast<PlainDate&>(relative_to_value.as_object());

        // a. Let calendar be relativeTo.[[Calendar]].
        auto& calendar = relative_to.calendar();

        // b. Let dateDuration1 be ? CreateTemporalDuration(y1, mon1, w1, d1, 0, 0, 0, 0, 0, 0).
        auto* date_duration1 = TRY(create_temporal_duration(global_object, years1, months1, weeks1, days1, 0, 0, 0, 0, 0, 0));

        // c. Let dateDuration2 be ? CreateTemporalDuration(y2, mon2, w2, d2, 0, 0, 0, 0, 0, 0).
        auto* date_duration2 = TRY(create_temporal_duration(global_object, years2, months2, weeks2, days2, 0, 0, 0, 0, 0, 0));

        // d. Let dateAdd be ? GetMethod(calendar, "dateAdd").
        auto* date_add = TRY(Value(&calendar).get_method(global_object, vm.names.dateAdd));

        // e. Let firstAddOptions be ! OrdinaryObjectCreate(null).
        auto* first_add_options = Object::create(global_object, nullptr);

        // f. Let intermediate be ? CalendarDateAdd(calendar, relativeTo, dateDuration1, firstAddOptions, dateAdd).
        auto* intermediate = TRY(calendar_date_add(global_object, calendar, &relative_to, *date_duration1, first_add_options, date_add));

        // g. Let secondAddOptions be ! OrdinaryObjectCreate(null).
        auto* second_add_options = Object::create(global_object, nullptr);

        // h. Let end be ? CalendarDateAdd(calendar, intermediate, dateDuration2, secondAddOptions, dateAdd).
        auto* end = TRY(calendar_date_add(global_object, calendar, intermediate, *date_duration2, second_add_options, date_add));

        // i. Let dateLargestUnit be ! LargerOfTwoTemporalUnits("day", largestUnit).
        auto date_largest_unit = larger_of_two_temporal_units("day"sv, largest_unit);

        // j. Let differenceOptions be ! OrdinaryObjectCreate(null).
        auto* difference_options = Object::create(global_object, nullptr);

        // k. Perform ! CreateDataPropertyOrThrow(differenceOptions, "largestUnit", dateLargestUnit).
        MUST(difference_options->create_data_property_or_throw(vm.names.largestUnit, js_string(vm, date_largest_unit)));

        // l. Let dateDifference be ? CalendarDateUntil(calendar, relativeTo, end, differenceOptions).
        auto* date_difference = TRY(calendar_date_until(global_object, calendar, &relative_to, end, *difference_options));

        // m. Let result be ! BalanceDuration(dateDifference.[[Days]], h1 + h2, min1 + min2, s1 + s2, ms1 + ms2, mus1 + mus2, ns1 + ns2, largestUnit).
        // FIXME: Narrowing conversion from 'double' to 'i64'
        auto* added_nanoseconds_bigint = js_bigint(vm, Crypto::SignedBigInteger::create_from(nanoseconds1 + nanoseconds2));
        auto result = MUST(balance_duration(global_object, date_difference->days(), hours1 + hours2, minutes1 + minutes2, seconds1 + seconds2, milliseconds1 + milliseconds2, microseconds1 + microseconds2, *added_nanoseconds_bigint, largest_unit));

        // n. Let years be dateDifference.[[Years]].
        years = date_difference->years();

        // o. Let months be dateDifference.[[Months]].
        months = date_difference->months();

        // p. Let weeks be dateDifference.[[Weeks]].
        weeks = date_difference->weeks();

        // q. Let days be result.[[Days]].
        days = result.days;

        // r. Let hours be result.[[Hours]].
        hours = result.hours;

        // s. Let minutes be result.[[Minutes]].
        minutes = result.minutes;

        // t. Let seconds be result.[[Seconds]].
        seconds = result.seconds;

        // u. Let milliseconds be result.[[Milliseconds]].
        milliseconds = result.milliseconds;

        // v. Let microseconds be result.[[Microseconds]].
        microseconds = result.microseconds;

        // w. Let nanoseconds be result.[[Nanoseconds]].
        nanoseconds = result.nanoseconds;
    }
    // 7. Else,
    else {
        // a. Assert: relativeTo has an [[InitializedTemporalZonedDateTime]] internal slot.
        auto& relative_to = verify_cast<ZonedDateTime>(relative_to_value.as_object());

        // b. Let timeZone be relativeTo.[[TimeZone]].
        auto& time_zone = relative_to.time_zone();

        // c. Let calendar be relativeTo.[[Calendar]].
        auto& calendar = relative_to.calendar();

        // d. Let intermediateNs be ? AddZonedDateTime(relativeTo.[[Nanoseconds]], timeZone, calendar, y1, mon1, w1, d1, h1, min1, s1, ms1, mus1, ns1).
        auto* intermediate_ns = TRY(add_zoned_date_time(global_object, relative_to.nanoseconds(), &time_zone, calendar, years1, months1, weeks1, days1, hours1, minutes1, seconds1, milliseconds1, microseconds1, nanoseconds1));

        // e. Let endNs be ? AddZonedDateTime(intermediateNs, timeZone, calendar, y2, mon2, w2, d2, h2, min2, s2, ms2, mus2, ns2).
        auto* end_ns = TRY(add_zoned_date_time(global_object, *intermediate_ns, &time_zone, calendar, years2, months2, weeks2, days2, hours2, minutes2, seconds2, milliseconds2, microseconds2, nanoseconds2));

        // f. If largestUnit is not one of "year", "month", "week", or "day", then
        if (!largest_unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv)) {
            // i. Let diffNs be ! DifferenceInstant(relativeTo.[[Nanoseconds]], endNs, 1, "nanosecond", "halfExpand").
            auto* diff_ns = difference_instant(global_object, relative_to.nanoseconds(), *end_ns, 1, "nanosecond"sv, "halfExpand"sv);

            // ii. Let result be ! BalanceDuration(0, 0, 0, 0, 0, 0, diffNs, largestUnit).
            auto result = MUST(balance_duration(global_object, 0, 0, 0, 0, 0, 0, *diff_ns, largest_unit));

            // iii. Let years be 0.
            years = 0;

            // iv. Let months be 0.
            months = 0;

            // v. Let weeks be 0.
            weeks = 0;

            // vi. Let days be 0.
            days = 0;

            // vii. Let hours be result.[[Hours]].
            hours = result.hours;

            // viii. Let minutes be result.[[Minutes]].
            minutes = result.minutes;

            // ix. Let seconds be result.[[Seconds]].
            seconds = result.seconds;

            // x. Let milliseconds be result.[[Milliseconds]].
            milliseconds = result.milliseconds;

            // xi. Let microseconds be result.[[Microseconds]].
            microseconds = result.microseconds;

            // xii. Let nanoseconds be result.[[Nanoseconds]].
            nanoseconds = result.nanoseconds;
        }
        // g. Else,
        else {
            // i. Let result be ? DifferenceZonedDateTime(relativeTo.[[Nanoseconds]], endNs, timeZone, calendar, largestUnit).
            auto result = TRY(difference_zoned_date_time(global_object, relative_to.nanoseconds(), *end_ns, time_zone, calendar, largest_unit));

            // ii. Let years be result.[[Years]].
            years = result.years;

            // iii. Let months be result.[[Months]].
            months = result.months;

            // iv. Let weeks be result.[[Weeks]].
            weeks = result.weeks;

            // v. Let days be result.[[Days]].
            days = result.days;

            // vi. Let hours be result.[[Hours]].
            hours = result.hours;

            // vii. Let minutes be result.[[Minutes]].
            minutes = result.minutes;

            // viii. Let seconds be result.[[Seconds]].
            seconds = result.seconds;

            // ix. Let milliseconds be result.[[Milliseconds]].
            milliseconds = result.milliseconds;

            // x. Let microseconds be result.[[Microseconds]].
            microseconds = result.microseconds;

            // xi. Let nanoseconds be result.[[Nanoseconds]].
            nanoseconds = result.nanoseconds;
        }
    }

    // 8. If ! IsValidDuration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds) is false, throw a RangeError exception.
    if (!is_valid_duration(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDuration);

    // 9. Return the Record { [[Years]]: years, [[Months]]: months, [[Weeks]]: weeks, [[Days]]: days, [[Hours]]: hours, [[Minutes]]: minutes, [[Seconds]]: seconds, [[Milliseconds]]: milliseconds, [[Microseconds]]: microseconds, [[Nanoseconds]]: nanoseconds }.
    return TemporalDuration { .years = years, .months = months, .weeks = weeks, .days = days, .hours = hours, .minutes = minutes, .seconds = seconds, .milliseconds = milliseconds, .microseconds = microseconds, .nanoseconds = nanoseconds };
}

// 7.5.16 MoveRelativeDate ( calendar, relativeTo, duration ), https://tc39.es/proposal-temporal/#sec-temporal-moverelativedate
ThrowCompletionOr<MoveRelativeDateResult> move_relative_date(GlobalObject& global_object, Object& calendar, PlainDate& relative_to, Duration& duration)
{
    // 1. Assert: Type(relativeTo) is Object.
    // 2. Assert: relativeTo has an [[InitializedTemporalDate]] internal slot.

    // 3. Let options be ! OrdinaryObjectCreate(null).
    auto* options = Object::create(global_object, nullptr);

    // 4. Let newDate be ? CalendarDateAdd(calendar, relativeTo, duration, options).
    auto* new_date = TRY(calendar_date_add(global_object, calendar, &relative_to, duration, options));

    // 5. Let days be ! DaysUntil(relativeTo, newDate).
    auto days = days_until(global_object, relative_to, *new_date);

    // 6. Return the Record { [[RelativeTo]]: newDate, [[Days]]: days }.
    return MoveRelativeDateResult { .relative_to = make_handle(new_date), .days = days };
}

// 7.5.17 MoveRelativeZonedDateTime ( zonedDateTime, years, months, weeks, days ), https://tc39.es/proposal-temporal/#sec-temporal-moverelativezoneddatetime
ThrowCompletionOr<ZonedDateTime*> move_relative_zoned_date_time(GlobalObject& global_object, ZonedDateTime& zoned_date_time, double years, double months, double weeks, double days)
{
    // 1. Let intermediateNs be ? AddZonedDateTime(zonedDateTime.[[Nanoseconds]], zonedDateTime.[[TimeZone]], zonedDateTime.[[Calendar]], years, months, weeks, days, 0, 0, 0, 0, 0, 0).
    auto* intermediate_ns = TRY(add_zoned_date_time(global_object, zoned_date_time.nanoseconds(), &zoned_date_time.time_zone(), zoned_date_time.calendar(), years, months, weeks, days, 0, 0, 0, 0, 0, 0));

    // 2. Return ! CreateTemporalZonedDateTime(intermediateNs, zonedDateTime.[[TimeZone]], zonedDateTime.[[Calendar]]).
    return MUST(create_temporal_zoned_date_time(global_object, *intermediate_ns, zoned_date_time.time_zone(), zoned_date_time.calendar()));
}

// 7.5.18 RoundDuration ( years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, increment, unit, roundingMode [ , relativeTo ] ), https://tc39.es/proposal-temporal/#sec-temporal-roundduration
ThrowCompletionOr<RoundedDuration> round_duration(GlobalObject& global_object, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, u32 increment, StringView unit, StringView rounding_mode, Object* relative_to_object)
{
    auto& vm = global_object.vm();

    Object* calendar = nullptr;
    double fractional_seconds = 0;

    // 1. If relativeTo is not present, set relativeTo to undefined.
    // NOTE: `relative_to_object` and `relative_to` in the various code paths below are all the same as far as the
    // spec is concerned, but the latter is more strictly typed for convenience.
    PlainDate* relative_to = nullptr;

    // 2. Let years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, and increment each be the mathematical values of themselves.

    // FIXME: assuming "smallestUnit" as the option name here leads to confusing error messages in some cases:
    //        > new Temporal.Duration().total({ unit: "month" })
    //        Uncaught exception: [RangeError] month is not a valid value for option smallestUnit
    // 3. If unit is "year", "month", or "week", and relativeTo is undefined, then
    if (unit.is_one_of("year"sv, "month"sv, "week"sv) && !relative_to_object) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, unit, "smallestUnit"sv);
    }

    // 4. Let zonedRelativeTo be undefined.
    ZonedDateTime* zoned_relative_to = nullptr;

    // 5. If relativeTo is not undefined, then
    if (relative_to_object) {
        // a. If relativeTo has an [[InitializedTemporalZonedDateTime]] internal slot, then
        if (is<ZonedDateTime>(relative_to_object)) {
            auto* relative_to_zoned_date_time = static_cast<ZonedDateTime*>(relative_to_object);

            // i. Let instant be ! CreateTemporalInstant(relativeTo.[[Nanoseconds]]).
            auto* instant = MUST(create_temporal_instant(global_object, relative_to_zoned_date_time->nanoseconds()));

            // ii. Set zonedRelativeTo to relativeTo.
            zoned_relative_to = relative_to_zoned_date_time;

            // iii. Let plainDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(relativeTo.[[TimeZone]], instant, relativeTo.[[Calendar]]).
            auto* plain_date_time = TRY(builtin_time_zone_get_plain_date_time_for(global_object, &relative_to_zoned_date_time->time_zone(), *instant, relative_to_zoned_date_time->calendar()));

            // iv. Set relativeTo to ! CreateTemporalDate(plainDateTime.[[ISOYear]], plainDateTime.[[ISOMonth]], plainDateTime.[[ISODay]], relativeTo.[[Calendar]]).
            relative_to = TRY(create_temporal_date(global_object, plain_date_time->iso_year(), plain_date_time->iso_month(), plain_date_time->iso_day(), relative_to_zoned_date_time->calendar()));
        }
        // b. Else,
        else {
            //     i. Assert: relativeTo has an [[InitializedTemporalDate]] internal slot.
            VERIFY(is<PlainDate>(relative_to_object));

            relative_to = static_cast<PlainDate*>(relative_to_object);
        }

        // c. Let calendar be relativeTo.[[Calendar]].
        calendar = &relative_to->calendar();
    }

    // 6. If unit is one of "year", "month", "week", or "day", then
    if (unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv)) {
        auto* nanoseconds_bigint = js_bigint(vm, Crypto::SignedBigInteger::create_from((i64)nanoseconds));

        // a. Let nanoseconds be ! TotalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0).
        nanoseconds_bigint = total_duration_nanoseconds(global_object, 0, hours, minutes, seconds, milliseconds, microseconds, *nanoseconds_bigint, 0);

        // b. Let intermediate be undefined.
        ZonedDateTime* intermediate = nullptr;

        // c. If zonedRelativeTo is not undefined, then
        if (zoned_relative_to) {
            // i. Let intermediate be ? MoveRelativeZonedDateTime(zonedRelativeTo, years, months, weeks, days).
            intermediate = TRY(move_relative_zoned_date_time(global_object, *zoned_relative_to, years, months, weeks, days));
        }

        // d. Let result be ? NanosecondsToDays(nanoseconds, intermediate).
        auto result = TRY(nanoseconds_to_days(global_object, *nanoseconds_bigint, intermediate));

        // e. Set days to days + result.[[Days]] + result.[[Nanoseconds]] / result.[[DayLength]].
        auto nanoseconds_division_result = result.nanoseconds->big_integer().divided_by(Crypto::UnsignedBigInteger::create_from((u64)result.day_length));
        days += result.days + nanoseconds_division_result.quotient.to_double() + nanoseconds_division_result.remainder.to_double() / result.day_length;

        // f. Set hours, minutes, seconds, milliseconds, microseconds, and nanoseconds to 0.
        hours = 0;
        minutes = 0;
        seconds = 0;
        milliseconds = 0;
        microseconds = 0;
        nanoseconds = 0;
    }
    // 7. Else,
    else {
        // a. Let fractionalSeconds be nanoseconds × 10^−9 + microseconds × 10^−6 + milliseconds × 10^−3 + seconds.
        fractional_seconds = nanoseconds * 0.000000001 + microseconds * 0.000001 + milliseconds * 0.001 + seconds;
    }

    // 8. Let remainder be undefined.
    double remainder = 0;

    // 9. If unit is "year", then
    if (unit == "year"sv) {
        VERIFY(relative_to);

        // a. Let yearsDuration be ? CreateTemporalDuration(years, 0, 0, 0, 0, 0, 0, 0, 0, 0).
        auto* years_duration = TRY(create_temporal_duration(global_object, years, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        // b. Let dateAdd be ? GetMethod(calendar, "dateAdd").
        auto* date_add = TRY(Value(calendar).get_method(global_object, vm.names.dateAdd));

        // c. Let firstAddOptions be ! OrdinaryObjectCreate(null).
        auto* first_add_options = Object::create(global_object, nullptr);

        // d. Let yearsLater be ? CalendarDateAdd(calendar, relativeTo, yearsDuration, firstAddOptions, dateAdd).
        auto* years_later = TRY(calendar_date_add(global_object, *calendar, relative_to, *years_duration, first_add_options, date_add));

        // e. Let yearsMonthsWeeks be ? CreateTemporalDuration(years, months, weeks, 0, 0, 0, 0, 0, 0, 0).
        auto* years_months_weeks = TRY(create_temporal_duration(global_object, years, months, weeks, 0, 0, 0, 0, 0, 0, 0));

        // f. Let secondAddOptions be ! OrdinaryObjectCreate(null).
        auto* second_add_options = Object::create(global_object, nullptr);

        // g. Let yearsMonthsWeeksLater be ? CalendarDateAdd(calendar, relativeTo, yearsMonthsWeeks, secondAddOptions, dateAdd).
        auto* years_months_weeks_later = TRY(calendar_date_add(global_object, *calendar, relative_to, *years_months_weeks, second_add_options, date_add));

        // h. Let monthsWeeksInDays be ? DaysUntil(yearsLater, yearsMonthsWeeksLater).
        auto months_weeks_in_days = days_until(global_object, *years_later, *years_months_weeks_later);

        // i. Set relativeTo to yearsLater.
        relative_to = years_later;

        // j. Let days be days + monthsWeeksInDays.
        days += months_weeks_in_days;

        // k. Let daysDuration be ? CreateTemporalDuration(0, 0, 0, days, 0, 0, 0, 0, 0, 0).
        auto* days_duration = TRY(create_temporal_duration(global_object, 0, 0, 0, days, 0, 0, 0, 0, 0, 0));

        // l. Let thirdAddOptions be ! OrdinaryObjectCreate(null).
        auto* third_add_options = Object::create(global_object, nullptr);

        // m. Let daysLater be ? CalendarDateAdd(calendar, relativeTo, daysDuration, thirdAddOptions, dateAdd).
        auto* days_later = TRY(calendar_date_add(global_object, *calendar, relative_to, *days_duration, third_add_options, date_add));

        // n. Let untilOptions be ! OrdinaryObjectCreate(null).
        auto* until_options = Object::create(global_object, nullptr);

        // o. Perform ! CreateDataPropertyOrThrow(untilOptions, "largestUnit", "year").
        MUST(until_options->create_data_property_or_throw(vm.names.largestUnit, js_string(vm, "year"sv)));

        // p. Let timePassed be ? CalendarDateUntil(calendar, relativeTo, daysLater, untilOptions).
        auto* time_passed = TRY(calendar_date_until(global_object, *calendar, relative_to, days_later, *until_options));

        // q. Let yearsPassed be timePassed.[[Years]].
        auto years_passed = time_passed->years();

        // r. Set years to years + yearsPassed.
        years += years_passed;

        // s. Let oldRelativeTo be relativeTo.
        auto* old_relative_to = relative_to;

        // t. Let yearsDuration be ? CreateTemporalDuration(yearsPassed, 0, 0, 0, 0, 0, 0, 0, 0, 0).
        years_duration = TRY(create_temporal_duration(global_object, years_passed, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        // u. Let fourthAddOptions be ! OrdinaryObjectCreate(null).
        auto* fourth_add_options = Object::create(global_object, nullptr);

        // v. Set relativeTo to ? CalendarDateAdd(calendar, relativeTo, yearsDuration, fourthAddOptions, dateAdd).
        relative_to = TRY(calendar_date_add(global_object, *calendar, relative_to, *years_duration, fourth_add_options, date_add));

        // w. Let daysPassed be ? DaysUntil(oldRelativeTo, relativeTo).
        auto days_passed = days_until(global_object, *old_relative_to, *relative_to);

        // x. Set days to days - daysPassed.
        days -= days_passed;

        // y. Let sign be ! Sign(days).
        auto sign = JS::Temporal::sign(days);

        // z. If sign is 0, set sign to 1.
        if (sign == 0)
            sign = 1;

        // aa. Let oneYear be ? CreateTemporalDuration(sign, 0, 0, 0, 0, 0, 0, 0, 0, 0).
        auto* one_year = TRY(create_temporal_duration(global_object, sign, 0, 0, 0, 0, 0, 0, 0, 0, 0));

        // ab. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneYear).
        auto move_result = TRY(move_relative_date(global_object, *calendar, *relative_to, *one_year));

        // ac. Let oneYearDays be moveResult.[[Days]].
        auto one_year_days = move_result.days;

        // ad. Let fractionalYears be years + days / abs(oneYearDays).
        auto fractional_years = years + days / fabs(one_year_days);

        // ae. Set years to ! RoundNumberToIncrement(fractionalYears, increment, roundingMode).
        years = (double)round_number_to_increment(fractional_years, increment, rounding_mode);

        // af. Set remainder to fractionalYears - years.
        remainder = fractional_years - years;

        // ag. Set months, weeks, and days to 0.
        months = 0;
        weeks = 0;
        days = 0;
    }
    // 10. Else if unit is "month", then
    else if (unit == "month"sv) {
        VERIFY(relative_to);

        // a. Let yearsMonths be ? CreateTemporalDuration(years, months, 0, 0, 0, 0, 0, 0, 0, 0).
        auto* years_months = TRY(create_temporal_duration(global_object, years, months, 0, 0, 0, 0, 0, 0, 0, 0));

        // b. Let dateAdd be ? GetMethod(calendar, "dateAdd").
        auto* date_add = TRY(Value(calendar).get_method(global_object, vm.names.dateAdd));

        // c. Let firstAddOptions be ! OrdinaryObjectCreate(null).
        auto* first_add_options = Object::create(global_object, nullptr);

        // d. Let yearsMonthsLater be ? CalendarDateAdd(calendar, relativeTo, yearsMonths, firstAddOptions, dateAdd).
        auto* years_months_later = TRY(calendar_date_add(global_object, *calendar, relative_to, *years_months, first_add_options, date_add));

        // e. Let yearsMonthsWeeks be ? CreateTemporalDuration(years, months, weeks, 0, 0, 0, 0, 0, 0, 0).
        auto* years_months_weeks = TRY(create_temporal_duration(global_object, years, months, weeks, 0, 0, 0, 0, 0, 0, 0));

        // f. Let secondAddOptions be ! OrdinaryObjectCreate(null).
        auto* seconds_add_options = Object::create(global_object, nullptr);

        // g. Let yearsMonthsWeeksLater be ? CalendarDateAdd(calendar, relativeTo, yearsMonthsWeeks, secondAddOptions, dateAdd).
        auto* years_months_weeks_later = TRY(calendar_date_add(global_object, *calendar, relative_to, *years_months_weeks, seconds_add_options, date_add));

        // h. Let weeksInDays be ? DaysUntil(yearsMonthsLater, yearsMonthsWeeksLater).
        auto weeks_in_days = days_until(global_object, *years_months_later, *years_months_weeks_later);

        // i. Set relativeTo to yearsMonthsLater.
        relative_to = years_months_later;

        // j. Let days be days + weeksInDays.
        days += weeks_in_days;

        // k. Let sign be ! Sign(days).
        auto sign = JS::Temporal::sign(days);

        // l. If sign is 0, set sign to 1.
        if (sign == 0)
            sign = 1;

        // m. Let oneMonth be ? CreateTemporalDuration(0, sign, 0, 0, 0, 0, 0, 0, 0, 0).
        auto* one_month = TRY(create_temporal_duration(global_object, 0, sign, 0, 0, 0, 0, 0, 0, 0, 0));

        // n. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneMonth).
        auto move_result = TRY(move_relative_date(global_object, *calendar, *relative_to, *one_month));

        // o. Set relativeTo to moveResult.[[RelativeTo]].
        relative_to = move_result.relative_to.cell();

        // p. Let oneMonthDays be moveResult.[[Days]].
        auto one_month_days = move_result.days;

        // q. Repeat, while abs(days) ≥ abs(oneMonthDays),
        while (fabs(days) >= fabs(one_month_days)) {
            // i. Set months to months + sign.
            months += sign;

            // ii. Set days to days − oneMonthDays.
            days -= one_month_days;

            // iii. Set moveResult to ? MoveRelativeDate(calendar, relativeTo, oneMonth).
            move_result = TRY(move_relative_date(global_object, *calendar, *relative_to, *one_month));

            // iv. Set relativeTo to moveResult.[[RelativeTo]].
            relative_to = move_result.relative_to.cell();

            // v. Set oneMonthDays to moveResult.[[Days]].
            one_month_days = move_result.days;
        }

        // r. Let fractionalMonths be months + days / abs(oneMonthDays).
        auto fractional_months = months + days / fabs(one_month_days);

        // s. Set months to ! RoundNumberToIncrement(fractionalMonths, increment, roundingMode).
        months = (double)round_number_to_increment(fractional_months, increment, rounding_mode);

        // t. Set remainder to fractionalMonths - months.
        remainder = fractional_months - months;

        // u. Set weeks and days to 0.
        weeks = 0;
        days = 0;
    }
    // 11. Else if unit is "week", then
    else if (unit == "week"sv) {
        VERIFY(relative_to);

        // a. Let sign be ! Sign(days).
        auto sign = JS::Temporal::sign(days);

        // b. If sign is 0, set sign to 1.
        if (sign == 0)
            sign = 1;

        // c. Let oneWeek be ? CreateTemporalDuration(0, 0, sign, 0, 0, 0, 0, 0, 0, 0).
        auto* one_week = TRY(create_temporal_duration(global_object, 0, 0, sign, 0, 0, 0, 0, 0, 0, 0));

        // d. Let moveResult be ? MoveRelativeDate(calendar, relativeTo, oneWeek).
        auto move_result = TRY(move_relative_date(global_object, *calendar, *relative_to, *one_week));

        // e. Set relativeTo to moveResult.[[RelativeTo]].
        relative_to = move_result.relative_to.cell();

        // f. Let oneWeekDays be moveResult.[[Days]].
        auto one_week_days = move_result.days;

        // g. Repeat, while abs(days) ≥ abs(oneWeekDays),
        while (fabs(days) >= fabs(one_week_days)) {
            // i. Set weeks to weeks + sign.
            weeks += sign;

            // ii. Set days to days − oneWeekDays.
            days -= one_week_days;

            // iii. Set moveResult to ? MoveRelativeDate(calendar, relativeTo, oneWeek).
            move_result = TRY(move_relative_date(global_object, *calendar, *relative_to, *one_week));

            // iv. Set relativeTo to moveResult.[[RelativeTo]].
            relative_to = move_result.relative_to.cell();

            // v. Set oneWeekDays to moveResult.[[Days]].
            one_week_days = move_result.days;
        }

        // h. Let fractionalWeeks be weeks + days / abs(oneWeekDays).
        auto fractional_weeks = weeks + days / fabs(one_week_days);

        // i. Set weeks to ! RoundNumberToIncrement(fractionalWeeks, increment, roundingMode).
        weeks = (double)round_number_to_increment(fractional_weeks, increment, rounding_mode);

        // j. Set remainder to fractionalWeeks - weeks.
        remainder = fractional_weeks - weeks;

        // k. Set days to 0.
        days = 0;
    }
    // 12. Else if unit is "day", then
    else if (unit == "day"sv) {
        // a. Let fractionalDays be days.
        auto fractional_days = days;

        // b. Set days to ! RoundNumberToIncrement(days, increment, roundingMode).
        days = (double)round_number_to_increment(days, increment, rounding_mode);

        // c. Set remainder to fractionalDays - days.
        remainder = fractional_days - days;
    }
    // 13. Else if unit is "hour", then
    else if (unit == "hour"sv) {
        // a. Let fractionalHours be (fractionalSeconds / 60 + minutes) / 60 + hours.
        auto fractional_hours = (fractional_seconds / 60 + minutes) / 60 + hours;

        // b. Set hours to ! RoundNumberToIncrement(fractionalHours, increment, roundingMode).
        hours = (double)round_number_to_increment(fractional_hours, increment, rounding_mode);

        // c. Set remainder to fractionalHours - hours.
        remainder = fractional_hours - hours;

        // d. Set minutes, seconds, milliseconds, microseconds, and nanoseconds to 0.
        minutes = 0;
        seconds = 0;
        milliseconds = 0;
        microseconds = 0;
        nanoseconds = 0;
    }
    // 14. Else if unit is "minute", then
    else if (unit == "minute"sv) {
        // a. Let fractionalMinutes be fractionalSeconds / 60 + minutes.
        auto fractional_minutes = fractional_seconds / 60 + minutes;

        // b. Set minutes to ! RoundNumberToIncrement(fractionalMinutes, increment, roundingMode).
        minutes = (double)round_number_to_increment(fractional_minutes, increment, rounding_mode);

        // c. Set remainder to fractionalMinutes - minutes.
        remainder = fractional_minutes - minutes;

        // d. Set seconds, milliseconds, microseconds, and nanoseconds to 0.
        seconds = 0;
        milliseconds = 0;
        microseconds = 0;
        nanoseconds = 0;
    }
    // 15. Else if unit is "second", then
    else if (unit == "second"sv) {
        // a. Set seconds to ! RoundNumberToIncrement(fractionalSeconds, increment, roundingMode).
        seconds = (double)round_number_to_increment(fractional_seconds, increment, rounding_mode);

        // b. Set remainder to fractionalSeconds - seconds.
        remainder = fractional_seconds - seconds;

        // c. Set milliseconds, microseconds, and nanoseconds to 0.
        milliseconds = 0;
        microseconds = 0;
        nanoseconds = 0;
    }
    // 16. Else if unit is "millisecond", then
    else if (unit == "millisecond"sv) {
        // a. Let fractionalMilliseconds be nanoseconds × 10^−6 + microseconds × 10^−3 + milliseconds.
        auto fractional_milliseconds = nanoseconds * 0.000001 + microseconds * 0.001 + milliseconds;

        // b. Set milliseconds to ! RoundNumberToIncrement(fractionalMilliseconds, increment, roundingMode).
        milliseconds = (double)round_number_to_increment(fractional_milliseconds, increment, rounding_mode);

        // c. Set remainder to fractionalMilliseconds - milliseconds.
        remainder = fractional_milliseconds - milliseconds;

        // d. Set microseconds and nanoseconds to 0.
        microseconds = 0;
        nanoseconds = 0;
    }
    // 17. Else if unit is "microsecond", then
    else if (unit == "microsecond"sv) {
        // a. Let fractionalMicroseconds be nanoseconds × 10^−3 + microseconds.
        auto fractional_microseconds = nanoseconds * 0.001 + microseconds;

        // b. Set microseconds to ! RoundNumberToIncrement(fractionalMicroseconds, increment, roundingMode).
        microseconds = (double)round_number_to_increment(fractional_microseconds, increment, rounding_mode);

        // c. Set remainder to fractionalMicroseconds - microseconds.
        remainder = fractional_microseconds - microseconds;

        // d. Set nanoseconds to 0.
        nanoseconds = 0;
    }
    // 18. Else,
    else {
        // a. Assert: unit is "nanosecond".
        VERIFY(unit == "nanosecond"sv);

        // b. Set remainder to nanoseconds.
        remainder = nanoseconds;

        // c. Set nanoseconds to ! RoundNumberToIncrement(nanoseconds, increment, roundingMode).
        nanoseconds = (double)round_number_to_increment(nanoseconds, increment, rounding_mode);

        // d. Set remainder to remainder − nanoseconds.
        remainder -= nanoseconds;
    }

    // Return the Record { [[Years]]: years, [[Months]]: months, [[Weeks]]: weeks, [[Days]]: days, [[Hours]]: hours, [[Minutes]]: minutes, [[Seconds]]: seconds, [[Milliseconds]]: milliseconds, [[Microseconds]]: microseconds, [[Nanoseconds]]: nanoseconds, [[Remainder]]: remainder }.
    return RoundedDuration { .years = years, .months = months, .weeks = weeks, .days = days, .hours = hours, .minutes = minutes, .seconds = seconds, .milliseconds = milliseconds, .microseconds = microseconds, .nanoseconds = nanoseconds, .remainder = remainder };
}

// 7.5.19 AdjustRoundedDurationDays ( years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, increment, unit, roundingMode [ , relativeTo ] ), https://tc39.es/proposal-temporal/#sec-temporal-adjustroundeddurationdays
ThrowCompletionOr<TemporalDuration> adjust_rounded_duration_days(GlobalObject& global_object, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, u32 increment, StringView unit, StringView rounding_mode, Object* relative_to_object)
{
    auto& vm = global_object.vm();

    // 1. If relativeTo is not present; or Type(relativeTo) is not Object; or relativeTo does not have an [[InitializedTemporalZonedDateTime]] internal slot; or unit is one of "year", "month", "week", or "day"; or unit is "nanosecond" and increment is 1, then
    if (relative_to_object == nullptr || !is<ZonedDateTime>(relative_to_object) || unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv) || (unit == "nanosecond"sv && increment == 1)) {
        // a. Return the Record { [[Years]]: years, [[Months]]: months, [[Weeks]]: weeks, [[Days]]: days, [[Hours]]: hours, [[Minutes]]: minutes, [[Seconds]]: seconds, [[Milliseconds]]: milliseconds, [[Microseconds]]: microseconds, [[Nanoseconds]]: nanoseconds }.
        return TemporalDuration { .years = years, .months = months, .weeks = weeks, .days = days, .hours = hours, .minutes = minutes, .seconds = seconds, .milliseconds = milliseconds, .microseconds = microseconds, .nanoseconds = nanoseconds };
    }

    auto& relative_to = static_cast<ZonedDateTime&>(*relative_to_object);

    // 2. Let timeRemainderNs be ! TotalDurationNanoseconds(0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, 0).
    auto time_remainder_ns = total_duration_nanoseconds(global_object, 0, hours, minutes, seconds, milliseconds, microseconds, *js_bigint(vm, Crypto::SignedBigInteger::create_from((i64)nanoseconds)), 0)->big_integer();

    // 3. Let direction be ! ℝ(Sign(𝔽(timeRemainderNs))).
    auto direction = Temporal::sign(time_remainder_ns);

    // 4. Let dayStart be ? AddZonedDateTime(relativeTo.[[Nanoseconds]], relativeTo.[[TimeZone]], relativeTo.[[Calendar]], years, months, weeks, days, 0, 0, 0, 0, 0, 0).
    auto* day_start = TRY(add_zoned_date_time(global_object, relative_to.nanoseconds(), &relative_to.time_zone(), relative_to.calendar(), years, months, weeks, days, 0, 0, 0, 0, 0, 0));

    // 5. Let dayEnd be ? AddZonedDateTime(dayStart, relativeTo.[[TimeZone]], relativeTo.[[Calendar]], 0, 0, 0, direction, 0, 0, 0, 0, 0, 0).
    auto* day_end = TRY(add_zoned_date_time(global_object, *day_start, &relative_to.time_zone(), relative_to.calendar(), 0, 0, 0, direction, 0, 0, 0, 0, 0, 0));

    // 6. Let dayLengthNs be ℝ(dayEnd − dayStart).
    auto day_length_ns = day_end->big_integer().minus(day_start->big_integer());

    // 7. If (timeRemainderNs − dayLengthNs) × direction < 0, then
    if (time_remainder_ns.minus(day_length_ns).multiplied_by(Crypto::SignedBigInteger { (i32)direction }).is_negative()) {
        // a. Return the Record { [[Years]]: years, [[Months]]: months, [[Weeks]]: weeks, [[Days]]: days, [[Hours]]: hours, [[Minutes]]: minutes, [[Seconds]]: seconds, [[Milliseconds]]: milliseconds, [[Microseconds]]: microseconds, [[Nanoseconds]]: nanoseconds }.
        return TemporalDuration { .years = years, .months = months, .weeks = weeks, .days = days, .hours = hours, .minutes = minutes, .seconds = seconds, .milliseconds = milliseconds, .microseconds = microseconds, .nanoseconds = nanoseconds };
    }

    // 8. Set timeRemainderNs to ! RoundTemporalInstant(ℤ(timeRemainderNs − dayLengthNs), increment, unit, roundingMode).
    time_remainder_ns = round_temporal_instant(global_object, *js_bigint(vm, time_remainder_ns.minus(day_length_ns)), increment, unit, rounding_mode)->big_integer();

    // 9. Let adjustedDateDuration be ? AddDuration(years, months, weeks, days, 0, 0, 0, 0, 0, 0, 0, 0, 0, direction, 0, 0, 0, 0, 0, 0, relativeTo).
    auto adjusted_date_duration = TRY(add_duration(global_object, years, months, weeks, days, 0, 0, 0, 0, 0, 0, 0, 0, 0, direction, 0, 0, 0, 0, 0, 0, &relative_to));

    // 10. Let adjustedTimeDuration be ? BalanceDuration(0, 0, 0, 0, 0, 0, timeRemainderNs, "hour").
    auto adjusted_time_duration = TRY(balance_duration(global_object, 0, 0, 0, 0, 0, 0, *js_bigint(vm, move(time_remainder_ns)), "hour"sv));

    // 11. Return the Record { [[Years]]: adjustedDateDuration.[[Years]], [[Months]]: adjustedDateDuration.[[Months]], [[Weeks]]: adjustedDateDuration.[[Weeks]], [[Days]]: adjustedDateDuration.[[Days]], [[Hours]]: adjustedTimeDuration.[[Hours]], [[Minutes]]: adjustedTimeDuration.[[Minutes]], [[Seconds]]: adjustedTimeDuration.[[Seconds]], [[Milliseconds]]: adjustedTimeDuration.[[Milliseconds]], [[Microseconds]]: adjustedTimeDuration.[[Microseconds]], [[Nanoseconds]]: adjustedTimeDuration.[[Nanoseconds]] }.
    return TemporalDuration { .years = adjusted_date_duration.years, .months = adjusted_date_duration.months, .weeks = adjusted_date_duration.weeks, .days = adjusted_date_duration.days, .hours = adjusted_time_duration.hours, .minutes = adjusted_time_duration.minutes, .seconds = adjusted_time_duration.seconds, .milliseconds = adjusted_time_duration.milliseconds, .microseconds = adjusted_time_duration.microseconds, .nanoseconds = adjusted_time_duration.nanoseconds };
}

// 7.5.20 ToLimitedTemporalDuration ( temporalDurationLike, disallowedFields ), https://tc39.es/proposal-temporal/#sec-temporal-tolimitedtemporalduration
ThrowCompletionOr<TemporalDuration> to_limited_temporal_duration(GlobalObject& global_object, Value temporal_duration_like, Vector<StringView> const& disallowed_fields)
{
    auto& vm = global_object.vm();

    TemporalDuration duration;

    // 1. If Type(temporalDurationLike) is not Object, then
    if (!temporal_duration_like.is_object()) {
        // a. Let str be ? ToString(temporalDurationLike).
        auto str = TRY(temporal_duration_like.to_string(global_object));

        // b. Let duration be ? ParseTemporalDurationString(str).
        duration = TRY(parse_temporal_duration_string(global_object, str));
    }
    // 2. Else,
    else {
        // a. Let duration be ? ToTemporalDurationRecord(temporalDurationLike).
        duration = TRY(to_temporal_duration_record(global_object, temporal_duration_like.as_object()));
    }

    // 3. If ! IsValidDuration(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]]) is false, throw a RangeError exception.
    if (!is_valid_duration(duration.years, duration.months, duration.weeks, duration.days, duration.hours, duration.minutes, duration.seconds, duration.milliseconds, duration.microseconds, duration.nanoseconds))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDuration);

    // 4. For each row of Table 7, except the header row, in table order, do
    for (auto& [internal_slot, property] : temporal_duration_like_properties<TemporalDuration, double>(vm)) {
        // a. Let prop be the Property value of the current row.

        // b. Let value be duration's internal slot whose name is the Internal Slot value of the current row.
        auto value = duration.*internal_slot;

        // If value is not 0 and disallowedFields contains prop, then
        if (value != 0 && disallowed_fields.contains_slow(property.as_string())) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidDurationPropertyValueNonZero, property.as_string(), value);
        }
    }

    // 5. Return duration.
    return duration;
}

// 7.5.21 TemporalDurationToString ( years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, precision ), https://tc39.es/proposal-temporal/#sec-temporal-temporaldurationtostring
String temporal_duration_to_string(double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, Variant<StringView, u8> const& precision)
{
    // 1. Assert: precision is not "minute".
    if (precision.has<StringView>())
        VERIFY(precision.get<StringView>() != "minute"sv);

    // 2. Set seconds to the mathematical value of seconds.
    // 3. Set milliseconds to the mathematical value of milliseconds.
    // 4. Set microseconds to the mathematical value of microseconds.
    // 5. Set nanoseconds to the mathematical value of nanoseconds.

    // 6. Let sign be ! DurationSign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
    auto sign = duration_sign(years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);

    // 7. Set microseconds to microseconds + the integral part of nanoseconds / 1000.
    microseconds += trunc(nanoseconds / 1000);

    // 8. Set nanoseconds to remainder(nanoseconds, 1000).
    nanoseconds = fmod(nanoseconds, 1000);

    // 9. Set milliseconds to milliseconds + the integral part of microseconds / 1000.
    milliseconds += trunc(microseconds / 1000);

    // 10. Set microseconds to remainder(microseconds, 1000).
    microseconds = fmod(microseconds, 1000);

    // 11. Set seconds to seconds + the integral part of milliseconds / 1000.
    seconds += trunc(milliseconds / 1000);

    // 12. Set milliseconds to remainder(milliseconds, 1000).
    milliseconds = fmod(milliseconds, 1000);

    // 13. Let datePart be "".
    StringBuilder date_part;

    // 14. If years is not 0, then
    if (years != 0) {
        // a. Set datePart to the string concatenation of abs(years) formatted as a decimal number and the code unit 0x0059 (LATIN CAPITAL LETTER Y).
        date_part.appendff("{}", fabs(years));
        date_part.append('Y');
    }

    // 15. If months is not 0, then
    if (months != 0) {
        // a. Set datePart to the string concatenation of datePart, abs(months) formatted as a decimal number, and the code unit 0x004D (LATIN CAPITAL LETTER M).
        date_part.appendff("{}", fabs(months));
        date_part.append('M');
    }

    // 16. If weeks is not 0, then
    if (weeks != 0) {
        // a. Set datePart to the string concatenation of datePart, abs(weeks) formatted as a decimal number, and the code unit 0x0057 (LATIN CAPITAL LETTER W).
        date_part.appendff("{}", fabs(weeks));
        date_part.append('W');
    }

    // 17. If days is not 0, then
    if (days != 0) {
        // a. Set datePart to the string concatenation of datePart, abs(days) formatted as a decimal number, and the code unit 0x0044 (LATIN CAPITAL LETTER D).
        date_part.appendff("{}", fabs(days));
        date_part.append('D');
    }

    // 18. Let timePart be "".
    StringBuilder time_part;

    // 19. If hours is not 0, then
    if (hours != 0) {
        // a. Set timePart to the string concatenation of abs(hours) formatted as a decimal number and the code unit 0x0048 (LATIN CAPITAL LETTER H).
        time_part.appendff("{}", fabs(hours));
        time_part.append('H');
    }

    // 20. If minutes is not 0, then
    if (minutes != 0) {
        // a. Set timePart to the string concatenation of timePart, abs(minutes) formatted as a decimal number, and the code unit 0x004D (LATIN CAPITAL LETTER M).
        time_part.appendff("{}", fabs(minutes));
        time_part.append('M');
    }

    // 21. If any of seconds, milliseconds, microseconds, and nanoseconds are not 0; or years, months, weeks, days, hours, and minutes are all 0, then
    if ((seconds != 0 || milliseconds != 0 || microseconds != 0 || nanoseconds != 0) || (years == 0 && months == 0 && weeks == 0 && days == 0 && hours == 0 && minutes == 0)) {
        // a. Let fraction be abs(milliseconds) × 10^6 + abs(microseconds) × 10^3 + abs(nanoseconds).
        auto fraction = fabs(milliseconds) * 1'000'000 + fabs(microseconds) * 1'000 + fabs(nanoseconds);

        // b. Let decimalPart be fraction formatted as a nine-digit decimal number, padded to the left with zeroes if necessary.
        // NOTE: padding with zeros leads to weird results when applied to a double. Not sure if that's a bug in AK/Format.h or if I'm doing this wrong.
        auto decimal_part = String::formatted("{:09}", (u64)fraction);

        // c. If precision is "auto", then
        if (precision.has<StringView>() && precision.get<StringView>() == "auto"sv) {
            // i. Set decimalPart to the longest possible substring of decimalPart starting at position 0 and not ending with the code unit 0x0030 (DIGIT ZERO).
            // NOTE: trim() would keep the left-most 0.
            while (decimal_part.ends_with('0'))
                decimal_part = decimal_part.substring(0, decimal_part.length() - 1);
        }
        // d. Else if precision = 0, then
        else if (precision.get<u8>() == 0) {
            // i. Set decimalPart to "".
            decimal_part = String::empty();
        }
        // e. Else,
        else {
            // i. Set decimalPart to the substring of decimalPart from 0 to precision.
            decimal_part = decimal_part.substring(0, precision.get<u8>());
        }

        // f. Let secondsPart be abs(seconds) formatted as a decimal number.
        StringBuilder seconds_part;
        seconds_part.appendff("{}", fabs(seconds));

        // g. If decimalPart is not "", then
        if (!decimal_part.is_empty()) {
            // i. Set secondsPart to the string-concatenation of secondsPart, the code unit 0x002E (FULL STOP), and decimalPart.
            seconds_part.append('.');
            seconds_part.append(decimal_part);
        }

        // h. Set timePart to the string concatenation of timePart, secondsPart, and the code unit 0x0053 (LATIN CAPITAL LETTER S).
        time_part.append(seconds_part.string_view());
        time_part.append('S');
    }

    // 22. Let signPart be the code unit 0x002D (HYPHEN-MINUS) if sign < 0, and otherwise the empty String.
    auto sign_part = sign < 0 ? "-"sv : ""sv;

    // 23. Let result be the string concatenation of signPart, the code unit 0x0050 (LATIN CAPITAL LETTER P) and datePart.
    StringBuilder result;
    result.append(sign_part);
    result.append('P');
    result.append(date_part.string_view());

    // 24. If timePart is not "", then
    if (!time_part.is_empty()) {
        // a. Set result to the string concatenation of result, the code unit 0x0054 (LATIN CAPITAL LETTER T), and timePart.
        result.append('T');
        result.append(time_part.string_view());
    }

    // 25. Return result.
    return result.to_string();
}

}
