/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Concepts.h>
#include <AK/Function.h>
#include <AK/Types.h>

#include <Kernel/Arch/ProcessorSpecificDataID.h>
#include <Kernel/Arch/aarch64/Registers.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel {

namespace Memory {
class PageDirectory;
};

class Thread;
class Processor;

// FIXME This needs to go behind some sort of platform abstraction
//       it is used between Thread and Processor.
struct [[gnu::aligned(16)]] FPUState
{
    u8 buffer[512];
};

// FIXME: Remove this once we support SMP in aarch64
extern Processor* g_current_processor;

class Processor {
    void* m_processor_specific_data[static_cast<size_t>(ProcessorSpecificDataID::__Count)];

public:
    void initialize(u32 cpu);

    template<typename T>
    T* get_specific()
    {
        return static_cast<T*>(m_processor_specific_data[static_cast<size_t>(T::processor_specific_data_id())]);
    }
    void set_specific(ProcessorSpecificDataID specific_id, void* ptr)
    {
        m_processor_specific_data[static_cast<size_t>(specific_id)] = ptr;
    }

    ALWAYS_INLINE static void pause()
    {
        TODO_AARCH64();
    }
    ALWAYS_INLINE static void wait_check()
    {
        TODO_AARCH64();
    }

    ALWAYS_INLINE u8 physical_address_bit_width() const
    {
        TODO_AARCH64();
        return 0;
    }

    ALWAYS_INLINE u8 virtual_address_bit_width() const
    {
        TODO_AARCH64();
        return 0;
    }

    ALWAYS_INLINE static bool is_initialized()
    {
        return false;
    }

    static void flush_tlb_local(VirtualAddress vaddr, size_t page_count);
    static void flush_tlb(Memory::PageDirectory const*, VirtualAddress, size_t);

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
        TODO_AARCH64();
        return 0;
    }

    ALWAYS_INLINE static u64 read_cpu_counter()
    {
        TODO_AARCH64();
        return 0;
    }

    ALWAYS_INLINE static bool are_interrupts_enabled()
    {
        auto daif = Aarch64::DAIF::read();
        return !daif.I;
    }

    ALWAYS_INLINE static void enable_interrupts()
    {
        Aarch64::DAIF::clear_I();
    }

    ALWAYS_INLINE static void disable_interrupts()
    {
        Aarch64::DAIF::set_I();
    }

    // FIXME: Share the critical functions with x86/Processor.h
    ALWAYS_INLINE static void enter_critical()
    {
        auto current_processor = current();
        current_processor.m_in_critical = current_processor.in_critical() + 1;
    }

    ALWAYS_INLINE static void leave_critical()
    {
        auto current_processor = current();
        current_processor.m_in_critical = current_processor.in_critical() - 1;
    }

    static u32 clear_critical();

    ALWAYS_INLINE static void restore_critical(u32 prev_critical)
    {
        (void)prev_critical;
        TODO_AARCH64();
    }

    ALWAYS_INLINE static u32 in_critical()
    {
        return current().m_in_critical;
    }

    ALWAYS_INLINE static void verify_no_spinlocks_held()
    {
        VERIFY(!Processor::in_critical());
    }

    // FIXME: Actually return the idle thread once aarch64 supports threading.
    ALWAYS_INLINE static Thread* idle_thread()
    {
        return nullptr;
    }

    ALWAYS_INLINE static Processor& current()
    {
        return *g_current_processor;
    }

    // FIXME: Move this into generic Processor class, when there is such a class.
    ALWAYS_INLINE static bool is_bootstrap_processor()
    {
        return Processor::current_id() == 0;
    }

    static void deferred_call_queue(Function<void()> /* callback */)
    {
        TODO_AARCH64();
    }

    [[noreturn]] static void halt();

private:
    u32 m_in_critical { 0 };
};

}
