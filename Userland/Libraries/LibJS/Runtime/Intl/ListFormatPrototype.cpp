/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/ListFormatPrototype.h>

namespace JS::Intl {

// 13.4 Properties of the Intl.ListFormat Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-listformat-prototype-object
ListFormatPrototype::ListFormatPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void ListFormatPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 13.4.2 Intl.ListFormat.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-Intl.ListFormat.prototype-toStringTag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.ListFormat"), Attribute::Configurable);
}

}
