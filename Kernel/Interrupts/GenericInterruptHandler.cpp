/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/Interrupts.h>
#include <Kernel/Assertions.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/InterruptManagement.h>

namespace Kernel {
GenericInterruptHandler& GenericInterruptHandler::from(u8 interrupt_number)
{
    return get_interrupt_handler(interrupt_number);
}

GenericInterruptHandler::GenericInterruptHandler(u8 interrupt_number, bool disable_remap)
    : m_interrupt_number(interrupt_number)
    , m_disable_remap(disable_remap)
{
    // NOTE: We cannot register or unregister the handler while the object
    // is being constructed or deconstructed!
}

void GenericInterruptHandler::will_be_destroyed()
{
    // This will be called for RefCounted interrupt handlers before the
    // object is being destroyed. As soon as the destructor is invoked
    // it is no longer advisable to unregister the handler (which causes
    // calls to virtual functions), so let's do this right before
    // invoking it
    unregister_interrupt_handler();
}

void GenericInterruptHandler::register_interrupt_handler()
{
    if (m_registered)
        return;
    if (m_disable_remap)
        register_generic_interrupt_handler(m_interrupt_number, *this);
    else
        register_generic_interrupt_handler(InterruptManagement::acquire_mapped_interrupt_number(m_interrupt_number), *this);
    m_registered = true;
}

void GenericInterruptHandler::unregister_interrupt_handler()
{
    if (!m_registered)
        return;
    if (m_disable_remap)
        unregister_generic_interrupt_handler(m_interrupt_number, *this);
    else
        unregister_generic_interrupt_handler(InterruptManagement::acquire_mapped_interrupt_number(m_interrupt_number), *this);
    m_registered = false;
}

void GenericInterruptHandler::change_interrupt_number(u8 number)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(!m_disable_remap);
    if (m_registered) {
        unregister_generic_interrupt_handler(InterruptManagement::acquire_mapped_interrupt_number(interrupt_number()), *this);
        m_registered = false;
    }
    m_interrupt_number = number;
    register_generic_interrupt_handler(InterruptManagement::acquire_mapped_interrupt_number(interrupt_number()), *this);
}

}
