/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SinglyLinkedList.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/Runtime/WeakContainer.h>

namespace JS {

class FinalizationRegistry final
    : public Object
    , public WeakContainer {
    JS_OBJECT(FinalizationRegistry, Object);

public:
    static FinalizationRegistry* create(GlobalObject&, Function&);

    explicit FinalizationRegistry(Function&, Object& prototype);
    virtual ~FinalizationRegistry() override;

    void add_finalization_record(Cell& target, Value held_value, Object* unregister_token);
    bool remove_by_token(Object& unregister_token);
    void cleanup(Function* callback = nullptr);

    virtual void remove_sweeped_cells(Badge<Heap>, Vector<Cell*>&) override;

private:
    virtual void visit_edges(Visitor& visitor) override;

    Function* m_cleanup_callback { nullptr };

    struct FinalizationRecord {
        Cell* target { nullptr };
        Value held_value;
        Object* unregister_token { nullptr };
    };
    SinglyLinkedList<FinalizationRecord> m_records;
};

}
