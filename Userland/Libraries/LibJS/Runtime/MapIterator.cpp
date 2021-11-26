/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/MapIterator.h>

namespace JS {

MapIterator* MapIterator::create(GlobalObject& global_object, Map& map, Object::PropertyKind iteration_kind)
{
    return global_object.heap().allocate<MapIterator>(global_object, map, iteration_kind, *global_object.map_iterator_prototype());
}

MapIterator::MapIterator(Map& map, Object::PropertyKind iteration_kind, Object& prototype)
    : Object(prototype)
    , m_map(map)
    , m_iteration_kind(iteration_kind)
    , m_iterator(map.entries().begin())
{
}

MapIterator::~MapIterator()
{
}

void MapIterator::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_map);
}

}
