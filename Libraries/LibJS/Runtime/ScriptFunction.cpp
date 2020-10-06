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

static ScriptFunction* typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!this_object->is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunctionNoParam);
        return nullptr;
    }
    return static_cast<ScriptFunction*>(this_object);
}

ScriptFunction* ScriptFunction::create(GlobalObject& global_object, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, LexicalEnvironment* parent_environment, bool is_strict, bool is_arrow_function)
{
    return global_object.heap().allocate<ScriptFunction>(global_object, global_object, name, body, move(parameters), m_function_length, parent_environment, *global_object.function_prototype(), is_strict, is_arrow_function);
}

ScriptFunction::ScriptFunction(GlobalObject& global_object, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, LexicalEnvironment* parent_environment, Object& prototype, bool is_strict, bool is_arrow_function)
    : Function(prototype, is_arrow_function ? vm().this_value(global_object) : Value(), {})
    , m_name(name)
    , m_body(body)
    , m_parameters(move(parameters))
    , m_parent_environment(parent_environment)
    , m_function_length(m_function_length)
    , m_is_strict(is_strict)
    , m_is_arrow_function(is_arrow_function)
{
}

void ScriptFunction::initialize(GlobalObject& global_object)
{
    Function::initialize(global_object);
    if (!m_is_arrow_function) {
        Object* prototype = Object::create_empty(global_object);
        prototype->define_property_without_transition("constructor", this, Attribute::Writable | Attribute::Configurable);
        define_property("prototype", prototype, 0);
    }
    define_native_property("length", length_getter, nullptr, Attribute::Configurable);
    define_native_property("name", name_getter, nullptr, Attribute::Configurable);
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

    auto* environment = heap().allocate<LexicalEnvironment>(global_object(), move(variables), m_parent_environment, LexicalEnvironment::EnvironmentRecordType::Function);
    environment->set_home_object(home_object());
    environment->set_current_function(*this);
    return environment;
}

Value ScriptFunction::call()
{
    OwnPtr<Interpreter> local_interpreter;
    Interpreter* interpreter = vm().interpreter_if_exists();

    if (!interpreter) {
        local_interpreter = Interpreter::create_with_existing_global_object(global_object());
        interpreter = local_interpreter.ptr();
    }

    VM::InterpreterExecutionScope scope(*interpreter);

    auto& argument_values = vm().call_frame().arguments;
    ArgumentVector arguments;
    for (size_t i = 0; i < m_parameters.size(); ++i) {
        auto parameter = parameters()[i];
        auto value = js_undefined();
        if (parameter.is_rest) {
            auto* array = Array::create(global_object());
            for (size_t rest_index = i; rest_index < argument_values.size(); ++rest_index)
                array->indexed_properties().append(argument_values[rest_index]);
            value = Value(array);
        } else {
            if (i < argument_values.size() && !argument_values[i].is_undefined()) {
                value = argument_values[i];
            } else if (parameter.default_value) {
                value = parameter.default_value->execute(*interpreter, global_object());
                if (vm().exception())
                    return {};
            }
        }
        arguments.append({ parameter.name, value });
        vm().current_environment()->set(global_object(), parameter.name, { value, DeclarationKind::Var });
    }

    return interpreter->execute_statement(global_object(), m_body, arguments, ScopeType::Function);
}

Value ScriptFunction::construct(Function&)
{
    if (m_is_arrow_function) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::NotAConstructor, m_name);
        return {};
    }
    return call();
}

JS_DEFINE_NATIVE_GETTER(ScriptFunction::length_getter)
{
    auto* function = typed_this(vm, global_object);
    if (!function)
        return {};
    return Value(static_cast<i32>(function->m_function_length));
}

JS_DEFINE_NATIVE_GETTER(ScriptFunction::name_getter)
{
    auto* function = typed_this(vm, global_object);
    if (!function)
        return {};
    return js_string(vm, function->name().is_null() ? "" : function->name());
}

}
