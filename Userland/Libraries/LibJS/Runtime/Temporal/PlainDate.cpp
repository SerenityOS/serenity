/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
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
PlainDate* create_temporal_date(GlobalObject& global_object, i32 iso_year, u8 iso_month, u8 iso_day, Object& calendar, FunctionObject const* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: isoYear is an integer.
    // 2. Assert: isoMonth is an integer.
    // 3. Assert: isoDay is an integer.
    // 4. Assert: Type(calendar) is Object.

    // 5. If ! IsValidISODate(isoYear, isoMonth, isoDay) is false, throw a RangeError exception.
    if (!is_valid_iso_date(iso_year, iso_month, iso_day)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
        return {};
    }

    // 6. If ! ISODateTimeWithinLimits(isoYear, isoMonth, isoDay, 12, 0, 0, 0, 0, 0) is false, throw a RangeError exception.
    if (!iso_date_time_within_limits(global_object, iso_year, iso_month, iso_day, 12, 0, 0, 0, 0, 0)) {
        vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
        return {};
    }

    // 7. If newTarget is not present, set it to %Temporal.PlainDate%.
    if (!new_target)
        new_target = global_object.temporal_plain_date_constructor();

    // 8. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.PlainDate.prototype%", « [[InitializedTemporalDate]], [[ISOYear]], [[ISOMonth]], [[ISODay]], [[Calendar]] »).
    // 9. Set object.[[ISOYear]] to isoYear.
    // 10. Set object.[[ISOMonth]] to isoMonth.
    // 11. Set object.[[ISODay]] to isoDay.
    // 12. Set object.[[Calendar]] to calendar.
    auto* object = ordinary_create_from_constructor<PlainDate>(global_object, *new_target, &GlobalObject::temporal_plain_date_prototype, iso_year, iso_month, iso_day, calendar);
    if (vm.exception())
        return {};

    return object;
}

// 3.5.2 ToTemporalDate ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-totemporaldate
PlainDate* to_temporal_date(GlobalObject& global_object, Value item, Object* options)
{
    auto& vm = global_object.vm();

    // 1. If options is not present, set options to ! OrdinaryObjectCreate(null).
    if (!options)
        options = Object::create(global_object, nullptr);

    // 2. Assert: Type(options) is Object.

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
            auto* instant = create_temporal_instant(global_object, zoned_date_time.nanoseconds());

            // ii. Let plainDateTime be ? BuiltinTimeZoneGetPlainDateTimeFor(item.[[TimeZone]], instant, item.[[Calendar]]).
            auto* plain_date_time = builtin_time_zone_get_plain_date_time_for(global_object, &zoned_date_time.time_zone(), *instant, zoned_date_time.calendar());
            if (vm.exception())
                return {};

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
        auto* calendar = get_temporal_calendar_with_iso_default(global_object, item_object);
        if (vm.exception())
            return {};

        // e. Let fieldNames be ? CalendarFields(calendar, « "day", "month", "monthCode", "year" »).
        auto field_names = calendar_fields(global_object, *calendar, { "day"sv, "month"sv, "monthCode"sv, "year"sv });
        if (vm.exception())
            return {};

        // f. Let fields be ? PrepareTemporalFields(item, fieldNames, «»).
        auto* fields = prepare_temporal_fields(global_object, item_object, field_names, {});
        if (vm.exception())
            return {};

        // g. Return ? DateFromFields(calendar, fields, options).
        return date_from_fields(global_object, *calendar, *fields, *options);
    }

    // 4. Perform ? ToTemporalOverflow(options).
    (void)to_temporal_overflow(global_object, *options);
    if (vm.exception())
        return {};

    // 5. Let string be ? ToString(item).
    auto string = item.to_string(global_object);
    if (vm.exception())
        return {};

    // 6. Let result be ? ParseTemporalDateString(string).
    auto result = parse_temporal_date_string(global_object, string);
    if (vm.exception())
        return {};

    // 7. Assert: ! IsValidISODate(result.[[Year]], result.[[Month]], result.[[Day]]) is true.
    VERIFY(is_valid_iso_date(result->year, result->month, result->day));

    // 8. Let calendar be ? ToTemporalCalendarWithISODefault(result.[[Calendar]]).
    auto calendar = to_temporal_calendar_with_iso_default(global_object, result->calendar.has_value() ? js_string(vm, *result->calendar) : js_undefined());
    if (vm.exception())
        return {};

    // 9. Return ? CreateTemporalDate(result.[[Year]], result.[[Month]], result.[[Day]], calendar).
    return create_temporal_date(global_object, result->year, result->month, result->day, *calendar);
}

