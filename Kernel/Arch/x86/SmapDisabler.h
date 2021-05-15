/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/x86/CPU.h>

namespace Kernel {

class SmapDisabler {
public:
    ALWAYS_INLINE SmapDisabler()
        : m_flags(cpu_flags())
    {
        stac();
    }

    ALWAYS_INLINE ~SmapDisabler()
    {
        if (!(m_flags & 0x40000))
            clac();
    }

private:
    const FlatPtr m_flags;
};

}
