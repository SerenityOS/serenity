/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ArrayIterator.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ArrayIterator* ArrayIterator::create(Realm& realm, Value array, Object::PropertyKind iteration_kind)
{
    return realm.heap().allocate<ArrayIterator>(realm.global_object(), array, iteration_kind, *realm.global_object().array_iterator_prototype());
}

ArrayIterator::ArrayIterator(Value array, Object::PropertyKind iteration_kind, Object& prototype)
    : Object(prototype)
    , m_array(array)
    , m_iteration_kind(iteration_kind)
{
}

void ArrayIterator::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_array);
}

}
