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

class VirtualAddress;

namespace Kernel {

namespace Memory {
class PageDirectory;
};

class Thread;

// FIXME This needs to go behind some sort of platform abstraction
//       it is used between Thread and Processor.
struct [[gnu::aligned(16)]] FPUState
{
    u8 buffer[512];
};

class Processor {
public:
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

    ALWAYS_INLINE static u32 current_id()
    {
        VERIFY_NOT_REACHED();
        return 0;
    }

    ALWAYS_INLINE static Thread* current_thread()
    {
        VERIFY_NOT_REACHED();
        return 0;
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

    ALWAYS_INLINE static Thread* idle_thread()
    {
        VERIFY_NOT_REACHED();
        return nullptr;
    }

    ALWAYS_INLINE static Processor& current() { VERIFY_NOT_REACHED(); }

    static void deferred_call_queue(Function<void()> /* callback */)
    {
        VERIFY_NOT_REACHED();
    }

    [[noreturn]] static void halt()
    {
        for (;;) { }
    }
};

}
