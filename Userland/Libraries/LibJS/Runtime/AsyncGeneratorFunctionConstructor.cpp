/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
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

    // 27.4.2.2 AsyncGeneratorFunction.prototype, https://tc39.es/ecma262/#sec-asyncgeneratorfunction-prototype
    define_direct_property(vm.names.prototype, global_object.async_generator_function_prototype(), 0);

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
    auto function = TRY(FunctionConstructor::create_dynamic_function_node(global_object(), new_target, FunctionKind::AsyncGenerator));

    OwnPtr<Interpreter> local_interpreter;
    Interpreter* interpreter = vm.interpreter_if_exists();

    if (!interpreter) {
        local_interpreter = Interpreter::create_with_existing_realm(*realm());
        interpreter = local_interpreter.ptr();
    }

    VM::InterpreterExecutionScope scope(*interpreter);
    auto result = function->execute(*interpreter, global_object());
    if (auto* exception = vm.exception())
        return throw_completion(exception->value());
    VERIFY(result.is_object() && is<ECMAScriptFunctionObject>(result.as_object()));
    return &result.as_object();
}

}
