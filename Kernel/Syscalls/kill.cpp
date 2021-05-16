/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

KResult Process::do_kill(Process& process, int signal)
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
    return KSuccess;
}

KResult Process::do_killpg(ProcessGroupID pgrp, int signal)
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
    KResult error = KSuccess;

    Process::for_each_in_pgrp(pgrp, [&](auto& process) {
        group_was_empty = false;

        KResult res = do_kill(process, signal);
        if (res.is_success())
            any_succeeded = true;
        else
            error = res;
    });

    if (group_was_empty)
        return ESRCH;
    if (any_succeeded)
        return KSuccess;
    return error;
}

KResult Process::do_killall(int signal)
{
    InterruptDisabler disabler;

    bool any_succeeded = false;
    KResult error = KSuccess;

    // Send the signal to all processes we have access to for.
    ScopedSpinLock lock(g_processes_lock);
    for (auto& process : *g_processes) {
        KResult res = KSuccess;
        if (process.pid() == pid())
            res = do_killself(signal);
        else
            res = do_kill(process, signal);

        if (res.is_success())
            any_succeeded = true;
        else
            error = res;
    }

    if (any_succeeded)
        return KSuccess;
    return error;
}

KResult Process::do_killself(int signal)
{
    if (signal == 0)
        return KSuccess;

    auto current_thread = Thread::current();
    if (!current_thread->should_ignore_signal(signal))
        current_thread->send_signal(signal, this);

    return KSuccess;
}

KResultOr<int> Process::sys$kill(pid_t pid_or_pgid, int signal)
{
    if (pid_or_pgid == pid().value())
        REQUIRE_PROMISE(stdio);
    else
        REQUIRE_PROMISE(proc);

    if (signal < 0 || signal >= 32)
        return EINVAL;
    if (pid_or_pgid < -1) {
        if (pid_or_pgid == NumericLimits<i32>::min())
            return EINVAL;
        return do_killpg(-pid_or_pgid, signal);
    }
    if (pid_or_pgid == -1)
        return do_killall(signal);
    if (pid_or_pgid == pid().value()) {
        return do_killself(signal);
    }
    VERIFY(pid_or_pgid >= 0);
    ScopedSpinLock lock(g_processes_lock);
    auto peer = Process::from_pid(pid_or_pgid);
    if (!peer)
        return ESRCH;
    return do_kill(*peer, signal);
}

KResultOr<int> Process::sys$killpg(pid_t pgrp, int signum)
{
    REQUIRE_PROMISE(proc);
    if (signum < 1 || signum >= 32)
        return EINVAL;
    if (pgrp < 0)
        return EINVAL;

    return do_killpg(pgrp, signum);
}

}
