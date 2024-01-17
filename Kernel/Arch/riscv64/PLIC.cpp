/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/riscv64/PLIC.h>

namespace Kernel::RISCV64 {

// FIXME: Once we support device tree parsing get the correct IDs for all supervisor mode contexts of all harts
//        via the "interrupts-extended" property of the PLIC node.

// FIXME: Read the count of interrupt sources from the "riscv,ndev" property.
static constexpr size_t QEMU_VIRT_SOURCE_COUNT = 95;

// FIXME: 1 is the context for supervisor mode on hart 0 for the QEMU virt machine's PLIC.
//        As we don't support SMP on riscv64 yet, using only this context ID should be OK for now.
static constexpr size_t QEMU_VIRT_HART_0_SUPERVISOR_CONTEXT_ID = 1;

ErrorOr<NonnullLockRefPtr<PLIC>> PLIC::try_to_initialize(PhysicalAddress paddr)
{
    auto regs = TRY(Memory::map_typed_writable<PLICRegisters volatile>(paddr));
    auto plic = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) PLIC(move(regs))));
    return plic;
}

PLIC::PLIC(Memory::TypedMapping<PLICRegisters volatile>&& regs)
    : m_regs(move(regs))
{
    // Disable all interrupt sources by default.
    for (size_t i = 0; i < (QEMU_VIRT_SOURCE_COUNT + (PLIC_SOURCE_BITS_PER_ENABLE_OR_PENDING_REGISTER - 1)) / PLIC_SOURCE_BITS_PER_ENABLE_OR_PENDING_REGISTER; ++i)
        m_regs->enable_for_context[QEMU_VIRT_HART_0_SUPERVISOR_CONTEXT_ID][i] = 0xffff'ffff;

    // Set all interrupt priorities to 1 (the lowest priority).
    // Note: Interrupt source 0 doesn't exist, so skip it.
    for (size_t i = 1; i < QEMU_VIRT_SOURCE_COUNT; ++i)
        m_regs->priority[i] = 5;

    // Set the priority threshould to 0 to allow all priorities.
    m_regs->context[QEMU_VIRT_HART_0_SUPERVISOR_CONTEXT_ID].priority_threshold = 0;

    RISCV64::CSR::set_bits(RISCV64::CSR::Address::SIE, to_underlying(CSR::SCAUSE::SupervisorExternalInterrupt) & ~CSR::SCAUSE_INTERRUPT_MASK);
}

void PLIC::enable(GenericInterruptHandler const& handler)
{
    u8 interrupt_number = handler.interrupt_number();

    // FIXME: support interrupt numbers above 255
    // VERIFY(interrupt_number < SOURCE_COUNT);

    m_regs->enable_for_context[QEMU_VIRT_HART_0_SUPERVISOR_CONTEXT_ID][interrupt_number / PLIC_SOURCE_BITS_PER_ENABLE_OR_PENDING_REGISTER] |= ((1 << interrupt_number) % PLIC_SOURCE_BITS_PER_ENABLE_OR_PENDING_REGISTER);
}

void PLIC::disable(GenericInterruptHandler const& handler)
{
    u8 interrupt_number = handler.interrupt_number();

    // FIXME: support interrupt numbers above 255
    // VERIFY(interrupt_number < SOURCE_COUNT);

    m_regs->enable_for_context[QEMU_VIRT_HART_0_SUPERVISOR_CONTEXT_ID][interrupt_number / PLIC_SOURCE_BITS_PER_ENABLE_OR_PENDING_REGISTER] &= ~((1 << interrupt_number) % PLIC_SOURCE_BITS_PER_ENABLE_OR_PENDING_REGISTER);
}

size_t PLIC::claim() const
{
    return m_regs->context[QEMU_VIRT_HART_0_SUPERVISOR_CONTEXT_ID].claim_complete;
}

void PLIC::eoi(GenericInterruptHandler const& handler) const
{
    dbgln("EOI {}", handler.interrupt_number());
    m_regs->context[QEMU_VIRT_HART_0_SUPERVISOR_CONTEXT_ID].claim_complete = handler.interrupt_number();
}

}
