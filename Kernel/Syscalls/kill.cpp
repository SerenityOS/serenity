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

namespace Kernel {

KResult Process::do_kill(Process& process, int signal)
{
    // FIXME: Allow sending SIGCONT to everyone in the process group.
    // FIXME: Should setuid processes have some special treatment here?
    if (!is_superuser() && m_euid != process.m_uid && m_uid != process.m_uid)
        return KResult(-EPERM);
    if (process.is_kernel_process() && signal == SIGKILL) {
        klog() << "attempted to send SIGKILL to kernel process " << process.name().characters() << "(" << process.pid().value() << ")";
        return KResult(-EPERM);
    }
    if (signal != 0)
        return process.send_signal(signal, this);
    return KSuccess;
}

KResult Process::do_killpg(ProcessGroupID pgrp, int signal)
{
    InterruptDisabler disabler;

    ASSERT(pgrp >= 0);

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

        return IterationDecision::Continue;
    });

    if (group_was_empty)
        return KResult(-ESRCH);
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
        if (process.pid() == m_pid)
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
    if (!current_thread->should_ignore_signal(signal)) {
        current_thread->send_signal(signal, this);
        (void)current_thread->block<Thread::SemiPermanentBlocker>(nullptr, Thread::SemiPermanentBlocker::Reason::Signal);
    }

    return KSuccess;
}

int Process::sys$kill(pid_t pid_or_pgid, int signal)
{
    if (pid_or_pgid == m_pid.value())
        REQUIRE_PROMISE(stdio);
    else
        REQUIRE_PROMISE(proc);

    if (signal < 0 || signal >= 32)
        return -EINVAL;
    if (pid_or_pgid < -1) {
        if (pid_or_pgid == NumericLimits<i32>::min())
            return -EINVAL;
        return do_killpg(-pid_or_pgid, signal);
    }
    if (pid_or_pgid == -1)
        return do_killall(signal);
    if (pid_or_pgid == m_pid.value()) {
        return do_killself(signal);
    }
    ASSERT(pid_or_pgid >= 0);
    ScopedSpinLock lock(g_processes_lock);
    auto peer = Process::from_pid(pid_or_pgid);
    if (!peer)
        return -ESRCH;
    return do_kill(*peer, signal);
}

int Process::sys$killpg(pid_t pgrp, int signum)
{
    REQUIRE_PROMISE(proc);
    if (signum < 1 || signum >= 32)
        return -EINVAL;
    if (pgrp < 0)
        return -EINVAL;

    return do_killpg(pgrp, signum);
}

}
