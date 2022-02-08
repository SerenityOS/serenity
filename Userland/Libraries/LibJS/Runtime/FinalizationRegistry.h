/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SinglyLinkedList.h>
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
    explicit FinalizationRegistry(Realm&, JS::JobCallback, Object& prototype);
    virtual ~FinalizationRegistry() override;

    void add_finalization_record(Cell& target, Value held_value, Object* unregister_token);
    bool remove_by_token(Object& unregister_token);
    ThrowCompletionOr<void> cleanup(Optional<JobCallback> = {});

    virtual void remove_dead_cells(Badge<Heap>) override;

    Realm& realm() { return *m_realm.cell(); }
    Realm const& realm() const { return *m_realm.cell(); }

    JobCallback& cleanup_callback() { return m_cleanup_callback; }
    JobCallback const& cleanup_callback() const { return m_cleanup_callback; }

private:
    virtual void visit_edges(Visitor& visitor) override;

    Handle<Realm> m_realm;
    JS::JobCallback m_cleanup_callback;

    struct FinalizationRecord {
        Cell* target { nullptr };
        Value held_value;
        Object* unregister_token { nullptr };
    };
    SinglyLinkedList<FinalizationRecord> m_records;
};

}
