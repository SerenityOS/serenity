/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/InterruptDisabler.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<void> Process::do_kill(Process& process, int signal)
{
    // FIXME: Allow sending SIGCONT to everyone in the process group.
    // FIXME: Should setuid processes have some special treatment here?
    if (!is_superuser() && euid() != process.uid() && uid() != process.uid())
        return EPERM;
    if (process.is_kernel_process()) {
        dbgln("Attempted to send signal {} to kernel process {} ({})", signal, process.name(), process.pid());
        return EPERM;
    }
    if (signal != 0)
        return process.send_signal(signal, this);
    return {};
}

ErrorOr<void> Process::do_killpg(ProcessGroupID pgrp, int signal)
{
    InterruptDisabler disabler;

    VERIFY(pgrp >= 0);

    // Send the signal to all processes in the given group.
    if (pgrp == 0) {
        // Send the signal to our own pgrp.
        pgrp = pgid();
    }

    bool group_was_empty = true;
    bool any_succeeded = false;
    ErrorOr<void> error;

    Process::for_each_in_pgrp(pgrp, [&](auto& process) {
        group_was_empty = false;

        ErrorOr<void> res = do_kill(process, signal);
        if (!res.is_error())
            any_succeeded = true;
        else
            error = move(res);
    });

    if (group_was_empty)
        return ESRCH;
    if (any_succeeded)
        return {};
    return error;
}

ErrorOr<void> Process::do_killall(int signal)
{
    InterruptDisabler disabler;

    bool any_succeeded = false;
    ErrorOr<void> error;

    // Send the signal to all processes we have access to for.
    processes().for_each([&](auto& process) {
        ErrorOr<void> res;
        if (process.pid() == pid())
            res = do_killself(signal);
        else
            res = do_kill(process, signal);

        if (!res.is_error())
            any_succeeded = true;
        else
            error = move(res);
    });

    if (any_succeeded)
        return {};
    return error;
}

ErrorOr<void> Process::do_killself(int signal)
{
    if (signal == 0)
        return {};

    auto current_thread = Thread::current();
    if (!current_thread->should_ignore_signal(signal))
        current_thread->send_signal(signal, this);

    return {};
}

ErrorOr<FlatPtr> Process::sys$kill(pid_t pid_or_pgid, int signal)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    if (pid_or_pgid == pid().value())
        REQUIRE_PROMISE(stdio);
    else
        REQUIRE_PROMISE(proc);

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
    auto peer = Process::from_pid(pid_or_pgid);
    if (!peer)
        return ESRCH;
    TRY(do_kill(*peer, signal));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$killpg(pid_t pgrp, int signum)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(proc);
    if (signum < 1 || signum >= 32)
        return EINVAL;
    if (pgrp < 0)
        return EINVAL;

    TRY(do_killpg(pgrp, signum));
    return 0;
}

}
