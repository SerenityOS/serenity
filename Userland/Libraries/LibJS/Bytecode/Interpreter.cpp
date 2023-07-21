/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/TemporaryChange.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Realm.h>

namespace JS::Bytecode {

static bool s_bytecode_interpreter_enabled = false;

bool Interpreter::enabled()
{
    return s_bytecode_interpreter_enabled;
}

void Interpreter::set_enabled(bool enabled)
{
    s_bytecode_interpreter_enabled = enabled;
}

bool g_dump_bytecode = false;

Interpreter::Interpreter(VM& vm)
    : m_vm(vm)
{
}

Interpreter::~Interpreter()
{
}

void Interpreter::visit_edges(Cell::Visitor& visitor)
{
    if (m_return_value.has_value())
        visitor.visit(*m_return_value);
    if (m_saved_return_value.has_value())
        visitor.visit(*m_saved_return_value);
    if (m_saved_exception.has_value())
        visitor.visit(*m_saved_exception);
    for (auto& frame : m_call_frames) {
        frame.visit([&](auto& value) { value->visit_edges(visitor); });
    }
}

// 16.1.6 ScriptEvaluation ( scriptRecord ), https://tc39.es/ecma262/#sec-runtime-semantics-scriptevaluation
ThrowCompletionOr<Value> Interpreter::run(Script& script_record, JS::GCPtr<Environment> lexical_environment_override)
{
    auto& vm = this->vm();

    // 1. Let globalEnv be scriptRecord.[[Realm]].[[GlobalEnv]].
    auto& global_environment = script_record.realm().global_environment();

    // 2. Let scriptContext be a new ECMAScript code execution context.
    ExecutionContext script_context(vm.heap());

    // 3. Set the Function of scriptContext to null.
    // NOTE: This was done during execution context construction.

    // 4. Set the Realm of scriptContext to scriptRecord.[[Realm]].
    script_context.realm = &script_record.realm();

    // 5. Set the ScriptOrModule of scriptContext to scriptRecord.
    script_context.script_or_module = NonnullGCPtr<Script>(script_record);

    // 6. Set the VariableEnvironment of scriptContext to globalEnv.
    script_context.variable_environment = &global_environment;

    // 7. Set the LexicalEnvironment of scriptContext to globalEnv.
    script_context.lexical_environment = &global_environment;

    // Non-standard: Override the lexical environment if requested.
    if (lexical_environment_override)
        script_context.lexical_environment = lexical_environment_override;

    // 8. Set the PrivateEnvironment of scriptContext to null.

    // NOTE: This isn't in the spec, but we require it.
    script_context.is_strict_mode = script_record.parse_node().is_strict_mode();

    // FIXME: 9. Suspend the currently running execution context.

    // 10. Push scriptContext onto the execution context stack; scriptContext is now the running execution context.
    TRY(vm.push_execution_context(script_context, {}));

    // 11. Let script be scriptRecord.[[ECMAScriptCode]].
    auto& script = script_record.parse_node();

    // 12. Let result be Completion(GlobalDeclarationInstantiation(script, globalEnv)).
    auto instantiation_result = script.global_declaration_instantiation(vm, global_environment);
    Completion result = instantiation_result.is_throw_completion() ? instantiation_result.throw_completion() : normal_completion({});

    // 13. If result.[[Type]] is normal, then
    if (result.type() == Completion::Type::Normal) {
        auto executable_result = JS::Bytecode::Generator::generate(script);

        if (executable_result.is_error()) {
            if (auto error_string = executable_result.error().to_string(); error_string.is_error())
                result = vm.template throw_completion<JS::InternalError>(vm.error_message(JS::VM::ErrorMessage::OutOfMemory));
            else if (error_string = String::formatted("TODO({})", error_string.value()); error_string.is_error())
                result = vm.template throw_completion<JS::InternalError>(vm.error_message(JS::VM::ErrorMessage::OutOfMemory));
            else
                result = JS::throw_completion(JS::InternalError::create(realm(), error_string.release_value()));
        } else {
            auto executable = executable_result.release_value();

            if (g_dump_bytecode)
                executable->dump();

            // a. Set result to the result of evaluating script.
            auto result_or_error = run_and_return_frame(script_record.realm(), *executable, nullptr);
            if (result_or_error.value.is_error())
                result = result_or_error.value.release_error();
            else
                result = result_or_error.frame->registers[0];
        }
    }

    // 14. If result.[[Type]] is normal and result.[[Value]] is empty, then
    if (result.type() == Completion::Type::Normal && !result.value().has_value()) {
        // a. Set result to NormalCompletion(undefined).
        result = normal_completion(js_undefined());
    }

    // FIXME: 15. Suspend scriptContext and remove it from the execution context stack.
    vm.pop_execution_context();

    // 16. Assert: The execution context stack is not empty.
    VERIFY(!vm.execution_context_stack().is_empty());

    // FIXME: 17. Resume the context that is now on the top of the execution context stack as the running execution context.

    // At this point we may have already run any queued promise jobs via on_call_stack_emptied,
    // in which case this is a no-op.
    // FIXME: These three should be moved out of Interpreter::run and give the host an option to run these, as it's up to the host when these get run.
    //        https://tc39.es/ecma262/#sec-jobs for jobs and https://tc39.es/ecma262/#_ref_3508 for ClearKeptObjects
    //        finish_execution_generation is particularly an issue for LibWeb, as the HTML spec wants to run it specifically after performing a microtask checkpoint.
    //        The promise and registry cleanup queues don't cause LibWeb an issue, as LibWeb overrides the hooks that push onto these queues.
    vm.run_queued_promise_jobs();

    vm.run_queued_finalization_registry_cleanup_jobs();

    vm.finish_execution_generation();

    // 18. Return ? result.
    if (result.is_abrupt()) {
        VERIFY(result.type() == Completion::Type::Throw);
        return result.release_error();
    }

    VERIFY(result.value().has_value());
    return *result.value();
}

ThrowCompletionOr<Value> Interpreter::run(SourceTextModule& module)
{
    // FIXME: This is not a entry point as defined in the spec, but is convenient.
    //        To avoid work we use link_and_eval_module however that can already be
    //        dangerous if the vm loaded other modules.
    auto& vm = this->vm();

    TRY(vm.link_and_eval_module(Badge<Bytecode::Interpreter> {}, module));

    vm.run_queued_promise_jobs();

    vm.run_queued_finalization_registry_cleanup_jobs();

    return js_undefined();
}

Interpreter::ValueAndFrame Interpreter::run_and_return_frame(Realm& realm, Executable& executable, BasicBlock const* entry_point, CallFrame* in_frame)
{
    dbgln_if(JS_BYTECODE_DEBUG, "Bytecode::Interpreter will run unit {:p}", &executable);

    TemporaryChange restore_executable { m_current_executable, &executable };
    TemporaryChange restore_saved_jump { m_scheduled_jump, static_cast<BasicBlock const*>(nullptr) };
    TemporaryChange restore_saved_exception { m_saved_exception, {} };

    bool pushed_execution_context = false;
    ExecutionContext execution_context(vm().heap());
    if (vm().execution_context_stack().is_empty() || !vm().running_execution_context().lexical_environment) {
        // The "normal" interpreter pushes an execution context without environment so in that case we also want to push one.
        execution_context.this_value = &realm.global_object();
        static DeprecatedFlyString global_execution_context_name = "(*BC* global execution context)";
        execution_context.function_name = global_execution_context_name;
        execution_context.lexical_environment = &realm.global_environment();
        execution_context.variable_environment = &realm.global_environment();
        execution_context.realm = realm;
        execution_context.is_strict_mode = executable.is_strict_mode;
        vm().push_execution_context(execution_context);
        pushed_execution_context = true;
    }

    TemporaryChange restore_current_block { m_current_block, entry_point ?: executable.basic_blocks.first() };

    if (in_frame)
        push_call_frame(in_frame, executable.number_of_registers);
    else
        push_call_frame(make<CallFrame>(), executable.number_of_registers);

    for (;;) {
        Bytecode::InstructionStreamIterator pc(m_current_block->instruction_stream());
        TemporaryChange temp_change { m_pc, &pc };

        // FIXME: This is getting kinda spaghetti-y
        bool will_jump = false;
        bool will_return = false;
        bool will_yield = false;
        while (!pc.at_end()) {
            auto& instruction = *pc;
            auto ran_or_error = instruction.execute(*this);
            if (ran_or_error.is_error()) {
                auto exception_value = *ran_or_error.throw_completion().value();
                m_saved_exception = exception_value;
                if (unwind_contexts().is_empty())
                    break;
                auto& unwind_context = unwind_contexts().last();
                if (unwind_context.executable != m_current_executable)
                    break;
                if (unwind_context.handler && !unwind_context.handler_called) {
                    vm().running_execution_context().lexical_environment = unwind_context.lexical_environment;
                    m_current_block = unwind_context.handler;
                    unwind_context.handler_called = true;

                    accumulator() = exception_value;
                    m_saved_exception = {};
                    will_jump = true;
                    break;
                }
                if (unwind_context.finalizer) {
                    m_current_block = unwind_context.finalizer;
                    // If an exception was thrown inside the corresponding `catch` block, we need to rethrow it
                    // from the `finally` block. But if the exception is from the `try` block, and has already been
                    // handled by `catch`, we swallow it.
                    if (!unwind_context.handler_called)
                        m_saved_exception = {};
                    will_jump = true;
                    break;
                }
                // An unwind context with no handler or finalizer? We have nowhere to jump, and continuing on will make us crash on the next `Call` to a non-native function if there's an exception! So let's crash here instead.
                // If you run into this, you probably forgot to remove the current unwind_context somewhere.
                VERIFY_NOT_REACHED();
            }
            if (m_pending_jump.has_value()) {
                m_current_block = m_pending_jump.release_value();
                will_jump = true;
                break;
            }
            if (m_return_value.has_value()) {
                will_return = true;
                // Note: A `yield` statement will not go through a finally statement,
                //       hence we need to set a flag to not do so,
                //       but we generate a Yield Operation in the case of returns in
                //       generators as well, so we need to check if it will actually
                //       continue or is a `return` in disguise
                will_yield = (instruction.type() == Instruction::Type::Yield && static_cast<Op::Yield const&>(instruction).continuation().has_value()) || instruction.type() == Instruction::Type::Await;
                break;
            }
            ++pc;
        }

        if (will_jump)
            continue;

        if (!unwind_contexts().is_empty() && !will_yield) {
            auto& unwind_context = unwind_contexts().last();
            if (unwind_context.executable == m_current_executable && unwind_context.finalizer) {
                m_saved_return_value = m_return_value;
                m_return_value = {};
                m_current_block = unwind_context.finalizer;
                // the unwind_context will be pop'ed when entering the finally block
                continue;
            }
        }

        if (pc.at_end())
            break;

        if (m_saved_exception.has_value())
            break;

        if (will_return)
            break;
    }

    dbgln_if(JS_BYTECODE_DEBUG, "Bytecode::Interpreter did run unit {:p}", &executable);

    if constexpr (JS_BYTECODE_DEBUG) {
        for (size_t i = 0; i < registers().size(); ++i) {
            String value_string;
            if (registers()[i].is_empty())
                value_string = MUST("(empty)"_string);
            else
                value_string = MUST(registers()[i].to_string_without_side_effects());
            dbgln("[{:3}] {}", i, value_string);
        }
    }

    auto frame = pop_call_frame();

    Value return_value = js_undefined();
    if (m_return_value.has_value()) {
        return_value = m_return_value.release_value();
    } else if (m_saved_return_value.has_value() && !m_saved_exception.has_value()) {
        return_value = m_saved_return_value.release_value();
    }

    // NOTE: The return value from a called function is put into $0 in the caller context.
    if (!m_call_frames.is_empty())
        call_frame().registers[0] = return_value;

    // At this point we may have already run any queued promise jobs via on_call_stack_emptied,
    // in which case this is a no-op.
    vm().run_queued_promise_jobs();

    if (pushed_execution_context) {
        VERIFY(&vm().running_execution_context() == &execution_context);
        vm().pop_execution_context();
    }

    vm().finish_execution_generation();

    if (m_saved_exception.has_value()) {
        Value thrown_value = m_saved_exception.value();
        m_saved_exception = {};
        m_saved_return_value = {};
        if (auto* call_frame = frame.get_pointer<NonnullOwnPtr<CallFrame>>())
            return { throw_completion(thrown_value), move(*call_frame) };
        return { throw_completion(thrown_value), nullptr };
    }

    if (auto* call_frame = frame.get_pointer<NonnullOwnPtr<CallFrame>>())
        return { return_value, move(*call_frame) };
    return { return_value, nullptr };
}

void Interpreter::enter_unwind_context(Optional<Label> handler_target, Optional<Label> finalizer_target)
{
    unwind_contexts().empend(
        m_current_executable,
        handler_target.has_value() ? &handler_target->block() : nullptr,
        finalizer_target.has_value() ? &finalizer_target->block() : nullptr,
        vm().running_execution_context().lexical_environment);
}

void Interpreter::leave_unwind_context()
{
    unwind_contexts().take_last();
}

ThrowCompletionOr<void> Interpreter::continue_pending_unwind(Label const& resume_label)
{
    if (m_saved_exception.has_value()) {
        return throw_completion(m_saved_exception.release_value());
    }

    if (m_saved_return_value.has_value()) {
        do_return(m_saved_return_value.release_value());
        return {};
    }

    if (m_scheduled_jump) {
        // FIXME: If we `break` or `continue` in the finally, we need to clear
        //        this field
        jump(Label { *m_scheduled_jump });
        m_scheduled_jump = nullptr;
    } else {
        jump(resume_label);
    }
    return {};
}

VM::InterpreterExecutionScope Interpreter::ast_interpreter_scope(Realm& realm)
{
    if (!m_ast_interpreter)
        m_ast_interpreter = JS::Interpreter::create_with_existing_realm(realm);

    return { *m_ast_interpreter };
}

size_t Interpreter::pc() const
{
    return m_pc ? m_pc->offset() : 0;
}

DeprecatedString Interpreter::debug_position() const
{
    return DeprecatedString::formatted("{}:{:2}:{:4x}", m_current_executable->name, m_current_block->name(), pc());
}

ThrowCompletionOr<NonnullOwnPtr<Bytecode::Executable>> compile(VM& vm, ASTNode const& node, FunctionKind kind, DeprecatedFlyString const& name)
{
    auto executable_result = Bytecode::Generator::generate(node, kind);
    if (executable_result.is_error())
        return vm.throw_completion<InternalError>(ErrorType::NotImplemented, TRY_OR_THROW_OOM(vm, executable_result.error().to_string()));

    auto bytecode_executable = executable_result.release_value();
    bytecode_executable->name = name;

    if (Bytecode::g_dump_bytecode)
        bytecode_executable->dump();

    return bytecode_executable;
}

Realm& Interpreter::realm()
{
    return *m_vm.current_realm();
}

void Interpreter::push_call_frame(Variant<NonnullOwnPtr<CallFrame>, CallFrame*> frame, size_t register_count)
{
    m_call_frames.append(move(frame));
    this->call_frame().registers.resize(register_count);
    m_current_call_frame = this->call_frame().registers;
}

Variant<NonnullOwnPtr<CallFrame>, CallFrame*> Interpreter::pop_call_frame()
{
    auto frame = m_call_frames.take_last();
    m_current_call_frame = m_call_frames.is_empty() ? Span<Value> {} : this->call_frame().registers;
    return frame;
}

}
