/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>
#include <LibJS/Runtime/Temporal/PlainYearMonthPrototype.h>

namespace JS::Temporal {

// 9.3 Properties of the Temporal.PlainYearMonth Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plainyearmonth-prototype-object
PlainYearMonthPrototype::PlainYearMonthPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void PlainYearMonthPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 9.3.2 Temporal.PlainYearMonth.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.PlainYearMonth"), Attribute::Configurable);

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.year, year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.month, month_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.monthCode, month_code_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInYear, days_in_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInMonth, days_in_month_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.monthsInYear, months_in_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.inLeapYear, in_leap_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.era, era_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.eraYear, era_year_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.with, with, 1, attr);
    define_native_function(vm.names.add, add, 1, attr);
    define_native_function(vm.names.subtract, subtract, 1, attr);
    define_native_function(vm.names.until, until, 1, attr);
    define_native_function(vm.names.since, since, 1, attr);
    define_native_function(vm.names.equals, equals, 1, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 0, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
    define_native_function(vm.names.toPlainDate, to_plain_date, 1, attr);
    define_native_function(vm.names.getISOFields, get_iso_fields, 0, attr);
}

// 9.3.3 get Temporal.PlainYearMonth.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::calendar_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Return yearMonth.[[Calendar]].
    return Value(&year_month->calendar());
}

// 9.3.4 get Temporal.PlainYearMonth.prototype.year, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.year
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::year_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return 𝔽(? CalendarYear(calendar, yearMonth)).
    return Value(TRY(calendar_year(global_object, calendar, *year_month)));
}

// 9.3.5 get Temporal.PlainYearMonth.prototype.month, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.month
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::month_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return 𝔽(? CalendarMonth(calendar, yearMonth)).
    return Value(TRY(calendar_month(global_object, calendar, *year_month)));
}

// 9.3.6 get Temporal.PlainYearMonth.prototype.monthCode, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.monthCode
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::month_code_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return ? CalendarMonthCode(calendar, yearMonth).
    return js_string(vm, TRY(calendar_month_code(global_object, calendar, *year_month)));
}

// 9.3.7 get Temporal.PlainYearMonth.prototype.daysInYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.daysinyear
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::days_in_year_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return ? CalendarDaysInYear(calendar, yearMonth).
    return Value(TRY(calendar_days_in_year(global_object, calendar, *year_month)));
}

// 9.3.8 get Temporal.PlainYearMonth.prototype.daysInMonth, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.daysinmonth
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::days_in_month_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return ? CalendarDaysInMonth(calendar, yearMonth).
    return Value(TRY(calendar_days_in_month(global_object, calendar, *year_month)));
}

// 9.3.9 get Temporal.PlainYearMonth.prototype.monthsInYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.monthsinyear
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::months_in_year_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return ? CalendarMonthsInYear(calendar, yearMonth).
    return Value(TRY(calendar_months_in_year(global_object, calendar, *year_month)));
}

// 9.3.10 get Temporal.PlainYearMonth.prototype.inLeapYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.inleapyear
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::in_leap_year_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return ? CalendarInLeapYear(calendar, yearMonth).
    return Value(TRY(calendar_in_leap_year(global_object, calendar, *year_month)));
}

// 15.6.9.2 get Temporal.PlainYearMonth.prototype.era, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.era
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::era_getter)
{
    // 1. Let plainYearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(plainYearMonth, [[InitializedTemporalYearMonth]]).
    auto* plain_year_month = TRY(typed_this_object(global_object));

    // 3. Let calendar be plainYearMonth.[[Calendar]].
    auto& calendar = plain_year_month->calendar();

    // 4. Return ? CalendarEra(calendar, plainYearMonth).
    return TRY(calendar_era(global_object, calendar, *plain_year_month));
}

// 15.6.9.3 get Temporal.PlainYearMonth.prototype.eraYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.erayear
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::era_year_getter)
{
    // 1. Let plainYearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(plainYearMonth, [[InitializedTemporalYearMonth]]).
    auto* plain_year_month = TRY(typed_this_object(global_object));

    // 3. Let calendar be plainYearMonth.[[Calendar]].
    auto& calendar = plain_year_month->calendar();

    // 4. Return ? CalendarEraYear(calendar, plainYearMonth).
    return TRY(calendar_era_year(global_object, calendar, *plain_year_month));
}

