/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Concepts.h>
#include <AK/Function.h>
#include <AK/SetOnce.h>
#include <AK/Types.h>

#include <Kernel/Arch/DeferredCallEntry.h>
#include <Kernel/Arch/DeferredCallPool.h>
#include <Kernel/Arch/ProcessorSpecificDataID.h>
#include <Kernel/Arch/x86_64/ASM_wrapper.h>
#include <Kernel/Arch/x86_64/CPUID.h>
#include <Kernel/Arch/x86_64/DescriptorTable.h>
#include <Kernel/Arch/x86_64/SIMDState.h>
#include <Kernel/Arch/x86_64/TSS.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/KString.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

class ProcessorInfo;
struct ProcessorMessage;
struct ProcessorMessageEntry;

#define MSR_EFER 0xc0000080
#define MSR_STAR 0xc0000081
#define MSR_LSTAR 0xc0000082
#define MSR_SFMASK 0xc0000084
#define MSR_FS_BASE 0xc0000100
#define MSR_GS_BASE 0xc0000101
#define MSR_IA32_EFER 0xc0000080
#define MSR_IA32_PAT 0x277

enum class InterruptsState;
class Processor;

template<typename ProcessorT>
class ProcessorBase;

// Note: We only support 64 processors at most at the moment,
// so allocate 64 slots of inline capacity in the container.

constexpr size_t MAX_CPU_COUNT = 64;
using ProcessorContainer = Array<Processor*, MAX_CPU_COUNT>;

extern "C" void context_first_init(Thread* from_thread, Thread* to_thread, [[maybe_unused]] TrapFrame* trap);

// If this fails to compile because ProcessorBase was not found, you are including this header directly.
// Include Arch/Processor.h instead.
class Processor final : public ProcessorBase<Processor> {
    friend class ProcessorInfo;
    // Allow some implementations to access the idle CPU mask and various x86 implementation details.
    friend class ProcessorBase<Processor>;

private:
    // Saved user stack for the syscall instruction.
    void* m_user_stack;

    DescriptorTablePointer m_gdtr;
    alignas(Descriptor) Descriptor m_gdt[256];
    u32 m_gdt_length;

    static Atomic<u32> s_idle_cpu_mask;

    TSS m_tss;
    SetOnce m_has_qemu_hvf_quirk;

    ProcessorInfo* m_info;

    Atomic<ProcessorMessageEntry*> m_message_queue;

    void gdt_init();
    void write_raw_gdt_entry(u16 selector, u32 low, u32 high);
    void write_gdt_entry(u16 selector, Descriptor& descriptor);
    static ProcessorContainer& processors();

    static void smp_return_to_pool(ProcessorMessage& msg);
    static ProcessorMessage& smp_get_from_pool();
    static void smp_cleanup_message(ProcessorMessage& msg);
    bool smp_enqueue_message(ProcessorMessage&);
    static void smp_unicast_message(u32 cpu, ProcessorMessage& msg, bool async);
    static void smp_broadcast_message(ProcessorMessage& msg);
    static void smp_broadcast_wait_sync(ProcessorMessage& msg);
    static void smp_broadcast_halt();

    void cpu_detect();
    void cpu_setup();

    void detect_hypervisor();
    void detect_hypervisor_hyperv(CPUID const& hypervisor_leaf_range);

public:
    Descriptor& get_gdt_entry(u16 selector);
    void flush_gdt();
    DescriptorTablePointer const& get_gdtr();

