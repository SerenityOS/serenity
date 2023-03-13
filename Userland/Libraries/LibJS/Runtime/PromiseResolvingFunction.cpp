/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/PromiseResolvingFunction.h>

namespace JS {

NonnullGCPtr<PromiseResolvingFunction> PromiseResolvingFunction::create(Realm& realm, Promise& promise, AlreadyResolved& already_resolved, FunctionType function)
{
    return realm.heap().allocate<PromiseResolvingFunction>(realm, promise, already_resolved, move(function), *realm.intrinsics().function_prototype()).release_allocated_value_but_fixme_should_propagate_errors();
}

PromiseResolvingFunction::PromiseResolvingFunction(Promise& promise, AlreadyResolved& already_resolved, FunctionType native_function, Object& prototype)
    : NativeFunction(prototype)
    , m_promise(promise)
    , m_already_resolved(already_resolved)
    , m_native_function(move(native_function))
{
}

ThrowCompletionOr<void> PromiseResolvingFunction::initialize(Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    define_direct_property(vm().names.length, Value(1), Attribute::Configurable);

    return {};
}

ThrowCompletionOr<Value> PromiseResolvingFunction::call()
{
    return m_native_function(vm(), m_promise, m_already_resolved);
}

void PromiseResolvingFunction::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_promise);
    visitor.visit(m_already_resolved);
}

}
