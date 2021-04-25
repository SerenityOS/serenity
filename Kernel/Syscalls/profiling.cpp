/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/CoreDump.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>

namespace Kernel {

bool g_profiling_all_threads;
PerformanceEventBuffer* g_global_perf_events;

KResultOr<int> Process::sys$profiling_enable(pid_t pid)
{
    REQUIRE_NO_PROMISES;

    if (pid == -1) {
        if (!is_superuser())
            return EPERM;
        ScopedCritical critical;
        if (g_global_perf_events)
            g_global_perf_events->clear();
        else
            g_global_perf_events = PerformanceEventBuffer::try_create_with_size(32 * MiB).leak_ptr();
        ScopedSpinLock lock(g_processes_lock);
        Process::for_each([](auto& process) {
            g_global_perf_events->add_process(process, ProcessEventType::Create);
            return IterationDecision::Continue;
        });
        g_profiling_all_threads = true;
        return 0;
    }

    ScopedSpinLock lock(g_processes_lock);
    auto process = Process::from_pid(pid);
    if (!process)
        return ESRCH;
    if (process->is_dead())
        return ESRCH;
    if (!is_superuser() && process->uid() != euid())
        return EPERM;
    if (!process->create_perf_events_buffer_if_needed())
        return ENOMEM;
    process->set_profiling(true);
    return 0;
}

KResultOr<int> Process::sys$profiling_disable(pid_t pid)
{
    REQUIRE_NO_PROMISES;

    if (pid == -1) {
        if (!is_superuser())
            return EPERM;
        ScopedCritical critical;
        g_profiling_all_threads = false;
        return 0;
    }

    ScopedSpinLock lock(g_processes_lock);
    auto process = Process::from_pid(pid);
    if (!process)
        return ESRCH;
    if (!is_superuser() && process->uid() != euid())
        return EPERM;
    if (!process->is_profiling())
        return EINVAL;
    process->set_profiling(false);
    return 0;
}

KResultOr<int> Process::sys$profiling_free_buffer(pid_t pid)
{
    REQUIRE_NO_PROMISES;

    if (pid == -1) {
        if (!is_superuser())
            return EPERM;

        OwnPtr<PerformanceEventBuffer> perf_events;

        {
            ScopedCritical critical;

            perf_events = g_global_perf_events;
            g_global_perf_events = nullptr;
        }

        return 0;
    }

    ScopedSpinLock lock(g_processes_lock);
    auto process = Process::from_pid(pid);
    if (!process)
        return ESRCH;
    if (!is_superuser() && process->uid() != euid())
        return EPERM;
    if (process->is_profiling())
        return EINVAL;
    process->delete_perf_events_buffer();
    return 0;
}
}
