/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

class CPUID {
public:
    explicit CPUID(u32 function, u32 ecx = 0) { asm volatile("cpuid"
                                                             : "=a"(m_eax), "=b"(m_ebx), "=c"(m_ecx), "=d"(m_edx)
                                                             : "a"(function), "c"(ecx)); }
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

// FIXME: This can't hold every CPU feature as-is.
enum class CPUFeature : u32 {
    NX = (1 << 0),
    PAE = (1 << 1),
    PGE = (1 << 2),
    RDRAND = (1 << 3),
    RDSEED = (1 << 4),
    SMAP = (1 << 5),
    SMEP = (1 << 6),
    SSE = (1 << 7),
    TSC = (1 << 8),
    RDTSCP = (1 << 9),
    CONSTANT_TSC = (1 << 10),
    NONSTOP_TSC = (1 << 11),
    UMIP = (1 << 12),
    SEP = (1 << 13),
    SYSCALL = (1 << 14),
    MMX = (1 << 15),
    SSE2 = (1 << 16),
    SSE3 = (1 << 17),
    SSSE3 = (1 << 18),
    SSE4_1 = (1 << 19),
    SSE4_2 = (1 << 20),
    XSAVE = (1 << 21),
    AVX = (1 << 22),
    FXSR = (1 << 23),
    LM = (1 << 24),
    HYPERVISOR = (1 << 25),
};

}
