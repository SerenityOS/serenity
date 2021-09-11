/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Function.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GeneratorObject.h>
#include <LibJS/Runtime/GeneratorObjectPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/OrdinaryFunctionObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

OrdinaryFunctionObject* OrdinaryFunctionObject::create(GlobalObject& global_object, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, Environment* parent_scope, FunctionKind kind, bool is_strict, bool is_arrow_function)
{
    Object* prototype = nullptr;
    switch (kind) {
    case FunctionKind::Regular:
        prototype = global_object.function_prototype();
        break;
    case FunctionKind::Generator:
        prototype = global_object.generator_function_prototype();
        break;
    }
    return global_object.heap().allocate<OrdinaryFunctionObject>(global_object, global_object, name, body, move(parameters), m_function_length, parent_scope, *prototype, kind, is_strict, is_arrow_function);
}

OrdinaryFunctionObject::OrdinaryFunctionObject(GlobalObject& global_object, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 function_length, Environment* parent_scope, Object& prototype, FunctionKind kind, bool is_strict, bool is_arrow_function)
    : FunctionObject(is_arrow_function ? vm().this_value(global_object) : Value(), {}, prototype)
    , m_name(name)
    , m_body(body)
    , m_parameters(move(parameters))
    , m_environment(parent_scope)
    , m_realm(&global_object)
    , m_function_length(function_length)
    , m_kind(kind)
    , m_is_strict(is_strict)
    , m_is_arrow_function(is_arrow_function)
{
    // NOTE: This logic is from OrdinaryFunctionCreate, https://tc39.es/ecma262/#sec-ordinaryfunctioncreate
    if (m_is_arrow_function)
        set_this_mode(ThisMode::Lexical);
    else if (m_is_strict)
        set_this_mode(ThisMode::Strict);
    else
        set_this_mode(ThisMode::Global);

    // 15.1.3 Static Semantics: IsSimpleParameterList, https://tc39.es/ecma262/#sec-static-semantics-issimpleparameterlist
    set_has_simple_parameter_list(all_of(m_parameters, [&](auto& parameter) {
        if (parameter.is_rest)
            return false;
        if (parameter.default_value)
            return false;
        if (!parameter.binding.template has<FlyString>())
            return false;
        return true;
    }));
}

void OrdinaryFunctionObject::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Base::initialize(global_object);
    if (!m_is_arrow_function) {
        auto* prototype = vm.heap().allocate<Object>(global_object, *global_object.new_ordinary_function_prototype_object_shape());
        switch (m_kind) {
        case FunctionKind::Regular:
            prototype->define_property_or_throw(vm.names.constructor, { .value = this, .writable = true, .enumerable = false, .configurable = true });
            break;
        case FunctionKind::Generator:
            // prototype is "g1.prototype" in figure-2 (https://tc39.es/ecma262/img/figure-2.png)
            prototype->internal_set_prototype_of(global_object.generator_object_prototype());
            break;
        }
        define_direct_property(vm.names.prototype, prototype, Attribute::Writable);
    }
    define_property_or_throw(vm.names.length, { .value = Value(m_function_length), .writable = false, .enumerable = false, .configurable = true });
    define_property_or_throw(vm.names.name, { .value = js_string(vm, m_name.is_null() ? "" : m_name), .writable = false, .enumerable = false, .configurable = true });
}

OrdinaryFunctionObject::~OrdinaryFunctionObject()
{
}

void OrdinaryFunctionObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_environment);
    visitor.visit(m_realm);
}

FunctionEnvironment* OrdinaryFunctionObject::create_environment(FunctionObject& function_being_invoked)
{
    HashMap<FlyString, Variable> variables;
    for (auto& parameter : m_parameters) {
        parameter.binding.visit(
            [&](const FlyString& name) { variables.set(name, { js_undefined(), DeclarationKind::Var }); },
            [&](const NonnullRefPtr<BindingPattern>& binding) {
                binding->for_each_bound_name([&](const auto& name) {
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
                        binding->for_each_bound_name([&](const auto& name) {
                            variables.set(name, { js_undefined(), declaration.declaration_kind() });
                        });
                    });
            }
        }
    }

    auto* environment = heap().allocate<FunctionEnvironment>(global_object(), m_environment, variables);
    environment->set_function_object(function_being_invoked);
    if (m_is_arrow_function) {
        environment->set_this_binding_status(FunctionEnvironment::ThisBindingStatus::Lexical);
        if (is<FunctionEnvironment>(m_environment))
            environment->set_new_target(static_cast<FunctionEnvironment*>(m_environment)->new_target());
    }
    return environment;
}

