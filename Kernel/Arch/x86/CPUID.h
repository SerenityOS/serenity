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
    /* EAX=1, ECX                                  */ //
    SSE3 = CPUFeature(1u) << 0u,                      // Streaming SIMD Extensions 3
    PCLMULQDQ = CPUFeature(1u) << 1u,                 // PCLMULDQ Instruction
    DTES64 = CPUFeature(1u) << 2u,                    // 64-Bit Debug Store
    MONITOR = CPUFeature(1u) << 3u,                   // MONITOR/MWAIT Instructions
    DS_CPL = CPUFeature(1u) << 4u,                    // CPL Qualified Debug Store
    VMX = CPUFeature(1u) << 5u,                       // Virtual Machine Extensions
    SMX = CPUFeature(1u) << 6u,                       // Safer Mode Extensions
    EST = CPUFeature(1u) << 7u,                       // Enhanced Intel SpeedStep® Technology
    TM2 = CPUFeature(1u) << 8u,                       // Thermal Monitor 2
    SSSE3 = CPUFeature(1u) << 9u,                     // Supplemental Streaming SIMD Extensions 3
    CNXT_ID = CPUFeature(1u) << 10u,                  // L1 Context ID
    SDBG = CPUFeature(1u) << 11u,                     // Silicon Debug (IA32_DEBUG_INTERFACE MSR)
    FMA = CPUFeature(1u) << 12u,                      // Fused Multiply Add
    CX16 = CPUFeature(1u) << 13u,                     // CMPXCHG16B Instruction
    XTPR = CPUFeature(1u) << 14u,                     // xTPR Update Control
    PDCM = CPUFeature(1u) << 15u,                     // Perfmon and Debug Capability (IA32_PERF_CAPABILITIES MSR)
    /* ECX Bit 16 */                                  // Reserved
    PCID = CPUFeature(1u) << 17u,                     // Process Context Identifiers
    DCA = CPUFeature(1u) << 18u,                      // Direct Cache Access
    SSE4_1 = CPUFeature(1u) << 19u,                   // Streaming SIMD Extensions 4.1
    SSE4_2 = CPUFeature(1u) << 20u,                   // Streaming SIMD Extensions 4.2
    X2APIC = CPUFeature(1u) << 21u,                   // Extended xAPIC Support
    MOVBE = CPUFeature(1u) << 22u,                    // MOVBE Instruction
    POPCNT = CPUFeature(1u) << 23u,                   // POPCNT Instruction
    TSC_DEADLINE = CPUFeature(1u) << 24u,             // Time Stamp Counter Deadline
    AES = CPUFeature(1u) << 25u,                      // AES Instruction Extensions
    XSAVE = CPUFeature(1u) << 26u,                    // XSAVE/XSTOR States
    OSXSAVE = CPUFeature(1u) << 27u,                  // OS-Enabled Extended State Management
    AVX = CPUFeature(1u) << 28u,                      // Advanced Vector Extensions
    F16C = CPUFeature(1u) << 29u,                     // 16-bit floating-point conversion instructions
    RDRAND = CPUFeature(1u) << 30u,                   // RDRAND Instruction
    HYPERVISOR = CPUFeature(1u) << 31u,               // Hypervisor present (always zero on physical CPUs)
    /* EAX=1, EDX */                                  //
    FPU = CPUFeature(1u) << 32u,                      // Floating-point Unit On-Chip
    VME = CPUFeature(1u) << 33u,                      // Virtual Mode Extension
    DE = CPUFeature(1u) << 34u,                       // Debugging Extension
    PSE = CPUFeature(1u) << 35u,                      // Page Size Extension
    TSC = CPUFeature(1u) << 36u,                      // Time Stamp Counter
    MSR = CPUFeature(1u) << 37u,                      // Model Specific Registers
    PAE = CPUFeature(1u) << 38u,                      // Physical Address Extension
    MCE = CPUFeature(1u) << 39u,                      // Machine-Check Exception
    CX8 = CPUFeature(1u) << 40u,                      // CMPXCHG8 Instruction
    APIC = CPUFeature(1u) << 41u,                     // On-chip APIC Hardware
    /* EDX Bit 10 */                                  // Reserved
    SEP = CPUFeature(1u) << 43u,                      // Fast System Call
    MTRR = CPUFeature(1u) << 44u,                     // Memory Type Range Registers
    PGE = CPUFeature(1u) << 45u,                      // Page Global Enable
    MCA = CPUFeature(1u) << 46u,                      // Machine-Check Architecture
    CMOV = CPUFeature(1u) << 47u,                     // Conditional Move Instruction
    PAT = CPUFeature(1u) << 48u,                      // Page Attribute Table
    PSE36 = CPUFeature(1u) << 49u,                    // 36-bit Page Size Extension
    PSN = CPUFeature(1u) << 50u,                      // Processor serial number is present and enabled
    CLFLUSH = CPUFeature(1u) << 51u,                  // CLFLUSH Instruction
    /* EDX Bit 20 */                                  // Reserved
    DS = CPUFeature(1u) << 53u,                       // CLFLUSH Instruction
    ACPI = CPUFeature(1u) << 54u,                     // CLFLUSH Instruction
    MMX = CPUFeature(1u) << 55u,                      // CLFLUSH Instruction
    FXSR = CPUFeature(1u) << 56u,                     // CLFLUSH Instruction
    SSE = CPUFeature(1u) << 57u,                      // Streaming SIMD Extensions
    SSE2 = CPUFeature(1u) << 58u,                     // Streaming SIMD Extensions 2
    SS = CPUFeature(1u) << 59u,                       // Self-Snoop
    HTT = CPUFeature(1u) << 60u,                      // Multi-Threading
    TM = CPUFeature(1u) << 61u,                       // Thermal Monitor
    IA64 = CPUFeature(1u) << 62u,                     // IA64 processor emulating x86
    PBE = CPUFeature(1u) << 63u,                      // Pending Break Enable
    /* EAX=7, EBX */                                  //
    SMEP = CPUFeature(1u) << 64u,                     // Supervisor Mode Execution Protection
    RDSEED = CPUFeature(1u) << 65u,                   // RDSEED Instruction
    SMAP = CPUFeature(1u) << 66u,                     // Supervisor Mode Access Prevention
    /* EAX=7, ECX */                                  //
    UMIP = CPUFeature(1u) << 67u,                     // User-Mode Instruction Prevention
    /* EAX=80000001h, EDX */                          //
    SYSCALL = CPUFeature(1u) << 68u,                  // SYSCALL/SYSRET Instructions
    NX = CPUFeature(1u) << 69u,                       // NX bit
    RDTSCP = CPUFeature(1u) << 70u,                   // RDTSCP Instruction
    LM = CPUFeature(1u) << 71u,                       // Long Mode
    /* EAX=80000007h, EDX */                          //
    CONSTANT_TSC = CPUFeature(1u) << 72u,             // Invariant TSC
    NONSTOP_TSC = CPUFeature(1u) << 73u,              // Invariant TSC
    __End = CPUFeature(1u) << 127u);

StringView cpu_feature_to_string_view(CPUFeature::Type const&);

}
