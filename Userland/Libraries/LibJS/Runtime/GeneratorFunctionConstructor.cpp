/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/GeneratorFunctionConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/OrdinaryFunctionObject.h>

namespace JS {

GeneratorFunctionConstructor::GeneratorFunctionConstructor(GlobalObject& global_object)
    : NativeFunction(*static_cast<Object*>(global_object.function_constructor()))
{
}

void GeneratorFunctionConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 27.3.2.1 GeneratorFunction.length, https://tc39.es/ecma262/#sec-generatorfunction.length
    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
    // 27.3.2.2 GeneratorFunction.prototype, https://tc39.es/ecma262/#sec-generatorfunction.length
    define_direct_property(vm.names.prototype, global_object.generator_function_prototype(), 0);
}

GeneratorFunctionConstructor::~GeneratorFunctionConstructor()
{
}

// 27.3.1.1 GeneratorFunction ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-generatorfunction
Value GeneratorFunctionConstructor::call()
{
    return construct(*this);
}

// 27.3.1.1 GeneratorFunction ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-generatorfunction
Value GeneratorFunctionConstructor::construct(FunctionObject& new_target)
{
    auto function = FunctionConstructor::create_dynamic_function_node(global_object(), new_target, FunctionKind::Generator);
    if (!function)
        return {};

    auto* bytecode_interpreter = Bytecode::Interpreter::current();
    VERIFY(bytecode_interpreter);

    auto executable = Bytecode::Generator::generate(function->body(), true);
    auto& passes = JS::Bytecode::Interpreter::optimization_pipeline();
    passes.perform(executable);
    if constexpr (JS_BYTECODE_DEBUG) {
        dbgln("Optimisation passes took {}us", passes.elapsed());
        dbgln("Compiled Bytecode::Block for function '{}':", function->name());
        for (auto& block : executable.basic_blocks)
            block.dump(executable);
    }

    return OrdinaryFunctionObject::create(global_object(), function->name(), function->body(), function->parameters(), function->function_length(), vm().lexical_environment(), FunctionKind::Generator, function->is_strict_mode(), false);
}

}
