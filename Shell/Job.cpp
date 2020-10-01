/*
 * Copyright (c) 2020, the SerenityOS developers
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
    if (rc == -1) {
        perror("waitpid");
        return false;
    }
    auto status = "running";

    if (rc != 0) {
        if (WIFEXITED(wstatus))
            status = "exited";

        if (WIFSTOPPED(wstatus))
            status = "stopped";

        if (WIFSIGNALED(wstatus))
            status = "signaled";
    }

    char background_indicator = '-';

    if (is_running_in_background())
        background_indicator = '+';

    switch (mode) {
    case PrintStatusMode::Basic:
        printf("[%" PRIu64 "] %c %s %s\n", m_job_id, background_indicator, status, m_cmd.characters());
        break;
    case PrintStatusMode::OnlyPID:
        printf("[%" PRIu64 "] %c %d %s %s\n", m_job_id, background_indicator, m_pid, status, m_cmd.characters());
        break;
    case PrintStatusMode::ListAll:
        printf("[%" PRIu64 "] %c %d %d %s %s\n", m_job_id, background_indicator, m_pid, m_pgid, status, m_cmd.characters());
        break;
    }

    return true;
}

Job::Job(pid_t pid, unsigned pgid, String cmd, u64 job_id, AST::Command&& command)
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

void Job::unblock() const
{
    if (!m_exited && on_exit)
        on_exit(*this);
}

}
