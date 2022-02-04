/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/WeakContainer.h>

namespace JS {

class WeakRef final
    : public Object
    , public WeakContainer {
    JS_OBJECT(WeakRef, Object);

public:
    static WeakRef* create(GlobalObject&, Object*);

    explicit WeakRef(Object*, Object& prototype);
    virtual ~WeakRef() override;

    Object* value() const { return m_value; };

    void update_execution_generation() { m_last_execution_generation = vm().execution_generation(); };

    virtual void remove_dead_cells(Badge<Heap>) override;

private:
    virtual void visit_edges(Visitor&) override;

    Object* m_value { nullptr };
    u32 m_last_execution_generation { 0 };
};

}
