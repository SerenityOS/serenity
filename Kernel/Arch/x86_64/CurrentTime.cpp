/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/CurrentTime.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/x86_64/ASM_wrapper.h>

namespace Kernel {

static u64 current_time_tsc()
{
    return read_tsc();
}

fptr optional_current_time()
{
    VERIFY(Processor::is_initialized()); // sanity check
    // Figure out a good scheduling time source
    if (Processor::current().has_feature(CPUFeature::TSC) && Processor::current().has_feature(CPUFeature::CONSTANT_TSC)) {
        return current_time_tsc;
    }
    return nullptr;
}

}
