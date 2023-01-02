/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/InterruptController.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>

namespace Kernel::RPi {

// "7.5 Interrupts Registers"
// https://github.com/raspberrypi/documentation/files/1888662/BCM2837-ARM-Peripherals.-.Revised.-.V2-1.pdf
struct InterruptControllerRegisters {
    u32 irq_basic_pending;
    u32 irq_pending_1;
    u32 irq_pending_2;
    u32 fiq_control;

    u32 enable_irqs_1;
    u32 enable_irqs_2;
    u32 enable_basic_irqs;

    u32 disable_irqs_1;
    u32 disable_irqs_2;
    u32 disable_basic_irqs;
};

InterruptController::InterruptController()
    : m_registers(MMIO::the().peripheral<InterruptControllerRegisters>(0xB200))
{
}

void InterruptController::enable(GenericInterruptHandler const& handler)
{
    u8 interrupt_number = handler.interrupt_number();
    VERIFY(interrupt_number < 64);

    if (interrupt_number < 32)
        m_registers->enable_irqs_1 = m_registers->enable_irqs_1 | (1 << interrupt_number);
    else
        m_registers->enable_irqs_2 = m_registers->enable_irqs_2 | (1 << (interrupt_number - 32));
}

void InterruptController::disable(GenericInterruptHandler const& handler)
{
    u8 interrupt_number = handler.interrupt_number();
    VERIFY(interrupt_number < 64);

    if (interrupt_number < 32)
        m_registers->disable_irqs_1 = m_registers->disable_irqs_1 | (1 << interrupt_number);
    else
        m_registers->disable_irqs_2 = m_registers->disable_irqs_2 | (1 << (interrupt_number - 32));
}

void InterruptController::eoi(GenericInterruptHandler const&) const
{
    // NOTE: The interrupt controller cannot clear the interrupt, since it is basically just a big multiplexer.
    //       The interrupt should be cleared by the corresponding device driver, such as a timer or uart.
}

u64 InterruptController::pending_interrupts() const
{
    return ((u64)m_registers->irq_pending_2 << 32) | (u64)m_registers->irq_pending_1;
}

}
