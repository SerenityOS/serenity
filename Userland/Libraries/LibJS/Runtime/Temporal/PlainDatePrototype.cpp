/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainDate.h>
#include <LibJS/Runtime/Temporal/PlainDatePrototype.h>

namespace JS::Temporal {

// 3.3 Properties of the Temporal.PlainDate Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plaindate-prototype-object
PlainDatePrototype::PlainDatePrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
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

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.getISOFields, get_iso_fields, 0, attr);
    define_native_function(vm.names.withCalendar, with_calendar, 1, attr);
    define_native_function(vm.names.equals, equals, 1, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

static PlainDate* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<PlainDate>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.PlainDate");
        return {};
    }
    return static_cast<PlainDate*>(this_object);
}

// 3.3.3 get Temporal.PlainDate.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plaindate.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::calendar_getter)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be temporalDate.[[Calendar]].
    auto& calendar = temporal_date->calendar();

    // 4. Return ? CalendarInLeapYear(calendar, temporalDate).
    return Value(calendar_in_leap_year(global_object, calendar, *temporal_date));
}

// 3.3.18 Temporal.PlainDate.prototype.getISOFields ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.getisofields
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::get_iso_fields)
{
    // 1. Let temporalDate be the this value.
    // 2. Perform ? RequireInternalSlot(temporalDate, [[InitializedTemporalDate]]).
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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
    auto* temporal_date = typed_this(global_object);
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

// 3.3.31 Temporal.PlainDate.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plaindate.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainDatePrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainDate", "a primitive value");
    return {};
}

}
