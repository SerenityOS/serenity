/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Interrupts/UnhandledInterruptHandler.h>
#include <Kernel/Library/Panic.h>

namespace Kernel {
UnhandledInterruptHandler::UnhandledInterruptHandler(u8 interrupt_vector)
    : GenericInterruptHandler(interrupt_vector)
{
}

bool UnhandledInterruptHandler::handle_interrupt()
{
    PANIC("Interrupt: Unhandled vector {} was invoked for handle_interrupt(RegisterState&).", interrupt_number());
}

[[noreturn]] bool UnhandledInterruptHandler::eoi()
{
    PANIC("Interrupt: Unhandled vector {} was invoked for eoi().", interrupt_number());
}

UnhandledInterruptHandler::~UnhandledInterruptHandler() = default;
}
