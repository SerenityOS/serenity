/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Calendar.h>
#include <LibJS/Runtime/Temporal/CalendarPrototype.h>

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
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.Calendar"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.toString, to_string, 0, attr);
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

}
