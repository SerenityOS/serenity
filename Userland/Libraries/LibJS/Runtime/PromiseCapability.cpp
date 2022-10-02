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

// 27.2.1.5 NewPromiseCapability ( C ), https://tc39.es/ecma262/#sec-newpromisecapability
ThrowCompletionOr<PromiseCapability> new_promise_capability(VM& vm, Value constructor)
{
    auto& realm = *vm.current_realm();

    // 1. If IsConstructor(C) is false, throw a TypeError exception.
    if (!constructor.is_constructor())
        return vm.throw_completion<TypeError>(ErrorType::NotAConstructor, constructor.to_string_without_side_effects());

    // 2. NOTE: C is assumed to be a constructor function that supports the parameter conventions of the Promise constructor (see 27.2.3.1).

    // 3. Let promiseCapability be the PromiseCapability Record { [[Promise]]: undefined, [[Resolve]]: undefined, [[Reject]]: undefined }.
    // FIXME: This should not be stack-allocated, the executor function below can be captured and outlive it!
    //        See https://discord.com/channels/830522505605283862/886211697843531866/900081190621569154 for some discussion.
    struct {
        Value resolve { js_undefined() };
        Value reject { js_undefined() };
    } promise_capability_functions;

    // 4. Let executorClosure be a new Abstract Closure with parameters (resolve, reject) that captures promiseCapability and performs the following steps when called:
    auto executor_closure = [&promise_capability_functions](auto& vm) -> ThrowCompletionOr<Value> {
        auto resolve = vm.argument(0);
        auto reject = vm.argument(1);

        // No idea what other engines say here.
        // a. If promiseCapability.[[Resolve]] is not undefined, throw a TypeError exception.
        if (!promise_capability_functions.resolve.is_undefined())
            return vm.template throw_completion<TypeError>(ErrorType::GetCapabilitiesExecutorCalledMultipleTimes);

        // b. If promiseCapability.[[Reject]] is not undefined, throw a TypeError exception.
        if (!promise_capability_functions.reject.is_undefined())
            return vm.template throw_completion<TypeError>(ErrorType::GetCapabilitiesExecutorCalledMultipleTimes);

        // c. Set promiseCapability.[[Resolve]] to resolve.
        promise_capability_functions.resolve = resolve;

        // d. Set promiseCapability.[[Reject]] to reject.
        promise_capability_functions.reject = reject;

        // e. Return undefined.
        return js_undefined();
    };

    // 5. Let executor be CreateBuiltinFunction(executorClosure, 2, "", « »).
    auto* executor = NativeFunction::create(realm, move(executor_closure), 2, "");

    // 6. Let promise be ? Construct(C, « executor »).
    auto* promise = TRY(construct(vm, constructor.as_function(), executor));

    // 7. If IsCallable(promiseCapability.[[Resolve]]) is false, throw a TypeError exception.
    if (!promise_capability_functions.resolve.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, promise_capability_functions.resolve.to_string_without_side_effects());

    // 8. If IsCallable(promiseCapability.[[Reject]]) is false, throw a TypeError exception.
    if (!promise_capability_functions.reject.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, promise_capability_functions.reject.to_string_without_side_effects());

    // 9. Set promiseCapability.[[Promise]] to promise.
    // 10. Return promiseCapability.
    return PromiseCapability {
        promise,
        &promise_capability_functions.resolve.as_function(),
        &promise_capability_functions.reject.as_function(),
    };
}

}
