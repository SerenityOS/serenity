/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
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
}

static PlainMonthDay* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<PlainMonthDay>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Temporal.PlainMonthDay");
        return {};
    }
    return static_cast<PlainMonthDay*>(this_object);
}

// 10.3.3 get Temporal.PlainMonthDay.prototype.calendar, https://tc39.es/proposal-temporal/#sec-get-temporal.plainmonthday.prototype.calendar
JS_DEFINE_NATIVE_FUNCTION(PlainMonthDayPrototype::calendar_getter)
{
    // 1. Let plainMonthDay be the this value.
    // 2. Perform ? RequireInternalSlot(plainMonthDay, [[InitializedTemporalMonthDay]]).
    auto* plain_month_day = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Return plainMonthDay.[[Calendar]].
    return Value(&plain_month_day->calendar());
}

}
