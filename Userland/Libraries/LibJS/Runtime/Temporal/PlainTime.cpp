/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/PlainTimeConstructor.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>

namespace JS::Temporal {

// 4 Temporal.PlainTime Objects, https://tc39.es/proposal-temporal/#sec-temporal-plaintime-objects
PlainTime::PlainTime(u8 iso_hour, u8 iso_minute, u8 iso_second, u16 iso_millisecond, u16 iso_microsecond, u16 iso_nanosecond, Calendar& calendar, Object& prototype)
    : Object(prototype)
    , m_iso_hour(iso_hour)
    , m_iso_minute(iso_minute)
    , m_iso_second(iso_second)
    , m_iso_millisecond(iso_millisecond)
    , m_iso_microsecond(iso_microsecond)
    , m_iso_nanosecond(iso_nanosecond)
    , m_calendar(calendar)
{
}

void PlainTime::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_calendar);
}

// 4.5.1 DifferenceTime ( h1, min1, s1, ms1, mus1, ns1, h2, min2, s2, ms2, mus2, ns2 ), https://tc39.es/proposal-temporal/#sec-temporal-differencetime
BalancedDuration difference_time(u8 hour1, u8 minute1, u8 second1, u16 millisecond1, u16 microsecond1, u16 nanosecond1, u8 hour2, u8 minute2, u8 second2, u16 millisecond2, u16 microsecond2, u16 nanosecond2)
{
    // Assert: h1, min1, s1, ms1, mus1, ns1, h2, min2, s2, ms2, mus2, and ns2 are integers.

    // 2. Let hours be h2 − h1.
    auto hours = hour2 - hour1;

    // 3. Let minutes be min2 − min1.
    auto minutes = minute2 - minute1;

    // 4. Let seconds be s2 − s1.
    auto seconds = second2 - second1;

    // 5. Let milliseconds be ms2 − ms1.
    auto milliseconds = millisecond2 - millisecond1;

    // 6. Let microseconds be mus2 − mus1.
    auto microseconds = microsecond2 - microsecond1;

    // 7. Let nanoseconds be ns2 − ns1.
    auto nanoseconds = nanosecond2 - nanosecond1;

    // 8. Let sign be ! DurationSign(0, 0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
    auto sign = duration_sign(0, 0, 0, 0, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);

    // 9. Let bt be ! BalanceTime(hours × sign, minutes × sign, seconds × sign, milliseconds × sign, microseconds × sign, nanoseconds × sign).
    auto bt = balance_time(hours * sign, minutes * sign, seconds * sign, milliseconds * sign, microseconds * sign, nanoseconds * sign);

    // 10. Return the Record { [[Days]]: bt.[[Days]] × sign, [[Hours]]: bt.[[Hour]] × sign, [[Minutes]]: bt.[[Minute]] × sign, [[Seconds]]: bt.[[Second]] × sign, [[Milliseconds]]: bt.[[Millisecond]] × sign, [[Microseconds]]: bt.[[Microsecond]] × sign, [[Nanoseconds]]: bt.[[Nanosecond]] × sign }.
    return BalancedDuration { .days = static_cast<double>(bt.days * sign), .hours = static_cast<double>(bt.hour * sign), .minutes = static_cast<double>(bt.minute * sign), .seconds = static_cast<double>(bt.second * sign), .milliseconds = static_cast<double>(bt.millisecond * sign), .microseconds = static_cast<double>(bt.microsecond * sign), .nanoseconds = static_cast<double>(bt.nanosecond * sign) };
}

