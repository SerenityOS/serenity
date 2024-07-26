/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Job.h"
#include "AST.h"
#include "Shell.h"
#include <inttypes.h>
#include <stdio.h>
#include <sys/wait.h>

namespace Shell {

bool Job::print_status(PrintStatusMode mode)
{
    int wstatus;
    auto rc = waitpid(m_pid, &wstatus, WNOHANG);
    auto status = "running";

    if (rc > 0) {
        if (WIFEXITED(wstatus))
            status = "exited";

        if (WIFSTOPPED(wstatus))
            status = "stopped";

        if (WIFSIGNALED(wstatus))
            status = "signaled";
    } else {
        // if rc < 0, We couldn't waitpid() it, probably because we're not the parent shell.
        // Otherwise, the information we have is already correct,
        // so just use the old information.
        if (exited())
            status = "exited";
        else if (m_is_suspended)
            status = "stopped";
        else if (signaled())
            status = "signaled";
    }

    char background_indicator = '-';

    if (is_running_in_background())
        background_indicator = '+';

    const AST::Command& command = *m_command;

    switch (mode) {
    case PrintStatusMode::Basic:
        outln("[{}] {} {} {}", m_job_id, background_indicator, status, command);
        break;
    case PrintStatusMode::OnlyPID:
        outln("[{}] {} {} {} {}", m_job_id, background_indicator, m_pid, status, command);
        break;
    case PrintStatusMode::ListAll:
        outln("[{}] {} {} {} {} {}", m_job_id, background_indicator, m_pid, m_pgid, status, command);
        break;
    }
    fflush(stdout);

    return true;
}

Job::Job(pid_t pid, unsigned pgid, ByteString cmd, u64 job_id, AST::Command&& command)
    : m_pgid(pgid)
    , m_pid(pid)
    , m_job_id(job_id)
    , m_cmd(move(cmd))
{
    m_command = make<AST::Command>(move(command));

    set_running_in_background(false);
    m_command_timer.start();
}

void Job::set_has_exit(int exit_code)
{
    if (m_exited)
        return;
    m_exit_code = exit_code;
    m_exited = true;
    if (on_exit)
        on_exit(*this);
}

void Job::set_signalled(int sig)
{
    if (m_exited)
        return;
    m_exited = true;
    m_exit_code = 126;
    m_term_sig = sig;
    if (on_exit)
        on_exit(*this);
}

void Job::unblock()
{
    if (!m_exited && on_exit)
        on_exit(*this);
}

}