// 9.3.11 Temporal.PlainYearMonth.prototype.with ( temporalYearMonthLike [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.with
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::with)
{
    auto temporal_year_month_like = vm.argument(0);

    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. If Type(temporalYearMonthLike) is not Object, then
    if (!temporal_year_month_like.is_object()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, temporal_year_month_like.to_string_without_side_effects());
    }

    // 4. Perform ? RejectObjectWithCalendarOrTimeZone(temporalYearMonthLike).
    TRY(reject_object_with_calendar_or_time_zone(global_object, temporal_year_month_like.as_object()));

    // 5. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 6. Let fieldNames be ? CalendarFields(calendar, « "month", "monthCode", "year" »).
    auto field_names = TRY(calendar_fields(global_object, calendar, { "month"sv, "monthCode"sv, "year"sv }));

    // 7. Let partialYearMonth be ? PreparePartialTemporalFields(temporalYearMonthLike, fieldNames).
    auto* partial_year_month = TRY(prepare_partial_temporal_fields(global_object, temporal_year_month_like.as_object(), field_names));

    // 8. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 9. Let fields be ? PrepareTemporalFields(yearMonth, fieldNames, «»).
    auto* fields = TRY(prepare_temporal_fields(global_object, *year_month, field_names, {}));

    // 10. Set fields to ? CalendarMergeFields(calendar, fields, partialYearMonth).
    fields = TRY(calendar_merge_fields(global_object, calendar, *fields, *partial_year_month));

    // 11. Set fields to ? PrepareTemporalFields(fields, fieldNames, «»).
    fields = TRY(prepare_temporal_fields(global_object, *fields, field_names, {}));

    // 12. Return ? YearMonthFromFields(calendar, fields, options).
    return TRY(year_month_from_fields(global_object, calendar, *fields, options));
}

// 9.3.12 Temporal.PlainYearMonth.prototype.add ( temporalDurationLike [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.add
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::add)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Let duration be ? ToLimitedTemporalDuration(temporalDurationLike, « »).
    auto duration = TRY(to_limited_temporal_duration(global_object, vm.argument(0), {}));

    // 4. Let balanceResult be ? BalanceDuration(duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]], "day").
    auto balance_result = TRY(balance_duration(global_object, duration.days, duration.hours, duration.minutes, duration.seconds, duration.milliseconds, duration.microseconds, *js_bigint(vm, Crypto::SignedBigInteger::create_from((i64)duration.nanoseconds)), "day"sv));

    // 5. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 6. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 7. Let fieldNames be ? CalendarFields(calendar, « "monthCode", "year" »).
    auto field_names = TRY(calendar_fields(global_object, calendar, { "monthCode"sv, "year"sv }));

    // 8. Let sign be ! DurationSign(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], balanceResult.[[Days]], 0, 0, 0, 0, 0, 0).
    auto sign = duration_sign(duration.years, duration.months, duration.weeks, balance_result.days, 0, 0, 0, 0, 0, 0);

    double day;

    // 9. If sign < 0, then
    if (sign < 0) {
        // a. Let dayFromCalendar be ? CalendarDaysInMonth(calendar, yearMonth).
        auto day_from_calendar = TRY(calendar_days_in_month(global_object, calendar, *year_month));

        // b. Let day be ? ToPositiveInteger(dayFromCalendar).
        day = TRY(to_positive_integer(global_object, day_from_calendar));
    }
    // 10. Else,
    else {
        // a. Let day be 1.
        day = 1;
    }

    // 11. Let date be ? CreateTemporalDate(yearMonth.[[ISOYear]], yearMonth.[[ISOMonth]], day, calendar).
    auto* date = TRY(create_temporal_date(global_object, year_month->iso_year(), year_month->iso_month(), day, calendar));

    // 12. Let durationToAdd be ! CreateTemporalDuration(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], balanceResult.[[Days]], 0, 0, 0, 0, 0, 0).
    auto* duration_to_add = MUST(create_temporal_duration(global_object, duration.years, duration.months, duration.weeks, balance_result.days, 0, 0, 0, 0, 0, 0));

    // 13. Let optionsCopy be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* options_copy = Object::create(global_object, global_object.object_prototype());

    // 14. Let entries be ? EnumerableOwnPropertyNames(options, key+value).
    auto entries = TRY(options->enumerable_own_property_names(Object::PropertyKind::KeyAndValue));

    // 15. For each element nextEntry of entries, do
    for (auto& next_entry : entries) {
        auto key = MUST(next_entry.as_array().get_without_side_effects(0).to_property_key(global_object));
        auto value = next_entry.as_array().get_without_side_effects(1);

        // a. Perform ! CreateDataPropertyOrThrow(optionsCopy, nextEntry[0], nextEntry[1]).
        MUST(options_copy->create_data_property_or_throw(key, value));
    }

    // 16. Let addedDate be ? CalendarDateAdd(calendar, date, durationToAdd, options).
    auto* added_date = TRY(calendar_date_add(global_object, calendar, date, *duration_to_add, options));

    // 17. Let addedDateFields be ? PrepareTemporalFields(addedDate, fieldNames, «»).
    auto* added_date_fields = TRY(prepare_temporal_fields(global_object, *added_date, field_names, {}));

    // 18. Return ? YearMonthFromFields(calendar, addedDateFields, optionsCopy).
    return TRY(year_month_from_fields(global_object, calendar, *added_date_fields, options_copy));
}

