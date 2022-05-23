/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
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
ThrowCompletionOr<PlainYearMonth*> to_temporal_year_month(GlobalObject& global_object, Value item, Object const* options)
{
    auto& vm = global_object.vm();

    // 1. If options is not present, set options to undefined.
    // 2. Assert: Type(options) is Object or Undefined.

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

        // e. Return ? CalendarYearMonthFromFields(calendar, fields, options).
        return calendar_year_month_from_fields(global_object, *calendar, *fields, options);
    }

    // 4. Perform ? ToTemporalOverflow(options).
    (void)TRY(to_temporal_overflow(global_object, options));

    // 5. Let string be ? ToString(item).
    auto string = TRY(item.to_string(global_object));

    // 6. Let result be ? ParseTemporalYearMonthString(string).
    auto result = TRY(parse_temporal_year_month_string(global_object, string));

    // 7. Let calendar be ? ToTemporalCalendarWithISODefault(result.[[Calendar]]).
    auto* calendar = TRY(to_temporal_calendar_with_iso_default(global_object, result.calendar.has_value() ? js_string(vm, *result.calendar) : js_undefined()));

    // 8. Set result to ? CreateTemporalYearMonth(result.[[Year]], result.[[Month]], calendar, result.[[Day]]).
    auto* creation_result = TRY(create_temporal_year_month(global_object, result.year, result.month, *calendar, result.day));

    // 9. NOTE: The following operation is called without options, in order for the calendar to store a canonical value in the [[ISODay]] internal slot of the result.
    // 10. Return ? CalendarYearMonthFromFields(calendar, result).
    return calendar_year_month_from_fields(global_object, *calendar, *creation_result);
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

    // 2. If year < -271821 or year > 275760, then
    if (year < -271821 || year > 275760) {
        // a. Return false.
        return false;
    }

    // 3. If year is -271821 and month < 4, then
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

    // 3. Set month to (month - 1) modulo 12 + 1.
    month = modulo(month - 1, 12) + 1;

    // 4. Return the Record { [[Year]]: year, [[Month]]: month }.
    return ISOYearMonth { .year = static_cast<i32>(year), .month = static_cast<u8>(month), .reference_iso_day = 0 };
}

// 9.5.6 ConstrainISOYearMonth ( year, month ), https://tc39.es/proposal-temporal/#sec-temporal-constrainisoyearmonth
ISOYearMonth constrain_iso_year_month(double year, double month)
{
    // 1. Assert: year and month are integers.
    VERIFY(year == trunc(year) && month == trunc(month));

    // 2. Set month to the result of clamping month between 1 and 12.
    month = clamp(month, 1, 12);

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

    // 3. If IsValidISODate(isoYear, isoMonth, referenceISODay) is false, throw a RangeError exception.
    if (!is_valid_iso_date(iso_year, iso_month, reference_iso_day))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);

    // 4. If ! ISOYearMonthWithinLimits(isoYear, isoMonth) is false, throw a RangeError exception.
    if (!iso_year_month_within_limits(iso_year, iso_month))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidPlainYearMonth);

    // 5. If newTarget is not present, set newTarget to %Temporal.PlainYearMonth%.
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
    // 4. Let month be ToZeroPaddedDecimalString(yearMonth.[[ISOMonth]], 2).
    // 5. Let result be the string-concatenation of year, the code unit 0x002D (HYPHEN-MINUS), and month.
    auto result = String::formatted("{}-{:02}", pad_iso_year(year_month.iso_year()), year_month.iso_month());

    // 6. Let calendarID be ? ToString(yearMonth.[[Calendar]]).
    auto calendar_id = TRY(Value(&year_month.calendar()).to_string(global_object));

    // 7. If showCalendar is "always" or if calendarID is not "iso8601", then
    if (show_calendar == "always"sv || calendar_id != "iso8601") {
        // a. Let day be ToZeroPaddedDecimalString(yearMonth.[[ISODay]], 2).
        // b. Set result to the string-concatenation of result, the code unit 0x002D (HYPHEN-MINUS), and day.
        result = String::formatted("{}-{:02}", result, year_month.iso_day());
    }

    // 8. Let calendarString be ! FormatCalendarAnnotation(calendarID, showCalendar).
    auto calendar_string = format_calendar_annotation(calendar_id, show_calendar);

    // 9. Set result to the string-concatenation of result and calendarString.
    // 10. Return result.
    return String::formatted("{}{}", result, calendar_string);
}

