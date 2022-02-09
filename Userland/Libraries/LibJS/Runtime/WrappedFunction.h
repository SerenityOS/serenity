/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/Realm.h>

namespace JS {

class WrappedFunction final : public FunctionObject {
    JS_OBJECT(WrappedFunction, FunctionObject);

public:
    static ThrowCompletionOr<WrappedFunction*> create(GlobalObject&, Realm& caller_realm, FunctionObject& target_function);

    WrappedFunction(Realm&, FunctionObject&, Object& prototype);
    virtual ~WrappedFunction() = default;

    virtual ThrowCompletionOr<Value> internal_call(Value this_argument, MarkedVector<Value> arguments_list) override;

    // FIXME: Remove this (and stop inventing random internal slots that shouldn't exist, jeez)
    virtual FlyString const& name() const override { return m_wrapped_target_function.name(); }

    virtual Realm* realm() const override { return &m_realm; }

private:
    virtual void visit_edges(Visitor&) override;

    // Internal Slots of Wrapped Function Exotic Objects, https://tc39.es/proposal-shadowrealm/#table-internal-slots-of-wrapped-function-exotic-objects
    FunctionObject& m_wrapped_target_function; // [[WrappedTargetFunction]]
    Realm& m_realm;                            // [[Realm]]
};

}
