/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Devices/KCOVDevice.h>

extern bool g_in_early_boot;


// Set ENABLE_KERNEL_COVERAGE_COLLECTION=ON via cmake, to inject this function on every program edge.
// Note: This function is only used by fuzzing builds. When in use, it becomes an ultra hot code path.
// See https://clang.llvm.org/docs/SanitizerCoverage.html#edge-coverage
extern "C" void __sanitizer_cov_trace_pc(void);
extern "C" void __sanitizer_cov_trace_pc(void)
{
    if (g_in_early_boot) [[unlikely]]
        return;

    auto* thread = Processor::current_thread();
    auto* kcov_instance = thread->process().kcov_instance();
    if (kcov_instance == nullptr || !thread->m_kcov_enabled) [[likely]]
        return; // KCOV device hasn't been opened yet or thread is not traced

    if (Processor::current_in_irq()) [[unlikely]] {
        // Do not collect coverage caused by interrupts. We want the collected coverage to be a function
        // of the syscalls executed by the fuzzer. Interrupts can occur more or less randomly. Fuzzers
        // uses coverage to identify function call sequences, which triggered new code paths. If the
        // coverage is noisy, the fuzzer will waste time on unintersting sequences.
        return;
    }

    kcov_instance->buffer_add_pc((u64)__builtin_return_address(0));
    return;
}
