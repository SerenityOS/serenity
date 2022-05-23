/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/CalendarPrototype.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainMonthDay.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>

namespace JS::Temporal {

// 12.4 Properties of the Temporal.Calendar Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-calendar-prototype-object
CalendarPrototype::CalendarPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void CalendarPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 12.4.2 Temporal.Calendar.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.Calendar"), Attribute::Configurable);

    define_native_accessor(vm.names.id, id_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.dateFromFields, date_from_fields, 1, attr);
    define_native_function(vm.names.yearMonthFromFields, year_month_from_fields, 1, attr);
    define_native_function(vm.names.monthDayFromFields, month_day_from_fields, 1, attr);
    define_native_function(vm.names.dateAdd, date_add, 2, attr);
    define_native_function(vm.names.dateUntil, date_until, 2, attr);
    define_native_function(vm.names.year, year, 1, attr);
    define_native_function(vm.names.month, month, 1, attr);
    define_native_function(vm.names.monthCode, month_code, 1, attr);
    define_native_function(vm.names.day, day, 1, attr);
    define_native_function(vm.names.dayOfWeek, day_of_week, 1, attr);
    define_native_function(vm.names.dayOfYear, day_of_year, 1, attr);
    define_native_function(vm.names.weekOfYear, week_of_year, 1, attr);
    define_native_function(vm.names.daysInWeek, days_in_week, 1, attr);
    define_native_function(vm.names.daysInMonth, days_in_month, 1, attr);
    define_native_function(vm.names.daysInYear, days_in_year, 1, attr);
    define_native_function(vm.names.monthsInYear, months_in_year, 1, attr);
    define_native_function(vm.names.inLeapYear, in_leap_year, 1, attr);
    define_native_function(vm.names.fields, fields, 1, attr);
    define_native_function(vm.names.mergeFields, merge_fields, 2, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 0, attr);
    define_native_function(vm.names.era, era, 1, attr);
    define_native_function(vm.names.eraYear, era_year, 1, attr);
}

// 12.4.3 get Temporal.Calendar.prototype.id, https://tc39.es/proposal-temporal/#sec-get-temporal.calendar.prototype.id
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::id_getter)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Return ? ToString(calendar).
    return { js_string(vm, TRY(Value(calendar).to_string(global_object))) };
}

// 12.4.4 Temporal.Calendar.prototype.dateFromFields ( fields [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.datefromfields
// NOTE: This is the minimum dateFromFields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::date_from_fields)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. If Type(fields) is not Object, throw a TypeError exception.
    auto fields = vm.argument(0);
    if (!fields.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, fields.to_string_without_side_effects());

    // 5. Set options to ? GetOptionsObject(options).
    auto const* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 6. Let result be ? ISODateFromFields(fields, options).
    auto result = TRY(iso_date_from_fields(global_object, fields.as_object(), *options));

    // 7. Return ? CreateTemporalDate(result.[[Year]], result.[[Month]], result.[[Day]], calendar).
    return TRY(create_temporal_date(global_object, result.year, result.month, result.day, *calendar));
}

// 12.4.5 Temporal.Calendar.prototype.yearMonthFromFields ( fields [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.yearmonthfromfields
// NOTE: This is the minimum yearMonthFromFields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::year_month_from_fields)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. If Type(fields) is not Object, throw a TypeError exception.
    auto fields = vm.argument(0);
    if (!fields.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, fields.to_string_without_side_effects());

    // 5. Set options to ? GetOptionsObject(options).
    auto const* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 6. Let result be ? ISOYearMonthFromFields(fields, options).
    auto result = TRY(iso_year_month_from_fields(global_object, fields.as_object(), *options));

    // 7. Return ? CreateTemporalYearMonth(result.[[Year]], result.[[Month]], calendar, result.[[ReferenceISODay]]).
    return TRY(create_temporal_year_month(global_object, result.year, result.month, *calendar, result.reference_iso_day));
}

