/*
 * Copyright (c) 2023, Pankaj R <dev@pankajraghav.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/InterruptManagement.h>
#include <Kernel/Arch/PCIMSI.h>
#include <Kernel/Debug.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>

namespace Kernel::PCI {

IRQHandler::IRQHandler(PCI::Device& device, u8 irq)
    : GenericInterruptHandler(irq)
    , device(device)
{
    auto type = device.get_interrupt_type();

    if (type == PCI::InterruptType::PIN)
        m_responsible_irq_controller = InterruptManagement::the().get_responsible_irq_controller(irq);

    if (is_registered())
        disable_irq();
}

bool IRQHandler::eoi()
{
    dbgln_if(IRQ_DEBUG, "EOI IRQ {}", interrupt_number());
    if (m_shared_with_others)
        return false;
    if (!m_responsible_irq_controller.is_null())
        m_responsible_irq_controller->eoi(*this);
    else
        msi_signal_eoi();
    return true;
}

void IRQHandler::enable_irq()
{
    dbgln_if(IRQ_DEBUG, "Enable IRQ {}", interrupt_number());
    if (!is_registered())
        register_interrupt_handler();
    m_enabled = true;
    if (m_shared_with_others)
        return;
    if (!m_responsible_irq_controller.is_null())
        m_responsible_irq_controller->enable(*this);
    else
        device.enable_interrupt(interrupt_number());
}

void IRQHandler::disable_irq()
{
    dbgln_if(IRQ_DEBUG, "Disable IRQ {}", interrupt_number());
    m_enabled = false;

    if (m_shared_with_others)
        return;
    if (!m_responsible_irq_controller.is_null())
        m_responsible_irq_controller->disable(*this);
    else
        device.disable_interrupt(interrupt_number());
}

bool IRQHandler::handle_interrupt()
{
    return handle_irq();
}

}