// 4.5.2 ToTemporalTime ( item [ , overflow ] ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaltime
ThrowCompletionOr<PlainTime*> to_temporal_time(GlobalObject& global_object, Value item, Optional<StringView> overflow)
{
    auto& vm = global_object.vm();

    // 1. If overflow is not present, set it to "constrain".
    if (!overflow.has_value())
        overflow = "constrain"sv;

    // 2. Assert: overflow is either "constrain" or "reject".
    VERIFY(overflow == "constrain"sv || overflow == "reject"sv);

    Optional<TemporalTime> result;

    // 3. If Type(item) is Object, then
    if (item.is_object()) {
        auto& item_object = item.as_object();

        // a. If item has an [[InitializedTemporalTime]] internal slot, then
        if (is<PlainTime>(item_object)) {
            // i. Return item.
            return &static_cast<PlainTime&>(item_object);
        }

        // b. If item has an [[InitializedTemporalZonedDateTime]] internal slot, then
        if (is<ZonedDateTime>(item_object)) {
            auto& zoned_date_time = static_cast<ZonedDateTime&>(item_object);
            // i. Let instant be ! CreateTemporalInstant(item.[[Nanoseconds]]).
            auto* instant = create_temporal_instant(global_object, zoned_date_time.nanoseconds()).release_value();
            // ii. Set plainDateTime to ? BuiltinTimeZoneGetPlainDateTimeFor(item.[[TimeZone]], instant, item.[[Calendar]]).
            auto* plain_date_time = TRY(builtin_time_zone_get_plain_date_time_for(global_object, &zoned_date_time.time_zone(), *instant, zoned_date_time.calendar()));
            // iii. Return ! CreateTemporalTime(plainDateTime.[[ISOHour]], plainDateTime.[[ISOMinute]], plainDateTime.[[ISOSecond]], plainDateTime.[[ISOMillisecond]], plainDateTime.[[ISOMicrosecond]], plainDateTime.[[ISONanosecond]]).
            return TRY(create_temporal_time(global_object, plain_date_time->iso_hour(), plain_date_time->iso_minute(), plain_date_time->iso_second(), plain_date_time->iso_millisecond(), plain_date_time->iso_microsecond(), plain_date_time->iso_nanosecond()));
        }

        // c. If item has an [[InitializedTemporalDateTime]] internal slot, then
        if (is<PlainDateTime>(item_object)) {
            auto& plain_date_time = static_cast<PlainDateTime&>(item_object);
            // i. Return ! CreateTemporalTime(item.[[ISOHour]], item.[[ISOMinute]], item.[[ISOSecond]], item.[[ISOMillisecond]], item.[[ISOMicrosecond]], item.[[ISONanosecond]]).
            return TRY(create_temporal_time(global_object, plain_date_time.iso_hour(), plain_date_time.iso_minute(), plain_date_time.iso_second(), plain_date_time.iso_millisecond(), plain_date_time.iso_microsecond(), plain_date_time.iso_nanosecond()));
        }

        // d. Let calendar be ? GetTemporalCalendarWithISODefault(item).
        auto* calendar = TRY(get_temporal_calendar_with_iso_default(global_object, item_object));

        // e. If ? ToString(calendar) is not "iso8601", then
        auto calendar_identifier = TRY(Value(calendar).to_string(global_object));
        if (calendar_identifier != "iso8601"sv) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarIdentifier, calendar_identifier);
        }

        // f. Let result be ? ToTemporalTimeRecord(item).
        auto unregulated_result = TRY(to_temporal_time_record(global_object, item_object));

        // g. Set result to ? RegulateTime(result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]], overflow).
        result = TRY(regulate_time(global_object, unregulated_result.hour, unregulated_result.minute, unregulated_result.second, unregulated_result.millisecond, unregulated_result.microsecond, unregulated_result.nanosecond, *overflow));
    }
    // 4. Else,
    else {
        // a. Let string be ? ToString(item).
        auto string = TRY(item.to_string(global_object));

        // b. Let result be ? ParseTemporalTimeString(string).
        result = TRY(parse_temporal_time_string(global_object, string));

        // c. Assert: ! IsValidTime(result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]]) is true.
        VERIFY(is_valid_time(result->hour, result->minute, result->second, result->millisecond, result->microsecond, result->nanosecond));

        // d. If result.[[Calendar]] is not one of undefined or "iso8601", then
        if (result->calendar.has_value() && *result->calendar != "iso8601"sv) {
            // i. Throw a RangeError exception.
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarIdentifier, *result->calendar);
        }
    }

    // 5. Return ? CreateTemporalTime(result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]]).
    return TRY(create_temporal_time(global_object, result->hour, result->minute, result->second, result->millisecond, result->microsecond, result->nanosecond));
}

