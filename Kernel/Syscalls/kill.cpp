/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<void> Process::do_kill(Process& process, int signal)
{
    // FIXME: Should setuid processes have some special treatment here?
    auto credentials = this->credentials();
    auto kill_process_credentials = process.credentials();

    bool can_send_signal = credentials->is_superuser()
        || credentials->euid() == kill_process_credentials->uid()
        || credentials->uid() == kill_process_credentials->uid()
        || (signal == SIGCONT && credentials->pgid() == kill_process_credentials->pgid());
    if (!can_send_signal)
        return EPERM;
    if (process.is_kernel_process()) {
        process.name().with([&](auto& process_name) {
            dbgln("Attempted to send signal {} to kernel process {} ({})", signal, process_name.representable_view(), process.pid());
        });
        return EPERM;
    }
    if (signal != 0)
        return process.send_signal(signal, this);
    return {};
}

ErrorOr<void> Process::do_killpg(ProcessGroupID pgrp, int signal)
{
    VERIFY(pgrp >= 0);

    // Send the signal to all processes in the given group.
    if (pgrp == 0) {
        // Send the signal to our own pgrp.
        pgrp = pgid();
    }

    bool group_was_empty = true;
    bool any_succeeded = false;
    ErrorOr<void> error;

    TRY(Process::current().for_each_in_pgrp_in_same_process_list(pgrp, [&](auto& process) -> ErrorOr<void> {
        group_was_empty = false;

        ErrorOr<void> res = do_kill(process, signal);
        if (!res.is_error())
            any_succeeded = true;
        else
            error = move(res);
        return {};
    }));

    if (group_was_empty)
        return ESRCH;
    if (any_succeeded)
        return {};
    return error;
}

ErrorOr<void> Process::do_killall(int signal)
{
    bool any_succeeded = false;
    ErrorOr<void> error;

    // Send the signal to all processes we have access to for.
    TRY(Process::for_each_in_same_process_list([&](auto& process) -> ErrorOr<void> {
        ErrorOr<void> res;
        if (process.pid() == pid())
            res = do_killself(signal);
        else
            res = do_kill(process, signal);

        if (!res.is_error())
            any_succeeded = true;
        else
            error = move(res);
        return {};
    }));

    if (any_succeeded)
        return {};
    return error;
}

ErrorOr<void> Process::do_killself(int signal)
{
    if (signal == 0)
        return {};

    auto* current_thread = Thread::current();
    if (!current_thread->should_ignore_signal(signal))
        current_thread->send_signal(signal, this);

    return {};
}

ErrorOr<FlatPtr> Process::sys$kill(pid_t pid_or_pgid, int signal)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    if (pid_or_pgid == pid().value())
        TRY(require_promise(Pledge::stdio));
    else
        TRY(require_promise(Pledge::proc));

    if (signal < 0 || signal >= 32)
        return EINVAL;
    if (pid_or_pgid < -1) {
        if (pid_or_pgid == NumericLimits<i32>::min())
            return EINVAL;
        TRY(do_killpg(-pid_or_pgid, signal));
        return 0;
    }
    if (pid_or_pgid == -1) {
        TRY(do_killall(signal));
        return 0;
    }
    if (pid_or_pgid == pid().value()) {
        TRY(do_killself(signal));
        return 0;
    }
    VERIFY(pid_or_pgid >= 0);
    auto peer = Process::from_pid_in_same_process_list(pid_or_pgid);
    if (!peer)
        return ESRCH;
    TRY(do_kill(*peer, signal));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$killpg(pid_t pgrp, int signum)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::proc));
    if (signum < 1 || signum >= 32)
        return EINVAL;
    if (pgrp < 0)
        return EINVAL;

    TRY(do_killpg(pgrp, signum));
    return 0;
}

}
