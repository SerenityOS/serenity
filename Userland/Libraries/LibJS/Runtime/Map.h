/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Map : public Object {
    JS_OBJECT(Map, Object);

public:
    static Map* create(GlobalObject&);

    explicit Map(Object& prototype);
    virtual ~Map() override;

    HashMap<Value, Value, ValueTraits> const& entries() const { return m_entries; };
    HashMap<Value, Value, ValueTraits>& entries() { return m_entries; };

private:
    virtual void visit_edges(Visitor& visitor) override;

    HashMap<Value, Value, ValueTraits> m_entries; // FIXME: Replace with a HashMap that maintains a linked list of insertion order for correct iteration order
};

}
