/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/Instant.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateConstructor.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>
#include <LibJS/Runtime/Temporal/TimeZone.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>

namespace JS::Temporal {

// 3 Temporal.PlainDate Objects, https://tc39.es/proposal-temporal/#sec-temporal-plaindate-objects
PlainDate::PlainDate(i32 year, u8 month, u8 day, Object& calendar, Object& prototype)
    : Object(prototype)
    , m_iso_year(year)
    , m_iso_month(month)
    , m_iso_day(day)
    , m_calendar(calendar)
{
}

void PlainDate::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_calendar);
}

// 3.5.1 CreateTemporalDate ( isoYear, isoMonth, isoDay, calendar [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporaldate
ThrowCompletionOr<PlainDate*> create_temporal_date(GlobalObject& global_object, i32 iso_year, u8 iso_month, u8 iso_day, Object& calendar, FunctionObject const* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: isoYear is an integer.
    // 2. Assert: isoMonth is an integer.
    // 3. Assert: isoDay is an integer.
    // 4. Assert: Type(calendar) is Object.

    // 5. If IsValidISODate(isoYear, isoMonth, isoDay) is false, throw a RangeError exception.
    if (!is_valid_iso_date(iso_year, iso_month, iso_day))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);

    // 6. If ISODateTimeWithinLimits(isoYear, isoMonth, isoDay, 12, 0, 0, 0, 0, 0) is false, throw a RangeError exception.
    if (!iso_date_time_within_limits(global_object, iso_year, iso_month, iso_day, 12, 0, 0, 0, 0, 0))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);

    // 7. If newTarget is not present, set newTarget to %Temporal.PlainDate%.
    if (!new_target)
        new_target = global_object.temporal_plain_date_constructor();

    // 8. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.PlainDate.prototype%", « [[InitializedTemporalDate]], [[ISOYear]], [[ISOMonth]], [[ISODay]], [[Calendar]] »).
    // 9. Set object.[[ISOYear]] to isoYear.
    // 10. Set object.[[ISOMonth]] to isoMonth.
    // 11. Set object.[[ISODay]] to isoDay.
    // 12. Set object.[[Calendar]] to calendar.
    auto* object = TRY(ordinary_create_from_constructor<PlainDate>(global_object, *new_target, &GlobalObject::temporal_plain_date_prototype, iso_year, iso_month, iso_day, calendar));

    return object;
}

// 3.5.2 ToTemporalDate ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaldate
ThrowCompletionOr<PlainDate*> to_temporal_date(GlobalObject& global_object, Value item, Object const* options)
{
    auto& vm = global_object.vm();

    // 1. If options is not present, set options to undefined.
    // 2. Assert: Type(options) is Object or Undefined.

    // 3. If Type(item) is Object, then
    if (item.is_object()) {
        auto& item_object = item.as_object();
        // a. If item has an [[InitializedTemporalDate]] internal slot, then
        if (is<PlainDate>(item_object)) {
            // i. Return item.
            return static_cast<PlainDate*>(&item_object);
        }

        // b. If item has an [[InitializedTemporalZonedDateTime]] internal slot, then
        if (is<ZonedDateTime>(item_object)) {
            auto& zoned_date_time = static_cast<ZonedDateTime&>(item_object);

            // i. Let instant be ! CreateTemporalInstant(item.[[Nanoseconds]]).
            auto* instant = create_temporal_instant(global_object, zoned_date_time.nanoseconds()).release_value();

            // ii. Let plainDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(item.[[TimeZone]], instant, item.[[Calendar]]).
            auto* plain_date_time = TRY(builtin_time_zone_get_plain_date_time_for(global_object, &zoned_date_time.time_zone(), *instant, zoned_date_time.calendar()));

            // iii. Return ! CreateTemporalDate(plainDateTime.[[ISOYear]], plainDateTime.[[ISOMonth]], plainDateTime.[[ISODay]], plainDateTime.[[Calendar]]).
            return create_temporal_date(global_object, plain_date_time->iso_year(), plain_date_time->iso_month(), plain_date_time->iso_day(), plain_date_time->calendar());
        }

        // c. If item has an [[InitializedTemporalDateTime]] internal slot, then
        if (is<PlainDateTime>(item_object)) {
            auto& date_time_item = static_cast<PlainDateTime&>(item_object);
            // i. Return ! CreateTemporalDate(item.[[ISOYear]], item.[[ISOMonth]], item.[[ISODay]], item.[[Calendar]]).
            return create_temporal_date(global_object, date_time_item.iso_year(), date_time_item.iso_month(), date_time_item.iso_day(), date_time_item.calendar());
        }

        // d. Let calendar be ? GetTemporalCalendarWithISODefault(item).
        auto* calendar = TRY(get_temporal_calendar_with_iso_default(global_object, item_object));

        // e. Let fieldNames be ? CalendarFields(calendar, « "day", "month", "monthCode", "year" »).
        auto field_names = TRY(calendar_fields(global_object, *calendar, { "day"sv, "month"sv, "monthCode"sv, "year"sv }));

        // f. Let fields be ? PrepareTemporalFields(item, fieldNames, «»).
        auto* fields = TRY(prepare_temporal_fields(global_object, item_object, field_names, {}));

        // g. Return ? CalendarDateFromFields(calendar, fields, options).
        return calendar_date_from_fields(global_object, *calendar, *fields, options);
    }

    // 4. Perform ? ToTemporalOverflow(options).
    (void)TRY(to_temporal_overflow(global_object, options));

    // 5. Let string be ? ToString(item).
    auto string = TRY(item.to_string(global_object));

    // 6. Let result be ? ParseTemporalDateString(string).
    auto result = TRY(parse_temporal_date_string(global_object, string));

    // 7. Assert: IsValidISODate(result.[[Year]], result.[[Month]], result.[[Day]]) is true.
    VERIFY(is_valid_iso_date(result.year, result.month, result.day));

    // 8. Let calendar be ? ToTemporalCalendarWithISODefault(result.[[Calendar]]).
    auto* calendar = TRY(to_temporal_calendar_with_iso_default(global_object, result.calendar.has_value() ? js_string(vm, *result.calendar) : js_undefined()));

    // 9. Return ? CreateTemporalDate(result.[[Year]], result.[[Month]], result.[[Day]], calendar).
    return create_temporal_date(global_object, result.year, result.month, result.day, *calendar);
}

