/*
 * Copyright (c) 2021-2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SinglyLinkedList.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JobCallback.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/Runtime/WeakContainer.h>

namespace JS {

class FinalizationRegistry final
    : public Object
    , public WeakContainer {
    JS_OBJECT(FinalizationRegistry, Object);

public:
    virtual ~FinalizationRegistry() override = default;

    void add_finalization_record(Cell& target, Value held_value, Cell* unregister_token);
    bool remove_by_token(Cell& unregister_token);
    ThrowCompletionOr<void> cleanup(Optional<JobCallback> = {});

    virtual void remove_dead_cells(Badge<Heap>) override;

    Realm& realm() { return *m_realm; }
    Realm const& realm() const { return *m_realm; }

    JobCallback& cleanup_callback() { return m_cleanup_callback; }
    JobCallback const& cleanup_callback() const { return m_cleanup_callback; }

private:
    FinalizationRegistry(Realm&, JobCallback, Object& prototype);

    virtual void visit_edges(Visitor& visitor) override;

    NonnullGCPtr<Realm> m_realm;
    JobCallback m_cleanup_callback;

    struct FinalizationRecord {
        Cell* target { nullptr };
        Value held_value;
        Cell* unregister_token { nullptr };
    };
    SinglyLinkedList<FinalizationRecord> m_records;
};

}
