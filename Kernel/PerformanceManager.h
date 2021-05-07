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

    inline static void add_cpu_sample_event(Thread& current_thread, const RegisterState& regs)
    {
        PerformanceEventBuffer* perf_events = nullptr;

        if (g_profiling_all_threads) {
            VERIFY(g_global_perf_events);
            // FIXME: We currently don't collect samples while idle.
            //        That will be an interesting mode to add in the future. :^)
            if (&current_thread != Processor::current().idle_thread()) {
                perf_events = g_global_perf_events;
            }
        } else if (current_thread.process().is_profiling()) {
            VERIFY(current_thread.process().perf_events());
            perf_events = current_thread.process().perf_events();
        }

        if (perf_events) {
            [[maybe_unused]] auto rc = perf_events->append_with_eip_and_ebp(
                current_thread.pid(), current_thread.tid(),
                regs.eip, regs.ebp, PERF_EVENT_SAMPLE, 0, 0, nullptr);
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
