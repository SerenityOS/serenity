/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ScriptFunction.h>

namespace JS {

FunctionConstructor::FunctionConstructor(GlobalObject& global_object)
    : NativeFunction("Function", *global_object.function_prototype())
{
}

void FunctionConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);
    define_property("prototype", global_object.function_prototype(), 0);
    define_property("length", Value(1), Attribute::Configurable);
}

FunctionConstructor::~FunctionConstructor()
{
}

Value FunctionConstructor::call()
{
    return construct(*this);
}

Value FunctionConstructor::construct(Function&)
{
    auto& vm = this->vm();
    String parameters_source = "";
    String body_source = "";
    if (vm.argument_count() == 1) {
        body_source = vm.argument(0).to_string(global_object());
        if (vm.exception())
            return {};
    }
    if (vm.argument_count() > 1) {
        Vector<String> parameters;
        for (size_t i = 0; i < vm.argument_count() - 1; ++i) {
            parameters.append(vm.argument(i).to_string(global_object()));
            if (vm.exception())
                return {};
        }
        StringBuilder parameters_builder;
        parameters_builder.join(',', parameters);
        parameters_source = parameters_builder.build();
        body_source = vm.argument(vm.argument_count() - 1).to_string(global_object());
        if (vm.exception())
            return {};
    }
    auto source = String::formatted("function anonymous({}) {{ {} }}", parameters_source, body_source);
    auto parser = Parser(Lexer(source));
    auto function_expression = parser.parse_function_node<FunctionExpression>();
    if (parser.has_errors()) {
        auto error = parser.errors()[0];
        vm.throw_exception<SyntaxError>(global_object(), error.to_string());
        return {};
    }

    OwnPtr<Interpreter> local_interpreter;
    Interpreter* interpreter = vm.interpreter_if_exists();

    if (!interpreter) {
        local_interpreter = Interpreter::create_with_existing_global_object(global_object());
        interpreter = local_interpreter.ptr();
    }

    VM::InterpreterExecutionScope scope(*interpreter);
    return function_expression->execute(*interpreter, global_object());
}

}
