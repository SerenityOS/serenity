/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Concepts.h>
#include <AK/Function.h>
#include <AK/Types.h>

#include <Kernel/Arch/DeferredCallEntry.h>
#include <Kernel/Arch/ProcessorSpecificDataID.h>
#include <Kernel/Arch/x86/ASM_wrapper.h>
#include <Kernel/Arch/x86/CPUID.h>
#include <Kernel/Arch/x86/DescriptorTable.h>
#include <Kernel/Arch/x86/PageDirectory.h>
#include <Kernel/Arch/x86/TSS.h>
#include <Kernel/Forward.h>
#include <Kernel/KString.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

class ProcessorInfo;
struct ProcessorMessage;
struct ProcessorMessageEntry;

#if ARCH(X86_64)
#    define MSR_EFER 0xc0000080
#    define MSR_STAR 0xc0000081
#    define MSR_LSTAR 0xc0000082
#    define MSR_SFMASK 0xc0000084
#    define MSR_FS_BASE 0xc0000100
#    define MSR_GS_BASE 0xc0000101
#endif
#define MSR_IA32_EFER 0xc0000080
#define MSR_IA32_PAT 0x277

// FIXME: Find a better place for these
extern "C" void thread_context_first_enter(void);
extern "C" void exit_kernel_thread(void);
extern "C" void do_assume_context(Thread* thread, u32 flags);

struct [[gnu::aligned(16)]] FPUState
{
    u8 buffer[512];
};

class Processor;
// Note: We only support 64 processors at most at the moment,
// so allocate 64 slots of inline capacity in the container.
using ProcessorContainer = Array<Processor*, 64>;

class Processor {
    friend class ProcessorInfo;

    AK_MAKE_NONCOPYABLE(Processor);
    AK_MAKE_NONMOVABLE(Processor);

    Processor* m_self;

#if ARCH(X86_64)
    // Saved user stack for the syscall instruction.
    void* m_user_stack;
#endif

    DescriptorTablePointer m_gdtr;
    alignas(Descriptor) Descriptor m_gdt[256];
    u32 m_gdt_length;

    u32 m_cpu;
    FlatPtr m_in_irq;
    volatile u32 m_in_critical;
    static Atomic<u32> s_idle_cpu_mask;

    TSS m_tss;
    static FPUState s_clean_fpu_state;
    CPUFeature m_features;
    static Atomic<u32> g_total_processors;
    u8 m_physical_address_bit_width;
    u8 m_virtual_address_bit_width;
#if ARCH(X86_64)
    bool m_has_qemu_hvf_quirk;
#endif

    ProcessorInfo* m_info;
    Thread* m_current_thread;
    Thread* m_idle_thread;

    Atomic<ProcessorMessageEntry*> m_message_queue;

    bool m_invoke_scheduler_async;
    bool m_scheduler_initialized;
    bool m_in_scheduler;
    Atomic<bool> m_halt_requested;

    DeferredCallEntry* m_pending_deferred_calls; // in reverse order
    DeferredCallEntry* m_free_deferred_call_pool_entry;
    DeferredCallEntry m_deferred_call_pool[5];

    void* m_processor_specific_data[(size_t)ProcessorSpecificDataID::__Count];

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

    void deferred_call_pool_init();
    void deferred_call_execute_pending();
    DeferredCallEntry* deferred_call_get_free();
    void deferred_call_return_to_pool(DeferredCallEntry*);
    void deferred_call_queue_entry(DeferredCallEntry*);

    void cpu_detect();
    void cpu_setup();

    NonnullOwnPtr<KString> features_string() const;

public:
    Processor() = default;

    void early_initialize(u32 cpu);
    void initialize(u32 cpu);

    void detect_hypervisor();
    void detect_hypervisor_hyperv(CPUID const& hypervisor_leaf_range);

    void idle_begin() const
    {
        s_idle_cpu_mask.fetch_or(1u << m_cpu, AK::MemoryOrder::memory_order_relaxed);
    }

