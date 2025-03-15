/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/SetOnce.h>
#include <Kernel/Arch/CPUID.h>
#include <Kernel/Arch/DeferredCallEntry.h>
#include <Kernel/Arch/DeferredCallPool.h>
#include <Kernel/Arch/FPUState.h>
#include <Kernel/Arch/ProcessorSpecificDataID.h>
#include <Kernel/Memory/VirtualAddress.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/DescriptorTable.h>
#endif

namespace Kernel {

enum class InterruptsState {
    Enabled,
    Disabled
};

namespace Memory {
class PageDirectory;
}

struct TrapFrame;
class Thread;

class Processor;

extern Atomic<u32> g_total_processors;

extern FPUState s_clean_fpu_state;

// context_first_init is an architecture-specific detail with various properties.
// All variants eventually call into the common code here.
void do_context_first_init(Thread* from_thread, Thread* to_thread);
extern "C" void exit_kernel_thread(void);
extern "C" void thread_context_first_enter(void);
extern "C" void do_assume_context(Thread* thread, u32 flags);
extern "C" FlatPtr do_init_context(Thread* thread, u32) __attribute__((used));

template<typename ProcessorT>
class ProcessorBase {
public:
    template<typename T>
    T* get_specific()
    {
        return static_cast<T*>(m_processor_specific_data[static_cast<size_t>(T::processor_specific_data_id())]);
    }

    void set_specific(ProcessorSpecificDataID specific_id, void* ptr)
    {
        m_processor_specific_data[static_cast<size_t>(specific_id)] = ptr;
    }

    static bool is_smp_enabled();
    static void smp_enable();
    static u32 smp_wake_n_idle_processors(u32 wake_count);

    static void flush_tlb_local(VirtualAddress vaddr, size_t page_count);
    static void flush_tlb(Memory::PageDirectory const*, VirtualAddress, size_t);

    static void flush_instruction_cache(VirtualAddress vaddr, size_t byte_count);

    void early_initialize(u32 cpu);
    void initialize(u32 cpu);
    ALWAYS_INLINE static bool is_initialized();

    [[noreturn]] static void halt();
    void wait_for_interrupt() const;
    ALWAYS_INLINE static void pause();
    ALWAYS_INLINE static void wait_check();

    ALWAYS_INLINE static ProcessorT& current();
    static Processor& by_id(u32);

    ALWAYS_INLINE u32 id() const
    {
        // NOTE: This variant should only be used when iterating over all
        // Processor instances, or when it's guaranteed that the thread
        // cannot move to another processor in between calling Processor::current
        // and Processor::id, or if this fact is not important.
        // All other cases should use Processor::current_id instead!
        return m_cpu;
    }

    ALWAYS_INLINE static u32 current_id();
    ALWAYS_INLINE static bool is_bootstrap_processor();
    ALWAYS_INLINE bool has_nx() const;
    ALWAYS_INLINE bool has_feature(CPUFeature::Type const& feature) const
    {
        return m_features.has_flag(feature);
    }
    static StringView platform_string();

    static u32 count()
    {
        // NOTE: because this value never changes once all APs are booted,
        // we can safely bypass loading it atomically.
        // NOTE: This does not work on aarch64, since the variable is never written.
        return *g_total_processors.ptr();
    }

    void enter_trap(TrapFrame& trap, bool raise_irq);
    void exit_trap(TrapFrame& trap);

    static void flush_entire_tlb_local();

    ALWAYS_INLINE static Thread* current_thread();
    ALWAYS_INLINE static void set_current_thread(Thread& current_thread);
    ALWAYS_INLINE static Thread* idle_thread();

    ALWAYS_INLINE static u32 in_critical();
    ALWAYS_INLINE static void enter_critical();
    static void leave_critical();
    void do_leave_critical();
    static u32 clear_critical();
    ALWAYS_INLINE static void restore_critical(u32 prev_critical);
    ALWAYS_INLINE static void verify_no_spinlocks_held()
    {
        VERIFY(!ProcessorBase::in_critical());
    }

    static InterruptsState interrupts_state();
    static void restore_interrupts_state(InterruptsState);
    static bool are_interrupts_enabled();
    ALWAYS_INLINE static void enable_interrupts();
    ALWAYS_INLINE static void disable_interrupts();
    ALWAYS_INLINE static FlatPtr current_in_irq();

