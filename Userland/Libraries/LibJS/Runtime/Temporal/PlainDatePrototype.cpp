/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDatePrototype.h>
#include <LibJS/Runtime/Temporal/PlainDateTime.h>
#include <LibJS/Runtime/Temporal/PlainMonthDay.h>
#include <LibJS/Runtime/Temporal/PlainTime.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>

namespace JS::Temporal {

// 3.3 Properties of the Temporal.PlainDate Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plaindate-prototype-object
PlainDatePrototype::PlainDatePrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void PlainDatePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 3.3.2 Temporal.PlainDate.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.PlainDate"), Attribute::Configurable);

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.year, year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.month, month_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.monthCode, month_code_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.day, day_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.dayOfWeek, day_of_week_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.dayOfYear, day_of_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.weekOfYear, week_of_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInWeek, days_in_week_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInMonth, days_in_month_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.daysInYear, days_in_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.monthsInYear, months_in_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.inLeapYear, in_leap_year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.era, era_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.eraYear, era_year_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.toPlainYearMonth, to_plain_year_month, 0, attr);
    define_native_function(vm.names.toPlainMonthDay, to_plain_month_day, 0, attr);
    define_native_function(vm.names.getISOFields, get_iso_fields, 0, attr);
    define_native_function(vm.names.withCalendar, with_calendar, 1, attr);
    define_native_function(vm.names.equals, equals, 1, attr);
    define_native_function(vm.names.toPlainDateTime, to_plain_date_time, 0, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 0, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

// 3.3.3 get Temporal.PlainDate.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::calendar_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return temporalDate.[[Calendar]].
    return Value(&temporal_date->calendar());
}

// 3.3.4 get Temporal.PlainDate.prototype.year, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.year
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::year_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarYear(calendar, temporalDate).
    return Value(calendar_year(global_object, calendar, *temporal_date));
}

// 3.3.5 get Temporal.PlainDate.prototype.month, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.month
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::month_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarMonth(calendar, temporalDate).
    return Value(calendar_month(global_object, calendar, *temporal_date));
}

// 3.3.6 get Temporal.PlainDate.prototype.monthCode, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.monthCode
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::month_code_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarMonthCode(calendar, temporalDate).
    return js_string(vm, calendar_month_code(global_object, calendar, *temporal_date));
}

// 3.3.7 get Temporal.PlainDate.prototype.day, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.day
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::day_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarDay(calendar, temporalDate).
    return Value(calendar_day(global_object, calendar, *temporal_date));
}

// 3.3.8 get Temporal.PlainDate.prototype.dayOfWeek, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.dayofweek
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::day_of_week_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // Return ? CalendarDayOfWeek(calendar, temporalDate).
    return Value(calendar_day_of_week(global_object, calendar, *temporal_date));
}

// 3.3.9 get Temporal.PlainDate.prototype.dayOfYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.dayofyear
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::day_of_year_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarDayOfYear(calendar, temporalDate).
    return Value(calendar_day_of_year(global_object, calendar, *temporal_date));
}

// 3.3.10 get Temporal.PlainDate.prototype.weekOfYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.weekofyear
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::week_of_year_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // Return ? CalendarWeekOfYear(calendar, temporalDate).
    return Value(calendar_week_of_year(global_object, calendar, *temporal_date));
}

// 3.3.11 get Temporal.PlainDate.prototype.daysInWeek, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.daysinweek
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::days_in_week_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarDaysInWeek(calendar, temporalDate).
    return Value(calendar_days_in_week(global_object, calendar, *temporal_date));
}

// 3.3.12 get Temporal.PlainDate.prototype.daysInMonth, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.daysinmonth
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::days_in_month_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarDaysInMonth(calendar, temporalDate).
    return Value(calendar_days_in_month(global_object, calendar, *temporal_date));
}

// 3.3.13 get Temporal.PlainDate.prototype.daysInYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.daysinyear
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::days_in_year_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarDaysInYear(calendar, temporalDate).
    return Value(calendar_days_in_year(global_object, calendar, *temporal_date));
}

// 3.3.14 get Temporal.PlainDate.prototype.monthsInYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.monthsinyear
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::months_in_year_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarMonthsInYear(calendar, temporalDate).
    return Value(calendar_months_in_year(global_object, calendar, *temporal_date));
}