// 4.5.3 ToPartialTime ( temporalTimeLike ), https://tc39.es/proposal-temporal/#sec-temporal-topartialtime
ThrowCompletionOr<PartialUnregulatedTemporalTime> to_partial_time(GlobalObject& global_object, Object& temporal_time_like)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(temporalTimeLike) is Object.

    // 2. Let result be the Record { [[Hour]]: undefined, [[Minute]]: undefined, [[Second]]: undefined, [[Millisecond]]: undefined, [[Microsecond]]: undefined, [[Nanosecond]]: undefined }.
    auto result = PartialUnregulatedTemporalTime {};

    // 3. Let any be false.
    bool any = false;

    // 4. For each row of Table 3, except the header row, in table order, do
    for (auto& [internal_slot, property] : temporal_time_like_properties<PartialUnregulatedTemporalTime, Optional<double>>(vm)) {
        // a. Let property be the Property value of the current row.

        // b. Let value be ? Get(temporalTimeLike, property).
        auto value = TRY(temporal_time_like.get(property));

        // c. If value is not undefined, then
        if (!value.is_undefined()) {
            // i. Set any to true.
            any = true;

            // ii. Set value to ? ToIntegerThrowOnInfinity(value).
            auto value_number = TRY(to_integer_throw_on_infinity(global_object, value, ErrorType::TemporalPropertyMustBeFinite));

            // iii. Set result's internal slot whose name is the Internal Slot value of the current row to value.
            result.*internal_slot = value_number;
        }
    }

    // 5. If any is false, then
    if (!any) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalInvalidPlainTimeLikeObject);
    }

    // 6. Return result.
    return result;
}

// 4.5.4 RegulateTime ( hour, minute, second, millisecond, microsecond, nanosecond, overflow ), https://tc39.es/proposal-temporal/#sec-temporal-regulatetime
ThrowCompletionOr<TemporalTime> regulate_time(GlobalObject& global_object, double hour, double minute, double second, double millisecond, double microsecond, double nanosecond, StringView overflow)
{
    auto& vm = global_object.vm();

    // 1. Assert: hour, minute, second, millisecond, microsecond and nanosecond are integers.
    // NOTE: As the spec is currently written this assertion can fail, these are either integers _or_ infinity.
    //       See https://github.com/tc39/proposal-temporal/issues/1672.

    // 2. Assert: overflow is either "constrain" or "reject".
    // NOTE: Asserted by the VERIFY_NOT_REACHED at the end

    // 3. If overflow is "constrain", then
    if (overflow == "constrain"sv) {
        // a. Return ! ConstrainTime(hour, minute, second, millisecond, microsecond, nanosecond).
        return constrain_time(hour, minute, second, millisecond, microsecond, nanosecond);
    }

    // 4. If overflow is "reject", then
    if (overflow == "reject"sv) {
        // a. If ! IsValidTime(hour, minute, second, millisecond, microsecond, nanosecond) is false, throw a RangeError exception.
        if (!is_valid_time(hour, minute, second, millisecond, microsecond, nanosecond))
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);

        // b. Return the Record { [[Hour]]: hour, [[Minute]]: minute, [[Second]]: second, [[Millisecond]]: millisecond, [[Microsecond]]: microsecond, [[Nanosecond]]: nanosecond }.
        return TemporalTime { .hour = static_cast<u8>(hour), .minute = static_cast<u8>(minute), .second = static_cast<u8>(second), .millisecond = static_cast<u16>(millisecond), .microsecond = static_cast<u16>(microsecond), .nanosecond = static_cast<u16>(nanosecond) };
    }

    VERIFY_NOT_REACHED();
}

// 4.5.5 IsValidTime ( hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-isvalidtime
bool is_valid_time(double hour, double minute, double second, double millisecond, double microsecond, double nanosecond)
{
    // 1. Assert: hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. If hour < 0 or hour > 23, then
    if (hour > 23) {
        // a. Return false.
        return false;
    }

    // 3. If minute < 0 or minute > 59, then
    if (minute > 59) {
        // a. Return false.
        return false;
    }

    // 4. If second < 0 or second > 59, then
    if (second > 59) {
        // a. Return false.
        return false;
    }

    // 5. If millisecond < 0 or millisecond > 999, then
    if (millisecond > 999) {
        // a. Return false.
        return false;
    }

    // 6. If microsecond < 0 or microsecond > 999, then
    if (microsecond > 999) {
        // a. Return false.
        return false;
    }

    // 7. If nanosecond < 0 or nanosecond > 999, then
    if (nanosecond > 999) {
        // a. Return false.
        return false;
    }

    // 8. Return true.
    return true;
}

