/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Set : public Object {
    JS_OBJECT(Set, Object);

public:
    static Set* create(GlobalObject&);

    explicit Set(Object& prototype);
    virtual ~Set() override;

    HashTable<Value, ValueTraits> const& values() const { return m_values; };
    HashTable<Value, ValueTraits>& values() { return m_values; };

private:
    virtual void visit_edges(Visitor& visitor) override;

    HashTable<Value, ValueTraits> m_values; // FIXME: Replace with a HashTable that maintains a linked list of insertion order for correct iteration order
};

}
