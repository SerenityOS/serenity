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

#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/FunctionPrototype.h>
#include <LibJS/Runtime/ScriptFunction.h>

namespace JS {

FunctionPrototype::FunctionPrototype()
    : Object(interpreter().object_prototype())
{
}

void FunctionPrototype::initialize()
{
    put_native_function("apply", apply, 2);
    put_native_function("bind", bind, 1);
    put_native_function("call", call, 1);
    put_native_function("toString", to_string);
    put("length", Value(0));
}

FunctionPrototype::~FunctionPrototype()
{
}

Value FunctionPrototype::apply(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!this_object->is_function())
        return interpreter.throw_exception<TypeError>("Not a Function object");
    auto function = static_cast<Function*>(this_object);
    auto this_arg = interpreter.argument(0);
    auto arg_array = interpreter.argument(1);
    if (arg_array.is_null() || arg_array.is_undefined())
        return interpreter.call(function, this_arg);
    if (!arg_array.is_object())
        return interpreter.throw_exception<TypeError>("argument array must be an object");
    size_t length = 0;
    auto length_property = arg_array.as_object().get("length");
    if (length_property.has_value())
        length = length_property.value().to_number().to_i32();
    Vector<Value> arguments;
    for (size_t i = 0; i < length; ++i)
        arguments.append(arg_array.as_object().get(String::number(i)).value_or(js_undefined()));
    return interpreter.call(function, this_arg, arguments);
}

Value FunctionPrototype::bind(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    // FIXME: Implement me :^)
    ASSERT_NOT_REACHED();
}

Value FunctionPrototype::call(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!this_object->is_function())
        return interpreter.throw_exception<TypeError>("Not a Function object");
    auto function = static_cast<Function*>(this_object);
    auto this_arg = interpreter.argument(0);
    Vector<Value> arguments;
    if (interpreter.argument_count() > 1) {
        for (size_t i = 1; i < interpreter.argument_count(); ++i)
            arguments.append(interpreter.argument(i));
    }
    return interpreter.call(function, this_arg, arguments);
}

Value FunctionPrototype::to_string(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!this_object->is_function())
        return interpreter.throw_exception<TypeError>("Not a Function object");

    String function_name = static_cast<Function*>(this_object)->name();
    String function_parameters = "";
    String function_body;

    if (this_object->is_native_function()) {
        function_body = String::format("  [%s]", this_object->class_name());
    } else {
        auto& parameters = static_cast<ScriptFunction*>(this_object)->parameters();
        StringBuilder parameters_builder;
        parameters_builder.join(", ", parameters);
        function_parameters = parameters_builder.build();
        // FIXME: ASTNodes should be able to dump themselves to source strings - something like this:
        // auto& body = static_cast<ScriptFunction*>(this_object)->body();
        // function_body = body.to_source();
        function_body = "  ???";
    }

    auto function_source = String::format("function %s(%s) {\n%s\n}",
        function_name.is_null() ? "" : function_name.characters(),
        function_parameters.characters(),
        function_body.characters());
    return js_string(interpreter, function_source);
}

}