// 4.5.6 BalanceTime ( hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-balancetime
DaysAndTime balance_time(double hour, double minute, double second, double millisecond, double microsecond, double nanosecond)
{
    // 1. Assert: hour, minute, second, millisecond, microsecond, and nanosecond are integers.
    VERIFY(hour == trunc(hour) && minute == trunc(minute) && second == trunc(second) && millisecond == trunc(millisecond) && microsecond == trunc(microsecond) && nanosecond == trunc(nanosecond));

    // 2. Set microsecond to microsecond + floor(nanosecond / 1000).
    microsecond += floor(nanosecond / 1000);

    // 3. Set nanosecond to nanosecond modulo 1000.
    nanosecond = modulo(nanosecond, 1000.0);

    // 4. Set millisecond to millisecond + floor(microsecond / 1000).
    millisecond += floor(microsecond / 1000);

    // 5. Set microsecond to microsecond modulo 1000.
    microsecond = modulo(microsecond, 1000.0);

    // 6. Set second to second + floor(millisecond / 1000).
    second += floor(millisecond / 1000);

    // 7. Set millisecond to millisecond modulo 1000.
    millisecond = modulo(millisecond, 1000.0);

    // 8. Set minute to minute + floor(second / 60).
    minute += floor(second / 60);

    // 9. Set second to second modulo 60.
    second = modulo(second, 60.0);

    // 10. Set hour to hour + floor(minute / 60).
    hour += floor(minute / 60);

    // 11. Set minute to minute modulo 60.
    minute = modulo(minute, 60.0);

    // 12. Let days be floor(hour / 24).
    auto days = floor(hour / 24);

    // 13. Set hour to hour modulo 24.
    hour = modulo(hour, 24.0);

    // 14. Return the Record { [[Days]]: days, [[Hour]]: hour, [[Minute]]: minute, [[Second]]: second, [[Millisecond]]: millisecond, [[Microsecond]]: microsecond, [[Nanosecond]]: nanosecond }.
    return DaysAndTime {
        .days = static_cast<i32>(days),
        .hour = static_cast<u8>(hour),
        .minute = static_cast<u8>(minute),
        .second = static_cast<u8>(second),
        .millisecond = static_cast<u16>(millisecond),
        .microsecond = static_cast<u16>(microsecond),
        .nanosecond = static_cast<u16>(nanosecond),
    };
}

// 4.5.7 ConstrainTime ( hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-constraintime
TemporalTime constrain_time(double hour, double minute, double second, double millisecond, double microsecond, double nanosecond)
{
    // 1. Assert: hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Set hour to ! ConstrainToRange(hour, 0, 23).
    hour = constrain_to_range(hour, 0, 23);

    // 3. Set minute to ! ConstrainToRange(minute, 0, 59).
    minute = constrain_to_range(minute, 0, 59);

    // 4. Set second to ! ConstrainToRange(second, 0, 59).
    second = constrain_to_range(second, 0, 59);

    // 5. Set millisecond to ! ConstrainToRange(millisecond, 0, 999).
    millisecond = constrain_to_range(millisecond, 0, 999);

    // 6. Set microsecond to ! ConstrainToRange(microsecond, 0, 999).
    microsecond = constrain_to_range(microsecond, 0, 999);

    // 7. Set nanosecond to ! ConstrainToRange(nanosecond, 0, 999).
    nanosecond = constrain_to_range(nanosecond, 0, 999);

    // 8. Return the Record { [[Hour]]: hour, [[Minute]]: minute, [[Second]]: second, [[Millisecond]]: millisecond, [[Microsecond]]: microsecond, [[Nanosecond]]: nanosecond }.
    return TemporalTime { .hour = static_cast<u8>(hour), .minute = static_cast<u8>(minute), .second = static_cast<u8>(second), .millisecond = static_cast<u16>(millisecond), .microsecond = static_cast<u16>(microsecond), .nanosecond = static_cast<u16>(nanosecond) };
}

