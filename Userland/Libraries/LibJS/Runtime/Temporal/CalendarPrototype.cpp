/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
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
}

}
