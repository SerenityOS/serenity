/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ArbitrarySizedEnum.h>
#include <AK/Array.h>
#include <AK/Concepts.h>
#include <AK/Function.h>
#include <AK/Platform.h>
#include <AK/Types.h>
#include <AK/UFixedBigInt.h>
#include <Kernel/Arch/ProcessorSpecificDataID.h>

class VirtualAddress;

namespace Kernel {

namespace Memory {
class PageDirectory;
};

class Thread;
class aarch64Processor;

AK_MAKE_ARBITRARY_SIZED_ENUM(CPUFeature, u256,
    CHANGEALLOFME = CPUFeature(1u) << 0u,
    RDSEED = CPUFeature(1u) << 0u,
    RDRAND = CPUFeature(1u) << 0u,
    TSC = CPUFeature(1u) << 0u,
    SMAP = CPUFeature(1u) << 0u,
    HYPERVISOR = CPUFeature(1u) << 0u);

// FIXME: Remove this once we support SMP in aarch64
extern aarch64Processor* g_current_processor;

class aarch64Processor {
public:
    void initialize(u32 cpu);

    void set_specific(ProcessorSpecificDataID /*specific_id*/, void* /*ptr*/)
    {
        VERIFY_NOT_REACHED();
    }
    template<typename T>
    T* get_specific()
    {
        VERIFY_NOT_REACHED();
        return 0;
    }

    ALWAYS_INLINE static void pause()
    {
        VERIFY_NOT_REACHED();
    }
    ALWAYS_INLINE static void wait_check()
    {
        VERIFY_NOT_REACHED();
    }

    ALWAYS_INLINE u8 physical_address_bit_width() const
    {
        VERIFY_NOT_REACHED();
        return 0;
    }

    ALWAYS_INLINE u8 virtual_address_bit_width() const
    {
        VERIFY_NOT_REACHED();
        return 0;
    }

    ALWAYS_INLINE static bool is_initialized()
    {
        return false;
    }

    ALWAYS_INLINE static void flush_tlb_local(VirtualAddress&, size_t&)
    {
        VERIFY_NOT_REACHED();
    }

    ALWAYS_INLINE static void flush_tlb(Memory::PageDirectory const*, VirtualAddress const&, size_t)
    {
        VERIFY_NOT_REACHED();
    }

    // FIXME: When aarch64 supports multiple cores, return the correct core id here.
    ALWAYS_INLINE static u32 current_id()
    {
        return 0;
    }

    // FIXME: Actually return the current thread once aarch64 supports threading.
    ALWAYS_INLINE static Thread* current_thread()
    {
        return nullptr;
    }

    ALWAYS_INLINE bool has_nx() const
    {
        return true;
    }

    ALWAYS_INLINE bool has_pat() const
    {
        return false;
    }

    ALWAYS_INLINE static FlatPtr current_in_irq()
    {
        VERIFY_NOT_REACHED();
        return 0;
    }

    ALWAYS_INLINE static u64 read_cpu_counter()
    {
        VERIFY_NOT_REACHED();
        return 0;
    }

    ALWAYS_INLINE static void enter_critical() { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE static void leave_critical() { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE static u32 in_critical()
    {
        VERIFY_NOT_REACHED();
        return 0;
    }

    // FIXME: Actually return the idle thread once aarch64 supports threading.
    ALWAYS_INLINE static Thread* idle_thread()
    {
        return nullptr;
    }

    ALWAYS_INLINE static aarch64Processor& current()
    {
        return *g_current_processor;
    }

    static void deferred_call_queue(Function<void()> /* callback */)
    {
        VERIFY_NOT_REACHED();
    }

    [[noreturn]] static void halt();

    [[noreturn]] ALWAYS_INLINE static void early_initialize(u32 /*cpu*/) { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE static StringView platform_string() { VERIFY_NOT_REACHED(); }
    [[noreturn]] static void assume_context(Thread& /*thread*/, FlatPtr /*flags*/) { VERIFY_NOT_REACHED(); }
    static u32 count() { VERIFY_NOT_REACHED(); }
    Descriptor& get_gdt_entry(u16 /*selector*/) { VERIFY_NOT_REACHED(); }
    DescriptorTablePointer const& get_gdtr() { VERIFY_NOT_REACHED(); }
    [[noreturn]] static void smp_enable() { VERIFY_NOT_REACHED(); }
    static u32 smp_wake_n_idle_processors(u32 /*wake_count*/) { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE ProcessorInfo& info() { VERIFY_NOT_REACHED(); }
    static void flush_entire_tlb_local() { VERIFY_NOT_REACHED(); }
    static aarch64Processor& by_id(u32 /*id*/) { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE static bool is_bootstrap_processor() { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE bool has_feature(CPUFeature::Type const& /*feature*/) const { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE static u32 clear_critical() { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE static void restore_critical(u32 /*prev_critical*/) { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE static FPUState const& clean_fpu_state() { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE static bool current_in_scheduler() { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE static void set_current_in_scheduler(bool /*value*/) { VERIFY_NOT_REACHED(); }
    ALWAYS_INLINE static void set_current_thread(Thread& /*current_thread*/) { VERIFY_NOT_REACHED(); }
    static ErrorOr<Vector<FlatPtr, 32>> capture_stack_trace(Thread& /*thread*/, size_t /*max_frames = 0*/) { VERIFY_NOT_REACHED(); }

    template<IteratorFunction<aarch64Processor&> Callback>
    static inline IterationDecision for_each(Callback /*callback*/)
    {
        VERIFY_NOT_REACHED();
    }

    template<VoidFunction<aarch64Processor&> Callback>
    static inline IterationDecision for_each(Callback /*callback*/)
    {
        VERIFY_NOT_REACHED();
    }

    static inline ErrorOr<void> try_for_each(Function<ErrorOr<void>(aarch64Processor&)> /*callback*/)
    {
        VERIFY_NOT_REACHED();
    }
};

}