// 9.3.13 Temporal.PlainYearMonth.prototype.subtract ( temporalDurationLike [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.subtract
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::subtract)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Let duration be ? ToLimitedTemporalDuration(temporalDurationLike, « »).
    auto duration = TRY(to_limited_temporal_duration(global_object, vm.argument(0), {}));

    // 4. Let balanceResult be ? BalanceDuration(duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]], "day").
    auto balance_result = TRY(balance_duration(global_object, duration.days, duration.hours, duration.minutes, duration.seconds, duration.milliseconds, duration.microseconds, *js_bigint(vm, Crypto::SignedBigInteger::create_from((i64)duration.nanoseconds)), "day"sv));

    // 5. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 6. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 7. Let fieldNames be ? CalendarFields(calendar, « "monthCode", "year" »).
    auto field_names = TRY(calendar_fields(global_object, calendar, { "monthCode"sv, "year"sv }));

    // 8. Let sign be ! DurationSign(duration.[[Years]], duration.[[Months]], duration.[[Weeks]], balanceResult.[[Days]], 0, 0, 0, 0, 0, 0).
    auto sign = duration_sign(duration.years, duration.months, duration.weeks, balance_result.days, 0, 0, 0, 0, 0, 0);

    double day;

    // 9. If sign < 0, then
    if (sign < 0) {
        // a. Let dayFromCalendar be ? CalendarDaysInMonth(calendar, yearMonth).
        auto day_from_calendar = TRY(calendar_days_in_month(global_object, calendar, *year_month));

        // b. Let day be ? ToPositiveInteger(dayFromCalendar).
        day = TRY(to_positive_integer(global_object, day_from_calendar));
    }
    // 10. Else,
    else {
        // a. Let day be 1.
        day = 1;
    }

    // 11. Let date be ? CreateTemporalDate(yearMonth.[[ISOYear]], yearMonth.[[ISOMonth]], day, calendar).
    auto* date = TRY(create_temporal_date(global_object, year_month->iso_year(), year_month->iso_month(), day, calendar));

    // 12. Let durationToAdd be ! CreateTemporalDuration(−duration.[[Years]], −duration.[[Months]], −duration.[[Weeks]], −balanceResult.[[Days]], 0, 0, 0, 0, 0, 0).
    auto* duration_to_add = MUST(create_temporal_duration(global_object, -duration.years, -duration.months, -duration.weeks, -balance_result.days, 0, 0, 0, 0, 0, 0));

    // 13. Let optionsCopy be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* options_copy = Object::create(global_object, global_object.object_prototype());

    // 14. Let entries be ? EnumerableOwnPropertyNames(options, key+value).
    auto entries = TRY(options->enumerable_own_property_names(Object::PropertyKind::KeyAndValue));

    // 15. For each element nextEntry of entries, do
    for (auto& next_entry : entries) {
        auto key = MUST(next_entry.as_array().get_without_side_effects(0).to_property_key(global_object));
        auto value = next_entry.as_array().get_without_side_effects(1);

        // a. Perform ! CreateDataPropertyOrThrow(optionsCopy, nextEntry[0], nextEntry[1]).
        MUST(options_copy->create_data_property_or_throw(key, value));
    }

    // 16. Let addedDate be ? CalendarDateAdd(calendar, date, durationToAdd, options).
    auto* added_date = TRY(calendar_date_add(global_object, calendar, date, *duration_to_add, options));

    // 17. Let addedDateFields be ? PrepareTemporalFields(addedDate, fieldNames, «»).
    auto* added_date_fields = TRY(prepare_temporal_fields(global_object, *added_date, field_names, {}));

    // 18. Return ? YearMonthFromFields(calendar, addedDateFields, optionsCopy).
    return TRY(year_month_from_fields(global_object, calendar, *added_date_fields, options_copy));
}