// 3.5.3 DifferenceISODate ( y1, m1, d1, y2, m2, d2, largestUnit ), https://tc39.es/proposal-temporal/#sec-temporal-differenceisodate
DateDurationRecord difference_iso_date(GlobalObject& global_object, i32 year1, u8 month1, u8 day1, i32 year2, u8 month2, u8 day2, StringView largest_unit)
{
    VERIFY(largest_unit.is_one_of("year"sv, "month"sv, "week"sv, "day"sv));

    // 1. If largestUnit is "year" or "month", then
    if (largest_unit.is_one_of("year"sv, "month"sv)) {
        // a. Let sign be -(! CompareISODate(y1, m1, d1, y2, m2, d2)).
        auto sign = -compare_iso_date(year1, month1, day1, year2, month2, day2);

        // b. If sign is 0, return ! CreateDateDurationRecord(0, 0, 0, 0).
        if (sign == 0)
            return create_date_duration_record(0, 0, 0, 0);

        // c. Let start be the Record { [[Year]]: y1, [[Month]]: m1, [[Day]]: d1 }.
        auto start = ISODate { .year = year1, .month = month1, .day = day1 };

        // d. Let end be the Record { [[Year]]: y2, [[Month]]: m2, [[Day]]: d2 }.
        auto end = ISODate { .year = year2, .month = month2, .day = day2 };

        // e. Let years be end.[[Year]] - start.[[Year]].
        double years = end.year - start.year;

        // f. Let mid be ! AddISODate(y1, m1, d1, years, 0, 0, 0, "constrain").
        auto mid = MUST(add_iso_date(global_object, year1, month1, day1, years, 0, 0, 0, "constrain"sv));

        // g. Let midSign be -(! CompareISODate(mid.[[Year]], mid.[[Month]], mid.[[Day]], y2, m2, d2)).
        auto mid_sign = -compare_iso_date(mid.year, mid.month, mid.day, year2, month2, day2);

        // h. If midSign is 0, then
        if (mid_sign == 0) {
            // i. If largestUnit is "year", return ! CreateDateDurationRecord(years, 0, 0, 0).
            if (largest_unit == "year"sv)
                return create_date_duration_record(years, 0, 0, 0);

            // ii. Return ! CreateDateDurationRecord(0, years × 12, 0, 0).
            return create_date_duration_record(0, years * 12, 0, 0);
        }

        // i. Let months be end.[[Month]] - start.[[Month]].
        double months = end.month - start.month;

        // j. If midSign is not equal to sign, then
        if (mid_sign != sign) {
            // i. Set years to years - sign.
            years -= sign;

            // ii. Set months to months + sign × 12.
            months += sign * 12;
        }

        // k. Set mid to ! AddISODate(y1, m1, d1, years, months, 0, 0, "constrain").
        mid = MUST(add_iso_date(global_object, year1, month1, day1, years, months, 0, 0, "constrain"sv));

        // l. Set midSign to -(! CompareISODate(mid.[[Year]], mid.[[Month]], mid.[[Day]], y2, m2, d2)).
        mid_sign = -compare_iso_date(mid.year, mid.month, mid.day, year2, month2, day2);

        // m. If midSign is 0, then
        if (mid_sign == 0) {
            // i. If largestUnit is "year", return ! CreateDateDurationRecord(years, months, 0, 0).
            if (largest_unit == "year"sv)
                return create_date_duration_record(years, months, 0, 0);

            // ii. Return ! CreateDateDurationRecord(0, months + years × 12, 0, 0).
            return create_date_duration_record(0, months + years * 12, 0, 0);
        }

        // n. If midSign is not equal to sign, then
        if (mid_sign != sign) {
            // i. Set months to months - sign.
            months -= sign;

            // ii. If months is equal to -sign, then
            if (months == -sign) {
                // 1. Set years to years - sign.
                years -= sign;

                // 2. Set months to 11 × sign.
                months = 11 * sign;
            }

            // iii. Set mid to ! AddISODate(y1, m1, d1, years, months, 0, 0, "constrain").
            mid = MUST(add_iso_date(global_object, year1, month1, day1, years, months, 0, 0, "constrain"sv));
        }

        // o. Let days be 0.
        double days = 0;

        // p. If mid.[[Month]] = end.[[Month]], then
        if (mid.month == end.month) {
            // i. Assert: mid.[[Year]] = end.[[Year]].
            VERIFY(mid.year == end.year);

            // ii. Set days to end.[[Day]] - mid.[[Day]].
            days = end.day - mid.day;
        }
        // q. Else if sign < 0, set days to -mid.[[Day]] - (! ISODaysInMonth(end.[[Year]], end.[[Month]]) - end.[[Day]]).
        else if (sign < 0) {
            days = -mid.day - (iso_days_in_month(end.year, end.month) - end.day);
        }
        // r. Else, set days to end.[[Day]] + (! ISODaysInMonth(mid.[[Year]], mid.[[Month]]) - mid.[[Day]]).
        else {
            days = end.day + (iso_days_in_month(mid.year, mid.month) - mid.day);
        }

        // s. If largestUnit is "month", then
        if (largest_unit == "month"sv) {
            // i. Set months to months + years × 12.
            months += years * 12;

            // ii. Set years to 0.
            years = 0;
        }

        // t. Return ! CreateDateDurationRecord(years, months, 0, days).
        return create_date_duration_record(years, months, 0, days);
    }
    // 2. If largestUnit is "day" or "week", then
    else {
        // a. Let epochDays1 be MakeDay(𝔽(y1), 𝔽(m1 - 1), 𝔽(d1)).
        auto epoch_days_1 = make_day(year1, month1 - 1, day1);

        // b. Assert: epochDays1 is finite.
        VERIFY(isfinite(epoch_days_1));

        // c. Let epochDays2 be MakeDay(𝔽(y2), 𝔽(m2 - 1), 𝔽(d2)).
        auto epoch_days_2 = make_day(year2, month2 - 1, day2);

        // d. Assert: epochDays2 is finite.
        VERIFY(isfinite(epoch_days_2));

        // e. Let days be ℝ(epochDays2) - ℝ(epochDays1).
        auto days = epoch_days_2 - epoch_days_1;

        // f. Let weeks be 0.
        double weeks = 0;

        // g. If largestUnit is "week", then
        if (largest_unit == "week"sv) {
            // i. Set weeks to RoundTowardsZero(days / 7).
            weeks = trunc(days / 7);

            // ii. Set days to remainder(days, 7).
            days = fmod(days, 7);
        }

        // h. Return ! CreateDateDurationRecord(0, 0, weeks, days).
        return create_date_duration_record(0, 0, weeks, days);
    }
    VERIFY_NOT_REACHED();
}

