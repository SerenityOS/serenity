/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/CalendarConstructor.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainMonthDay.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>
#include <LibJS/Runtime/Temporal/ZonedDateTime.h>
#include <LibJS/Runtime/Value.h>
#include <LibUnicode/Locale.h>

namespace JS::Temporal {

// 12 Temporal.Calendar Objects, https://tc39.es/proposal-temporal/#sec-temporal-calendar-objects
Calendar::Calendar(String identifier, Object& prototype)
    : Object(prototype)
    , m_identifier(move(identifier))
{
}

// 12.1.1 IsBuiltinCalendar ( id ), https://tc39.es/proposal-temporal/#sec-temporal-isbuiltincalendar
bool is_builtin_calendar(String const& identifier)
{
    // 1. Let calendars be AvailableCalendars().
    auto calendars = available_calendars();

    // 2. If calendars contains id, return true.
    if (calendars.contains_slow(identifier))
        return true;

    // 3. Return false.
    return false;
}

// 12.1.2 AvailableCalendars ( ), https://tc39.es/proposal-temporal/#sec-temporal-availablecalendars
// NOTE: This is the minimum AvailableCalendars implementation for engines without ECMA-402.
// NOTE: This can be removed in favor of using `Unicode::get_available_calendars()` once everything is updated to handle non-iso8601 calendars.
Span<StringView const> available_calendars()
{
    // 1. Return « "iso8601" ».
    static constexpr AK::Array values { "iso8601"sv };
    return values.span();
}

// 12.2.1 CreateTemporalCalendar ( identifier [ , newTarget ] ), https://tc39.es/proposal-temporal/#sec-temporal-createtemporalcalendar
ThrowCompletionOr<Calendar*> create_temporal_calendar(GlobalObject& global_object, String const& identifier, FunctionObject const* new_target)
{
    // 1. Assert: IsBuiltinCalendar(identifier) is true.
    VERIFY(is_builtin_calendar(identifier));

    // 2. If newTarget is not provided, set newTarget to %Temporal.Calendar%.
    if (!new_target)
        new_target = global_object.temporal_calendar_constructor();

    // 3. Let object be ? OrdinaryCreateFromConstructor(newTarget, "%Temporal.Calendar.prototype%", « [[InitializedTemporalCalendar]], [[Identifier]] »).
    // 4. Set object.[[Identifier]] to identifier.
    auto* object = TRY(ordinary_create_from_constructor<Calendar>(global_object, *new_target, &GlobalObject::temporal_calendar_prototype, identifier));

    // 5. Return object.
    return object;
}

// 12.2.2 GetBuiltinCalendar ( id ), https://tc39.es/proposal-temporal/#sec-temporal-getbuiltincalendar
ThrowCompletionOr<Calendar*> get_builtin_calendar(GlobalObject& global_object, String const& identifier)
{
    auto& vm = global_object.vm();

    // 1. If IsBuiltinCalendar(id) is false, throw a RangeError exception.
    if (!is_builtin_calendar(identifier))
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarIdentifier, identifier);

    // 2. Return ! CreateTemporalCalendar(id).
    return MUST(create_temporal_calendar(global_object, identifier));
}

// 12.2.3 GetISO8601Calendar ( ), https://tc39.es/proposal-temporal/#sec-temporal-getiso8601calendar
Calendar* get_iso8601_calendar(GlobalObject& global_object)
{
    // 1. Return ! GetBuiltinCalendar("iso8601").
    return MUST(get_builtin_calendar(global_object, "iso8601"));
}

// 12.2.4 CalendarFields ( calendar, fieldNames ), https://tc39.es/proposal-temporal/#sec-temporal-calendarfields
ThrowCompletionOr<Vector<String>> calendar_fields(GlobalObject& global_object, Object& calendar, Vector<StringView> const& field_names)
{
    auto& vm = global_object.vm();

    // 1. Let fields be ? GetMethod(calendar, "fields").
    auto fields = TRY(Value(&calendar).get_method(global_object, vm.names.fields));

    // 2. Let fieldsArray be CreateArrayFromList(fieldNames).
    auto field_names_values = MarkedVector<Value> { vm.heap() };
    for (auto& field_name : field_names)
        field_names_values.append(js_string(vm, field_name));
    Value fields_array = Array::create_from(global_object, field_names_values);

    // 3. If fields is not undefined, then
    if (fields) {
        // a. Set fieldsArray to ? Call(fields, calendar, « fieldsArray »).
        fields_array = TRY(call(global_object, *fields, &calendar, fields_array));
    }

    // 4. Return ? IterableToListOfType(fieldsArray, « String »).
    auto list = TRY(iterable_to_list_of_type(global_object, fields_array, { OptionType::String }));

    Vector<String> result;
    for (auto& value : list)
        result.append(value.as_string().string());
    return result;
}

// 12.2.5 CalendarMergeFields ( calendar, fields, additionalFields ), https://tc39.es/proposal-temporal/#sec-temporal-calendarmergefields
ThrowCompletionOr<Object*> calendar_merge_fields(GlobalObject& global_object, Object& calendar, Object& fields, Object& additional_fields)
{
    auto& vm = global_object.vm();

    // 1. Let mergeFields be ? GetMethod(calendar, "mergeFields").
    auto* merge_fields = TRY(Value(&calendar).get_method(global_object, vm.names.mergeFields));

    // 2. If mergeFields is undefined, then
    if (!merge_fields) {
        // a. Return ? DefaultMergeFields(fields, additionalFields).
        return TRY(default_merge_fields(global_object, fields, additional_fields));
    }

    // 3. Let result be ? Call(mergeFields, calendar, « fields, additionalFields »).
    auto result = TRY(call(global_object, merge_fields, &calendar, &fields, &additional_fields));

    // 4. If Type(result) is not Object, throw a TypeError exception.
    if (!result.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, result.to_string_without_side_effects());

    // 5. Return result.
    return &result.as_object();
}

