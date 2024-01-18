/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/riscv64/IRQController.h>
#include <Kernel/Arch/riscv64/InterruptManagement.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>

namespace Kernel {

static InterruptManagement* s_interrupt_management;

bool InterruptManagement::initialized()
{
    return s_interrupt_management != nullptr;
}

InterruptManagement& InterruptManagement::the()
{
    VERIFY(InterruptManagement::initialized());
    return *s_interrupt_management;
}

void InterruptManagement::initialize()
{
    VERIFY(!InterruptManagement::initialized());
    s_interrupt_management = new InterruptManagement;

    the().find_controllers();
}

void InterruptManagement::find_controllers()
{
    // TODO: Once device tree support is in place, find interrupt controllers using that.
}

u8 InterruptManagement::acquire_mapped_interrupt_number(u8 original_irq)
{
    return original_irq;
}

Vector<NonnullLockRefPtr<IRQController>> const& InterruptManagement::controllers()
{
    return m_interrupt_controllers;
}

NonnullLockRefPtr<IRQController> InterruptManagement::get_responsible_irq_controller(size_t)
{
    TODO_RISCV64();
}

void InterruptManagement::enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)>)
{
    TODO_RISCV64();
}

}