// 3.5.4 RegulateISODate ( year, month, day, overflow ), https://tc39.es/proposal-temporal/#sec-temporal-regulateisodate
ThrowCompletionOr<ISODate> regulate_iso_date(GlobalObject& global_object, double year, double month, double day, StringView overflow)
{
    auto& vm = global_object.vm();
    // 1. Assert: year, month, and day are integers.
    VERIFY(year == trunc(year) && month == trunc(month) && day == trunc(day));
    // 2. Assert: overflow is either "constrain" or "reject".
    // NOTE: Asserted by the VERIFY_NOT_REACHED at the end

    // 3. If overflow is "reject", then
    if (overflow == "reject"sv) {
        // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
        // This does not change the exposed behavior as the call to IsValidISODate will immediately check that these values are valid ISO
        // values (for years: -273975 - 273975, for months: 1 - 12, for days: 1 - 31) all of which are subsets of this check.
        if (!AK::is_within_range<i32>(year) || !AK::is_within_range<u8>(month) || !AK::is_within_range<u8>(day))
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);

        auto y = static_cast<i32>(year);
        auto m = static_cast<u8>(month);
        auto d = static_cast<u8>(day);
        // a. If IsValidISODate(year, month, day) is false, throw a RangeError exception.
        if (!is_valid_iso_date(y, m, d))
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);

        // b. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day }.
        return ISODate { .year = y, .month = m, .day = d };
    }
    // 4. If overflow is "constrain", then
    else if (overflow == "constrain"sv) {
        // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat this double as normal integer from this point onwards. This
        // does not change the exposed behavior as the parent's call to CreateTemporalDate will immediately check that this value is a valid
        // ISO value for years: -273975 - 273975, which is a subset of this check.
        if (!AK::is_within_range<i32>(year))
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);

        auto y = static_cast<i32>(year);

        // a. Set month to the result of clamping month between 1 and 12.
        month = clamp(month, 1, 12);

        // b. Let daysInMonth be ! ISODaysInMonth(year, month).
        auto days_in_month = iso_days_in_month(y, (u8)month);

        // c. Set day to the result of clamping day between 1 and daysInMonth.
        day = clamp(day, 1, days_in_month);

        // d. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day }.
        return ISODate { .year = y, .month = static_cast<u8>(month), .day = static_cast<u8>(day) };
    }
    VERIFY_NOT_REACHED();
}

