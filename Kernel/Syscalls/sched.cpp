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

KResultOr<int> Process::sys$yield()
{
    REQUIRE_PROMISE(stdio);
    Thread::current()->yield_without_holding_big_lock();
    return 0;
}

KResultOr<int> Process::sys$donate(pid_t tid)
{
    REQUIRE_PROMISE(stdio);
    if (tid < 0)
        return EINVAL;

    ScopedCritical critical;
    auto thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return ESRCH;
    Thread::current()->donate_without_holding_big_lock(thread, "sys$donate");
    return 0;
}

KResultOr<int> Process::sys$sched_setparam(int pid, Userspace<const struct sched_param*> user_param)
{
    REQUIRE_PROMISE(proc);
    struct sched_param desired_param;
    if (!copy_from_user(&desired_param, user_param))
        return EFAULT;

    if (desired_param.sched_priority < THREAD_PRIORITY_MIN || desired_param.sched_priority > THREAD_PRIORITY_MAX)
        return EINVAL;

    auto* peer = Thread::current();
    ScopedSpinLock lock(g_scheduler_lock);
    if (pid != 0)
        peer = Thread::from_tid(pid);

    if (!peer)
        return ESRCH;

    if (!is_superuser() && m_euid != peer->process().m_uid && m_uid != peer->process().m_uid)
        return EPERM;

    peer->set_priority((u32)desired_param.sched_priority);
    return 0;
}

KResultOr<int> Process::sys$sched_getparam(pid_t pid, Userspace<struct sched_param*> user_param)
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
            return ESRCH;

        if (!is_superuser() && m_euid != peer->process().m_uid && m_uid != peer->process().m_uid)
            return EPERM;

        priority = (int)peer->priority();
    }

    struct sched_param param {
        priority
    };
    if (!copy_to_user(user_param, &param))
        return EFAULT;
    return 0;
}

}
