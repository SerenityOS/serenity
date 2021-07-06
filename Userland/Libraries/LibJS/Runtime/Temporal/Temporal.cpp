/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Temporal.h>

namespace JS::Temporal {

// 1 The Temporal Object, https://tc39.es/proposal-temporal/#sec-temporal-objects
Temporal::Temporal(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void Temporal::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
}

}