// 3.5.5 IsValidISODate ( year, month, day ), https://tc39.es/proposal-temporal/#sec-temporal-isvalidisodate
bool is_valid_iso_date(i32 year, u8 month, u8 day)
{
    // 1. If month < 1 or month > 12, then
    if (month < 1 || month > 12) {
        // a. Return false.
        return false;
    }

    // 2. Let daysInMonth be ! ISODaysInMonth(year, month).
    auto days_in_month = iso_days_in_month(year, month);

    // 3. If day < 1 or day > daysInMonth, then
    if (day < 1 || day > days_in_month) {
        // a. Return false.
        return false;
    }

    // 4. Return true.
    return true;
}

// 3.5.6 BalanceISODate ( year, month, day ), https://tc39.es/proposal-temporal/#sec-temporal-balanceisodate
ISODate balance_iso_date(double year, double month, double day)
{
    // 1. Let epochDays be MakeDay(𝔽(year), 𝔽(month - 1), 𝔽(day)).
    auto epoch_days = make_day(year, month - 1, day);

    // 2. Assert: epochDays is finite.
    VERIFY(isfinite(epoch_days));

    // 3. Let ms be MakeDate(epochDays, +0𝔽).
    auto ms = make_date(epoch_days, 0);

    // 4. Return the Record { [[Year]]: ℝ(YearFromTime(ms)), [[Month]]: ℝ(MonthFromTime(ms)) + 1, [[Day]]: ℝ(DateFromTime(ms)) }.
    return { .year = year_from_time(ms), .month = static_cast<u8>(month_from_time(ms) + 1), .day = date_from_time(ms) };
}

// 3.5.7 PadISOYear ( y ), https://tc39.es/proposal-temporal/#sec-temporal-padisoyear
String pad_iso_year(i32 y)
{
    // 1. Assert: y is an integer.

    // 2. If y ≥ 0 and y ≤ 9999, then
    if (y >= 0 && y <= 9999) {
        // a. Return ToZeroPaddedDecimalString(y, 4).
        return String::formatted("{:04}", y);
    }

    // 3. If y > 0, let yearSign be "+"; otherwise, let yearSign be "-".
    auto year_sign = y > 0 ? '+' : '-';

    // 4. Let year be ToZeroPaddedDecimalString(abs(y), 6).
    // 5. Return the string-concatenation of yearSign and year.
    return String::formatted("{}{:06}", year_sign, abs(y));
}

