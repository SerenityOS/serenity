/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Now.h>

namespace JS::Temporal {

// 2 The Temporal.now Object, https://tc39.es/proposal-temporal/#sec-temporal-now-object
Now::Now(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void Now::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
}

}