// 12.2.6 CalendarDateAdd ( calendar, date, duration [ , options [ , dateAdd ] ] ), https://tc39.es/proposal-temporal/#sec-temporal-calendardateadd
ThrowCompletionOr<PlainDate*> calendar_date_add(GlobalObject& global_object, Object& calendar, Value date, Duration& duration, Object* options, FunctionObject* date_add)
{
    // NOTE: `date` is a `Value` because we sometimes need to pass a PlainDate, sometimes a PlainDateTime, and sometimes undefined.
    auto& vm = global_object.vm();

    // 1. Assert: Type(calendar) is Object.
    // 2. If options is not present, set options to undefined.
    // 3. Assert: Type(options) is Object or Undefined.

    // 4. If dateAdd is not present, set dateAdd to ? GetMethod(calendar, "dateAdd").
    if (!date_add)
        date_add = TRY(Value(&calendar).get_method(global_object, vm.names.dateAdd));

    // 5. Let addedDate be ? Call(dateAdd, calendar, « date, duration, options »).
    auto added_date = TRY(call(global_object, date_add ?: js_undefined(), &calendar, date, &duration, options ?: js_undefined()));

    // 6. Perform ? RequireInternalSlot(addedDate, [[InitializedTemporalDate]]).
    auto* added_date_object = TRY(added_date.to_object(global_object));
    if (!is<PlainDate>(added_date_object))
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Temporal.PlainDate");

    // 7. Return addedDate.
    return static_cast<PlainDate*>(added_date_object);
}

// 12.2.7 CalendarDateUntil ( calendar, one, two, options [ , dateUntil ] ), https://tc39.es/proposal-temporal/#sec-temporal-calendardateuntil
ThrowCompletionOr<Duration*> calendar_date_until(GlobalObject& global_object, Object& calendar, Value one, Value two, Object& options, FunctionObject* date_until)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(calendar) is Object.

    // 2. If dateUntil is not present, set dateUntil to ? GetMethod(calendar, "dateUntil").
    if (!date_until)
        date_until = TRY(Value(&calendar).get_method(global_object, vm.names.dateUntil));

    // 3. Let duration be ? Call(dateUntil, calendar, « one, two, options »).
    auto duration = TRY(call(global_object, date_until ?: js_undefined(), &calendar, one, two, &options));

    // 4. Perform ? RequireInternalSlot(duration, [[InitializedTemporalDuration]]).
    auto* duration_object = TRY(duration.to_object(global_object));
    if (!is<Duration>(duration_object))
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Temporal.Duration");

    // 5. Return duration.
    return static_cast<Duration*>(duration_object);
}

// 12.2.8 CalendarYear ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendaryear
ThrowCompletionOr<double> calendar_year(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Let result be ? Invoke(calendar, "year", « dateLike »).
    auto result = TRY(Value(&calendar).invoke(global_object, vm.names.year, &date_like));

    // 3. If result is undefined, throw a RangeError exception.
    if (result.is_undefined())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarFunctionResult, vm.names.year.as_string(), vm.names.undefined.as_string());

    // 4. Return ? ToIntegerThrowOnInfinity(result).
    return TRY(to_integer_throw_on_infinity(global_object, result, ErrorType::TemporalInvalidCalendarFunctionResult, vm.names.year.as_string(), vm.names.Infinity.as_string()));
}

// 12.2.9 CalendarMonth ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendarmonth
ThrowCompletionOr<double> calendar_month(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Let result be ? Invoke(calendar, "month", « dateLike »).
    auto result = TRY(Value(&calendar).invoke(global_object, vm.names.month, &date_like));

    // 3. If result is undefined, throw a RangeError exception.
    if (result.is_undefined())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarFunctionResult, vm.names.month.as_string(), vm.names.undefined.as_string());

    // 4. Return ? ToPositiveInteger(result).
    return TRY(to_positive_integer(global_object, result));
}

// 12.2.10 CalendarMonthCode ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendarmonthcode
ThrowCompletionOr<String> calendar_month_code(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Let result be ? Invoke(calendar, "monthCode", « dateLike »).
    auto result = TRY(Value(&calendar).invoke(global_object, vm.names.monthCode, &date_like));

    // 3. If result is undefined, throw a RangeError exception.
    if (result.is_undefined())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarFunctionResult, vm.names.monthCode.as_string(), vm.names.undefined.as_string());

    // 4. Return ? ToString(result).
    return result.to_string(global_object);
}

// 12.2.11 CalendarDay ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendarday
ThrowCompletionOr<double> calendar_day(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Let result be ? Invoke(calendar, "day", « dateLike »).
    auto result = TRY(Value(&calendar).invoke(global_object, vm.names.day, &date_like));

    // 3. If result is undefined, throw a RangeError exception.
    if (result.is_undefined())
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarFunctionResult, vm.names.day.as_string(), vm.names.undefined.as_string());

    // 4. Return ? ToPositiveInteger(result).
    return TRY(to_positive_integer(global_object, result));
}

// 12.2.12 CalendarDayOfWeek ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendardayofweek
ThrowCompletionOr<Value> calendar_day_of_week(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Return ? Invoke(calendar, "dayOfWeek", « dateLike »).
    return TRY(Value(&calendar).invoke(global_object, vm.names.dayOfWeek, &date_like));
}