    ALWAYS_INLINE void set_idle_thread(Thread& idle_thread)
    {
        m_idle_thread = &idle_thread;
    }
    void idle_begin() const;
    void idle_end() const;
    u64 time_spent_idle() const;
    ALWAYS_INLINE static Optional<u64> read_cycle_count();

    void check_invoke_scheduler();
    void invoke_scheduler_async() { m_invoke_scheduler_async = true; }
    ALWAYS_INLINE static bool current_in_scheduler();
    ALWAYS_INLINE static void set_current_in_scheduler(bool value);
    ALWAYS_INLINE bool is_in_scheduler() const { return m_in_scheduler; }

    ALWAYS_INLINE u8 physical_address_bit_width() const
    {
        return m_physical_address_bit_width;
    }
    ALWAYS_INLINE u8 virtual_address_bit_width() const
    {
        return m_virtual_address_bit_width;
    }

    ALWAYS_INLINE static FPUState const& clean_fpu_state() { return s_clean_fpu_state; }

    static void deferred_call_queue(Function<void()> callback);

    [[noreturn]] void initialize_context_switching(Thread& initial_thread);
    NEVER_INLINE void switch_context(Thread*& from_thread, Thread*& to_thread);
    [[noreturn]] static void assume_context(Thread& thread, InterruptsState new_interrupts_state);
    FlatPtr init_context(Thread& thread, bool leave_crit);
    static ErrorOr<Vector<FlatPtr, 32>> capture_stack_trace(Thread& thread, size_t max_frames = 0);

protected:
    ProcessorT* m_self;
    CPUFeature::Type m_features;

    Atomic<bool> m_halt_requested;

    u8 m_physical_address_bit_width;
    u8 m_virtual_address_bit_width;

private:
    void* m_processor_specific_data[static_cast<size_t>(ProcessorSpecificDataID::__Count)];
    Thread* m_idle_thread;
    Thread* m_current_thread;

    u32 m_cpu { 0 };

    // FIXME: On aarch64, once there is code in place to differentiate IRQs from synchronous exceptions (syscalls),
    //        this member should be incremented. Also this member shouldn't be a FlatPtr.
    FlatPtr m_in_irq { 0 };
    u32 volatile m_in_critical;
    // NOTE: Since these variables are accessed with atomic magic on x86 (through GP with a single load instruction),
    //       they need to be FlatPtrs or everything becomes highly unsound and breaks. They are actually just booleans.
    FlatPtr m_in_scheduler;
    FlatPtr m_invoke_scheduler_async;

    SetOnce m_scheduler_initialized;

    DeferredCallPool m_deferred_call_pool {};
};

template class ProcessorBase<Processor>;

}

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Processor.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/Processor.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/Processor.h>
#else
#    error "Unknown architecture"
#endif

namespace Kernel {

template<typename T>
ALWAYS_INLINE bool ProcessorBase<T>::is_bootstrap_processor()
{
    return current_id() == 0;
}

template<typename T>
InterruptsState ProcessorBase<T>::interrupts_state()
{
    return Processor::are_interrupts_enabled() ? InterruptsState::Enabled : InterruptsState::Disabled;
}

template<typename T>
void ProcessorBase<T>::restore_interrupts_state(InterruptsState interrupts_state)
{
    if (interrupts_state == InterruptsState::Enabled)
        Processor::enable_interrupts();
    else
        Processor::disable_interrupts();
}

struct ProcessorMessageEntry;
struct ProcessorMessage {
    using CallbackFunction = Function<void()>;

    enum Type {
        FlushTlb,
        Callback,
    };
    Type type;
    Atomic<u32> refs;
    union {
        ProcessorMessage* next; // only valid while in the pool
        alignas(CallbackFunction) u8 callback_storage[sizeof(CallbackFunction)];
        struct {
            Memory::PageDirectory const* page_directory;
            u8* ptr;
            size_t page_count;
        } flush_tlb;
    };

    bool volatile async;

    ProcessorMessageEntry* per_proc_entries;

    CallbackFunction& callback_value()
    {
        return *bit_cast<CallbackFunction*>(&callback_storage);
    }

    void invoke_callback()
    {
        VERIFY(type == Type::Callback);
        callback_value()();
    }
};

struct ProcessorMessageEntry {
    ProcessorMessageEntry* next;
    ProcessorMessage* msg;
};

template<typename T>
class ProcessorSpecific {
public:
    static void initialize()
    {
        Processor::current().set_specific(T::processor_specific_data_id(), new T);
    }
    static T& get()
    {
        return *Processor::current().get_specific<T>();
    }
};
}