// 3.3.15 get Temporal.PlainDate.prototype.inLeapYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.inleapyear
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::in_leap_year_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarInLeapYear(calendar, temporalDate).
    return Value(calendar_in_leap_year(global_object, calendar, *temporal_date));
}

// 15.6.5.2 get Temporal.PlainDate.prototype.era, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.era
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::era_getter)
{
    // 1. Let plainDate be the this value.
    // 2. Perform ? RequireInternalSlot(plainDate, [[InitializedTemporalDate]]).
    auto* plain_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be plainDate.[[Calendar]].
    auto& calendar = plain_date->calendar();

    // 4. Return ? CalendarEra(calendar, plainDate).
    return calendar_era(global_object, calendar, *plain_date);
}

// 15.6.5.3 get Temporal.PlainDate.prototype.eraYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.erayear
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::era_year_getter)
{
    // 1. Let plainDate be the this value.
    // 2. Perform ? RequireInternalSlot(plainDate, [[InitializedTemporalDate]]).
    auto* plain_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be plainDate.[[Calendar]].
    auto& calendar = plain_date->calendar();

    // 4. Return ? CalendarEraYear(calendar, plainDate).
    return calendar_era_year(global_object, calendar, *plain_date);
}

// 3.3.16 Temporal.PlainDate.prototype.toPlainYearMonth ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.toplainyearmonth
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::to_plain_year_month)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Let fieldNames be ? CalendarFields(calendar, « "monthCode", "year" »).
    auto field_names = calendar_fields(global_object, calendar, { "monthCode"sv, "year"sv });
    if (vm.exception())
        return {};

    // 5. Let fields be ? PrepareTemporalFields(temporalDate, fieldNames, «»).
    auto* fields = TRY_OR_DISCARD(prepare_temporal_fields(global_object, *temporal_date, field_names, {}));

    // 6. Return ? YearMonthFromFields(calendar, fields).
    return year_month_from_fields(global_object, calendar, *fields);
}

// 3.3.17 Temporal.PlainDate.prototype.toPlainMonthDay ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.toplainmonthday
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::to_plain_month_day)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Let fieldNames be ? CalendarFields(calendar, « "day", "monthCode" »).
    auto field_names = calendar_fields(global_object, calendar, { "day"sv, "monthCode"sv });
    if (vm.exception())
        return {};

    // 5. Let fields be ? PrepareTemporalFields(temporalDate, fieldNames, «»).
    auto* fields = TRY_OR_DISCARD(prepare_temporal_fields(global_object, *temporal_date, field_names, {}));

    // 6. Return ? MonthDayFromFields(calendar, fields).
    return month_day_from_fields(global_object, calendar, *fields);
}

// 3.3.18 Temporal.PlainDate.prototype.getISOFields ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.getisofields
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::get_iso_fields)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let fields be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* fields = Object::create(global_object, global_object.object_prototype());

    // 4. Perform ! CreateDataPropertyOrThrow(fields, "calendar", temporalDate.[[Calendar]]).
    fields->create_data_property_or_throw(vm.names.calendar, Value(&temporal_date->calendar()));

    // 5. Perform ! CreateDataPropertyOrThrow(fields, "isoDay", 𝔽(temporalDate.[[ISODay]])).
    fields->create_data_property_or_throw(vm.names.isoDay, Value(temporal_date->iso_day()));

    // 6. Perform ! CreateDataPropertyOrThrow(fields, "isoMonth", 𝔽(temporalDate.[[ISOMonth]])).
    fields->create_data_property_or_throw(vm.names.isoMonth, Value(temporal_date->iso_month()));

    // 7. Perform ! CreateDataPropertyOrThrow(fields, "isoYear", 𝔽(temporalDate.[[ISOYear]])).
    fields->create_data_property_or_throw(vm.names.isoYear, Value(temporal_date->iso_year()));

    // 8. Return fields.
    return fields;
}

// 3.3.22 Temporal.PlainDate.prototype.withCalendar ( calendar ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.withcalendar
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::with_calendar)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be ? ToTemporalCalendar(calendar).
    auto calendar = to_temporal_calendar(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. Return ? CreateTemporalDate(temporalDate.[[ISOYear]], temporalDate.[[ISOMonth]], temporalDate.[[ISODay]], calendar).
    return create_temporal_date(global_object, temporal_date->iso_year(), temporal_date->iso_month(), temporal_date->iso_day(), *calendar);
}

