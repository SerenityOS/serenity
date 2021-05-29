/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

ScriptFunction* ScriptFunction::create(GlobalObject& global_object, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, ScopeObject* parent_scope, bool is_strict, bool is_arrow_function)
{
    return global_object.heap().allocate<ScriptFunction>(global_object, global_object, name, body, move(parameters), m_function_length, parent_scope, *global_object.function_prototype(), is_strict, is_arrow_function);
}

ScriptFunction::ScriptFunction(GlobalObject& global_object, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, ScopeObject* parent_scope, Object& prototype, bool is_strict, bool is_arrow_function)
    : Function(prototype, is_arrow_function ? vm().this_value(global_object) : Value(), {})
    , m_name(name)
    , m_body(body)
    , m_parameters(move(parameters))
    , m_parent_scope(parent_scope)
    , m_function_length(m_function_length)
    , m_is_strict(is_strict)
    , m_is_arrow_function(is_arrow_function)
{
}

void ScriptFunction::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Function::initialize(global_object);
    if (!m_is_arrow_function) {
        Object* prototype = vm.heap().allocate<Object>(global_object, *global_object.new_script_function_prototype_object_shape());
        prototype->define_property(vm.names.constructor, this, Attribute::Writable | Attribute::Configurable);
        define_property(vm.names.prototype, prototype, Attribute::Writable);
    }
    define_native_property(vm.names.length, length_getter, {}, Attribute::Configurable);
    define_native_property(vm.names.name, name_getter, {}, Attribute::Configurable);
}

ScriptFunction::~ScriptFunction()
{
}

void ScriptFunction::visit_edges(Visitor& visitor)
{
    Function::visit_edges(visitor);
    visitor.visit(m_parent_scope);
}

LexicalEnvironment* ScriptFunction::create_environment()
{
    HashMap<FlyString, Variable> variables;
    for (auto& parameter : m_parameters) {
        parameter.binding.visit(
            [&](const FlyString& name) { variables.set(name, { js_undefined(), DeclarationKind::Var }); },
            [&](const NonnullRefPtr<BindingPattern>& binding) {
                binding->for_each_assigned_name([&](const auto& name) {
                    variables.set(name, { js_undefined(), DeclarationKind::Var });
                });
            });
    }

    if (is<ScopeNode>(body())) {
        for (auto& declaration : static_cast<const ScopeNode&>(body()).variables()) {
            for (auto& declarator : declaration.declarations()) {
                declarator.target().visit(
                    [&](const NonnullRefPtr<Identifier>& id) {
                        variables.set(id->string(), { js_undefined(), declaration.declaration_kind() });
                    },
                    [&](const NonnullRefPtr<BindingPattern>& binding) {
                        binding->for_each_assigned_name([&](const auto& name) {
                            variables.set(name, { js_undefined(), declaration.declaration_kind() });
                        });
                    });
            }
        }
    }

    auto* environment = heap().allocate<LexicalEnvironment>(global_object(), move(variables), m_parent_scope, LexicalEnvironment::EnvironmentRecordType::Function);
    environment->set_home_object(home_object());
    environment->set_current_function(*this);
    if (m_is_arrow_function) {
        if (is<LexicalEnvironment>(m_parent_scope))
            environment->set_new_target(static_cast<LexicalEnvironment*>(m_parent_scope)->new_target());
    }
    return environment;
}

Value ScriptFunction::execute_function_body()
{
    auto& vm = this->vm();

    OwnPtr<Interpreter> local_interpreter;
    Interpreter* interpreter = vm.interpreter_if_exists();

    if (!interpreter) {
        local_interpreter = Interpreter::create_with_existing_global_object(global_object());
        interpreter = local_interpreter.ptr();
    }

    VM::InterpreterExecutionScope scope(*interpreter);

    auto& call_frame_args = vm.call_frame().arguments;
    for (size_t i = 0; i < m_parameters.size(); ++i) {
        auto& parameter = m_parameters[i];
        parameter.binding.visit(
            [&](const auto& param) {
                Value argument_value;
                if (parameter.is_rest) {
                    auto* array = Array::create(global_object());
                    for (size_t rest_index = i; rest_index < call_frame_args.size(); ++rest_index)
                        array->indexed_properties().append(call_frame_args[rest_index]);
                    argument_value = move(array);
                } else if (i < call_frame_args.size() && !call_frame_args[i].is_undefined()) {
                    argument_value = call_frame_args[i];
                } else if (parameter.default_value) {
                    argument_value = parameter.default_value->execute(*interpreter, global_object());
                    if (vm.exception())
                        return;
                } else {
                    argument_value = js_undefined();
                }

                vm.assign(param, argument_value, global_object(), true, vm.current_scope());
            });

        if (vm.exception())
            return {};
    }

    return interpreter->execute_statement(global_object(), m_body, ScopeType::Function);
}

Value ScriptFunction::call()
{
    if (m_is_class_constructor) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ClassConstructorWithoutNew, m_name);
        return {};
    }
    return execute_function_body();
}

Value ScriptFunction::construct(Function&)
{
    if (m_is_arrow_function) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::NotAConstructor, m_name);
        return {};
    }
    return execute_function_body();
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
