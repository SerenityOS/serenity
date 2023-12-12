/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Types.h>

#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>

namespace Kernel {

void dump_registers(RegisterState const&)
{
}

// FIXME: Share the code below with Arch/x86_64/Interrupts.cpp
//        While refactoring, the interrupt handlers can also be moved into the InterruptManagement class.
GenericInterruptHandler& get_interrupt_handler(u8)
{
    TODO_RISCV64();
}

// Sets the reserved flag on `number_of_irqs` if it finds unused interrupt handler on
// a contiguous range.
ErrorOr<u8> reserve_interrupt_handlers(u8)
{
    TODO_RISCV64();
}

void register_generic_interrupt_handler(u8, GenericInterruptHandler&)
{
    TODO_RISCV64();
}

void unregister_generic_interrupt_handler(u8, GenericInterruptHandler&)
{
}

void initialize_interrupts()
{
    TODO_RISCV64();
}

}
