/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

// 27.1.4.3 Properties of Async-from-Sync Iterator Instances, https://tc39.es/ecma262/#sec-properties-of-async-from-sync-iterator-instances
class AsyncFromSyncIterator final : public Object {
    JS_OBJECT(AsyncFromSyncIterator, Object);

public:
    static AsyncFromSyncIterator* create(GlobalObject&, Object* sync_iterator_record);

    explicit AsyncFromSyncIterator(GlobalObject&, Object* sync_iterator_record);
    virtual void initialize(GlobalObject&) override;
    virtual ~AsyncFromSyncIterator() override = default;

    void visit_edges(Visitor& visitor) override;

    Object& sync_iterator_record() const { return *m_sync_iterator_record; }

private:
    Object* m_sync_iterator_record { nullptr }; // [[SyncIteratorRecord]]
};

}