    void idle_end() const
    {
        s_idle_cpu_mask.fetch_and(~(1u << m_cpu), AK::MemoryOrder::memory_order_relaxed);
    }

    static Processor& by_id(u32);

    static u32 count()
    {
        // NOTE: because this value never changes once all APs are booted,
        // we can safely bypass loading it atomically.
        return *g_total_processors.ptr();
    }

    ALWAYS_INLINE static void pause()
    {
        asm volatile("pause");
    }

    ALWAYS_INLINE static void wait_check()
    {
        Processor::pause();
        if (Processor::is_smp_enabled())
            Processor::current().smp_process_pending_messages();
    }

    [[noreturn]] static void halt();

    static void flush_entire_tlb_local()
    {
        write_cr3(read_cr3());
    }

    static void flush_tlb_local(VirtualAddress vaddr, size_t page_count);
    static void flush_tlb(Memory::PageDirectory const*, VirtualAddress, size_t);

    Descriptor& get_gdt_entry(u16 selector);
    void flush_gdt();
    const DescriptorTablePointer& get_gdtr();

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

    ALWAYS_INLINE u8 physical_address_bit_width() const { return m_physical_address_bit_width; }
    ALWAYS_INLINE u8 virtual_address_bit_width() const { return m_virtual_address_bit_width; }

    ALWAYS_INLINE ProcessorInfo& info() { return *m_info; }

    u64 time_spent_idle() const;

    static bool is_smp_enabled();

#if ARCH(X86_64)
    static constexpr u64 user_stack_offset()
    {
        return __builtin_offsetof(Processor, m_user_stack);
    }
    static constexpr u64 kernel_stack_offset()
    {
        return __builtin_offsetof(Processor, m_tss) + __builtin_offsetof(TSS, rsp0l);
    }
#endif

    ALWAYS_INLINE static Processor& current()
    {
        return *(Processor*)read_gs_ptr(__builtin_offsetof(Processor, m_self));
    }

    ALWAYS_INLINE static bool is_initialized()
    {
        return
#if ARCH(I386)
            get_gs() == GDT_SELECTOR_PROC &&
#endif
            read_gs_ptr(__builtin_offsetof(Processor, m_self)) != 0;
    }

    template<typename T>
    T* get_specific()
    {
        return static_cast<T*>(m_processor_specific_data[static_cast<size_t>(T::processor_specific_data_id())]);
    }

    void set_specific(ProcessorSpecificDataID specific_id, void* ptr)
    {
        m_processor_specific_data[static_cast<size_t>(specific_id)] = ptr;
    }

    ALWAYS_INLINE void set_idle_thread(Thread& idle_thread)
    {
        m_idle_thread = &idle_thread;
    }

    ALWAYS_INLINE static Thread* current_thread()
    {
        // If we were to use Processor::current here, we'd have to
        // disable interrupts to prevent a race where we may get pre-empted
        // right after getting the Processor structure and then get moved
        // to another processor, which would lead us to get the wrong thread.
        // To avoid having to disable interrupts, we can just read the field
        // directly in an atomic fashion, similar to Processor::current.
        return (Thread*)read_gs_ptr(__builtin_offsetof(Processor, m_current_thread));
    }

    ALWAYS_INLINE static void set_current_thread(Thread& current_thread)
    {
        // See comment in Processor::current_thread
        write_gs_ptr(__builtin_offsetof(Processor, m_current_thread), FlatPtr(&current_thread));
    }

    ALWAYS_INLINE static Thread* idle_thread()
    {
        // See comment in Processor::current_thread
        return (Thread*)read_gs_ptr(__builtin_offsetof(Processor, m_idle_thread));
    }

    ALWAYS_INLINE u32 id() const
    {
        // NOTE: This variant should only be used when iterating over all
        // Processor instances, or when it's guaranteed that the thread
        // cannot move to another processor in between calling Processor::current
        // and Processor::get_id, or if this fact is not important.
        // All other cases should use Processor::id instead!
        return m_cpu;
    }