// 9.3.14 Temporal.PlainYearMonth.prototype.until ( other [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.until
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::until)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Set other to ? ToTemporalYearMonth(other).
    auto* other = TRY(to_temporal_year_month(global_object, vm.argument(0)));

    // 4. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 5. If ? CalendarEquals(calendar, other.[[Calendar]]) is false, throw a RangeError exception.
    if (!TRY(calendar_equals(global_object, calendar, other->calendar())))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalDifferentCalendars);

    // 6. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 7. Let disallowedUnits be « "week", "day", "hour", "minute", "second", "millisecond", "microsecond", "nanosecond" ».
    auto disallowed_units = Vector<StringView> { "week"sv, "day"sv, "hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv };

    // 8. Let smallestUnit be ? ToSmallestTemporalUnit(options, disallowedUnits, "month").
    auto smallest_unit = TRY(to_smallest_temporal_unit(global_object, *options, disallowed_units, "month"sv));

    // 9. Let largestUnit be ? ToLargestTemporalUnit(options, disallowedUnits, "auto", "year").
    auto largest_unit = TRY(to_largest_temporal_unit(global_object, *options, disallowed_units, "auto"sv, "year"));

    // 10. Perform ? ValidateTemporalUnitRange(largestUnit, smallestUnit).
    TRY(validate_temporal_unit_range(global_object, *largest_unit, *smallest_unit));

    // 11. Let roundingMode be ? ToTemporalRoundingMode(options, "trunc").
    auto rounding_mode = TRY(to_temporal_rounding_mode(global_object, *options, "trunc"sv));

    // 12. Let roundingIncrement be ? ToTemporalRoundingIncrement(options, undefined, false).
    auto rounding_increment = TRY(to_temporal_rounding_increment(global_object, *options, {}, false));

    // 13. Let fieldNames be ? CalendarFields(calendar, « "monthCode", "year" »).
    auto field_names = TRY(calendar_fields(global_object, calendar, { "monthCode"sv, "year"sv }));

    // 14. Let otherFields be ? PrepareTemporalFields(other, fieldNames, «»).
    auto* other_fields = TRY(prepare_temporal_fields(global_object, *other, field_names, {}));

    // 15. Perform ! CreateDataPropertyOrThrow(otherFields, "day", 1𝔽).
    MUST(other_fields->create_data_property_or_throw(vm.names.day, Value(1)));

    // 16. Let otherDate be ? DateFromFields(calendar, otherFields).
    // FIXME: Spec doesn't pass required options, see https://github.com/tc39/proposal-temporal/issues/1685.
    auto* other_date = TRY(date_from_fields(global_object, calendar, *other_fields, *options));

    // 17. Let thisFields be ? PrepareTemporalFields(yearMonth, fieldNames, «»).
    auto* this_fields = TRY(prepare_temporal_fields(global_object, *year_month, field_names, {}));

    // 18. Perform ! CreateDataPropertyOrThrow(thisFields, "day", 1𝔽).
    MUST(this_fields->create_data_property_or_throw(vm.names.day, Value(1)));

    // 19. Let thisDate be ? DateFromFields(calendar, thisFields).
    // FIXME: Spec doesn't pass required options, see https://github.com/tc39/proposal-temporal/issues/1685.
    auto* this_date = TRY(date_from_fields(global_object, calendar, *this_fields, *options));

    // 20. Let untilOptions be ? MergeLargestUnitOption(options, largestUnit).
    auto* until_options = TRY(merge_largest_unit_option(global_object, *options, *largest_unit));

    // 21. Let result be ? CalendarDateUntil(calendar, thisDate, otherDate, untilOptions).
    auto* result = TRY(calendar_date_until(global_object, calendar, this_date, other_date, *until_options));

    // 22. If smallestUnit is "month" and roundingIncrement = 1, then
    if (smallest_unit == "month"sv && rounding_increment == 1) {
        // a. Return ! CreateTemporalDuration(result.[[Years]], result.[[Months]], 0, 0, 0, 0, 0, 0, 0, 0).
        return MUST(create_temporal_duration(global_object, result->years(), result->months(), 0, 0, 0, 0, 0, 0, 0, 0));
    }

    // 23. Let result be ? RoundDuration(result.[[Years]], result.[[Months]], 0, 0, 0, 0, 0, 0, 0, 0, roundingIncrement, smallestUnit, roundingMode, thisDate).
    auto round_result = TRY(round_duration(global_object, result->years(), result->months(), 0, 0, 0, 0, 0, 0, 0, 0, rounding_increment, *smallest_unit, rounding_mode, this_date));

    // 24. Return ! CreateTemporalDuration(result.[[Years]], result.[[Months]], 0, 0, 0, 0, 0, 0, 0, 0).
    return MUST(create_temporal_duration(global_object, round_result.years, round_result.months, 0, 0, 0, 0, 0, 0, 0, 0));
}