// 4.5.8 CreateTemporalTime ( hour, minute, second, millisecond, microsecond, nanosecond [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporaltime
ThrowCompletionOr<PlainTime*> create_temporal_time(GlobalObject& global_object, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, FunctionObject const* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: hour, minute, second, millisecond, microsecond and nanosecond are integers.

    // 2. If ! IsValidTime(hour, minute, second, millisecond, microsecond, nanosecond) is false, throw a RangeError exception.
    if (!is_valid_time(hour, minute, second, millisecond, microsecond, nanosecond))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainTime);

    // 3. If newTarget is not present, set it to %Temporal.PlainTime%.
    if (!new_target)
        new_target = global_object.temporal_plain_time_constructor();

    // 4. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.PlainTime.prototype%", « [[InitializedTemporalTime]], [[ISOHour]], [[ISOMinute]], [[ISOSecond]], [[ISOMillisecond]], [[ISOMicrosecond]], [[ISONanosecond]], [[Calendar]] »).
    // 5. Set object.[[ISOHour]] to hour.
    // 6. Set object.[[ISOMinute]] to minute.
    // 7. Set object.[[ISOSecond]] to second.
    // 8. Set object.[[ISOMillisecond]] to millisecond.
    // 9. Set object.[[ISOMicrosecond]] to microsecond.
    // 10. Set object.[[ISONanosecond]] to nanosecond.
    // 11. Set object.[[Calendar]] to ! GetISO8601Calendar().
    auto* object = TRY(ordinary_create_from_constructor<PlainTime>(global_object, *new_target, &GlobalObject::temporal_plain_time_prototype, hour, minute, second, millisecond, microsecond, nanosecond, *get_iso8601_calendar(global_object)));

    // 12. Return object.
    return object;
}

// 4.5.9 ToTemporalTimeRecord ( temporalTimeLike ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaltimerecord
ThrowCompletionOr<UnregulatedTemporalTime> to_temporal_time_record(GlobalObject& global_object, Object const& temporal_time_like)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(temporalTimeLike) is Object.

    // 2. Let result be the Record { [[Hour]]: undefined, [[Minute]]: undefined, [[Second]]: undefined, [[Millisecond]]: undefined, [[Microsecond]]: undefined, [[Nanosecond]]: undefined }.
    auto result = UnregulatedTemporalTime {};

    // 3. Let any be false.
    auto any = false;

    // 4. For each row of Table 3, except the header row, in table order, do
    for (auto& [internal_slot, property] : temporal_time_like_properties<UnregulatedTemporalTime, double>(vm)) {
        // a. Let property be the Property value of the current row.

        // b. Let value be ? Get(temporalTimeLike, property).
        auto value = TRY(temporal_time_like.get(property));

        // c. If value is not undefined, then
        if (!value.is_undefined()) {
            // i. Set any to true.
            any = true;
        }

        // d. Set value to ? ToIntegerThrowOnInfinity(value).
        auto value_number = TRY(to_integer_throw_on_infinity(global_object, value, ErrorType::TemporalPropertyMustBeFinite));

        // e. Set result's internal slot whose name is the Internal Slot value of the current row to value.
        result.*internal_slot = value_number;
    }

    // 5. If any is false, then
    if (!any) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalInvalidPlainTimeLikeObject);
    }

    // 6. Return result.
    return result;
}

// 4.5.10 TemporalTimeToString ( hour, minute, second, millisecond, microsecond, nanosecond, precision ), https://tc39.es/proposal-temporal/#sec-temporal-temporaltimetostring
String temporal_time_to_string(u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, Variant<StringView, u8> const& precision)
{
    // 1. Assert: hour, minute, second, millisecond, microsecond and nanosecond are integers.

    // 2. Let hour be hour formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    // 3. Let minute be minute formatted as a two-digit decimal number, padded to the left with a zero if necessary.

    // 4. Let seconds be ! FormatSecondsStringPart(second, millisecond, microsecond, nanosecond, precision).
    auto seconds = format_seconds_string_part(second, millisecond, microsecond, nanosecond, precision);

    // 5. Return the string-concatenation of hour, the code unit 0x003A (COLON), minute, and seconds.
    return String::formatted("{:02}:{:02}{}", hour, minute, seconds);
}

