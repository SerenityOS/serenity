/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Syscall.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$yield()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    Thread::current()->yield_without_releasing_big_lock();
    return 0;
}

ErrorOr<NonnullRefPtr<Thread>> Process::get_thread_from_pid_or_tid(pid_t pid_or_tid, Syscall::SchedulerParametersMode mode)
{
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());
    Thread* peer;
    switch (mode) {
    case Syscall::SchedulerParametersMode::Thread: {
        peer = Thread::current();
        if (pid_or_tid != 0)
            peer = Thread::from_tid_in_same_process_list(pid_or_tid);

        // Only superuser can access other processes' threads.
        if (!credentials()->is_superuser() && peer && &peer->process() != this)
            return EPERM;

        break;
    }
    case Syscall::SchedulerParametersMode::Process: {
        auto* searched_process = this;
        if (pid_or_tid != 0)
            searched_process = Process::from_pid_in_same_process_list(pid_or_tid);

        if (searched_process == nullptr)
            return ESRCH;
        auto pid = searched_process->pid().value();
        // Main thread has tid == pid
        this->thread_list().for_each([&](auto& thread) {
            if (thread.tid().value() == pid)
                peer = &thread;
        });
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
    if (!peer)
        return ESRCH;
    return NonnullRefPtr<Thread> { *peer };
}

ErrorOr<FlatPtr> Process::sys$scheduler_set_parameters(Userspace<Syscall::SC_scheduler_parameters_params const*> user_param)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::proc));
    auto parameters = TRY(copy_typed_from_user(user_param));

    if (parameters.parameters.sched_priority < THREAD_PRIORITY_MIN || parameters.parameters.sched_priority > THREAD_PRIORITY_MAX)
        return EINVAL;

    SpinlockLocker lock(g_scheduler_lock);
    auto peer = TRY(get_thread_from_pid_or_tid(parameters.pid_or_tid, parameters.mode));

    auto credentials = this->credentials();
    auto peer_credentials = peer->process().credentials();
    if (!credentials->is_superuser() && credentials->euid() != peer_credentials->uid() && credentials->uid() != peer_credentials->uid())
        return EPERM;

    peer->set_priority((u32)parameters.parameters.sched_priority);
    // POSIX says that process scheduling parameters have precedence over thread scheduling parameters.
    // We don't track them separately, so overwrite the thread scheduling settings manually for now.
    if (parameters.mode == Syscall::SchedulerParametersMode::Process) {
        peer->process().for_each_thread([&](auto& thread) {
            thread.set_priority((u32)parameters.parameters.sched_priority);
        });
    }

    return 0;
}

ErrorOr<FlatPtr> Process::sys$scheduler_get_parameters(Userspace<Syscall::SC_scheduler_parameters_params*> user_param)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::proc));

    Syscall::SC_scheduler_parameters_params parameters;
    TRY(copy_from_user(&parameters, user_param));

    int priority;
    {
        SpinlockLocker lock(g_scheduler_lock);
        auto peer = TRY(get_thread_from_pid_or_tid(parameters.pid_or_tid, parameters.mode));

        auto credentials = this->credentials();
        auto peer_credentials = peer->process().credentials();
        if (!credentials->is_superuser() && credentials->euid() != peer_credentials->uid() && credentials->uid() != peer_credentials->uid())
            return EPERM;

        priority = (int)peer->priority();
    }

    parameters.parameters.sched_priority = priority;

    TRY(copy_to_user(user_param, &parameters));
    return 0;
}

}
