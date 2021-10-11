/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#if ARCH(X86_64) || ARCH(I386)
#    include <Kernel/Arch/x86/InterruptDisabler.h>
#else

// FIXME To implement

#include <Kernel/Arch/Processor.h>

namespace Kernel {

class InterruptDisabler {
public:
    InterruptDisabler()
    {
        //state = Processor::disable_interrups();
    }

    ~InterruptDisabler()
    {
        //Processor::enable_interrups(state);
    }
};

class NonMaskableInterruptDisabler {
public:
    NonMaskableInterruptDisabler()
    {
        //state = Processor::disable_non_maskable_interrups();
    }

    ~NonMaskableInterruptDisabler()
    {
        //Processor::enable_non_maskable_interrups(state);
    }
};

}

#endif
