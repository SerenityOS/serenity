/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>
#include <LibJS/Runtime/Temporal/PlainYearMonthConstructor.h>

namespace JS::Temporal {

// 9 Temporal.PlainYearMonth Objects, https://tc39.es/proposal-temporal/#sec-temporal-plainyearmonth-objects
PlainYearMonth::PlainYearMonth(i32 iso_year, u8 iso_month, u8 iso_day, Object& calendar, Object& prototype)
    : Object(prototype)
    , m_iso_year(iso_year)
    , m_iso_month(iso_month)
    , m_iso_day(iso_day)
    , m_calendar(calendar)
{
}

void PlainYearMonth::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_calendar);
}

// 9.5.1 ToTemporalYearMonth ( item [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalyearmonth
ThrowCompletionOr<PlainYearMonth*> to_temporal_year_month(GlobalObject& global_object, Value item, Object* options)
{
    auto& vm = global_object.vm();

    // 1. If options is not present, set options to ! OrdinaryObjectCreate(null).
    if (!options)
        options = Object::create(global_object, nullptr);

    // 2. Assert: Type(options) is Object.

    // 3. If Type(item) is Object, then
    if (item.is_object()) {
        auto& item_object = item.as_object();

        // a. If item has an [[InitializedTemporalYearMonth]] internal slot, then
        if (is<PlainYearMonth>(item_object)) {
            // i. Return item.
            return static_cast<PlainYearMonth*>(&item_object);
        }

        // b. Let calendar be ? GetTemporalCalendarWithISODefault(item).
        auto* calendar = TRY(get_temporal_calendar_with_iso_default(global_object, item_object));

        // c. Let fieldNames be ? CalendarFields(calendar, « "month", "monthCode", "year" »).
        auto field_names = TRY(calendar_fields(global_object, *calendar, { "month"sv, "monthCode"sv, "year"sv }));

        // d. Let fields be ? PrepareTemporalFields(item, fieldNames, «»).
        auto* fields = TRY(prepare_temporal_fields(global_object, item_object, field_names, {}));

        // e. Return ? YearMonthFromFields(calendar, fields, options).
        return year_month_from_fields(global_object, *calendar, *fields, options);
    }

    // 4. Perform ? ToTemporalOverflow(options).
    (void)TRY(to_temporal_overflow(global_object, *options));

    // 5. Let string be ? ToString(item).
    auto string = TRY(item.to_string(global_object));

    // 6. Let result be ? ParseTemporalYearMonthString(string).
    auto result = TRY(parse_temporal_year_month_string(global_object, string));

    // 7. Let calendar be ? ToTemporalCalendarWithISODefault(result.[[Calendar]]).
    auto* calendar = TRY(to_temporal_calendar_with_iso_default(global_object, result.calendar.has_value() ? js_string(vm, *result.calendar) : js_undefined()));

    // 8. Set result to ? CreateTemporalYearMonth(result.[[Year]], result.[[Month]], calendar, result.[[Day]]).
    auto* creation_result = TRY(create_temporal_year_month(global_object, result.year, result.month, *calendar, result.day));

    // 9. Let canonicalYearMonthOptions be ! OrdinaryObjectCreate(null).
    auto* canonical_year_month_options = Object::create(global_object, nullptr);

    // 10. Return ? YearMonthFromFields(calendar, result, canonicalYearMonthOptions).
    return year_month_from_fields(global_object, *calendar, *creation_result, canonical_year_month_options);
}

