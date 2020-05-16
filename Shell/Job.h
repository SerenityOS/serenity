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
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/Object.h>

class Job {
public:
    explicit Job()
    {
    }

    ~Job()
    {
        auto elapsed = m_command_timer.elapsed();
        dbg() << "Command \"" << m_cmd << "\" finished in " << elapsed << " ms";
    }

    Job(pid_t pid, unsigned pgid, String cmd, u64 job_id)
        : m_pgid(pgid)
        , m_pid(pid)
        , m_job_id(job_id)
        , m_cmd(move(cmd))
    {
        set_running_in_background(false);
        m_command_timer.start();
    }

    unsigned pgid() const { return m_pgid; }
    pid_t pid() const { return m_pid; }
    const String& cmd() const { return m_cmd; }
    u64 job_id() const { return m_job_id; }
    bool exited() const { return m_exited; }
    int exit_code() const { return m_exit_code; }
    bool is_running_in_background() const { return m_running_in_background; }

    Core::ElapsedTimer& timer() { return m_command_timer; }

    void set_has_exit(int exit_code)
    {
        if (m_exited)
            return;
        m_exit_code = exit_code;
        m_exited = true;
    }

    void set_running_in_background(bool running_in_background)
    {
        m_running_in_background = running_in_background;
    }

private:
    unsigned m_pgid { 0 };
    pid_t m_pid { 0 };
    u64 m_job_id { 0 };
    String m_cmd;
    bool m_exited { false };
    bool m_running_in_background { false };
    int m_exit_code { -1 };
    Core::ElapsedTimer m_command_timer;
};
