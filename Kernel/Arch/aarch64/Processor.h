/*
 * Copyright (c) 2018-2021, James Mintram <me@jamesrm.com>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Function.h>
#include <AK/Types.h>

#include <Kernel/Arch/DeferredCallEntry.h>
#include <Kernel/Arch/DeferredCallPool.h>
#include <Kernel/Arch/ProcessorSpecificDataID.h>
#include <Kernel/Arch/aarch64/CPUID.h>
#include <Kernel/Arch/aarch64/Registers.h>
#include <Kernel/Memory/VirtualAddress.h>

#include <AK/Platform.h>
VALIDATE_IS_AARCH64()

namespace Kernel {

namespace Memory {
class PageDirectory;
};

class Thread;
class Processor;
struct TrapFrame;
enum class InterruptsState;

// FIXME: Remove this once we support SMP in aarch64
extern Processor* g_current_processor;

constexpr size_t MAX_CPU_COUNT = 1;

class Processor final : public ProcessorBase {
public:
    template<IteratorFunction<Processor&> Callback>
    static inline IterationDecision for_each(Callback callback)
    {
        // FIXME: Once we support SMP for aarch64, make sure to call the callback for every processor.
        if (callback(*g_current_processor) == IterationDecision::Break)
            return IterationDecision::Break;
        return IterationDecision::Continue;
    }

    template<VoidFunction<Processor&> Callback>
    static inline IterationDecision for_each(Callback callback)
    {
        // FIXME: Once we support SMP for aarch64, make sure to call the callback for every processor.
        callback(*g_current_processor);
        return IterationDecision::Continue;
    }
};

ALWAYS_INLINE bool ProcessorBase::is_initialized()
{
    return g_current_processor != nullptr;
}

ALWAYS_INLINE Thread* ProcessorBase::idle_thread()
{
    return current().m_idle_thread;
}

ALWAYS_INLINE void ProcessorBase::set_current_thread(Thread& current_thread)
{
    current().m_current_thread = &current_thread;
}

// FIXME: When aarch64 supports multiple cores, return the correct core id here.
ALWAYS_INLINE u32 ProcessorBase::current_id()
{
    return 0;
}

ALWAYS_INLINE u32 ProcessorBase::in_critical()
{
    return current().m_in_critical;
}

ALWAYS_INLINE void ProcessorBase::enter_critical()
{
    auto& current_processor = current();
    current_processor.m_in_critical = current_processor.m_in_critical + 1;
}

ALWAYS_INLINE void ProcessorBase::restore_critical(u32 prev_critical)
{
    current().m_in_critical = prev_critical;
}

ALWAYS_INLINE Processor& ProcessorBase::current()
{
    return *g_current_processor;
}

ALWAYS_INLINE bool ProcessorBase::are_interrupts_enabled()
{
    auto daif = Aarch64::DAIF::read();
    return !daif.I;
}

ALWAYS_INLINE void ProcessorBase::enable_interrupts()
{
    Aarch64::DAIF::clear_I();
}

ALWAYS_INLINE void ProcessorBase::disable_interrupts()
{
    Aarch64::DAIF::set_I();
}

ALWAYS_INLINE bool ProcessorBase::current_in_scheduler()
{
    return current().m_in_scheduler;
}

ALWAYS_INLINE void ProcessorBase::set_current_in_scheduler(bool value)
{
    current().m_in_scheduler = value;
}

ALWAYS_INLINE bool ProcessorBase::has_nx() const
{
    return true;
}

ALWAYS_INLINE FlatPtr ProcessorBase::current_in_irq()
{
    return current().m_in_irq;
}

ALWAYS_INLINE Thread* ProcessorBase::current_thread()
{
    return current().m_current_thread;
}

ALWAYS_INLINE void ProcessorBase::pause()
{
    asm volatile("isb sy");
}

ALWAYS_INLINE void ProcessorBase::wait_check()
{
    asm volatile("yield");
    // FIXME: Process SMP messages once we support SMP on aarch64; cf. x86_64
}

ALWAYS_INLINE Optional<u64> ProcessorBase::read_cycle_count()
{
    if (Processor::current().has_feature(CPUFeature::PMUv3))
        return Aarch64::PMCCNTR_EL0::read().CCNT;
    return {};
}

}
