/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/InterruptDisabler.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$getsid(pid_t pid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::stdio));
    if (pid == 0)
        return sid().value();
    auto process = Process::from_pid_in_same_jail(pid);
    if (!process)
        return ESRCH;
    if (sid() != process->sid())
        return EPERM;
    return process->sid().value();
}

ErrorOr<FlatPtr> Process::sys$setsid()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::proc));
    InterruptDisabler disabler;
    bool found_process_with_same_pgid_as_my_pid = false;
    TRY(Process::for_each_in_pgrp_in_same_jail(pid().value(), [&](auto&) -> ErrorOr<void> {
        found_process_with_same_pgid_as_my_pid = true;
        return {};
    }));
    if (found_process_with_same_pgid_as_my_pid)
        return EPERM;
    // Create a new Session and a new ProcessGroup.

    m_pg = TRY(ProcessGroup::try_create(ProcessGroupID(pid().value())));
    m_tty = nullptr;
    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        protected_data.sid = pid().value();
        return protected_data.sid.value();
    });
}

ErrorOr<FlatPtr> Process::sys$getpgid(pid_t pid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::stdio));
    if (pid == 0)
        return pgid().value();
    auto process = Process::from_pid_in_same_jail(pid);
    if (!process)
        return ESRCH;
    return process->pgid().value();
}

ErrorOr<FlatPtr> Process::sys$getpgrp()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::stdio));
    return pgid().value();
}

SessionID Process::get_sid_from_pgid(ProcessGroupID pgid)
{
    // FIXME: This xor sys$setsid() uses the wrong locking mechanism.

    SessionID sid { -1 };
    MUST(Process::current().for_each_in_pgrp_in_same_jail(pgid, [&](auto& process) -> ErrorOr<void> {
        sid = process.sid();
        return {};
    }));

    return sid;
}

ErrorOr<FlatPtr> Process::sys$setpgid(pid_t specified_pid, pid_t specified_pgid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::proc));
    ProcessID pid = specified_pid ? ProcessID(specified_pid) : this->pid();
    if (specified_pgid < 0) {
        // The value of the pgid argument is less than 0, or is not a value supported by the implementation.
        return EINVAL;
    }
    auto process = Process::from_pid_in_same_jail(pid);
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
    process->m_pg = TRY(ProcessGroup::try_find_or_create(new_pgid));
    return with_mutable_protected_data([&](auto& protected_data) -> ErrorOr<FlatPtr> {
        auto credentials = this->credentials();

        auto new_credentials = TRY(Credentials::create(
            credentials->uid(),
            credentials->gid(),
            credentials->euid(),
            credentials->egid(),
            credentials->suid(),
            credentials->sgid(),
            credentials->extra_gids(),
            new_sid,
            new_pgid));

        protected_data.credentials = move(new_credentials);
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$get_root_session_id(pid_t force_sid)
{
    TRY(require_promise(Pledge::stdio));
    pid_t sid = (force_sid == -1) ? this->sid().value() : force_sid;
    if (sid == 0)
        return 0;
    while (true) {
        auto sid_process = Process::from_pid_in_same_jail(sid);
        if (!sid_process)
            return ESRCH;
        auto parent_pid = sid_process->ppid().value();
        auto parent_process = Process::from_pid_in_same_jail(parent_pid);
        if (!parent_process)
            return ESRCH;
        pid_t parent_sid = parent_process->sid().value();
        if (parent_sid == 0)
            break;
        sid = parent_sid;
    }
    return sid;
}

}
