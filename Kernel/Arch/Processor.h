/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/Platform.h>
#include <AK/Vector.h>
#include <Kernel/Arch/DeferredCallEntry.h>
#include <Kernel/Arch/ProcessorSpecificDataID.h>
#include <Kernel/VirtualAddress.h>

#if ARCH(X86_64) || ARCH(I386)
#    include <Kernel/Arch/x86/DescriptorTable.h>
#endif

namespace Kernel {

#if ARCH(X86_64) || ARCH(I386)
class x86Processor;
#elif ARCH(AARCH64)
class aarch64Processor;
class Descriptor;
class DescriptorTablePointer;
#else
#    error "Unknown architecture"
#endif

class ProcessorInfo;
class Thread;
namespace Memory {
class PageDirectory;
}
struct [[gnu::packed]] BootInfo;

extern "C" [[noreturn]] void init(BootInfo const&);

// FIXME This needs to go behind some sort of platform abstraction
//       it is used between Thread and Processor.
struct [[gnu::aligned(64), gnu::packed]] FPUState;

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

#if ARCH(X86_64) || ARCH(I386)
typedef x86Processor ProcessorImpl;
#elif ARCH(AARCH64)
typedef aarch64Processor ProcessorImpl;
#endif

class Processor {
    AK_MAKE_NONCOPYABLE(Processor);
    AK_MAKE_NONMOVABLE(Processor);

public:
    Processor() = default;

    void early_initialize(u32 cpu);
    void initialize(u32 cpu);

    static ProcessorImpl& current();
    ProcessorImpl const& processor() const { return *impl; }
    static void wait_check();
    static Thread* current_thread();
    static Thread* idle_thread();
    static FlatPtr current_in_irq();
    static u32 in_critical();
    static bool has_nx();
    static u64 read_cpu_counter();
    static u32 current_id();
    static void enter_critical();
    static void leave_critical();
    static StringView platform_string();
    [[noreturn]] static void assume_context(Thread& thread, FlatPtr flags);
    static u32 count();
    [[noreturn]] static void halt();

    static Descriptor& get_gdt_entry(u16 selector);
    static DescriptorTablePointer const& get_gdtr();
    static void smp_enable();
    static u32 smp_wake_n_idle_processors(u32 wake_count);

    static ProcessorInfo& info();
    static void flush_entire_tlb_local();
    static ProcessorImpl& by_id(u32);
    static bool is_bootstrap_processor();
    static u8 physical_address_bit_width();
    static bool has_pat();
    static bool has_rdseed();
    static bool has_rdrand();
    static bool has_tsc();
    static bool has_smap();
    static bool has_hypervisor();
    static bool is_initialized();
    static u32 clear_critical();
    static void restore_critical(u32 prev_critical);
    static FPUState const& clean_fpu_state();
    static bool current_in_scheduler();
    static void set_current_in_scheduler(bool value);

    static void flush_tlb_local(VirtualAddress vaddr, size_t page_count);
    static void flush_tlb(Memory::PageDirectory const*, VirtualAddress, size_t);
    static ErrorOr<AK::Vector<FlatPtr, 32>> capture_stack_trace(Thread& thread, size_t max_frames = 0);
    static void set_current_thread(Thread& current_thread);

    static void set_specific(ProcessorSpecificDataID specific_id, void* ptr);
    template<typename T>
    static T* get_specific();

    static void deferred_call_queue(Function<void()> callback);

    template<IteratorFunction<ProcessorImpl&> Callback>
    static IterationDecision for_each(Callback callback);

    template<VoidFunction<ProcessorImpl&> Callback>
    static IterationDecision for_each(Callback callback);

    static ErrorOr<void> try_for_each(Function<ErrorOr<void>(ProcessorImpl&)> callback);

public:
    static ProcessorImpl* impl;

public: // only for init.cpp
    void set_impl(ProcessorImpl* i) { impl = i; }

    // friend void ::init(BootInfo const& boot_info);
};

template<typename T>
class ProcessorSpecific {
public:
    static void initialize()
    {
        Processor::set_specific(T::processor_specific_data_id(), new T);
    }

    static T& get()
    {
        return *Processor::get_specific<T>();
    }
};
}