// 12.2.13 CalendarDayOfYear ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendardayofyear
ThrowCompletionOr<Value> calendar_day_of_year(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Return ? Invoke(calendar, "dayOfYear", « dateLike »).
    return TRY(Value(&calendar).invoke(global_object, vm.names.dayOfYear, &date_like));
}

// 12.2.14 CalendarWeekOfYear ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendarweekofyear
ThrowCompletionOr<Value> calendar_week_of_year(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Return ? Invoke(calendar, "weekOfYear", « dateLike »).
    return TRY(Value(&calendar).invoke(global_object, vm.names.weekOfYear, &date_like));
}

// 12.2.14 CalendarDaysInWeek ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendardaysinweek
ThrowCompletionOr<Value> calendar_days_in_week(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Return ? Invoke(calendar, "daysInWeek", « dateLike »).
    return TRY(Value(&calendar).invoke(global_object, vm.names.daysInWeek, &date_like));
}

// 12.2.16 CalendarDaysInMonth ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendardaysinmonth
ThrowCompletionOr<Value> calendar_days_in_month(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Return ? Invoke(calendar, "daysInMonth", « dateLike »).
    return TRY(Value(&calendar).invoke(global_object, vm.names.daysInMonth, &date_like));
}

// 12.2.17 CalendarDaysInYear ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendardaysinyear
ThrowCompletionOr<Value> calendar_days_in_year(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Return ? Invoke(calendar, "daysInYear", « dateLike »).
    return TRY(Value(&calendar).invoke(global_object, vm.names.daysInYear, &date_like));
}

// 12.2.18 CalendarMonthsInYear ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendarmonthsinyear
ThrowCompletionOr<Value> calendar_months_in_year(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Return ? Invoke(calendar, "monthsInYear", « dateLike »).
    return TRY(Value(&calendar).invoke(global_object, vm.names.monthsInYear, &date_like));
}

// 12.2.29 CalendarInLeapYear ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendarinleapyear
ThrowCompletionOr<Value> calendar_in_leap_year(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();
    // 1. Assert: Type(calendar) is Object.

    // 2. Return ? Invoke(calendar, "inLeapYear", « dateLike »).
    return TRY(Value(&calendar).invoke(global_object, vm.names.inLeapYear, &date_like));
}

// 15.6.1.1 CalendarEra ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendarera
ThrowCompletionOr<Value> calendar_era(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(calendar) is Object.

    // 2. Let result be ? Invoke(calendar, "era", « dateLike »).
    auto result = TRY(Value(&calendar).invoke(global_object, vm.names.era, &date_like));

    // 3. If result is not undefined, set result to ? ToString(result).
    if (!result.is_undefined())
        result = js_string(vm, TRY(result.to_string(global_object)));

    // 4. Return result.
    return result;
}

// 15.6.1.2 CalendarEraYear ( calendar, dateLike ), https://tc39.es/proposal-temporal/#sec-temporal-calendarerayear
ThrowCompletionOr<Value> calendar_era_year(GlobalObject& global_object, Object& calendar, Object& date_like)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(calendar) is Object.

    // 2. Let result be ? Invoke(calendar, "eraYear", « dateLike »).
    auto result = TRY(Value(&calendar).invoke(global_object, vm.names.eraYear, &date_like));

    // 3. If result is not undefined, set result to ? ToIntegerThrowOnInfinity(result).
    if (!result.is_undefined())
        result = Value(TRY(to_integer_throw_on_infinity(global_object, result, ErrorType::TemporalInvalidCalendarFunctionResult, vm.names.eraYear.as_string(), "Infinity"sv)));

    // 4. Return result.
    return result;
}

// 12.2.20 ToTemporalCalendar ( temporalCalendarLike ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalcalendar
ThrowCompletionOr<Object*> to_temporal_calendar(GlobalObject& global_object, Value temporal_calendar_like)
{
    auto& vm = global_object.vm();

    // 1. If Type(temporalCalendarLike) is Object, then
    if (temporal_calendar_like.is_object()) {
        auto& temporal_calendar_like_object = temporal_calendar_like.as_object();
        // a. If temporalCalendarLike has an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]], [[InitializedTemporalMonthDay]], [[InitializedTemporalTime]], [[InitializedTemporalYearMonth]], or [[InitializedTemporalZonedDateTime]] internal slot, then
        // i. Return temporalCalendarLike.[[Calendar]].
        if (is<PlainDate>(temporal_calendar_like_object))
            return &static_cast<PlainDate&>(temporal_calendar_like_object).calendar();
        if (is<PlainDateTime>(temporal_calendar_like_object))
            return &static_cast<PlainDateTime&>(temporal_calendar_like_object).calendar();
        if (is<PlainMonthDay>(temporal_calendar_like_object))
            return &static_cast<PlainMonthDay&>(temporal_calendar_like_object).calendar();
        if (is<PlainTime>(temporal_calendar_like_object))
            return &static_cast<PlainTime&>(temporal_calendar_like_object).calendar();
        if (is<PlainYearMonth>(temporal_calendar_like_object))
            return &static_cast<PlainYearMonth&>(temporal_calendar_like_object).calendar();
        if (is<ZonedDateTime>(temporal_calendar_like_object))
            return &static_cast<ZonedDateTime&>(temporal_calendar_like_object).calendar();

        // b. If ? HasProperty(temporalCalendarLike, "calendar") is false, return temporalCalendarLike.
        if (!TRY(temporal_calendar_like_object.has_property(vm.names.calendar)))
            return &temporal_calendar_like_object;

        // c. Set temporalCalendarLike to ? Get(temporalCalendarLike, "calendar").
        temporal_calendar_like = TRY(temporal_calendar_like_object.get(vm.names.calendar));

        // d. If Type(temporalCalendarLike) is Object and ? HasProperty(temporalCalendarLike, "calendar") is false, return temporalCalendarLike.
        if (temporal_calendar_like.is_object() && !TRY(temporal_calendar_like.as_object().has_property(vm.names.calendar)))
            return &temporal_calendar_like.as_object();
    }

    // 2. Let identifier be ? ToString(temporalCalendarLike).
    auto identifier = TRY(temporal_calendar_like.to_string(global_object));

    // 3. If IsBuiltinCalendar(identifier) is false, then
    if (!is_builtin_calendar(identifier)) {
        // a. Set identifier to ? ParseTemporalCalendarString(identifier).
        identifier = TRY(parse_temporal_calendar_string(global_object, identifier));

        // b. If IsBuiltinCalendar(identifier) is false, throw a RangeError exception.
        if (!is_builtin_calendar(identifier))
            return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendarIdentifier, identifier);
    }

    // 4. Return ! CreateTemporalCalendar(identifier).
    return MUST(create_temporal_calendar(global_object, identifier));
}

