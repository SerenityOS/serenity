/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

// 27.2.1.5 NewPromiseCapability ( C ), https://tc39.es/ecma262/#sec-newpromisecapability
ThrowCompletionOr<PromiseCapability> new_promise_capability(GlobalObject& global_object, Value constructor)
{
    auto& vm = global_object.vm();

    // 1. If IsConstructor(C) is false, throw a TypeError exception.
    if (!constructor.is_constructor())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAConstructor, constructor.to_string_without_side_effects());

    // 2. NOTE: C is assumed to be a constructor function that supports the parameter conventions of the Promise constructor (see 27.2.3.1).

    // 3. Let promiseCapability be the PromiseCapability Record { [[Promise]]: undefined, [[Resolve]]: undefined, [[Reject]]: undefined }.
    // FIXME: This should not be stack-allocated, the executor function below can be captured and outlive it!
    //        See https://discord.com/channels/830522505605283862/886211697843531866/900081190621569154 for some discussion.
    struct {
        Value resolve { js_undefined() };
        Value reject { js_undefined() };
    } promise_capability_functions;

    // 4. Let executorClosure be a new Abstract Closure with parameters (resolve, reject) that captures promiseCapability and performs the following steps when called:
    // 5. Let executor be ! CreateBuiltinFunction(executorClosure, 2, "", « »).
    auto* executor = NativeFunction::create(global_object, "", [&promise_capability_functions](auto& vm, auto& global_object) -> ThrowCompletionOr<Value> {
        auto resolve = vm.argument(0);
        auto reject = vm.argument(1);

        // No idea what other engines say here.
        // a. If promiseCapability.[[Resolve]] is not undefined, throw a TypeError exception.
        if (!promise_capability_functions.resolve.is_undefined())
            return vm.template throw_completion<TypeError>(global_object, ErrorType::GetCapabilitiesExecutorCalledMultipleTimes);

        // b. If promiseCapability.[[Reject]] is not undefined, throw a TypeError exception.
        if (!promise_capability_functions.reject.is_undefined())
            return vm.template throw_completion<TypeError>(global_object, ErrorType::GetCapabilitiesExecutorCalledMultipleTimes);

        // c. Set promiseCapability.[[Resolve]] to resolve.
        promise_capability_functions.resolve = resolve;

        // d. Set promiseCapability.[[Reject]] to reject.
        promise_capability_functions.reject = reject;

        // e. Return undefined.
        return js_undefined();
    });
    executor->define_direct_property(vm.names.length, Value(2), Attribute::Configurable);
    executor->define_direct_property(vm.names.name, js_string(vm, String::empty()), Attribute::Configurable);

    // 6. Let promise be ? Construct(C, « executor »).
    MarkedValueList arguments(vm.heap());
    arguments.append(executor);
    auto* promise = TRY(construct(global_object, constructor.as_function(), move(arguments)));

    // 7. If IsCallable(promiseCapability.[[Resolve]]) is false, throw a TypeError exception.
    if (!promise_capability_functions.resolve.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, promise_capability_functions.resolve.to_string_without_side_effects());

    // 8. If IsCallable(promiseCapability.[[Reject]]) is false, throw a TypeError exception.
    if (!promise_capability_functions.reject.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, promise_capability_functions.reject.to_string_without_side_effects());

    // 9. Set promiseCapability.[[Promise]] to promise.
    // 10. Return promiseCapability.
    return PromiseCapability {
        promise,
        &promise_capability_functions.resolve.as_function(),
        &promise_capability_functions.reject.as_function(),
    };
}

PromiseReaction::PromiseReaction(Type type, Optional<PromiseCapability> capability, Optional<JobCallback> handler)
    : m_type(type)
    , m_capability(move(capability))
    , m_handler(move(handler))
{
}

void PromiseReaction::visit_edges(Cell::Visitor& visitor)
{
    Cell::visit_edges(visitor);
    if (m_capability.has_value()) {
        auto& capability = m_capability.value();
        visitor.visit(capability.promise);
        visitor.visit(capability.resolve);
        visitor.visit(capability.reject);
    }
    if (m_handler.has_value()) {
        auto& handler = m_handler.value();
        visitor.visit(handler.callback);
    }
}

}
