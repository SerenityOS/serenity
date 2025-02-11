/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Platform.h>
#include <AK/Types.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Interrupts.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/Interrupts.h>
#endif

namespace Kernel {

class GenericInterruptHandler;

GenericInterruptHandler& get_interrupt_handler(u8 interrupt_number);
void register_generic_interrupt_handler(u8 number, GenericInterruptHandler&);
void unregister_generic_interrupt_handler(u8 number, GenericInterruptHandler&);
ErrorOr<u8> reserve_interrupt_handlers(u8 number_of_irqs);

void initialize_interrupts();

}
