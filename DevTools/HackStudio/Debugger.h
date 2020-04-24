
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

class Debugger {
public:
    static Debugger& the();
    static void initialize(
        Function<void(DebugInfo::SourcePosition)> on_stop_callback,
        Function<void()> on_continue_callback,
        Function<void()> on_exit_callback);

    static bool is_initialized();

    static void on_breakpoint_change(const String& file, size_t line, BreakpointChange change_type);

    void set_executable_path(const String& path) { m_executable_path = path; }

    DebugSession* session() { return m_debug_session.ptr(); }

    // Thread entry point
    static int start_static();

    pthread_mutex_t* continue_mutex() { return &m_continue_mutex; }
    pthread_cond_t* continue_cond() { return &m_continue_cond; }

    enum class ContinueType {
        Continue,
        SourceSingleStep,
    };

    void set_continue_type(ContinueType type) { m_continue_type = type; }
    void reset_breakpoints() { m_breakpoints.clear(); }

private:
    explicit Debugger(
        Function<void(DebugInfo::SourcePosition)> on_stop_callback,
        Function<void()> on_continue_callback,
        Function<void()> on_exit_callback);

    static DebugInfo::SourcePosition create_source_position(const String& file, size_t line);

    void start();
    int debugger_loop();

    OwnPtr<DebugSession> m_debug_session;

    pthread_mutex_t m_continue_mutex {};
    pthread_cond_t m_continue_cond {};

    Vector<DebugInfo::SourcePosition> m_breakpoints;
    String m_executable_path;

    Function<void(DebugInfo::SourcePosition)> m_on_stopped_callback;
    Function<void()> m_on_continue_callback;
    Function<void()> m_on_exit_callback;

    ContinueType m_continue_type { ContinueType::Continue };
};
