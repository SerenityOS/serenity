/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class ArrayIterator final : public Object {
    JS_OBJECT(ArrayIterator, Object);

public:
    static ArrayIterator* create(GlobalObject&, Value array, Object::PropertyKind iteration_kind);

    explicit ArrayIterator(Value array, Object::PropertyKind iteration_kind, Object& prototype);
    virtual ~ArrayIterator() override;

    Value array() const { return m_array; }
    Object::PropertyKind iteration_kind() const { return m_iteration_kind; }
    size_t index() const { return m_index; }

private:
    friend class ArrayIteratorPrototype;

    virtual void visit_edges(Cell::Visitor&) override;

    Value m_array;
    Object::PropertyKind m_iteration_kind;
    size_t m_index { 0 };
};

}
