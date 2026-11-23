/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/riscv64/SBI.h>
#include <Kernel/Arch/riscv64/Timer.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Library/Panic.h>

namespace Kernel::RISCV64 {

static Timer* s_the;

Timer::Timer()
    : TimeSource(TimeSource::CounterSize::Bits64)
{
    if (DeviceTree::is_present()) {
        m_frequency = DeviceTree::get().resolve_property("/cpus/timebase-frequency"sv).value().as<u32>();
    } else if (ACPI::is_enabled()) {
        auto rhct_table_paddr = ACPI::Parser::the()->find_table("RHCT"sv);
        if (!rhct_table_paddr.has_value())
            PANIC("No RHCT ACPI table found; unable to determine RISC-V timer frequency");

        auto rhct = Memory::map_typed<::ACPI::Structures::RHCT>(rhct_table_paddr.value()).release_value_but_fixme_should_propagate_errors();

        m_frequency = rhct->time_base_frequency;
    } else {
        PANIC("Neither devicetree present nor ACPI enabled; unable to determine RISC-V timer frequency");
    }

    m_interrupt_interval = m_frequency / OPTIMAL_TICKS_PER_SECOND_RATE;

    set_compare(read_counter() + m_interrupt_interval);
    RISCV64::CSR::set_bits<RISCV64::CSR::Address::SIE>(CSR::SIE::SupervisorTimerInterrupt);
}

NonnullLockRefPtr<Timer> Timer::initialize()
{
    VERIFY(!s_the);
    auto timer = adopt_lock_ref(*new Timer);
    s_the = timer.ptr();
    return timer;
}

bool Timer::is_initialized()
{
    return s_the != nullptr;
}

Timer& Timer::the()
{
    VERIFY(s_the);
    return *s_the;
}

u64 Timer::read_counter_raw() const
{
    return RISCV64::CSR::read<RISCV64::CSR::Address::TIME>();
}

void Timer::handle_interrupt()
{
    if (m_callback)
        m_callback();
    set_compare(read_counter() + m_interrupt_interval);
}

void Timer::disable()
{
    RISCV64::CSR::clear_bits<RISCV64::CSR::Address::SIE>(CSR::SIE::SupervisorTimerInterrupt);
}

void Timer::set_compare(u64 compare)
{
    if (Processor::current().has_feature(CPUFeature::Sstc)) {
        RISCV64::CSR::write<RISCV64::CSR::Address::STIMECMP>(compare);
        return;
    }

    if (SBI::Timer::set_timer(compare).is_error())
        MUST(SBI::Legacy::set_timer(compare));
}

}
