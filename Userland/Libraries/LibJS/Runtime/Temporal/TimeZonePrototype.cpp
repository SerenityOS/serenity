/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/TimeZonePrototype.h>

namespace JS::Temporal {

// 11.4 Properties of the Temporal.TimeZone Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-timezone-prototype-object
TimeZonePrototype::TimeZonePrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void TimeZonePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 11.4.2 Temporal.TimeZone.prototype[ @@toStringTag ], https://tc39.es/proposal-temporal/#sec-temporal.timezone.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.TimeZone"), Attribute::Configurable);
}

}
