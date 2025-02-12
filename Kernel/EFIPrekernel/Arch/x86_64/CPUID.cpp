/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/Arch/x86_64/CPUID.h>
#include <Kernel/EFIPrekernel/Arch/x86_64/CPUID.h>

namespace Kernel {

bool has_nx()
{
    static TriState nx_supported = TriState::Unknown;

    if (nx_supported == TriState::Unknown) {
        CPUID extended_processor_info(0x80000001);

        if ((extended_processor_info.edx() & (1 << 20)) != 0)
            nx_supported = TriState::True;
        else
            nx_supported = TriState::False;
    }

    return nx_supported == TriState::True;
}

}
