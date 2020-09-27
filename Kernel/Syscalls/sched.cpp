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

int Process::sys$yield()
{
    REQUIRE_PROMISE(stdio);
    Thread::current()->yield_without_holding_big_lock();
    return 0;
}

int Process::sys$donate(pid_t tid)
{
    REQUIRE_PROMISE(stdio);
    if (tid < 0)
        return -EINVAL;

    // We don't strictly need to grab the scheduler lock here, but it
    // will close a race where we can find the thread but it disappears
    // before we call Scheduler::donate_to.
    ScopedSpinLock lock(g_scheduler_lock);
    auto thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return -ESRCH;
    Scheduler::donate_to(thread, "sys$donate");
    return 0;
}

int Process::sys$sched_setparam(int pid, Userspace<const struct sched_param*> user_param)
{
    REQUIRE_PROMISE(proc);
    struct sched_param desired_param;
    if (!copy_from_user(&desired_param, user_param))
        return -EFAULT;

    if (desired_param.sched_priority < THREAD_PRIORITY_MIN || desired_param.sched_priority > THREAD_PRIORITY_MAX)
        return -EINVAL;

    auto* peer = Thread::current();
    ScopedSpinLock lock(g_scheduler_lock);
    if (pid != 0)
        peer = Thread::from_tid(pid);

    if (!peer)
        return -ESRCH;

    if (!is_superuser() && m_euid != peer->process().m_uid && m_uid != peer->process().m_uid)
        return -EPERM;

    peer->set_priority((u32)desired_param.sched_priority);
    return 0;
}

int Process::sys$sched_getparam(pid_t pid, Userspace<struct sched_param*> user_param)
{
    REQUIRE_PROMISE(proc);
    int priority;
    {
        auto* peer = Thread::current();
        ScopedSpinLock lock(g_scheduler_lock);
        if (pid != 0) {
            // FIXME: PID/TID BUG
            // The entire process is supposed to be affected.
            peer = Thread::from_tid(pid);
        }

        if (!peer)
            return -ESRCH;

        if (!is_superuser() && m_euid != peer->process().m_uid && m_uid != peer->process().m_uid)
            return -EPERM;

        priority = (int)peer->priority();
    }

    struct sched_param param {
        priority
    };
    if (!copy_to_user(user_param, &param))
        return -EFAULT;
    return 0;
}

int Process::sys$set_thread_boost(pid_t tid, int amount)
{
    REQUIRE_PROMISE(proc);
    if (amount < 0 || amount > 20)
        return -EINVAL;
    ScopedSpinLock lock(g_scheduler_lock);
    auto thread = Thread::from_tid(tid);
    if (!thread)
        return -ESRCH;
    if (thread->state() == Thread::State::Dead || thread->state() == Thread::State::Dying)
        return -ESRCH;
    if (!is_superuser() && thread->process().uid() != euid())
        return -EPERM;
    thread->set_priority_boost(amount);
    return 0;
}

int Process::sys$set_process_boost(pid_t pid, int amount)
{
    REQUIRE_PROMISE(proc);
    if (amount < 0 || amount > 20)
        return -EINVAL;
    ScopedSpinLock lock(g_processes_lock);
    auto process = Process::from_pid(pid);
    if (!process || process->is_dead())
        return -ESRCH;
    if (!is_superuser() && process->uid() != euid())
        return -EPERM;
    process->m_priority_boost = amount;
    return 0;
}

}
