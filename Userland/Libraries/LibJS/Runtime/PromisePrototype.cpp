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
    auto* promise = typed_this_object(global_object);
    if (!promise)
        return {};
    auto on_fulfilled = vm.argument(0);
    auto on_rejected = vm.argument(1);
    auto* constructor = TRY_OR_DISCARD(species_constructor(global_object, *promise, *global_object.promise_constructor()));
    auto result_capability = new_promise_capability(global_object, constructor);
    if (vm.exception())
        return {};
    return promise->perform_then(on_fulfilled, on_rejected, result_capability);
}

// 27.2.5.1 Promise.prototype.catch ( onRejected ), https://tc39.es/ecma262/#sec-promise.prototype.catch
JS_DEFINE_NATIVE_FUNCTION(PromisePrototype::catch_)
{
    auto this_value = vm.this_value(global_object);
    auto on_rejected = vm.argument(0);
    return TRY_OR_DISCARD(this_value.invoke(global_object, vm.names.then, js_undefined(), on_rejected));
}

// 27.2.5.3 Promise.prototype.finally ( onFinally ), https://tc39.es/ecma262/#sec-promise.prototype.finally
JS_DEFINE_NATIVE_FUNCTION(PromisePrototype::finally)
{
    auto* promise = TRY_OR_DISCARD(vm.this_value(global_object).to_object(global_object));
    auto* constructor = TRY_OR_DISCARD(species_constructor(global_object, *promise, *global_object.promise_constructor()));
    Value then_finally;
    Value catch_finally;
    auto on_finally = vm.argument(0);
    if (!on_finally.is_function()) {
        then_finally = on_finally;
        catch_finally = on_finally;
    } else {
        // 27.2.5.3.1 Then Finally Functions, https://tc39.es/ecma262/#sec-thenfinallyfunctions
        auto* then_finally_function = NativeFunction::create(global_object, "", [constructor_handle = make_handle(constructor), on_finally_handle = make_handle(&on_finally.as_function())](auto& vm, auto& global_object) -> Value {
            auto& constructor = const_cast<FunctionObject&>(*constructor_handle.cell());
            auto& on_finally = const_cast<FunctionObject&>(*on_finally_handle.cell());
            auto value = vm.argument(0);
            auto result = TRY_OR_DISCARD(vm.call(on_finally, js_undefined()));
            auto* promise = promise_resolve(global_object, constructor, result);
            if (vm.exception())
                return {};
            auto* value_thunk = NativeFunction::create(global_object, "", [value](auto&, auto&) -> Value {
                return value;
            });
            return TRY_OR_DISCARD(Value(promise).invoke(global_object, vm.names.then, value_thunk));
        });
        then_finally_function->define_direct_property(vm.names.length, Value(1), Attribute::Configurable);

        // 27.2.5.3.2 Catch Finally Functions, https://tc39.es/ecma262/#sec-catchfinallyfunctions
        auto* catch_finally_function = NativeFunction::create(global_object, "", [constructor_handle = make_handle(constructor), on_finally_handle = make_handle(&on_finally.as_function())](auto& vm, auto& global_object) -> Value {
            auto& constructor = const_cast<FunctionObject&>(*constructor_handle.cell());
            auto& on_finally = const_cast<FunctionObject&>(*on_finally_handle.cell());
            auto reason = vm.argument(0);
            auto result = TRY_OR_DISCARD(vm.call(on_finally, js_undefined()));
            auto* promise = promise_resolve(global_object, constructor, result);
            if (vm.exception())
                return {};
            auto* thrower = NativeFunction::create(global_object, "", [reason](auto& vm, auto& global_object) -> Value {
                vm.throw_exception(global_object, reason);
                return {};
            });
            return TRY_OR_DISCARD(Value(promise).invoke(global_object, vm.names.then, thrower));
        });
        catch_finally_function->define_direct_property(vm.names.length, Value(1), Attribute::Configurable);

        then_finally = Value(then_finally_function);
        catch_finally = Value(catch_finally_function);
    }
    return TRY_OR_DISCARD(Value(promise).invoke(global_object, vm.names.then, then_finally, catch_finally));
}

}
