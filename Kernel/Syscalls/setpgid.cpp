/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/Process.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

pid_t Process::sys$getsid(pid_t pid)
{
    REQUIRE_PROMISE(proc);
    if (pid == 0)
        return m_sid.value();
    ScopedSpinLock lock(g_processes_lock);
    auto process = Process::from_pid(pid);
    if (!process)
        return -ESRCH;
    if (m_sid != process->m_sid)
        return -EPERM;
    return process->m_sid.value();
}

pid_t Process::sys$setsid()
{
    REQUIRE_PROMISE(proc);
    InterruptDisabler disabler;
    bool found_process_with_same_pgid_as_my_pid = false;
    Process::for_each_in_pgrp(pid().value(), [&](auto&) {
        found_process_with_same_pgid_as_my_pid = true;
        return IterationDecision::Break;
    });
    if (found_process_with_same_pgid_as_my_pid)
        return -EPERM;
    // Create a new Session and a new ProcessGroup.
    m_sid = m_pid.value();
    m_pg = ProcessGroup::create(ProcessGroupID(m_pid.value()));
    m_tty = nullptr;
    return m_sid.value();
}

pid_t Process::sys$getpgid(pid_t pid)
{
    REQUIRE_PROMISE(proc);
    if (pid == 0)
        return pgid().value();
    ScopedSpinLock lock(g_processes_lock); // FIXME: Use a ProcessHandle
    auto process = Process::from_pid(pid);
    if (!process)
        return -ESRCH;
    return process->pgid().value();
}

pid_t Process::sys$getpgrp()
{
    REQUIRE_PROMISE(stdio);
    return pgid().value();
}

SessionID Process::get_sid_from_pgid(ProcessGroupID pgid)
{
    // FIXME: This xor sys$setsid() uses the wrong locking mechanism.
    ScopedSpinLock lock(g_processes_lock);

    SessionID sid { -1 };
    Process::for_each_in_pgrp(pgid, [&](auto& process) {
        sid = process.sid();
        return IterationDecision::Break;
    });

    return sid;
}

int Process::sys$setpgid(pid_t specified_pid, pid_t specified_pgid)
{
    REQUIRE_PROMISE(proc);
    ScopedSpinLock lock(g_processes_lock); // FIXME: Use a ProcessHandle
    ProcessID pid = specified_pid ? ProcessID(specified_pid) : m_pid;
    if (specified_pgid < 0) {
        // The value of the pgid argument is less than 0, or is not a value supported by the implementation.
        return -EINVAL;
    }
    auto process = Process::from_pid(pid);
    if (!process)
        return -ESRCH;
    if (process != this && process->ppid() != m_pid) {
        // The value of the pid argument does not match the process ID
        // of the calling process or of a child process of the calling process.
        return -ESRCH;
    }
    if (process->is_session_leader()) {
        // The process indicated by the pid argument is a session leader.
        return -EPERM;
    }
    if (process->ppid() == m_pid && process->sid() != sid()) {
        // The value of the pid argument matches the process ID of a child
        // process of the calling process and the child process is not in
        // the same session as the calling process.
        return -EPERM;
    }

    ProcessGroupID new_pgid = specified_pgid ? ProcessGroupID(specified_pgid) : process->m_pid.value();
    SessionID current_sid = sid();
    SessionID new_sid = get_sid_from_pgid(new_pgid);
    if (new_sid != -1 && current_sid != new_sid) {
        // Can't move a process between sessions.
        return -EPERM;
    }
    if (new_sid == -1 && new_pgid != process->m_pid.value()) {
        // The value of the pgid argument is valid, but is not
        // the calling pid, and is not an existing process group.
        return -EPERM;
    }
    // FIXME: There are more EPERM conditions to check for here..
    process->m_pg = ProcessGroup::find_or_create(new_pgid);
    return 0;
}

}
