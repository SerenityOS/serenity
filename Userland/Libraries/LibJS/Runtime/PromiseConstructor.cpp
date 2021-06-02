/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

PromiseConstructor::PromiseConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Promise, *global_object.function_prototype())
{
}

void PromiseConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    define_property(vm.names.prototype, global_object.promise_prototype());
    define_property(vm.names.length, Value(1));

    u8 attr = Attribute::Writable | Attribute::Configurable;
    // TODO: Implement these functions below and uncomment this.
    // define_native_function(vm.names.all, all, 1, attr);
    // define_native_function(vm.names.allSettled, all_settled, 1, attr);
    // define_native_function(vm.names.any, any, 1, attr);
    // define_native_function(vm.names.race, race, 1, attr);
    define_native_function(vm.names.reject, reject, 1, attr);
    define_native_function(vm.names.resolve, resolve, 1, attr);
}

Value PromiseConstructor::call()
{
    auto& vm = this->vm();
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.Promise);
    return {};
}

// 27.2.3.1 Promise, https://tc39.es/ecma262/#sec-promise-executor
Value PromiseConstructor::construct(Function&)
{
    auto& vm = this->vm();
    auto executor = vm.argument(0);
    if (!executor.is_function()) {
        vm.throw_exception<TypeError>(global_object(), ErrorType::PromiseExecutorNotAFunction);
        return {};
    }
    auto* promise = Promise::create(global_object());
    auto [resolve_function, reject_function] = promise->create_resolving_functions();

    auto completion_value = vm.call(executor.as_function(), js_undefined(), &resolve_function, &reject_function);
    if (vm.exception()) {
        vm.clear_exception();
        vm.stop_unwind();
        [[maybe_unused]] auto result = vm.call(reject_function, js_undefined(), completion_value);
    }
    return promise;
}

// 27.2.4.1 Promise.all, https://tc39.es/ecma262/#sec-promise.all
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::all)
{
    TODO();
}

// 27.2.4.2 Promise.allSettled, https://tc39.es/ecma262/#sec-promise.allsettled
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::all_settled)
{
    TODO();
}

// 27.2.4.3 Promise.any, https://tc39.es/ecma262/#sec-promise.any
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::any)
{
    TODO();
}

// 27.2.4.5 Promise.race, https://tc39.es/ecma262/#sec-promise.race
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::race)
{
    TODO();
}

// 27.2.4.6 Promise.reject, https://tc39.es/ecma262/#sec-promise.reject
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::reject)
{
    auto* constructor = vm.this_value(global_object).to_object(global_object);
    if (!constructor)
        return {};
    auto promise_capability = new_promise_capability(global_object, constructor);
    if (vm.exception())
        return {};
    auto reason = vm.argument(0);
    [[maybe_unused]] auto result = vm.call(*promise_capability.reject, js_undefined(), reason);
    return promise_capability.promise;
}

// 27.2.4.7 Promise.resolve, https://tc39.es/ecma262/#sec-promise.resolve
JS_DEFINE_NATIVE_FUNCTION(PromiseConstructor::resolve)
{
    auto* constructor = vm.this_value(global_object).to_object(global_object);
    if (!constructor)
        return {};
    auto value = vm.argument(0);
    return promise_resolve(global_object, *constructor, value);
}

}
