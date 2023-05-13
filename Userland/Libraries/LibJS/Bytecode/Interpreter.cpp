/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/TemporaryChange.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Realm.h>

namespace JS::Bytecode {

static Interpreter* s_current;
bool g_dump_bytecode = false;

Interpreter* Interpreter::current()
{
    return s_current;
}

Interpreter::Interpreter(Realm& realm)
    : m_vm(realm.vm())
    , m_realm(realm)
{
    VERIFY(!s_current);
    s_current = this;
}

Interpreter::~Interpreter()
{
    VERIFY(s_current == this);
    s_current = nullptr;
}

Interpreter::ValueAndFrame Interpreter::run_and_return_frame(Executable const& executable, BasicBlock const* entry_point, RegisterWindow* in_frame)
{
    dbgln_if(JS_BYTECODE_DEBUG, "Bytecode::Interpreter will run unit {:p}", &executable);

    TemporaryChange restore_executable { m_current_executable, &executable };
    TemporaryChange restore_saved_jump { m_scheduled_jump, static_cast<BasicBlock const*>(nullptr) };
    TemporaryChange restore_saved_exception { m_saved_exception, {} };

    bool pushed_execution_context = false;
    ExecutionContext execution_context(vm().heap());
    if (vm().execution_context_stack().is_empty() || !vm().running_execution_context().lexical_environment) {
        // The "normal" interpreter pushes an execution context without environment so in that case we also want to push one.
        execution_context.this_value = &m_realm->global_object();
        static DeprecatedFlyString global_execution_context_name = "(*BC* global execution context)";
        execution_context.function_name = global_execution_context_name;
        execution_context.lexical_environment = &m_realm->global_environment();
        execution_context.variable_environment = &m_realm->global_environment();
        execution_context.realm = m_realm;
        execution_context.is_strict_mode = executable.is_strict_mode;
        vm().push_execution_context(execution_context);
        pushed_execution_context = true;
    }

    TemporaryChange restore_current_block { m_current_block, entry_point ?: executable.basic_blocks.first() };

    if (in_frame)
        m_register_windows.append(in_frame);
    else
        m_register_windows.append(make<RegisterWindow>(MarkedVector<Value>(vm().heap()), MarkedVector<GCPtr<Environment>>(vm().heap()), MarkedVector<GCPtr<Environment>>(vm().heap()), Vector<UnwindInfo> {}));

    registers().resize(executable.number_of_registers);

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
                m_saved_exception = make_handle(exception_value);
                if (unwind_contexts().is_empty())
                    break;
                auto& unwind_context = unwind_contexts().last();
                if (unwind_context.executable != m_current_executable)
                    break;
                if (unwind_context.handler) {
                    vm().running_execution_context().lexical_environment = unwind_context.lexical_environment;
                    vm().running_execution_context().variable_environment = unwind_context.variable_environment;
                    m_current_block = unwind_context.handler;
                    unwind_context.handler = nullptr;

                    accumulator() = exception_value;
                    m_saved_exception = {};
                    will_jump = true;
                    break;
                }
                if (unwind_context.finalizer) {
                    m_current_block = unwind_context.finalizer;
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
            if (!m_return_value.is_empty()) {
                will_return = true;
                // Note: A `yield` statement will not go through a finally statement,
                //       hence we need to set a flag to not do so,
                //       but we generate a Yield Operation in the case of returns in
                //       generators as well, so we need to check if it will actually
                //       continue or is a `return` in disguise
                will_yield = instruction.type() == Instruction::Type::Yield && static_cast<Op::Yield const&>(instruction).continuation().has_value();
                break;
            }
            ++pc;
        }

        if (will_jump)
            continue;

        if (!unwind_contexts().is_empty() && !will_yield) {
            auto& unwind_context = unwind_contexts().last();
            if (unwind_context.executable == m_current_executable && unwind_context.finalizer) {
                m_saved_return_value = make_handle(m_return_value);
                m_return_value = {};
                m_current_block = unwind_context.finalizer;
                // the unwind_context will be pop'ed when entering the finally block
                continue;
            }
        }

        if (pc.at_end())
            break;

        if (!m_saved_exception.is_null())
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

    auto frame = m_register_windows.take_last();

    Value return_value = js_undefined();
    if (!m_return_value.is_empty()) {
        return_value = m_return_value;
        m_return_value = {};
    } else if (!m_saved_return_value.is_null() && m_saved_exception.is_null()) {
        return_value = m_saved_return_value.value();
        m_saved_return_value = {};
    }

    // NOTE: The return value from a called function is put into $0 in the caller context.
    if (!m_register_windows.is_empty())
        window().registers[0] = return_value;

    // At this point we may have already run any queued promise jobs via on_call_stack_emptied,
    // in which case this is a no-op.
    vm().run_queued_promise_jobs();

    if (pushed_execution_context) {
        VERIFY(&vm().running_execution_context() == &execution_context);
        vm().pop_execution_context();
    }

    vm().finish_execution_generation();

    if (!m_saved_exception.is_null()) {
        Value thrown_value = m_saved_exception.value();
        m_saved_exception = {};
        m_saved_return_value = {};
        if (auto* register_window = frame.get_pointer<NonnullOwnPtr<RegisterWindow>>())
            return { throw_completion(thrown_value), move(*register_window) };
        return { throw_completion(thrown_value), nullptr };
    }

    if (auto* register_window = frame.get_pointer<NonnullOwnPtr<RegisterWindow>>())
        return { return_value, move(*register_window) };
    return { return_value, nullptr };
}

void Interpreter::enter_unwind_context(Optional<Label> handler_target, Optional<Label> finalizer_target)
{
    unwind_contexts().empend(
        m_current_executable,
        handler_target.has_value() ? &handler_target->block() : nullptr,
        finalizer_target.has_value() ? &finalizer_target->block() : nullptr,
        vm().running_execution_context().lexical_environment,
        vm().running_execution_context().variable_environment);
}

void Interpreter::leave_unwind_context()
{
    unwind_contexts().take_last();
}

ThrowCompletionOr<void> Interpreter::continue_pending_unwind(Label const& resume_label)
{
    if (!m_saved_exception.is_null()) {
        auto result = throw_completion(m_saved_exception.value());
        m_saved_exception = {};
        return result;
    }

    if (!m_saved_return_value.is_null()) {
        do_return(m_saved_return_value.value());
        m_saved_return_value = {};
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

VM::InterpreterExecutionScope Interpreter::ast_interpreter_scope()
{
    if (!m_ast_interpreter)
        m_ast_interpreter = JS::Interpreter::create_with_existing_realm(m_realm);

    return { *m_ast_interpreter };
}

AK::Array<OwnPtr<PassManager>, static_cast<UnderlyingType<Interpreter::OptimizationLevel>>(Interpreter::OptimizationLevel::__Count)> Interpreter::s_optimization_pipelines {};

Bytecode::PassManager& Interpreter::optimization_pipeline(Interpreter::OptimizationLevel level)
{
    auto underlying_level = to_underlying(level);
    VERIFY(underlying_level <= to_underlying(Interpreter::OptimizationLevel::__Count));
    auto& entry = s_optimization_pipelines[underlying_level];

    if (entry)
        return *entry;

    auto pm = make<PassManager>();
    if (level == OptimizationLevel::None) {
        // No optimization.
    } else if (level == OptimizationLevel::Optimize) {
        pm->add<Passes::GenerateCFG>();
        pm->add<Passes::UnifySameBlocks>();
        pm->add<Passes::GenerateCFG>();
        pm->add<Passes::MergeBlocks>();
        pm->add<Passes::GenerateCFG>();
        pm->add<Passes::UnifySameBlocks>();
        pm->add<Passes::GenerateCFG>();
        pm->add<Passes::MergeBlocks>();
        pm->add<Passes::GenerateCFG>();
        pm->add<Passes::PlaceBlocks>();
        pm->add<Passes::EliminateLoads>();
    } else {
        VERIFY_NOT_REACHED();
    }

    auto& passes = *pm;
    entry = move(pm);

    return passes;
}

}