// 12.4.6 Temporal.Calendar.prototype.monthDayFromFields ( fields [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.monthdayfromfields
// NOTE: This is the minimum monthDayFromFields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::month_day_from_fields)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. If Type(fields) is not Object, throw a TypeError exception.
    auto fields = vm.argument(0);
    if (!fields.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, fields.to_string_without_side_effects());

    // 5. Set options to ? GetOptionsObject(options).
    auto const* options = TRY(get_options_object(global_object, vm.argument(1)));

    // 6. Let result be ? ISOMonthDayFromFields(fields, options).
    auto result = TRY(iso_month_day_from_fields(global_object, fields.as_object(), *options));

    // 7. Return ? CreateTemporalMonthDay(result.[[Month]], result.[[Day]], calendar, result.[[ReferenceISOYear]]).
    return TRY(create_temporal_month_day(global_object, result.month, result.day, *calendar, result.reference_iso_year));
}

// 12.4.7 Temporal.Calendar.prototype.dateAdd ( date, duration [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.dateadd
// NOTE: This is the minimum dateAdd implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::date_add)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Set date to ? ToTemporalDate(date).
    auto* date = TRY(to_temporal_date(global_object, vm.argument(0)));

    // 5. Set duration to ? ToTemporalDuration(duration).
    auto* duration = TRY(to_temporal_duration(global_object, vm.argument(1)));

    // 6. Set options to ? GetOptionsObject(options).
    auto const* options = TRY(get_options_object(global_object, vm.argument(2)));

    // 7. Let overflow be ? ToTemporalOverflow(options).
    auto overflow = TRY(to_temporal_overflow(global_object, options));

    // 8. Let balanceResult be ? BalanceDuration(duration.[[Days]], duration.[[Hours]], duration.[[Minutes]], duration.[[Seconds]], duration.[[Milliseconds]], duration.[[Microseconds]], duration.[[Nanoseconds]], "day").
    // FIXME: Narrowing conversion from 'double' to 'i64'
    auto balance_result = TRY(balance_duration(global_object, duration->days(), duration->hours(), duration->minutes(), duration->seconds(), duration->milliseconds(), duration->microseconds(), Crypto::SignedBigInteger::create_from(duration->nanoseconds()), "day"sv));

    // 9. Let result be ? AddISODate(date.[[ISOYear]], date.[[ISOMonth]], date.[[ISODay]], duration.[[Years]], duration.[[Months]], duration.[[Weeks]], balanceResult.[[Days]], overflow).
    auto result = TRY(add_iso_date(global_object, date->iso_year(), date->iso_month(), date->iso_day(), duration->years(), duration->months(), duration->weeks(), balance_result.days, overflow));

    // 10. Return ? CreateTemporalDate(result.[[Year]], result.[[Month]], result.[[Day]], calendar).
    return TRY(create_temporal_date(global_object, result.year, result.month, result.day, *calendar));
}

// 12.4.8 Temporal.Calendar.prototype.dateUntil ( one, two [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.dateuntil
// NOTE: This is the minimum dateUntil implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::date_until)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Set one to ? ToTemporalDate(one).
    auto* one = TRY(to_temporal_date(global_object, vm.argument(0)));

    // 5. Set two to ? ToTemporalDate(two).
    auto* two = TRY(to_temporal_date(global_object, vm.argument(1)));

    // 6. Set options to ? GetOptionsObject(options).
    auto const* options = TRY(get_options_object(global_object, vm.argument(2)));

    // 7. Let largestUnit be ? ToLargestTemporalUnit(options, « "hour", "minute", "second", "millisecond", "microsecond", "nanosecond" », "auto", "day").
    auto largest_unit = TRY(to_largest_temporal_unit(global_object, *options, { "hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv }, "auto"sv, "day"sv));

    // 8. Let result be DifferenceISODate(one.[[ISOYear]], one.[[ISOMonth]], one.[[ISODay]], two.[[ISOYear]], two.[[ISOMonth]], two.[[ISODay]], largestUnit).
    auto result = difference_iso_date(global_object, one->iso_year(), one->iso_month(), one->iso_day(), two->iso_year(), two->iso_month(), two->iso_day(), *largest_unit);

    // 9. Return ! CreateTemporalDuration(result.[[Years]], result.[[Months]], result.[[Weeks]], result.[[Days]], 0, 0, 0, 0, 0, 0).
    return MUST(create_temporal_duration(global_object, result.years, result.months, result.weeks, result.days, 0, 0, 0, 0, 0, 0));
}

// 12.4.9 Temporal.Calendar.prototype.year ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.year
// NOTE: This is the minimum year implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = TRY(to_temporal_date(global_object, temporal_date_like));
    }

    // 5. Return ! ISOYear(temporalDateLike).
    return Value(iso_year(temporal_date_like.as_object()));
}

