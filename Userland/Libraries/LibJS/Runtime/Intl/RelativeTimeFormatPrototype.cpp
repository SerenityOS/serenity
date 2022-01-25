/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormatPrototype.h>

namespace JS::Intl {

// 17.4 Properties of the Intl.RelativeTimeFormat Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-relativetimeformat-prototype-object
RelativeTimeFormatPrototype::RelativeTimeFormatPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void RelativeTimeFormatPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 17.4.2 Intl.RelativeTimeFormat.prototype[ @@toStringTag ], https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat.prototype-toStringTag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.RelativeTimeFormat"sv), Attribute::Configurable);
}

}