    ALWAYS_INLINE static u32 current_id()
    {
        // See comment in Processor::current_thread
        return read_gs_ptr(__builtin_offsetof(Processor, m_cpu));
    }

    ALWAYS_INLINE static bool is_bootstrap_processor()
    {
        return Processor::current_id() == 0;
    }

    ALWAYS_INLINE static FlatPtr current_in_irq()
    {
        return read_gs_ptr(__builtin_offsetof(Processor, m_in_irq));
    }

    ALWAYS_INLINE static void restore_in_critical(u32 critical)
    {
        write_gs_ptr(__builtin_offsetof(Processor, m_in_critical), critical);
    }

    ALWAYS_INLINE static void enter_critical()
    {
        write_gs_ptr(__builtin_offsetof(Processor, m_in_critical), in_critical() + 1);
    }

    ALWAYS_INLINE static bool current_in_scheduler()
    {
        return read_gs_value<decltype(m_in_scheduler)>(__builtin_offsetof(Processor, m_in_scheduler));
    }

    ALWAYS_INLINE static void set_current_in_scheduler(bool value)
    {
        write_gs_value<decltype(m_in_scheduler)>(__builtin_offsetof(Processor, m_in_scheduler), value);
    }

private:
    ALWAYS_INLINE void do_leave_critical()
    {
        VERIFY(m_in_critical > 0);
        if (m_in_critical == 1) {
            if (m_in_irq == 0) {
                deferred_call_execute_pending();
                VERIFY(m_in_critical == 1);
            }
            m_in_critical = 0;
            if (m_in_irq == 0)
                check_invoke_scheduler();
        } else {
            m_in_critical = m_in_critical - 1;
        }
    }

public:
    ALWAYS_INLINE static void leave_critical()
    {
        current().do_leave_critical();
    }

    ALWAYS_INLINE static u32 clear_critical()
    {
        auto prev_critical = in_critical();
        write_gs_ptr(__builtin_offsetof(Processor, m_in_critical), 0);
        auto& proc = current();
        if (proc.m_in_irq == 0)
            proc.check_invoke_scheduler();
        return prev_critical;
    }

    ALWAYS_INLINE static void restore_critical(u32 prev_critical)
    {
        // NOTE: This doesn't have to be atomic, and it's also fine if we
        // get preempted in between these steps. If we move to another
        // processors m_in_critical will move along with us. And if we
        // are preempted, we would resume with the same flags.
        write_gs_ptr(__builtin_offsetof(Processor, m_in_critical), prev_critical);
    }

    ALWAYS_INLINE static u32 in_critical()
    {
        // See comment in Processor::current_thread
        return read_gs_ptr(__builtin_offsetof(Processor, m_in_critical));
    }

    ALWAYS_INLINE static FPUState const& clean_fpu_state() { return s_clean_fpu_state; }

    static void smp_enable();
    bool smp_process_pending_messages();

    static void smp_unicast(u32 cpu, Function<void()>, bool async);
    static void smp_broadcast_flush_tlb(Memory::PageDirectory const*, VirtualAddress, size_t);
    static u32 smp_wake_n_idle_processors(u32 wake_count);

    static void deferred_call_queue(Function<void()> callback);

    ALWAYS_INLINE bool has_feature(CPUFeature f) const
    {
        return (static_cast<u32>(m_features) & static_cast<u32>(f)) != 0;
    }

    void check_invoke_scheduler();
    void invoke_scheduler_async() { m_invoke_scheduler_async = true; }

    void enter_trap(TrapFrame& trap, bool raise_irq);

    void exit_trap(TrapFrame& trap);

    [[noreturn]] void initialize_context_switching(Thread& initial_thread);
    NEVER_INLINE void switch_context(Thread*& from_thread, Thread*& to_thread);
    [[noreturn]] static void assume_context(Thread& thread, FlatPtr flags);
    FlatPtr init_context(Thread& thread, bool leave_crit);
    static ErrorOr<Vector<FlatPtr, 32>> capture_stack_trace(Thread& thread, size_t max_frames = 0);

    static StringView platform_string();
};

}
