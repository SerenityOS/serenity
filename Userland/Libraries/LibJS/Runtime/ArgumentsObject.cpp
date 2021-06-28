/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ArgumentsObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ArgumentsObject::ArgumentsObject(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void ArgumentsObject::initialize(GlobalObject& global_object)
{
    Base::initialize(global_object);
}

ArgumentsObject::~ArgumentsObject()
{
}

}