// 12.4.10 Temporal.Calendar.prototype.month ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.month
// NOTE: This is the minimum month implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::month)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);

    // 4. If Type(temporalDateLike) is Object and temporalDateLike has an [[InitializedTemporalMonthDay]] internal slot, then
    if (temporal_date_like.is_object() && is<PlainMonthDay>(temporal_date_like.as_object())) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::TemporalAmbiguousMonthOfPlainMonthDay);
    }

    // 5. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = TRY(to_temporal_date(global_object, temporal_date_like));
    }

    // 6. Return ! ISOMonth(temporalDateLike).
    return Value(iso_month(temporal_date_like.as_object()));
}

// 12.4.11 Temporal.Calendar.prototype.monthCode ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.monthcode
// NOTE: This is the minimum monthCode implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::month_code)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]], [[InitializedTemporalMonthDay]], or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainMonthDay>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = TRY(to_temporal_date(global_object, temporal_date_like));
    }

    // 5. Return ! ISOMonthCode(temporalDateLike).
    return js_string(vm, iso_month_code(temporal_date_like.as_object()));
}

// 12.4.12 Temporal.Calendar.prototype.day ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.day
// NOTE: This is the minimum day implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::day)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalMonthDay]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainMonthDay>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = TRY(to_temporal_date(global_object, temporal_date_like));
    }

    // 5. Return ! ISODay(temporalDateLike).
    return Value(iso_day(temporal_date_like.as_object()));
}

// 12.4.13 Temporal.Calendar.prototype.dayOfWeek ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.dayofweek
// NOTE: This is the minimum dayOfWeek implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::day_of_week)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Let temporalDate be ? ToTemporalDate(temporalDateLike).
    auto* temporal_date = TRY(to_temporal_date(global_object, vm.argument(0)));

    // 5. Let epochDays be MakeDay(𝔽(temporalDate.[[ISOYear]]), 𝔽(temporalDate.[[ISOMonth]] - 1), 𝔽(temporalDate.[[ISODay]])).
    auto epoch_days = make_day(temporal_date->iso_year(), temporal_date->iso_month() - 1, temporal_date->iso_day());

    // 6. Assert: epochDays is finite.
    VERIFY(isfinite(epoch_days));

    // 7. Let dayOfWeek be WeekDay(MakeDate(epochDays, +0𝔽)).
    auto day_of_week = week_day(make_date(epoch_days, 0));

    // 8. If dayOfWeek = +0𝔽, return 7𝔽.
    if (day_of_week == 0)
        return Value(7);

    // 9. Return dayOfWeek.
    return Value(day_of_week);
}

// 12.4.14 Temporal.Calendar.prototype.dayOfYear ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.dayofyear
// NOTE: This is the minimum dayOfYear implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::day_of_year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Let temporalDate be ? ToTemporalDate(temporalDateLike).
    auto* temporal_date = TRY(to_temporal_date(global_object, vm.argument(0)));

    // 5. Let epochDays be MakeDay(𝔽(temporalDate.[[ISOYear]]), 𝔽(temporalDate.[[ISOMonth]] - 1), 𝔽(temporalDate.[[ISODay]])).
    auto epoch_days = make_day(temporal_date->iso_year(), temporal_date->iso_month() - 1, temporal_date->iso_day());

    // 6. Assert: epochDays is finite.
    VERIFY(isfinite(epoch_days));

    // 7. Return DayWithinYear(MakeDate(epochDays, +0𝔽)) + 1𝔽.
    return Value(day_within_year(make_date(epoch_days, 0)) + 1);
}

