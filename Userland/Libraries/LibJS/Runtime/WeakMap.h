/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/WeakContainer.h>

namespace JS {

class WeakMap final
    : public Object
    , public WeakContainer {
    JS_OBJECT(WeakMap, Object);

public:
    static WeakMap* create(GlobalObject&);

    explicit WeakMap(Object& prototype);
    virtual ~WeakMap() override;

    HashMap<Cell*, Value> const& values() const { return m_values; };
    HashMap<Cell*, Value>& values() { return m_values; };

    virtual void remove_dead_cells(Badge<Heap>) override;

private:
#ifdef JS_TRACK_ZOMBIE_CELLS
    virtual void did_become_zombie() override
    {
        deregister();
    }
#endif

    void visit_edges(Visitor&) override;

    HashMap<Cell*, Value> m_values; // This stores Cell pointers instead of Object pointers to aide with sweeping
};

}
