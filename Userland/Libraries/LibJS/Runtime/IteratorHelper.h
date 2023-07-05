/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/SafeFunction.h>

namespace JS {

class IteratorHelper final : public Object {
    JS_OBJECT(IteratorHelper, Object);

public:
    using Closure = JS::SafeFunction<ThrowCompletionOr<Value>(IteratorHelper&)>;

    static ThrowCompletionOr<NonnullGCPtr<IteratorHelper>> create(Realm&, IteratorRecord, Closure);

    IteratorRecord const& underlying_iterator() const { return m_underlying_iterator; }
    Closure& closure() { return m_closure; }

    size_t counter() const { return m_counter; }
    void increment_counter() { ++m_counter; }

    Value result(Value);
    ThrowCompletionOr<Value> close_result(Completion);

    bool done() const { return m_done; }

private:
    IteratorHelper(Object& prototype, IteratorRecord, Closure);

    virtual void visit_edges(Visitor&) override;

    IteratorRecord m_underlying_iterator; // [[UnderlyingIterator]]
    Closure m_closure;

    size_t m_counter { 0 };
    bool m_done { false };
};

}
