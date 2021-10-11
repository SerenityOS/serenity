/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Concepts.h>
#include <AK/Function.h>
#include <AK/Types.h>

#include <Kernel/Arch/x86/ASM_wrapper.h>
#include <Kernel/Arch/x86/CPUID.h>
#include <Kernel/Arch/x86/DescriptorTable.h>
#include <Kernel/Arch/x86/PageDirectory.h>
#include <Kernel/Arch/x86/TSS.h>
#include <Kernel/Forward.h>

namespace Kernel 
{

class ProcessorMessageEntry;
class DeferredCallEntry;

enum class ProcessorSpecificDataID {
    MemoryManager,
    __Count,
};
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

    volatile bool async;

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


struct DeferredCallEntry {
    using HandlerFunction = Function<void()>;

    DeferredCallEntry* next;
    alignas(HandlerFunction) u8 handler_storage[sizeof(HandlerFunction)];
    bool was_allocated;

    HandlerFunction& handler_value()
    {
        return *bit_cast<HandlerFunction*>(&handler_storage);
    }

    void invoke_handler()
    {
        handler_value()();
    }
};

}


#if ARCH(X86_64) || ARCH(I386)
#    include <Kernel/Arch/x86/Processor.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/Processor.h>
#else
#   error "Unknown architecture"
#endif

namespace Kernel {
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