// 12.2.21 ToTemporalCalendarWithISODefault ( temporalCalendarLike ), https://tc39.es/proposal-temporal/#sec-temporal-totemporalcalendarwithisodefault
ThrowCompletionOr<Object*> to_temporal_calendar_with_iso_default(GlobalObject& global_object, Value temporal_calendar_like)
{
    // 1. If temporalCalendarLike is undefined, then
    if (temporal_calendar_like.is_undefined()) {
        // a. Return ! GetISO8601Calendar().
        return get_iso8601_calendar(global_object);
    }
    // 2. Return ? ToTemporalCalendar(temporalCalendarLike).
    return to_temporal_calendar(global_object, temporal_calendar_like);
}

// 12.2.22 GetTemporalCalendarWithISODefault ( item ), https://tc39.es/proposal-temporal/#sec-temporal-gettemporalcalendarwithisodefault
ThrowCompletionOr<Object*> get_temporal_calendar_with_iso_default(GlobalObject& global_object, Object& item)
{
    auto& vm = global_object.vm();

    // 1. If item has an [[InitializedTemporalDate]], [[InitializedTemporalDateTime]], [[InitializedTemporalMonthDay]], [[InitializedTemporalTime]], [[InitializedTemporalYearMonth]], or [[InitializedTemporalZonedDateTime]] internal slot, then
    // a. Return item.[[Calendar]].
    if (is<PlainDate>(item))
        return &static_cast<PlainDate&>(item).calendar();
    if (is<PlainDateTime>(item))
        return &static_cast<PlainDateTime&>(item).calendar();
    if (is<PlainMonthDay>(item))
        return &static_cast<PlainMonthDay&>(item).calendar();
    if (is<PlainTime>(item))
        return &static_cast<PlainTime&>(item).calendar();
    if (is<PlainYearMonth>(item))
        return &static_cast<PlainYearMonth&>(item).calendar();
    if (is<ZonedDateTime>(item))
        return &static_cast<ZonedDateTime&>(item).calendar();

    // 2. Let calendarLike be ? Get(item, "calendar").
    auto calendar_like = TRY(item.get(vm.names.calendar));

    // 3. Return ? ToTemporalCalendarWithISODefault(calendarLike).
    return to_temporal_calendar_with_iso_default(global_object, calendar_like);
}

// 12.2.23 CalendarDateFromFields ( calendar, fields [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-calendardatefromfields
ThrowCompletionOr<PlainDate*> calendar_date_from_fields(GlobalObject& global_object, Object& calendar, Object const& fields, Object const* options)
{
    auto& vm = global_object.vm();

    // 1. If options is not present, set options to undefined.

    // 2. Let date be ? Invoke(calendar, "dateFromFields", « fields, options »).
    auto date = TRY(Value(&calendar).invoke(global_object, vm.names.dateFromFields, &fields, options ?: js_undefined()));

    // 3. Perform ? RequireInternalSlot(date, [[InitializedTemporalDate]]).
    auto* date_object = TRY(date.to_object(global_object));
    if (!is<PlainDate>(date_object))
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Temporal.PlainDate");

    // 4. Return date.
    return static_cast<PlainDate*>(date_object);
}

// 12.2.24 CalendarYearMonthFromFields ( calendar, fields [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-calendaryearmonthfromfields
ThrowCompletionOr<PlainYearMonth*> calendar_year_month_from_fields(GlobalObject& global_object, Object& calendar, Object const& fields, Object const* options)
{
    auto& vm = global_object.vm();

    // 1. If options is not present, set options to undefined.

    // 2. Let yearMonth be ? Invoke(calendar, "yearMonthFromFields", « fields, options »).
    auto year_month = TRY(Value(&calendar).invoke(global_object, vm.names.yearMonthFromFields, &fields, options ?: js_undefined()));

    // 3. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month_object = TRY(year_month.to_object(global_object));
    if (!is<PlainYearMonth>(year_month_object))
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Temporal.PlainYearMonth");

    // 4. Return yearMonth.
    return static_cast<PlainYearMonth*>(year_month_object);
}