// 9.3.15 Temporal.PlainYearMonth.prototype.since ( other [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.since
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::since)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Set other to ? ToTemporalYearMonth(other).
    auto* other = TRY(to_temporal_year_month(global_object, vm.argument(0)));

    // 4. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 5. If ? CalendarEquals(calendar, other.[[Calendar]]) is false, throw a RangeError exception.
    if (!TRY(calendar_equals(global_object, calendar, other->calendar())))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalDifferentCalendars);

    // 6. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 7. Let disallowedUnits be « "week", "day", "hour", "minute", "second", "millisecond", "microsecond", "nanosecond" ».
    auto disallowed_units = Vector<StringView> { "week"sv, "day"sv, "hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv };

    // 8. Let smallestUnit be ? ToSmallestTemporalUnit(options, disallowedUnits, "month").
    auto smallest_unit = TRY(to_smallest_temporal_unit(global_object, *options, disallowed_units, "month"sv));

    // 9. Let largestUnit be ? ToLargestTemporalUnit(options, disallowedUnits, "auto", "year").
    auto largest_unit = TRY(to_largest_temporal_unit(global_object, *options, disallowed_units, "auto"sv, "year"));

    // 10. Perform ? ValidateTemporalUnitRange(largestUnit, smallestUnit).
    TRY(validate_temporal_unit_range(global_object, *largest_unit, *smallest_unit));

    // 11. Let roundingMode be ? ToTemporalRoundingMode(options, "trunc").
    auto rounding_mode = TRY(to_temporal_rounding_mode(global_object, *options, "trunc"sv));

    // 12. Set roundingMode to ! NegateTemporalRoundingMode(roundingMode).
    rounding_mode = negate_temporal_rounding_mode(rounding_mode);

    // 13. Let roundingIncrement be ? ToTemporalRoundingIncrement(options, undefined, false).
    auto rounding_increment = TRY(to_temporal_rounding_increment(global_object, *options, {}, false));

    // 14. Let fieldNames be ? CalendarFields(calendar, « "monthCode", "year" »).
    auto field_names = TRY(calendar_fields(global_object, calendar, { "monthCode"sv, "year"sv }));

    // 15. Let otherFields be ? PrepareTemporalFields(other, fieldNames, «»).
    auto* other_fields = TRY(prepare_temporal_fields(global_object, *other, field_names, {}));

    // 16. Perform ! CreateDataPropertyOrThrow(otherFields, "day", 1𝔽).
    MUST(other_fields->create_data_property_or_throw(vm.names.day, Value(1)));

    // 17. Let otherDate be ? DateFromFields(calendar, otherFields).
    // FIXME: Spec doesn't pass required options, see https://github.com/tc39/proposal-temporal/issues/1685.
    auto* other_date = TRY(date_from_fields(global_object, calendar, *other_fields, *options));

    // 18. Let thisFields be ? PrepareTemporalFields(yearMonth, fieldNames, «»).
    auto* this_fields = TRY(prepare_temporal_fields(global_object, *year_month, field_names, {}));

    // 19. Perform ! CreateDataPropertyOrThrow(thisFields, "day", 1𝔽).
    MUST(this_fields->create_data_property_or_throw(vm.names.day, Value(1)));

    // 20. Let thisDate be ? DateFromFields(calendar, thisFields).
    // FIXME: Spec doesn't pass required options, see https://github.com/tc39/proposal-temporal/issues/1685.
    auto* this_date = TRY(date_from_fields(global_object, calendar, *this_fields, *options));

    // 21. Let untilOptions be ? MergeLargestUnitOption(options, largestUnit).
    auto* until_options = TRY(merge_largest_unit_option(global_object, *options, *largest_unit));

    // 22. Let result be ? CalendarDateUntil(calendar, thisDate, otherDate, untilOptions).
    auto* result = TRY(calendar_date_until(global_object, calendar, this_date, other_date, *until_options));

    // 23. If smallestUnit is "month" and roundingIncrement = 1, then
    if (smallest_unit == "month"sv && rounding_increment == 1) {
        // a. Return ! CreateTemporalDuration(−result.[[Years]], −result.[[Months]], 0, 0, 0, 0, 0, 0, 0, 0).
        return MUST(create_temporal_duration(global_object, -result->years(), -result->months(), 0, 0, 0, 0, 0, 0, 0, 0));
    }

    // 24. Let result be ? RoundDuration(result.[[Years]], result.[[Months]], 0, 0, 0, 0, 0, 0, 0, 0, roundingIncrement, smallestUnit, roundingMode, thisDate).
    auto round_result = TRY(round_duration(global_object, result->years(), result->months(), 0, 0, 0, 0, 0, 0, 0, 0, rounding_increment, *smallest_unit, rounding_mode, this_date));

    // 25. Return ! CreateTemporalDuration(−result.[[Years]], −result.[[Months]], 0, 0, 0, 0, 0, 0, 0, 0).
    return MUST(create_temporal_duration(global_object, -round_result.years, -round_result.months, 0, 0, 0, 0, 0, 0, 0, 0));
}

