/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include "Execution.h"
#include "Forward.h"
#include <AK/Function.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/Object.h>

#define JOB_TIME_INFO
#ifndef __serenity__
#    undef JOB_TIME_INFO
#endif

namespace Shell {

struct LocalFrame;

class Job : public RefCounted<Job> {
public:
    static NonnullRefPtr<Job> create(pid_t pid, pid_t pgid, String cmd, u64 job_id, AST::Command&& command) { return adopt(*new Job(pid, pgid, move(cmd), job_id, move(command))); }

    ~Job()
    {
#ifdef JOB_TIME_INFO
        if (m_active) {
            auto elapsed = m_command_timer.elapsed();
            // Don't mistake this for the command!
            dbg() << "Job entry \"" << m_cmd << "\" deleted in " << elapsed << " ms";
        }
#endif
    }

    Function<void(RefPtr<Job>)> on_exit;

    pid_t pgid() const { return m_pgid; }
    pid_t pid() const { return m_pid; }
    const String& cmd() const { return m_cmd; }
    const AST::Command& command() const { return *m_command; }
    AST::Command* command_ptr() { return m_command; }
    u64 job_id() const { return m_job_id; }
    bool exited() const { return m_exited; }
    bool signaled() const { return m_term_sig != -1; }
    int exit_code() const
    {
        ASSERT(exited());
        return m_exit_code;
    }
    int termination_signal() const
    {
        ASSERT(signaled());
        return m_term_sig;
    }
    bool should_be_disowned() const { return m_should_be_disowned; }
    void disown() { m_should_be_disowned = true; }
    bool is_running_in_background() const { return m_running_in_background; }
    bool should_announce_exit() const { return m_should_announce_exit; }
    bool is_suspended() const { return m_is_suspended; }
    void unblock() const;

    Core::ElapsedTimer& timer() { return m_command_timer; }

    void set_has_exit(int exit_code);
    void set_signalled(int sig);

    void set_is_suspended(bool value) const { m_is_suspended = value; }

    void set_running_in_background(bool running_in_background)
    {
        m_running_in_background = running_in_background;
    }

    void set_should_announce_exit(bool value) { m_should_announce_exit = value; }

    void deactivate() const { m_active = false; }

    enum class PrintStatusMode {
        Basic,
        OnlyPID,
        ListAll,
    };

    bool print_status(PrintStatusMode);

private:
    Job(pid_t pid, unsigned pgid, String cmd, u64 job_id, AST::Command&& command);

    pid_t m_pgid { 0 };
    pid_t m_pid { 0 };
    u64 m_job_id { 0 };
    String m_cmd;
    bool m_exited { false };
    bool m_running_in_background { false };
    bool m_should_announce_exit { false };
    int m_exit_code { -1 };
    int m_term_sig { -1 };
    Core::ElapsedTimer m_command_timer;
    mutable bool m_active { true };
    mutable bool m_is_suspended { false };
    bool m_should_be_disowned { false };
    OwnPtr<AST::Command> m_command;
};

}