// 12.2.25 CalendarMonthDayFromFields ( calendar, fields [ , options ] ), https://tc39.es/proposal-temporal/#sec-temporal-calendarmonthdayfromfields
ThrowCompletionOr<PlainMonthDay*> calendar_month_day_from_fields(GlobalObject& global_object, Object& calendar, Object const& fields, Object const* options)
{
    auto& vm = global_object.vm();

    // 1. If options is not present, set options to undefined.

    // 2. Let monthDay be ? Invoke(calendar, "monthDayFromFields", « fields, options »).
    auto month_day = TRY(Value(&calendar).invoke(global_object, vm.names.monthDayFromFields, &fields, options ?: js_undefined()));

    // 3. Perform ? RequireInternalSlot(monthDay, [[InitializedTemporalMonthDay]]).
    auto* month_day_object = TRY(month_day.to_object(global_object));
    if (!is<PlainMonthDay>(month_day_object))
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Temporal.PlainMonthDay");

    // 4. Return monthDay.
    return static_cast<PlainMonthDay*>(month_day_object);
}

// 12.2.26 FormatCalendarAnnotation ( id, showCalendar ), https://tc39.es/proposal-temporal/#sec-temporal-formatcalendarannotation
String format_calendar_annotation(StringView id, StringView show_calendar)
{
    // 1. Assert: showCalendar is "auto", "always", or "never".
    VERIFY(show_calendar == "auto"sv || show_calendar == "always"sv || show_calendar == "never"sv);

    // 2. If showCalendar is "never", return the empty String.
    if (show_calendar == "never"sv)
        return String::empty();

    // 3. If showCalendar is "auto" and id is "iso8601", return the empty String.
    if (show_calendar == "auto"sv && id == "iso8601"sv)
        return String::empty();

    // 4. Return the string-concatenation of "[u-ca=", id, and "]".
    return String::formatted("[u-ca={}]", id);
}

// 12.2.27 CalendarEquals ( one, two ), https://tc39.es/proposal-temporal/#sec-temporal-calendarequals
ThrowCompletionOr<bool> calendar_equals(GlobalObject& global_object, Object& one, Object& two)
{
    // 1. If one and two are the same Object value, return true.
    if (&one == &two)
        return true;

    // 2. Let calendarOne be ? ToString(one).
    auto calendar_one = TRY(Value(&one).to_string(global_object));

    // 3. Let calendarTwo be ? ToString(two).
    auto calendar_two = TRY(Value(&two).to_string(global_object));

    // 4. If calendarOne is calendarTwo, return true.
    if (calendar_one == calendar_two)
        return true;

    // 5. Return false.
    return false;
}

// 12.2.28 ConsolidateCalendars ( one, two ), https://tc39.es/proposal-temporal/#sec-temporal-consolidatecalendars
ThrowCompletionOr<Object*> consolidate_calendars(GlobalObject& global_object, Object& one, Object& two)
{
    auto& vm = global_object.vm();
    // 1. If one and two are the same Object value, return two.
    if (&one == &two)
        return &two;

    // 2. Let calendarOne be ? ToString(one).
    auto calendar_one = TRY(Value(&one).to_string(global_object));

    // 3. Let calendarTwo be ? ToString(two).
    auto calendar_two = TRY(Value(&two).to_string(global_object));

    // 4. If calendarOne is calendarTwo, return two.
    if (calendar_one == calendar_two)
        return &two;

    // 5. If calendarOne is "iso8601", return two.
    if (calendar_one == "iso8601"sv)
        return &two;

    // 6. If calendarTwo is "iso8601", return one.
    if (calendar_two == "iso8601"sv)
        return &one;

    // 7. Throw a RangeError exception.
    return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidCalendar);
}

// 12.2.29 ISODaysInMonth ( year, month ), https://tc39.es/proposal-temporal/#sec-temporal-isodaysinmonth
u8 iso_days_in_month(i32 year, u8 month)
{
    // 1. Assert: year is an integer.

    // 2. Assert: month is an integer, month ≥ 1, and month ≤ 12.
    VERIFY(month >= 1 && month <= 12);

    // 3. If month is 1, 3, 5, 7, 8, 10, or 12, return 31.
    if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12)
        return 31;

    // 4. If month is 4, 6, 9, or 11, return 30.
    if (month == 4 || month == 6 || month == 9 || month == 11)
        return 30;

    // 5. Return 28 + ℝ(InLeapYear(TimeFromYear(𝔽(year)))).
    return 28 + JS::in_leap_year(time_from_year(year));
}

// 12.2.30 ToISOWeekOfYear ( year, month, day ), https://tc39.es/proposal-temporal/#sec-temporal-toisoweekofyear
u8 to_iso_week_of_year(i32 year, u8 month, u8 day)
{
    // 1. Assert: year is an integer.
    // 2. Assert: month is an integer.
    // 3. Assert: day is an integer.

    // 4. Let date be the date given by year, month, and day.
    // 5. Return date's week number according to ISO-8601 as an integer.
    auto t = make_date(make_day(year, month - 1, day), 0);
    auto day_of_year = day_within_year(t) + 1;
    auto day_of_week = week_day(t);
    if (day_of_week == 0)
        day_of_week = 7;
    auto week = (day_of_year - day_of_week + 10) / 7;

    if (week < 1) {
        // NOTE: The resulting week is actually part of the previous year. If that year ends with a
        // Thursday (i.e. the first day of the given year is a Friday, or day 5), or the previous
        // year is a leap year and ends with a Friday (i.e. the first day of the given year is a
        // Saturday, or day 6), it has 53 weeks, and 52 weeks otherwise.
        auto day_of_jump = week_day(make_date(make_day(year, 0, 1), 0));
        if (day_of_jump == 5 || (in_leap_year(time_from_year(year - 1)) && day_of_jump == 6))
            return 53;
        else
            return 52;
    } else if (week == 53) {
        auto days_in_year = JS::days_in_year(year);
        if (days_in_year - day_of_year < 4 - day_of_week)
            return 1;
    }

    return week;
}