// 9.3.16 Temporal.PlainYearMonth.prototype.equals ( other ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.equals
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::equals)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Set other to ? ToTemporalYearMonth(other).
    auto* other = TRY(to_temporal_year_month(global_object, vm.argument(0)));

    // 4. If yearMonth.[[ISOYear]] ≠ other.[[ISOYear]], return false.
    if (year_month->iso_year() != other->iso_year())
        return Value(false);

    // 5. If yearMonth.[[ISOMonth]] ≠ other.[[ISOMonth]], return false.
    if (year_month->iso_month() != other->iso_month())
        return Value(false);

    // 6. If yearMonth.[[ISODay]] ≠ other.[[ISODay]], return false.
    if (year_month->iso_day() != other->iso_day())
        return Value(false);

    // 7. Return ? CalendarEquals(yearMonth.[[Calendar]], other.[[Calendar]]).
    return Value(TRY(calendar_equals(global_object, year_month->calendar(), other->calendar())));
}

// 9.3.17 Temporal.PlainYearMonth.prototype.toString ( [ options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::to_string)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Set options to ? GetOptionsObject(options).
    auto* options = TRY(get_options_object(global_object, vm.argument(0)));

    // 4. Let showCalendar be ? ToShowCalendarOption(options).
    auto show_calendar = TRY(to_show_calendar_option(global_object, *options));

    // 5. Return ? TemporalYearMonthToString(yearMonth, showCalendar).
    return js_string(vm, TRY(temporal_year_month_to_string(global_object, *year_month, show_calendar)));
}

// 9.3.18 Temporal.PlainYearMonth.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.tolocalestring
// NOTE: This is the minimum toLocaleString implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::to_locale_string)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Return ? TemporalYearMonthToString(yearMonth, "auto").
    return js_string(vm, TRY(temporal_year_month_to_string(global_object, *year_month, "auto"sv)));
}

