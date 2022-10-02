/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseCapability.h>

namespace JS {

NonnullGCPtr<PromiseCapability> PromiseCapability::create(VM& vm, GCPtr<Object> promise, GCPtr<FunctionObject> resolve, GCPtr<FunctionObject> reject)
{
    return NonnullGCPtr { *vm.heap().allocate_without_realm<PromiseCapability>(promise, resolve, reject) };
}

PromiseCapability::PromiseCapability(GCPtr<Object> promise, GCPtr<FunctionObject> resolve, GCPtr<FunctionObject> reject)
    : m_promise(promise)
    , m_resolve(resolve)
    , m_reject(reject)
{
}

void PromiseCapability::visit_edges(Cell::Visitor& visitor)
{
    visitor.visit(m_promise);
    visitor.visit(m_resolve);
    visitor.visit(m_reject);
}

// 27.2.1.5 NewPromiseCapability ( C ), https://tc39.es/ecma262/#sec-newpromisecapability
ThrowCompletionOr<NonnullGCPtr<PromiseCapability>> new_promise_capability(VM& vm, Value constructor)
{
    auto& realm = *vm.current_realm();

    // 1. If IsConstructor(C) is false, throw a TypeError exception.
    if (!constructor.is_constructor())
        return vm.throw_completion<TypeError>(ErrorType::NotAConstructor, constructor.to_string_without_side_effects());

    // 2. NOTE: C is assumed to be a constructor function that supports the parameter conventions of the Promise constructor (see 27.2.3.1).

    // 3. Let promiseCapability be the PromiseCapability Record { [[Promise]]: undefined, [[Resolve]]: undefined, [[Reject]]: undefined }.
    auto promise_capability = PromiseCapability::create(vm, nullptr, nullptr, nullptr);

    // 4. Let executorClosure be a new Abstract Closure with parameters (resolve, reject) that captures promiseCapability and performs the following steps when called:
    // NOTE: Additionally to capturing the GC-allocated promise capability, we also create handles for
    // the resolve and reject values to keep them alive within the closure, as it may outlive the former.
    auto executor_closure = [&promise_capability, resolve_handle = make_handle({}), reject_handle = make_handle({})](auto& vm) mutable -> ThrowCompletionOr<Value> {
        auto resolve = vm.argument(0);
        auto reject = vm.argument(1);

        // No idea what other engines say here.
        // a. If promiseCapability.[[Resolve]] is not undefined, throw a TypeError exception.
        if (!resolve_handle.is_null())
            return vm.template throw_completion<TypeError>(ErrorType::GetCapabilitiesExecutorCalledMultipleTimes);

        // b. If promiseCapability.[[Reject]] is not undefined, throw a TypeError exception.
        if (!reject_handle.is_null())
            return vm.template throw_completion<TypeError>(ErrorType::GetCapabilitiesExecutorCalledMultipleTimes);

        // c. Set promiseCapability.[[Resolve]] to resolve.
        resolve_handle = make_handle(resolve);
        if (resolve.is_function())
            promise_capability->set_resolve(resolve.as_function());

        // d. Set promiseCapability.[[Reject]] to reject.
        reject_handle = make_handle(reject);
        if (reject.is_function())
            promise_capability->set_reject(reject.as_function());

        // e. Return undefined.
        return js_undefined();
    };

    // 5. Let executor be CreateBuiltinFunction(executorClosure, 2, "", « »).
    auto* executor = NativeFunction::create(realm, move(executor_closure), 2, "");

    // 6. Let promise be ? Construct(C, « executor »).
    auto* promise = TRY(construct(vm, constructor.as_function(), executor));

    // 7. If IsCallable(promiseCapability.[[Resolve]]) is false, throw a TypeError exception.
    // NOTE: We only assign a value in the executor closure if it is a function.
    if (promise_capability->resolve() == nullptr)
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "Promise capability resolve value");

    // 8. If IsCallable(promiseCapability.[[Reject]]) is false, throw a TypeError exception.
    // NOTE: We only assign a value in the executor closure if it is a function.
    if (promise_capability->reject() == nullptr)
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "Promise capability reject value");

    // 9. Set promiseCapability.[[Promise]] to promise.
    promise_capability->set_promise(*promise);

    // 10. Return promiseCapability.
    return promise_capability;
}

}