// 12.2.31 BuildISOMonthCode ( month ), https://tc39.es/proposal-temporal/#sec-buildisomonthcode
String build_iso_month_code(u8 month)
{
    // 1. Let numberPart be ToZeroPaddedDecimalString(month, 2).
    // 2. Return the string-concatenation of "M" and numberPart.
    return String::formatted("M{:02}", month);
}

// 12.2.32 ResolveISOMonth ( fields ), https://tc39.es/proposal-temporal/#sec-temporal-resolveisomonth
ThrowCompletionOr<double> resolve_iso_month(GlobalObject& global_object, Object const& fields)
{
    auto& vm = global_object.vm();

    // 1. Assert: fields is an ordinary object with no more and no less than the own data properties listed in Table 13.

    // 2. Let month be ! Get(fields, "month").
    auto month = MUST(fields.get(vm.names.month));

    // 3. Let monthCode be ! Get(fields, "monthCode").
    auto month_code = MUST(fields.get(vm.names.monthCode));

    // 4. If monthCode is undefined, then
    if (month_code.is_undefined()) {
        // a. If month is undefined, throw a TypeError exception.
        if (month.is_undefined())
            return vm.throw_completion<TypeError>(global_object, ErrorType::MissingRequiredProperty, vm.names.month.as_string());

        // b. Return month.
        return month.as_double();
    }

    // 5. Assert: Type(monthCode) is String.
    VERIFY(month_code.is_string());
    auto& month_code_string = month_code.as_string().string();

    // 6. Let monthLength be the length of monthCode.
    auto month_length = month_code_string.length();

    // 7. If monthLength is not 3, throw a RangeError exception.
    if (month_length != 3)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidMonthCode);

    // 8. Let numberPart be the substring of monthCode from 1.
    auto number_part = month_code_string.substring(1);

    // 9. Set numberPart to ! ToIntegerOrInfinity(numberPart).
    auto number_part_integer = MUST(Value(js_string(vm, move(number_part))).to_integer_or_infinity(global_object));

    // 10. If numberPart < 1 or numberPart > 12, throw a RangeError exception.
    if (number_part_integer < 1 || number_part_integer > 12)
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidMonthCode);

    // 11. If month is not undefined, and month ≠ numberPart, then
    if (!month.is_undefined() && month.as_double() != number_part_integer) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidMonthCode);
    }

    // 12. If SameValueNonNumeric(monthCode, ! BuildISOMonthCode(numberPart)) is false, then
    if (month_code_string != build_iso_month_code(number_part_integer)) {
        // a. Throw a RangeError exception.
        return vm.throw_completion<RangeError>(global_object, ErrorType::TemporalInvalidMonthCode);
    }

    // 13. Return numberPart.
    return number_part_integer;
}

// 12.2.33 ISODateFromFields ( fields, options ), https://tc39.es/proposal-temporal/#sec-temporal-isodatefromfields
ThrowCompletionOr<ISODate> iso_date_from_fields(GlobalObject& global_object, Object const& fields, Object const& options)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(fields) is Object.

    // 2. Let overflow be ? ToTemporalOverflow(options).
    auto overflow = TRY(to_temporal_overflow(global_object, &options));

    // 3. Set fields to ? PrepareTemporalFields(fields, « "day", "month", "monthCode", "year" », «»).
    auto* prepared_fields = TRY(prepare_temporal_fields(global_object, fields, { "day", "month", "monthCode", "year" }, {}));

    // 4. Let year be ! Get(fields, "year").
    auto year = MUST(prepared_fields->get(vm.names.year));

    // 5. If year is undefined, throw a TypeError exception.
    if (year.is_undefined())
        return vm.throw_completion<TypeError>(global_object, ErrorType::MissingRequiredProperty, vm.names.year.as_string());

    // 6. Let month be ? ResolveISOMonth(fields).
    auto month = TRY(resolve_iso_month(global_object, *prepared_fields));

    // 7. Let day be ! Get(fields, "day").
    auto day = MUST(prepared_fields->get(vm.names.day));

    // 8. If day is undefined, throw a TypeError exception.
    if (day.is_undefined())
        return vm.throw_completion<TypeError>(global_object, ErrorType::MissingRequiredProperty, vm.names.day.as_string());

    // 9. Return ? RegulateISODate(year, month, day, overflow).
    return regulate_iso_date(global_object, year.as_double(), month, day.as_double(), overflow);
}