// 3.5.8 TemporalDateToString ( temporalDate, showCalendar ), https://tc39.es/proposal-temporal/#sec-temporal-temporaldatetostring
ThrowCompletionOr<String> temporal_date_to_string(GlobalObject& global_object, PlainDate& temporal_date, StringView show_calendar)
{
    // 1. Assert: Type(temporalDate) is Object.
    // 2. Assert: temporalDate has an [[InitializedTemporalDate]] internal slot.

    // 3. Let year be ! PadISOYear(temporalDate.[[ISOYear]]).
    auto year = pad_iso_year(temporal_date.iso_year());

    // 4. Let month be ToZeroPaddedDecimalString(monthDay.[[ISOMonth]], 2).
    auto month = String::formatted("{:02}", temporal_date.iso_month());

    // 5. Let day be ToZeroPaddedDecimalString(monthDay.[[ISODay]], 2).
    auto day = String::formatted("{:02}", temporal_date.iso_day());

    // 6. Let calendarID be ? ToString(temporalDate.[[Calendar]]).
    auto calendar_id = TRY(Value(&temporal_date.calendar()).to_string(global_object));

    // 7. Let calendar be ! FormatCalendarAnnotation(calendarID, showCalendar).
    auto calendar = format_calendar_annotation(calendar_id, show_calendar);

    // 8. Return the string-concatenation of year, the code unit 0x002D (HYPHEN-MINUS), month, the code unit 0x002D (HYPHEN-MINUS), day, and calendar.
    return String::formatted("{}-{}-{}{}", year, month, day, calendar);
}

// 3.5.9 AddISODate ( year, month, day, years, months, weeks, days, overflow ), https://tc39.es/proposal-temporal/#sec-temporal-addisodate
ThrowCompletionOr<ISODate> add_iso_date(GlobalObject& global_object, i32 year, u8 month, u8 day, double years, double months, double weeks, double days, StringView overflow)
{
    // 1. Assert: year, month, day, years, months, weeks, and days are integers.
    VERIFY(years == trunc(years) && months == trunc(months) && weeks == trunc(weeks) && days == trunc(days));

    // 2. Assert: overflow is either "constrain" or "reject".
    VERIFY(overflow == "constrain"sv || overflow == "reject"sv);

    // 3. Let intermediate be ! BalanceISOYearMonth(year + years, month + months).
    auto intermediate_year_month = balance_iso_year_month(year + years, month + months);

    // 4. Let intermediate be ? RegulateISODate(intermediate.[[Year]], intermediate.[[Month]], day, overflow).
    auto intermediate_date = TRY(regulate_iso_date(global_object, intermediate_year_month.year, intermediate_year_month.month, day, overflow));

    // 5. Set days to days + 7 × weeks.
    days += 7 * weeks;

    // 6. Let d be intermediate.[[Day]] + days.
    auto d = intermediate_date.day + days;

    // 7. Let intermediate be BalanceISODate(intermediate.[[Year]], intermediate.[[Month]], d).
    auto intermediate = balance_iso_date(intermediate_date.year, intermediate_date.month, d);

    // 8. Return ? RegulateISODate(intermediate.[[Year]], intermediate.[[Month]], intermediate.[[Day]], overflow).
    return regulate_iso_date(global_object, intermediate.year, intermediate.month, intermediate.day, overflow);
}

// 3.5.10 CompareISODate ( y1, m1, d1, y2, m2, d2 ), https://tc39.es/proposal-temporal/#sec-temporal-compareisodate
i8 compare_iso_date(i32 year1, u8 month1, u8 day1, i32 year2, u8 month2, u8 day2)
{
    // 1. Assert: y1, m1, d1, y2, m2, and d2 are integers.

    // 2. If y1 > y2, return 1.
    if (year1 > year2)
        return 1;

    // 3. If y1 < y2, return -1.
    if (year1 < year2)
        return -1;

    // 4. If m1 > m2, return 1.
    if (month1 > month2)
        return 1;

    // 5. If m1 < m2, return -1.
    if (month1 < month2)
        return -1;

    // 6. If d1 > d2, return 1.
    if (day1 > day2)
        return 1;

    // 7. If d1 < d2, return -1.
    if (day1 < day2)
        return -1;

    // 8. Return 0.
    return 0;
}

