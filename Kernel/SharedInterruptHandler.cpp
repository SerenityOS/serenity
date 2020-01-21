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
#include <Kernel/Arch/i386/PIC.h>
#include <Kernel/InterruptHandler.h>
#include <Kernel/SharedInterruptHandler.h>

//#define INTERRUPT_DEBUG

SharedInterruptHandler& SharedInterruptHandler::from(u8 interrupt_number)
{
    return get_interrupt_handler(interrupt_number);
}

void SharedInterruptHandler::initialize(u8 interrupt_number)
{
    new SharedInterruptHandler(interrupt_number);
}

void SharedInterruptHandler::register_handler(InterruptHandler& handler)
{
#ifdef INTERRUPT_DEBUG
    kprintf("Interrupt Handler registered @ Shared Interrupt Handler %d\n", m_interrupt_number);
#endif
    m_handlers.set(&handler);
    enable_interrupt_vector();
}
void SharedInterruptHandler::unregister_handler(InterruptHandler& handler)
{
#ifdef INTERRUPT_DEBUG
    kprintf("Interrupt Handler unregistered @ Shared Interrupt Handler %d\n", m_interrupt_number);
#endif
    m_handlers.remove(&handler);
    if (m_handlers.is_empty())
        disable_interrupt_vector();
}

SharedInterruptHandler::SharedInterruptHandler(u8 interrupt_number)
    : m_interrupt_number(interrupt_number)
    , m_enabled(true)
{
#ifdef INTERRUPT_DEBUG
    kprintf("Shared Interrupt Handler registered @ %d\n", m_interrupt_number);
#endif
    register_shared_interrupt_handler(m_interrupt_number, *this);
    disable_interrupt_vector();
}

SharedInterruptHandler::~SharedInterruptHandler()
{
#ifdef INTERRUPT_DEBUG
    kprintf("Shared Interrupt Handler unregistered @ %d\n", m_interrupt_number);
#endif
    disable_interrupt_vector();
    unregister_shared_interrupt_handler(m_interrupt_number, *this);
}

void SharedInterruptHandler::handle_interrupt()
{
#ifdef INTERRUPT_DEBUG
    kprintf("Interrupt @ %d\n", m_interrupt_number);
    kprintf("Interrupt Handlers registered - %d\n", m_handlers.size());
#endif
    int i = 0;
    for (auto* handler : m_handlers) {
#ifdef INTERRUPT_DEBUG
        kprintf("Going for Interrupt Handling @ %d, Shared Interrupt %d\n", i, m_interrupt_number);
#endif
        ASSERT(handler != nullptr);
        if (handler->is_enabled())
            handler->handle_interrupt();

#ifdef INTERRUPT_DEBUG
        kprintf("Going for Interrupt Handling @ %d, Shared Interrupt %d - End\n", i, m_interrupt_number);
#endif
        i++;
    }
    // FIXME: Determine if we use IRQs or MSIs (in the future) to send EOI...
}

void SharedInterruptHandler::enable_interrupt_vector()
{
    if (m_enabled)
        return;
    m_enabled = true;
    // FIXME: Determine if we use IRQs or MSIs (in the future) to enable the interrupt vector...
    PIC::enable(m_interrupt_number);
}

void SharedInterruptHandler::disable_interrupt_vector()
{
    if (!m_enabled)
        return;
    m_enabled = false;
    // FIXME: Determine if we use IRQs or MSIs (in the future) to disable the interrupt vector...
    PIC::disable(m_interrupt_number);
}
