/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Execution.h"
#include "Forward.h"
#include <AK/ByteString.h>
#include <AK/Debug.h>
#include <AK/Function.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/OwnPtr.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/EventReceiver.h>

namespace Shell {

struct LocalFrame;

class Job : public RefCounted<Job> {
public:
    static NonnullRefPtr<Job> create(pid_t pid, pid_t pgid, ByteString cmd, u64 job_id, AST::Command&& command) { return adopt_ref(*new Job(pid, pgid, move(cmd), job_id, move(command))); }

    ~Job()
    {
        if constexpr (SHELL_JOB_DEBUG) {
            if (m_active) {
                auto elapsed = m_command_timer.elapsed();
                // Don't mistake this for the command!
                dbgln("Job entry '{}' deleted in {} ms", m_cmd, elapsed);
            }
        }
    }

    Function<void(RefPtr<Job>)> on_exit;

    pid_t pgid() const { return m_pgid; }
    pid_t pid() const { return m_pid; }
    ByteString const& cmd() const { return m_cmd; }
    const AST::Command& command() const { return *m_command; }
    AST::Command* command_ptr() { return m_command; }
    u64 job_id() const { return m_job_id; }
    bool exited() const { return m_exited; }
    bool signaled() const { return m_term_sig != -1; }
    int exit_code() const
    {
        VERIFY(exited());
        return m_exit_code;
    }
    int termination_signal() const
    {
        VERIFY(signaled());
        return m_term_sig;
    }
    bool should_be_disowned() const { return m_should_be_disowned; }
    void disown() { m_should_be_disowned = true; }
    bool is_running_in_background() const { return m_running_in_background; }
    bool should_announce_exit() const { return m_should_announce_exit; }
    bool should_announce_signal() const { return m_should_announce_signal; }
    bool is_suspended() const { return m_is_suspended; }
    bool shell_did_continue() const { return m_shell_did_continue; }
    void unblock();

    Core::ElapsedTimer& timer() { return m_command_timer; }

    void set_has_exit(int exit_code);
    void set_signalled(int sig);

    void set_is_suspended(bool value) const { m_is_suspended = value; }
    void set_shell_did_continue(bool value) const { m_shell_did_continue = value; }

    void set_running_in_background(bool running_in_background)
    {
        m_running_in_background = running_in_background;
    }

    void set_should_announce_exit(bool value) { m_should_announce_exit = value; }
    void set_should_announce_signal(bool value) { m_should_announce_signal = value; }

    void deactivate() const { m_active = false; }

    enum class PrintStatusMode {
        Basic,
        OnlyPID,
        ListAll,
    };

    bool print_status(PrintStatusMode);

private:
    Job(pid_t pid, unsigned pgid, ByteString cmd, u64 job_id, AST::Command&& command);

    pid_t m_pgid { 0 };
    pid_t m_pid { 0 };
    u64 m_job_id { 0 };
    ByteString m_cmd;
    bool m_exited { false };
    bool m_running_in_background { false };
    bool m_should_announce_exit { false };
    bool m_should_announce_signal { true };
    int m_exit_code { -1 };
    int m_term_sig { -1 };
    Core::ElapsedTimer m_command_timer;
    mutable bool m_active { true };
    mutable bool m_is_suspended { false };
    mutable bool m_shell_did_continue { false };
    bool m_should_be_disowned { false };
    OwnPtr<AST::Command> m_command;
};

}
