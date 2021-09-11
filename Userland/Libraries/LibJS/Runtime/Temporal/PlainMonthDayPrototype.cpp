/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainMonthDay.h>
#include <LibJS/Runtime/Temporal/PlainMonthDayPrototype.h>

namespace JS::Temporal {

// 10.3 Properties of the Temporal.PlainMonthDay Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plainmonthday-prototype-object
PlainMonthDayPrototype::PlainMonthDayPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void PlainMonthDayPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 10.3.2 Temporal.PlainMonthDay.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Temporal.PlainMonthDay"), Attribute::Configurable);

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.monthCode, month_code_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.day, day_getter, {}, Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.equals, equals, 1, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.toJSON, to_json, 0, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
    define_native_function(vm.names.getISOFields, get_iso_fields, 0, attr);
}

static PlainMonthDay* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<PlainMonthDay>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Temporal.PlainMonthDay");
        return {};
    }
    return static_cast<PlainMonthDay*>(this_object);
}

// 10.3.3 get Temporal.PlainMonthDay.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plainmonthday.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayPrototype::calendar_getter)
{
    // 1. Let monthDay be the this value.
    // 2. Perform ? RequireInternalSlot(monthDay, [[InitializedTemporalMonthDay]]).
    auto* month_day = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return monthDay.[[Calendar]].
    return Value(&month_day->calendar());
}

// 10.3.4 get Temporal.PlainMonthDay.prototype.monthCode, https://tc39.es/proposal-temporal/#sec-get-temporal.plainmonthday.prototype.monthcode
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayPrototype::month_code_getter)
{
    // 1. Let monthDay be the this value.
    // 2. Perform ? RequireInternalSlot(monthDay, [[InitializedTemporalMonthDay]]).
    auto* month_day = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be monthDay.[[Calendar]].
    auto& calendar = month_day->calendar();

    // 4. Return ? CalendarMonthCode(calendar, monthDay).
    return js_string(vm, calendar_month_code(global_object, calendar, *month_day));
}

// 10.3.5 get Temporal.PlainMonthDay.prototype.day, https://tc39.es/proposal-temporal/#sec-get-temporal.plainmonthday.prototype.day
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayPrototype::day_getter)
{
    // 1. Let monthDay be the this value.
    // 2. Perform ? RequireInternalSlot(monthDay, [[InitializedTemporalMonthDay]]).
    auto* month_day = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be monthDay.[[Calendar]].
    auto& calendar = month_day->calendar();

    // 4. Return 𝔽(? CalendarDay(calendar, monthDay)).
    return Value(calendar_day(global_object, calendar, *month_day));
}

// 10.3.7 Temporal.PlainMonthDay.prototype.equals ( other ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday.prototype.equals
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayPrototype::equals)
{
    // 1. Let monthDay be the this value.
    // 2. Perform ? RequireInternalSlot(monthDay, [[InitializedTemporalMonthDay]]).
    auto* month_day = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Set other to ? ToTemporalMonthDay(other).
    auto* other = to_temporal_month_day(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. If monthDay.[[ISOMonth]] ≠ other.[[ISOMonth]], return false.
    if (month_day->iso_month() != other->iso_month())
        return Value(false);

    // 5. If monthDay.[[ISODay]] ≠ other.[[ISODay]], return false.
    if (month_day->iso_day() != other->iso_day())
        return Value(false);

    // 6. If monthDay.[[ISOYear]] ≠ other.[[ISOYear]], return false.
    if (month_day->iso_year() != other->iso_year())
        return Value(false);

    // 7. Return ? CalendarEquals(monthDay.[[Calendar]], other.[[Calendar]]).
    return Value(calendar_equals(global_object, month_day->calendar(), other->calendar()));
}

// 10.3.8 Temporal.PlainMonthDay.prototype.toString ( [ options ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayPrototype::to_string)
{
    // 1. Let monthDay be the this value.
    // 2. Perform ? RequireInternalSlot(monthDay, [[InitializedTemporalMonthDay]]).
    auto* month_day = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Set options to ? GetOptionsObject(options).
    auto* options = get_options_object(global_object, vm.argument(0));
    if (vm.exception())
        return {};

    // 4. Let showCalendar be ? ToShowCalendarOption(options).
    auto show_calendar = to_show_calendar_option(global_object, *options);
    if (vm.exception())
        return {};

    // 5. Return ? TemporalMonthDayToString(monthDay, showCalendar).
    auto string = temporal_month_day_to_string(global_object, *month_day, *show_calendar);
    if (vm.exception())
        return {};

    return js_string(vm, *string);
}

// 10.3.9 Temporal.PlainMonthDay.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday.prototype.tolocalestring
// NOTE: This is the minimum toLocaleString implementation for engines without ECMA-402.
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayPrototype::to_locale_string)
{
    // 1. Let monthDay be the this value.
    // 2. Perform ? RequireInternalSlot(monthDay, [[InitializedTemporalMonthDay]]).
    auto* month_day = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return ? TemporalMonthDayToString(monthDay, "auto").
    auto string = temporal_month_day_to_string(global_object, *month_day, "auto"sv);
    if (vm.exception())
        return {};

    return js_string(vm, *string);
}

// 10.3.10 Temporal.PlainMonthDay.prototype.toJSON ( ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday.prototype.tojson
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayPrototype::to_json)
{
    // 1. Let monthDay be the this value.
    // 2. Perform ? RequireInternalSlot(monthDay, [[InitializedTemporalMonthDay]]).
    auto* month_day = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return ? TemporalMonthDayToString(monthDay, "auto").
    auto string = temporal_month_day_to_string(global_object, *month_day, "auto"sv);
    if (vm.exception())
        return {};

    return js_string(vm, *string);
}

// 10.3.11 Temporal.PlainMonthDay.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayPrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainMonthDay", "a primitive value");
    return {};
}

// 10.3.13 Temporal.PlainMonthDay.prototype.getISOFields ( ), https://tc39.es/proposal-temporal/#sec-temporal.plainmonthday.prototype.getisofields
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayPrototype::get_iso_fields)
{
    // 1. Let monthDay be the this value.
    // 2. Perform ? RequireInternalSlot(monthDay, [[InitializedTemporalMonthDay]]).
    auto* month_day = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let fields be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* fields = Object::create(global_object, global_object.object_prototype());

    // 4. Perform ! CreateDataPropertyOrThrow(fields, "calendar", monthDay.[[Calendar]]).
    fields->create_data_property_or_throw(vm.names.calendar, Value(&month_day->calendar()));

    // 5. Perform ! CreateDataPropertyOrThrow(fields, "isoDay", 𝔽(monthDay.[[ISODay]])).
    fields->create_data_property_or_throw(vm.names.isoDay, Value(month_day->iso_day()));

    // 6. Perform ! CreateDataPropertyOrThrow(fields, "isoMonth", 𝔽(monthDay.[[ISOMonth]])).
    fields->create_data_property_or_throw(vm.names.isoMonth, Value(month_day->iso_month()));

    // 7. Perform ! CreateDataPropertyOrThrow(fields, "isoYear", 𝔽(monthDay.[[ISOYear]])).
    fields->create_data_property_or_throw(vm.names.isoYear, Value(month_day->iso_year()));

    // 8. Return fields.
    return fields;
}

}
