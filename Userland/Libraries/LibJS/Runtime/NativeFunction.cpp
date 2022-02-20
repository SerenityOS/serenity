/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// 10.3.3 CreateBuiltinFunction ( behaviour, length, name, additionalInternalSlotsList [ , realm [ , prototype [ , prefix ] ] ] ), https://tc39.es/ecma262/#sec-createbuiltinfunction
// NOTE: This doesn't consider additionalInternalSlotsList, which is rarely used, and can either be implemented using only the `function` lambda, or needs a NativeFunction subclass.
NativeFunction* NativeFunction::create(GlobalObject& global_object, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> behaviour, i32 length, PropertyKey const& name, Optional<Realm*> realm, Optional<Object*> prototype, Optional<StringView> const& prefix)
{
    auto& vm = global_object.vm();

    // 1. If realm is not present, set realm to the current Realm Record.
    if (!realm.has_value())
        realm = vm.current_realm();

    // 2. If prototype is not present, set prototype to realm.[[Intrinsics]].[[%Function.prototype%]].
    if (!prototype.has_value())
        prototype = realm.value()->global_object().function_prototype();

    // 3. Let internalSlotsList be a List containing the names of all the internal slots that 10.3 requires for the built-in function object that is about to be created.
    // 4. Append to internalSlotsList the elements of additionalInternalSlotsList.

    // 5. Let func be a new built-in function object that, when called, performs the action described by behaviour using the provided arguments as the values of the corresponding parameters specified by behaviour. The new function object has internal slots whose names are the elements of internalSlotsList, and an [[InitialName]] internal slot.
    // 6. Set func.[[Prototype]] to prototype.
    // 7. Set func.[[Extensible]] to true.
    // 8. Set func.[[Realm]] to realm.
    // 9. Set func.[[InitialName]] to null.
    auto* function = global_object.heap().allocate<NativeFunction>(global_object, global_object, move(behaviour), prototype.value(), *realm.value());

    // 10. Perform ! SetFunctionLength(func, length).
    function->set_function_length(length);

    // 11. If prefix is not present, then
    //     a. Perform ! SetFunctionName(func, name).
    // 12. Else,
    //     a. Perform ! SetFunctionName(func, name, prefix).
    function->set_function_name(name, prefix);

    // 13. Return func.
    return function;
}

NativeFunction* NativeFunction::create(GlobalObject& global_object, const FlyString& name, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> function)
{
    return global_object.heap().allocate<NativeFunction>(global_object, name, move(function), *global_object.function_prototype());
}

NativeFunction::NativeFunction(GlobalObject& global_object, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> native_function, Object* prototype, Realm& realm)
    : FunctionObject(global_object, prototype)
    , m_native_function(move(native_function))
    , m_realm(&realm)
{
}

// FIXME: m_realm is supposed to be the realm argument of CreateBuiltinFunction, or the current
//        Realm Record. The former is not something that's commonly used or we support, the
//        latter is impossible as no ExecutionContext exists when most NativeFunctions are created...

NativeFunction::NativeFunction(Object& prototype)
    : FunctionObject(prototype)
    , m_realm(global_object().associated_realm())
{
}

NativeFunction::NativeFunction(FlyString name, Function<ThrowCompletionOr<Value>(VM&, GlobalObject&)> native_function, Object& prototype)
    : FunctionObject(prototype)
    , m_name(move(name))
    , m_native_function(move(native_function))
    , m_realm(global_object().associated_realm())
{
}

NativeFunction::NativeFunction(FlyString name, Object& prototype)
    : FunctionObject(prototype)
    , m_name(move(name))
    , m_realm(global_object().associated_realm())
{
}

NativeFunction::~NativeFunction()
{
}

// NOTE: Do not attempt to DRY these, it's not worth it. The difference in return types (Value vs Object*),
// called functions (call() vs construct(FunctionObject&)), and this value (passed vs uninitialized) make
// these good candidates for a bit of code duplication :^)

