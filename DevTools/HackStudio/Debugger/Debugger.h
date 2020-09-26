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

#pragma once

#include "BreakpointCallback.h"
#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibDebug/DebugSession.h>
#include <LibThread/Lock.h>
#include <LibThread/Thread.h>

namespace HackStudio {

class Debugger {
public:
    static Debugger& the();

    enum class HasControlPassedToUser {
        No,
        Yes,
    };

    static void initialize(
        Function<HasControlPassedToUser(const PtraceRegisters&)> on_stop_callback,
        Function<void()> on_continue_callback,
        Function<void()> on_exit_callback);

    static bool is_initialized();

    static void on_breakpoint_change(const String& file, size_t line, BreakpointChange change_type);

    void set_executable_path(const String& path) { m_executable_path = path; }

    Debug::DebugSession* session() { return m_debug_session.ptr(); }

    // Thread entry point
    static int start_static();

    pthread_mutex_t* continue_mutex() { return &m_ui_action_mutex; }
    pthread_cond_t* continue_cond() { return &m_ui_action_cond; }

    enum class DebuggerAction {
        Continue,
        SourceSingleStep,
        SourceStepOut,
        SourceStepOver,
        Exit,
    };

    void set_requested_debugger_action(DebuggerAction);
    void reset_breakpoints() { m_breakpoints.clear(); }

private:
    class DebuggingState {
    public:
        enum State {
            Normal, // Continue normally until we hit a breakpoint / program terminates
            SingleStepping,
            SteppingOut,
            SteppingOver,
        };
        State get() const { return m_state; }

        void set_normal();
        void set_single_stepping(Debug::DebugInfo::SourcePosition original_source_position);
        void set_stepping_out() { m_state = State::SteppingOut; }
        void set_stepping_over() { m_state = State::SteppingOver; }

        bool should_stop_single_stepping(const Debug::DebugInfo::SourcePosition& current_source_position) const;
        void clear_temporary_breakpoints();
        void add_temporary_breakpoint(u32 address);
        const Vector<u32>& temporary_breakpoints() const { return m_addresses_of_temporary_breakpoints; }

    private:
        State m_state { Normal };
        Optional<Debug::DebugInfo::SourcePosition> m_original_source_position; // The source position at which we started the current single step
        Vector<u32> m_addresses_of_temporary_breakpoints;
    };

    explicit Debugger(
        Function<HasControlPassedToUser(const PtraceRegisters&)> on_stop_callback,
        Function<void()> on_continue_callback,
        Function<void()> on_exit_callback);

    static Debug::DebugInfo::SourcePosition create_source_position(const String& file, size_t line);

    void start();
    int debugger_loop();

    void remove_temporary_breakpoints();
    void do_step_out(const PtraceRegisters&);
    void do_step_over(const PtraceRegisters&);
    void insert_temporary_breakpoint(FlatPtr address);
    void insert_temporary_breakpoint_at_return_address(const PtraceRegisters&);

    OwnPtr<Debug::DebugSession> m_debug_session;
    DebuggingState m_state;

    pthread_mutex_t m_ui_action_mutex {};
    pthread_cond_t m_ui_action_cond {};
    DebuggerAction m_requested_debugger_action { DebuggerAction::Continue };

    Vector<Debug::DebugInfo::SourcePosition> m_breakpoints;

    String m_executable_path;

    Function<HasControlPassedToUser(const PtraceRegisters&)> m_on_stopped_callback;
    Function<void()> m_on_continue_callback;
    Function<void()> m_on_exit_callback;
};

}
