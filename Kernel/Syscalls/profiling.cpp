/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/CoreDump.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

bool g_profiling_all_threads;
PerformanceEventBuffer* g_global_perf_events;
u64 g_profiling_event_mask;

KResultOr<FlatPtr> Process::sys$profiling_enable(pid_t pid, u64 event_mask)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_NO_PROMISES;

    if (pid == -1) {
        if (!is_superuser())
            return EPERM;
        ScopedCritical critical;
        g_profiling_event_mask = PERF_EVENT_PROCESS_CREATE | PERF_EVENT_THREAD_CREATE | PERF_EVENT_MMAP;
        if (g_global_perf_events)
            g_global_perf_events->clear();
        else
            g_global_perf_events = PerformanceEventBuffer::try_create_with_size(32 * MiB).leak_ptr();

        ScopedSpinlock lock(g_profiling_lock);
        if (!TimeManagement::the().enable_profile_timer())
            return ENOTSUP;
        g_profiling_all_threads = true;
        PerformanceManager::add_process_created_event(*Scheduler::colonel());
        Process::for_each([](auto& process) {
            PerformanceManager::add_process_created_event(process);
            return IterationDecision::Continue;
        });
        g_profiling_event_mask = event_mask;
        return 0;
    }

    auto process = Process::from_pid(pid);
    if (!process)
        return ESRCH;
    if (process->is_dead())
        return ESRCH;
    if (!is_superuser() && process->uid() != euid())
        return EPERM;
    ScopedSpinlock lock(g_profiling_lock);
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

KResultOr<FlatPtr> Process::sys$profiling_disable(pid_t pid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_NO_PROMISES;

    if (pid == -1) {
        if (!is_superuser())
            return EPERM;
        ScopedCritical critical;
        if (!TimeManagement::the().disable_profile_timer())
            return ENOTSUP;
        g_profiling_all_threads = false;
        return 0;
    }

    auto process = Process::from_pid(pid);
    if (!process)
        return ESRCH;
    if (!is_superuser() && process->uid() != euid())
        return EPERM;
    ScopedSpinlock lock(g_profiling_lock);
    if (!process->is_profiling())
        return EINVAL;
    // FIXME: If we enabled the profile timer and it's not supported, how do we disable it now?
    if (!TimeManagement::the().disable_profile_timer())
        return ENOTSUP;
    process->set_profiling(false);
    return 0;
}

KResultOr<FlatPtr> Process::sys$profiling_free_buffer(pid_t pid)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_NO_PROMISES;

    if (pid == -1) {
        if (!is_superuser())
            return EPERM;

        OwnPtr<PerformanceEventBuffer> perf_events;

        {
            ScopedCritical critical;

            perf_events = adopt_own_if_nonnull(g_global_perf_events);
            g_global_perf_events = nullptr;
        }

        return 0;
    }

    auto process = Process::from_pid(pid);
    if (!process)
        return ESRCH;
    if (!is_superuser() && process->uid() != euid())
        return EPERM;
    ScopedSpinlock lock(g_profiling_lock);
    if (process->is_profiling())
        return EINVAL;
    process->delete_perf_events_buffer();
    return 0;
}
}
