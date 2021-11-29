/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainDateTimeConstructor.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>

namespace JS::Temporal {

// 5 Temporal.PlainDateTime Objects, https://tc39.es/proposal-temporal/#sec-temporal-plaindatetime-objects
PlainDateTime::PlainDateTime(i32 iso_year, u8 iso_month, u8 iso_day, u8 iso_hour, u8 iso_minute, u8 iso_second, u16 iso_millisecond, u16 iso_microsecond, u16 iso_nanosecond, Object& calendar, Object& prototype)
    : Object(prototype)
    , m_iso_year(iso_year)
    , m_iso_month(iso_month)
    , m_iso_day(iso_day)
    , m_iso_hour(iso_hour)
    , m_iso_minute(iso_minute)
    , m_iso_second(iso_second)
    , m_iso_millisecond(iso_millisecond)
    , m_iso_microsecond(iso_microsecond)
    , m_iso_nanosecond(iso_nanosecond)
    , m_calendar(calendar)
{
}

void PlainDateTime::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_calendar);
}

// 5.5.1 GetEpochFromISOParts ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-getepochfromisoparts
BigInt* get_epoch_from_iso_parts(GlobalObject& global_object, i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond)
{
    auto& vm = global_object.vm();

    // 1. Assert: year, month, day, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Assert: ! IsValidISODate(year, month, day) is true.
    VERIFY(is_valid_iso_date(year, month, day));

    // 3. Assert: ! IsValidTime(hour, minute, second, millisecond, microsecond, nanosecond) is true.
    VERIFY(is_valid_time(hour, minute, second, millisecond, microsecond, nanosecond));

    // 4. Let date be ! MakeDay(𝔽(year), 𝔽(month − 1), 𝔽(day)).
    auto date = make_day(global_object, Value(year), Value(month - 1), Value(day));

    // 5. Let time be ! MakeTime(𝔽(hour), 𝔽(minute), 𝔽(second), 𝔽(millisecond)).
    auto time = make_time(global_object, Value(hour), Value(minute), Value(second), Value(millisecond));

    // 6. Let ms be ! MakeDate(date, time).
    auto ms = make_date(date, time);

    // 7. Assert: ms is finite.
    VERIFY(ms.is_finite_number());

    // 8. Return ℝ(ms) × 10^6 + microsecond × 10^3 + nanosecond.
    return js_bigint(vm, Crypto::SignedBigInteger::create_from(static_cast<i64>(ms.as_double())).multiplied_by(Crypto::UnsignedBigInteger { 1'000'000 }).plus(Crypto::SignedBigInteger::create_from((i64)microsecond * 1000)).plus(Crypto::SignedBigInteger(nanosecond)));
}

// -864 * 10^19 - 864 * 10^11
const auto DATETIME_NANOSECONDS_MIN = "-8640000086400000000000"_sbigint;
// +864 * 10^19 + 864 * 10^11
const auto DATETIME_NANOSECONDS_MAX = "8640000086400000000000"_sbigint;

// 5.5.2 ISODateTimeWithinLimits ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-isodatetimewithinlimits
bool iso_date_time_within_limits(GlobalObject& global_object, i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond)
{
    // 1. Assert: year, month, day, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Let ns be ! GetEpochFromISOParts(year, month, day, hour, minute, second, millisecond, microsecond, nanosecond).
    auto ns = get_epoch_from_iso_parts(global_object, year, month, day, hour, minute, second, millisecond, microsecond, nanosecond);

    // 3. If ns ≤ -8.64 × 10^21 - 8.64 × 10^13, then
    if (ns->big_integer() <= DATETIME_NANOSECONDS_MIN) {
        // a. Return false.
        return false;
    }

    // 4. If ns ≥ 8.64 × 10^21 + 8.64 × 10^13, then
    if (ns->big_integer() >= DATETIME_NANOSECONDS_MAX) {
        // a. Return false.
        return false;
    }
    // 5. Return true.
    return true;
}

