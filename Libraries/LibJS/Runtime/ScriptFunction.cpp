/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
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
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

static ScriptFunction* script_function_from(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return nullptr;
    if (!this_object->is_function()) {
        interpreter.throw_exception<TypeError>("Not a function");
        return nullptr;
    }
    return static_cast<ScriptFunction*>(this_object);
}

ScriptFunction* ScriptFunction::create(GlobalObject& global_object, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, LexicalEnvironment* parent_environment)
{
    return global_object.heap().allocate<ScriptFunction>(name, body, move(parameters), parent_environment, *global_object.function_prototype());
}

ScriptFunction::ScriptFunction(const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, LexicalEnvironment* parent_environment, Object& prototype)
    : Function(prototype)
    , m_name(name)
    , m_body(body)
    , m_parameters(move(parameters))
    , m_parent_environment(parent_environment)
{
    put("prototype", Object::create_empty(interpreter(), interpreter().global_object()), 0);
    put_native_property("length", length_getter, nullptr, Attribute::Configurable);
    put_native_property("name", name_getter, nullptr, Attribute::Configurable);
}

ScriptFunction::~ScriptFunction()
{
}

void ScriptFunction::visit_children(Visitor& visitor)
{
    Function::visit_children(visitor);
    visitor.visit(m_parent_environment);
}

LexicalEnvironment* ScriptFunction::create_environment()
{
    HashMap<FlyString, Variable> variables;
    for (auto& parameter : m_parameters) {
        variables.set(parameter.name, { js_undefined(), DeclarationKind::Var });
    }

    if (body().is_scope_node()) {
        for (auto& declaration : static_cast<const ScopeNode&>(body()).variables()) {
            for (auto& declarator : declaration.declarations()) {
                variables.set(declarator.id().string(), { js_undefined(), DeclarationKind::Var });
            }
        }
    }
    if (variables.is_empty())
        return m_parent_environment;
    return heap().allocate<LexicalEnvironment>(move(variables), m_parent_environment);
}

Value ScriptFunction::call(Interpreter& interpreter)
{
    auto& argument_values = interpreter.call_frame().arguments;
    ArgumentVector arguments;
    for (size_t i = 0; i < m_parameters.size(); ++i) {
        auto parameter = parameters()[i];
        auto value = js_undefined();
        if (parameter.is_rest) {
            auto* array = Array::create(interpreter.global_object());
            for (size_t rest_index = i; rest_index < argument_values.size(); ++rest_index)
                array->elements().append(argument_values[rest_index]);
            value = Value(array);
        } else {
            if (i < argument_values.size() && !argument_values[i].is_undefined()) {
                value = argument_values[i];
            } else if (parameter.default_value) {
                value = parameter.default_value->execute(interpreter);
                if (interpreter.exception())
                    return {};
            }
        }
        arguments.append({ parameter.name, value });
        interpreter.current_environment()->set(parameter.name, { value, DeclarationKind::Var });
    }
    return interpreter.run(m_body, arguments, ScopeType::Function);
}

Value ScriptFunction::construct(Interpreter& interpreter)
{
    return call(interpreter);
}

Value ScriptFunction::length_getter(Interpreter& interpreter)
{
    auto* function = script_function_from(interpreter);
    if (!function)
        return {};
    return Value(static_cast<i32>(function->parameters().size()));
}

Value ScriptFunction::name_getter(Interpreter& interpreter)
{
    auto* function = script_function_from(interpreter);
    if (!function)
        return {};
    return js_string(interpreter, function->name().is_null() ? "" : function->name());
}

}
