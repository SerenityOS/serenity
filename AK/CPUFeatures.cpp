/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CPUFeatures.h>

namespace AK {

#ifndef KERNEL
#    if ARCH(I386) || ARCH(X86_64)
struct CPUIDResult {
    u32 eax { 0 };
    u32 ebx { 0 };
    u32 ecx { 0 };
    u32 edx { 0 };
};

static CPUIDResult cpuid(u32 leaf, u32 subleaf)
{
    CPUIDResult result;
    asm("cpuid"
        : "=a"(result.eax), "=b"(result.ebx), "=c"(result.ecx), "=d"(result.edx)
        : "0"(leaf), "2"(subleaf));
    return result;
}
#    endif

CPUFeatures Detail::detect_cpu_features_uncached()
{
    CPUFeatures result = CPUFeatures::None;

#    if ARCH(I386) || ARCH(X86_64)
    u32 max_leaf = cpuid(0, 0).eax;
    [[maybe_unused]] auto cpuid1 = max_leaf >= 1 ? cpuid(1, 0) : CPUIDResult {};
    [[maybe_unused]] auto cpuid7 = max_leaf >= 7 ? cpuid(7, 0) : CPUIDResult {};

#        if AK_CAN_CODEGEN_FOR_X86_SSE42
    if (cpuid1.ecx >> 20 & 1)
        result |= CPUFeatures::X86_SSE42;
#        endif
#        if AK_CAN_CODEGEN_FOR_X86_SHA
    if (cpuid7.ebx >> 29 & 1)
        result |= CPUFeatures::X86_SHA;
#        endif
#        if AK_CAN_CODEGEN_FOR_X86_AES
    if (cpuid1.ecx >> 25 & 1)
        result |= CPUFeatures::X86_AES;
#        endif
#    endif

    return result;
}
#endif

}