// 5.5.3 InterpretTemporalDateTimeFields ( calendar, fields, options ), https://tc39.es/proposal-temporal/#sec-temporal-interprettemporaldatetimefields
ThrowCompletionOr<ISODateTime> interpret_temporal_date_time_fields(GlobalObject& global_object, Object& calendar, Object& fields, Object& options)
{
    // 1. Let timeResult be ? ToTemporalTimeRecord(fields).
    auto unregulated_time_result = TRY(to_temporal_time_record(global_object, fields));

    // 2. Let overflow be ? ToTemporalOverflow(options).
    auto overflow = TRY(to_temporal_overflow(global_object, options));

    // 3. Let temporalDate be ? DateFromFields(calendar, fields, options).
    auto* temporal_date = TRY(date_from_fields(global_object, calendar, fields, options));

    // 4. Let timeResult be ? RegulateTime(timeResult.[[Hour]], timeResult.[[Minute]], timeResult.[[Second]], timeResult.[[Millisecond]], timeResult.[[Microsecond]], timeResult.[[Nanosecond]], overflow).
    auto time_result = TRY(regulate_time(global_object, unregulated_time_result.hour, unregulated_time_result.minute, unregulated_time_result.second, unregulated_time_result.millisecond, unregulated_time_result.microsecond, unregulated_time_result.nanosecond, overflow));

    // 5. Return the Record { [[Year]]: temporalDate.[[ISOYear]], [[Month]]: temporalDate.[[ISOMonth]], [[Day]]: temporalDate.[[ISODay]], [[Hour]]: timeResult.[[Hour]], [[Minute]]: timeResult.[[Minute]], [[Second]]: timeResult.[[Second]], [[Millisecond]]: timeResult.[[Millisecond]], [[Microsecond]]: timeResult.[[Microsecond]], [[Nanosecond]]: timeResult.[[Nanosecond]] }.
    return ISODateTime {
        .year = temporal_date->iso_year(),
        .month = temporal_date->iso_month(),
        .day = temporal_date->iso_day(),
        .hour = time_result.hour,
        .minute = time_result.minute,
        .second = time_result.second,
        .millisecond = time_result.millisecond,
        .microsecond = time_result.microsecond,
        .nanosecond = time_result.nanosecond,
    };
}

