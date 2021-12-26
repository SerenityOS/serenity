/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

FunctionConstructor::FunctionConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Function, *global_object.function_prototype())
{
}

void FunctionConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 20.2.2.2 Function.prototype, https://tc39.es/ecma262/#sec-function.prototype
    define_property(vm.names.prototype, global_object.function_prototype(), 0);

    define_property(vm.names.length, Value(1), Attribute::Configurable);
}

FunctionConstructor::~FunctionConstructor()
{
}

// 20.2.1.1.1 CreateDynamicFunction ( constructor, newTarget, kind, args ), https://tc39.es/ecma262/#sec-createdynamicfunction
RefPtr<FunctionExpression> FunctionConstructor::create_dynamic_function_node(GlobalObject& global_object, Function&, FunctionKind kind)
{
    auto& vm = global_object.vm();
    String parameters_source = "";
    String body_source = "";
    if (vm.argument_count() == 1) {
        body_source = vm.argument(0).to_string(global_object);
        if (vm.exception())
            return {};
    }
    if (vm.argument_count() > 1) {
        Vector<String> parameters;
        for (size_t i = 0; i < vm.argument_count() - 1; ++i) {
            parameters.append(vm.argument(i).to_string(global_object));
            if (vm.exception())
                return {};
        }
        StringBuilder parameters_builder;
        parameters_builder.join(',', parameters);
        parameters_source = parameters_builder.build();
        body_source = vm.argument(vm.argument_count() - 1).to_string(global_object);
        if (vm.exception())
            return {};
    }
    auto is_generator = kind == FunctionKind::Generator;
    auto source = String::formatted("function{} anonymous({}\n) {{\n{}\n}}", is_generator ? "*" : "", parameters_source, body_source);
    auto parser = Parser(Lexer(source));
    auto function = parser.parse_function_node<FunctionExpression>();
    if (parser.has_errors()) {
        auto error = parser.errors()[0];
        vm.throw_exception<SyntaxError>(global_object, error.to_string());
        return {};
    }

    return function;
}

// 20.2.1.1 Function ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-function-p1-p2-pn-body
Value FunctionConstructor::call()
{
    return construct(*this);
}

// 20.2.1.1 Function ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-function-p1-p2-pn-body
Value FunctionConstructor::construct(Function& new_target)
{
    auto function = create_dynamic_function_node(global_object(), new_target, FunctionKind::Regular);
    if (!function)
        return {};

    OwnPtr<Interpreter> local_interpreter;
    Interpreter* interpreter = vm().interpreter_if_exists();

    if (!interpreter) {
        local_interpreter = Interpreter::create_with_existing_global_object(global_object());
        interpreter = local_interpreter.ptr();
    }

    VM::InterpreterExecutionScope scope(*interpreter);
    return function->execute(*interpreter, global_object());
}

}
