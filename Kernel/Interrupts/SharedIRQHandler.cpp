/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/InterruptManagement.h>
#include <Kernel/Debug.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>
#include <Kernel/Library/Assertions.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT void SharedIRQHandler::initialize(u8 interrupt_number)
{
    auto* handler = new SharedIRQHandler(interrupt_number);
    handler->register_interrupt_handler();
    handler->disable_interrupt_vector();
}

void SharedIRQHandler::register_handler(GenericInterruptHandler& handler)
{
    dbgln_if(INTERRUPT_DEBUG, "Interrupt Handler registered @ Shared Interrupt Handler {}", interrupt_number());
    m_handlers.with([&handler](auto& list) { list.append(handler); });
    enable_interrupt_vector();
}
void SharedIRQHandler::unregister_handler(GenericInterruptHandler& handler)
{
    dbgln_if(INTERRUPT_DEBUG, "Interrupt Handler unregistered @ Shared Interrupt Handler {}", interrupt_number());
    m_handlers.with([&handler, this](auto& list) {
        list.remove(handler);
        if (list.is_empty())
            disable_interrupt_vector();
    });
}

bool SharedIRQHandler::eoi()
{
    dbgln_if(INTERRUPT_DEBUG, "EOI IRQ {}", interrupt_number());
    m_responsible_irq_controller->eoi(*this);
    return true;
}

void SharedIRQHandler::enumerate_handlers(Function<void(GenericInterruptHandler&)>& callback)
{
    m_handlers.for_each([&](auto& handler) { callback(handler); });
}

SharedIRQHandler::SharedIRQHandler(u8 irq)
    : GenericInterruptHandler(irq)
    , m_responsible_irq_controller(InterruptManagement::the().get_responsible_irq_controller(irq))
{
    dbgln_if(INTERRUPT_DEBUG, "Shared Interrupt Handler registered @ {}", interrupt_number());
}

SharedIRQHandler::~SharedIRQHandler()
{
    dbgln_if(INTERRUPT_DEBUG, "Shared Interrupt Handler unregistered @ {}", interrupt_number());
    disable_interrupt_vector();
}

bool SharedIRQHandler::handle_interrupt()
{
    VERIFY_INTERRUPTS_DISABLED();

    if constexpr (INTERRUPT_DEBUG) {
        dbgln("Interrupt @ {}", interrupt_number());
        dbgln("Interrupt Handlers registered - {}", sharing_devices_count());
    }
    int i = 0;
    bool was_handled = false;
    m_handlers.for_each([&](auto& handler) {
        dbgln_if(INTERRUPT_DEBUG, "Going for Interrupt Handling @ {}, Shared Interrupt {}", i, interrupt_number());
        if (handler.handle_interrupt()) {
            handler.increment_call_count();
            was_handled = true;
        }
        dbgln_if(INTERRUPT_DEBUG, "Going for Interrupt Handling @ {}, Shared Interrupt {} - End", i, interrupt_number());
        i++;
    });

    return was_handled;
}

void SharedIRQHandler::enable_interrupt_vector()
{
    if (m_enabled)
        return;
    m_enabled = true;
    m_responsible_irq_controller->enable(*this);
}

void SharedIRQHandler::disable_interrupt_vector()
{
    if (!m_enabled)
        return;
    m_enabled = false;
    m_responsible_irq_controller->disable(*this);
}

}
