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
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GeneratorObject.h>
#include <LibJS/Runtime/GeneratorObjectPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

ECMAScriptFunctionObject* ECMAScriptFunctionObject::create(GlobalObject& global_object, FlyString name, Statement const& ecmascript_code, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, Environment* parent_scope, FunctionKind kind, bool is_strict, bool might_need_arguments_object, bool is_arrow_function)
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
    return global_object.heap().allocate<ECMAScriptFunctionObject>(global_object, move(name), ecmascript_code, move(parameters), m_function_length, parent_scope, *prototype, kind, is_strict, might_need_arguments_object, is_arrow_function);
}

ECMAScriptFunctionObject::ECMAScriptFunctionObject(FlyString name, Statement const& ecmascript_code, Vector<FunctionNode::Parameter> formal_parameters, i32 function_length, Environment* parent_scope, Object& prototype, FunctionKind kind, bool strict, bool might_need_arguments_object, bool is_arrow_function)
    : FunctionObject(prototype)
    , m_environment(parent_scope)
    , m_formal_parameters(move(formal_parameters))
    , m_ecmascript_code(ecmascript_code)
    , m_realm(vm().interpreter_if_exists() ? &vm().interpreter().realm() : nullptr)
    , m_strict(strict)
    , m_name(move(name))
    , m_function_length(function_length)
    , m_kind(kind)
    , m_might_need_arguments_object(might_need_arguments_object)
    , m_is_arrow_function(is_arrow_function)
{
    // NOTE: This logic is from OrdinaryFunctionCreate, https://tc39.es/ecma262/#sec-ordinaryfunctioncreate
    if (m_is_arrow_function)
        m_this_mode = ThisMode::Lexical;
    else if (m_strict)
        m_this_mode = ThisMode::Strict;
    else
        m_this_mode = ThisMode::Global;

    // 15.1.3 Static Semantics: IsSimpleParameterList, https://tc39.es/ecma262/#sec-static-semantics-issimpleparameterlist
    m_has_simple_parameter_list = all_of(m_formal_parameters, [&](auto& parameter) {
        if (parameter.is_rest)
            return false;
        if (parameter.default_value)
            return false;
        if (!parameter.binding.template has<FlyString>())
            return false;
        return true;
    });
}

void ECMAScriptFunctionObject::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Base::initialize(global_object);
    if (!m_is_arrow_function) {
        auto* prototype = vm.heap().allocate<Object>(global_object, *global_object.new_ordinary_function_prototype_object_shape());
        switch (m_kind) {
        case FunctionKind::Regular:
            MUST(prototype->define_property_or_throw(vm.names.constructor, { .value = this, .writable = true, .enumerable = false, .configurable = true }));
            break;
        case FunctionKind::Generator:
            // prototype is "g1.prototype" in figure-2 (https://tc39.es/ecma262/img/figure-2.png)
            set_prototype(global_object.generator_object_prototype());
            break;
        }
        define_direct_property(vm.names.prototype, prototype, Attribute::Writable);
    }
    MUST(define_property_or_throw(vm.names.length, { .value = Value(m_function_length), .writable = false, .enumerable = false, .configurable = true }));
    MUST(define_property_or_throw(vm.names.name, { .value = js_string(vm, m_name.is_null() ? "" : m_name), .writable = false, .enumerable = false, .configurable = true }));
}

ECMAScriptFunctionObject::~ECMAScriptFunctionObject()
{
}

void ECMAScriptFunctionObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_environment);
    visitor.visit(m_realm);
    visitor.visit(m_home_object);

    for (auto& field : m_fields) {
        field.name.visit_edges(visitor);
        visitor.visit(field.initializer);
    }
}