// 9.5.9 DifferenceTemporalPlainYearMonth ( operation, yearMonth, other, options ), https://tc39.es/proposal-temporal/#sec-temporal-differencetemporalplainyearmonth
ThrowCompletionOr<Duration*> difference_temporal_plain_year_month(GlobalObject& global_object, DifferenceOperation operation, PlainYearMonth& year_month, Value other_value, Value options_value)
{
    auto& vm = global_object.vm();

    // 1. If operation is since, let sign be -1. Otherwise, let sign be 1.
    i8 sign = operation == DifferenceOperation::Since ? -1 : 1;

    // 2. Set other to ? ToTemporalYearMonth(other).
    auto* other = TRY(to_temporal_year_month(global_object, other_value));

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month.calendar();

    // 4. If ? CalendarEquals(calendar, other.[[Calendar]]) is false, throw a RangeError exception.
    if (!TRY(calendar_equals(global_object, calendar, other->calendar())))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalDifferentCalendars);

    // 5. Set options to ? GetOptionsObject(options).
    auto const* options = TRY(get_options_object(global_object, options_value));

    // 6. Let disallowedUnits be « "week", "day", "hour", "minute", "second", "millisecond", "microsecond", "nanosecond" ».
    auto disallowed_units = Vector<StringView> { "week"sv, "day"sv, "hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv };

    // 7. Let smallestUnit be ? ToSmallestTemporalUnit(options, disallowedUnits, "month").
    auto smallest_unit = TRY(to_smallest_temporal_unit(global_object, *options, disallowed_units, "month"sv));

    // 8. Let largestUnit be ? ToLargestTemporalUnit(options, disallowedUnits, "auto", "year").
    auto largest_unit = TRY(to_largest_temporal_unit(global_object, *options, disallowed_units, "auto"sv, "year"sv));

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

    // 13. Let fieldNames be ? CalendarFields(calendar, « "monthCode", "year" »).
    auto field_names = TRY(calendar_fields(global_object, calendar, { "monthCode"sv, "year"sv }));

    // 14. Let otherFields be ? PrepareTemporalFields(other, fieldNames, «»).
    auto* other_fields = TRY(prepare_temporal_fields(global_object, *other, field_names, {}));

    // 15. Perform ! CreateDataPropertyOrThrow(otherFields, "day", 1𝔽).
    MUST(other_fields->create_data_property_or_throw(vm.names.day, Value(1)));

    // 16. Let otherDate be ? CalendarDateFromFields(calendar, otherFields).
    auto* other_date = TRY(calendar_date_from_fields(global_object, calendar, *other_fields));

    // 17. Let thisFields be ? PrepareTemporalFields(yearMonth, fieldNames, «»).
    auto* this_fields = TRY(prepare_temporal_fields(global_object, year_month, field_names, {}));

    // 18. Perform ! CreateDataPropertyOrThrow(thisFields, "day", 1𝔽).
    MUST(this_fields->create_data_property_or_throw(vm.names.day, Value(1)));

    // 19. Let thisDate be ? CalendarDateFromFields(calendar, thisFields).
    auto* this_date = TRY(calendar_date_from_fields(global_object, calendar, *this_fields));

    // 20. Let untilOptions be ? MergeLargestUnitOption(options, largestUnit).
    auto* until_options = TRY(merge_largest_unit_option(global_object, options, *largest_unit));

    // 21. Let result be ? CalendarDateUntil(calendar, thisDate, otherDate, untilOptions).
    auto* duration = TRY(calendar_date_until(global_object, calendar, this_date, other_date, *until_options));

    auto result = DurationRecord { duration->years(), duration->months(), 0, 0, 0, 0, 0, 0, 0, 0 };

    // 22. If smallestUnit is not "month" or roundingIncrement ≠ 1, then
    if (smallest_unit != "month"sv || rounding_increment != 1) {
        // a. Set result to (? RoundDuration(result.[[Years]], result.[[Months]], 0, 0, 0, 0, 0, 0, 0, 0, roundingIncrement, smallestUnit, roundingMode, thisDate)).[[DurationRecord]].
        result = TRY(round_duration(global_object, result.years, result.months, 0, 0, 0, 0, 0, 0, 0, 0, rounding_increment, *smallest_unit, rounding_mode, this_date)).duration_record;
    }

    // 23. Return ! CreateTemporalDuration(sign × result.[[Years]], sign × result.[[Months]], 0, 0, 0, 0, 0, 0, 0, 0).
    return MUST(create_temporal_duration(global_object, sign * result.years, sign * result.months, 0, 0, 0, 0, 0, 0, 0, 0));
}