// 9.3.19 Temporal.PlainYearMonth.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::to_json)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Return ? TemporalYearMonthToString(yearMonth, "auto").
    return js_string(vm, TRY(temporal_year_month_to_string(global_object, *year_month, "auto"sv)));
}

// 9.3.20 Temporal.PlainYearMonth.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::value_of)
{
    // 1. Throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainYearMonth", "a primitive value");
}

// 9.3.21 Temporal.PlainYearMonth.prototype.toPlainDate ( item ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.toplaindate
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::to_plain_date)
{
    auto item = vm.argument(0);

    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. If Type(item) is not Object, then
    if (!item.is_object()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, item);
    }

    // 4. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 5. Let receiverFieldNames be ? CalendarFields(calendar, « "monthCode", "year" »).
    auto receiver_field_names = TRY(calendar_fields(global_object, calendar, { "monthCode"sv, "year"sv }));

    // 6. Let fields be ? PrepareTemporalFields(yearMonth, receiverFieldNames, «»).
    auto* fields = TRY(prepare_temporal_fields(global_object, *year_month, receiver_field_names, {}));

    // 7. Let inputFieldNames be ? CalendarFields(calendar, « "day" »).
    auto input_field_names = TRY(calendar_fields(global_object, calendar, { "day"sv }));

    // 8. Let inputFields be ? PrepareTemporalFields(item, inputFieldNames, «»).
    auto* input_fields = TRY(prepare_temporal_fields(global_object, item.as_object(), input_field_names, {}));

    // 9. Let mergedFields be ? CalendarMergeFields(calendar, fields, inputFields).
    auto* merged_fields = TRY(calendar_merge_fields(global_object, calendar, *fields, *input_fields));

    // 10. Let mergedFieldNames be the List containing all the elements of receiverFieldNames followed by all the elements of inputFieldNames, with duplicate elements removed.
    Vector<String> merged_field_names;
    for (auto& field_name : receiver_field_names) {
        if (!merged_field_names.contains_slow(field_name))
            merged_field_names.append(move(field_name));
    }
    for (auto& field_name : input_field_names) {
        if (!merged_field_names.contains_slow(field_name))
            merged_field_names.append(move(field_name));
    }

    // 11. Set mergedFields to ? PrepareTemporalFields(mergedFields, mergedFieldNames, «»).
    merged_fields = TRY(prepare_temporal_fields(global_object, *merged_fields, merged_field_names, {}));

    // 12. Let options be ! OrdinaryObjectCreate(null).
    auto* options = Object::create(global_object, nullptr);

    // 13. Perform ! CreateDataPropertyOrThrow(options, "overflow", "reject").
    MUST(options->create_data_property_or_throw(vm.names.overflow, js_string(vm, vm.names.reject.as_string())));

    // 14. Return ? DateFromFields(calendar, mergedFields, options).
    return TRY(date_from_fields(global_object, calendar, *merged_fields, *options));
}

// 9.3.22 Temporal.PlainYearMonth.prototype.getISOFields ( ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.getisofields
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::get_iso_fields)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = TRY(typed_this_object(global_object));

    // 3. Let fields be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* fields = Object::create(global_object, global_object.object_prototype());

    // 4. Perform ! CreateDataPropertyOrThrow(fields, "calendar", yearMonth.[[Calendar]]).
    MUST(fields->create_data_property_or_throw(vm.names.calendar, Value(&year_month->calendar())));

    // 5. Perform ! CreateDataPropertyOrThrow(fields, "isoDay", 𝔽(yearMonth.[[ISODay]])).
    MUST(fields->create_data_property_or_throw(vm.names.isoDay, Value(year_month->iso_day())));

    // 6. Perform ! CreateDataPropertyOrThrow(fields, "isoMonth", 𝔽(yearMonth.[[ISOMonth]])).
    MUST(fields->create_data_property_or_throw(vm.names.isoMonth, Value(year_month->iso_month())));

    // 7. Perform ! CreateDataPropertyOrThrow(fields, "isoYear", 𝔽(yearMonth.[[ISOYear]])).
    MUST(fields->create_data_property_or_throw(vm.names.isoYear, Value(year_month->iso_year())));

    // 8. Return fields.
    return fields;
}

}