// 3.5.4 RegulateISODate ( year, month, day, overflow ), https://tc39.es/proposal-temporal/#sec-temporal-regulateisodate
Optional<ISODate> regulate_iso_date(GlobalObject& global_object, double year, double month, double day, StringView overflow)
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
        if (!AK::is_within_range<i32>(year) || !AK::is_within_range<u8>(month) || !AK::is_within_range<u8>(day)) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
            return {};
        }
        auto y = static_cast<i32>(year);
        auto m = static_cast<u8>(month);
        auto d = static_cast<u8>(day);
        // a. If ! IsValidISODate(year, month, day) is false, throw a RangeError exception.
        if (!is_valid_iso_date(y, m, d)) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
            return {};
        }
        // b. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day }.
        return ISODate { .year = y, .month = m, .day = d };
    }
    // 4. If overflow is "constrain", then
    else if (overflow == "constrain"sv) {
        // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat this double as normal integer from this point onwards. This
        // does not change the exposed behavior as the parent's call to CreateTemporalDate will immediately check that this value is a valid
        // ISO value for years: -273975 - 273975, which is a subset of this check.
        if (!AK::is_within_range<i32>(year)) {
            vm.throw_exception<RangeError>(global_object, ErrorType::TemporalInvalidPlainDate);
            return {};
        }
        auto y = static_cast<i32>(year);

        // a. Set month to ! ConstrainToRange(month, 1, 12).
        month = constrain_to_range(month, 1, 12);
        // b. Set day to ! ConstrainToRange(day, 1, ! ISODaysInMonth(year, month)).
        day = constrain_to_range(day, 1, iso_days_in_month(y, month));

        // c. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day }.
        return ISODate { .year = y, .month = static_cast<u8>(month), .day = static_cast<u8>(day) };
    }
    VERIFY_NOT_REACHED();
}

// 3.5.5 IsValidISODate ( year, month, day ), https://tc39.es/proposal-temporal/#sec-temporal-isvalidisodate
bool is_valid_iso_date(i32 year, u8 month, u8 day)
{
    // 1. Assert: year, month, and day are integers.

    // 2. If month < 1 or month > 12, then
    if (month < 1 || month > 12) {
        // a. Return false.
        return false;
    }

    // 3. Let daysInMonth be ! ISODaysInMonth(year, month).
    auto days_in_month = iso_days_in_month(year, month);

    // 4. If day < 1 or day > daysInMonth, then
    if (day < 1 || day > days_in_month) {
        // a. Return false.
        return false;
    }

    // 5. Return true.
    return true;
}

// 3.5.6 BalanceISODate ( year, month, day ), https://tc39.es/proposal-temporal/#sec-temporal-balanceisodate
ISODate balance_iso_date(double year_, double month_, double day)
{
    // 1. Assert: year, month, and day are integers.

    // 2. Let balancedYearMonth be ! BalanceISOYearMonth(year, month).
    auto balanced_year_month = balance_iso_year_month(year_, month_);

    // 3. Set month to balancedYearMonth.[[Month]].
    auto month = balanced_year_month.month;

    // 4. Set year to balancedYearMonth.[[Year]].
    auto year = balanced_year_month.year;

    // 5. NOTE: To deal with negative numbers of days whose absolute value is greater than the number of days in a year, the following section subtracts years and adds days until the number of days is greater than −366 or −365.

    i32 test_year;

    // 6. If month > 2, then
    if (month > 2) {
        // a. Let testYear be year.
        test_year = year;
    }
    // 7. Else,
    else {
        // a. Let testYear be year − 1.
        test_year = year - 1;
    }

    // 8. Repeat, while day < −1 × ! ISODaysInYear(testYear),
    while (day < -1 * iso_days_in_year(test_year)) {
        // a.Set day to day + !ISODaysInYear(testYear).
        day += iso_days_in_year(test_year);

        // b.Set year to year − 1.
        year--;

        // c.Set testYear to testYear − 1.
        test_year--;
    }

    // 9. NOTE: To deal with numbers of days greater than the number of days in a year, the following section adds years and subtracts days until the number of days is less than 366 or 365.

    // 10. Let testYear be year + 1.
    test_year = year + 1;

    // 11. Repeat, while day > ! ISODaysInYear(testYear),
    while (day > iso_days_in_year(test_year)) {
        // a. Set day to day − ! ISODaysInYear(testYear).
        day -= iso_days_in_year(test_year);

        // b. Set year to year + 1.
        year++;

        // c. Set testYear to testYear + 1.
        test_year++;
    }

    // 12. NOTE: To deal with negative numbers of days whose absolute value is greater than the number of days in the current month, the following section subtracts months and adds days until the number of days is greater than 0.

    // 13. Repeat, while day < 1,
    while (day < 1) {
        // a. Set balancedYearMonth to ! BalanceISOYearMonth(year, month − 1).
        balanced_year_month = balance_iso_year_month(year, month - 1);

        // b. Set year to balancedYearMonth.[[Year]].
        year = balanced_year_month.year;

        // c. Set month to balancedYearMonth.[[Month]].
        month = balanced_year_month.month;

        // d. Set day to day + ! ISODaysInMonth(year, month).
        day += iso_days_in_month(year, month);
    }

    // 14. NOTE: To deal with numbers of days greater than the number of days in the current month, the following section adds months and subtracts days until the number of days is less than the number of days in the month.

    // 15. Repeat, while day > ! ISODaysInMonth(year, month),
    while (day > iso_days_in_month(year, month)) {
        // a. Set day to day − ! ISODaysInMonth(year, month).
        day -= iso_days_in_month(year, month);

        // b. Set balancedYearMonth to ! BalanceISOYearMonth(year, month + 1).
        balanced_year_month = balance_iso_year_month(year, month + 1);

        // c. Set year to balancedYearMonth.[[Year]].
        year = balanced_year_month.year;

        // d. Set month to balancedYearMonth.[[Month]].
        month = balanced_year_month.month;
    }

    // 16. Return the Record { [[Year]]: year, [[Month]]: month, [[Day]]: day }.
    return ISODate { .year = year, .month = static_cast<u8>(month), .day = static_cast<u8>(day) };
}

