/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/WeakSet.h>

namespace JS {

WeakSet* WeakSet::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<WeakSet>(global_object, *global_object.weak_set_prototype());
}

WeakSet::WeakSet(Object& prototype)
    : Object(prototype)
    , WeakContainer(heap())
{
}

WeakSet::~WeakSet()
{
}

void WeakSet::remove_dead_cells(Badge<Heap>)
{
    // FIXME: Do this in a single pass.
    Vector<Cell*> to_remove;
    for (auto* cell : m_values) {
        if (cell->state() != Cell::State::Live)
            to_remove.append(cell);
    }
    for (auto* cell : to_remove)
        m_values.remove(cell);
}

}
