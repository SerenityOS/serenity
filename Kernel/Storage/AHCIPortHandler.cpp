/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Storage/AHCIPortHandler.h>

namespace Kernel {

NonnullRefPtr<AHCIPortHandler> AHCIPortHandler::create(AHCIController& controller, u8 irq, AHCI::MaskedBitField taken_ports)
{
    return adopt(*new AHCIPortHandler(controller, irq, taken_ports));
}

AHCIPortHandler::AHCIPortHandler(AHCIController& controller, u8 irq, AHCI::MaskedBitField taken_ports)
    : IRQHandler(irq)
    , m_parent_controller(controller)
    , m_taken_ports(taken_ports)
    , m_pending_ports_interrupts(create_pending_ports_interrupts_bitfield())
{
    // FIXME: Use the number of taken ports to determine how many pages we should allocate.
    for (size_t index = 0; index < (((size_t)AHCI::Limits::MaxPorts * 512) / PAGE_SIZE); index++) {
        m_identify_metadata_pages.append(MM.allocate_supervisor_physical_page().release_nonnull());
    }
    // Clear pending interrupts, if there are any!
    m_pending_ports_interrupts.set_all();
    enable_irq();
    for (auto index : taken_ports.to_vector()) {
        auto port = AHCIPort::create(*this, static_cast<volatile AHCI::PortRegisters&>(controller.hba().port_regs[index]), index);
        m_handled_ports.set(index, port);
        port->reset();
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

void AHCIPortHandler::handle_irq(const RegisterState&)
{
    for (auto port_index : m_pending_ports_interrupts.to_vector()) {
        auto port = m_handled_ports.get(port_index);
        VERIFY(port.has_value());
        port.value()->handle_interrupt();
        // We do this to clear the pending interrupt after we handled it.
        m_pending_ports_interrupts.set_at(port_index);
    }
}

}
