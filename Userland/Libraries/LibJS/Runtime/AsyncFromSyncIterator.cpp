/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AsyncFromSyncIterator.h>
#include <LibJS/Runtime/AsyncFromSyncIteratorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

NonnullGCPtr<AsyncFromSyncIterator> AsyncFromSyncIterator::create(Realm& realm, IteratorRecord sync_iterator_record)
{
    return realm.heap().allocate<AsyncFromSyncIterator>(realm, realm, sync_iterator_record).release_allocated_value_but_fixme_should_propagate_errors();
}

AsyncFromSyncIterator::AsyncFromSyncIterator(Realm& realm, IteratorRecord sync_iterator_record)
    : Object(ConstructWithPrototypeTag::Tag, realm.intrinsics().async_from_sync_iterator_prototype())
    , m_sync_iterator_record(sync_iterator_record)
{
}

void AsyncFromSyncIterator::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_sync_iterator_record.iterator);
    visitor.visit(m_sync_iterator_record.next_method);
}

}
