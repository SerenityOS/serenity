/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Interrupts.h>

namespace Kernel {

GenericInterruptHandler& get_interrupt_handler(u8)
{
    VERIFY_NOT_REACHED();
}

void register_generic_interrupt_handler(u8, GenericInterruptHandler&)
{
    VERIFY_NOT_REACHED();
}

void unregister_generic_interrupt_handler(u8, GenericInterruptHandler&)
{
    VERIFY_NOT_REACHED();
}

}