// 3.3.25 Temporal.PlainDate.prototype.equals ( other ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.equals
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::equals)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Set other to ? ToTemporalDate(other).
    auto* other = to_temporal_date(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. If temporalDate.[[ISOYear]] ≠ other.[[ISOYear]], return false.
    if (temporal_date->iso_year() != other->iso_year())
        return Value(false);
    // 5. If temporalDate.[[ISOMonth]] ≠ other.[[ISOMonth]], return false.
    if (temporal_date->iso_month() != other->iso_month())
        return Value(false);
    // 6. If temporalDate.[[ISODay]] ≠ other.[[ISODay]], return false.
    if (temporal_date->iso_day() != other->iso_day())
        return Value(false);
    // 7. Return ? CalendarEquals(temporalDate.[[Calendar]], other.[[Calendar]]).
    return Value(calendar_equals(global_object, temporal_date->calendar(), other->calendar()));
}

// 3.3.26 Temporal.PlainDate.prototype.toPlainDateTime ( [ temporalTime ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.toplaindatetime
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::to_plain_date_time)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. If temporalTime is undefined, then
    if (vm.argument(0).is_undefined()) {
        // a. Return ? CreateTemporalDateTime(temporalDate.[[ISOYear]], temporalDate.[[ISOMonth]], temporalDate.[[ISODay]], 0, 0, 0, 0, 0, 0, temporalDate.[[Calendar]]).
        return create_temporal_date_time(global_object, temporal_date->iso_year(), temporal_date->iso_month(), temporal_date->iso_day(), 0, 0, 0, 0, 0, 0, temporal_date->calendar());
    }

    // 4. Set temporalTime to ? ToTemporalTime(temporalTime).
    auto* temporal_time = TRY_OR_DISCARD(to_temporal_time(global_object, vm.argument(0)));

    // 5. Return ? CreateTemporalDateTime(temporalDate.[[ISOYear]], temporalDate.[[ISOMonth]], temporalDate.[[ISODay]], temporalTime.[[ISOHour]], temporalTime.[[ISOMinute]], temporalTime.[[ISOSecond]], temporalTime.[[ISOMillisecond]], temporalTime.[[ISOMicrosecond]], temporalTime.[[ISONanosecond]], temporalDate.[[Calendar]]).
    return create_temporal_date_time(global_object, temporal_date->iso_year(), temporal_date->iso_month(), temporal_date->iso_day(), temporal_time->iso_hour(), temporal_time->iso_minute(), temporal_time->iso_second(), temporal_time->iso_millisecond(), temporal_time->iso_microsecond(), temporal_time->iso_nanosecond(), temporal_date->calendar());
}

// 3.3.28 Temporal.PlainDate.prototype.toString ( [ options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::to_string)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Set options to ? GetOptionsObject(options).
    auto* options = TRY_OR_DISCARD(get_options_object(global_object, vm.argument(0)));

    // 4. Let showCalendar be ? ToShowCalendarOption(options).
    auto show_calendar = TRY_OR_DISCARD(to_show_calendar_option(global_object, *options));

    // 5. Return ? TemporalDateToString(temporalDate, showCalendar).
    auto string = temporal_date_to_string(global_object, *temporal_date, show_calendar);
    if (vm.exception())
        return {};

    return js_string(vm, *string);
}

// 3.3.29 Temporal.PlainDate.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.tolocalestring
// NOTE: This is the minimum toLocaleString implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::to_locale_string)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return ? TemporalDateToString(temporalDate, "auto").
    auto string = temporal_date_to_string(global_object, *temporal_date, "auto"sv);
    if (vm.exception())
        return {};

    return js_string(vm, *string);
}

// 3.3.30 Temporal.PlainDate.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::to_json)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this_object(global_object);
    if (vm.exception())
        return {};

    // 3. Return ? TemporalDateToString(temporalDate, "auto").
    auto string = temporal_date_to_string(global_object, *temporal_date, "auto"sv);
    if (vm.exception())
        return {};

    return js_string(vm, *string);
}

// 3.3.31 Temporal.PlainDate.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainDate", "a primitive value");
    return {};
}

}
