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
#include <LibJS/Runtime/Temporal/CalendarPrototype.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainMonthDay.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>

namespace JS::Temporal {

// 12.4 Properties of the Temporal.Calendar Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-calendar-prototype-object
CalendarPrototype::CalendarPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
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
    define_native_function(vm.names.dateFromFields, date_from_fields, 2, attr);
    define_native_function(vm.names.yearMonthFromFields, year_month_from_fields, 2, attr);
    define_native_function(vm.names.monthDayFromFields, month_day_from_fields, 2, attr);
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
}

static Calendar* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<Calendar>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.Calendar");
        return {};
    }
    return static_cast<Calendar*>(this_object);
}

// 12.4.3 get Temporal.Calendar.prototype.id, https://tc39.es/proposal-temporal/#sec-get-temporal.calendar.prototype.id
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::id_getter)
{
    // 1. Let calendar be the this value.
    auto calendar = vm.this_value(global_object);

    // 2. Return ? ToString(calendar).
    return js_string(vm, calendar.to_string(global_object));
}

// 12.4.4 Temporal.Calendar.prototype.dateFromFields ( fields, options ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.datefromfields
// NOTE: This is the minimum dateFromFields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::date_from_fields)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. If Type(fields) is not Object, throw a TypeError exception.
    auto fields = vm.argument(0);
    if (!fields.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, fields.to_string_without_side_effects());
        return {};
    }

    // 5. Set options to ? GetOptionsObject(options).
    auto* options = get_options_object(global_object, vm.argument(1));
    if (vm.exception())
        return {};

    // 6. Let result be ? ISODateFromFields(fields, options).
    auto result = iso_date_from_fields(global_object, fields.as_object(), *options);
    if (vm.exception())
        return {};

    // 7. Return ? CreateTemporalDate(result.[[Year]], result.[[Month]], result.[[Day]], calendar).
    return create_temporal_date(global_object, result->year, result->month, result->day, *calendar);
}

// 12.4.5 Temporal.Calendar.prototype.yearMonthFromFields ( fields, options ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.yearmonthfromfields
// NOTE: This is the minimum yearMonthFromFields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::year_month_from_fields)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. If Type(fields) is not Object, throw a TypeError exception.
    auto fields = vm.argument(0);
    if (!fields.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, fields.to_string_without_side_effects());
        return {};
    }

    // 5. Set options to ? GetOptionsObject(options).
    auto* options = get_options_object(global_object, vm.argument(1));
    if (vm.exception())
        return {};

    // 6. Let result be ? ISOYearMonthFromFields(fields, options).
    auto result = iso_year_month_from_fields(global_object, fields.as_object(), *options);
    if (vm.exception())
        return {};

    // 7. Return ? CreateTemporalYearMonth(result.[[Year]], result.[[Month]], calendar, result.[[ReferenceISODay]]).
    return create_temporal_year_month(global_object, result->year, result->month, *calendar, result->reference_iso_day);
}

// 12.4.6 Temporal.Calendar.prototype.monthDayFromFields ( fields, options ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.monthdayfromfields
// NOTE: This is the minimum monthDayFromFields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::month_day_from_fields)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. If Type(fields) is not Object, throw a TypeError exception.
    auto fields = vm.argument(0);
    if (!fields.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, fields.to_string_without_side_effects());
        return {};
    }

    // 5. Set options to ? GetOptionsObject(options).
    auto* options = get_options_object(global_object, vm.argument(1));
    if (vm.exception())
        return {};

    // 6. Let result be ? ISOMonthDayFromFields(fields, options).
    auto result = iso_month_day_from_fields(global_object, fields.as_object(), *options);
    if (vm.exception())
        return {};

    // 7. Return ? CreateTemporalMonthDay(result.[[Month]], result.[[Day]], calendar, result.[[ReferenceISOYear]]).
    return create_temporal_month_day(global_object, result->month, result->day, *calendar, result->reference_iso_year);
}

// 12.4.9 Temporal.Calendar.prototype.year ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.year
// NOTE: This is the minimum year implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = to_temporal_date(global_object, temporal_date_like);
        if (vm.exception())
            return {};
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
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);

    // 4. If Type(temporalDateLike) is Object and temporalDateLike has an [[InitializedTemporalMonthDay]] internal slot, then
    if (temporal_date_like.is_object() && is<PlainMonthDay>(temporal_date_like.as_object())) {
        // a. Throw a TypeError exception.
        vm.throw_exception<TypeError>(global_object, ErrorType::TemporalAmbiguousMonthOfPlainMonthDay);
        return {};
    }

    // 5. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = to_temporal_date(global_object, temporal_date_like);
        if (vm.exception())
            return {};
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
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]], [[InitializedTemporalMonthDay]], or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainMonthDay>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = to_temporal_date(global_object, temporal_date_like);
        if (vm.exception())
            return {};
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
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalMonthDay]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainMonthDay>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = to_temporal_date(global_object, temporal_date_like);
        if (vm.exception())
            return {};
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
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Let temporalDate be ? ToTemporalDate(temporalDateLike).
    auto* temporal_date = to_temporal_date(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 5. Return 𝔽(! ToISODayOfWeek(temporalDate.[[ISOYear]], temporalDate.[[ISOMonth]], temporalDate.[[ISODay]])).
    return Value(to_iso_day_of_week(temporal_date->iso_year(), temporal_date->iso_month(), temporal_date->iso_day()));
}

