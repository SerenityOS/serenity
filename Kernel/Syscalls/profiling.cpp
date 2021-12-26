/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/CoreDump.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>

namespace Kernel {

PerformanceEventBuffer* g_global_perf_events;
bool g_profiling_all_threads;

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

}
