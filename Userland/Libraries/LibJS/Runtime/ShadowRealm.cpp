/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ShadowRealm.h>
#include <LibJS/Runtime/WrappedFunction.h>

namespace JS {

ShadowRealm::ShadowRealm(Realm& shadow_realm, ExecutionContext execution_context, Object& prototype)
    : Object(prototype)
    , m_shadow_realm(shadow_realm)
    , m_execution_context(move(execution_context))
{
}

void ShadowRealm::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(&m_shadow_realm);
}

// 3.1.3 GetWrappedValue ( callerRealm, value ), https://tc39.es/proposal-shadowrealm/#sec-getwrappedvalue
ThrowCompletionOr<Value> get_wrapped_value(GlobalObject& global_object, Realm& caller_realm, Value value)
{
    auto& vm = global_object.vm();

    // 1. Assert: callerRealm is a Realm Record.

    // 2. If Type(value) is Object, then
    if (value.is_object()) {
        // a. If IsCallable(value) is false, throw a TypeError exception.
        if (!value.is_function())
            return vm.throw_completion<TypeError>(global_object, ErrorType::ShadowRealmWrappedValueNonFunctionObject, value);

        // b. Return ! WrappedFunctionCreate(callerRealm, value).
        return { WrappedFunction::create(global_object, caller_realm, value.as_function()) };
    }

    // 3. Return value.
    return value;
}

}