// 5.5.4 ToTemporalDateTime ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaldatetime
ThrowCompletionOr<PlainDateTime*> to_temporal_date_time(GlobalObject& global_object, Value item, Object* options)
{
    auto& vm = global_object.vm();

    // 1. If options is not present, set options to ! OrdinaryObjectCreate(null).
    if (!options)
        options = Object::create(global_object, nullptr);

    Object* calendar;
    ISODateTime result;

    // 2. If Type(item) is Object, then
    if (item.is_object()) {
        auto& item_object = item.as_object();

        // a. If item has an [[InitializedTemporalDateTime]] internal slot, then
        if (is<PlainDateTime>(item_object)) {
            // i. Return item.
            return &static_cast<PlainDateTime&>(item_object);
        }

        // b. If item has an [[InitializedTemporalZonedDateTime]] internal slot, then
        if (is<ZonedDateTime>(item_object)) {
            auto& zoned_date_time = static_cast<ZonedDateTime&>(item_object);

            // i. Let instant be ! CreateTemporalInstant(item.[[Nanoseconds]]).
            auto* instant = create_temporal_instant(global_object, zoned_date_time.nanoseconds()).release_value();

            // ii. Return ? BuiltinTimeZoneGetPlainDateTimeFor(item.[[TimeZone]], instant, item.[[Calendar]]).
            return builtin_time_zone_get_plain_date_time_for(global_object, &zoned_date_time.time_zone(), *instant, zoned_date_time.calendar());
        }

        // c. If item has an [[InitializedTemporalDate]] internal slot, then
        if (is<PlainDate>(item_object)) {
            auto& plain_date = static_cast<PlainDate&>(item_object);

            // i. Return ? CreateTemporalDateTime(item.[[ISOYear]], item.[[ISOMonth]], item.[[ISODay]], 0, 0, 0, 0, 0, 0, item.[[Calendar]]).
            return create_temporal_date_time(global_object, plain_date.iso_year(), plain_date.iso_month(), plain_date.iso_day(), 0, 0, 0, 0, 0, 0, plain_date.calendar());
        }

        // d. Let calendar be ? GetTemporalCalendarWithISODefault(item).
        calendar = TRY(get_temporal_calendar_with_iso_default(global_object, item_object));

        // e. Let fieldNames be ? CalendarFields(calendar, « "day", "hour", "microsecond", "millisecond", "minute", "month", "monthCode", "nanosecond", "second", "year" »).
        auto field_names = TRY(calendar_fields(global_object, *calendar, { "day"sv, "hour"sv, "microsecond"sv, "millisecond"sv, "minute"sv, "month"sv, "monthCode"sv, "nanosecond"sv, "second"sv, "year"sv }));

        // f. Let fields be ? PrepareTemporalFields(item, fieldNames, «»).
        auto* fields = TRY(prepare_temporal_fields(global_object, item_object, field_names, {}));

        // g. Let result be ? InterpretTemporalDateTimeFields(calendar, fields, options).
        result = TRY(interpret_temporal_date_time_fields(global_object, *calendar, *fields, *options));
    }
    // 3. Else,
    else {
        // a. Perform ? ToTemporalOverflow(options).
        (void)TRY(to_temporal_overflow(global_object, *options));

        // b. Let string be ? ToString(item).
        auto string = TRY(item.to_string(global_object));

        // c. Let result be ? ParseTemporalDateTimeString(string).
        result = TRY(parse_temporal_date_time_string(global_object, string));

        // d. Assert: ! IsValidISODate(result.[[Year]], result.[[Month]], result.[[Day]]) is true.
        VERIFY(is_valid_iso_date(result.year, result.month, result.day));

        // e. Assert: ! IsValidTime(result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]]) is true.
        VERIFY(is_valid_time(result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond));

        // f. Let calendar be ? ToTemporalCalendarWithISODefault(result.[[Calendar]]).
        calendar = TRY(to_temporal_calendar_with_iso_default(global_object, result.calendar.has_value() ? js_string(vm, *result.calendar) : js_undefined()));
    }

    // 4. Return ? CreateTemporalDateTime(result.[[Year]], result.[[Month]], result.[[Day]], result.[[Hour]], result.[[Minute]], result.[[Second]], result.[[Millisecond]], result.[[Microsecond]], result.[[Nanosecond]], calendar).
    return create_temporal_date_time(global_object, result.year, result.month, result.day, result.hour, result.minute, result.second, result.millisecond, result.microsecond, result.nanosecond, *calendar);
}

// 5.5.5 BalanceISODateTime ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond ), https://tc39.es/proposal-temporal/#sec-temporal-balanceisodatetime
ISODateTime balance_iso_date_time(i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, i64 nanosecond)
{
    // NOTE: The only use of this AO is in BuiltinTimeZoneGetPlainDateTimeFor, where we know that all values
    // but `nanosecond` are in their usual range, hence why that's the only outlier here. The range for that
    // is -86400000000000 to 86400000000999, so an i32 is not enough.

    // 1. Assert: year, month, day, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Let balancedTime be ! BalanceTime(hour, minute, second, millisecond, microsecond, nanosecond).
    auto balanced_time = balance_time(hour, minute, second, millisecond, microsecond, nanosecond);

    // 3. Let balancedDate be ! BalanceISODate(year, month, day + balancedTime.[[Days]]).
    auto balanced_date = balance_iso_date(year, month, day + balanced_time.days);

    // 4. Return the Record { [[Year]]: balancedDate.[[Year]], [[Month]]: balancedDate.[[Month]], [[Day]]: balancedDate.[[Day]], [[Hour]]: balancedTime.[[Hour]], [[Minute]]: balancedTime.[[Minute]], [[Second]]: balancedTime.[[Second]], [[Millisecond]]: balancedTime.[[Millisecond]], [[Microsecond]]: balancedTime.[[Microsecond]], [[Nanosecond]]: balancedTime.[[Nanosecond]] }.
    return ISODateTime { .year = balanced_date.year, .month = balanced_date.month, .day = balanced_date.day, .hour = balanced_time.hour, .minute = balanced_time.minute, .second = balanced_time.second, .millisecond = balanced_time.millisecond, .microsecond = balanced_time.microsecond, .nanosecond = balanced_time.nanosecond };
}

