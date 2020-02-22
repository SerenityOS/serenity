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

#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Assertions.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/PIC.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>

//#define INTERRUPT_DEBUG
namespace Kernel {

void SharedIRQHandler::initialize(u8 interrupt_number)
{
    new SharedIRQHandler(interrupt_number);
}

void SharedIRQHandler::register_handler(GenericInterruptHandler& handler)
{
#ifdef INTERRUPT_DEBUG
    kprintf("Interrupt Handler registered @ Shared Interrupt Handler %d\n", m_interrupt_number);
#endif
    m_handlers.set(&handler);
    enable_interrupt_vector();
}
void SharedIRQHandler::unregister_handler(GenericInterruptHandler& handler)
{
#ifdef INTERRUPT_DEBUG
    kprintf("Interrupt Handler unregistered @ Shared Interrupt Handler %d\n", m_interrupt_number);
#endif
    m_handlers.remove(&handler);
    if (m_handlers.is_empty())
        disable_interrupt_vector();
}

bool SharedIRQHandler::eoi()
{
#ifdef INTERRUPT_DEBUG
    dbg() << "EOI IRQ " << interrupt_number();
#endif
    InterruptManagement::the().eoi(interrupt_number());
    return true;
}

SharedIRQHandler::SharedIRQHandler(u8 irq)
    : GenericInterruptHandler(irq)
{
#ifdef INTERRUPT_DEBUG
    kprintf("Shared Interrupt Handler registered @ %d\n", m_interrupt_number);
#endif
    register_generic_interrupt_handler(irq, *this);
    disable_interrupt_vector();
}

SharedIRQHandler::~SharedIRQHandler()
{
#ifdef INTERRUPT_DEBUG
    kprintf("Shared Interrupt Handler unregistered @ %d\n", interrupt_number());
#endif
    disable_interrupt_vector();
    unregister_generic_interrupt_handler(interrupt_number(), *this);
}

void SharedIRQHandler::handle_interrupt(RegisterState& regs)
{
    ASSERT_INTERRUPTS_DISABLED();
    increment_invoking_counter();
#ifdef INTERRUPT_DEBUG
    dbg() << "Interrupt @ " << interrupt_number();
    dbg() << "Interrupt Handlers registered - " << m_handlers.size();
#endif
    int i = 0;
    for (auto* handler : m_handlers) {
#ifdef INTERRUPT_DEBUG
        dbg() << "Going for Interrupt Handling @ " << i << ", Shared Interrupt " << interrupt_number();
#endif
        ASSERT(handler != nullptr);
        if (handler->is_enabled()) {
            handler->increment_invoking_counter();
            handler->handle_interrupt(regs);
        }

#ifdef INTERRUPT_DEBUG
        dbg() << "Going for Interrupt Handling @ " << i << ", Shared Interrupt " << interrupt_number() << " - End";
#endif
        i++;
    }
}

void SharedIRQHandler::enable_interrupt_vector()
{
    if (m_enabled)
        return;
    m_enabled = true;
    InterruptManagement::the().enable(interrupt_number());
}

void SharedIRQHandler::disable_interrupt_vector()
{
    if (!m_enabled)
        return;
    m_enabled = false;
    InterruptManagement::the().disable(interrupt_number());
}

}
