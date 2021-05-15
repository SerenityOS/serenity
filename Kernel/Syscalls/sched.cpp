/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    if (!is_superuser() && euid() != peer->process().uid() && uid() != peer->process().uid())
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

        if (!is_superuser() && euid() != peer->process().uid() && uid() != peer->process().uid())
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