// 5.5.6 CreateTemporalDateTime ( isoYear, isoMonth, isoDay, hour, minute, second, millisecond, microsecond, nanosecond, calendar [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporaldatetime
ThrowCompletionOr<PlainDateTime*> create_temporal_date_time(GlobalObject& global_object, i32 iso_year, u8 iso_month, u8 iso_day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, Object& calendar, FunctionObject const* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: isoYear, isoMonth, isoDay, hour, minute, second, millisecond, microsecond, and nanosecond are integers.
    // 2. Assert: Type(calendar) is Object.

    // 3. If ! IsValidISODate(isoYear, isoMonth, isoDay) is false, throw a RangeError exception.
    if (!is_valid_iso_date(iso_year, iso_month, iso_day))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);

    // 4. If ! IsValidTime(hour, minute, second, millisecond, microsecond, nanosecond) is false, throw a RangeError exception.
    if (!is_valid_time(hour, minute, second, millisecond, microsecond, nanosecond))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);

    // 5. If ! ISODateTimeWithinLimits(isoYear, isoMonth, isoDay, hour, minute, second, millisecond, microsecond, nanosecond) is false, then
    if (!iso_date_time_within_limits(global_object, iso_year, iso_month, iso_day, hour, minute, second, millisecond, microsecond, nanosecond)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainDateTime);
    }

    // 6. If newTarget is not present, set it to %Temporal.PlainDateTime%.
    if (!new_target)
        new_target = global_object.temporal_plain_date_time_constructor();

    // 7. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.PlainDateTime.prototype%", « [[InitializedTemporalDateTime]], [[ISOYear]], [[ISOMonth]], [[ISODay]], [[ISOHour]], [[ISOMinute]], [[ISOSecond]], [[ISOMillisecond]], [[ISOMicrosecond]], [[ISONanosecond]], [[Calendar]] »).
    // 8. Set object.[[ISOYear]] to isoYear.
    // 9. Set object.[[ISOMonth]] to isoMonth.
    // 10. Set object.[[ISODay]] to isoDay.
    // 11. Set object.[[ISOHour]] to hour.
    // 12. Set object.[[ISOMinute]] to minute.
    // 13. Set object.[[ISOSecond]] to second.
    // 14. Set object.[[ISOMillisecond]] to millisecond.
    // 15. Set object.[[ISOMicrosecond]] to microsecond.
    // 16. Set object.[[ISONanosecond]] to nanosecond.
    // 17. Set object.[[Calendar]] to calendar.
    auto* object = TRY(ordinary_create_from_constructor<PlainDateTime>(global_object, *new_target, &GlobalObject::temporal_plain_date_prototype, iso_year, iso_month, iso_day, hour, minute, second, millisecond, microsecond, nanosecond, calendar));

    // 18. Return object.
    return object;
}

// 5.5.7 TemporalDateTimeToString ( isoYear, isoMonth, isoDay, hour, minute, second, millisecond, microsecond, nanosecond, calendar, precision, showCalendar ), https://tc39.es/proposal-temporal/#sec-temporal-temporaldatetimetostring
ThrowCompletionOr<String> temporal_date_time_to_string(GlobalObject& global_object, i32 iso_year, u8 iso_month, u8 iso_day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, Value calendar, Variant<StringView, u8> const& precision, StringView show_calendar)
{
    // 1. Assert: isoYear, isoMonth, isoDay, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Let year be ! PadISOYear(isoYear).
    // 3. Let month be isoMonth formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    // 4. Let day be isoDay formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    // 5. Let hour be hour formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    // 6. Let minute be minute formatted as a two-digit decimal number, padded to the left with a zero if necessary.

    // 7. Let seconds be ! FormatSecondsStringPart(second, millisecond, microsecond, nanosecond, precision).
    auto seconds = format_seconds_string_part(second, millisecond, microsecond, nanosecond, precision);

    // 8. Let calendarID be ? ToString(calendar).
    auto calendar_id = TRY(calendar.to_string(global_object));

    // 9. Let calendarString be ! FormatCalendarAnnotation(calendarID, showCalendar).
    auto calendar_string = format_calendar_annotation(calendar_id, show_calendar);

    // 10. Return the string-concatenation of year, the code unit 0x002D (HYPHEN-MINUS), month, the code unit 0x002D (HYPHEN-MINUS), day, 0x0054 (LATIN CAPITAL LETTER T), hour, the code unit 0x003A (COLON), minute, seconds, and calendarString.
    return String::formatted("{}-{:02}-{:02}T{:02}:{:02}{}{}", pad_iso_year(iso_year), iso_month, iso_day, hour, minute, seconds, calendar_string);
}

