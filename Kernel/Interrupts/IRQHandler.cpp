/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Interrupts/InterruptManagement.h>

//#define IRQ_DEBUG

namespace Kernel {

IRQHandler::IRQHandler(u8 irq)
    : GenericInterruptHandler(irq)
    , m_responsible_irq_controller(InterruptManagement::the().get_responsible_irq_controller(irq))
{
}

IRQHandler::~IRQHandler()
{
}

bool IRQHandler::eoi()
{
#ifdef IRQ_DEBUG
    dbg() << "EOI IRQ " << interrupt_number();
#endif
    if (!m_shared_with_others) {
        ASSERT(!m_responsible_irq_controller.is_null());
        m_responsible_irq_controller->eoi(interrupt_number());
        return true;
    }
    return false;
}

void IRQHandler::enable_irq()
{
#ifdef IRQ_DEBUG
    dbg() << "Enable IRQ " << interrupt_number();
#endif
    if (!m_shared_with_others)
        m_responsible_irq_controller->enable(interrupt_number());
    else
        m_enabled = true;
}

void IRQHandler::disable_irq()
{
#ifdef IRQ_DEBUG
    dbg() << "Disable IRQ " << interrupt_number();
#endif
    if (!m_shared_with_others)
        m_responsible_irq_controller->disable(interrupt_number());
    else
        m_enabled = false;
}

void IRQHandler::change_irq_number(u8 irq)
{
    InterruptDisabler disabler;
    change_interrupt_number(irq);
    m_responsible_irq_controller = InterruptManagement::the().get_responsible_irq_controller(irq);
}

}