// 12.4.14 Temporal.Calendar.prototype.dayOfYear ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.dayofyear
// NOTE: This is the minimum dayOfYear implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::day_of_year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Let temporalDate be ? ToTemporalDate(temporalDateLike).
    auto* temporal_date = to_temporal_date(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 5. Return 𝔽(! ToISODayOfYear(temporalDate.[[ISOYear]], temporalDate.[[ISOMonth]], temporalDate.[[ISODay]])).
    return Value(to_iso_day_of_year(temporal_date->iso_year(), temporal_date->iso_month(), temporal_date->iso_day()));
}

// 12.4.15 Temporal.Calendar.prototype.weekOfYear ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.weekofyear
// NOTE: This is the minimum weekOfYear implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::week_of_year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Let temporalDate be ? ToTemporalDate(temporalDateLike).
    auto* temporal_date = to_temporal_date(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 5. Return 𝔽(! ToISODayOfYear(temporalDate.[[ISOYear]], temporalDate.[[ISOMonth]], temporalDate.[[ISODay]])).
    return Value(to_iso_week_of_year(temporal_date->iso_year(), temporal_date->iso_month(), temporal_date->iso_day()));
}

// 12.4.16 Temporal.Calendar.prototype.daysInWeek ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.daysinweek
// NOTE: This is the minimum daysInWeek implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::days_in_week)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Let temporalDate be ? ToTemporalDate(temporalDateLike).
    [[maybe_unused]] auto* temporal_date = to_temporal_date(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 5. Return 7𝔽.
    return Value(7);
}

// 12.4.16 Temporal.Calendar.prototype.daysInMonth ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.daysinweek
// NOTE: This is the minimum daysInMonth implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::days_in_month)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slots, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = to_temporal_date(global_object, temporal_date_like);
        if (vm.exception())
            return {};
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
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = to_temporal_date(global_object, temporal_date_like);
        if (vm.exception())
            return {};
    }

    // 5. Return 𝔽(! ISODaysInYear(temporalDateLike.[[ISOYear]])).
    return Value(iso_days_in_year(iso_year(temporal_date_like.as_object())));
}

// 12.4.19 Temporal.Calendar.prototype.monthsInYear ( temporalDateLike ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.monthsinyear
// NOTE: This is the minimum monthsInYear implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::months_in_year)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Perform ? ToTemporalDate(temporalDateLike).
        (void)to_temporal_date(global_object, temporal_date_like);
        if (vm.exception())
            return {};
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
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    auto temporal_date_like = vm.argument(0);
    // 4. If Type(temporalDateLike) is not Object or temporalDateLike does not have an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]] or [[InitializedTemporalYearMonth]] internal slot, then
    if (!temporal_date_like.is_object() || !(is<PlainDate>(temporal_date_like.as_object()) || is<PlainDateTime>(temporal_date_like.as_object()) || is<PlainYearMonth>(temporal_date_like.as_object()))) {
        // a. Set temporalDateLike to ? ToTemporalDate(temporalDateLike).
        temporal_date_like = to_temporal_date(global_object, temporal_date_like);
        if (vm.exception())
            return {};
    }

    // 5. Return ! IsISOLeapYear(temporalDateLike.[[ISOYear]]).
    return Value(is_iso_leap_year(iso_year(temporal_date_like.as_object())));
}

// 12.4.21 Temporal.Calendar.prototype.fields ( fields ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.fields
// NOTE: This is the minimum fields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::fields)
{
    auto fields = vm.argument(0);

    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Let fieldNames be ? IterableToListOfType(fields, « String »).
    auto field_names = iterable_to_list_of_type(global_object, fields, { OptionType::String });
    if (vm.exception())
        return {};

    // 5. Return ! CreateArrayFromList(fieldNames).
    return Array::create_from(global_object, field_names);
}

// 12.4.22 Temporal.Calendar.prototype.mergeFields ( fields, additionalFields ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.mergefields
// NOTE: This is the minimum mergeFields implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::merge_fields)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Assert: calendar.[[Identifier]] is "iso8601".
    VERIFY(calendar->identifier() == "iso8601"sv);

    // 4. Set fields to ? ToObject(fields).
    auto* fields = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};

    // 5. Set additionalFields to ? ToObject(additionalFields).
    auto* additional_fields = vm.argument(1).to_object(global_object);
    if (vm.exception())
        return {};

    // 6. Return ? DefaultMergeFields(fields, additionalFields).
    return default_merge_fields(global_object, *fields, *additional_fields);
}

// 12.4.23 Temporal.Calendar.prototype.toString ( ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::to_string)
{
    // 1. Let calendar be the this value.
    // 2. Perform ? RequireInternalSlot(calendar, [[InitializedTemporalCalendar]]).
    auto* calendar = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return calendar.[[Identifier]].
    return js_string(vm, calendar->identifier());
}

// 12.4.24 Temporal.Calendar.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.calendar.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(CalendarPrototype::to_json)
{
    // 1. Let calendar be the this value.
    auto calendar = vm.this_value(global_object);

    // 2. Return ? ToString(calendar).
    return js_string(vm, calendar.to_string(global_object));
}

}
