/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AsyncGeneratorFunctionConstructor.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

AsyncGeneratorFunctionConstructor::AsyncGeneratorFunctionConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.AsyncGeneratorFunction.as_string(), *global_object.function_prototype())
{
}

void AsyncGeneratorFunctionConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 27.4.2.1 AsyncGeneratorFunction.length, https://tc39.es/ecma262/#sec-asyncgeneratorfunction-length
    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);

    // 27.4.2.2 AsyncGeneratorFunction.prototype, https://tc39.es/ecma262/#sec-asyncgeneratorfunction-prototype
    define_direct_property(vm.names.prototype, global_object.async_generator_function_prototype(), 0);
}

// 27.4.1.1 AsyncGeneratorFunction ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-asyncgeneratorfunction
ThrowCompletionOr<Value> AsyncGeneratorFunctionConstructor::call()
{
    return TRY(construct(*this));
}

// 27.4.1.1 AsyncGeneratorFunction ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-asyncgeneratorfunction
ThrowCompletionOr<Object*> AsyncGeneratorFunctionConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Let C be the active function object.
    auto* constructor = vm.active_function_object();

    // 2. Let args be the argumentsList that was passed to this function by [[Call]] or [[Construct]].
    auto& args = vm.running_execution_context().arguments;

    // 3. Return ? CreateDynamicFunction(C, NewTarget, asyncGenerator, args).
    return TRY(FunctionConstructor::create_dynamic_function(global_object, *constructor, &new_target, FunctionKind::AsyncGenerator, args));
}

}