// 9.5.10 AddDurationToOrSubtractDurationFromPlainYearMonth ( operation, yearMonth, temporalDurationLike, options ), https://tc39.es/proposal-temporal/#sec-temporal-addtemporalplainyearmonth
ThrowCompletionOr<PlainYearMonth*> add_duration_to_or_subtract_duration_from_plain_year_month(GlobalObject& global_object, ArithmeticOperation operation, PlainYearMonth& year_month, Value temporal_duration_like, Value options_value)
{
    auto& vm = global_object.vm();

    // 1. Let duration be ? ToTemporalDurationRecord(temporalDurationLike).
    auto duration = TRY(to_temporal_duration_record(global_object, temporal_duration_like));

    // 2. If operation is subtract, then
    if (operation == ArithmeticOperation::Subtract) {
        // a. Set duration to ! CreateNegatedTemporalDuration(duration).
        // FIXME: According to the spec CreateNegatedTemporalDuration takes a Temporal.Duration object,
        //        not a record, so we have to do some trickery. If they want to accept anything that has
        //        the required internal slots, this should be updated in the AO's description.
        //        We also have to convert back to a Duration Record afterwards to match the initial type.
        auto* actual_duration = MUST(create_temporal_duration(global_object, duration.years, duration.months, duration.weeks, duration.days, duration.hours, duration.minutes, duration.seconds, duration.milliseconds, duration.microseconds, duration.nanoseconds));
        auto* negated_duration = create_negated_temporal_duration(global_object, *actual_duration);
        duration = MUST(to_temporal_duration_record(global_object, negated_duration));
    }

    // 3. Let balanceResult be ? BalanceDuration(duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]], "day").
    auto balance_result = TRY(balance_duration(global_object, duration.days, duration.hours, duration.minutes, duration.seconds, duration.milliseconds, duration.microseconds, Crypto::SignedBigInteger::create_from((i64)duration.nanoseconds), "day"sv));

    // 4. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, options_value));

    // 5. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month.calendar();

    // 6. Let fieldNames be ? CalendarFields(calendar, « "monthCode", "year" »).
    auto field_names = TRY(calendar_fields(global_object, calendar, { "monthCode"sv, "year"sv }));

    // 7. Let fields be ? PrepareTemporalFields(yearMonth, fieldNames, «»).
    auto* fields = TRY(prepare_temporal_fields(global_object, year_month, field_names, {}));

    // 8. Set sign to ! DurationSign(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], balanceResult.[[Days]], 0, 0, 0, 0, 0, 0).
    auto sign = duration_sign(duration.years, duration.months, duration.weeks, balance_result.days, 0, 0, 0, 0, 0, 0);

    double day;

    // 9. If sign < 0, then
    if (sign < 0) {
        // a. Let dayFromCalendar be ? CalendarDaysInMonth(calendar, yearMonth).
        auto day_from_calendar = TRY(calendar_days_in_month(global_object, calendar, year_month));

        // b. Let day be ? ToPositiveInteger(dayFromCalendar).
        day = TRY(to_positive_integer(global_object, day_from_calendar));
    }
    // 10. Else,
    else {
        // a. Let day be 1.
        day = 1;
    }

    // 11. Perform ! CreateDataPropertyOrThrow(fields, "day", day).
    MUST(fields->create_data_property_or_throw(vm.names.day, Value(day)));

    // 12. Let date be ? CalendarDateFromFields(calendar, fields, undefined).
    auto* date = TRY(calendar_date_from_fields(global_object, calendar, *fields, nullptr));

    // 13. Let durationToAdd be ! CreateTemporalDuration(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], balanceResult.[[Days]], 0, 0, 0, 0, 0, 0).
    auto* duration_to_add = MUST(create_temporal_duration(global_object, duration.years, duration.months, duration.weeks, balance_result.days, 0, 0, 0, 0, 0, 0));

    // 14. Let optionsCopy be OrdinaryObjectCreate(%Object.prototype%).
    auto* options_copy = Object::create(global_object, global_object.object_prototype());

    // 15. Let entries be ? EnumerableOwnPropertyNames(options, key+value).
    auto entries = TRY(options->enumerable_own_property_names(Object::PropertyKind::KeyAndValue));

    // 16. For each element nextEntry of entries, do
    for (auto& next_entry : entries) {
        auto key = MUST(next_entry.as_array().get_without_side_effects(0).to_property_key(global_object));
        auto value = next_entry.as_array().get_without_side_effects(1);

        // a. Perform ! CreateDataPropertyOrThrow(optionsCopy, nextEntry[0], nextEntry[1]).
        MUST(options_copy->create_data_property_or_throw(key, value));
    }

    // 17. Let addedDate be ? CalendarDateAdd(calendar, date, durationToAdd, options).
    auto* added_date = TRY(calendar_date_add(global_object, calendar, date, *duration_to_add, options));

    // 18. Let addedDateFields be ? PrepareTemporalFields(addedDate, fieldNames, «»).
    auto* added_date_fields = TRY(prepare_temporal_fields(global_object, *added_date, field_names, {}));

    // 19. Return ? CalendarYearMonthFromFields(calendar, addedDateFields, optionsCopy).
    return calendar_year_month_from_fields(global_object, calendar, *added_date_fields, options_copy);
}

}
