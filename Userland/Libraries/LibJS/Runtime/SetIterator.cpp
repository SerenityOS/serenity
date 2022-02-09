/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/SetIterator.h>

namespace JS {

SetIterator* SetIterator::create(GlobalObject& global_object, Set& set, Object::PropertyKind iteration_kind)
{
    return global_object.heap().allocate<SetIterator>(global_object, set, iteration_kind, *global_object.set_iterator_prototype());
}

SetIterator::SetIterator(Set& set, Object::PropertyKind iteration_kind, Object& prototype)
    : Object(prototype)
    , m_set(set)
    , m_iteration_kind(iteration_kind)
    , m_iterator(static_cast<Set const&>(set).begin())
{
}

SetIterator::~SetIterator()
{
}

void SetIterator::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_set);
}

}
