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

#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/SpuriousInterruptHandler.h>

namespace Kernel {

void SpuriousInterruptHandler::initialize(u8 interrupt_number)
{
    new SpuriousInterruptHandler(interrupt_number);
}

void SpuriousInterruptHandler::register_handler(GenericInterruptHandler&)
{
}
void SpuriousInterruptHandler::unregister_handler(GenericInterruptHandler&)
{
}

bool SpuriousInterruptHandler::eoi()
{
    // FIXME: Actually check if IRQ7 or IRQ15 are spurious, and if not, call EOI with the correct interrupt number.
    if (interrupt_number() == 15)
        m_responsible_irq_controller->eoi(7);
    return false;
}

SpuriousInterruptHandler::SpuriousInterruptHandler(u8 irq)
    : GenericInterruptHandler(irq)
    , m_responsible_irq_controller(InterruptManagement::the().get_responsible_irq_controller(irq))
{
}

SpuriousInterruptHandler::~SpuriousInterruptHandler()
{
}

void SpuriousInterruptHandler::handle_interrupt(RegisterState&)
{
    // FIXME: Actually check if IRQ7 or IRQ15 are spurious, and if not, call the real handler to handle the IRQ.
    kprintf("Spurious Interrupt, vector %d\n", interrupt_number());
}

void SpuriousInterruptHandler::enable_interrupt_vector()
{
    if (m_enabled)
        return;
    m_enabled = true;
    m_responsible_irq_controller->enable(interrupt_number());
}

void SpuriousInterruptHandler::disable_interrupt_vector()
{
    if (!m_enabled)
        return;
    m_enabled = false;
    m_responsible_irq_controller->disable(interrupt_number());
}

}
