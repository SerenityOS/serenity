/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <Kernel/Arch/DeferredCallEntry.h>

#if ARCH(X86_64) || ARCH(I386)
#    include <Kernel/Arch/x86/Processor.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/Processor.h>
#else
#    error "Unknown architecture"
#endif

namespace Kernel {

namespace Memory {
class PageDirectory;
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
