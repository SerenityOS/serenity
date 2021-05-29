/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/LexicalEnvironment.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

NonnullOwnPtr<Interpreter> Interpreter::create_with_existing_global_object(GlobalObject& global_object)
{
    DeferGC defer_gc(global_object.heap());
    auto interpreter = adopt_own(*new Interpreter(global_object.vm()));
    interpreter->m_global_object = make_handle(static_cast<Object*>(&global_object));
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
    auto& vm = this->vm();
    VERIFY(!vm.exception());

    VM::InterpreterExecutionScope scope(*this);

    vm.set_last_value({}, {});

    CallFrame global_call_frame;
    global_call_frame.current_node = &program;
    global_call_frame.this_value = &global_object;
    static FlyString global_execution_context_name = "(global execution context)";
    global_call_frame.function_name = global_execution_context_name;
    global_call_frame.scope = &global_object;
    VERIFY(!vm.exception());
    global_call_frame.is_strict_mode = program.is_strict_mode();
    vm.push_call_frame(global_call_frame, global_object);
    VERIFY(!vm.exception());
    program.execute(*this, global_object);

    // Whatever the promise jobs or on_call_stack_emptied do should not affect the effective
    // 'last value'.
    auto last_value = vm.last_value();

    vm.pop_call_frame();

    // At this point we may have already run any queued promise jobs via on_call_stack_emptied,
    // in which case this is a no-op.
    vm.run_queued_promise_jobs();

    vm.set_last_value({}, last_value.value_or(js_undefined()));
}

GlobalObject& Interpreter::global_object()
{
    return static_cast<GlobalObject&>(*m_global_object.cell());
}

const GlobalObject& Interpreter::global_object() const
{
    return static_cast<const GlobalObject&>(*m_global_object.cell());
}

void Interpreter::enter_scope(const ScopeNode& scope_node, ScopeType scope_type, GlobalObject& global_object)
{
    ScopeGuard guard([&] {
        for (auto& declaration : scope_node.functions()) {
            auto* function = ScriptFunction::create(global_object, declaration.name(), declaration.body(), declaration.parameters(), declaration.function_length(), current_scope(), declaration.is_strict_mode());
            vm().set_variable(declaration.name(), function, global_object);
        }
    });

    if (scope_type == ScopeType::Function) {
        push_scope({ scope_type, scope_node, false });
        for (auto& declaration : scope_node.functions())
            current_scope()->put_to_scope(declaration.name(), { js_undefined(), DeclarationKind::Var });
        return;
    }

    HashMap<FlyString, Variable> scope_variables_with_declaration_kind;
    scope_variables_with_declaration_kind.ensure_capacity(16);

    for (auto& declaration : scope_node.variables()) {
        for (auto& declarator : declaration.declarations()) {
            if (is<Program>(scope_node)) {
                declarator.target().visit(
                    [&](const NonnullRefPtr<Identifier>& id) {
                        global_object.put(id->string(), js_undefined());
                    },
                    [&](const NonnullRefPtr<BindingPattern>& binding) {
                        binding->for_each_assigned_name([&](const auto& name) {
                            global_object.put(name, js_undefined());
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
                        binding->for_each_assigned_name([&](const auto& name) {
                            scope_variables_with_declaration_kind.set(name, { js_undefined(), declaration.declaration_kind() });
                        });
                    });
            }
        }
    }

    bool pushed_lexical_environment = false;

    if (!scope_variables_with_declaration_kind.is_empty()) {
        auto* block_lexical_environment = heap().allocate<LexicalEnvironment>(global_object, move(scope_variables_with_declaration_kind), current_scope());
        vm().call_frame().scope = block_lexical_environment;
        pushed_lexical_environment = true;
    }

    push_scope({ scope_type, scope_node, pushed_lexical_environment });
}

void Interpreter::exit_scope(const ScopeNode& scope_node)
{
    while (!m_scope_stack.is_empty()) {
        auto popped_scope = m_scope_stack.take_last();
        if (popped_scope.pushed_environment)
            vm().call_frame().scope = vm().call_frame().scope->parent();
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
    enter_scope(block, scope_type, global_object);

    for (auto& node : block.children()) {
        auto value = node.execute(*this, global_object);
        if (!value.is_empty())
            vm().set_last_value({}, value);
        if (vm().should_unwind()) {
            if (!block.label().is_null() && vm().should_unwind_until(ScopeType::Breakable, block.label()))
                vm().stop_unwind();
            break;
        }
    }

    if (scope_type == ScopeType::Function) {
        bool did_return = vm().unwind_until() == ScopeType::Function;
        if (!did_return)
            vm().set_last_value({}, js_undefined());
    }

    if (vm().unwind_until() == scope_type)
        vm().stop_unwind();

    exit_scope(block);

    return vm().last_value();
}

LexicalEnvironment* Interpreter::current_environment()
{
    VERIFY(is<LexicalEnvironment>(vm().call_frame().scope));
    return static_cast<LexicalEnvironment*>(vm().call_frame().scope);
}

}
