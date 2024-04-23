/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/riscv64/Interrupts/PLIC.h>

namespace Kernel {

UNMAP_AFTER_INIT PLIC::PLIC(PhysicalAddress address, size_t size, u32 interrupt_count)
    : m_registers(Memory::map_typed<RegisterMap volatile>(address, size, Memory::Region::Access::ReadWrite).release_value_but_fixme_should_propagate_errors())
    , m_interrupt_count(interrupt_count)
{
    VERIFY(m_interrupt_count < 256); // TODO: Serenity currently only supports up to 256 unique interrupts, but the PLIC supports up to 1024
    initialize();
}

UNMAP_AFTER_INIT void PLIC::initialize()
{
    for (auto i = 1u; i < m_interrupt_count; ++i) {
        // Initialize all interrupt priorities to 1 (0 means never-interrupt)
        m_registers->interrupt_priority[i] = 1;
    }
    for (auto i = 0u; i <= ((m_interrupt_count - 1) >> 5); ++i) {
        // Initialize all interrupt sources to disabled
        m_registers->interrupt_enable_bitmap[interrupt_context][i] = 0;
    }
    // Initialize priority-threshold to 0 (accept any interrupt of priority 1 or above)
    m_registers->contexts[interrupt_context].priority_threshold = 0;
    // Enable external interrupts in the current hart
    RISCV64::CSR::set_bits(RISCV64::CSR::Address::SIE, 1 << (to_underlying(RISCV64::CSR::SCAUSE::SupervisorExternalInterrupt) & ~RISCV64::CSR::SCAUSE_INTERRUPT_MASK));
}

void PLIC::enable(GenericInterruptHandler const& handler)
{
    auto interrupt_number = handler.interrupt_number();
    VERIFY(interrupt_number > 0); // Interrupt number 0 is reserved to mean no-interrupt
    m_registers->interrupt_enable_bitmap[interrupt_context][interrupt_number >> 5] |= 1u << (interrupt_number & 0x1F);
}

void PLIC::disable(GenericInterruptHandler const& handler)
{
    auto interrupt_number = handler.interrupt_number();
    VERIFY(interrupt_number > 0); // Interrupt number 0 is reserved to mean no-interrupt
    m_registers->interrupt_enable_bitmap[interrupt_context][interrupt_number >> 5] &= ~(1u << (interrupt_number & 0x1F));
}

void PLIC::eoi(GenericInterruptHandler const& handler)
{
    m_registers->contexts[interrupt_context].claim_complete = handler.interrupt_number();
}

u8 PLIC::pending_interrupt() const
{
    return m_registers->contexts[interrupt_context].claim_complete;
}

}
