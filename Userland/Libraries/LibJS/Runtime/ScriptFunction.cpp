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
#include <LibJS/Bytecode/PassManager.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GeneratorObject.h>
#include <LibJS/Runtime/GeneratorObjectPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
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

ScriptFunction* ScriptFunction::create(GlobalObject& global_object, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, ScopeObject* parent_scope, FunctionKind kind, bool is_strict, bool is_arrow_function)
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
    return global_object.heap().allocate<ScriptFunction>(global_object, global_object, name, body, move(parameters), m_function_length, parent_scope, *prototype, kind, is_strict, is_arrow_function);
}

ScriptFunction::ScriptFunction(GlobalObject& global_object, const FlyString& name, const Statement& body, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, ScopeObject* parent_scope, Object& prototype, FunctionKind kind, bool is_strict, bool is_arrow_function)
    : Function(is_arrow_function ? vm().this_value(global_object) : Value(), {}, prototype)
    , m_name(name)
    , m_body(body)
    , m_parameters(move(parameters))
    , m_parent_scope(parent_scope)
    , m_function_length(m_function_length)
    , m_kind(kind)
    , m_is_strict(is_strict)
    , m_is_arrow_function(is_arrow_function)
{
}

void ScriptFunction::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Function::initialize(global_object);
    if (!m_is_arrow_function) {
        auto* prototype = vm.heap().allocate<Object>(global_object, *global_object.new_script_function_prototype_object_shape());
        switch (m_kind) {
        case FunctionKind::Regular:
            prototype->define_property(vm.names.constructor, this, Attribute::Writable | Attribute::Configurable);
            break;
        case FunctionKind::Generator:
            // prototype is "g1.prototype" in figure-2 (https://tc39.es/ecma262/img/figure-2.png)
            prototype->set_prototype(global_object.generator_object_prototype());
            break;
        }
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

    Interpreter* ast_interpreter = nullptr;
    auto* bytecode_interpreter = Bytecode::Interpreter::current();

    auto prepare_arguments = [&] {
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
                        // FIXME: Support default arguments in the bytecode world!
                        if (!bytecode_interpreter)
                            argument_value = parameter.default_value->execute(*ast_interpreter, global_object());
                        if (vm.exception())
                            return;
                    } else {
                        argument_value = js_undefined();
                    }

                    if (i >= call_frame_args.size())
                        call_frame_args.resize(i + 1);
                    call_frame_args[i] = argument_value;
                    vm.assign(param, argument_value, global_object(), true, vm.current_scope());
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

        return GeneratorObject::create(global_object(), result, this, vm.call_frame().scope, bytecode_interpreter->snapshot_frame());
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
    if (m_is_arrow_function || m_kind == FunctionKind::Generator) {
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
