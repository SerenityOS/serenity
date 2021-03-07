/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Assertions.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/PIC.h>

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
