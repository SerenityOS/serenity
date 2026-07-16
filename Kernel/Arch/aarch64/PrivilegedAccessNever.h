/*
 * Copyright (c) 2026, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/CPUID.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>

namespace Kernel::Aarch64::Asm {

extern "C" void asm_clear_pan();
extern "C" void asm_set_pan();
extern "C" bool asm_is_pan_set();

inline void clear_pan()
{
    if (!Processor::current().has_feature(CPUFeature::PAN))
        return;
    asm_clear_pan();
}

inline void set_pan()
{
    if (!Processor::current().has_feature(CPUFeature::PAN))
        return;
    asm_set_pan();
}

inline bool is_pan_set()
{
    if (!Processor::current().has_feature(CPUFeature::PAN))
        return false;
    return asm_is_pan_set();
}

inline void clear_sctlr_el1_span()
{
    if (!Processor::current().has_feature(CPUFeature::PAN))
        return;

    auto sctlr_el1 = SCTLR_EL1::read();
    sctlr_el1.SPAN = 0;
    SCTLR_EL1::write(sctlr_el1);
    instruction_synchronization_barrier();
}

}