// 9.5.2 RegulateISOYearMonth ( year, month, overflow ), https://tc39.es/proposal-temporal/#sec-temporal-regulateisoyearmonth
ThrowCompletionOr<ISOYearMonth> regulate_iso_year_month(GlobalObject& global_object, double year, double month, StringView overflow)
{
    auto& vm = global_object.vm();

    // 1. Assert: year and month are integers.
    VERIFY(year == trunc(year) && month == trunc(month));

    // 2. Assert: overflow is either "constrain" or "reject".
    // NOTE: Asserted by the VERIFY_NOT_REACHED at the end

    // 3. If overflow is "constrain", then
    if (overflow == "constrain"sv) {
        // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat `year` (a double) as normal integer from this point onwards.
        // This does not change the exposed behavior as the subsequent call to CreateTemporalYearMonth will check that its value is a valid ISO
        // values (for years: -273975 - 273975) which is a subset of this check.
        // If RegulateISOYearMonth is ever used outside ISOYearMonthFromFields, this may need to be changed.
        if (!AK::is_within_range<i32>(year))
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);

        // a. Return ! ConstrainISOYearMonth(year, month).
        return constrain_iso_year_month(year, month);
    }

    // 4. If overflow is "reject", then
    if (overflow == "reject"sv) {
        // IMPLEMENTATION DEFINED: This is an optimization that allows us to treat these doubles as normal integers from this point onwards.
        // This does not change the exposed behavior as the call to IsValidISOMonth and subsequent call to CreateTemporalDateTime will check
        // that these values are valid ISO values (for years: -273975 - 273975, for months: 1 - 12) all of which are subsets of this check.
        if (!AK::is_within_range<i32>(year) || !AK::is_within_range<u8>(month))
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);

        // a. If ! IsValidISOMonth(month) is false, throw a RangeError exception.
        if (!is_valid_iso_month(month))
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);

        // b. Return the Record { [[Year]]: year, [[Month]]: month }.
        return ISOYearMonth { .year = static_cast<i32>(year), .month = static_cast<u8>(month), .reference_iso_day = 0 };
    }

    VERIFY_NOT_REACHED();
}

// 9.5.3 IsValidISOMonth ( month ), https://tc39.es/proposal-temporal/#sec-temporal-isvalidisomonth
bool is_valid_iso_month(u8 month)
{
    // 1. Assert: month is an integer.
    // 2. If month < 1 or month > 12, then
    if (month < 1 || month > 12) {
        // a.Return false.
        return false;
    }

    // 3. Return true.
    return true;
}

// 9.5.4 ISOYearMonthWithinLimits ( year, month ), https://tc39.es/proposal-temporal/#sec-temporal-isoyearmonthwithinlimits
bool iso_year_month_within_limits(i32 year, u8 month)
{
    // 1. Assert: year and month are integers.

    // 2. If year < −271821 or year > 275760, then
    if (year < -271821 || year > 275760) {
        // a. Return false.
        return false;
    }

    // 3. If year is −271821 and month < 4, then
    if (year == -271821 && month < 4) {
        // a. Return false.
        return false;
    }

    // 4. If year is 275760 and month > 9, then
    if (year == 275760 && month > 9) {
        // a. Return false.
        return false;
    }

    // 5. Return true.
    return true;
}

// 9.5.5 BalanceISOYearMonth ( year, month ), https://tc39.es/proposal-temporal/#sec-temporal-balanceisoyearmonth
ISOYearMonth balance_iso_year_month(double year, double month)
{
    // 1. Assert: year and month are integers.
    VERIFY(year == trunc(year) && month == trunc(month));

    // 2. Set year to year + floor((month - 1) / 12).
    year += floor((month - 1) / 12);

    // 3. Set month to (month − 1) modulo 12 + 1.
    month = modulo(month - 1, 12) + 1;

    // 4. Return the Record { [[Year]]: year, [[Month]]: month }.
    return ISOYearMonth { .year = static_cast<i32>(year), .month = static_cast<u8>(month), .reference_iso_day = 0 };
}

// 9.5.6 ConstrainISOYearMonth ( year, month ), https://tc39.es/proposal-temporal/#sec-temporal-constrainisoyearmonth
ISOYearMonth constrain_iso_year_month(double year, double month)
{
    // 1. Assert: year and month are integers.
    VERIFY(year == trunc(year) && month == trunc(month));

    // 2. Set month to ! ConstrainToRange(month, 1, 12).
    month = constrain_to_range(month, 1, 12);

    // 3. Return the Record { [[Year]]: year, [[Month]]: month }.
    // NOTE: `year` is known to be in the i32 range.
    return ISOYearMonth { .year = static_cast<i32>(year), .month = static_cast<u8>(month), .reference_iso_day = 0 };
}

