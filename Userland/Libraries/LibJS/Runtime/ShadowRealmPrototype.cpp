/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ShadowRealmPrototype.h>

namespace JS {

// 3.4 Properties of the ShadowRealm Prototype Object, https://tc39.es/proposal-shadowrealm/#sec-properties-of-the-shadowrealm-prototype-object
ShadowRealmPrototype::ShadowRealmPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void ShadowRealmPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    // 3.4.3 ShadowRealm.prototype [ @@toStringTag ], https://tc39.es/proposal-shadowrealm/#sec-shadowrealm.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.ShadowRealm.as_string()), Attribute::Configurable);
}

}