// 10.3.1 [[Call]] ( thisArgument, argumentsList ), https://tc39.es/ecma262/#sec-built-in-function-objects-call-thisargument-argumentslist
ThrowCompletionOr<Value> NativeFunction::internal_call(Value this_argument, MarkedVector<Value> arguments_list)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Let callerContext be the running execution context.
    auto& caller_context = vm.running_execution_context();

    // 2. If callerContext is not already suspended, suspend callerContext.
    // NOTE: We don't support this concept yet.

    // 3. Let calleeContext be a new execution context.
    ExecutionContext callee_context(heap());

    // 4. Set the Function of calleeContext to F.
    callee_context.function = this;
    callee_context.function_name = m_name;

    // 5. Let calleeRealm be F.[[Realm]].
    auto* callee_realm = m_realm;
    // NOTE: This non-standard fallback is needed until we can guarantee that literally
    // every function has a realm - especially in LibWeb that's sometimes not the case
    // when a function is created while no JS is running, as we currently need to rely on
    // that (:acid2:, I know - see set_event_handler_attribute() for an example).
    // If there's no 'current realm' either, we can't continue and crash.
    if (!callee_realm)
        callee_realm = vm.current_realm();
    VERIFY(callee_realm);

    // 6. Set the Realm of calleeContext to calleeRealm.
    callee_context.realm = callee_realm;

    // 7. Set the ScriptOrModule of calleeContext to null.
    // Note: This is already the default value.

    // 8. Perform any necessary implementation-defined initialization of calleeContext.

    callee_context.this_value = this_argument;
    callee_context.arguments.extend(move(arguments_list));

    callee_context.lexical_environment = caller_context.lexical_environment;
    callee_context.variable_environment = caller_context.variable_environment;

    // NOTE: This is a LibJS specific hack for NativeFunction to inherit the strictness of its caller.
    callee_context.is_strict_mode = vm.in_strict_mode();

    if (auto* interpreter = vm.interpreter_if_exists())
        callee_context.current_node = interpreter->current_node();

    // </8.> --------------------------------------------------------------------------

    // 9. Push calleeContext onto the execution context stack; calleeContext is now the running execution context.
    TRY(vm.push_execution_context(callee_context, global_object));

    // 10. Let result be the Completion Record that is the result of evaluating F in a manner that conforms to the specification of F. thisArgument is the this value, argumentsList provides the named parameters, and the NewTarget value is undefined.
    auto result = call();

    // 11. Remove calleeContext from the execution context stack and restore callerContext as the running execution context.
    vm.pop_execution_context();

    // 12. Return result.
    return result;
}

// 10.3.2 [[Construct]] ( argumentsList, newTarget ), https://tc39.es/ecma262/#sec-built-in-function-objects-construct-argumentslist-newtarget
ThrowCompletionOr<Object*> NativeFunction::internal_construct(MarkedVector<Value> arguments_list, FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Let callerContext be the running execution context.
    auto& caller_context = vm.running_execution_context();

    // 2. If callerContext is not already suspended, suspend callerContext.
    // NOTE: We don't support this concept yet.

    // 3. Let calleeContext be a new execution context.
    ExecutionContext callee_context(heap());

    // 4. Set the Function of calleeContext to F.
    callee_context.function = this;
    callee_context.function_name = m_name;

    // 5. Let calleeRealm be F.[[Realm]].
    auto* callee_realm = m_realm;
    // NOTE: This non-standard fallback is needed until we can guarantee that literally
    // every function has a realm - especially in LibWeb that's sometimes not the case
    // when a function is created while no JS is running, as we currently need to rely on
    // that (:acid2:, I know - see set_event_handler_attribute() for an example).
    // If there's no 'current realm' either, we can't continue and crash.
    if (!callee_realm)
        callee_realm = vm.current_realm();
    VERIFY(callee_realm);

    // 6. Set the Realm of calleeContext to calleeRealm.
    callee_context.realm = callee_realm;

    // 7. Set the ScriptOrModule of calleeContext to null.
    // Note: This is already the default value.

    // 8. Perform any necessary implementation-defined initialization of calleeContext.

    callee_context.arguments.extend(move(arguments_list));

    callee_context.lexical_environment = caller_context.lexical_environment;
    callee_context.variable_environment = caller_context.variable_environment;

    // NOTE: This is a LibJS specific hack for NativeFunction to inherit the strictness of its caller.
    callee_context.is_strict_mode = vm.in_strict_mode();

    if (auto* interpreter = vm.interpreter_if_exists())
        callee_context.current_node = interpreter->current_node();

    // </8.> --------------------------------------------------------------------------

    // 9. Push calleeContext onto the execution context stack; calleeContext is now the running execution context.
    TRY(vm.push_execution_context(callee_context, global_object));

    // 10. Let result be the Completion Record that is the result of evaluating F in a manner that conforms to the specification of F. The this value is uninitialized, argumentsList provides the named parameters, and newTarget provides the NewTarget value.
    auto result = construct(new_target);

    // 11. Remove calleeContext from the execution context stack and restore callerContext as the running execution context.
    vm.pop_execution_context();

    // 12. Return result.
    return result;
}

ThrowCompletionOr<Value> NativeFunction::call()
{
    return m_native_function(vm(), global_object());
}

ThrowCompletionOr<Object*> NativeFunction::construct(FunctionObject&)
{
    // Needs to be overridden if [[Construct]] is needed.
    VERIFY_NOT_REACHED();
}

bool NativeFunction::is_strict_mode() const
{
    return true;
}

}
