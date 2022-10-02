/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::WebIDL {

// https://webidl.spec.whatwg.org/#a-new-promise
JS::NonnullGCPtr<JS::PromiseCapability> create_promise(JS::Realm& realm)
{
    auto& vm = realm.vm();

    // 1. Let constructor be realm.[[Intrinsics]].[[%Promise%]].
    auto* constructor = realm.intrinsics().promise_constructor();

    // Return ? NewPromiseCapability(constructor).
    // NOTE: When called with %Promise%, NewPromiseCapability can't throw.
    return MUST(JS::new_promise_capability(vm, constructor));
}

// https://webidl.spec.whatwg.org/#a-promise-resolved-with
JS::NonnullGCPtr<JS::PromiseCapability> create_resolved_promise(JS::Realm& realm, JS::Value value)
{
    auto& vm = realm.vm();

    // 1. Let value be the result of converting x to an ECMAScript value.

    // 2. Let constructor be realm.[[Intrinsics]].[[%Promise%]].
    auto* constructor = realm.intrinsics().promise_constructor();

    // 3. Let promiseCapability be ? NewPromiseCapability(constructor).
    // NOTE: When called with %Promise%, NewPromiseCapability can't throw.
    auto promise_capability = MUST(JS::new_promise_capability(vm, constructor));

    // 4. Perform ! Call(promiseCapability.[[Resolve]], undefined, « value »).
    MUST(JS::call(vm, *promise_capability->resolve(), JS::js_undefined(), value));

    // 5. Return promiseCapability.
    return promise_capability;
}

// https://webidl.spec.whatwg.org/#a-promise-rejected-with
JS::NonnullGCPtr<JS::PromiseCapability> create_rejected_promise(JS::Realm& realm, JS::Value reason)
{
    auto& vm = realm.vm();

    // 1. Let constructor be realm.[[Intrinsics]].[[%Promise%]].
    auto* constructor = realm.intrinsics().promise_constructor();

    // 2. Let promiseCapability be ? NewPromiseCapability(constructor).
    // NOTE: When called with %Promise%, NewPromiseCapability can't throw.
    auto promise_capability = MUST(JS::new_promise_capability(vm, constructor));

    // 3. Perform ! Call(promiseCapability.[[Reject]], undefined, « r »).
    MUST(JS::call(vm, *promise_capability->reject(), JS::js_undefined(), reason));

    // 4. Return promiseCapability.
    return promise_capability;
}

// https://webidl.spec.whatwg.org/#resolve
void resolve_promise(JS::VM& vm, JS::PromiseCapability const& promise_capability, JS::Value value)
{
    // 1. If x is not given, then let it be the undefined value.
    // NOTE: This is done via the default argument.

    // 2. Let value be the result of converting x to an ECMAScript value.

    // 3. Perform ! Call(p.[[Resolve]], undefined, « value »).
    MUST(JS::call(vm, *promise_capability.resolve(), JS::js_undefined(), value));
}

// https://webidl.spec.whatwg.org/#reject
void reject_promise(JS::VM& vm, JS::PromiseCapability const& promise_capability, JS::Value reason)
{
    // 1. Perform ! Call(p.[[Reject]], undefined, « r »).
    MUST(JS::call(vm, *promise_capability.reject(), JS::js_undefined(), reason));
}

// https://webidl.spec.whatwg.org/#dfn-perform-steps-once-promise-is-settled
JS::NonnullGCPtr<JS::Promise> react_to_promise(JS::PromiseCapability const& promise_capability, Optional<ReactionSteps> on_fulfilled_callback, Optional<ReactionSteps> on_rejected_callback)
{
    auto& realm = promise_capability.promise()->shape().realm();
    auto& vm = realm.vm();

    // 1. Let onFulfilledSteps be the following steps given argument V:
    auto on_fulfilled_steps = [on_fulfilled_callback = move(on_fulfilled_callback)](JS::VM& vm) -> JS::ThrowCompletionOr<JS::Value> {
        // 1. Let value be the result of converting V to an IDL value of type T.
        auto value = vm.argument(0);

        // 2. If there is a set of steps to be run if the promise was fulfilled, then let result be the result of performing them, given value if T is not undefined. Otherwise, let result be value.
        auto result = on_fulfilled_callback.has_value()
            ? TRY(Bindings::throw_dom_exception_if_needed(vm, [&] { return (*on_fulfilled_callback)(value); }))
            : value;

        // 3. Return result, converted to an ECMAScript value.
        return result;
    };

    // 2. Let onFulfilled be CreateBuiltinFunction(onFulfilledSteps, « »):
    auto* on_fulfilled = JS::NativeFunction::create(realm, move(on_fulfilled_steps), 1, "");

    // 3. Let onRejectedSteps be the following steps given argument R:
    auto on_rejected_steps = [&realm, on_rejected_callback = move(on_rejected_callback)](JS::VM& vm) -> JS::ThrowCompletionOr<JS::Value> {
        // 1. Let reason be the result of converting R to an IDL value of type any.
        auto reason = vm.argument(0);

        // 2. If there is a set of steps to be run if the promise was rejected, then let result be the result of performing them, given reason. Otherwise, let result be a promise rejected with reason.
        auto result = on_rejected_callback.has_value()
            ? TRY(Bindings::throw_dom_exception_if_needed(vm, [&] { return (*on_rejected_callback)(reason); }))
            : WebIDL::create_rejected_promise(realm, reason)->promise();

        // 3. Return result, converted to an ECMAScript value.
        return result;
    };

    // 4. Let onRejected be CreateBuiltinFunction(onRejectedSteps, « »):
    auto* on_rejected = JS::NativeFunction::create(realm, move(on_rejected_steps), 1, "");

    // 5. Let constructor be promise.[[Promise]].[[Realm]].[[Intrinsics]].[[%Promise%]].
    auto* constructor = realm.intrinsics().promise_constructor();

    // 6. Let newCapability be ? NewPromiseCapability(constructor).
    // NOTE: When called with %Promise%, NewPromiseCapability can't throw.
    auto new_capability = MUST(JS::new_promise_capability(vm, constructor));

    // 7. Return PerformPromiseThen(promise.[[Promise]], onFulfilled, onRejected, newCapability).
    auto* promise = verify_cast<JS::Promise>(promise_capability.promise().ptr());
    auto value = promise->perform_then(on_fulfilled, on_rejected, new_capability);
    return verify_cast<JS::Promise>(value.as_object());
}

// https://webidl.spec.whatwg.org/#upon-fulfillment
JS::NonnullGCPtr<JS::Promise> upon_fulfillment(JS::PromiseCapability const& promise_capability, ReactionSteps steps)
{
    // 1. Return the result of reacting to promise:
    return react_to_promise(promise_capability,
        // - If promise was fulfilled with value v, then:
        [steps = move(steps)](auto value) {
            // 1. Perform steps with v.
            // NOTE: The `return` is not immediately obvious, but `steps` may be something like
            // "Return the result of ...", which we also need to do _here_.
            return steps(value);
        },
        {});
}

// https://webidl.spec.whatwg.org/#upon-rejection
JS::NonnullGCPtr<JS::Promise> upon_rejection(JS::PromiseCapability const& promise_capability, ReactionSteps steps)
{
    // 1. Return the result of reacting to promise:
    return react_to_promise(promise_capability, {},
        // - If promise was rejected with reason r, then:
        [steps = move(steps)](auto reason) {
            // 1. Perform steps with r.
            // NOTE: The `return` is not immediately obvious, but `steps` may be something like
            // "Return the result of ...", which we also need to do _here_.
            return steps(reason);
        });
}

// https://webidl.spec.whatwg.org/#mark-a-promise-as-handled
void mark_promise_as_handled(JS::PromiseCapability const& promise_capability)
{
    // To mark as handled a Promise<T> promise, set promise.[[Promise]].[[PromiseIsHandled]] to true.
    auto* promise = verify_cast<JS::Promise>(promise_capability.promise().ptr());
    promise->set_is_handled();
}

}