// 5.5.8 CompareISODateTime ( y1, mon1, d1, h1, min1, s1, ms1, mus1, ns1, y2, mon2, d2, h2, min2, s2, ms2, mus2, ns2 ), https://tc39.es/proposal-temporal/#sec-temporal-compareisodatetime
i8 compare_iso_date_time(i32 year1, u8 month1, u8 day1, u8 hour1, u8 minute1, u8 second1, u16 millisecond1, u16 microsecond1, u16 nanosecond1, i32 year2, u8 month2, u8 day2, u8 hour2, u8 minute2, u8 second2, u16 millisecond2, u16 microsecond2, u16 nanosecond2)
{
    // 1. Assert: y1, mon1, d1, h1, min1, s1, ms1, mus1, ns1, y2, mon2, d2, h2, min2, s2, ms2, mus2, and ns2 are integers.

    // 2. Let dateResult be ! CompareISODate(y1, mon1, d1, y2, mon2, d2).
    auto date_result = compare_iso_date(year1, month1, day1, year2, month2, day2);

    // 3. If dateResult is not 0, then
    if (date_result != 0) {
        // a. Return dateResult.
        return date_result;
    }

    // 4. Return ! CompareTemporalTime(h1, min1, s1, ms1, mus1, ns1, h2, min2, s2, ms2, mus2, ns2).
    return compare_temporal_time(hour1, minute1, second1, millisecond1, microsecond1, nanosecond1, hour2, minute2, second2, millisecond2, microsecond2, nanosecond2);
}

// 5.5.9 AddDateTime ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, calendar, years, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds, options ), https://tc39.es/proposal-temporal/#sec-temporal-adddatetime
ThrowCompletionOr<TemporalPlainDateTime> add_date_time(GlobalObject& global_object, i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, Object& calendar, double years, double months, double weeks, double days, double hours, double minutes, double seconds, double milliseconds, double microseconds, double nanoseconds, Object* options)
{
    // 1. Assert: year, month, day, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. Let timeResult be ! AddTime(hour, minute, second, millisecond, microsecond, nanosecond, hours, minutes, seconds, milliseconds, microseconds, nanoseconds).
    auto time_result = add_time(hour, minute, second, millisecond, microsecond, nanosecond, hours, minutes, seconds, milliseconds, microseconds, nanoseconds);

    // 3. Let datePart be ? CreateTemporalDate(year, month, day, calendar).
    auto* date_part = TRY(create_temporal_date(global_object, year, month, day, calendar));

    // 4. Let dateDuration be ? CreateTemporalDuration(years, months, weeks, days + timeResult.[[Days]], 0, 0, 0, 0, 0, 0).
    auto* date_duration = TRY(create_temporal_duration(global_object, years, months, weeks, days + time_result.days, 0, 0, 0, 0, 0, 0));

    // 5. Let addedDate be ? CalendarDateAdd(calendar, datePart, dateDuration, options).
    auto* added_date = TRY(calendar_date_add(global_object, calendar, date_part, *date_duration, options));

    // 6. Return the Record { [[Year]]: addedDate.[[ISOYear]], [[Month]]: addedDate.[[ISOMonth]], [[Day]]: addedDate.[[ISODay]], [[Hour]]: timeResult.[[Hour]], [[Minute]]: timeResult.[[Minute]], [[Second]]: timeResult.[[Second]], [[Millisecond]]: timeResult.[[Millisecond]], [[Microsecond]]: timeResult.[[Microsecond]], [[Nanosecond]]: timeResult.[[Nanosecond]] }.
    return TemporalPlainDateTime { .year = added_date->iso_year(), .month = added_date->iso_month(), .day = added_date->iso_day(), .hour = time_result.hour, .minute = time_result.minute, .second = time_result.second, .millisecond = time_result.millisecond, .microsecond = time_result.microsecond, .nanosecond = time_result.nanosecond };
}

