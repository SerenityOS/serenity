/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/StringObject.h>

namespace JS {

StringObject* StringObject::create(GlobalObject& global_object, PrimitiveString& primitive_string)
{
    return global_object.heap().allocate<StringObject>(global_object, primitive_string, *global_object.string_prototype());
}

StringObject::StringObject(PrimitiveString& string, Object& prototype)
    : Object(prototype)
    , m_string(string)
{
}

StringObject::~StringObject()
{
}

void StringObject::visit_edges(Cell::Visitor& visitor)
{
    Object::visit_edges(visitor);
    visitor.visit(&m_string);
}

}