Value OrdinaryFunctionObject::execute_function_body()
{
    auto& vm = this->vm();

    Interpreter* ast_interpreter = nullptr;
    auto* bytecode_interpreter = Bytecode::Interpreter::current();

    auto prepare_arguments = [&] {
        auto& execution_context_arguments = vm.running_execution_context().arguments;
        for (size_t i = 0; i < m_parameters.size(); ++i) {
            auto& parameter = m_parameters[i];
            parameter.binding.visit(
                [&](const auto& param) {
                    Value argument_value;
                    if (parameter.is_rest) {
                        auto* array = Array::create(global_object(), 0);
                        for (size_t rest_index = i; rest_index < execution_context_arguments.size(); ++rest_index)
                            array->indexed_properties().append(execution_context_arguments[rest_index]);
                        argument_value = move(array);
                    } else if (i < execution_context_arguments.size() && !execution_context_arguments[i].is_undefined()) {
                        argument_value = execution_context_arguments[i];
                    } else if (parameter.default_value) {
                        // FIXME: Support default arguments in the bytecode world!
                        if (!bytecode_interpreter)
                            argument_value = parameter.default_value->execute(*ast_interpreter, global_object());
                        if (vm.exception())
                            return;
                    } else {
                        argument_value = js_undefined();
                    }

                    vm.assign(param, argument_value, global_object(), true, vm.lexical_environment());
                });

            if (vm.exception())
                return;
        }
    };

    if (bytecode_interpreter) {
        prepare_arguments();
        if (!m_bytecode_executable.has_value()) {
            m_bytecode_executable = Bytecode::Generator::generate(m_body, m_kind == FunctionKind::Generator);
            auto& passes = JS::Bytecode::Interpreter::optimization_pipeline();
            passes.perform(*m_bytecode_executable);
            if constexpr (JS_BYTECODE_DEBUG) {
                dbgln("Optimisation passes took {}us", passes.elapsed());
                dbgln("Compiled Bytecode::Block for function '{}':", m_name);
                for (auto& block : m_bytecode_executable->basic_blocks)
                    block.dump(*m_bytecode_executable);
            }
        }
        auto result = bytecode_interpreter->run(*m_bytecode_executable);
        if (m_kind != FunctionKind::Generator)
            return result;

        return GeneratorObject::create(global_object(), result, this, vm.running_execution_context().lexical_environment, bytecode_interpreter->snapshot_frame());
    } else {
        VERIFY(m_kind != FunctionKind::Generator);
        OwnPtr<Interpreter> local_interpreter;
        ast_interpreter = vm.interpreter_if_exists();

        if (!ast_interpreter) {
            local_interpreter = Interpreter::create_with_existing_global_object(global_object());
            ast_interpreter = local_interpreter.ptr();
        }

        VM::InterpreterExecutionScope scope(*ast_interpreter);

        prepare_arguments();
        if (vm.exception())
            return {};

        return ast_interpreter->execute_statement(global_object(), m_body, ScopeType::Function);
    }
}

Value OrdinaryFunctionObject::call()
{
    if (m_is_class_constructor) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ClassConstructorWithoutNew, m_name);
        return {};
    }
    return execute_function_body();
}

Value OrdinaryFunctionObject::construct(FunctionObject&)
{
    if (m_is_arrow_function || m_kind == FunctionKind::Generator) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::NotAConstructor, m_name);
        return {};
    }
    return execute_function_body();
}

void OrdinaryFunctionObject::set_name(const FlyString& name)
{
    VERIFY(!name.is_null());
    auto& vm = this->vm();
    m_name = name;
    auto success = define_property_or_throw(vm.names.name, { .value = js_string(vm, m_name), .writable = false, .enumerable = false, .configurable = true });
    VERIFY(success);
}

}