// 3.5.11 DifferenceTemporalPlainDate ( operation, temporalDate, other, options ), https://tc39.es/proposal-temporal/#sec-temporal-differencetemporalplaindate
ThrowCompletionOr<Duration*> difference_temporal_plain_date(GlobalObject& global_object, DifferenceOperation operation, PlainDate& temporal_date, Value other_value, Value options_value)
{
    auto& vm = global_object.vm();

    // 1. If operation is since, let sign be -1. Otherwise, let sign be 1.
    i8 sign = operation == DifferenceOperation::Since ? -1 : 1;

    // 2. Set other to ? ToTemporalDate(other).
    auto* other = TRY(to_temporal_date(global_object, other_value));

    // 3. If ? CalendarEquals(temporalDate.[[Calendar]], other.[[Calendar]]) is false, throw a RangeError exception.
    if (!TRY(calendar_equals(global_object, temporal_date.calendar(), other->calendar())))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalDifferentCalendars);

    // 4. Set options to ? GetOptionsObject(options).
    auto const* options = TRY(get_options_object(global_object, options_value));

    // 5. Let disallowedUnits be « "hour", "minute", "second", "millisecond", "microsecond", "nanosecond" ».
    auto disallowed_units = Vector<StringView> { "hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv };

    // 6. Let smallestUnit be ? ToSmallestTemporalUnit(options, disallowedUnits, "day").
    auto smallest_unit = TRY(to_smallest_temporal_unit(global_object, *options, disallowed_units, "day"sv));

    // 7. Let defaultLargestUnit be ! LargerOfTwoTemporalUnits("day", smallestUnit).
    auto default_largest_unit = larger_of_two_temporal_units("day"sv, *smallest_unit);

    // 8. Let largestUnit be ? ToLargestTemporalUnit(options, disallowedUnits, "auto", defaultLargestUnit).
    auto largest_unit = TRY(to_largest_temporal_unit(global_object, *options, disallowed_units, "auto"sv, default_largest_unit));

    // 9. Perform ? ValidateTemporalUnitRange(largestUnit, smallestUnit).
    TRY(validate_temporal_unit_range(global_object, *largest_unit, *smallest_unit));

    // 10. Let roundingMode be ? ToTemporalRoundingMode(options, "trunc").
    auto rounding_mode = TRY(to_temporal_rounding_mode(global_object, *options, "trunc"sv));

    // 11. If operation is since, then
    if (operation == DifferenceOperation::Since) {
        // a. Set roundingMode to ! NegateTemporalRoundingMode(roundingMode).
        rounding_mode = negate_temporal_rounding_mode(rounding_mode);
    }

    // 12. Let roundingIncrement be ? ToTemporalRoundingIncrement(options, undefined, false).
    auto rounding_increment = TRY(to_temporal_rounding_increment(global_object, *options, {}, false));

    // 13. Let untilOptions be ? MergeLargestUnitOption(options, largestUnit).
    auto* until_options = TRY(merge_largest_unit_option(global_object, options, largest_unit.release_value()));

    // 14. Let result be ? CalendarDateUntil(temporalDate.[[Calendar]], temporalDate, other, untilOptions).
    auto* duration = TRY(calendar_date_until(global_object, temporal_date.calendar(), &temporal_date, other, *until_options));

    auto result = DurationRecord { duration->years(), duration->months(), duration->weeks(), duration->days(), 0, 0, 0, 0, 0, 0 };

    // 15. If smallestUnit is not "day" or roundingIncrement ≠ 1, then
    if (*smallest_unit != "day"sv || rounding_increment != 1) {
        // a. Set result to (? RoundDuration(result.[[Years]], result.[[Months]], result.[[Weeks]], result.[[Days]], 0, 0, 0, 0, 0, 0, roundingIncrement, smallestUnit, roundingMode, temporalDate)).[[DurationRecord]].
        result = TRY(round_duration(global_object, result.years, result.months, result.weeks, result.days, 0, 0, 0, 0, 0, 0, rounding_increment, *smallest_unit, rounding_mode, &temporal_date)).duration_record;
    }

    // 16. Return ! CreateTemporalDuration(sign × result.[[Years]], sign × result.[[Months]], sign × result.[[Weeks]], sign × result.[[Days]], 0, 0, 0, 0, 0, 0).
    return TRY(create_temporal_duration(global_object, sign * result.years, sign * result.months, sign * result.weeks, sign * result.days, 0, 0, 0, 0, 0, 0));
}

}
