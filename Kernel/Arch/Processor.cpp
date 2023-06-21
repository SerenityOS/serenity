/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>

namespace Kernel {

// FIXME: Move the InterruptsState related functions inside the Processor class, when we have a generic Processor base class.
InterruptsState processor_interrupts_state()
{
    return Processor::are_interrupts_enabled() ? InterruptsState::Enabled : InterruptsState::Disabled;
}

void restore_processor_interrupts_state(InterruptsState interrupts_state)
{
    if (interrupts_state == InterruptsState::Enabled)
        Processor::enable_interrupts();
    else
        Processor::disable_interrupts();
}

}
