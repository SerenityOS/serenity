/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class WeakSet : public Object {
    JS_OBJECT(WeakSet, Object);

public:
    static WeakSet* create(GlobalObject&);

    explicit WeakSet(Object& prototype);
    virtual ~WeakSet() override;

    HashTable<Object*> const& values() const { return m_values; };
    HashTable<Object*>& values() { return m_values; };

private:
    HashTable<Object*> m_values;
};

}
