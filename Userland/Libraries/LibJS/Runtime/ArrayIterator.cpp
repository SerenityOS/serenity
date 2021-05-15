/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ArrayIterator.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ArrayIterator* ArrayIterator::create(GlobalObject& global_object, Value array, Object::PropertyKind iteration_kind)
{
    return global_object.heap().allocate<ArrayIterator>(global_object, *global_object.array_iterator_prototype(), array, iteration_kind);
}

ArrayIterator::ArrayIterator(Object& prototype, Value array, Object::PropertyKind iteration_kind)
    : Object(prototype)
    , m_array(array)
    , m_iteration_kind(iteration_kind)
{
}

ArrayIterator::~ArrayIterator()
{
}

void ArrayIterator::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_array);
}

}
