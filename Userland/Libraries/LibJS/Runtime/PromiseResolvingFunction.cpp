/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/PromiseResolvingFunction.h>

namespace JS {

PromiseResolvingFunction* PromiseResolvingFunction::create(Realm& realm, Promise& promise, AlreadyResolved& already_resolved, FunctionType function)
{
    return realm.heap().allocate<PromiseResolvingFunction>(realm.global_object(), promise, already_resolved, move(function), *realm.global_object().function_prototype());
}

PromiseResolvingFunction::PromiseResolvingFunction(Promise& promise, AlreadyResolved& already_resolved, FunctionType native_function, Object& prototype)
    : NativeFunction(prototype)
    , m_promise(promise)
    , m_already_resolved(already_resolved)
    , m_native_function(move(native_function))
{
}

void PromiseResolvingFunction::initialize(Realm& realm)
{
    Base::initialize(realm);
    define_direct_property(vm().names.length, Value(1), Attribute::Configurable);
}

ThrowCompletionOr<Value> PromiseResolvingFunction::call()
{
    return m_native_function(vm(), global_object(), m_promise, m_already_resolved);
}

void PromiseResolvingFunction::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_promise);
    visitor.visit(&m_already_resolved);
}

}
