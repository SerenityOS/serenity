/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Badge.h>
#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/LexicalEnvironment.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/SymbolObject.h>
#include <LibJS/Runtime/Value.h>

//#define INTERPRETER_DEBUG

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

Value Interpreter::run(GlobalObject& global_object, const Program& program)
{
    ASSERT(!vm().exception());

    VM::InterpreterExecutionScope scope(*this);

    CallFrame global_call_frame;
    global_call_frame.this_value = &global_object;
    global_call_frame.function_name = "(global execution context)";
    global_call_frame.environment = heap().allocate<LexicalEnvironment>(global_object, LexicalEnvironment::EnvironmentRecordType::Global);
    global_call_frame.environment->bind_this_value(global_object, &global_object);
    global_call_frame.is_strict_mode = program.is_strict_mode();
    if (vm().exception())
        return {};
    vm().call_stack().append(move(global_call_frame));

    auto result = program.execute(*this, global_object);
    vm().pop_call_frame();
    return result;
}

GlobalObject& Interpreter::global_object()
{
    return static_cast<GlobalObject&>(*m_global_object.cell());
}

const GlobalObject& Interpreter::global_object() const
{
    return static_cast<const GlobalObject&>(*m_global_object.cell());
}

void Interpreter::enter_scope(const ScopeNode& scope_node, ArgumentVector arguments, ScopeType scope_type, GlobalObject& global_object)
{
    for (auto& declaration : scope_node.functions()) {
        auto* function = ScriptFunction::create(global_object, declaration.name(), declaration.body(), declaration.parameters(), declaration.function_length(), current_environment(), declaration.is_strict_mode());
        vm().set_variable(declaration.name(), function, global_object);
    }

    if (scope_type == ScopeType::Function) {
        push_scope({ scope_type, scope_node, false });
        return;
    }

    HashMap<FlyString, Variable> scope_variables_with_declaration_kind;
    scope_variables_with_declaration_kind.ensure_capacity(16);

    for (auto& declaration : scope_node.variables()) {
        for (auto& declarator : declaration.declarations()) {
            if (scope_node.is_program()) {
                global_object.put(declarator.id().string(), js_undefined());
                if (exception())
                    return;
            } else {
                scope_variables_with_declaration_kind.set(declarator.id().string(), { js_undefined(), declaration.declaration_kind() });
            }
        }
    }

    for (auto& argument : arguments) {
        scope_variables_with_declaration_kind.set(argument.name, { argument.value, DeclarationKind::Var });
    }

    bool pushed_lexical_environment = false;

    if (!scope_variables_with_declaration_kind.is_empty()) {
        auto* block_lexical_environment = heap().allocate<LexicalEnvironment>(global_object, move(scope_variables_with_declaration_kind), current_environment());
        vm().call_stack().last().environment = block_lexical_environment;
        pushed_lexical_environment = true;
    }

    push_scope({ scope_type, scope_node, pushed_lexical_environment });
}

void Interpreter::exit_scope(const ScopeNode& scope_node)
{
    while (!m_scope_stack.is_empty()) {
        auto popped_scope = m_scope_stack.take_last();
        if (popped_scope.pushed_environment)
            vm().call_frame().environment = vm().call_frame().environment->parent();
        if (popped_scope.scope_node.ptr() == &scope_node)
            break;
    }

    // If we unwind all the way, just reset m_unwind_until so that future "return" doesn't break.
    if (m_scope_stack.is_empty())
        vm().unwind(ScopeType::None);
}

void Interpreter::push_scope(ScopeFrame frame)
{
    m_scope_stack.append(move(frame));
}

Value Interpreter::execute_statement(GlobalObject& global_object, const Statement& statement, ArgumentVector arguments, ScopeType scope_type)
{
    if (!statement.is_scope_node())
        return statement.execute(*this, global_object);

    auto& block = static_cast<const ScopeNode&>(statement);
    enter_scope(block, move(arguments), scope_type, global_object);

    if (block.children().is_empty())
        vm().set_last_value({}, js_undefined());

    for (auto& node : block.children()) {
        vm().set_last_value({}, node.execute(*this, global_object));
        if (vm().should_unwind()) {
            if (!block.label().is_null() && vm().should_unwind_until(ScopeType::Breakable, block.label()))
                vm().stop_unwind();
            break;
        }
    }

    bool did_return = vm().unwind_until() == ScopeType::Function;

    if (vm().unwind_until() == scope_type)
        vm().unwind(ScopeType::None);

    exit_scope(block);

    return did_return ? vm().last_value() : js_undefined();
}

}
