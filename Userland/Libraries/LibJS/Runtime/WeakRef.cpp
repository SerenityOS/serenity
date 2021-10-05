/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/WeakRef.h>

namespace JS {

WeakRef* WeakRef::create(GlobalObject& global_object, Object* object)
{
    return global_object.heap().allocate<WeakRef>(global_object, object, *global_object.weak_ref_prototype());
}

WeakRef::WeakRef(Object* object, Object& prototype)
    : Object(prototype)
    , WeakContainer(heap())
    , m_value(object)
    , m_last_execution_generation(vm().execution_generation())
{
}

WeakRef::~WeakRef()
{
}

void WeakRef::remove_dead_cells(Badge<Heap>)
{
    VERIFY(m_value);
    if (m_value->state() == Cell::State::Live)
        return;

    m_value = nullptr;
    // This is an optimization, we deregister from the garbage collector early (even if we were not garbage collected ourself yet)
    // to reduce the garbage collection overhead, which we can do because a cleared weak ref cannot be reused.
    WeakContainer::deregister();
}

void WeakRef::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);

    if (vm().execution_generation() == m_last_execution_generation)
        visitor.visit(m_value);
}

}
