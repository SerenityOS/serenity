/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <AK/SetOnce.h>
#include <AK/TemporaryChange.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Devices/KCOVDevice.h>
#include <Kernel/Library/Panic.h>

extern SetOnce g_not_in_early_boot;

#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION_DEBUG
// Set kcov_emergency_off=true before making calls from __sanitizer_cov_trace_pc to coverage
// instrumented code, in order to prevent an infinite recursion.
// Any code reachable from the non-failure path in __sanitizer_cov_trace_pc must not be
// coverage instrumented. However, once a fatal error was detected, crash_and_burn will use
// a bunch of extra code to print useful debugging information. It would be wasteful not to
// instrument all of that code, so kcov_emergency_off=true can be used to bail out from
// recursive __sanitizer_cov_trace_pc calls while inside crash_and_burn.
bool kcov_emergency_off { false };
static void crash_and_burn(Thread* thread)
{
    kcov_emergency_off = true;
    thread->print_backtrace();
    PANIC("KCOV is b0rked.");
    VERIFY_NOT_REACHED();
}
#endif

// Set ENABLE_KERNEL_COVERAGE_COLLECTION=ON via cmake, to inject this function on every program edge.
// Note: This function is only used by fuzzing builds. When in use, it becomes an ultra hot code path.
// See https://clang.llvm.org/docs/SanitizerCoverage.html#edge-coverage
extern "C" void __sanitizer_cov_trace_pc(void);
extern "C" void __sanitizer_cov_trace_pc(void)
{
    if (!g_not_in_early_boot.was_set()) [[unlikely]]
        return;

    auto* thread = Processor::current_thread();

#ifdef ENABLE_KERNEL_COVERAGE_COLLECTION_DEBUG
    if (kcov_emergency_off) [[unlikely]]
        return;

    // Use are_interrupts_enabled() as a proxy to check we are not currently in an interrupt.
    // current_in_irq() will only start returning true, once it incremented m_in_irq, which it
    // doesn't do right away. This results in a short interval where we are in an interrupt,
    // but the check will not tell us so. In that case, we would incorrectly identify the
    // interrupt as __sanitizer_cov_trace_pc recursion here:
    if (thread->m_kcov_recursion_hint && Processor::are_interrupts_enabled()) [[unlikely]] {
        kcov_emergency_off = true;
        dbgln("KCOV Error: __sanitizer_cov_trace_pc causes recursion. If possible, modify "
              "__sanitizer_cov_trace_pc to not make the call which transitively caused the recursion. "
              "Alternatively either mark the caller of the second __sanitizer_cov_trace_pc with "
              "NO_SANITIZE_COVERAGE, or add that callers .cpp file to KCOV_EXCLUDED_SOURCES.");
        crash_and_burn(thread);
        VERIFY_NOT_REACHED();
    }
    TemporaryChange kcov_recursion_hint { thread->m_kcov_recursion_hint, true };
#endif

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
