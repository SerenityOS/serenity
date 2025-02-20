/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Types.h>
#include <AK/Vector.h>

#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Arch/DeferredCallPool.h>
#include <Kernel/Arch/ProcessorSpecificDataID.h>
#include <Kernel/Arch/riscv64/CSR.h>
#include <Kernel/Memory/VirtualAddress.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel {

namespace Memory {
class PageDirectory;
};

class Thread;
class Processor;
struct TrapFrame;
enum class InterruptsState;

template<typename ProcessorT>
class ProcessorBase;

// FIXME: Remove this once we support SMP in riscv64
extern Processor* g_current_processor;

constexpr size_t MAX_CPU_COUNT = 1;

class Processor final : public ProcessorBase<Processor> {
public:
    template<IteratorFunction<Processor&> Callback>
    static inline IterationDecision for_each(Callback callback)
    {
        // FIXME: Once we support SMP for riscv64, make sure to call the callback for every processor.
        if (callback(*g_current_processor) == IterationDecision::Break)
            return IterationDecision::Break;
        return IterationDecision::Continue;
    }

    template<VoidFunction<Processor&> Callback>
    static inline IterationDecision for_each(Callback callback)
    {
        // FIXME: Once we support SMP for riscv64, make sure to call the callback for every processor.
        callback(*g_current_processor);
        return IterationDecision::Continue;
    }
};

template<typename T>
ALWAYS_INLINE bool ProcessorBase<T>::is_initialized()
{
    return g_current_processor != nullptr;
}

template<typename T>
ALWAYS_INLINE Thread* ProcessorBase<T>::idle_thread()
{
    return current().m_idle_thread;
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::set_current_thread(Thread& current_thread)
{
    current().m_current_thread = &current_thread;
}

// FIXME: When riscv64 supports multiple cores, return the correct core id here.
template<typename T>
ALWAYS_INLINE u32 ProcessorBase<T>::current_id()
{
    return 0;
}

template<typename T>
ALWAYS_INLINE u32 ProcessorBase<T>::in_critical()
{
    return current().m_in_critical;
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::enter_critical()
{
    auto& current_processor = current();
    current_processor.m_in_critical += 1;
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::restore_critical(u32 prev_critical)
{
    current().m_in_critical = prev_critical;
}

template<typename T>
ALWAYS_INLINE T& ProcessorBase<T>::current()
{
    return *g_current_processor;
}

template<typename T>
void ProcessorBase<T>::idle_begin() const
{
    // FIXME: Implement this when SMP for riscv64 is supported.
}

template<typename T>
void ProcessorBase<T>::idle_end() const
{
    // FIXME: Implement this when SMP for riscv64 is supported.
}

template<typename T>
void ProcessorBase<T>::smp_enable()
{
    // FIXME: Implement this when SMP for riscv64 is supported.
}

template<typename T>
bool ProcessorBase<T>::is_smp_enabled()
{
    return false;
}

template<typename T>
ALWAYS_INLINE bool ProcessorBase<T>::are_interrupts_enabled()
{
    return RISCV64::CSR::SSTATUS::read().SIE == 1;
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::enable_interrupts()
{
    RISCV64::CSR::set_bits(RISCV64::CSR::Address::SSTATUS, 1 << to_underlying(RISCV64::CSR::SSTATUS::Offset::SIE));
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::disable_interrupts()
{
    RISCV64::CSR::clear_bits(RISCV64::CSR::Address::SSTATUS, 1 << to_underlying(RISCV64::CSR::SSTATUS::Offset::SIE));
}

template<typename T>
ALWAYS_INLINE bool ProcessorBase<T>::current_in_scheduler()
{
    return current().m_in_scheduler;
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::set_current_in_scheduler(bool value)
{
    current().m_in_scheduler = value;
}

template<typename T>
ALWAYS_INLINE bool ProcessorBase<T>::has_nx() const
{
    return true;
}

template<typename T>
ALWAYS_INLINE FlatPtr ProcessorBase<T>::current_in_irq()
{
    return current().m_in_irq;
}

template<typename T>
ALWAYS_INLINE Thread* ProcessorBase<T>::current_thread()
{
    return current().m_current_thread;
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::pause()
{
    // PAUSE is a HINT defined by the Zihintpause extension.
    // We don't have to check if that extension is supported, since HINTs effectively behave like NOPs if they are not implemented.
    asm volatile(R"(
        .option push
        .option arch, +zihintpause
            pause
        .option pop
    )" :);
}

template<typename T>
ALWAYS_INLINE void ProcessorBase<T>::wait_check()
{
    Processor::pause();
    // FIXME: Process SMP messages once we support SMP on riscv64; cf. x86_64
}

template<typename T>
ALWAYS_INLINE Optional<u64> ProcessorBase<T>::read_cycle_count()
{
    return RISCV64::CSR::read(RISCV64::CSR::Address::CYCLE);
}

}
