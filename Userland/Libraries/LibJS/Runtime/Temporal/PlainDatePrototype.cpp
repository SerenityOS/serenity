/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
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
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Temporal.PlainDate"), Attribute::Configurable);
}

}
