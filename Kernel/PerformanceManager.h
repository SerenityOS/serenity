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
            [[maybe_unused]] auto rc = g_global_perf_events->append_with_ip_and_bp(
                process.pid(), 0, 0, 0, PERF_EVENT_PROCESS_EXIT, 0, 0, 0, nullptr);
        }
    }

    inline static void add_thread_created_event(Thread& thread)
    {
        if (thread.is_profiling_suppressed())
            return;
        if (auto* event_buffer = thread.process().current_perf_events_buffer()) {
            [[maybe_unused]] auto rc = event_buffer->append(PERF_EVENT_THREAD_CREATE, thread.tid().value(), 0, nullptr, &thread);
        }
    }

    inline static void add_thread_exit_event(Thread& thread)
    {
        // As an exception this doesn't check whether profiling is suppressed for
        // the thread so we can record the thread_exit event anyway.
        if (auto* event_buffer = thread.process().current_perf_events_buffer()) {
            [[maybe_unused]] auto rc = event_buffer->append(PERF_EVENT_THREAD_EXIT, thread.tid().value(), 0, nullptr, &thread);
        }
    }

    inline static void add_cpu_sample_event(Thread& current_thread, const RegisterState& regs, u32 lost_time)
    {
        if (current_thread.is_profiling_suppressed())
            return;
        if (auto* event_buffer = current_thread.process().current_perf_events_buffer()) {
            [[maybe_unused]] auto rc = event_buffer->append_with_ip_and_bp(
                current_thread.pid(), current_thread.tid(), regs, PERF_EVENT_SAMPLE, lost_time, 0, 0, nullptr);
        }
    }

    inline static void add_mmap_perf_event(Process& current_process, Memory::Region const& region)
    {
        if (auto* event_buffer = current_process.current_perf_events_buffer()) {
            [[maybe_unused]] auto res = event_buffer->append(PERF_EVENT_MMAP, region.vaddr().get(), region.size(), region.name());
        }
    }

    inline static void add_unmap_perf_event(Process& current_process, Memory::VirtualRange const& region)
    {
        if (auto* event_buffer = current_process.current_perf_events_buffer()) {
            [[maybe_unused]] auto res = event_buffer->append(PERF_EVENT_MUNMAP, region.base().get(), region.size(), nullptr);
        }
    }

    inline static void add_context_switch_perf_event(Thread& current_thread, Thread& next_thread)
    {
        if (current_thread.is_profiling_suppressed())
            return;
        if (auto* event_buffer = current_thread.process().current_perf_events_buffer()) {
            [[maybe_unused]] auto res = event_buffer->append(PERF_EVENT_CONTEXT_SWITCH, next_thread.pid().value(), next_thread.tid().value(), nullptr);
        }
    }

    inline static void add_kmalloc_perf_event(Thread& current_thread, size_t size, FlatPtr ptr)
    {
        if (current_thread.is_profiling_suppressed())
            return;
        if (auto* event_buffer = current_thread.process().current_perf_events_buffer()) {
            [[maybe_unused]] auto res = event_buffer->append(PERF_EVENT_KMALLOC, size, ptr, nullptr);
        }
    }

    inline static void add_kfree_perf_event(Thread& current_thread, size_t size, FlatPtr ptr)
    {
        if (current_thread.is_profiling_suppressed())
            return;
        if (auto* event_buffer = current_thread.process().current_perf_events_buffer()) {
            [[maybe_unused]] auto res = event_buffer->append(PERF_EVENT_KFREE, size, ptr, nullptr);
        }
    }

    inline static void add_page_fault_event(Thread& thread, const RegisterState& regs)
    {
        if (thread.is_profiling_suppressed())
            return;
        if (auto* event_buffer = thread.process().current_perf_events_buffer()) {
            [[maybe_unused]] auto rc = event_buffer->append_with_ip_and_bp(
                thread.pid(), thread.tid(), regs, PERF_EVENT_PAGE_FAULT, 0, 0, 0, nullptr);
        }
    }

    inline static void add_syscall_event(Thread& thread, const RegisterState& regs)
    {
        if (thread.is_profiling_suppressed())
            return;
        if (auto* event_buffer = thread.process().current_perf_events_buffer()) {
            [[maybe_unused]] auto rc = event_buffer->append_with_ip_and_bp(
                thread.pid(), thread.tid(), regs, PERF_EVENT_SYSCALL, 0, 0, 0, nullptr);
        }
    }

    inline static void timer_tick(RegisterState const& regs)
    {
        static Time last_wakeup;
        auto now = kgettimeofday();
        constexpr auto ideal_interval = Time::from_microseconds(1000'000 / OPTIMAL_PROFILE_TICKS_PER_SECOND_RATE);
        auto expected_wakeup = last_wakeup + ideal_interval;
        auto delay = (now > expected_wakeup) ? now - expected_wakeup : Time::from_microseconds(0);
        last_wakeup = now;
        auto* current_thread = Thread::current();
        // FIXME: We currently don't collect samples while idle.
        //        That will be an interesting mode to add in the future. :^)
        if (!current_thread || current_thread == Processor::idle_thread())
            return;

        auto lost_samples = delay.to_microseconds() / ideal_interval.to_microseconds();
        PerformanceManager::add_cpu_sample_event(*current_thread, regs, lost_samples);
    }
};

}
