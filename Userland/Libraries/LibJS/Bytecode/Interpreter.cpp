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

Interpreter::Interpreter(GlobalObject& global_object, Realm& realm)
    : m_vm(global_object.vm())
    , m_global_object(global_object)
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

Interpreter::ValueAndFrame Interpreter::run_and_return_frame(Executable const& executable, BasicBlock const* entry_point)
{
    dbgln_if(JS_BYTECODE_DEBUG, "Bytecode::Interpreter will run unit {:p}", &executable);

    TemporaryChange restore_executable { m_current_executable, &executable };
    VERIFY(m_saved_exception.is_null());

    bool pushed_execution_context = false;
    ExecutionContext execution_context(vm().heap());
    if (vm().execution_context_stack().is_empty() || !vm().running_execution_context().lexical_environment) {
        // The "normal" interpreter pushes an execution context without environment so in that case we also want to push one.
        execution_context.this_value = &global_object();
        static FlyString global_execution_context_name = "(*BC* global execution context)";
        execution_context.function_name = global_execution_context_name;
        execution_context.lexical_environment = &m_realm.global_environment();
        execution_context.variable_environment = &m_realm.global_environment();
        execution_context.realm = &m_realm;
        // FIXME: How do we know if we're in strict mode? Maybe the Bytecode::Block should know this?
        // execution_context.is_strict_mode = ???;
        MUST(vm().push_execution_context(execution_context, global_object()));
        pushed_execution_context = true;
    }

    auto block = entry_point ?: &executable.basic_blocks.first();
    if (!m_manually_entered_frames.is_empty() && m_manually_entered_frames.last()) {
        m_register_windows.append(make<RegisterWindow>(m_register_windows.last()));
    } else {
        m_register_windows.append(make<RegisterWindow>(MarkedVector<Value>(vm().heap()), MarkedVector<Environment*>(vm().heap()), MarkedVector<Environment*>(vm().heap())));
    }

    registers().resize(executable.number_of_registers);
    registers()[Register::global_object_index] = Value(&global_object());
    m_manually_entered_frames.append(false);

    for (;;) {
        Bytecode::InstructionStreamIterator pc(block->instruction_stream());
        bool will_jump = false;
        bool will_return = false;
        while (!pc.at_end()) {
            auto& instruction = *pc;
            auto ran_or_error = instruction.execute(*this);
            if (ran_or_error.is_error()) {
                auto exception_value = *ran_or_error.throw_completion().value();
                m_saved_exception = make_handle(exception_value);
                if (m_unwind_contexts.is_empty())
                    break;
                auto& unwind_context = m_unwind_contexts.last();
                if (unwind_context.executable != m_current_executable)
                    break;
                if (unwind_context.handler) {
                    block = unwind_context.handler;
                    unwind_context.handler = nullptr;
                    accumulator() = exception_value;
                    m_saved_exception = {};
                    will_jump = true;
                    break;
                }
                if (unwind_context.finalizer) {
                    block = unwind_context.finalizer;
                    m_unwind_contexts.take_last();
                    will_jump = true;
                    break;
                }
            }
            if (m_pending_jump.has_value()) {
                block = m_pending_jump.release_value();
                will_jump = true;
                break;
            }
            if (!m_return_value.is_empty()) {
                will_return = true;
                break;
            }
            ++pc;
        }

        if (will_return)
            break;

        if (pc.at_end() && !will_jump)
            break;

        if (!m_saved_exception.is_null())
            break;
    }

    dbgln_if(JS_BYTECODE_DEBUG, "Bytecode::Interpreter did run unit {:p}", &executable);

    if constexpr (JS_BYTECODE_DEBUG) {
        for (size_t i = 0; i < registers().size(); ++i) {
            String value_string;
            if (registers()[i].is_empty())
                value_string = "(empty)";
            else
                value_string = registers()[i].to_string_without_side_effects();
            dbgln("[{:3}] {}", i, value_string);
        }
    }

    OwnPtr<RegisterWindow> frame;
    if (!m_manually_entered_frames.last()) {
        frame = m_register_windows.take_last();
        m_manually_entered_frames.take_last();
    }

    auto return_value = m_return_value.value_or(js_undefined());
    m_return_value = {};

    // NOTE: The return value from a called function is put into $0 in the caller context.
    if (!m_register_windows.is_empty())
        m_register_windows.last().registers[0] = return_value;

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
        return { throw_completion(thrown_value), move(frame) };
    }

    return { return_value, move(frame) };
}

void Interpreter::enter_unwind_context(Optional<Label> handler_target, Optional<Label> finalizer_target)
{
    m_unwind_contexts.empend(m_current_executable, handler_target.has_value() ? &handler_target->block() : nullptr, finalizer_target.has_value() ? &finalizer_target->block() : nullptr);
}

void Interpreter::leave_unwind_context()
{
    m_unwind_contexts.take_last();
}

ThrowCompletionOr<void> Interpreter::continue_pending_unwind(Label const& resume_label)
{
    if (!m_saved_exception.is_null()) {
        auto result = throw_completion(m_saved_exception.value());
        m_saved_exception = {};
        return result;
    }

    jump(resume_label);
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
    if (level == OptimizationLevel::Default) {
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
    } else {
        VERIFY_NOT_REACHED();
    }

    auto& passes = *pm;
    entry = move(pm);

    return passes;
}

}
