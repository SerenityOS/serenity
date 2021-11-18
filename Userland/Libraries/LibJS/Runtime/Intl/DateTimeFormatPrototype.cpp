/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DateTimeFormatPrototype.h>

namespace JS::Intl {

// 11.4 Properties of the Intl.DateTimeFormat Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-datetimeformat-prototype-object
DateTimeFormatPrototype::DateTimeFormatPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void DateTimeFormatPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 11.4.2 Intl.DateTimeFormat.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-intl.datetimeformat.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.DateTimeFormat"), Attribute::Configurable);
}

}
