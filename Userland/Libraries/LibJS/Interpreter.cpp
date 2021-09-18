/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

NonnullOwnPtr<Interpreter> Interpreter::create_with_existing_realm(Realm& realm)
{
    auto& global_object = realm.global_object();
    DeferGC defer_gc(global_object.heap());
    auto interpreter = adopt_own(*new Interpreter(global_object.vm()));
    interpreter->m_global_object = make_handle(&global_object);
    interpreter->m_realm = make_handle(&realm);
    return interpreter;
}

Interpreter::Interpreter(VM& vm)
    : m_vm(vm)
{
}

Interpreter::~Interpreter()
{
}

void Interpreter::run(GlobalObject& global_object, const Program& program)
{
    // FIXME: Why does this receive a GlobalObject? Interpreter has one already, and this might not be in sync with the Realm's GlobalObject.

    auto& vm = this->vm();
    VERIFY(!vm.exception());

    VM::InterpreterExecutionScope scope(*this);

    vm.set_last_value(Badge<Interpreter> {}, {});

    ExecutionContext execution_context(heap());
    execution_context.current_node = &program;
    execution_context.this_value = &global_object;
    static FlyString global_execution_context_name = "(global execution context)";
    execution_context.function_name = global_execution_context_name;
    execution_context.lexical_environment = &realm().global_environment();
    execution_context.variable_environment = &realm().global_environment();
    execution_context.realm = &realm();
    execution_context.is_strict_mode = program.is_strict_mode();
    vm.push_execution_context(execution_context, global_object);
    VERIFY(!vm.exception());
    auto value = program.execute(*this, global_object);
    vm.set_last_value(Badge<Interpreter> {}, value.value_or(js_undefined()));

    vm.pop_execution_context();

    // At this point we may have already run any queued promise jobs via on_call_stack_emptied,
    // in which case this is a no-op.
    vm.run_queued_promise_jobs();

    vm.run_queued_finalization_registry_cleanup_jobs();

    vm.finish_execution_generation();
}

GlobalObject& Interpreter::global_object()
{
    return static_cast<GlobalObject&>(*m_global_object.cell());
}

const GlobalObject& Interpreter::global_object() const
{
    return static_cast<const GlobalObject&>(*m_global_object.cell());
}

Realm& Interpreter::realm()
{
    return static_cast<Realm&>(*m_realm.cell());
}

const Realm& Interpreter::realm() const
{
    return static_cast<const Realm&>(*m_realm.cell());
}

void Interpreter::enter_scope(const ScopeNode& scope_node, ScopeType scope_type, GlobalObject& global_object)
{
    ScopeGuard guard([&] {
        for (auto& declaration : scope_node.hoisted_functions()) {
            lexical_environment()->put_into_environment(declaration.name(), { js_undefined(), DeclarationKind::Var });
        }
        for (auto& declaration : scope_node.functions()) {
            auto* function = ECMAScriptFunctionObject::create(global_object, declaration.name(), declaration.body(), declaration.parameters(), declaration.function_length(), lexical_environment(), declaration.kind(), declaration.is_strict_mode());
            vm().set_variable(declaration.name(), function, global_object);
        }
    });

    if (scope_type == ScopeType::Function) {
        push_scope({ scope_type, scope_node, false });
        for (auto& declaration : scope_node.functions())
            lexical_environment()->put_into_environment(declaration.name(), { js_undefined(), DeclarationKind::Var });
        return;
    }

    HashMap<FlyString, Variable> scope_variables_with_declaration_kind;

    if (!scope_node.variables().is_empty())
        scope_variables_with_declaration_kind.ensure_capacity(16);

    bool is_program_node = is<Program>(scope_node);

    for (auto& declaration : scope_node.variables()) {
        for (auto& declarator : declaration.declarations()) {
            if (is_program_node && declaration.declaration_kind() == DeclarationKind::Var) {
                declarator.target().visit(
                    [&](const NonnullRefPtr<Identifier>& id) {
                        global_object.define_direct_property(id->string(), js_undefined(), JS::Attribute::Writable | JS::Attribute::Enumerable);
                    },
                    [&](const NonnullRefPtr<BindingPattern>& binding) {
                        binding->for_each_bound_name([&](const auto& name) {
                            global_object.define_direct_property(name, js_undefined(), JS::Attribute::Writable | JS::Attribute::Enumerable);
                        });
                    });
                if (exception())
                    return;
            } else {
                declarator.target().visit(
                    [&](const NonnullRefPtr<Identifier>& id) {
                        scope_variables_with_declaration_kind.set(id->string(), { js_undefined(), declaration.declaration_kind() });
                    },
                    [&](const NonnullRefPtr<BindingPattern>& binding) {
                        binding->for_each_bound_name([&](const auto& name) {
                            scope_variables_with_declaration_kind.set(name, { js_undefined(), declaration.declaration_kind() });
                        });
                    });
            }
        }
    }

    bool pushed_environment = false;

    if (!scope_variables_with_declaration_kind.is_empty()) {
        auto* environment = heap().allocate<DeclarativeEnvironment>(global_object, move(scope_variables_with_declaration_kind), lexical_environment());
        vm().running_execution_context().lexical_environment = environment;
        vm().running_execution_context().variable_environment = environment;
        pushed_environment = true;
    }

    push_scope({ scope_type, scope_node, pushed_environment });
}

void Interpreter::exit_scope(const ScopeNode& scope_node)
{
    while (!m_scope_stack.is_empty()) {
        auto popped_scope = m_scope_stack.take_last();
        if (popped_scope.pushed_environment) {
            vm().running_execution_context().lexical_environment = vm().running_execution_context().lexical_environment->outer_environment();
            vm().running_execution_context().variable_environment = vm().running_execution_context().variable_environment->outer_environment();
        }
        if (popped_scope.scope_node.ptr() == &scope_node)
            break;
    }

    // If we unwind all the way, just reset m_unwind_until so that future "return" doesn't break.
    if (m_scope_stack.is_empty())
        vm().stop_unwind();
}

void Interpreter::push_scope(ScopeFrame frame)
{
    m_scope_stack.append(move(frame));
}

Value Interpreter::execute_statement(GlobalObject& global_object, const Statement& statement, ScopeType scope_type)
{
    if (!is<ScopeNode>(statement))
        return statement.execute(*this, global_object);

    auto& block = static_cast<const ScopeNode&>(statement);
    Vector<FlyString> const& labels = [&] {
        if (is<BlockStatement>(block)) {
            return static_cast<BlockStatement const&>(block).labels();
        } else {
            return Vector<FlyString>();
        }
    }();

    enter_scope(block, scope_type, global_object);

    Value last_value;
    for (auto& node : block.children()) {
        auto value = node.execute(*this, global_object);
        if (!value.is_empty())
            last_value = value;
        if (vm().should_unwind()) {
            if (!labels.is_empty() && vm().should_unwind_until(ScopeType::Breakable, labels))
                vm().stop_unwind();
            break;
        }
    }

    if (scope_type == ScopeType::Function) {
        bool did_return = vm().unwind_until() == ScopeType::Function;
        if (!did_return)
            last_value = js_undefined();
    }

    if (vm().unwind_until() == scope_type)
        vm().stop_unwind();

    exit_scope(block);

    return last_value;
}

FunctionEnvironment* Interpreter::current_function_environment()
{
    return verify_cast<FunctionEnvironment>(vm().running_execution_context().lexical_environment);
}

}
