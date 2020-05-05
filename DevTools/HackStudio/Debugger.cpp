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
    pthread_mutex_init(&m_continue_mutex, nullptr);
    pthread_cond_init(&m_continue_cond, nullptr);
}

void Debugger::on_breakpoint_change(const String& file, size_t line, BreakpointChange change_type)
{
    auto position = create_source_position(file, line);

    if (change_type == BreakpointChange::Added) {
        Debugger::the().m_breakpoints.append(position);
    } else {
        Debugger::the().m_breakpoints.remove_all_matching([&](DebugInfo::SourcePosition val) { return val == position; });
    }

    auto session = Debugger::the().session();
    if (!session)
        return;

    auto address = session->debug_info().get_instruction_from_source(position.file_path, position.line_number);
    if (!address.has_value())
        return;

    if (change_type == BreakpointChange::Added) {
        bool success = session->insert_breakpoint(reinterpret_cast<void*>(address.value()));
        ASSERT(success);
    } else {
        bool success = session->remove_breakpoint(reinterpret_cast<void*>(address.value()));
        ASSERT(success);
    }
}

DebugInfo::SourcePosition Debugger::create_source_position(const String& file, size_t line)
{
    return { String::format("./%s", file.characters()), line + 1 };
}

int Debugger::start_static()
{
    Debugger::the().start();
    return 0;
}

void Debugger::start()
{
    m_debug_session = DebugSession::exec_and_attach(m_executable_path);
    ASSERT(!!m_debug_session);

    for (const auto& breakpoint : m_breakpoints) {
        dbg() << "insertig breakpoint at: " << breakpoint.file_path << ":" << breakpoint.line_number;
        auto address = m_debug_session->debug_info().get_instruction_from_source(breakpoint.file_path, breakpoint.line_number);
        if (address.has_value()) {
            bool success = m_debug_session->insert_breakpoint(reinterpret_cast<void*>(address.value()));
            ASSERT(success);
        } else {
            dbg() << "couldn't insert breakpoint";
        }
    }

    debugger_loop();
}

int Debugger::debugger_loop()
{
    bool in_single_step_mode = false;
    Vector<void*> temporary_breakpoints;

    m_debug_session->run([&](DebugSession::DebugBreakReason reason, Optional<PtraceRegisters> optional_regs) {
        if (reason == DebugSession::DebugBreakReason::Exited) {
            dbg() << "Program exited";
            m_on_exit_callback();
            return DebugSession::DebugDecision::Detach;
        }
        ASSERT(optional_regs.has_value());
        const PtraceRegisters& regs = optional_regs.value();

        if (in_single_step_mode) {
            for (auto address : temporary_breakpoints) {
                m_debug_session->remove_breakpoint(address);
            }
            temporary_breakpoints.clear();
            in_single_step_mode = false;
        }

        auto control_passed_to_user = m_on_stopped_callback(regs);

        if (control_passed_to_user == HasControlPassedToUser::Yes) {
            pthread_mutex_lock(&m_continue_mutex);
            pthread_cond_wait(&m_continue_cond, &m_continue_mutex);
            pthread_mutex_unlock(&m_continue_mutex);

            m_on_continue_callback();
        } else {
            m_continue_type = ContinueType::Continue;
        }

        if (m_continue_type == ContinueType::Continue) {
            return DebugSession::DebugDecision::Continue;
        }

        if (m_continue_type == ContinueType::SourceSingleStep) {
            // A possible method for source level single stepping is to single step
            // in assembly level, until the current instruction's source position has changed.
            // However, since we do not currently generate debug symbols for library code,
            // we may have to single-step over lots of library code instructions until we get back to our code,
            // which is very slow.
            // So the current method is to insert a temporary breakpoint at every known statement in our source code,
            // continue execution, and remove the temporary breakpoints once we hit the first breakpoint.
            m_debug_session->debug_info().for_each_source_position([&](DebugInfo::SourcePosition position) {
                auto address = (void*)position.address_of_first_statement;
                if ((u32)address != regs.eip && !m_debug_session->breakpoint_exists(address)) {
                    m_debug_session->insert_breakpoint(address);
                    temporary_breakpoints.append(address);
                }
            });
            in_single_step_mode = true;
            return DebugSession::DebugDecision::Continue;
        }
        ASSERT_NOT_REACHED();
    });
    m_debug_session.clear();
    return 0;
}
