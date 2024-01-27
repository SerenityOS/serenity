/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/Coredump.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

bool g_profiling_all_threads;
PerformanceEventBuffer* g_global_perf_events;
u64 g_profiling_event_mask;

ErrorOr<FlatPtr> Process::sys$profiling_enable(pid_t pid, u64 event_mask)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_no_promises());

    return profiling_enable(pid, event_mask);
}

// NOTE: This second entrypoint exists to allow the kernel to invoke the syscall to enable boot profiling.
ErrorOr<FlatPtr> Process::profiling_enable(pid_t pid, u64 event_mask)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);

    if (pid == -1) {
        auto credentials = this->credentials();
        if (!credentials->is_superuser())
            return EPERM;
        ScopedCritical critical;
        g_profiling_event_mask = PERF_EVENT_PROCESS_CREATE | PERF_EVENT_THREAD_CREATE | PERF_EVENT_MMAP;
        if (g_global_perf_events) {
            g_global_perf_events->clear();
        } else {
            g_global_perf_events = PerformanceEventBuffer::try_create_with_size(32 * MiB).leak_ptr();
            if (!g_global_perf_events) {
                g_profiling_event_mask = 0;
                return ENOMEM;
            }
        }

        SpinlockLocker lock(g_profiling_lock);
        if (!TimeManagement::the().enable_profile_timer())
            return ENOTSUP;
        g_profiling_all_threads = true;
        PerformanceManager::add_process_created_event(*Scheduler::colonel());
        TRY(Process::for_each_in_same_process_list([](auto& process) -> ErrorOr<void> {
            PerformanceManager::add_process_created_event(process);
            return {};
        }));
        g_profiling_event_mask = event_mask;
        return 0;
    }

    auto process = Process::from_pid_in_same_process_list(pid);
    if (!process)
        return ESRCH;
    if (process->is_dead())
        return ESRCH;
    auto credentials = this->credentials();
    auto profile_process_credentials = process->credentials();
    if (!credentials->is_superuser() && profile_process_credentials->uid() != credentials->euid())
        return EPERM;
    SpinlockLocker lock(g_profiling_lock);
    g_profiling_event_mask = PERF_EVENT_PROCESS_CREATE | PERF_EVENT_THREAD_CREATE | PERF_EVENT_MMAP;
    process->set_profiling(true);
    if (!process->create_perf_events_buffer_if_needed()) {
        process->set_profiling(false);
        return ENOMEM;
    }
    g_profiling_event_mask = event_mask;
    if (!TimeManagement::the().enable_profile_timer()) {
        process->set_profiling(false);
        return ENOTSUP;
    }
    return 0;
}

ErrorOr<FlatPtr> Process::sys$profiling_disable(pid_t pid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_no_promises());

    if (pid == -1) {
        auto credentials = this->credentials();
        if (!credentials->is_superuser())
            return EPERM;
        ScopedCritical critical;
        if (!TimeManagement::the().disable_profile_timer())
            return ENOTSUP;
        g_profiling_all_threads = false;
        return 0;
    }

    auto process = Process::from_pid_in_same_process_list(pid);
    if (!process)
        return ESRCH;
    auto credentials = this->credentials();
    auto profile_process_credentials = process->credentials();
    if (!credentials->is_superuser() && profile_process_credentials->uid() != credentials->euid())
        return EPERM;
    SpinlockLocker lock(g_profiling_lock);
    if (!process->is_profiling())
        return EINVAL;
    // FIXME: If we enabled the profile timer and it's not supported, how do we disable it now?
    if (!TimeManagement::the().disable_profile_timer())
        return ENOTSUP;
    process->set_profiling(false);
    return 0;
}

ErrorOr<FlatPtr> Process::sys$profiling_free_buffer(pid_t pid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_no_promises());

    if (pid == -1) {
        auto credentials = this->credentials();
        if (!credentials->is_superuser())
            return EPERM;

        OwnPtr<PerformanceEventBuffer> perf_events;

        {
            ScopedCritical critical;

            perf_events = adopt_own_if_nonnull(g_global_perf_events);
            g_global_perf_events = nullptr;
        }

        return 0;
    }

    auto process = Process::from_pid_in_same_process_list(pid);
    if (!process)
        return ESRCH;
    auto credentials = this->credentials();
    auto profile_process_credentials = process->credentials();
    if (!credentials->is_superuser() && profile_process_credentials->uid() != credentials->euid())
        return EPERM;
    SpinlockLocker lock(g_profiling_lock);
    if (process->is_profiling())
        return EINVAL;
    process->delete_perf_events_buffer();
    return 0;
}
}
