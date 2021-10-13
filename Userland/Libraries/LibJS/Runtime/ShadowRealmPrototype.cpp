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
    Object::initialize(global_object);
}

}
