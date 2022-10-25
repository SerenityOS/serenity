/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/InterruptManagement.h>
#include <Kernel/Arch/aarch64/RPi/InterruptController.h>

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
    m_interrupt_controllers.append(adopt_lock_ref(*new (nothrow) RPi::InterruptController));
}

u8 InterruptManagement::acquire_mapped_interrupt_number(u8 interrupt_number)
{
    return interrupt_number;
}

Vector<NonnullLockRefPtr<IRQController>> const& InterruptManagement::controllers()
{
    return m_interrupt_controllers;
}

NonnullLockRefPtr<IRQController> InterruptManagement::get_responsible_irq_controller(u8)
{
    // TODO: Support more interrupt controllers
    VERIFY(m_interrupt_controllers.size() == 1);
    return m_interrupt_controllers[0];
}

void InterruptManagement::enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)>)
{
    TODO_AARCH64();
}

}