// 12.4.15 Temporal.Calendar.prototype.weekOfYear ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.weekofyear
// NOTE: This is the minimum weekOfYear implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::week_of_year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Let temporalDate be ? ToTemporalDate(temporalDateLike).
    auto* temporal_date = TRY(to_temporal_date(global_object, vm.argument(0)));

    // 5. Return 𝔽(! ToISODayOfYear(temporalDate.[[ISOYear]], temporalDate.[[ISOMonth]], temporalDate.[[ISODay]])).
    return Value(to_iso_week_of_year(temporal_date->iso_year(), temporal_date->iso_month(), temporal_date->iso_day()));
}

// 12.4.16 Temporal.Calendar.prototype.daysInWeek ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.daysinweek
// NOTE: This is the minimum daysInWeek implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::days_in_week)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Let temporalDate be ? ToTemporalDate(temporalDateLike).
    [[maybe_unused]] auto* temporal_date = TRY(to_temporal_date(global_object, vm.argument(0)));

    // 5. Return 7𝔽.
    return Value(7);
}

// 12.4.16 Temporal.Calendar.prototype.daysInMonth ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.daysinweek
// NOTE: This is the minimum daysInMonth implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::days_in_month)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slots, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = TRY(to_temporal_date(global_object, temporal_date_like));
    }

    // 5. Return 𝔽(! ISODaysInMonth(temporalDateLike.[[ISOYear]], temporalDateLike.[[ISOMonth]])).
    return Value(iso_days_in_month(iso_year(temporal_date_like.as_object()), iso_month(temporal_date_like.as_object())));
}

// 12.4.18 Temporal.Calendar.prototype.daysInYear ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.daysinyear
// NOTE: This is the minimum daysInYear implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::days_in_year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = TRY(to_temporal_date(global_object, temporal_date_like));
    }

    // 5. Return DaysInYear(𝔽(temporalDateLike.[[ISOYear]])).
    return Value(JS::days_in_year(iso_year(temporal_date_like.as_object())));
}

// 12.4.19 Temporal.Calendar.prototype.monthsInYear ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.monthsinyear
// NOTE: This is the minimum monthsInYear implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::months_in_year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Perform ? ToTemporalDate(temporalDateLike).
        (void)TRY(to_temporal_date(global_object, temporal_date_like));
    }

    // 5. Return 12𝔽.
    return Value(12);
}

// 12.4.20 Temporal.Calendar.prototype.inLeapYear ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.inleapyear
// NOTE: This is the minimum inLeapYear implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::in_leap_year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = TRY(to_temporal_date(global_object, temporal_date_like));
    }

    // 5. If InLeapYear(TimeFromYear(𝔽(temporalDateLike.[[ISOYear]]))) is 1𝔽, return true.
    if (JS::in_leap_year(time_from_year(iso_year(temporal_date_like.as_object()))))
        return Value(true);

    // 6. Return false.
    return Value(false);
}

