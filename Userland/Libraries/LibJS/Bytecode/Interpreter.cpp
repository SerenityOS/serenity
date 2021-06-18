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
#include <LibJS/Runtime/GlobalObject.h>

namespace JS::Bytecode {

static Interpreter* s_current;

Interpreter* Interpreter::current()
{
    return s_current;
}

Interpreter::Interpreter(GlobalObject& global_object)
    : m_vm(global_object.vm())
    , m_global_object(global_object)
{
    VERIFY(!s_current);
    s_current = this;
}

Interpreter::~Interpreter()
{
    VERIFY(s_current == this);
    s_current = nullptr;
}

Value Interpreter::run(Executable const& executable, BasicBlock const* entry_point)
{
    dbgln_if(JS_BYTECODE_DEBUG, "Bytecode::Interpreter will run unit {:p}", &executable);

    TemporaryChange restore_executable { m_current_executable, &executable };

    vm().set_last_value(Badge<Interpreter> {}, {});

    CallFrame global_call_frame;
    if (vm().call_stack().is_empty()) {
        global_call_frame.this_value = &global_object();
        static FlyString global_execution_context_name = "(*BC* global execution context)";
        global_call_frame.function_name = global_execution_context_name;
        global_call_frame.scope = &global_object();
        VERIFY(!vm().exception());
        // FIXME: How do we know if we're in strict mode? Maybe the Bytecode::Block should know this?
        // global_call_frame.is_strict_mode = ???;
        vm().push_call_frame(global_call_frame, global_object());
        VERIFY(!vm().exception());
    }

    auto block = entry_point ?: &executable.basic_blocks.first();
    if (m_manually_entered_frames) {
        VERIFY(registers().size() >= executable.number_of_registers);
    } else {
        m_register_windows.append(make<RegisterWindow>());
        registers().resize(executable.number_of_registers);
        registers()[Register::global_object_index] = Value(&global_object());
    }

    for (;;) {
        Bytecode::InstructionStreamIterator pc(block->instruction_stream());
        bool will_jump = false;
        bool will_return = false;
        while (!pc.at_end()) {
            auto& instruction = *pc;
            instruction.execute(*this);
            if (vm().exception()) {
                m_saved_exception = {};
                if (m_unwind_contexts.is_empty())
                    break;
                auto& unwind_context = m_unwind_contexts.last();
                if (unwind_context.handler) {
                    block = unwind_context.handler;
                    unwind_context.handler = nullptr;
                    accumulator() = vm().exception()->value();
                    vm().clear_exception();
                    will_jump = true;
                } else if (unwind_context.finalizer) {
                    block = unwind_context.finalizer;
                    m_unwind_contexts.take_last();
                    will_jump = true;
                    m_saved_exception = Handle<Exception>::create(vm().exception());
                    vm().clear_exception();
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

        if (vm().exception())
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

    vm().set_last_value(Badge<Interpreter> {}, accumulator());

    if (!m_manually_entered_frames)
        m_register_windows.take_last();

    auto return_value = m_return_value.value_or(js_undefined());
    m_return_value = {};

    // NOTE: The return value from a called function is put into $0 in the caller context.
    if (!m_register_windows.is_empty())
        m_register_windows.last()[0] = return_value;

    if (vm().call_stack().size() == 1)
        vm().pop_call_frame();

    vm().finish_execution_generation();

    return return_value;
}

void Interpreter::enter_unwind_context(Optional<Label> handler_target, Optional<Label> finalizer_target)
{
    m_unwind_contexts.empend(handler_target.has_value() ? &handler_target->block() : nullptr, finalizer_target.has_value() ? &finalizer_target->block() : nullptr);
}

void Interpreter::leave_unwind_context()
{
    m_unwind_contexts.take_last();
}

void Interpreter::continue_pending_unwind(Label const& resume_label)
{
    if (!m_saved_exception.is_null()) {
        vm().set_exception(*m_saved_exception.cell());
        m_saved_exception = {};
    } else {
        jump(resume_label);
    }
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