// 3.5.7 PadISOYear ( y ), https://tc39.es/proposal-temporal/#sec-temporal-padisoyear
String pad_iso_year(i32 y)
{
    // 1. Assert: y is an integer.

    // 2. If y > 999 and y ≤ 9999, then
    if (y > 999 && y <= 9999) {
        // a. Return y formatted as a four-digit decimal number.
        return String::number(y);
    }
    // 3. If y ≥ 0, let yearSign be "+"; otherwise, let yearSign be "-".
    auto year_sign = y >= 0 ? '+' : '-';

    // 4. Let year be abs(y), formatted as a six-digit decimal number, padded to the left with zeroes as necessary.
    // 5. Return the string-concatenation of yearSign and year.
    return String::formatted("{}{:06}", year_sign, abs(y));
}

// 3.5.8 TemporalDateToString ( temporalDate, showCalendar ), https://tc39.es/proposal-temporal/#sec-temporal-temporaldatetostring
Optional<String> temporal_date_to_string(GlobalObject& global_object, PlainDate& temporal_date, StringView show_calendar)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(temporalDate) is Object.
    // 2. Assert: temporalDate has an [[InitializedTemporalDate]] internal slot.

    // 3. Let year be ! PadISOYear(temporalDate.[[ISOYear]]).
    auto year = pad_iso_year(temporal_date.iso_year());

    // 4. Let month be temporalDate.[[ISOMonth]] formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    auto month = String::formatted("{:02}", temporal_date.iso_month());

    // 5. Let day be temporalDate.[[ISODay]] formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    auto day = String::formatted("{:02}", temporal_date.iso_day());

    // 6. Let calendarID be ? ToString(temporalDate.[[Calendar]]).
    auto calendar_id = Value(&temporal_date.calendar()).to_string(global_object);
    if (vm.exception())
        return {};

    // 7. Let calendar be ! FormatCalendarAnnotation(calendarID, showCalendar).
    auto calendar = format_calendar_annotation(calendar_id, show_calendar);

    // 8. Return the string-concatenation of year, the code unit 0x002D (HYPHEN-MINUS), month, the code unit 0x002D (HYPHEN-MINUS), day, and calendar.
    return String::formatted("{}-{}-{}{}", year, month, day, calendar);
}

// 3.5.9 AddISODate ( year, month, day, years, months, weeks, days, overflow ), https://tc39.es/proposal-temporal/#sec-temporal-addisodate
Optional<ISODate> add_iso_date(GlobalObject& global_object, i32 year, u8 month, u8 day, double years, double months, double weeks, double days, StringView overflow)
{
    auto& vm = global_object.vm();

    // 1. Assert: year, month, day, years, months, weeks, and days are integers.
    VERIFY(years == trunc(years) && months == trunc(months) && weeks == trunc(weeks) && days == trunc(days));

    // 2. Assert: overflow is either "constrain" or "reject".
    VERIFY(overflow == "constrain"sv || overflow == "reject"sv);

    // 3. Let intermediate be ! BalanceISOYearMonth(year + years, month + months).
    auto intermediate_year_month = balance_iso_year_month(year + years, month + months);

    // 4. Let intermediate be ? RegulateISODate(intermediate.[[Year]], intermediate.[[Month]], day, overflow).
    auto intermediate_date = regulate_iso_date(global_object, intermediate_year_month.year, intermediate_year_month.month, day, overflow);
    if (vm.exception())
        return {};

    // 5. Set days to days + 7 × weeks.
    days += 7 * weeks;

    // 6. Let d be intermediate.[[Day]] + days.
    auto d = intermediate_date->day + days;

    // 7. Let intermediate be ! BalanceISODate(intermediate.[[Year]], intermediate.[[Month]], d).
    auto intermediate = balance_iso_date(intermediate_date->year, intermediate_date->month, d);

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

}
