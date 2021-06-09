/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

struct ValueTraits : public Traits<Value> {
    static unsigned hash(Value value)
    {
        VERIFY(!value.is_empty());
        if (value.is_string())
            return value.as_string().string().hash();

        if (value.is_bigint())
            return value.as_bigint().big_integer().hash();

        return u64_hash(value.encoded()); // FIXME: Is this the best way to hash pointers, doubles & ints?
    }
    static bool equals(const Value a, const Value b)
    {
        return same_value_zero(a, b);
    }
};

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
