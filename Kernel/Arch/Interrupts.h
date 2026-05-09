/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Platform.h>
#include <AK/Types.h>
#include <Kernel/Interrupts/Interrupts.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Interrupts.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/Interrupts.h>
#endif

namespace Kernel {

class GenericInterruptHandler;

GenericInterruptHandler& get_interrupt_handler(InterruptNumber);
void register_generic_interrupt_handler(InterruptNumber, GenericInterruptHandler&);
void unregister_generic_interrupt_handler(InterruptNumber, GenericInterruptHandler&);
ErrorOr<InterruptNumber> reserve_interrupt_handlers(size_t number_of_irqs);

void initialize_interrupts();

}
