/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include "Debugger.h"
#include <LibDebug/StackFrameUtils.h>

namespace HackStudio {

static Debugger* s_the;

Debugger& Debugger::the()
{
    ASSERT(s_the);
    return *s_the;
}

void Debugger::initialize(
    Function<HasControlPassedToUser(const PtraceRegisters&)> on_stop_callback,
    Function<void()> on_continue_callback,
    Function<void()> on_exit_callback)
{
    s_the = new Debugger(move(on_stop_callback), move(on_continue_callback), move(on_exit_callback));
}

bool Debugger::is_initialized()
{
    return s_the;
}

Debugger::Debugger(
    Function<HasControlPassedToUser(const PtraceRegisters&)> on_stop_callback,
    Function<void()> on_continue_callback,
    Function<void()> on_exit_callback)
    : m_on_stopped_callback(move(on_stop_callback))
    , m_on_continue_callback(move(on_continue_callback))
    , m_on_exit_callback(move(on_exit_callback))
{
    pthread_mutex_init(&m_ui_action_mutex, nullptr);
    pthread_cond_init(&m_ui_action_cond, nullptr);
}

void Debugger::on_breakpoint_change(const String& file, size_t line, BreakpointChange change_type)
{
    auto position = create_source_position(file, line);

    if (change_type == BreakpointChange::Added) {
        Debugger::the().m_breakpoints.append(position);
    } else {
        Debugger::the().m_breakpoints.remove_all_matching([&](Debug::DebugInfo::SourcePosition val) { return val == position; });
    }

    auto session = Debugger::the().session();
    if (!session)
        return;

    auto address = session->debug_info().get_instruction_from_source(position.file_path, position.line_number);
    if (!address.has_value()) {
        dbgln("Warning: couldn't get instruction address from source");
        // TODO: Currently, the GUI will indicate that a breakpoint was inserted/removed at this line,
        // regardless of whether we actually succeeded to insert it. (For example a breakpoint on a comment, or an include statement).
        // We should indicate failure via a return value from this function, and not update the breakpoint GUI if we fail.
        return;
    }

    if (change_type == BreakpointChange::Added) {
        bool success = session->insert_breakpoint(reinterpret_cast<void*>(address.value()));
        ASSERT(success);
    } else {
        bool success = session->remove_breakpoint(reinterpret_cast<void*>(address.value()));
        ASSERT(success);
    }
}

Debug::DebugInfo::SourcePosition Debugger::create_source_position(const String& file, size_t line)
{
    if (!file.starts_with('/') && !file.starts_with("./"))
        return { String::formatted("./{}", file), line + 1 };
    return { file, line + 1 };
}

int Debugger::start_static()
{
    Debugger::the().start();
    return 0;
}

void Debugger::start()
{
    m_debug_session = Debug::DebugSession::exec_and_attach(m_executable_path);
    ASSERT(!!m_debug_session);

    for (const auto& breakpoint : m_breakpoints) {
        dbgln("insertig breakpoint at: {}:{}", breakpoint.file_path, breakpoint.line_number);
        auto address = m_debug_session->debug_info().get_instruction_from_source(breakpoint.file_path, breakpoint.line_number);
        if (address.has_value()) {
            bool success = m_debug_session->insert_breakpoint(reinterpret_cast<void*>(address.value()));
            ASSERT(success);
        } else {
            dbgln("couldn't insert breakpoint");
        }
    }

    debugger_loop();
}

int Debugger::debugger_loop()
{
    ASSERT(m_debug_session);

    m_debug_session->run([this](Debug::DebugSession::DebugBreakReason reason, Optional<PtraceRegisters> optional_regs) {
        if (reason == Debug::DebugSession::DebugBreakReason::Exited) {
            dbgln("Program exited");
            m_on_exit_callback();
            return Debug::DebugSession::DebugDecision::Detach;
        }
        remove_temporary_breakpoints();
        ASSERT(optional_regs.has_value());
        const PtraceRegisters& regs = optional_regs.value();

        auto source_position = m_debug_session->debug_info().get_source_position(regs.eip);
        if (m_state.get() == Debugger::DebuggingState::SingleStepping) {
            ASSERT(source_position.has_value());
            if (m_state.should_stop_single_stepping(source_position.value())) {
                m_state.set_normal();
            } else {
                return Debug::DebugSession::DebugDecision::SingleStep;
            }
        }

        auto control_passed_to_user = m_on_stopped_callback(regs);

        if (control_passed_to_user == HasControlPassedToUser::Yes) {
            pthread_mutex_lock(&m_ui_action_mutex);
            pthread_cond_wait(&m_ui_action_cond, &m_ui_action_mutex);
            pthread_mutex_unlock(&m_ui_action_mutex);

            if (m_requested_debugger_action != DebuggerAction::Exit)
                m_on_continue_callback();

        } else {
            m_requested_debugger_action = DebuggerAction::Continue;
        }

        switch (m_requested_debugger_action) {
        case DebuggerAction::Continue:
            m_state.set_normal();
            return Debug::DebugSession::DebugDecision::Continue;
        case DebuggerAction::SourceSingleStep:
            m_state.set_single_stepping(source_position.value());
            return Debug::DebugSession::DebugDecision::SingleStep;
        case DebuggerAction::SourceStepOut:
            m_state.set_stepping_out();
            do_step_out(regs);
            return Debug::DebugSession::DebugDecision::Continue;
        case DebuggerAction::SourceStepOver:
            m_state.set_stepping_over();
            do_step_over(regs);
            return Debug::DebugSession::DebugDecision::Continue;
        case DebuggerAction::Exit:
            // NOTE: Is detaching from the debuggee the best thing to do here?
            // We could display a dialog in the UI, remind the user that there is
            // a live debugged process, and ask whether they want to terminate/detach.
            dbgln("Debugger exiting");
            return Debug::DebugSession::DebugDecision::Detach;
        }
        ASSERT_NOT_REACHED();
    });
    m_debug_session.clear();
    return 0;
}

void Debugger::DebuggingState::set_normal()
{
    m_state = State::Normal;
    m_original_source_position.clear();
}

void Debugger::DebuggingState::set_single_stepping(Debug::DebugInfo::SourcePosition original_source_position)
{
    m_state = State::SingleStepping;
    m_original_source_position = original_source_position;
}

bool Debugger::DebuggingState::should_stop_single_stepping(const Debug::DebugInfo::SourcePosition& current_source_position) const
{
    ASSERT(m_state == State::SingleStepping);
    return m_original_source_position.value() != current_source_position;
}

void Debugger::remove_temporary_breakpoints()
{
    for (auto breakpoint_address : m_state.temporary_breakpoints()) {
        ASSERT(m_debug_session->breakpoint_exists((void*)breakpoint_address));
        bool rc = m_debug_session->remove_breakpoint((void*)breakpoint_address);
        ASSERT(rc);
    }
    m_state.clear_temporary_breakpoints();
}

void Debugger::DebuggingState::clear_temporary_breakpoints()
{
    m_addresses_of_temporary_breakpoints.clear();
}
void Debugger::DebuggingState::add_temporary_breakpoint(u32 address)
{
    m_addresses_of_temporary_breakpoints.append(address);
}

void Debugger::do_step_out(const PtraceRegisters& regs)
{
    // To step out, we simply insert a temporary breakpoint at the
    // instruction the current function returns to, and continue
    // execution until we hit that instruction (or some other breakpoint).
    insert_temporary_breakpoint_at_return_address(regs);
}

void Debugger::do_step_over(const PtraceRegisters& regs)
{
    // To step over, we insert a temporary breakpoint at each line in the current function,
    // as well as at the current function's return point, and continue execution.
    auto current_function = m_debug_session->debug_info().get_containing_function(regs.eip);
    ASSERT(current_function.has_value());
    auto lines_in_current_function = m_debug_session->debug_info().source_lines_in_scope(current_function.value());
    for (const auto& line : lines_in_current_function) {
        insert_temporary_breakpoint(line.address_of_first_statement);
    }
    insert_temporary_breakpoint_at_return_address(regs);
}

void Debugger::insert_temporary_breakpoint_at_return_address(const PtraceRegisters& regs)
{
    auto frame_info = Debug::StackFrameUtils::get_info(*m_debug_session, regs.ebp);
    ASSERT(frame_info.has_value());
    u32 return_address = frame_info.value().return_address;
    insert_temporary_breakpoint(return_address);
}

void Debugger::insert_temporary_breakpoint(FlatPtr address)
{
    if (m_debug_session->breakpoint_exists((void*)address))
        return;
    bool success = m_debug_session->insert_breakpoint(reinterpret_cast<void*>(address));
    ASSERT(success);
    m_state.add_temporary_breakpoint(address);
}

void Debugger::set_requested_debugger_action(DebuggerAction action)
{
    pthread_mutex_lock(continue_mutex());
    m_requested_debugger_action = action;
    pthread_cond_signal(continue_cond());
    pthread_mutex_unlock(continue_mutex());
}

}