    template<IteratorFunction<Processor&> Callback>
    static inline IterationDecision for_each(Callback callback)
    {
        auto& procs = processors();
        size_t count = procs.size();
        for (size_t i = 0; i < count; i++) {
            if (callback(*procs[i]) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<VoidFunction<Processor&> Callback>
    static inline IterationDecision for_each(Callback callback)
    {
        auto& procs = processors();
        size_t count = procs.size();
        for (size_t i = 0; i < count; i++) {
            if (procs[i] != nullptr)
                callback(*procs[i]);
        }
        return IterationDecision::Continue;
    }

    static inline ErrorOr<void> try_for_each(Function<ErrorOr<void>(Processor&)> callback)
    {
        auto& procs = processors();
        size_t count = procs.size();
        for (size_t i = 0; i < count; i++) {
            if (procs[i] != nullptr)
                TRY(callback(*procs[i]));
        }
        return {};
    }

    ALWAYS_INLINE ProcessorInfo& info() { return *m_info; }

    static constexpr u64 user_stack_offset()
    {
        return __builtin_offsetof(Processor, m_user_stack);
    }
    static constexpr u64 kernel_stack_offset()
    {
        return __builtin_offsetof(Processor, m_tss) + __builtin_offsetof(TSS, rsp0l);
    }

    bool smp_process_pending_messages();

    static void smp_unicast(u32 cpu, Function<void()>, bool async);
    static void smp_broadcast_flush_tlb(Memory::PageDirectory const*, VirtualAddress, size_t);

    static void set_fs_base(FlatPtr);
};

template<typename T>
ALWAYS_INLINE NO_SANITIZE_COVERAGE Thread* ProcessorBase<T>::current_thread()
{
    // If we were to use ProcessorBase::current here, we'd have to
    // disable interrupts to prevent a race where we may get pre-empted
    // right after getting the Processor structure and then get moved
    // to another processor, which would lead us to get the wrong thread.
    // To avoid having to disable interrupts, we can just read the field
    // directly in an atomic fashion, similar to Processor::current.
    return (Thread*)read_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_current_thread));
}

template<typename T>
ALWAYS_INLINE u32 ProcessorBase<T>::current_id()
{
    // See comment in ProcessorBase::current_thread
    return read_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_cpu));
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::restore_critical(u32 prev_critical)
{
    // NOTE: This doesn't have to be atomic, and it's also fine if we
    // get preempted in between these steps. If we move to another
    // processors m_in_critical will move along with us. And if we
    // are preempted, we would resume with the same flags.
    write_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_in_critical), prev_critical);
}

template<typename T>
ALWAYS_INLINE u32 ProcessorBase<T>::in_critical()
{
    // See comment in ProcessorBase::current_thread
    return read_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_in_critical));
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::set_current_thread(Thread& current_thread)
{
    // See comment in ProcessorBase::current_thread
    write_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_current_thread), FlatPtr(&current_thread));
}

template<typename T>
ALWAYS_INLINE Thread* ProcessorBase<T>::idle_thread()
{
    // See comment in ProcessorBase::current_thread
    return (Thread*)read_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_idle_thread));
}

template<typename T>
T& ProcessorBase<T>::current()
{
    return *(Processor*)(read_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_self)));
}

template<typename T>
ALWAYS_INLINE bool ProcessorBase<T>::is_initialized()
{
    return read_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_self)) != 0;
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::enter_critical()
{
    write_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_in_critical), in_critical() + 1);
}

template<typename T>
ALWAYS_INLINE NO_SANITIZE_COVERAGE bool ProcessorBase<T>::are_interrupts_enabled()
{
    return Kernel::are_interrupts_enabled();
}

template<typename T>
ALWAYS_INLINE bool ProcessorBase<T>::current_in_scheduler()
{
    auto value = read_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_in_scheduler));
    return value;
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::set_current_in_scheduler(bool value)
{
    write_gs_ptr(__builtin_offsetof(ProcessorBase<T>, m_in_scheduler), value);
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::enable_interrupts()
{
    sti();
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::disable_interrupts()
{
    cli();
}

template<typename T>
ALWAYS_INLINE bool ProcessorBase<T>::has_nx() const
{
    return has_feature(CPUFeature::NX);
}

template<typename T>
ALWAYS_INLINE Optional<u64> ProcessorBase<T>::read_cycle_count()
{
    return read_tsc();
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::pause()
{
    asm volatile("pause");
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::wait_check()
{
    Processor::pause();
    if (Processor::is_smp_enabled())
        Processor::current().smp_process_pending_messages();
}

template<typename T>
ALWAYS_INLINE NO_SANITIZE_COVERAGE FlatPtr ProcessorBase<T>::current_in_irq()
{
    return read_gs_ptr(__builtin_offsetof(Processor, m_in_irq));
}

}