// 4.5.11 CompareTemporalTime ( h1, min1, s1, ms1, mus1, ns1, h2, min2, s2, ms2, mus2, ns2 ), https://tc39.es/proposal-temporal/#sec-temporal-comparetemporaltime
i8 compare_temporal_time(u8 hour1, u8 minute1, u8 second1, u16 millisecond1, u16 microsecond1, u16 nanosecond1, u8 hour2, u8 minute2, u8 second2, u16 millisecond2, u16 microsecond2, u16 nanosecond2)
{
    // 1. Assert: h1, min1, s1, ms1, mus1, ns1, h2, min2, s2, ms2, mus2, and ns2 are integers.

    // 2. If h1 > h2, return 1.
    if (hour1 > hour2)
        return 1;

    // 3. If h1 < h2, return -1.
    if (hour1 < hour2)
        return -1;

    // 4. If min1 > min2, return 1.
    if (minute1 > minute2)
        return 1;

    // 5. If min1 < min2, return -1.
    if (minute1 < minute2)
        return -1;

    // 6. If s1 > s2, return 1.
    if (second1 > second2)
        return 1;

    // 7. If s1 < s2, return -1.
    if (second1 < second2)
        return -1;

    // 8. If ms1 > ms2, return 1.
    if (millisecond1 > millisecond2)
        return 1;

    // 9. If ms1 < ms2, return -1.
    if (millisecond1 < millisecond2)
        return -1;

    // 10. If mus1 > mus2, return 1.
    if (microsecond1 > microsecond2)
        return 1;

    // 11. If mus1 < mus2, return -1.
    if (microsecond1 < microsecond2)
        return -1;

    // 12. If ns1 > ns2, return 1.
    if (nanosecond1 > nanosecond2)
        return 1;

    // 13. If ns1 < ns2, return -1.
    if (nanosecond1 < nanosecond2)
        return -1;

    // 14. Return 0.
    return 0;
}

// 4.5.12 AddTime ( hour, minute, second, millisecond, microsecond, nanosecond, hours, minutes, seconds, milliseconds, microseconds, nanoseconds ), https://tc39.es/proposal-temporal/#sec-temporal-addtime
DaysAndTime add_time(u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds)
{
    // 1. Assert: hour, minute, second, millisecond, microsecond, nanosecond, hours, minutes, seconds, milliseconds, microseconds, and nanoseconds are integers.
    VERIFY(hours == trunc(hours) && minutes == trunc(minutes) && seconds == trunc(seconds) && milliseconds == trunc(milliseconds) && microseconds == trunc(microseconds) && nanoseconds == trunc(nanoseconds));

    // 2. Let hour be hour + hours.
    auto hour_ = hour + hours;

    // 3. Let minute be minute + minutes.
    auto minute_ = minute + minutes;

    // 4. Let second be second + seconds.
    auto second_ = second + seconds;

    // 5. Let millisecond be millisecond + milliseconds.
    auto millisecond_ = millisecond + milliseconds;

    // 6. Let microsecond be microsecond + microseconds.
    auto microsecond_ = microsecond + microseconds;

    // 7. Let nanosecond be nanosecond + nanoseconds.
    auto nanosecond_ = nanosecond + nanoseconds;

    // 8. Return ! BalanceTime(hour, minute, second, millisecond, microsecond, nanosecond).
    return balance_time(hour_, minute_, second_, millisecond_, microsecond_, nanosecond_);
}

