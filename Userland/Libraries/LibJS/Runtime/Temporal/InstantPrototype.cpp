/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/InstantPrototype.h>

namespace JS::Temporal {

// 8.3 Properties of the Temporal.Instant Prototype Object, https://tc39.es/proposal-temporal/#sec-properties-of-the-temporal-instant-prototype-object
InstantPrototype::InstantPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void InstantPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
}

}
