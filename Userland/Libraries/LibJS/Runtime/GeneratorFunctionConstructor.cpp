/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Optional.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/GeneratorFunctionConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>

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
ThrowCompletionOr<Value> GeneratorFunctionConstructor::call()
{
    return TRY(construct(*this));
}

// 27.3.1.1 GeneratorFunction ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-generatorfunction
ThrowCompletionOr<Object*> GeneratorFunctionConstructor::construct(FunctionObject& new_target)
{
    auto function = TRY(FunctionConstructor::create_dynamic_function_node(global_object(), new_target, FunctionKind::Generator));

    auto* bytecode_interpreter = Bytecode::Interpreter::current();
    VERIFY(bytecode_interpreter);

    auto executable = Bytecode::Generator::generate(function->body(), FunctionKind::Generator);
    auto& passes = JS::Bytecode::Interpreter::optimization_pipeline();
    passes.perform(executable);
    if constexpr (JS_BYTECODE_DEBUG) {
        dbgln("Optimisation passes took {}us", passes.elapsed());
        dbgln("Compiled Bytecode::Block for function '{}':", function->name());
        for (auto& block : executable.basic_blocks)
            block.dump(executable);
    }

    return ECMAScriptFunctionObject::create(global_object(), function->name(), function->body(), function->parameters(), function->function_length(), vm().lexical_environment(), nullptr, FunctionKind::Generator, function->is_strict_mode(), function->might_need_arguments_object());
}

}
