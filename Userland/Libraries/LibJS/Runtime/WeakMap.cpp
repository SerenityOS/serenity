/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/WeakMap.h>

namespace JS {

WeakMap* WeakMap::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<WeakMap>(global_object, *global_object.weak_map_prototype());
}

WeakMap::WeakMap(Object& prototype)
    : Object(prototype)
    , WeakContainer(heap())
{
}

WeakMap::~WeakMap()
{
}

void WeakMap::remove_swept_cells(Badge<Heap>, Span<Cell*> cells)
{
    for (auto* cell : cells)
        m_values.remove(cell);
}

void WeakMap::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& entry : m_values)
        visitor.visit(entry.value);
}

}
