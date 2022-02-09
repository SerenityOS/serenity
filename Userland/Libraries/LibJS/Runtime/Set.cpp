/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Set.h>

namespace JS {

Set* Set::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<Set>(global_object, *global_object.set_prototype());
}

Set::Set(Object& prototype)
    : Object(prototype)
    , m_values(*prototype.global_object().map_prototype())
{
}

Set::~Set()
{
}

void Set::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    static_cast<Object&>(m_values).visit_edges(visitor);
}

}
