/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PromiseReaction.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// 6.2.3.1 Await, https://tc39.es/ecma262/#await
ThrowCompletionOr<Value> await(GlobalObject& global_object, Value value)
{
    auto& vm = global_object.vm();

    // 1. Let asyncContext be the running execution context.
    auto& async_context = vm.running_execution_context();

    // 2. Let promise be ? PromiseResolve(%Promise%, value).
    auto* promise = TRY(promise_resolve(global_object, *global_object.promise_constructor(), value));

    bool success = false;
    Value result;
    // 3. Let fulfilledClosure be a new Abstract Closure with parameters (value) that captures asyncContext and performs the following steps when called:
    auto fulfilled_closure = [&async_context, &success, &result](VM& vm, GlobalObject& global_object) -> ThrowCompletionOr<Value> {
        VERIFY(!vm.argument(0).is_undefined());
        // a. Let prevContext be the running execution context.
        // b. Suspend prevContext.
        // FIXME: We don't have this concept yet.

        // NOTE: Since we don't support context suspension, we exfiltrate the result to await()'s scope instead
        success = true;
        result = vm.argument(0);

        // c. Push asyncContext onto the execution context stack; asyncContext is now the running execution context.
        vm.push_execution_context(async_context, global_object);

        // d. Resume the suspended evaluation of asyncContext using NormalCompletion(value) as the result of the operation that suspended it.
        // e. Assert: When we reach this step, asyncContext has already been removed from the execution context stack and prevContext is the currently running execution context.
        // FIXME: We don't have this concept yet.

        // f. Return undefined.
        return js_undefined();
    };

    // 4. Let onFulfilled be ! CreateBuiltinFunction(fulfilledClosure, 1, "", « »).
    auto on_fulfilled = NativeFunction::create(global_object, "", move(fulfilled_closure));

    // 5. Let rejectedClosure be a new Abstract Closure with parameters (reason) that captures asyncContext and performs the following steps when called:
    auto rejected_closure = [&async_context, &success, &result](VM& vm, GlobalObject& global_object) -> ThrowCompletionOr<Value> {
        // a. Let prevContext be the running execution context.
        // b. Suspend prevContext.
        // FIXME: We don't have this concept yet.

        // NOTE: Since we don't support context suspension, we exfiltrate the result to await()'s scope instead
        success = false;
        result = vm.argument(0);

        // c. Push asyncContext onto the execution context stack; asyncContext is now the running execution context.
        vm.push_execution_context(async_context, global_object);

        // d. Resume the suspended evaluation of asyncContext using ThrowCompletion(reason) as the result of the operation that suspended it.
        // e. Assert: When we reach this step, asyncContext has already been removed from the execution context stack and prevContext is the currently running execution context.
        // FIXME: We don't have this concept yet.

        // f. Return undefined.
        return js_undefined();
    };

    // 6. Let onRejected be ! CreateBuiltinFunction(rejectedClosure, 1, "", « »).
    auto on_rejected = NativeFunction::create(global_object, "", move(rejected_closure));

    // 7. Perform ! PerformPromiseThen(promise, onFulfilled, onRejected).
    verify_cast<Promise>(promise)->perform_then(on_fulfilled, on_rejected, {});

    // 8. Remove asyncContext from the execution context stack and restore the execution context that is at the top of the execution context stack as the running execution context.
    vm.pop_execution_context();

    // 9. Set the code evaluation state of asyncContext such that when evaluation is resumed with a Completion completion, the following steps of the algorithm that invoked Await will be performed, with completion available.
    // 10. Return.
    // 11. NOTE: This returns to the evaluation of the operation that had most previously resumed evaluation of asyncContext.
    // FIXME: Since we don't support context suspension, we synchronously execute the promise
    vm.run_queued_promise_jobs();

    if (success)
        return result;
    else
        return throw_completion(result);
}

}
