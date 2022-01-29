/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/CommandLine.h>
#include <Kernel/Storage/ATA/AHCIPortHandler.h>

namespace Kernel {

NonnullRefPtr<AHCIPortHandler> AHCIPortHandler::create(AHCIController& controller, u8 irq, AHCI::MaskedBitField taken_ports)
{
    return adopt_ref(*new AHCIPortHandler(controller, irq, taken_ports));
}

AHCIPortHandler::AHCIPortHandler(AHCIController& controller, u8 irq, AHCI::MaskedBitField taken_ports)
    : IRQHandler(irq)
    , m_parent_controller(controller)
    , m_taken_ports(taken_ports)
    , m_pending_ports_interrupts(create_pending_ports_interrupts_bitfield())
{
    // FIXME: Use the number of taken ports to determine how many pages we should allocate.
    for (size_t index = 0; index < (((size_t)AHCI::Limits::MaxPorts * 512) / PAGE_SIZE); index++) {
        m_identify_metadata_pages.append(MM.allocate_supervisor_physical_page().release_value_but_fixme_should_propagate_errors());
    }

    dbgln_if(AHCI_DEBUG, "AHCI Port Handler: IRQ {}", irq);

    // Clear pending interrupts, if there are any!
    m_pending_ports_interrupts.set_all();
    enable_irq();

    if (kernel_command_line().ahci_reset_mode() == AHCIResetMode::Aggressive) {
        for (auto index : taken_ports.to_vector()) {
            auto port = AHCIPort::create(*this, static_cast<volatile AHCI::PortRegisters&>(controller.hba().port_regs[index]), index);
            m_handled_ports.set(index, port);
            port->reset();
        }
        return;
    }
    for (auto index : taken_ports.to_vector()) {
        auto port = AHCIPort::create(*this, static_cast<volatile AHCI::PortRegisters&>(controller.hba().port_regs[index]), index);
        m_handled_ports.set(index, port);
        port->initialize_without_reset();
    }
}

void AHCIPortHandler::enumerate_ports(Function<void(const AHCIPort&)> callback) const
{
    for (auto& port : m_handled_ports) {
        callback(*port.value);
    }
}

RefPtr<AHCIPort> AHCIPortHandler::port_at_index(u32 port_index) const
{
    VERIFY(m_taken_ports.is_set_at(port_index));
    auto it = m_handled_ports.find(port_index);
    if (it == m_handled_ports.end())
        return nullptr;
    return (*it).value;
}

PhysicalAddress AHCIPortHandler::get_identify_metadata_physical_region(u32 port_index) const
{
    dbgln_if(AHCI_DEBUG, "AHCI Port Handler: Get identify metadata physical address of port {} - {}", port_index, (port_index * 512) / PAGE_SIZE);
    return m_identify_metadata_pages[(port_index * 512) / PAGE_SIZE].paddr().offset((port_index * 512) % PAGE_SIZE);
}

AHCI::MaskedBitField AHCIPortHandler::create_pending_ports_interrupts_bitfield() const
{
    return AHCI::MaskedBitField((volatile u32&)m_parent_controller->hba().control_regs.is, m_taken_ports.bit_mask());
}

AHCI::HBADefinedCapabilities AHCIPortHandler::hba_capabilities() const
{
    return m_parent_controller->hba_capabilities();
}

AHCIPortHandler::~AHCIPortHandler()
{
}

bool AHCIPortHandler::handle_irq(const RegisterState&)
{
    dbgln_if(AHCI_DEBUG, "AHCI Port Handler: IRQ received");
    if (m_pending_ports_interrupts.is_zeroed())
        return false;
    for (auto port_index : m_pending_ports_interrupts.to_vector()) {
        auto port = m_handled_ports.get(port_index);
        VERIFY(port.has_value());
        dbgln_if(AHCI_DEBUG, "AHCI Port Handler: Handling IRQ for port {}", port_index);
        port.value()->handle_interrupt();
        // We do this to clear the pending interrupt after we handled it.
        m_pending_ports_interrupts.set_at(port_index);
    }
    return true;
}

}
