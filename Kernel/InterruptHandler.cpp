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

#include <Kernel/InterruptHandler.h>
#include <Kernel/SharedInterruptHandler.h>

InterruptHandler::InterruptHandler(u8 irq)
    : m_irq_number(irq)
{
    SharedInterruptHandler::from(m_irq_number).register_handler(*this);
    disable_interrupts();
}

InterruptHandler::~InterruptHandler()
{
    disable_interrupts();
    SharedInterruptHandler::from(m_irq_number).unregister_handler(*this);
}

void InterruptHandler::enable_interrupts()
{
    m_enabled = true;
}

void InterruptHandler::change_irq_number(u8 irq_number)
{
    bool was_enabled = m_enabled;
    disable_interrupts();
    SharedInterruptHandler::from(m_irq_number).unregister_handler(*this);
    m_irq_number = irq_number;
    SharedInterruptHandler::from(m_irq_number).register_handler(*this);
    if (was_enabled)
        enable_interrupts();
}

void InterruptHandler::disable_interrupts()
{
    m_enabled = false;
}

InterruptHandler::Enabler::Enabler(InterruptHandler& handler)
    : m_handler(handler)
    , m_was_enabled(m_handler.is_enabled())
{
    m_handler.enable_interrupts();
}
InterruptHandler::Enabler::~Enabler()
{
    if (!m_was_enabled)
        m_handler.disable_interrupts();
}
