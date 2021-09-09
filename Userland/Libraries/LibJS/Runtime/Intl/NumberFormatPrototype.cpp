/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/NumberFormatPrototype.h>

namespace JS::Intl {

// 15.4 Properties of the Intl.NumberFormat Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-numberformat-prototype-object
NumberFormatPrototype::NumberFormatPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void NumberFormatPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 15.4.2 Intl.NumberFormat.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-intl.numberformat.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.NumberFormat"), Attribute::Configurable);
}

}
