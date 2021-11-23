/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AsyncFromSyncIterator.h>
#include <LibJS/Runtime/AsyncFromSyncIteratorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

AsyncFromSyncIterator* AsyncFromSyncIterator::create(GlobalObject& global_object, Object* sync_iterator_record)
{
    return global_object.heap().allocate<AsyncFromSyncIterator>(global_object, global_object, sync_iterator_record);
}

AsyncFromSyncIterator::AsyncFromSyncIterator(GlobalObject& global_object, Object* sync_iterator_record)
    : Object(*global_object.async_from_sync_iterator_prototype())
    , m_sync_iterator_record(sync_iterator_record)
{
    VERIFY(m_sync_iterator_record);
}

void AsyncFromSyncIterator::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
}

void AsyncFromSyncIterator::visit_edges(Cell::Visitor& visitor)
{
    Object::visit_edges(visitor);
    visitor.visit(m_sync_iterator_record);
}

}
