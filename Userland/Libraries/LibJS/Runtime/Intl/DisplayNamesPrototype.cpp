/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DisplayNames.h>
#include <LibJS/Runtime/Intl/DisplayNamesPrototype.h>

namespace JS::Intl {

// 12.4 Properties of the Intl.DisplayNames Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-displaynames-prototype-object
DisplayNamesPrototype::DisplayNamesPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void DisplayNamesPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 12.4.2 Intl.DisplayNames.prototype[ @@toStringTag ], https://tc39.es/ecma402/#sec-Intl.DisplayNames.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.DisplayNames"), Attribute::Configurable);
}

}
