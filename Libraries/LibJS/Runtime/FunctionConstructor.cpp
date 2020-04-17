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
#include <LibJS/Runtime/ScriptFunction.h>

namespace JS {

FunctionConstructor::FunctionConstructor()
    : NativeFunction("Function")
{
    put("prototype", interpreter().function_prototype());
    put("length", Value(1));
}

FunctionConstructor::~FunctionConstructor()
{
}

Value FunctionConstructor::call(Interpreter& interpreter)
{
    return construct(interpreter);
}

Value FunctionConstructor::construct(Interpreter& interpreter)
{
    String parameters_source = "";
    String body_source = "";
    if (interpreter.argument_count() == 1)
        body_source = interpreter.argument(0).to_string();
    if (interpreter.argument_count() > 1) {
        Vector<String> parameters;
        for (size_t i = 0; i < interpreter.argument_count() - 1; ++i)
            parameters.append(interpreter.argument(i).to_string());
        StringBuilder parameters_builder;
        parameters_builder.join(',', parameters);
        parameters_source = parameters_builder.build();
        body_source = interpreter.argument(interpreter.argument_count() - 1).to_string();
    }
    auto source = String::format("function (%s) { %s }", parameters_source.characters(), body_source.characters());
    auto parser = Parser(Lexer(source));
    auto function_expression = parser.parse_function_node<FunctionExpression>();
    if (parser.has_errors()) {
        // FIXME: The parser should expose parsing error strings rather than just fprintf()'ing them
        return Error::create(interpreter.global_object(), "SyntaxError", "");
    }
    return function_expression->execute(interpreter);
}

}
