/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NeverDestroyed.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/riscv64/InterruptManagement.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>
#include <Kernel/Library/Panic.h>

namespace Kernel {

static NeverDestroyed<Vector<NonnullLockRefPtr<IRQController>>> s_interrupt_controllers;
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

    if (s_interrupt_controllers->is_empty())
        PANIC("InterruptManagement: No supported interrupt controller was found");
}

ErrorOr<void> InterruptManagement::register_interrupt_controller(NonnullLockRefPtr<IRQController> interrupt_controller)
{
    // This function has to be called before InterruptManagement is initialized,
    // as we do not support dynamic registration of interrupt controllers.
    VERIFY(!initialized());

    return s_interrupt_controllers->try_append(move(interrupt_controller));
}

u8 InterruptManagement::acquire_mapped_interrupt_number(u8 original_irq)
{
    return original_irq;
}

Vector<NonnullLockRefPtr<IRQController>> const& InterruptManagement::controllers()
{
    return *s_interrupt_controllers;
}

NonnullLockRefPtr<IRQController> InterruptManagement::get_responsible_irq_controller(size_t)
{
    // TODO: Support more interrupt controllers
    VERIFY(s_interrupt_controllers->size() == 1);
    return (*s_interrupt_controllers)[0];
}

void InterruptManagement::enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)> callback)
{
    for (size_t i = 0; i < GENERIC_INTERRUPT_HANDLERS_COUNT; i++) {
        auto& handler = get_interrupt_handler(i);
        if (handler.type() == HandlerType::SharedIRQHandler) {
            static_cast<SharedIRQHandler&>(handler).enumerate_handlers(callback);
            continue;
        }
        if (handler.type() != HandlerType::UnhandledInterruptHandler)
            callback(handler);
    }
}

}
