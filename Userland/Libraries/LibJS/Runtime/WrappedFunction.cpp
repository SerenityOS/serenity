/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ShadowRealm.h>
#include <LibJS/Runtime/WrappedFunction.h>

namespace JS {

// 2.2 WrappedFunctionCreate ( callerRealm, targetFunction ), https://tc39.es/proposal-shadowrealm/#sec-wrappedfunctioncreate
WrappedFunction* WrappedFunction::create(GlobalObject& global_object, Realm& caller_realm, FunctionObject& target_function)
{
    // 1. Assert: callerRealm is a Realm Record.
    // 2. Assert: IsCallable(targetFunction) is true.

    // 3. Let internalSlotsList be the internal slots listed in Table 2, plus [[Prototype]] and [[Extensible]].
    // 4. Let obj be ! MakeBasicObject(internalSlotsList).
    auto& prototype = *caller_realm.global_object().function_prototype();
    auto* object = global_object.heap().allocate<WrappedFunction>(global_object, caller_realm, target_function, prototype);

    // 5. Set obj.[[Prototype]] to callerRealm.[[Intrinsics]].[[%Function.prototype%]].
    // 6. Set obj.[[Call]] as described in 2.1.
    // 7. Set obj.[[WrappedTargetFunction]] to targetFunction.
    // 8. Set obj.[[Realm]] to callerRealm.

    // 9. Return obj.
    return object;
}

// 2 Wrapped Function Exotic Objects, https://tc39.es/proposal-shadowrealm/#sec-wrapped-function-exotic-objects
WrappedFunction::WrappedFunction(Realm& realm, FunctionObject& wrapped_target_function, Object& prototype)
    : FunctionObject(prototype)
    , m_wrapped_target_function(wrapped_target_function)
    , m_realm(realm)
{
}

// 2.1 [[Call]] ( thisArgument, argumentsList ), https://tc39.es/proposal-shadowrealm/#sec-wrapped-function-exotic-objects-call-thisargument-argumentslist
ThrowCompletionOr<Value> WrappedFunction::internal_call(Value this_argument, MarkedValueList arguments_list)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Let target be F.[[WrappedTargetFunction]].
    auto& target = m_wrapped_target_function;

    // 2. Assert: IsCallable(target) is true.
    VERIFY(Value(&target).is_function());

    // 3. Let targetRealm be ? GetFunctionRealm(target).
    auto* target_realm = TRY(get_function_realm(global_object, target));

    // 4. Let callerRealm be ? GetFunctionRealm(F).
    auto* caller_realm = TRY(get_function_realm(global_object, *this));

    // 5. NOTE: Any exception objects produced after this point are associated with callerRealm.

    // 6. Let wrappedArgs be a new empty List.
    auto wrapped_args = MarkedValueList { vm.heap() };
    wrapped_args.ensure_capacity(arguments_list.size());

    // 7. For each element arg of argumentsList, do
    for (auto& arg : arguments_list) {
        // a. Let wrappedValue be ? GetWrappedValue(targetRealm, arg).
        auto wrapped_value = TRY(get_wrapped_value(global_object, *target_realm, arg));

        // b. Append wrappedValue to wrappedArgs.
        wrapped_args.append(wrapped_value);
    }

    // 8. Let wrappedThisArgument to ? GetWrappedValue(targetRealm, thisArgument).
    auto wrapped_this_argument = TRY(get_wrapped_value(global_object, *target_realm, this_argument));

    // 9. Let result be the Completion Record of Call(target, wrappedThisArgument, wrappedArgs).
    auto result = call(global_object, &target, wrapped_this_argument, move(wrapped_args));

    // 10. If result.[[Type]] is normal or result.[[Type]] is return, then
    if (!result.is_throw_completion()) {
        // a. Return ? GetWrappedValue(callerRealm, result.[[Value]]).
        return get_wrapped_value(global_object, *caller_realm, result.value());
    }
    // 11. Else,
    else {
        // a. Throw a TypeError exception.
        return vm.throw_completion<TypeError>(caller_realm->global_object(), ErrorType::WrappedFunctionCallThrowCompletion);
    }

    // NOTE: Also see "Editor's Note" in the spec regarding the TypeError above.
}

void WrappedFunction::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(&m_wrapped_target_function);
    visitor.visit(&m_realm);
}

}
