/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <Kernel/Arch/x86_64/IO.h>

namespace Kernel {

class NonMaskableInterruptDisabler {
public:
    NonMaskableInterruptDisabler()
    {
        IO::out8(0x70, IO::in8(0x70) | 0x80);
    }

    ~NonMaskableInterruptDisabler()
    {
        IO::out8(0x70, IO::in8(0x70) & 0x7F);
    }
};

}
