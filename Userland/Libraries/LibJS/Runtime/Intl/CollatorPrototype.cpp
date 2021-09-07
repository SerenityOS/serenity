/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/CollatorPrototype.h>

namespace JS::Intl {

// 10.3 Properties of the Intl.Collator Prototype Object, https://tc39.es/ecma402/#sec-properties-of-the-intl-collator-prototype-object
CollatorPrototype::CollatorPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void CollatorPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 10.3.2 Intl.Collator.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-intl.collator.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.Collator"), Attribute::Configurable);
}

}