// 9.5.7 CreateTemporalYearMonth ( isoYear, isoMonth, calendar, referenceISODay [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalyearmonth
ThrowCompletionOr<PlainYearMonth*> create_temporal_year_month(GlobalObject& global_object, i32 iso_year, u8 iso_month, Object& calendar, u8 reference_iso_day, FunctionObject const* new_target)
{
    auto& vm = global_object.vm();

    // 1. Assert: isoYear, isoMonth, and referenceISODay are integers.
    // 2. Assert: Type(calendar) is Object.

    // 3. If ! IsValidISODate(isoYear, isoMonth, referenceISODay) is false, throw a RangeError exception.
    if (!is_valid_iso_date(iso_year, iso_month, reference_iso_day))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);

    // 4. If ! ISOYearMonthWithinLimits(isoYear, isoMonth) is false, throw a RangeError exception.
    if (!iso_year_month_within_limits(iso_year, iso_month))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);

    // 5. If newTarget is not present, set it to %Temporal.PlainYearMonth%.
    if (!new_target)
        new_target = global_object.temporal_plain_year_month_constructor();

    // 6. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.PlainYearMonth.prototype%", « [[InitializedTemporalYearMonth]], [[ISOYear]], [[ISOMonth]], [[ISODay]], [[Calendar]] »).
    // 7. Set object.[[ISOYear]] to isoYear.
    // 8. Set object.[[ISOMonth]] to isoMonth.
    // 9. Set object.[[Calendar]] to calendar.
    // 10. Set object.[[ISODay]] to referenceISODay.
    auto* object = TRY(ordinary_create_from_constructor<PlainYearMonth>(global_object, *new_target, &GlobalObject::temporal_plain_year_month_prototype, iso_year, iso_month, reference_iso_day, calendar));

    // 11. Return object.
    return object;
}

// 9.5.8 TemporalYearMonthToString ( yearMonth, showCalendar ), https://tc39.es/proposal-temporal/#sec-temporal-temporalyearmonthtostring
ThrowCompletionOr<String> temporal_year_month_to_string(GlobalObject& global_object, PlainYearMonth& year_month, StringView show_calendar)
{
    // 1. Assert: Type(yearMonth) is Object.
    // 2. Assert: yearMonth has an [[InitializedTemporalYearMonth]] internal slot.

    // 3. Let year be ! PadISOYear(yearMonth.[[ISOYear]]).
    // 4. Let month be yearMonth.[[ISOMonth]] formatted as a two-digit decimal number, padded to the left with a zero if necessary.
    // 5. Let result be the string-concatenation of year, the code unit 0x002D (HYPHEN-MINUS), and month.
    auto result = String::formatted("{}-{:02}", pad_iso_year(year_month.iso_year()), year_month.iso_month());

    // 6. Let calendarID be ? ToString(yearMonth.[[Calendar]]).
    auto calendar_id = TRY(Value(&year_month.calendar()).to_string(global_object));

    // 7. If calendarID is not "iso8601", then
    if (calendar_id != "iso8601") {
        // a. Let day be yearMonth.[[ISODay]] formatted as a two-digit decimal number, padded to the left with a zero if necessary.
        // b. Set result to the string-concatenation of result, the code unit 0x002D (HYPHEN-MINUS), and day.
        result = String::formatted("{}-{:02}", result, year_month.iso_day());
    }

    // 8. Let calendarString be ! FormatCalendarAnnotation(calendarID, showCalendar).
    auto calendar_string = format_calendar_annotation(calendar_id, show_calendar);

    // 9. Set result to the string-concatenation of result and calendarString.
    // 10. Return result.
    return String::formatted("{}{}", result, calendar_string);
}

}
