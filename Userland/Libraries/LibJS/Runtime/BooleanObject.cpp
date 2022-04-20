/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BooleanObject* BooleanObject::create(GlobalObject& global_object, bool value)
{
    return global_object.heap().allocate<BooleanObject>(global_object, value, *global_object.boolean_prototype());
}

BooleanObject::BooleanObject(bool value, Object& prototype)
    : Object(prototype)
    , m_value(value)
{
}

}
