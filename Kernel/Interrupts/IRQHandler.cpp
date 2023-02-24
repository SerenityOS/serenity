/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/InterruptManagement.h>
#include <Kernel/Debug.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Interrupts/InterruptDisabler.h>

namespace Kernel {

IRQHandler::IRQHandler(u8 irq)
    : GenericInterruptHandler(irq)
    , m_responsible_irq_controller(InterruptManagement::the().get_responsible_irq_controller(irq))
{
    if (is_registered())
        disable_irq();
}

IRQHandler::~IRQHandler() = default;

bool IRQHandler::eoi()
{
    dbgln_if(IRQ_DEBUG, "EOI IRQ {}", interrupt_number());
    if (!m_shared_with_others) {
        m_responsible_irq_controller->eoi(*this);
        return true;
    }
    return false;
}

void IRQHandler::enable_irq()
{
    dbgln_if(IRQ_DEBUG, "Enable IRQ {}", interrupt_number());
    if (!is_registered())
        register_interrupt_handler();
    m_enabled = true;
    if (!m_shared_with_others)
        m_responsible_irq_controller->enable(*this);
}

void IRQHandler::disable_irq()
{
    dbgln_if(IRQ_DEBUG, "Disable IRQ {}", interrupt_number());
    m_enabled = false;
    if (!m_shared_with_others)
        m_responsible_irq_controller->disable(*this);
}

void IRQHandler::change_irq_number(u8 irq)
{
    InterruptDisabler disabler;
    change_interrupt_number(irq);
    m_responsible_irq_controller = InterruptManagement::the().get_responsible_irq_controller(irq);
}

}
