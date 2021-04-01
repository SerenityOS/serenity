/*
 * Copyright (c) 2021, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Function.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PromisePrototype.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

PromisePrototype::PromisePrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
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
}

static Promise* promise_from(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!is<Promise>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, vm.names.Promise);
        return nullptr;
    }
    return static_cast<Promise*>(this_object);
}

// 27.2.5.4 Promise.prototype.then, https://tc39.es/ecma262/#sec-promise.prototype.then
JS_DEFINE_NATIVE_FUNCTION(PromisePrototype::then)
{
    auto* promise = promise_from(vm, global_object);
    if (!promise)
        return {};
    auto on_fulfilled = vm.argument(0);
    auto on_rejected = vm.argument(1);
    auto* constructor = species_constructor(global_object, *promise, *global_object.promise_constructor());
    if (vm.exception())
        return {};
    auto result_capability = new_promise_capability(global_object, constructor);
    if (vm.exception())
        return {};
    return promise->perform_then(on_fulfilled, on_rejected, result_capability);
}

// 27.2.5.1 Promise.prototype.catch, https://tc39.es/ecma262/#sec-promise.prototype.catch
JS_DEFINE_NATIVE_FUNCTION(PromisePrototype::catch_)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    auto on_rejected = vm.argument(0);
    return this_object->invoke(vm.names.then, js_undefined(), on_rejected);
}

// 27.2.5.3 Promise.prototype.finally, https://tc39.es/ecma262/#sec-promise.prototype.finally
JS_DEFINE_NATIVE_FUNCTION(PromisePrototype::finally)
{
    auto* promise = vm.this_value(global_object).to_object(global_object);
    if (!promise)
        return {};
    auto* constructor = species_constructor(global_object, *promise, *global_object.promise_constructor());
    if (vm.exception())
        return {};
    Value then_finally;
    Value catch_finally;
    auto on_finally = vm.argument(0);
    if (!on_finally.is_function()) {
        then_finally = on_finally;
        catch_finally = on_finally;
    } else {
        // 27.2.5.3.1 Then Finally Functions, https://tc39.es/ecma262/#sec-thenfinallyfunctions
        auto* then_finally_function = NativeFunction::create(global_object, "", [constructor_handle = make_handle(constructor), on_finally_handle = make_handle(&on_finally.as_function())](auto& vm, auto& global_object) -> Value {
            auto& constructor = const_cast<Object&>(*constructor_handle.cell());
            auto& on_finally = const_cast<Function&>(*on_finally_handle.cell());
            auto value = vm.argument(0);
            auto result = vm.call(on_finally, js_undefined());
            if (vm.exception())
                return {};
            auto* promise = promise_resolve(global_object, constructor, result);
            if (vm.exception())
                return {};
            auto* value_thunk = NativeFunction::create(global_object, "", [value](auto&, auto&) -> Value {
                return value;
            });
            return promise->invoke(vm.names.then, value_thunk);
        });
        then_finally_function->define_property(vm.names.length, Value(1));

        // 27.2.5.3.2 Catch Finally Functions, https://tc39.es/ecma262/#sec-catchfinallyfunctions
        auto* catch_finally_function = NativeFunction::create(global_object, "", [constructor_handle = make_handle(constructor), on_finally_handle = make_handle(&on_finally.as_function())](auto& vm, auto& global_object) -> Value {
            auto& constructor = const_cast<Object&>(*constructor_handle.cell());
            auto& on_finally = const_cast<Function&>(*on_finally_handle.cell());
            auto reason = vm.argument(0);
            auto result = vm.call(on_finally, js_undefined());
            if (vm.exception())
                return {};
            auto* promise = promise_resolve(global_object, constructor, result);
            if (vm.exception())
                return {};
            auto* thrower = NativeFunction::create(global_object, "", [reason](auto& vm, auto& global_object) -> Value {
                vm.throw_exception(global_object, reason);
                return {};
            });
            return promise->invoke(vm.names.then, thrower);
        });
        catch_finally_function->define_property(vm.names.length, Value(1));

        then_finally = Value(then_finally_function);
        catch_finally = Value(catch_finally_function);
    }
    return promise->invoke(vm.names.then, then_finally, catch_finally);
}

}