// 4.5.13 RoundTime ( hour, minute, second, millisecond, microsecond, nanosecond, increment, unit, roundingMode [ , dayLengthNs ] ), https://tc39.es/proposal-temporal/#sec-temporal-roundtime
DaysAndTime round_time(u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, u64 increment, StringView unit, StringView rounding_mode, Optional<double> day_length_ns)
{
    // 1. Assert: hour, minute, second, millisecond, microsecond, nanosecond, and increment are integers.

    // 2. Let fractionalSecond be nanosecond × 10−9 + microsecond × 10−6 + millisecond × 10−3 + second.
    double fractional_second = nanosecond * 0.000000001 + microsecond * 0.000001 + millisecond * 0.001 + second;
    double quantity;

    // 3. If unit is "day", then
    if (unit == "day"sv) {
        // a. If dayLengthNs is not present, set it to 8.64 × 10^13.
        if (!day_length_ns.has_value())
            day_length_ns = 86400000000000;

        // b. Let quantity be (((((hour × 60 + minute) × 60 + second) × 1000 + millisecond) × 1000 + microsecond) × 1000 + nanosecond) / dayLengthNs.
        quantity = (((((hour * 60 + minute) * 60 + second) * 1000 + millisecond) * 1000 + microsecond) * 1000 + nanosecond) / *day_length_ns;
    }
    // 4. Else if unit is "hour", then
    else if (unit == "hour"sv) {
        // a. Let quantity be (fractionalSecond / 60 + minute) / 60 + hour.
        quantity = (fractional_second / 60 + minute) / 60 + hour;
    }
    // 5. Else if unit is "minute", then
    else if (unit == "minute"sv) {
        // a. Let quantity be fractionalSecond / 60 + minute.
        quantity = fractional_second / 60 + minute;
    }
    // 6. Else if unit is "second", then
    else if (unit == "second"sv) {
        // a. Let quantity be fractionalSecond.
        quantity = fractional_second;
    }
    // 7. Else if unit is "millisecond", then
    else if (unit == "millisecond"sv) {
        // a. Let quantity be nanosecond × 10−6 + microsecond × 10−3 + millisecond.
        quantity = nanosecond * 0.000001 + 0.001 * microsecond + millisecond;
    }
    // 8. Else if unit is "microsecond", then
    else if (unit == "microsecond"sv) {
        // a. Let quantity be nanosecond × 10−3 + microsecond.
        quantity = nanosecond * 0.001 + microsecond;
    }
    // 9. Else,
    else {
        // a. Assert: unit is "nanosecond".
        VERIFY(unit == "nanosecond"sv);

        // b. Let quantity be nanosecond.
        quantity = nanosecond;
    }

    // 10. Let result be ! RoundNumberToIncrement(quantity, increment, roundingMode).
    auto result = round_number_to_increment(quantity, increment, rounding_mode);

    // If unit is "day", then
    if (unit == "day"sv) {
        // a. Return the Record { [[Days]]: result, [[Hour]]: 0, [[Minute]]: 0, [[Second]]: 0, [[Millisecond]]: 0, [[Microsecond]]: 0, [[Nanosecond]]: 0 }.
        return DaysAndTime { .days = (i32)result, .hour = 0, .minute = 0, .second = 0, .millisecond = 0, .microsecond = 0, .nanosecond = 0 };
    }

    // 12. If unit is "hour", then
    if (unit == "hour"sv) {
        // a. Return ! BalanceTime(result, 0, 0, 0, 0, 0).
        return balance_time(result, 0, 0, 0, 0, 0);
    }

    // 13. If unit is "minute", then
    if (unit == "minute"sv) {
        // a. Return ! BalanceTime(hour, result, 0, 0, 0, 0).
        return balance_time(hour, result, 0, 0, 0, 0);
    }

    // 14. If unit is "second", then
    if (unit == "second"sv) {
        // a. Return ! BalanceTime(hour, minute, result, 0, 0, 0).
        return balance_time(hour, minute, result, 0, 0, 0);
    }

    // 15. If unit is "millisecond", then
    if (unit == "millisecond"sv) {
        // a. Return ! BalanceTime(hour, minute, second, result, 0, 0).
        return balance_time(hour, minute, second, result, 0, 0);
    }

    // 16. If unit is "microsecond", then
    if (unit == "microsecond"sv) {
        // a. Return ! BalanceTime(hour, minute, second, millisecond, result, 0).
        return balance_time(hour, minute, second, millisecond, result, 0);
    }

    // 17. Assert: unit is "nanosecond".
    VERIFY(unit == "nanosecond"sv);

    // 18. Return ! BalanceTime(hour, minute, second, millisecond, microsecond, result).
    return balance_time(hour, minute, second, millisecond, microsecond, result);
}

}
