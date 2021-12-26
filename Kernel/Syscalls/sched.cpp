/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$yield()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    REQUIRE_PROMISE(stdio);
    Thread::current()->yield_assuming_not_holding_big_lock();
    return 0;
}

KResultOr<FlatPtr> Process::sys$sched_setparam(int pid, Userspace<const struct sched_param*> user_param)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
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

KResultOr<FlatPtr> Process::sys$sched_getparam(pid_t pid, Userspace<struct sched_param*> user_param)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
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
