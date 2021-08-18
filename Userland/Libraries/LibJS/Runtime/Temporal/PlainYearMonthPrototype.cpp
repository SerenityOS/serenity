/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/PlainYearMonth.h>
#include <LibJS/Runtime/Temporal/PlainYearMonthPrototype.h>

namespace JS::Temporal {

// 9.3 Properties of the Temporal.PlainYearMonth Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-plainyearmonth-prototype-object
PlainYearMonthPrototype::PlainYearMonthPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
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

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.valueOf, value_of, 0, attr);
    define_native_function(vm.names.getISOFields, get_iso_fields, 0, attr);
}

static PlainYearMonth* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<PlainYearMonth>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.PlainYearMonth");
        return {};
    }
    return static_cast<PlainYearMonth*>(this_object);
}

// 9.3.3 get Temporal.PlainYearMonth.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::calendar_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return yearMonth.[[Calendar]].
    return Value(&year_month->calendar());
}

// 9.3.4 get Temporal.PlainYearMonth.prototype.year, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.year
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::year_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return 𝔽(? CalendarYear(calendar, yearMonth)).
    return Value(calendar_year(global_object, calendar, *year_month));
}

// 9.3.5 get Temporal.PlainYearMonth.prototype.month, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.month
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::month_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return 𝔽(? CalendarMonth(calendar, yearMonth)).
    return Value(calendar_month(global_object, calendar, *year_month));
}

// 9.3.6 get Temporal.PlainYearMonth.prototype.monthCode, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.monthCode
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::month_code_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return ? CalendarMonthCode(calendar, yearMonth).
    return js_string(vm, calendar_month_code(global_object, calendar, *year_month));
}

// 9.3.7 get Temporal.PlainYearMonth.prototype.daysInYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.daysinyear
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::days_in_year_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return ? CalendarDaysInYear(calendar, yearMonth).
    return Value(calendar_days_in_year(global_object, calendar, *year_month));
}

// 9.3.8 get Temporal.PlainYearMonth.prototype.daysInMonth, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.daysinmonth
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::days_in_month_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return ? CalendarDaysInMonth(calendar, yearMonth).
    return Value(calendar_days_in_month(global_object, calendar, *year_month));
}

// 9.3.9 get Temporal.PlainYearMonth.prototype.monthsInYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.monthsinyear
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::months_in_year_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return ? CalendarMonthsInYear(calendar, yearMonth).
    return Value(calendar_months_in_year(global_object, calendar, *year_month));
}

// 9.3.10 get Temporal.PlainYearMonth.prototype.inLeapYear, https://tc39.es/proposal-temporal/#sec-get-temporal.plainyearmonth.prototype.inleapyear
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::in_leap_year_getter)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let calendar be yearMonth.[[Calendar]].
    auto& calendar = year_month->calendar();

    // 4. Return ? CalendarInLeapYear(calendar, yearMonth).
    return Value(calendar_in_leap_year(global_object, calendar, *year_month));
}

// 9.3.20 Temporal.PlainYearMonth.prototype.valueOf ( ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::value_of)
{
    // 1. Throw a TypeError exception.
    vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "Temporal.PlainYearMonth", "a primitive value");
    return {};
}

// 9.3.22 Temporal.PlainYearMonth.prototype.getISOFields ( ), https://tc39.es/proposal-temporal/#sec-temporal.plainyearmonth.prototype.getisofields
JS_DEFINE_NATIVE_FUNCTION(PlainYearMonthPrototype::get_iso_fields)
{
    // 1. Let yearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(yearMonth, [[InitializedTemporalYearMonth]]).
    auto* year_month = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let fields be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* fields = Object::create(global_object, global_object.object_prototype());

    // 4. Perform ! CreateDataPropertyOrThrow(fields, "calendar", yearMonth.[[Calendar]]).
    fields->create_data_property_or_throw(vm.names.calendar, Value(&year_month->calendar()));

    // 5. Perform ! CreateDataPropertyOrThrow(fields, "isoDay", 𝔽(yearMonth.[[ISODay]])).
    fields->create_data_property_or_throw(vm.names.isoDay, Value(year_month->iso_day()));

    // 6. Perform ! CreateDataPropertyOrThrow(fields, "isoMonth", 𝔽(yearMonth.[[ISOMonth]])).
    fields->create_data_property_or_throw(vm.names.isoMonth, Value(year_month->iso_month()));

    // 7. Perform ! CreateDataPropertyOrThrow(fields, "isoYear", 𝔽(yearMonth.[[ISOYear]])).
    fields->create_data_property_or_throw(vm.names.isoYear, Value(year_month->iso_year()));

    // 8. Return fields.
    return fields;
}

}