// 9.1.2.4 NewFunctionEnvironment ( F, newTarget ), https://tc39.es/ecma262/#sec-newfunctionenvironment
FunctionEnvironment* ECMAScriptFunctionObject::new_function_environment(Object* new_target)
{
    auto* environment = heap().allocate<FunctionEnvironment>(global_object(), m_environment);
    environment->set_function_object(*this);
    if (this_mode() == ThisMode::Lexical) {
        environment->set_this_binding_status(FunctionEnvironment::ThisBindingStatus::Lexical);
    }

    environment->set_new_target(new_target ? new_target : js_undefined());
    return environment;
}

// 10.2.11 FunctionDeclarationInstantiation ( func, argumentsList ), https://tc39.es/ecma262/#sec-functiondeclarationinstantiation
ThrowCompletionOr<void> ECMAScriptFunctionObject::function_declaration_instantiation(Interpreter* interpreter)
{
    auto& vm = this->vm();

    auto& callee_context = vm.running_execution_context();

    // Needed to extract declarations and functions
    ScopeNode const* scope_body = nullptr;
    if (is<ScopeNode>(*m_ecmascript_code))
        scope_body = static_cast<ScopeNode const*>(m_ecmascript_code.ptr());

    bool has_parameter_expressions = false;

    // FIXME: Maybe compute has duplicates at parse time? (We need to anyway since it's an error in some cases)

    bool has_duplicates = false;
    HashTable<FlyString> parameter_names;
    for (auto& parameter : m_formal_parameters) {
        if (parameter.default_value)
            has_parameter_expressions = true;

        parameter.binding.visit(
            [&](FlyString const& name) {
                if (parameter_names.set(name) != AK::HashSetResult::InsertedNewEntry)
                    has_duplicates = true;
            },
            [&](NonnullRefPtr<BindingPattern> const& pattern) {
                if (pattern->contains_expression())
                    has_parameter_expressions = true;

                pattern->for_each_bound_name([&](auto& name) {
                    if (parameter_names.set(name) != AK::HashSetResult::InsertedNewEntry)
                        has_duplicates = true;
                });
            });
    }

    auto arguments_object_needed = m_might_need_arguments_object;

    if (this_mode() == ThisMode::Lexical)
        arguments_object_needed = false;

    if (parameter_names.contains(vm.names.arguments.as_string()))
        arguments_object_needed = false;

    HashTable<FlyString> function_names;
    Vector<FunctionDeclaration const&> functions_to_initialize;

    if (scope_body) {
        scope_body->for_each_var_function_declaration_in_reverse_order([&](FunctionDeclaration const& function) {
            if (function_names.set(function.name()) == AK::HashSetResult::InsertedNewEntry)
                functions_to_initialize.append(function);
        });

        auto const& arguments_name = vm.names.arguments.as_string();

        if (!has_parameter_expressions && function_names.contains(arguments_name))
            arguments_object_needed = false;

        if (!has_parameter_expressions && arguments_object_needed) {
            scope_body->for_each_lexically_declared_name([&](auto const& name) {
                if (name == arguments_name) {
                    arguments_object_needed = false;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
        }
    } else {
        arguments_object_needed = false;
    }

    Environment* environment;

    if (is_strict_mode() || !has_parameter_expressions) {
        environment = callee_context.lexical_environment;
    } else {
        environment = new_declarative_environment(*callee_context.lexical_environment);
        VERIFY(callee_context.variable_environment == callee_context.lexical_environment);
        callee_context.lexical_environment = environment;
    }

    for (auto const& parameter_name : parameter_names) {
        if (environment->has_binding(parameter_name))
            continue;

        environment->create_mutable_binding(global_object(), parameter_name, false);
        if (has_duplicates)
            environment->initialize_binding(global_object(), parameter_name, js_undefined());
        VERIFY(!vm.exception());
    }

    if (arguments_object_needed) {
        Object* arguments_object;
        if (is_strict_mode() || !has_simple_parameter_list())
            arguments_object = create_unmapped_arguments_object(global_object(), vm.running_execution_context().arguments);
        else
            arguments_object = create_mapped_arguments_object(global_object(), *this, formal_parameters(), vm.running_execution_context().arguments, *environment);

        if (is_strict_mode())
            environment->create_immutable_binding(global_object(), vm.names.arguments.as_string(), false);
        else
            environment->create_mutable_binding(global_object(), vm.names.arguments.as_string(), false);

        environment->initialize_binding(global_object(), vm.names.arguments.as_string(), arguments_object);
        parameter_names.set(vm.names.arguments.as_string());
    }

    // We now treat parameterBindings as parameterNames.

    // The spec makes an iterator here to do IteratorBindingInitialization but we just do it manually
    auto& execution_context_arguments = vm.running_execution_context().arguments;

    for (size_t i = 0; i < m_formal_parameters.size(); ++i) {
        auto& parameter = m_formal_parameters[i];
        parameter.binding.visit(
            [&](auto const& param) {
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
                    if (interpreter)
                        argument_value = parameter.default_value->execute(*interpreter, global_object());
                    if (vm.exception())
                        return;
                } else {
                    argument_value = js_undefined();
                }

                Environment* used_environment = has_duplicates ? nullptr : environment;

                if constexpr (IsSame<FlyString const&, decltype(param)>) {
                    Reference reference = vm.resolve_binding(param, used_environment);
                    if (vm.exception())
                        return;
                    // Here the difference from hasDuplicates is important
                    if (has_duplicates)
                        reference.put_value(global_object(), argument_value);
                    else
                        reference.initialize_referenced_binding(global_object(), argument_value);
                } else if (IsSame<NonnullRefPtr<BindingPattern> const&, decltype(param)>) {
                    // Here the difference from hasDuplicates is important
                    auto result = vm.binding_initialization(param, argument_value, used_environment, global_object());
                    if (result.is_error())
                        return;
                }

                if (vm.exception())
                    return;
            });

        if (auto* exception = vm.exception())
            return throw_completion(exception->value());
    }

    Environment* var_environment;

    HashTable<FlyString> instantiated_var_names;
    if (scope_body)
        instantiated_var_names.ensure_capacity(scope_body->var_declaration_count());

    if (!has_parameter_expressions) {
        if (scope_body) {
            scope_body->for_each_var_declared_name([&](auto const& name) {
                if (!parameter_names.contains(name) && instantiated_var_names.set(name) == AK::HashSetResult::InsertedNewEntry) {
                    environment->create_mutable_binding(global_object(), name, false);
                    environment->initialize_binding(global_object(), name, js_undefined());
                }
            });
        }
        var_environment = environment;
    } else {
        var_environment = new_declarative_environment(*environment);
        callee_context.variable_environment = var_environment;

        if (scope_body) {
            scope_body->for_each_var_declared_name([&](auto const& name) {
                if (instantiated_var_names.set(name) != AK::HashSetResult::InsertedNewEntry)
                    return IterationDecision::Continue;
                var_environment->create_mutable_binding(global_object(), name, false);

                Value initial_value;
                if (!parameter_names.contains(name) || function_names.contains(name))
                    initial_value = js_undefined();
                else
                    initial_value = environment->get_binding_value(global_object(), name, false);

                var_environment->initialize_binding(global_object(), name, initial_value);

                return IterationDecision::Continue;
            });
        }
    }

    // B.3.2.1 Changes to FunctionDeclarationInstantiation, https://tc39.es/ecma262/#sec-web-compat-functiondeclarationinstantiation
    if (!m_strict && scope_body) {
        scope_body->for_each_function_hoistable_with_annexB_extension([&](FunctionDeclaration& function_declaration) {
            auto& function_name = function_declaration.name();
            if (parameter_names.contains(function_name))
                return IterationDecision::Continue;
            // The spec says 'initializedBindings' here but that does not exist and it then adds it to 'instantiatedVarNames' so it probably means 'instantiatedVarNames'.
            if (!instantiated_var_names.contains(function_name) && function_name != vm.names.arguments.as_string()) {
                var_environment->create_mutable_binding(global_object(), function_name, false);
                VERIFY(!vm.exception());
                var_environment->initialize_binding(global_object(), function_name, js_undefined());
                instantiated_var_names.set(function_name);
            }

            function_declaration.set_should_do_additional_annexB_steps();
            return IterationDecision::Continue;
        });
    }

    Environment* lex_environment;

    if (!is_strict_mode())
        lex_environment = new_declarative_environment(*var_environment);
    else
        lex_environment = var_environment;

    callee_context.lexical_environment = lex_environment;

    if (!scope_body)
        return {};

    scope_body->for_each_lexically_scoped_declaration([&](Declaration const& declaration) {
        declaration.for_each_bound_name([&](auto const& name) {
            if (declaration.is_constant_declaration())
                lex_environment->create_immutable_binding(global_object(), name, true);
            else
                lex_environment->create_mutable_binding(global_object(), name, false);
            return IterationDecision::Continue;
        });
    });

    VERIFY(!vm.exception());

    for (auto& declaration : functions_to_initialize) {
        auto* function = ECMAScriptFunctionObject::create(global_object(), declaration.name(), declaration.body(), declaration.parameters(), declaration.function_length(), lex_environment, declaration.kind(), declaration.is_strict_mode(), declaration.might_need_arguments_object());
        var_environment->set_mutable_binding(global_object(), declaration.name(), function, false);
    }

    return {};
}

Value ECMAScriptFunctionObject::execute_function_body()
{
    auto& vm = this->vm();
    auto* bytecode_interpreter = Bytecode::Interpreter::current();

    if (bytecode_interpreter) {
        // FIXME: pass something to evaluate default arguments with
        TRY_OR_DISCARD(function_declaration_instantiation(nullptr));
        if (!m_bytecode_executable.has_value()) {
            m_bytecode_executable = Bytecode::Generator::generate(m_ecmascript_code, m_kind == FunctionKind::Generator);
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
        Interpreter* ast_interpreter = vm.interpreter_if_exists();

        if (!ast_interpreter) {
            local_interpreter = Interpreter::create_with_existing_realm(*realm());
            ast_interpreter = local_interpreter.ptr();
        }

        VM::InterpreterExecutionScope scope(*ast_interpreter);

        TRY_OR_DISCARD(function_declaration_instantiation(ast_interpreter));

        return m_ecmascript_code->execute(*ast_interpreter, global_object());
    }
}

Value ECMAScriptFunctionObject::call()
{
    if (m_is_class_constructor) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::ClassConstructorWithoutNew, m_name);
        return {};
    }
    return execute_function_body();
}

Value ECMAScriptFunctionObject::construct(FunctionObject&)
{
    if (m_is_arrow_function || m_kind == FunctionKind::Generator) {
        vm().throw_exception<TypeError>(global_object(), ErrorType::NotAConstructor, m_name);
        return {};
    }
    return execute_function_body();
}

void ECMAScriptFunctionObject::set_name(const FlyString& name)
{
    VERIFY(!name.is_null());
    auto& vm = this->vm();
    m_name = name;
    auto success = MUST(define_property_or_throw(vm.names.name, { .value = js_string(vm, m_name), .writable = false, .enumerable = false, .configurable = true }));
    VERIFY(success);
}

// 7.3.31 DefineField ( receiver, fieldRecord ), https://tc39.es/ecma262/#sec-definefield
void ECMAScriptFunctionObject::InstanceField::define_field(VM& vm, Object& receiver) const
{
    Value init_value = js_undefined();
    if (initializer) {
        auto init_value_or_error = vm.call(*initializer, receiver.value_of());
        if (init_value_or_error.is_error())
            return;
        init_value = init_value_or_error.release_value();
    }
    (void)receiver.create_data_property_or_throw(name, init_value);
}

}