// 12.4.21 Temporal.Calendar.prototype.fields ( fields ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.fields
// NOTE: This is the minimum fields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::fields)
{
    auto fields = vm.argument(0);

    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Let iteratorRecord be ? Getiterator(fields, sync).
    auto iterator_record = TRY(get_iterator(global_object, fields, IteratorHint::Sync));

    // 5. Let fieldNames be a new empty List.
    auto field_names = MarkedVector<Value> { vm.heap() };

    // 6. Let next be true.
    // 7. Repeat, while next is not false,
    while (true) {
        // a. Set next to ? IteratorStep(iteratorRecord).
        auto* next = TRY(iterator_step(global_object, iterator_record));

        // b. If next is not false, then
        if (!next)
            break;

        // i. Let nextValue be ? IteratorValue(next).
        auto next_value = TRY(iterator_value(global_object, *next));

        // ii. If Type(nextValue) is not String, then
        if (!next_value.is_string()) {
            // 1. Let completion be ThrowCompletion(a newly created TypeError object).
            auto completion = vm.throw_completion<TypeError>(global_object, ErrorType::TemporalInvalidCalendarFieldValue, next_value.to_string_without_side_effects());

            // 2. Return ? IteratorClose(iteratorRecord, completion).
            return TRY(iterator_close(global_object, iterator_record, move(completion)));
        }

        // iii. If fieldNames contains nextValue, then
        if (field_names.contains_slow(next_value)) {
            // 1. Let completion be ThrowCompletion(a newly created RangeError object).
            auto completion = vm.throw_completion<RangeError>(global_object, ErrorType::TemporalDuplicateCalendarField, next_value.as_string().string());

            // 2. Return ? IteratorClose(iteratorRecord, completion).
            return TRY(iterator_close(global_object, iterator_record, move(completion)));
        }

        // iv. If nextValue is not one of "year", "month", "monthCode", "day", "hour", "minute", "second", "millisecond", "microsecond", "nanosecond", then
        if (!next_value.as_string().string().is_one_of("year"sv, "month"sv, "monthCode"sv, "day"sv, "hour"sv, "minute"sv, "second"sv, "millisecond"sv, "microsecond"sv, "nanosecond"sv)) {
            // 1. Let completion be ThrowCompletion(a newly created RangeError object).
            auto completion = vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarFieldName, next_value.as_string().string());

            // 2. Return ? IteratorClose(iteratorRecord, completion).
            return TRY(iterator_close(global_object, iterator_record, move(completion)));
        }

        // v. Append nextValue to the end of the List fieldNames.
        field_names.append(next_value);
    }

    // 8. Return CreateArrayFromList(fieldNames).
    return Array::create_from(global_object, field_names);
}

// 12.4.22 Temporal.Calendar.prototype.mergeFields ( fields, additionalFields ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.mergefields
// NOTE: This is the minimum mergeFields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::merge_fields)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Set fields to ? ToObject(fields).
    auto* fields = TRY(vm.argument(0).to_object(global_object));

    // 5. Set additionalFields to ? ToObject(additionalFields).
    auto* additional_fields = TRY(vm.argument(1).to_object(global_object));

    // 6. Return ? DefaultMergeFields(fields, additionalFields).
    return TRY(default_merge_fields(global_object, *fields, *additional_fields));
}

// 12.4.23 Temporal.Calendar.prototype.toString ( ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::to_string)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Return calendar.[[Identifier]].
    return js_string(vm, calendar->identifier());
}

// 12.4.24 Temporal.Calendar.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::to_json)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. Return ? ToString(calendar).
    return js_string(vm, TRY(Value(calendar).to_string(global_object)));
}

// 15.6.2.6 Temporal.Calendar.prototype.era ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.era
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::era)
{
    auto temporal_date_like = vm.argument(0);

    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]], or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = TRY(to_temporal_date(global_object, temporal_date_like));
    }

    // 4. If calendar.[[Identifier]] is "iso8601", then
    if (calendar->identifier() == "iso8601"sv) {
        // a. Return undefined.
        return js_undefined();
    }

    // 5. Let era be the result of implementation-defined processing of temporalDateLike and calendar.[[Identifier]].
    // 6. Return era.

    // NOTE: No support for non-iso8601 calendars yet.
    VERIFY_NOT_REACHED();
}

// 15.6.2.7 Temporal.Calendar.prototype.eraYear ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.erayear
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::era_year)
{
    auto temporal_date_like = vm.argument(0);

    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = TRY(typed_this_object(global_object));

    // 3. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]], or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = TRY(to_temporal_date(global_object, temporal_date_like));
    }

    // 4. If calendar.[[Identifier]] is "iso8601", then
    if (calendar->identifier() == "iso8601"sv) {
        // a. Return undefined.
        return js_undefined();
    }

    // 5. Let eraYear be the result of implementation-defined processing of temporalDateLike and calendar.[[Identifier]].
    // 6. Return 𝔽(eraYear).

    // NOTE: No support for non-iso8601 calendars yet.
    VERIFY_NOT_REACHED();
}

}
