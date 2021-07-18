/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/DurationConstructor.h>

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

// 7.5.3 DurationSign ( years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds )
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

// 7.5.4 IsValidDuration ( years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds )
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

    // 2. Let result be the new Record { [[Years]]: undefined, [[Months]]: undefined, [[Weeks]]: undefined, [[Days]]: undefined, [[Hours]]: undefined, [[Minutes]]: undefined, [[Seconds]]: undefined, [[Milliseconds]]: undefined, [[Microseconds]]: undefined, [[Nanoseconds]]: undefined }.
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

            // ii. Set value to ? ToIntegerOrInfinity(value).
            auto value_number = value.to_integer_or_infinity(global_object);
            if (vm.exception())
                return {};

            // iii. Set result's internal slot whose name is the Internal Slot value of the current row to value.
            result.*internal_slot = value_number;
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
Duration* create_temporal_duration(GlobalObject& global_object, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, FunctionObject* new_target)
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

}
