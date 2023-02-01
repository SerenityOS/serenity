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

AK_MAKE_ARBITRARY_SIZED_ENUM(CPUFeature, u256,
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
    FSGSBASE = CPUFeature(1u) << 64u,                 // Access to base of %fs and %gs
    TSC_ADJUST = CPUFeature(1u) << 65u,               // IA32_TSC_ADJUST MSR
    SGX = CPUFeature(1u) << 66u,                      // Software Guard Extensions
    BMI1 = CPUFeature(1u) << 67u,                     // Bit Manipulation Instruction Set 1
    HLE = CPUFeature(1u) << 68u,                      // TSX Hardware Lock Elision
    AVX2 = CPUFeature(1u) << 69u,                     // Advanced Vector Extensions 2
    FDP_EXCPTN_ONLY = CPUFeature(1u) << 70u,          // FDP_EXCPTN_ONLY
    SMEP = CPUFeature(1u) << 71u,                     // Supervisor Mode Execution Protection
    BMI2 = CPUFeature(1u) << 72u,                     // Bit Manipulation Instruction Set 2
    ERMS = CPUFeature(1u) << 73u,                     // Enhanced REP MOVSB/STOSB
    INVPCID = CPUFeature(1u) << 74u,                  // INVPCID Instruction
    RTM = CPUFeature(1u) << 75u,                      // TSX Restricted Transactional Memory
    PQM = CPUFeature(1u) << 76u,                      // Platform Quality of Service Monitoring
    ZERO_FCS_FDS = CPUFeature(1u) << 77u,             // FPU CS and FPU DS deprecated
    MPX = CPUFeature(1u) << 78u,                      // Intel MPX (Memory Protection Extensions)
    PQE = CPUFeature(1u) << 79u,                      // Platform Quality of Service Enforcement
    AVX512_F = CPUFeature(1u) << 80u,                 // AVX-512 Foundation
    AVX512_DQ = CPUFeature(1u) << 81u,                // AVX-512 Doubleword and Quadword Instructions
    RDSEED = CPUFeature(1u) << 82u,                   // RDSEED Instruction
    ADX = CPUFeature(1u) << 83u,                      // Intel ADX (Multi-Precision Add-Carry Instruction Extensions)
    SMAP = CPUFeature(1u) << 84u,                     // Supervisor Mode Access Prevention
    AVX512_IFMA = CPUFeature(1u) << 85u,              // AVX-512 Integer Fused Multiply-Add Instructions
    PCOMMIT = CPUFeature(1u) << 86u,                  // PCOMMIT Instruction
    CLFLUSHOPT = CPUFeature(1u) << 87u,               // CLFLUSHOPT Instruction
    CLWB = CPUFeature(1u) << 88u,                     // CLWB Instruction
    INTEL_PT = CPUFeature(1u) << 89u,                 // Intel Processor Tracing
    AVX512_PF = CPUFeature(1u) << 90u,                // AVX-512 Prefetch Instructions
    AVX512_ER = CPUFeature(1u) << 91u,                // AVX-512 Exponential and Reciprocal Instructions
    AVX512_CD = CPUFeature(1u) << 92u,                // AVX-512 Conflict Detection Instructions
    SHA = CPUFeature(1u) << 93u,                      // Intel SHA Extensions
    AVX512_BW = CPUFeature(1u) << 94u,                // AVX-512 Byte and Word Instructions
    AVX512_VL = CPUFeature(1u) << 95u,                // AVX-512 Vector Length Extensions
    /* EAX=7, ECX */                                  //
    PREFETCHWT1 = CPUFeature(1u) << 96u,              // PREFETCHWT1 Instruction
    AVX512_VBMI = CPUFeature(1u) << 97u,              // AVX-512 Vector Bit Manipulation Instructions
    UMIP = CPUFeature(1u) << 98u,                     // UMIP
    PKU = CPUFeature(1u) << 99u,                      // Memory Protection Keys for User-mode pages
    OSPKE = CPUFeature(1u) << 100u,                   // PKU enabled by OS
    WAITPKG = CPUFeature(1u) << 101u,                 // Timed pause and user-level monitor/wait
    AVX512_VBMI2 = CPUFeature(1u) << 102u,            // AVX-512 Vector Bit Manipulation Instructions 2
    CET_SS = CPUFeature(1u) << 103u,                  // Control Flow Enforcement (CET) Shadow Stack
    GFNI = CPUFeature(1u) << 104u,                    // Galois Field Instructions
    VAES = CPUFeature(1u) << 105u,                    // Vector AES instruction set (VEX-256/EVEX)
    VPCLMULQDQ = CPUFeature(1u) << 106u,              // CLMUL instruction set (VEX-256/EVEX)
    AVX512_VNNI = CPUFeature(1u) << 107u,             // AVX-512 Vector Neural Network Instructions
    AVX512_BITALG = CPUFeature(1u) << 108u,           // AVX-512 BITALG Instructions
    TME_EN = CPUFeature(1u) << 109u,                  // IA32_TME related MSRs are supported
    AVX512_VPOPCNTDQ = CPUFeature(1u) << 110u,        // AVX-512 Vector Population Count Double and Quad-word
    /* ECX Bit 15 */                                  // Reserved
    INTEL_5_LEVEL_PAGING = CPUFeature(1u) << 112u,    // Intel 5-Level Paging
    RDPID = CPUFeature(1u) << 113u,                   // RDPID Instruction
    KL = CPUFeature(1u) << 114u,                      // Key Locker
    /* ECX Bit 24 */                                  // Reserved
    CLDEMOTE = CPUFeature(1u) << 116u,                // Cache Line Demote
    /* ECX Bit 26 */                                  // Reserved
    MOVDIRI = CPUFeature(1u) << 118u,                 // MOVDIRI Instruction
    MOVDIR64B = CPUFeature(1u) << 119u,               // MOVDIR64B Instruction
    ENQCMD = CPUFeature(1u) << 120u,                  // ENQCMD Instruction
    SGX_LC = CPUFeature(1u) << 121u,                  // SGX Launch Configuration
    PKS = CPUFeature(1u) << 122u,                     // Protection Keys for Supervisor-Mode Pages
    /* EAX=7, EDX */                                  //
    /* ECX Bit 0-1 */                                 // Reserved
    AVX512_4VNNIW = CPUFeature(1u) << 125u,           // AVX-512 4-register Neural Network Instructions
    AVX512_4FMAPS = CPUFeature(1u) << 126u,           // AVX-512 4-register Multiply Accumulation Single precision
    FSRM = CPUFeature(1u) << 127u,                    // Fast Short REP MOVSB
    /* ECX Bit 5-7 */                                 // Reserved
    AVX512_VP2INTERSECT = CPUFeature(1u) << 131u,     // AVX-512 VP2INTERSECT Doubleword and Quadword Instructions
    SRBDS_CTRL = CPUFeature(1u) << 132u,              // Special Register Buffer Data Sampling Mitigations
    MD_CLEAR = CPUFeature(1u) << 133u,                // VERW instruction clears CPU buffers
    RTM_ALWAYS_ABORT = CPUFeature(1u) << 134u,        // All TSX transactions are aborted
    /* ECX Bit 12 */                                  // Reserved
    TSX_FORCE_ABORT = CPUFeature(1u) << 136u,         // TSX_FORCE_ABORT MSR
    SERIALIZE = CPUFeature(1u) << 137u,               // Serialize instruction execution
    HYBRID = CPUFeature(1u) << 138u,                  // Mixture of CPU types in processor topology
    TSXLDTRK = CPUFeature(1u) << 139u,                // TSX suspend load address tracking
    /* ECX Bit 17 */                                  // Reserved
    PCONFIG = CPUFeature(1u) << 141u,                 // Platform configuration (Memory Encryption Technologies Instructions)
    LBR = CPUFeature(1u) << 142u,                     // Architectural Last Branch Records
    CET_IBT = CPUFeature(1u) << 143u,                 // Control flow enforcement (CET) indirect branch tracking
    /* ECX Bit 21 */                                  // Reserved
    AMX_BF16 = CPUFeature(1u) << 145u,                // Tile computation on bfloat16 numbers
    AVX512_FP16 = CPUFeature(1u) << 146u,             // AVX512-FP16 half-precision floating-point instructions
    AMX_TILE = CPUFeature(1u) << 147u,                // Tile architecture
    AMX_INT8 = CPUFeature(1u) << 148u,                // Tile computation on 8-bit integers
    SPEC_CTRL = CPUFeature(1u) << 149u,               // Speculation Control
    STIBP = CPUFeature(1u) << 150u,                   // Single Thread Indirect Branch Predictor
    L1D_FLUSH = CPUFeature(1u) << 151u,               // IA32_FLUSH_CMD MSR
    IA32_ARCH_CAPABILITIES = CPUFeature(1u) << 152u,  // IA32_ARCH_CAPABILITIES MSR
    IA32_CORE_CAPABILITIES = CPUFeature(1u) << 153u,  // IA32_CORE_CAPABILITIES MSR
    SSBD = CPUFeature(1u) << 154u,                    // Speculative Store Bypass Disable
    /* EAX=80000001h, ECX */                          //
    LAHF_LM = CPUFeature(1u) << 155u,                 // LAHF/SAHF in long mode
    CMP_LEGACY = CPUFeature(1u) << 156u,              // Hyperthreading not valid
    SVM = CPUFeature(1u) << 157u,                     // Secure Virtual Machine
    EXTAPIC = CPUFeature(1u) << 158u,                 // Extended APIC Space
    CR8_LEGACY = CPUFeature(1u) << 159u,              // CR8 in 32-bit mode
    ABM = CPUFeature(1u) << 160u,                     // Advanced Bit Manipulation
    SSE4A = CPUFeature(1u) << 161u,                   // SSE4a
    MISALIGNSSE = CPUFeature(1u) << 162u,             // Misaligned SSE Mode
    _3DNOWPREFETCH = CPUFeature(1u) << 163u,          // PREFETCH and PREFETCHW Instructions
    OSVW = CPUFeature(1u) << 164u,                    // OS Visible Workaround
    IBS = CPUFeature(1u) << 165u,                     // Instruction Based Sampling
    XOP = CPUFeature(1u) << 166u,                     // XOP instruction set
    SKINIT = CPUFeature(1u) << 167u,                  // SKINIT/STGI Instructions
    WDT = CPUFeature(1u) << 168u,                     // Watchdog timer
    LWP = CPUFeature(1u) << 169u,                     // Light Weight Profiling
    FMA4 = CPUFeature(1u) << 170u,                    // FMA4 instruction set
    TCE = CPUFeature(1u) << 171u,                     // Translation Cache Extension
    NODEID_MSR = CPUFeature(1u) << 172u,              // NodeID MSR
    TBM = CPUFeature(1u) << 173u,                     // Trailing Bit Manipulation
    TOPOEXT = CPUFeature(1u) << 174u,                 // Topology Extensions
    PERFCTR_CORE = CPUFeature(1u) << 175u,            // Core Performance Counter Extensions
    PERFCTR_NB = CPUFeature(1u) << 176u,              // NB Performance Counter Extensions
    DBX = CPUFeature(1u) << 177u,                     // Data Breakpoint Extensions
    PERFTSC = CPUFeature(1u) << 178u,                 // Performance TSC
    PCX_L2I = CPUFeature(1u) << 179u,                 // L2I Performance Counter Extensions
    /* EAX=80000001h, EDX */                          //
    SYSCALL = CPUFeature(1u) << 180u,                 // SYSCALL/SYSRET Instructions
    MP = CPUFeature(1u) << 181u,                      // Multiprocessor Capable
    NX = CPUFeature(1u) << 182u,                      // NX bit
    MMXEXT = CPUFeature(1u) << 183u,                  // Extended MMX
    FXSR_OPT = CPUFeature(1u) << 184u,                // FXSAVE/FXRSTOR Optimizations
    PDPE1GB = CPUFeature(1u) << 185u,                 // Gigabyte Pages
    RDTSCP = CPUFeature(1u) << 186u,                  // RDTSCP Instruction
    LM = CPUFeature(1u) << 187u,                      // Long Mode
    _3DNOWEXT = CPUFeature(1u) << 188u,               // Extended 3DNow!
    _3DNOW = CPUFeature(1u) << 189u,                  // 3DNow!
    /* EAX=80000007h, EDX */                          //
    CONSTANT_TSC = CPUFeature(1u) << 190u,            // Invariant TSC
    NONSTOP_TSC = CPUFeature(1u) << 191u,             // Invariant TSC
    __End = CPUFeature(1u) << 255u);

StringView cpu_feature_to_name(CPUFeature::Type const&);

}
