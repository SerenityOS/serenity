/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NumberObject.h>

namespace JS {

NumberObject* NumberObject::create(GlobalObject& global_object, double value)
{
    return global_object.heap().allocate<NumberObject>(global_object, value, *global_object.number_prototype());
}

NumberObject::NumberObject(double value, Object& prototype)
    : Object(prototype)
    , m_value(value)
{
}

}
