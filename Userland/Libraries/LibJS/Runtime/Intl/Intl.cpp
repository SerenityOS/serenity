/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Intl.h>

namespace JS::Intl {

// 8 The Intl Object, https://tc39.es/ecma402/#intl-object
Intl::Intl(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void Intl::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
}

}