// 5.5.10 RoundISODateTime ( year, month, day, hour, minute, second, millisecond, microsecond, nanosecond, increment, unit, roundingMode [ , dayLength ] ), https://tc39.es/proposal-temporal/#sec-temporal-roundisodatetime
ISODateTime round_iso_date_time(i32 year, u8 month, u8 day, u8 hour, u8 minute, u8 second, u16 millisecond, u16 microsecond, u16 nanosecond, u64 increment, StringView unit, StringView rounding_mode, Optional<double> day_length)
{
    // 1. Assert: year, month, day, hour, minute, second, millisecond, microsecond, and nanosecond are integers.

    // 2. If dayLength is not present, set dayLength to 8.64 × 10^13.
    if (!day_length.has_value())
        day_length = 86400000000000;

    // 3. Let roundedTime be ! RoundTime(hour, minute, second, millisecond, microsecond, nanosecond, increment, unit, roundingMode, dayLength).
    auto rounded_time = round_time(hour, minute, second, millisecond, microsecond, nanosecond, increment, unit, rounding_mode, day_length);

    // 4. Let balanceResult be ! BalanceISODate(year, month, day + roundedTime.[[Days]]).
    auto balance_result = balance_iso_date(year, month, day + rounded_time.days);

    // 5. Return the Record { [[Year]]: balanceResult.[[Year]], [[Month]]: balanceResult.[[Month]], [[Day]]: balanceResult.[[Day]], [[Hour]]: roundedTime.[[Hour]], [[Minute]]: roundedTime.[[Minute]], [[Second]]: roundedTime.[[Second]], [[Millisecond]]: roundedTime.[[Millisecond]], [[Microsecond]]: roundedTime.[[Microsecond]], [[Nanosecond]]: roundedTime.[[Nanosecond]] }.
    return ISODateTime { .year = balance_result.year, .month = balance_result.month, .day = balance_result.day, .hour = rounded_time.hour, .minute = rounded_time.minute, .second = rounded_time.second, .millisecond = rounded_time.millisecond, .microsecond = rounded_time.microsecond, .nanosecond = rounded_time.nanosecond };
}

