/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ArbitrarySizedEnum.h>
#include <AK/Types.h>
#include <AK/UFixedBigInt.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

class CPUID {
public:
    explicit CPUID(u32 function, u32 ecx = 0)
    {
        asm volatile("cpuid"
                     : "=a"(m_eax), "=b"(m_ebx), "=c"(m_ecx), "=d"(m_edx)
                     : "a"(function), "c"(ecx));
    }

    u32 eax() const { return m_eax; }
    u32 ebx() const { return m_ebx; }
    u32 ecx() const { return m_ecx; }
    u32 edx() const { return m_edx; }

private:
    u32 m_eax { 0xffffffff };
    u32 m_ebx { 0xffffffff };
    u32 m_ecx { 0xffffffff };
    u32 m_edx { 0xffffffff };
};

AK_MAKE_ARBITRARY_SIZED_ENUM(CPUFeature, u128,
    NX = CPUFeature(1u) << 0u,
    PAE = CPUFeature(1u) << 1u,
    PGE = CPUFeature(1u) << 2u,
    RDRAND = CPUFeature(1u) << 3u,
    RDSEED = CPUFeature(1u) << 4u,
    SMAP = CPUFeature(1u) << 5u,
    SMEP = CPUFeature(1u) << 6u,
    SSE = CPUFeature(1u) << 7u,
    TSC = CPUFeature(1u) << 8u,
    RDTSCP = CPUFeature(1u) << 9u,
    CONSTANT_TSC = CPUFeature(1u) << 10u,
    NONSTOP_TSC = CPUFeature(1u) << 11u,
    UMIP = CPUFeature(1u) << 12u,
    SEP = CPUFeature(1u) << 13u,
    SYSCALL = CPUFeature(1u) << 14u,
    MMX = CPUFeature(1u) << 15u,
    SSE2 = CPUFeature(1u) << 16u,
    SSE3 = CPUFeature(1u) << 17u,
    SSSE3 = CPUFeature(1u) << 18u,
    SSE4_1 = CPUFeature(1u) << 19u,
    SSE4_2 = CPUFeature(1u) << 20u,
    XSAVE = CPUFeature(1u) << 21u,
    AVX = CPUFeature(1u) << 22u,
    FXSR = CPUFeature(1u) << 23u,
    LM = CPUFeature(1u) << 24u,
    HYPERVISOR = CPUFeature(1u) << 25u,
    PAT = CPUFeature(1u) << 26u,
    __End = CPUFeature(1u) << 27u);

}
