/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Arch/Processor.h>

namespace Kernel {

class InterruptDisabler {
public:
    InterruptDisabler()
        : m_interrupts_were_enabled(Processor::are_interrupts_enabled())
    {
        Processor::disable_interrupts();
    }

    ~InterruptDisabler()
    {
        if (m_interrupts_were_enabled)
            Processor::enable_interrupts();
    }

private:
    bool m_interrupts_were_enabled;
};

}