// 5.5.11 DifferenceISODateTime ( y1, mon1, d1, h1, min1, s1, ms1, mus1, ns1, y2, mon2, d2, h2, min2, s2, ms2, mus2, ns2, calendar, largestUnit [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-differenceisodatetime
ThrowCompletionOr<TemporalDuration> difference_iso_date_time(GlobalObject& global_object, i32 year1, u8 month1, u8 day1, u8 hour1, u8 minute1, u8 second1, u16 millisecond1, u16 microsecond1, u16 nanosecond1, i32 year2, u8 month2, u8 day2, u8 hour2, u8 minute2, u8 second2, u16 millisecond2, u16 microsecond2, u16 nanosecond2, Object& calendar, StringView largest_unit, Object* options)
{
    auto& vm = global_object.vm();

    // 1. Assert: y1, mon1, d1, h1, min1, s1, ms1, mus1, ns1, y2, mon2, d2, h2, min2, s2, ms2, mus2, and ns2 are integers.

    // 2. If options is not present, set options to ! OrdinaryObjectCreate(null).
    if (!options)
        options = Object::create(global_object, nullptr);

    // 3. Let timeDifference be ! DifferenceTime(h1, min1, s1, ms1, mus1, ns1, h2, min2, s2, ms2, mus2, ns2).
    auto time_difference = difference_time(hour1, minute1, second1, millisecond1, microsecond1, nanosecond1, hour2, minute2, second2, millisecond2, microsecond2, nanosecond2);

    // 4. Let timeSign be ! DurationSign(0, 0, 0, timeDifference.[[Days]], timeDifference.[[Hours]], timeDifference.[[Minutes]], timeDifference.[[Seconds]], timeDifference.[[Milliseconds]], timeDifference.[[Microseconds]], timeDifference.[[Nanoseconds]]).
    auto time_sign = duration_sign(0, 0, 0, time_difference.days, time_difference.hours, time_difference.minutes, time_difference.seconds, time_difference.milliseconds, time_difference.microseconds, time_difference.nanoseconds);

    // 5. Let dateSign be ! CompareISODate(y2, mon2, d2, y1, mon1, d1).
    auto date_sign = compare_iso_date(year2, month2, day2, year1, month1, day1);

    // 6. Let balanceResult be ! BalanceISODate(y1, mon1, d1 + timeDifference.[[Days]]).
    auto balance_result = balance_iso_date(year1, month1, day1 + time_difference.days);

    // 7. If timeSign is -dateSign, then
    if (time_sign == -date_sign) {
        // a. Set balanceResult to ! BalanceISODate(balanceResult.[[Year]], balanceResult.[[Month]], balanceResult.[[Day]] - timeSign).
        balance_result = balance_iso_date(balance_result.year, balance_result.month, balance_result.day - time_sign);

        // b. Set timeDifference to ? BalanceDuration(-timeSign, timeDifference.[[Hours]], timeDifference.[[Minutes]], timeDifference.[[Seconds]], timeDifference.[[Milliseconds]], timeDifference.[[Microseconds]], timeDifference.[[Nanoseconds]], largestUnit).
        time_difference = TRY(balance_duration(global_object, -time_sign, time_difference.hours, time_difference.minutes, time_difference.seconds, time_difference.milliseconds, time_difference.microseconds, *js_bigint(vm, { (i32)time_difference.nanoseconds }), largest_unit));
    }

    // 8. Let date1 be ? CreateTemporalDate(balanceResult.[[Year]], balanceResult.[[Month]], balanceResult.[[Day]], calendar).
    auto* date1 = TRY(create_temporal_date(global_object, balance_result.year, balance_result.month, balance_result.day, calendar));

    // 9. Let date2 be ? CreateTemporalDate(y2, mon2, d2, calendar).
    auto* date2 = TRY(create_temporal_date(global_object, year2, month2, day2, calendar));

    // 10. Let dateLargestUnit be ! LargerOfTwoTemporalUnits("day", largestUnit).
    auto date_largest_unit = larger_of_two_temporal_units("day"sv, largest_unit);

    // 11. Let untilOptions be ? MergeLargestUnitOption(options, dateLargestUnit).
    auto* until_options = TRY(merge_largest_unit_option(global_object, *options, date_largest_unit));

    // 12. Let dateDifference be ? CalendarDateUntil(calendar, date1, date2, untilOptions).
    auto* date_difference = TRY(calendar_date_until(global_object, calendar, date1, date2, *until_options));

    // 13. Let balanceResult be ? BalanceDuration(dateDifference.[[Days]], timeDifference.[[Hours]], timeDifference.[[Minutes]], timeDifference.[[Seconds]], timeDifference.[[Milliseconds]], timeDifference.[[Microseconds]], timeDifference.[[Nanoseconds]], largestUnit).
    auto balance_result_ = TRY(balance_duration(global_object, date_difference->days(), time_difference.hours, time_difference.minutes, time_difference.seconds, time_difference.milliseconds, time_difference.microseconds, *js_bigint(vm, { (i32)time_difference.nanoseconds }), largest_unit));

    // 14. Return the Record { [[Years]]: dateDifference.[[Years]], [[Months]]: dateDifference.[[Months]], [[Weeks]]: dateDifference.[[Weeks]], [[Days]]: balanceResult.[[Days]], [[Hours]]: balanceResult.[[Hours]], [[Minutes]]: balanceResult.[[Minutes]], [[Seconds]]: balanceResult.[[Seconds]], [[Milliseconds]]: balanceResult.[[Milliseconds]], [[Microseconds]]: balanceResult.[[Microseconds]], [[Nanoseconds]]: balanceResult.[[Nanoseconds]] }.
    return TemporalDuration { .years = date_difference->years(), .months = date_difference->months(), .weeks = date_difference->weeks(), .days = balance_result_.days, .hours = balance_result_.hours, .minutes = balance_result_.minutes, .seconds = balance_result_.seconds, .milliseconds = balance_result_.milliseconds, .microseconds = balance_result_.microseconds, .nanoseconds = balance_result_.nanoseconds };
}

}
