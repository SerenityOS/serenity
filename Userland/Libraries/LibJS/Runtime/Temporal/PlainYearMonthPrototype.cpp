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
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.PlainYearMonth"), Attribute::Configurable);

    define_native_accessor(vm.names.calendar, calendar_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.year, year_getter, {}, Attribute::Configurable);
    define_native_accessor(vm.names.month, month_getter, {}, Attribute::Configurable);
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
    // 1. Let plainYearMonth be the this value.
    // 2. Perform ? RequireInternalSlot(plainYearMonth, [[InitializedTemporalYearMonth]]).
    auto* plain_year_month = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return plainYearMonth.[[Calendar]].
    return Value(&plain_year_month->calendar());
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

}
