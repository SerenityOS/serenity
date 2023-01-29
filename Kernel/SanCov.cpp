/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/KCOVDevice.h>
#include <Kernel/Thread.h>

extern bool g_in_early_boot;

extern "C" {

void __sanitizer_cov_trace_pc(void);
void __sanitizer_cov_trace_pc(void)
{
    if (g_in_early_boot) [[unlikely]]
        return;

    if (Processor::current_in_irq()) [[unlikely]] {
        // Do not trace in interrupts.
        return;
    }

    auto const* thread = Thread::current();
    auto tid = thread->tid();
    auto maybe_kcov_instance = KCOVDevice::thread_instance->get(tid);
    if (!maybe_kcov_instance.has_value()) [[likely]] {
        // not traced
        return;
    }
    auto* kcov_instance = maybe_kcov_instance.value();
    if (kcov_instance->state() < KCOVInstance::TRACING) [[likely]]
        return;
    kcov_instance->buffer_add_pc((u64)__builtin_return_address(0));
}
}
