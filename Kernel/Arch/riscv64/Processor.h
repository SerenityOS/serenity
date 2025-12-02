/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Types.h>
#include <AK/Vector.h>

#include <Kernel/API/POSIX/errno.h>
#include <Kernel/API/RISCVExtensionBitmask.h>
#include <Kernel/Arch/DeferredCallPool.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/ProcessorSpecificDataID.h>
#include <Kernel/Arch/riscv64/CSR.h>
#include <Kernel/Arch/riscv64/ProcessorInfo.h>
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

// FIXME: Remove this once we support SMP in riscv64
extern Processor* g_current_processor;

constexpr size_t MAX_CPU_COUNT = 1;

class Processor final : public ProcessorBase {
    friend class ProcessorBase;

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

    ProcessorInfo const& info() const
    {
        VERIFY(m_info.has_value());
        return m_info.value();
    }

    void find_and_parse_devicetree_node();

    ReadonlySpan<unsigned long long> userspace_extension_bitmask() const { return m_userspace_extension_bitmask; }

private:
    void generate_userspace_extension_bitmask();

    Optional<ProcessorInfo> m_info;
    Array<unsigned long long, EXTENSION_BITMASK_GROUP_COUNT> m_userspace_extension_bitmask {};
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

// FIXME: When riscv64 supports multiple cores, return the correct core id here.
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
    current_processor.m_in_critical += 1;
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
    return RISCV64::CSR::SSTATUS::read().SIE == 1;
}

ALWAYS_INLINE void ProcessorBase::enable_interrupts()
{
    RISCV64::CSR::set_bits<RISCV64::CSR::Address::SSTATUS>(RISCV64::CSR::SSTATUS::Bit::SIE);
}

ALWAYS_INLINE void ProcessorBase::disable_interrupts()
{
    RISCV64::CSR::clear_bits<RISCV64::CSR::Address::SSTATUS>(RISCV64::CSR::SSTATUS::Bit::SIE);
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
    // PAUSE is a HINT defined by the Zihintpause extension.
    // We don't have to check if that extension is supported, since HINTs effectively behave like NOPs if they are not implemented.
    asm volatile(R"(
        .option push
        .option arch, +zihintpause
            pause
        .option pop
    )" :);
}

ALWAYS_INLINE void ProcessorBase::wait_check()
{
    Processor::pause();
    // FIXME: Process SMP messages once we support SMP on riscv64; cf. x86_64
}

ALWAYS_INLINE Optional<u64> ProcessorBase::read_cycle_count()
{
    return RISCV64::CSR::read<RISCV64::CSR::Address::CYCLE>();
}

}