// 12.2.34 ISOYearMonthFromFields ( fields, options ), https://tc39.es/proposal-temporal/#sec-temporal-isoyearmonthfromfields
ThrowCompletionOr<ISOYearMonth> iso_year_month_from_fields(GlobalObject& global_object, Object const& fields, Object const& options)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(fields) is Object.

    // 2. Let overflow be ? ToTemporalOverflow(options).
    auto overflow = TRY(to_temporal_overflow(global_object, &options));

    // 3. Set fields to ? PrepareTemporalFields(fields, « "month", "monthCode", "year" », «»).
    auto* prepared_fields = TRY(prepare_temporal_fields(global_object, fields, { "month"sv, "monthCode"sv, "year"sv }, {}));

    // 4. Let year be ! Get(fields, "year").
    auto year = MUST(prepared_fields->get(vm.names.year));

    // 5. If year is undefined, throw a TypeError exception.
    if (year.is_undefined())
        return vm.throw_completion<TypeError>(global_object, ErrorType::MissingRequiredProperty, vm.names.year.as_string());

    // 6. Let month be ? ResolveISOMonth(fields).
    auto month = TRY(resolve_iso_month(global_object, *prepared_fields));

    // 7. Let result be ? RegulateISOYearMonth(year, month, overflow).
    auto result = TRY(regulate_iso_year_month(global_object, year.as_double(), month, overflow));

    // 8. Return the Record { [[Year]]: result.[[Year]], [[Month]]: result.[[Month]], [[ReferenceISODay]]: 1 }.
    return ISOYearMonth { .year = result.year, .month = result.month, .reference_iso_day = 1 };
}

// 12.2.35 ISOMonthDayFromFields ( fields, options ), https://tc39.es/proposal-temporal/#sec-temporal-isomonthdayfromfields
ThrowCompletionOr<ISOMonthDay> iso_month_day_from_fields(GlobalObject& global_object, Object const& fields, Object const& options)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(fields) is Object.

    // 2. Let overflow be ? ToTemporalOverflow(options).
    auto overflow = TRY(to_temporal_overflow(global_object, &options));

    // 3. Set fields to ? PrepareTemporalFields(fields, « "day", "month", "monthCode", "year" », «»).
    auto* prepared_fields = TRY(prepare_temporal_fields(global_object, fields, { "day"sv, "month"sv, "monthCode"sv, "year"sv }, {}));

    // 4. Let month be ! Get(fields, "month").
    auto month_value = MUST(prepared_fields->get(vm.names.month));

    // 5. Let monthCode be ! Get(fields, "monthCode").
    auto month_code = MUST(prepared_fields->get(vm.names.monthCode));

    // 6. Let year be ! Get(fields, "year").
    auto year = MUST(prepared_fields->get(vm.names.year));

    // 7. If month is not undefined, and monthCode and year are both undefined, then
    if (!month_value.is_undefined() && month_code.is_undefined() && year.is_undefined()) {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::MissingRequiredProperty, "monthCode or year");
    }

    // 8. Set month to ? ResolveISOMonth(fields).
    auto month = TRY(resolve_iso_month(global_object, *prepared_fields));

    // 9. Let day be ! Get(fields, "day").
    auto day = MUST(prepared_fields->get(vm.names.day));

    // 10. If day is undefined, throw a TypeError exception.
    if (day.is_undefined())
        return vm.throw_completion<TypeError>(global_object, ErrorType::MissingRequiredProperty, vm.names.day.as_string());

    // 11. Let referenceISOYear be 1972 (the first leap year after the Unix epoch).
    i32 reference_iso_year = 1972;

    Optional<ISODate> result;

    // 12. If monthCode is undefined, then
    if (month_code.is_undefined()) {
        // a. Let result be ? RegulateISODate(year, month, day, overflow).
        result = TRY(regulate_iso_date(global_object, year.as_double(), month, day.as_double(), overflow));
    }
    // 13. Else,
    else {
        // a. Let result be ? RegulateISODate(referenceISOYear, month, day, overflow).
        result = TRY(regulate_iso_date(global_object, reference_iso_year, month, day.as_double(), overflow));
    }

    // 14. Return the Record { [[Month]]: result.[[Month]], [[Day]]: result.[[Day]], [[ReferenceISOYear]]: referenceISOYear }.
    return ISOMonthDay { .month = result->month, .day = result->day, .reference_iso_year = reference_iso_year };
}

// 12.2.36 ISOYear ( temporalObject ), https://tc39.es/proposal-temporal/#sec-temporal-isoyear
i32 iso_year(Object& temporal_object)
{
    // 1. Assert: temporalObject has an [[ISOYear]] internal slot.
    // NOTE: Asserted by the VERIFY_NOT_REACHED at the end

    // 2. Return 𝔽(temporalObject.[[ISOYear]]).
    if (is<PlainDate>(temporal_object))
        return static_cast<PlainDate&>(temporal_object).iso_year();
    if (is<PlainDateTime>(temporal_object))
        return static_cast<PlainDateTime&>(temporal_object).iso_year();
    if (is<PlainYearMonth>(temporal_object))
        return static_cast<PlainYearMonth&>(temporal_object).iso_year();
    if (is<PlainMonthDay>(temporal_object))
        return static_cast<PlainMonthDay&>(temporal_object).iso_year();
    VERIFY_NOT_REACHED();
}

// 12.2.37 ISOMonth ( temporalObject ), https://tc39.es/proposal-temporal/#sec-temporal-isomonth
u8 iso_month(Object& temporal_object)
{
    // 1. Assert: temporalObject has an [[ISOMonth]] internal slot.
    // NOTE: Asserted by the VERIFY_NOT_REACHED at the end

    // 2. Return 𝔽(temporalObject.[[ISOMonth]]).
    if (is<PlainDate>(temporal_object))
        return static_cast<PlainDate&>(temporal_object).iso_month();
    if (is<PlainDateTime>(temporal_object))
        return static_cast<PlainDateTime&>(temporal_object).iso_month();
    if (is<PlainYearMonth>(temporal_object))
        return static_cast<PlainYearMonth&>(temporal_object).iso_month();
    if (is<PlainMonthDay>(temporal_object))
        return static_cast<PlainMonthDay&>(temporal_object).iso_month();
    VERIFY_NOT_REACHED();
}

