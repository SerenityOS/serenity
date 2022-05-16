/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <Kernel/Arch/x86/IO.h>

#include <Kernel/Arch/x86/ASM_wrapper.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

class InterruptDisabler {
public:
    InterruptDisabler()
    {
        m_flags = cpu_flags();
        cli();
    }

    ~InterruptDisabler()
    {
        if ((m_flags & 0x200) != 0)
            sti();
    }

private:
    u32 m_flags;
};

}
