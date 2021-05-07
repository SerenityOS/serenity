/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

namespace Kernel {

class PerformanceManager {
public:
    inline static void add_process_created_event(Process& process)
    {
        if (g_profiling_all_threads) {
            VERIFY(g_global_perf_events);
            g_global_perf_events->add_process(process, ProcessEventType::Create);
        }
    }

    inline static void add_process_exec_event(Process& process)
    {
        if (auto* event_buffer = process.current_perf_events_buffer()) {
            event_buffer->add_process(process, ProcessEventType::Exec);
        }
    }

    inline static void add_process_exit_event(Process& process)
    {
        if (g_profiling_all_threads) {
            VERIFY(g_global_perf_events);
            [[maybe_unused]] auto rc = g_global_perf_events->append_with_eip_and_ebp(
                process.pid(), 0, 0, 0, PERF_EVENT_PROCESS_EXIT, 0, 0, nullptr);
        }
    }

    inline static void add_thread_created_event(Thread& thread)
    {
        if (auto* event_buffer = thread.process().current_perf_events_buffer()) {
            [[maybe_unused]] auto rc = event_buffer->append(PERF_EVENT_THREAD_CREATE, thread.tid().value(), 0, nullptr, &thread);
        }
    }

    inline static void add_thread_exit_event(Thread& thread)
    {
        if (auto* event_buffer = thread.process().current_perf_events_buffer()) {
            [[maybe_unused]] auto rc = event_buffer->append(PERF_EVENT_THREAD_EXIT, thread.tid().value(), 0, nullptr, &thread);
        }
    }

    inline static void add_mmap_perf_event(Process& current_process, Region const& region)
    {
        if (auto* event_buffer = current_process.current_perf_events_buffer()) {
            [[maybe_unused]] auto res = event_buffer->append(PERF_EVENT_MMAP, region.vaddr().get(), region.size(), region.name());
        }
    }

    inline static void add_unmap_perf_event(Process& current_process, Range const& region)
    {
        if (auto* event_buffer = current_process.current_perf_events_buffer()) {
            [[maybe_unused]] auto res = event_buffer->append(PERF_EVENT_MUNMAP, region.base().get(), region.size(), nullptr);
        }
    }
};

}