// 12.2.38 ISOMonthCode ( temporalObject ), https://tc39.es/proposal-temporal/#sec-temporal-isomonthcode
String iso_month_code(Object& temporal_object)
{
    // 1. Assert: temporalObject has an [[ISOMonth]] internal slot.
    // NOTE: Asserted by the VERIFY_NOT_REACHED at the end

    // 2. Return ! BuildISOMonthCode(temporalObject.[[ISOMonth]]).
    if (is<PlainDate>(temporal_object))
        return build_iso_month_code(static_cast<PlainDate&>(temporal_object).iso_month());
    if (is<PlainDateTime>(temporal_object))
        return build_iso_month_code(static_cast<PlainDateTime&>(temporal_object).iso_month());
    if (is<PlainYearMonth>(temporal_object))
        return build_iso_month_code(static_cast<PlainYearMonth&>(temporal_object).iso_month());
    if (is<PlainMonthDay>(temporal_object))
        return build_iso_month_code(static_cast<PlainMonthDay&>(temporal_object).iso_month());
    VERIFY_NOT_REACHED();
}

// 12.2.49 ISODay ( temporalObject ), https://tc39.es/proposal-temporal/#sec-temporal-isomonthcode
u8 iso_day(Object& temporal_object)
{
    // 1. Assert: temporalObject has an [[ISODay]] internal slot.
    // NOTE: Asserted by the VERIFY_NOT_REACHED at the end

    // 2. Return 𝔽(temporalObject.[[ISODay]]).
    if (is<PlainDate>(temporal_object))
        return static_cast<PlainDate&>(temporal_object).iso_day();
    if (is<PlainDateTime>(temporal_object))
        return static_cast<PlainDateTime&>(temporal_object).iso_day();
    if (is<PlainYearMonth>(temporal_object))
        return static_cast<PlainYearMonth&>(temporal_object).iso_day();
    if (is<PlainMonthDay>(temporal_object))
        return static_cast<PlainMonthDay&>(temporal_object).iso_day();
    VERIFY_NOT_REACHED();
}

// 12.2.40 DefaultMergeFields ( fields, additionalFields ), https://tc39.es/proposal-temporal/#sec-temporal-defaultmergefields
ThrowCompletionOr<Object*> default_merge_fields(GlobalObject& global_object, Object const& fields, Object const& additional_fields)
{
    auto& vm = global_object.vm();

    // 1. Let merged be OrdinaryObjectCreate(%Object.prototype%).
    auto* merged = Object::create(global_object, global_object.object_prototype());

    // 2. Let originalKeys be ? EnumerableOwnPropertyNames(fields, key).
    auto original_keys = TRY(fields.enumerable_own_property_names(Object::PropertyKind::Key));

    // 3. For each element nextKey of originalKeys, do
    for (auto& next_key : original_keys) {
        // a. If nextKey is not "month" or "monthCode", then
        if (next_key.as_string().string() != vm.names.month.as_string() && next_key.as_string().string() != vm.names.monthCode.as_string()) {
            auto property_key = MUST(PropertyKey::from_value(global_object, next_key));

            // i. Let propValue be ? Get(fields, nextKey).
            auto prop_value = TRY(fields.get(property_key));

            // ii. If propValue is not undefined, then
            if (!prop_value.is_undefined()) {
                // 1. Perform ! CreateDataPropertyOrThrow(merged, nextKey, propValue).
                MUST(merged->create_data_property_or_throw(property_key, prop_value));
            }
        }
    }

    // 4. Let newKeys be ? EnumerableOwnPropertyNames(additionalFields, key).
    auto new_keys = TRY(additional_fields.enumerable_own_property_names(Object::PropertyKind::Key));

    // IMPLEMENTATION DEFINED: This is an optimization, so we don't have to iterate new_keys three times (worst case), but only once.
    bool new_keys_contains_month_or_month_code_property = false;

    // 5. For each element nextKey of newKeys, do
    for (auto& next_key : new_keys) {
        auto property_key = MUST(PropertyKey::from_value(global_object, next_key));

        // a. Let propValue be ? Get(additionalFields, nextKey).
        auto prop_value = TRY(additional_fields.get(property_key));

        // b. If propValue is not undefined, then
        if (!prop_value.is_undefined()) {
            // i. Perform ! CreateDataPropertyOrThrow(merged, nextKey, propValue).
            MUST(merged->create_data_property_or_throw(property_key, prop_value));
        }

        // See comment above.
        new_keys_contains_month_or_month_code_property |= next_key.as_string().string() == vm.names.month.as_string() || next_key.as_string().string() == vm.names.monthCode.as_string();
    }

    // 6. If newKeys does not contain either "month" or "monthCode", then
    if (!new_keys_contains_month_or_month_code_property) {
        // a. Let month be ? Get(fields, "month").
        auto month = TRY(fields.get(vm.names.month));

        // b. If month is not undefined, then
        if (!month.is_undefined()) {
            // i. Perform ! CreateDataPropertyOrThrow(merged, "month", month).
            MUST(merged->create_data_property_or_throw(vm.names.month, month));
        }

        // c. Let monthCode be ? Get(fields, "monthCode").
        auto month_code = TRY(fields.get(vm.names.monthCode));

        // d. If monthCode is not undefined, then
        if (!month_code.is_undefined()) {
            // i. Perform ! CreateDataPropertyOrThrow(merged, "monthCode", monthCode).
            MUST(merged->create_data_property_or_throw(vm.names.monthCode, month_code));
        }
    }

    // 7. Return merged.
    return merged;
}

}
