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
#include <inttypes.h>
#include <stdio.h>
#include <sys/wait.h>

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
