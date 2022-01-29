/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$yield()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    Thread::current()->yield_without_releasing_big_lock();
    return 0;
}

ErrorOr<FlatPtr> Process::sys$sched_setparam(int pid, Userspace<const struct sched_param*> user_param)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::proc));
    auto param = TRY(copy_typed_from_user(user_param));

    if (param.sched_priority < THREAD_PRIORITY_MIN || param.sched_priority > THREAD_PRIORITY_MAX)
        return EINVAL;

    auto* peer = Thread::current();
    SpinlockLocker lock(g_scheduler_lock);
    if (pid != 0)
        peer = Thread::from_tid(pid);

    if (!peer)
        return ESRCH;

    if (!is_superuser() && euid() != peer->process().uid() && uid() != peer->process().uid())
        return EPERM;

    peer->set_priority((u32)param.sched_priority);
    return 0;
}

ErrorOr<FlatPtr> Process::sys$sched_getparam(pid_t pid, Userspace<struct sched_param*> user_param)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    TRY(require_promise(Pledge::proc));
    int priority;
    {
        auto* peer = Thread::current();
        SpinlockLocker lock(g_scheduler_lock);
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

    TRY(copy_to_user(user_param, &param));
    return 0;
}

}
