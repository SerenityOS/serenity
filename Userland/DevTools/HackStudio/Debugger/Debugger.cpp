/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Debugger.h"
#include <LibDebug/StackFrameUtils.h>

namespace HackStudio {

static Debugger* s_the;

Debugger& Debugger::the()
{
    VERIFY(s_the);
    return *s_the;
}

void Debugger::initialize(
    String source_root,
    Function<HasControlPassedToUser(const PtraceRegisters&)> on_stop_callback,
    Function<void()> on_continue_callback,
    Function<void()> on_exit_callback)
{
    s_the = new Debugger(source_root, move(on_stop_callback), move(on_continue_callback), move(on_exit_callback));
}

bool Debugger::is_initialized()
{
    return s_the;
}

Debugger::Debugger(
    String source_root,
    Function<HasControlPassedToUser(const PtraceRegisters&)> on_stop_callback,
    Function<void()> on_continue_callback,
    Function<void()> on_exit_callback)
    : m_source_root(source_root)
    , m_on_stopped_callback(move(on_stop_callback))
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
        m_breakpoints.append(position);
    } else {
        m_breakpoints.remove_all_matching([&](const Debug::DebugInfo::SourcePosition& val) { return val == position; });
    }

    auto session = Debugger::the().session();
    if (!session)
        return;

    auto address = session->get_address_from_source_position(position.file_path, position.line_number);
    if (!address.has_value()) {
        dbgln("Warning: couldn't get instruction address from source");
        // TODO: Currently, the GUI will indicate that a breakpoint was inserted/removed at this line,
        // regardless of whether we actually succeeded to insert it. (For example a breakpoint on a comment, or an include statement).
        // We should indicate failure via a return value from this function, and not update the breakpoint GUI if we fail.
        return;
    }

    if (change_type == BreakpointChange::Added) {
        bool success = session->insert_breakpoint(address.value().address);
        VERIFY(success);
    } else {
        bool success = session->remove_breakpoint(address.value().address);
        VERIFY(success);
    }
}

bool Debugger::set_execution_position(const String& file, size_t line)
{
    auto position = create_source_position(file, line);
    auto session = Debugger::the().session();
    if (!session)
        return false;
    auto address = session->get_address_from_source_position(position.file_path, position.line_number);
    if (!address.has_value())
        return false;
    auto registers = session->get_registers();
    registers.set_ip(address.value().address);
    session->set_registers(registers);
    return true;
}

Debug::DebugInfo::SourcePosition Debugger::create_source_position(const String& file, size_t line)
{
    if (file.starts_with("/"))
        return { file, line + 1 };
    return { LexicalPath::canonicalized_path(String::formatted("{}/{}", m_source_root, file)), line + 1 };
}

intptr_t Debugger::start_static()
{
    Debugger::the().start();
    return 0;
}

void Debugger::stop()
{
    set_requested_debugger_action(DebuggerAction::Exit);
}

void Debugger::start()
{

    auto child_setup_callback = [this]() {
        if (m_child_setup_callback)
            return m_child_setup_callback();
        return ErrorOr<void> {};
    };
    m_debug_session = Debug::DebugSession::exec_and_attach(m_executable_path, m_source_root, move(child_setup_callback));
    VERIFY(!!m_debug_session);

    for (const auto& breakpoint : m_breakpoints) {
        dbgln("inserting breakpoint at: {}:{}", breakpoint.file_path, breakpoint.line_number);
        auto address = m_debug_session->get_address_from_source_position(breakpoint.file_path, breakpoint.line_number);
        if (address.has_value()) {
            bool success = m_debug_session->insert_breakpoint(address.value().address);
            VERIFY(success);
        } else {
            dbgln("couldn't insert breakpoint");
        }
    }

    debugger_loop();
}

int Debugger::debugger_loop()
{
    VERIFY(m_debug_session);

    m_debug_session->run(Debug::DebugSession::DesiredInitialDebugeeState::Running, [this](Debug::DebugSession::DebugBreakReason reason, Optional<PtraceRegisters> optional_regs) {
        if (reason == Debug::DebugSession::DebugBreakReason::Exited) {
            dbgln("Program exited");
            m_on_exit_callback();
            return Debug::DebugSession::DebugDecision::Detach;
        }
        remove_temporary_breakpoints();
        VERIFY(optional_regs.has_value());
        const PtraceRegisters& regs = optional_regs.value();

        auto source_position = m_debug_session->get_source_position(regs.ip());
        if (!source_position.has_value())
            return Debug::DebugSession::DebugDecision::SingleStep;

        // We currently do no support stepping through assembly source
        if (source_position.value().file_path.ends_with(".S"))
            return Debug::DebugSession::DebugDecision::SingleStep;

        VERIFY(source_position.has_value());
        if (m_state.get() == Debugger::DebuggingState::SingleStepping) {
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
            dbgln("Debugger exiting");
            m_on_exit_callback();
            return Debug::DebugSession::DebugDecision::Kill;
        }
        VERIFY_NOT_REACHED();
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
    VERIFY(m_state == State::SingleStepping);
    return m_original_source_position.value() != current_source_position;
}

void Debugger::remove_temporary_breakpoints()
{
    for (auto breakpoint_address : m_state.temporary_breakpoints()) {
        VERIFY(m_debug_session->breakpoint_exists(breakpoint_address));
        bool rc = m_debug_session->remove_breakpoint(breakpoint_address);
        VERIFY(rc);
    }
    m_state.clear_temporary_breakpoints();
}

void Debugger::DebuggingState::clear_temporary_breakpoints()
{
    m_addresses_of_temporary_breakpoints.clear();
}
void Debugger::DebuggingState::add_temporary_breakpoint(FlatPtr address)
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
    auto lib = m_debug_session->library_at(regs.ip());
    if (!lib)
        return;
    auto current_function = lib->debug_info->get_containing_function(regs.ip() - lib->base_address);
    if (!current_function.has_value()) {
        dbgln("cannot perform step_over, failed to find containing function of: {:p}", regs.ip());
        return;
    }
    VERIFY(current_function.has_value());
    auto lines_in_current_function = lib->debug_info->source_lines_in_scope(current_function.value());
    for (const auto& line : lines_in_current_function) {
        insert_temporary_breakpoint(line.address_of_first_statement.value() + lib->base_address);
    }
    insert_temporary_breakpoint_at_return_address(regs);
}

void Debugger::insert_temporary_breakpoint_at_return_address(const PtraceRegisters& regs)
{
    auto frame_info = Debug::StackFrameUtils::get_info(*m_debug_session, regs.bp());
    VERIFY(frame_info.has_value());
    FlatPtr return_address = frame_info.value().return_address;
    insert_temporary_breakpoint(return_address);
}

void Debugger::insert_temporary_breakpoint(FlatPtr address)
{
    if (m_debug_session->breakpoint_exists(address))
        return;
    bool success = m_debug_session->insert_breakpoint(address);
    VERIFY(success);
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
