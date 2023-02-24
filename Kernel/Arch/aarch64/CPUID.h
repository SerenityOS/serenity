/*
 * Copyright (c) 2023, Konrad <konrad@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ArbitrarySizedEnum.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>
#include <AK/UFixedBigInt.h>
#include <Kernel/Arch/aarch64/Registers.h>
#include <Kernel/Library/KString.h>

#include <AK/Platform.h>
VALIDATE_IS_AARCH64()

namespace Kernel {

// https://developer.arm.com/downloads/-/exploration-tools/feature-names-for-a-profile
AK_MAKE_ARBITRARY_SIZED_ENUM(CPUFeature, u256,
    // 2022 Architecture Extensions
    ABLE = CPUFeature(1u) << 0u,                 // Address Breakpoint Linking extension
    ADERR = CPUFeature(1u) << 1u,                // RASv2 Additional Error syndrome reporting, for Device memory
    ANERR = CPUFeature(1u) << 2u,                // RASv2 Additional Error syndrome reporting, for Normal memory
    AIE = CPUFeature(1u) << 3u,                  // Memory Attribute Index Enhancement
    B16B16 = CPUFeature(1u) << 4u,               // Non-widening BFloat16 to BFloat16 arithmetic for SVE2.1 and SME2.1
    CLRBHB = CPUFeature(1u) << 5u,               // A new instruction CLRBHB is added in HINT space
    CHK = CPUFeature(1u) << 6u,                  // Detect when Guarded Control Stacks are implemented
    CSSC = CPUFeature(1u) << 7u,                 // Common Short Sequence Compression scalar integer instructions
    CSV2_3 = CPUFeature(1u) << 8u,               // New identification mechanism for Branch History information
    D128 = CPUFeature(1u) << 9u,                 // 128-bit Translation Tables, 56 bit PA
    Debugv8p9 = CPUFeature(1u) << 10u,           // Debug 2022
    DoubleFault2 = CPUFeature(1u) << 11u,        // Error exception routing extensions.
    EBEP = CPUFeature(1u) << 12u,                // Exception-based event profiling
    ECBHB = CPUFeature(1u) << 13u,               // Imposes restrictions on branch history speculation around exceptions
    ETEv1p3 = CPUFeature(1u) << 14u,             // ETE support for v9.3
    FGT2 = CPUFeature(1u) << 15u,                // Fine-grained traps 2
    GCS = CPUFeature(1u) << 16u,                 // Guarded Control Stack Extension
    HAFT = CPUFeature(1u) << 17u,                // Hardware managed Access Flag for Table descriptors
    ITE = CPUFeature(1u) << 18u,                 // Instrumentation trace extension
    LRCPC3 = CPUFeature(1u) << 19u,              // Load-Acquire RCpc instructions version 3
    LSE128 = CPUFeature(1u) << 20u,              // 128-bit Atomics
    LVA3 = CPUFeature(1u) << 21u,                // 56-bit VA
    MEC = CPUFeature(1u) << 22u,                 // Memory Encryption Contexts
    MTE4 = CPUFeature(1u) << 23u,                // Support for Canonical tag checking, reporting of all non-address bits on a fault, Store-only Tag checking, Memory tagging with Address tagging disabled
    MTE_CANONICAL_TAGS = CPUFeature(1u) << 24u,  // Support for Canonical tag checking
    MTE_TAGGED_FAR = CPUFeature(1u) << 25u,      // Support for reporting of all non-address bits on a fault
    MTE_STORE_ONLY = CPUFeature(1u) << 26u,      // Support for Store-only Tag checking
    MTE_NO_ADDRESS_TAGS = CPUFeature(1u) << 27u, // Support for Memory tagging with Address tagging disabled
    MTE_ASYM_FAULT = CPUFeature(1u) << 28u,      // Asymmetric Tag Check Fault handling
    MTE_ASYNC = CPUFeature(1u) << 29u,           // Asynchronous Tag Check Fault handling
    MTE_PERM = CPUFeature(1u) << 30u,            // Allocation tag access permission
    PCSRv8p9 = CPUFeature(1u) << 31u,            // PCSR disable control
    PIE = CPUFeature(1u) << 32u,                 // Permission model enhancements
    POE = CPUFeature(1u) << 33u,                 // Permission model enhancements
    S1PIE = CPUFeature(1u) << 34u,               // Permission model enhancements
    S2PIE = CPUFeature(1u) << 35u,               // Permission model enhancements
    S1POE = CPUFeature(1u) << 36u,               // Permission model enhancements
    S2POE = CPUFeature(1u) << 37u,               // Permission model enhancements
    PMUv3p9 = CPUFeature(1u) << 38u,             // EL0 access controls for PMU event counters
    PMUv3_EDGE = CPUFeature(1u) << 39u,          // PMU event edge detection
    PMUv3_ICNTR = CPUFeature(1u) << 40u,         // PMU instruction counter
    PMUv3_SS = CPUFeature(1u) << 41u,            // PMU snapshot
    PRFMSLC = CPUFeature(1u) << 42u,             // Prefetching enhancements
    PFAR = CPUFeature(1u) << 43u,                // Physical Fault Address Extension [NOTE: not yet listed]
    RASv2 = CPUFeature(1u) << 44u,               // Reliability, Availability, and Serviceability (RAS) Extension version 2
    RPZ = CPUFeature(1u) << 45u,                 // ? [NOTE: not yet listed]
    RPRFM = CPUFeature(1u) << 46u,               // RPRFM range prefetch hint instruction
    SCTLR2 = CPUFeature(1u) << 47u,              // Extension to SCTLR_ELx
    SEBEP = CPUFeature(1u) << 48u,               // Synchronous Exception-based event profiling
    SME_F16F16 = CPUFeature(1u) << 49u,          // Non-widening half-precision FP16 to FP16 arithmetic for SME2.1
    SME2 = CPUFeature(1u) << 50u,                // Scalable Matrix Extension version 2
    SME2p1 = CPUFeature(1u) << 51u,              // Scalable Matrix Extension version 2.1
    SPECRES2 = CPUFeature(1u) << 52u,            // Adds new Clear Other Speculative Predictions instruction
    SPMU = CPUFeature(1u) << 53u,                // System PMU
    SPEv1p4 = CPUFeature(1u) << 54u,             // Additional SPE events
    SPE_FDS = CPUFeature(1u) << 55u,             // SPE filtering by data source
    SVE2p1 = CPUFeature(1u) << 56u,              // Scalable Vector Extension version SVE2.1
    SYSINSTR128 = CPUFeature(1u) << 57u,         // 128-bit System instructions
    SYSREG128 = CPUFeature(1u) << 58u,           // 128-bit System registers
    TCR2 = CPUFeature(1u) << 59u,                // Extension to TCR_ELx
    THE = CPUFeature(1u) << 60u,                 // Translation Hardening Extension
    TRBE_EXT = CPUFeature(1u) << 61u,            // Represents TRBE external mode
    TRBE_MPAM = CPUFeature(1u) << 62u,           // Trace Buffer MPAM extensions

    // 2021 Architecture Extensions
    CMOW = CPUFeature(1u) << 63u,          // Control for cache maintenance permission
    CONSTPACFIELD = CPUFeature(1u) << 64u, // PAC Algorithm enhancement
    Debugv8p8 = CPUFeature(1u) << 65u,     // Debug v8.8
    HBC = CPUFeature(1u) << 66u,           // Hinted conditional branches
    HPMN0 = CPUFeature(1u) << 67u,         // Setting of MDCR_EL2.HPMN to zero
    NMI = CPUFeature(1u) << 68u,           // Non-maskable Interrupts
    GICv3_NMI = CPUFeature(1u) << 69u,     // Non-maskable Interrupts
    MOPS = CPUFeature(1u) << 70u,          // Standardization of memory operations
    PACQARMA3 = CPUFeature(1u) << 71u,     // Pointer authentication - QARMA3 algorithm
    PMUv3_TH = CPUFeature(1u) << 72u,      // Event counting threshold
    PMUv3p8 = CPUFeature(1u) << 73u,       // Armv8.8 PMU Extensions
    PMUv3_EXT64 = CPUFeature(1u) << 74u,   // Optional 64-bit external interface to the Performance Monitors
    PMUv3_EXT32 = CPUFeature(1u) << 75u,   // Represents the original mostly 32-bit external interface to the Performance Monitors
    RNG_TRAP = CPUFeature(1u) << 76u,      // Trapping support for RNDR and RNDRRS
    SPEv1p3 = CPUFeature(1u) << 77u,       // Armv8.8 Statistical Profiling Extensions
    TIDCP1 = CPUFeature(1u) << 78u,        // EL0 use of IMPLEMENTATION DEFINED functionality
    BRBEv1p1 = CPUFeature(1u) << 79u,      // Branch Record Buffer Extensions version 1.1

    // 2020 Architecture Extensions
    AFP = CPUFeature(1u) << 80u,          // Alternate floating-point behavior
    HCX = CPUFeature(1u) << 81u,          // Support for the HCRX_EL2 register
    LPA2 = CPUFeature(1u) << 82u,         // Larger physical address for 4KB and 16KB translation granules
    LS64 = CPUFeature(1u) << 83u,         // Support for 64 byte loads/stores without return
    LS64_V = CPUFeature(1u) << 84u,       // Support for 64-byte stores with return
    LS64_ACCDATA = CPUFeature(1u) << 85u, // Support for 64-byte EL0 stores with return
    MTE3 = CPUFeature(1u) << 86u,         // MTE Asymmetric Fault Handling
    PAN3 = CPUFeature(1u) << 87u,         // Support for SCTLR_ELx.EPAN
    PMUv3p7 = CPUFeature(1u) << 88u,      // Armv8.7 PMU Extensions
    RPRES = CPUFeature(1u) << 89u,        // Increased precision of Reciprocal Estimate and Reciprocal Square Root Estimate
    RME = CPUFeature(1u) << 90u,          // Realm Management Extension
    SME_FA64 = CPUFeature(1u) << 91u,     // Additional instructions for the SME Extension
    SME_F64F64 = CPUFeature(1u) << 92u,   // Additional instructions for the SME Extension
    SME_I16I64 = CPUFeature(1u) << 93u,   // Additional instructions for the SME Extension
    EBF16 = CPUFeature(1u) << 94u,        // Additional instructions for the SME Extension
    SPEv1p2 = CPUFeature(1u) << 95u,      // Armv8.7 SPE
    WFxT = CPUFeature(1u) << 96u,         // WFE and WFI instructions with timeout
    XS = CPUFeature(1u) << 97u,           // XS attribute
    BRBE = CPUFeature(1u) << 98u,         // Branch Record Buffer Extensions

    // Features introduced prior to 2020
    AdvSIMD = CPUFeature(1u) << 99u,        // Advanced SIMD Extension
    AES = CPUFeature(1u) << 100u,           // Advanced SIMD AES instructions
    PMULL = CPUFeature(1u) << 101u,         // Advanced SIMD PMULL instructions; ARMv8.0-AES is split into AES and PMULL
    CP15SDISABLE2 = CPUFeature(1u) << 102u, // CP15DISABLE2
    CSV2 = CPUFeature(1u) << 103u,          // Cache Speculation Variant 2
    CSV2_1p1 = CPUFeature(1u) << 104u,      // Cache Speculation Variant 2 version 1.1
    CSV2_1p2 = CPUFeature(1u) << 105u,      // Cache Speculation Variant 2 version 1.2
    CSV2_2 = CPUFeature(1u) << 106u,        // Cache Speculation Variant 2 version 2 [NOTE: name mistake in source!]
    CSV3 = CPUFeature(1u) << 107u,          // Cache Speculation Variant 3
    DGH = CPUFeature(1u) << 108u,           // Data Gathering Hint
    DoubleLock = CPUFeature(1u) << 109u,    // Double Lock
    ETS = CPUFeature(1u) << 110u,           // Enhanced Translation Synchronization
    FP = CPUFeature(1u) << 111u,            // Floating point extension
    IVIPT = CPUFeature(1u) << 112u,         // The IVIPT Extension
    PCSRv8 = CPUFeature(1u) << 113u,        // PC Sample-base Profiling extension (not EL3 and EL2)
    SPECRES = CPUFeature(1u) << 114u,       // Speculation restriction instructions
    RAS = CPUFeature(1u) << 115u,           // Reliability, Availability, and Serviceability (RAS) Extension
    SB = CPUFeature(1u) << 116u,            // Speculation barrier
    SHA1 = CPUFeature(1u) << 117u,          // Advanced SIMD SHA1 instructions
    SHA256 = CPUFeature(1u) << 118u,        // Advanced SIMD SHA256 instructions; Split ARMv8.2-SHA into SHA-256, SHA-512 and SHA-3
    SSBS = CPUFeature(1u) << 119u,          // Speculative Store Bypass Safe Instruction; ARMv8.0-SSBS is split into SSBS and SSBS2
    SSBS2 = CPUFeature(1u) << 120u,         // MRS and MSR instructions for SSBS; ARMv8.0-SSBS is split into SSBS and SSBS2
    CRC32 = CPUFeature(1u) << 121u,         // CRC32 instructions
    nTLBPA = CPUFeature(1u) << 122u,        // No intermediate caching by output address in TLB
    Debugv8p1 = CPUFeature(1u) << 123u,     // Debug with VHE
    HPDS = CPUFeature(1u) << 124u,          // Hierarchical permission disables in translation tables
    LOR = CPUFeature(1u) << 125u,           // Limited ordering regions
    LSE = CPUFeature(1u) << 126u,           // Large System Extensions
    PAN = CPUFeature(1u) << 127u,           // Privileged access-never
    PMUv3p1 = CPUFeature(1u) << 128u,       // PMU extensions version 3.1
    RDM = CPUFeature(1u) << 129u,           // Rounding double multiply accumulate
    HAFDBS = CPUFeature(1u) << 130u,        // Hardware updates to access flag and dirty state in translation tables
    VHE = CPUFeature(1u) << 131u,           // Virtualization Host Extensions
    VMID16 = CPUFeature(1u) << 132u,        // 16-bit VMID
    AA32BF16 = CPUFeature(1u) << 133u,      // AArch32 BFloat16 instructions
    AA32HPD = CPUFeature(1u) << 134u,       // AArch32 Hierarchical permission disables
    AA32I8MM = CPUFeature(1u) << 135u,      // AArch32 Int8 Matrix Multiplication
    PAN2 = CPUFeature(1u) << 136u,          // AT S1E1R and AT S1E1W instruction variants for PAN
    BF16 = CPUFeature(1u) << 137u,          // AARch64 BFloat16 instructions
    DPB2 = CPUFeature(1u) << 138u,          // DC CVADP instruction
    DPB = CPUFeature(1u) << 139u,           // DC CVAP instruction
    Debugv8p2 = CPUFeature(1u) << 140u,     // ARMv8.2 Debug
    DotProd = CPUFeature(1u) << 141u,       // Advanced SIMD Int8 dot product instructions
    EVT = CPUFeature(1u) << 142u,           // Enhanced Virtualization Traps
    F32MM = CPUFeature(1u) << 143u,         // SVE single-precision floating-point matrix multiply instruction
    F64MM = CPUFeature(1u) << 144u,         // SVE double-precision floating-point matrix multiply instruction
    FHM = CPUFeature(1u) << 145u,           // Half-precision floating-point FMLAL instructions
    FP16 = CPUFeature(1u) << 146u,          // Half-precision floating-point data processing
    I8MM = CPUFeature(1u) << 147u,          // Int8 Matrix Multiplication
    IESB = CPUFeature(1u) << 148u,          // Implicit Error synchronization event
    LPA = CPUFeature(1u) << 149u,           // Large PA and IPA support
    LSMAOC = CPUFeature(1u) << 150u,        // Load/Store instruction multiple atomicity and ordering controls
    LVA = CPUFeature(1u) << 151u,           // Large VA support
    MPAM = CPUFeature(1u) << 152u,          // Memory Partitioning and Monitoring
    PCSRv8p2 = CPUFeature(1u) << 153u,      // PC Sample-based profiling version 8.2
    SHA3 = CPUFeature(1u) << 154u,          // Advanced SIMD EOR3, RAX1, XAR, and BCAX instructions; Split ARMv8.2-SHA into SHA-256, SHA-512 and SHA-3
    SHA512 = CPUFeature(1u) << 155u,        // Advanced SIMD SHA512 instructions; Split ARMv8.2-SHA into SHA-256, SHA-512 and SHA-3
    SM3 = CPUFeature(1u) << 156u,           // Advanced SIMD SM3 instructions; Split into SM3 and SM4
    SM4 = CPUFeature(1u) << 157u,           // Advanced SIMD SM4 instructions; Split into SM3 and SM4
    SPE = CPUFeature(1u) << 158u,           // Statistical Profiling Extension
    SVE = CPUFeature(1u) << 159u,           // Scalable Vector Extension
    TTCNP = CPUFeature(1u) << 160u,         // Common not private translations
    HPDS2 = CPUFeature(1u) << 161u,         // Heirarchical permission disables in translation tables 2
    XNX = CPUFeature(1u) << 162u,           // Execute-never control distinction by Exception level at stage 2
    UAO = CPUFeature(1u) << 163u,           // Unprivileged Access Override control
    VPIPT = CPUFeature(1u) << 164u,         // VMID-aware PIPT instruction cache
    CCIDX = CPUFeature(1u) << 165u,         // Extended cache index
    FCMA = CPUFeature(1u) << 166u,          // Floating-point FCMLA and FCADD instructions
    DoPD = CPUFeature(1u) << 167u,          // Debug over Powerdown
    EPAC = CPUFeature(1u) << 168u,          // Enhanced Pointer authentication
    FPAC = CPUFeature(1u) << 169u,          // Faulting on pointer authentication instructions
    FPACCOMBINE = CPUFeature(1u) << 170u,   // Faulting on combined pointer authentication instructions
    JSCVT = CPUFeature(1u) << 171u,         // JavaScript FJCVTS conversion instruction
    LRCPC = CPUFeature(1u) << 172u,         // Load-acquire RCpc instructions
    NV = CPUFeature(1u) << 173u,            // Nested virtualization
    PACQARMA5 = CPUFeature(1u) << 174u,     // Pointer authentication - QARMA5 algorithm
    PACIMP = CPUFeature(1u) << 175u,        // Pointer authentication - IMPLEMENTATION DEFINED algorithm
    PAuth = CPUFeature(1u) << 176u,         // Pointer authentication
    PAuth2 = CPUFeature(1u) << 177u,        // Enhancements to pointer authentication
    SPEv1p1 = CPUFeature(1u) << 178u,       // Statistical Profiling Extensions version 1.1
    AMUv1 = CPUFeature(1u) << 179u,         // Activity Monitors Extension
    CNTSC = CPUFeature(1u) << 180u,         // Generic Counter Scaling
    Debugv8p4 = CPUFeature(1u) << 181u,     // Debug relaxations and extensions version 8.4
    DoubleFault = CPUFeature(1u) << 182u,   // Double Fault Extension
    DIT = CPUFeature(1u) << 183u,           // Data Independent Timing instructions
    FlagM = CPUFeature(1u) << 184u,         // Condition flag manipulation
    IDST = CPUFeature(1u) << 185u,          // ID space trap handling
    LRCPC2 = CPUFeature(1u) << 186u,        // Load-acquire RCpc instructions version 2
    LSE2 = CPUFeature(1u) << 187u,          // Large System Extensions version 2
    NV2 = CPUFeature(1u) << 188u,           // Enhanced support for nested virtualization
    PMUv3p4 = CPUFeature(1u) << 189u,       // PMU extension version 3.4
    RASv1p1 = CPUFeature(1u) << 190u,       // Reliability, Availability, and Serviceability (RAS) Extension version 1.1
    S2FWB = CPUFeature(1u) << 191u,         // Stage 2 forced write-back
    SEL2 = CPUFeature(1u) << 192u,          // Secure EL2
    TLBIOS = CPUFeature(1u) << 193u,        // TLB invalidate outer-shared instructions; Split into TLBIOS and TLBIRANGE
    TLBIRANGE = CPUFeature(1u) << 194u,     // TLB range invalidate range instructions; Split into TLBIOS and TLBIRANGE
    TRF = CPUFeature(1u) << 195u,           // Self hosted Trace Extensions
    TTL = CPUFeature(1u) << 196u,           // Translation Table Level
    BBM = CPUFeature(1u) << 197u,           // Translation table break before make levels
    TTST = CPUFeature(1u) << 198u,          // Small translation tables
    BTI = CPUFeature(1u) << 199u,           // Branch target identification
    FlagM2 = CPUFeature(1u) << 200u,        // Condition flag manipulation version 2
    ExS = CPUFeature(1u) << 201u,           // Disabling context synchronizing exception entry and exit
    E0PD = CPUFeature(1u) << 202u,          // Preventing EL0 access to halves of address maps
    FRINTTS = CPUFeature(1u) << 203u,       // FRINT32Z, FRINT32X, FRINT64Z, and FRINT64X instructions
    GTG = CPUFeature(1u) << 204u,           // Guest translation granule size
    MTE = CPUFeature(1u) << 205u,           // Instruction-only Memory Tagging Extension
    MTE2 = CPUFeature(1u) << 206u,          // Full Memory Tagging Extension
    PMUv3p5 = CPUFeature(1u) << 207u,       // PMU Extension version 3.5
    RNG = CPUFeature(1u) << 208u,           // Random number generator
    AMUv1p1 = CPUFeature(1u) << 209u,       // Activity Monitors Extension version 1.1
    ECV = CPUFeature(1u) << 210u,           // Enhanced counter virtualization
    FGT = CPUFeature(1u) << 211u,           // Fine Grain Traps
    MPAMv0p1 = CPUFeature(1u) << 212u,      // Memory Partitioning and Monitoring version 0.1
    MPAMv1p1 = CPUFeature(1u) << 213u,      // Memory Partitioning and Monitoring version 1.1
    MTPMU = CPUFeature(1u) << 214u,         // Multi-threaded PMU Extensions
    TWED = CPUFeature(1u) << 215u,          // Delayed trapping of WFE
    ETMv4 = CPUFeature(1u) << 216u,         // Embedded Trace Macrocell version4
    ETMv4p1 = CPUFeature(1u) << 217u,       // Embedded Trace Macrocell version 4.1
    ETMv4p2 = CPUFeature(1u) << 218u,       // Embedded Trace Macrocell version 4.2
    ETMv4p3 = CPUFeature(1u) << 219u,       // Embedded Trace Macrocell version 4.3
    ETMv4p4 = CPUFeature(1u) << 220u,       // Embedded Trace Macrocell version 4.3
    ETMv4p5 = CPUFeature(1u) << 221u,       // Embedded Trace Macrocell version 4.4
    ETMv4p6 = CPUFeature(1u) << 222u,       // Embedded Trace Macrocell version 4.5
    GICv3 = CPUFeature(1u) << 223u,         // Generic Interrupt Controller version 3
    GICv3p1 = CPUFeature(1u) << 224u,       // Generic Interrupt Controller version 3.1
    // Note: cf. https://developer.arm.com/documentation/ihi0069/h/?lang=en
    GICv3_LEGACY = CPUFeature(1u) << 225u, // Support for GICv2 legacy operation
    GICv3_TDIR = CPUFeature(1u) << 226u,   // Trapping Non-secure EL1 writes to ICV_DIR
    GICv4 = CPUFeature(1u) << 227u,        // Generic Interrupt Controller version 4
    GICv4p1 = CPUFeature(1u) << 228u,      // Generic Interrupt Controller version 4.1
    PMUv3 = CPUFeature(1u) << 229u,        // PMU extension version 3
    ETE = CPUFeature(1u) << 230u,          // Embedded Trace Extension
    ETEv1p1 = CPUFeature(1u) << 231u,      // Embedded Trace Extension, version 1.1
    SVE2 = CPUFeature(1u) << 232u,         // SVE version 2
    SVE_AES = CPUFeature(1u) << 233u,      // SVE AES instructions
    SVE_PMULL128 = CPUFeature(1u) << 234u, // SVE PMULL instructions; SVE2-AES is split into AES and PMULL support
    SVE_BitPerm = CPUFeature(1u) << 235u,  // SVE Bit Permute
    SVE_SHA3 = CPUFeature(1u) << 236u,     // SVE SHA-3 instructions
    SVE_SM4 = CPUFeature(1u) << 237u,      // SVE SM4 instructions
    TME = CPUFeature(1u) << 238u,          // Transactional Memory Extension
    TRBE = CPUFeature(1u) << 239u,         // Trace Buffer Extension
    SME = CPUFeature(1u) << 240u,          // Scalable Matrix Extension

    __End = CPUFeature(1u) << 255u); // SENTINEL VALUE

CPUFeature::Type detect_cpu_features();
StringView cpu_feature_to_name(CPUFeature::Type const&);
StringView cpu_feature_to_description(CPUFeature::Type const&);
NonnullOwnPtr<KString> build_cpu_feature_names(CPUFeature::Type const&);

u8 detect_physical_address_bit_width();
u8 detect_virtual_address_bit_width();

}
