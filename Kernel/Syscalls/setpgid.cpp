/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

KResultOr<pid_t> Process::sys$getsid(pid_t pid)
{
    REQUIRE_PROMISE(proc);
    if (pid == 0)
        return sid().value();
    ScopedSpinLock lock(g_processes_lock);
    auto process = Process::from_pid(pid);
    if (!process)
        return ESRCH;
    if (sid() != process->sid())
        return EPERM;
    return process->sid().value();
}

KResultOr<pid_t> Process::sys$setsid()
{
    REQUIRE_PROMISE(proc);
    InterruptDisabler disabler;
    bool found_process_with_same_pgid_as_my_pid = false;
    Process::for_each_in_pgrp(pid().value(), [&](auto&) {
        found_process_with_same_pgid_as_my_pid = true;
        return IterationDecision::Break;
    });
    if (found_process_with_same_pgid_as_my_pid)
        return EPERM;
    // Create a new Session and a new ProcessGroup.
    m_pg = ProcessGroup::create(ProcessGroupID(pid().value()));
    m_tty = nullptr;
    ProtectedDataMutationScope scope { *this };
    m_sid = pid().value();
    return sid().value();
}

KResultOr<pid_t> Process::sys$getpgid(pid_t pid)
{
    REQUIRE_PROMISE(proc);
    if (pid == 0)
        return pgid().value();
    ScopedSpinLock lock(g_processes_lock); // FIXME: Use a ProcessHandle
    auto process = Process::from_pid(pid);
    if (!process)
        return ESRCH;
    return process->pgid().value();
}

KResultOr<pid_t> Process::sys$getpgrp()
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

KResultOr<int> Process::sys$setpgid(pid_t specified_pid, pid_t specified_pgid)
{
    REQUIRE_PROMISE(proc);
    ScopedSpinLock lock(g_processes_lock); // FIXME: Use a ProcessHandle
    ProcessID pid = specified_pid ? ProcessID(specified_pid) : this->pid();
    if (specified_pgid < 0) {
        // The value of the pgid argument is less than 0, or is not a value supported by the implementation.
        return EINVAL;
    }
    auto process = Process::from_pid(pid);
    if (!process)
        return ESRCH;
    if (process != this && process->ppid() != this->pid()) {
        // The value of the pid argument does not match the process ID
        // of the calling process or of a child process of the calling process.
        return ESRCH;
    }
    if (process->is_session_leader()) {
        // The process indicated by the pid argument is a session leader.
        return EPERM;
    }
    if (process->ppid() == this->pid() && process->sid() != sid()) {
        // The value of the pid argument matches the process ID of a child
        // process of the calling process and the child process is not in
        // the same session as the calling process.
        return EPERM;
    }

    ProcessGroupID new_pgid = specified_pgid ? ProcessGroupID(specified_pgid) : process->pid().value();
    SessionID current_sid = sid();
    SessionID new_sid = get_sid_from_pgid(new_pgid);
    if (new_sid != -1 && current_sid != new_sid) {
        // Can't move a process between sessions.
        return EPERM;
    }
    if (new_sid == -1 && new_pgid != process->pid().value()) {
        // The value of the pgid argument is valid, but is not
        // the calling pid, and is not an existing process group.
        return EPERM;
    }
    // FIXME: There are more EPERM conditions to check for here..
    process->m_pg = ProcessGroup::find_or_create(new_pgid);
    if (!process->m_pg) {
        return ENOMEM;
    }
    return 0;
}

}
