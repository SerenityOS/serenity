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

UNMAP_AFTER_INIT void SpuriousInterruptHandler::initialize(u8 interrupt_number)
{
    auto* handler = new SpuriousInterruptHandler(interrupt_number);
    handler->register_interrupt_handler();
}

void SpuriousInterruptHandler::register_handler(GenericInterruptHandler& handler)
{
    VERIFY(!m_real_handler);
    m_real_handler = &handler;
}
void SpuriousInterruptHandler::unregister_handler(GenericInterruptHandler&)
{
    TODO();
}

bool SpuriousInterruptHandler::eoi()
{
    // Actually check if IRQ7 or IRQ15 are spurious, and if not, call EOI with the correct interrupt number.
    if (m_real_irq) {
        m_responsible_irq_controller->eoi(*this);
        m_real_irq = false; // return to default state!
        return true;
    }
    m_responsible_irq_controller->spurious_eoi(*this);
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

void SpuriousInterruptHandler::handle_interrupt(const RegisterState& state)
{
    // Actually check if IRQ7 or IRQ15 are spurious, and if not, call the real handler to handle the IRQ.
    if (m_responsible_irq_controller->get_isr() & (1 << interrupt_number())) {
        m_real_irq = true; // remember that we had a real IRQ, when EOI later!
        m_real_handler->increment_invoking_counter();
        m_real_handler->handle_interrupt(state);
        return;
    }
    dbgln("Spurious interrupt, vector {}", interrupt_number());
}

void SpuriousInterruptHandler::enable_interrupt_vector()
{
    if (m_enabled)
        return;
    m_enabled = true;
    m_responsible_irq_controller->enable(*this);
}

void SpuriousInterruptHandler::disable_interrupt_vector()
{
    VERIFY(!m_real_irq); // this flag should not be set when we call this method
    if (!m_enabled)
        return;
    m_enabled = false;
    m_responsible_irq_controller->disable(*this);
}

const char* SpuriousInterruptHandler::controller() const
{
    if (m_responsible_irq_controller->type() == IRQControllerType::i82093AA)
        return "";
    return m_responsible_irq_controller->model();
}
}
