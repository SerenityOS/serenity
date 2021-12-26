/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PromisePrototype.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

PromisePrototype::PromisePrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void PromisePrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.then, then, 2, attr);
    define_native_function(vm.names.catch_, catch_, 1, attr);
    define_native_function(vm.names.finally, finally, 1, attr);

    // 27.2.5.5 Promise.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-promise.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.Promise.as_string()), Attribute::Configurable);
}

// 27.2.5.4 Promise.prototype.then ( onFulfilled, onRejected ), https://tc39.es/ecma262/#sec-promise.prototype.then
JS_DEFINE_NATIVE_FUNCTION(PromisePrototype::then)
{
    auto on_fulfilled = vm.argument(0);
    auto on_rejected = vm.argument(1);

    // 1. Let promise be the this value.
    // 2. If IsPromise(promise) is false, throw a TypeError exception.
    auto* promise = TRY(typed_this_object(global_object));

    // 3. Let C be ? SpeciesConstructor(promise, %Promise%).
    auto* constructor = TRY(species_constructor(global_object, *promise, *global_object.promise_constructor()));

    // 4. Let resultCapability be ? NewPromiseCapability(C).
    auto result_capability = TRY(new_promise_capability(global_object, constructor));

    // 5. Return PerformPromiseThen(promise, onFulfilled, onRejected, resultCapability).
    return promise->perform_then(on_fulfilled, on_rejected, result_capability);
}

// 27.2.5.1 Promise.prototype.catch ( onRejected ), https://tc39.es/ecma262/#sec-promise.prototype.catch
JS_DEFINE_NATIVE_FUNCTION(PromisePrototype::catch_)
{
    auto on_rejected = vm.argument(0);

    // 1. Let promise be the this value.
    auto this_value = vm.this_value(global_object);

    // 2. Return ? Invoke(promise, "then", « undefined, onRejected »).
    return TRY(this_value.invoke(global_object, vm.names.then, js_undefined(), on_rejected));
}

// 27.2.5.3 Promise.prototype.finally ( onFinally ), https://tc39.es/ecma262/#sec-promise.prototype.finally
JS_DEFINE_NATIVE_FUNCTION(PromisePrototype::finally)
{
    auto on_finally = vm.argument(0);

    // 1. Let promise be the this value.
    // 2. If Type(promise) is not Object, throw a TypeError exception.
    // FIXME: Don't coerce to object!
    auto* promise = TRY(vm.this_value(global_object).to_object(global_object));

    // 3. Let C be ? SpeciesConstructor(promise, %Promise%).
    auto* constructor = TRY(species_constructor(global_object, *promise, *global_object.promise_constructor()));

    // 4. Assert: IsConstructor(C) is true.
    VERIFY(constructor);

    Value then_finally;
    Value catch_finally;

    // 5. If IsCallable(onFinally) is false, then
    if (!on_finally.is_function()) {
        // a. Let thenFinally be onFinally.
        then_finally = on_finally;

        // b. Let catchFinally be onFinally.
        catch_finally = on_finally;
    }
    // 6. Else,
    else {
        // a. Let thenFinallyClosure be a new Abstract Closure with parameters (value) that captures onFinally and C and performs the following steps when called:
        // b. Let thenFinally be ! CreateBuiltinFunction(thenFinallyClosure, 1, "", « »).
        auto* then_finally_function = NativeFunction::create(global_object, "", [constructor_handle = make_handle(constructor), on_finally_handle = make_handle(&on_finally.as_function())](auto& vm, auto& global_object) -> ThrowCompletionOr<Value> {
            auto& constructor = const_cast<FunctionObject&>(*constructor_handle.cell());
            auto& on_finally = const_cast<FunctionObject&>(*on_finally_handle.cell());
            auto value = vm.argument(0);

            // i. Let result be ? Call(onFinally, undefined).
            auto result = TRY(vm.call(on_finally, js_undefined()));

            // ii. Let promise be ? PromiseResolve(C, result).
            auto* promise = TRY(promise_resolve(global_object, constructor, result));

            // iii. Let returnValue be a new Abstract Closure with no parameters that captures value and performs the following steps when called:
            // iv. Let valueThunk be ! CreateBuiltinFunction(returnValue, 0, "", « »).
            auto* value_thunk = NativeFunction::create(global_object, "", [value](auto&, auto&) -> ThrowCompletionOr<Value> {
                // 1. Return value.
                return value;
            });

            // v. Return ? Invoke(promise, "then", « valueThunk »).
            return TRY(Value(promise).invoke(global_object, vm.names.then, value_thunk));
        });
        then_finally_function->define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
        then_finally = Value(then_finally_function);

        // c. Let catchFinallyClosure be a new Abstract Closure with parameters (reason) that captures onFinally and C and performs the following steps when called:
        // d. Let catchFinally be ! CreateBuiltinFunction(catchFinallyClosure, 1, "", « »).
        auto* catch_finally_function = NativeFunction::create(global_object, "", [constructor_handle = make_handle(constructor), on_finally_handle = make_handle(&on_finally.as_function())](auto& vm, auto& global_object) -> ThrowCompletionOr<Value> {
            auto& constructor = const_cast<FunctionObject&>(*constructor_handle.cell());
            auto& on_finally = const_cast<FunctionObject&>(*on_finally_handle.cell());
            auto reason = vm.argument(0);

            // i. Let result be ? Call(onFinally, undefined).
            auto result = TRY(vm.call(on_finally, js_undefined()));

            // ii. Let promise be ? PromiseResolve(C, result).
            auto* promise = TRY(promise_resolve(global_object, constructor, result));

            // iv. Let thrower be ! CreateBuiltinFunction(throwReason, 0, "", « »).
            auto* thrower = NativeFunction::create(global_object, "", [reason](auto& vm, auto& global_object) -> ThrowCompletionOr<Value> {
                // 1. Return ThrowCompletion(reason).
                vm.throw_exception(global_object, reason);
                return throw_completion(reason);
            });

            // v. Return ? Invoke(promise, "then", « thrower »).
            return TRY(Value(promise).invoke(global_object, vm.names.then, thrower));
        });
        catch_finally_function->define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
        catch_finally = Value(catch_finally_function);
    }

    // 7. Return ? Invoke(promise, "then", « thenFinally, catchFinally »).
    return TRY(Value(promise).invoke(global_object, vm.names.then, then_finally, catch_finally));
}

}
