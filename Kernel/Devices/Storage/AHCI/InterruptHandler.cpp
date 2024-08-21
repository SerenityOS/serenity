/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Storage/AHCI/InterruptHandler.h>

namespace Kernel {

UNMAP_AFTER_INIT ErrorOr<NonnullOwnPtr<AHCIInterruptHandler>> AHCIInterruptHandler::create(AHCIController& controller, u8 irq, AHCI::MaskedBitField taken_ports)
{
    auto port_handler = TRY(adopt_nonnull_own_or_enomem(new (nothrow) AHCIInterruptHandler(controller, irq, taken_ports)));
    port_handler->allocate_resources_and_initialize_ports();
    return port_handler;
}

void AHCIInterruptHandler::allocate_resources_and_initialize_ports()
{
    // Clear pending interrupts, if there are any!
    m_pending_ports_interrupts.set_all();
    enable_irq();
}

UNMAP_AFTER_INIT AHCIInterruptHandler::AHCIInterruptHandler(AHCIController& controller, u8 irq, AHCI::MaskedBitField taken_ports)
    : PCI::IRQHandler(controller, irq)
    , m_parent_controller(controller)
    , m_taken_ports(taken_ports)
    , m_pending_ports_interrupts(create_pending_ports_interrupts_bitfield())
{
    dbgln_if(AHCI_DEBUG, "AHCI Port Handler: IRQ {}", irq);
}

AHCI::MaskedBitField AHCIInterruptHandler::create_pending_ports_interrupts_bitfield() const
{
    return AHCI::MaskedBitField((u32 volatile&)m_parent_controller->hba().control_regs.is, m_taken_ports.bit_mask());
}

AHCIInterruptHandler::~AHCIInterruptHandler() = default;

bool AHCIInterruptHandler::handle_irq()
{
    dbgln_if(AHCI_DEBUG, "AHCI Port Handler: IRQ received");
    if (m_pending_ports_interrupts.is_zeroed())
        return false;
    for (auto port_index : m_pending_ports_interrupts.to_vector()) {
        dbgln_if(AHCI_DEBUG, "AHCI Port Handler: Handling IRQ for port {}", port_index);
        m_parent_controller->handle_interrupt_for_port({}, port_index);
        // We do this to clear the pending interrupt after we handled it.
        m_pending_ports_interrupts.set_at(port_index);
    }
    return true;
}

}
