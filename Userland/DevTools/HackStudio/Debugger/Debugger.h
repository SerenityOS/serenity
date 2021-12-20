/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BreakpointCallback.h"
#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <AK/Vector.h>
#include <LibDebug/DebugSession.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>

namespace HackStudio {

class Debugger {
public:
    static Debugger& the();

    enum class HasControlPassedToUser {
        No,
        Yes,
    };

    static void initialize(
        String source_root,
        Function<HasControlPassedToUser(const PtraceRegisters&)> on_stop_callback,
        Function<void()> on_continue_callback,
        Function<void()> on_exit_callback);

    static bool is_initialized();

    void on_breakpoint_change(const String& file, size_t line, BreakpointChange change_type);
    bool set_execution_position(const String& file, size_t line);

    void set_executable_path(const String& path) { m_executable_path = path; }
    void set_source_root(const String& source_root) { m_source_root = source_root; }

    Debug::DebugSession* session() { return m_debug_session.ptr(); }

    void stop();

    // Thread entry point
    static intptr_t start_static();

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

    void set_child_setup_callback(Function<ErrorOr<void>()> callback) { m_child_setup_callback = move(callback); }

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
        void add_temporary_breakpoint(FlatPtr address);
        const Vector<FlatPtr>& temporary_breakpoints() const { return m_addresses_of_temporary_breakpoints; }

    private:
        State m_state { Normal };
        Optional<Debug::DebugInfo::SourcePosition> m_original_source_position; // The source position at which we started the current single step
        Vector<FlatPtr> m_addresses_of_temporary_breakpoints;
    };

    explicit Debugger(
        String source_root,
        Function<HasControlPassedToUser(const PtraceRegisters&)> on_stop_callback,
        Function<void()> on_continue_callback,
        Function<void()> on_exit_callback);

    Debug::DebugInfo::SourcePosition create_source_position(const String& file, size_t line);

    void start();
    int debugger_loop();

    void remove_temporary_breakpoints();
    void do_step_out(const PtraceRegisters&);
    void do_step_over(const PtraceRegisters&);
    void insert_temporary_breakpoint(FlatPtr address);
    void insert_temporary_breakpoint_at_return_address(const PtraceRegisters&);

    OwnPtr<Debug::DebugSession> m_debug_session;
    String m_source_root;
    DebuggingState m_state;

    pthread_mutex_t m_ui_action_mutex {};
    pthread_cond_t m_ui_action_cond {};
    DebuggerAction m_requested_debugger_action { DebuggerAction::Continue };

    Vector<Debug::DebugInfo::SourcePosition> m_breakpoints;

    String m_executable_path;

    Function<HasControlPassedToUser(const PtraceRegisters&)> m_on_stopped_callback;
    Function<void()> m_on_continue_callback;
    Function<void()> m_on_exit_callback;
    Function<ErrorOr<void>()> m_child_setup_callback;
};

}
