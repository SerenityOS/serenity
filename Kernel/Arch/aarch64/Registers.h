/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 * Copyright (c) 2022, Konrad <konrad@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>

namespace Kernel::Aarch64 {

// https://developer.arm.com/documentation/ddi0601/2022-09/AArch64-Registers/ID-AA64ISAR0-EL1--AArch64-Instruction-Set-Attribute-Register-0?lang=en
// ID_AA64ISAR0_EL1, AArch64 Instruction Set Attribute Register 0
struct alignas(u64) ID_AA64ISAR0_EL1 {
    int : 4;
    int AES : 4;
    int SHA1 : 4;
    int SHA2 : 4;
    int CRC32 : 4;
    int Atomic : 4;
    int TME : 4;
    int RDM : 4;
    int SHA3 : 4;
    int SM3 : 4;
    int SM4 : 4;
    int DP : 4;
    int FHM : 4;
    int TS : 4;
    int TLB : 4;
    int RNDR : 4;

    static inline ID_AA64ISAR0_EL1 read()
    {
        ID_AA64ISAR0_EL1 feature_register;

        asm("mrs %[value], ID_AA64ISAR0_EL1"
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64ISAR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64ISAR1-EL1--AArch64-Instruction-Set-Attribute-Register-1
// ID_AA64ISAR1_EL1, AArch64 Instruction Set Attribute Register 1
struct alignas(u64) ID_AA64ISAR1_EL1 {
    int DPB : 4;
    int APA : 4;
    int API : 4;
    int JSCVT : 4;
    int FCMA : 4;
    int LRCPC : 4;
    int GPA : 4;
    int GPI : 4;
    int FRINTTS : 4;
    int SB : 4;
    int SPECRES : 4;
    int BF16 : 4;
    int DGH : 4;
    int I8MM : 4;
    int XS : 4;
    int LS64 : 4;

    static inline ID_AA64ISAR1_EL1 read()
    {
        ID_AA64ISAR1_EL1 feature_register;

        asm("mrs %[value], ID_AA64ISAR1_EL1"
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64ISAR1_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64ISAR2-EL1--AArch64-Instruction-Set-Attribute-Register-2
// ID_AA64ISAR2_EL1, AArch64 Instruction Set Attribute Register 2
struct alignas(u64) ID_AA64ISAR2_EL1 {
    int WFxT : 4;
    int RPRES : 4;
    int GPA3 : 4;
    int APA3 : 4;
    int MOPS : 4;
    int BC : 4;
    int PAC_frac : 4;
    int CLRBHB : 4;
    int SYSREG_128 : 4;
    int SYSINSTR_128 : 4;
    int PRFMSLC : 4;
    int : 4;
    int RPRFM : 4;
    int CSSC : 4;
    int : 8;

    static inline ID_AA64ISAR2_EL1 read()
    {
        ID_AA64ISAR2_EL1 feature_register;

        asm("mrs %[value], ID_AA64ISAR2_EL1"
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64ISAR2_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64PFR0-EL1--AArch64-Processor-Feature-Register-0
// ID_AA64PFR0_EL1, AArch64 Processor Feature Register 0
struct alignas(u64) ID_AA64PFR0_EL1 {
    int EL0 : 4;
    int EL1 : 4;
    int EL2 : 4;
    int EL3 : 4;
    int FP : 4;
    int AdvSIMD : 4;
    int GIC : 4;
    int RAS : 4;
    int SVE : 4;
    int SEL2 : 4;
    int MPAM : 4;
    int AMU : 4;
    int DIT : 4;
    int RME : 4;
    int CSV2 : 4;
    int CSV3 : 4;

    static inline ID_AA64PFR0_EL1 read()
    {
        ID_AA64PFR0_EL1 feature_register;

        asm("mrs %[value], ID_AA64PFR0_EL1"
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64PFR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64PFR1-EL1--AArch64-Processor-Feature-Register-1
// ID_AA64PFR1_EL1, AArch64 Processor Feature Register 1
struct alignas(u64) ID_AA64PFR1_EL1 {
    int BT : 4;
    int SSBS : 4;
    int MTE : 4;
    int RAS_frac : 4;
    int MPAM_frac : 4;
    int : 4;
    int SME : 4;
    int RNDR_trap : 4;
    int CSV2_frac : 4;
    int NMI : 4;
    int MTE_frac : 4;
    int GCS : 4;
    int THE : 4;
    int MTEX : 4;
    int DF2 : 4;
    int PFAR : 4;

    static inline ID_AA64PFR1_EL1 read()
    {
        ID_AA64PFR1_EL1 feature_register;

        asm("mrs %[value], ID_AA64PFR1_EL1"
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64PFR1_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64PFR2-EL1--AArch64-Processor-Feature-Register-2
// ID_AA64PFR2_EL1, AArch64 Processor Feature Register 2
struct alignas(u64) ID_AA64PFR2_EL1 {
    int MTEPERM : 4;
    int MTESTOREONLY : 4;
    int MTEFAR : 4;
    int : 20;
    int : 32;

    static inline ID_AA64PFR2_EL1 read()
    {
        ID_AA64PFR2_EL1 feature_register;

        asm("mrs %[value], s3_0_c0_c4_2" // encoded ID_AA64PFR2_EL1 register
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64PFR2_EL1) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-12/AArch64-Registers/MPIDR-EL1--Multiprocessor-Affinity-Register?lang=en
// MPIDR_EL1, Multiprocessor Affinity Register
struct alignas(u64) MPIDR_EL1 {
    int Aff0 : 8;
    int Aff1 : 8;
    int Aff2 : 8;
    int MT : 1;
    int : 5;
    int U : 1;
    int : 1;
    int Aff3 : 8;
    int : 24;

    static inline MPIDR_EL1 read()
    {
        MPIDR_EL1 affinity_register;

        asm("mrs %[value], MPIDR_EL1"
            : [value] "=r"(affinity_register));

        return affinity_register;
    }
};
static_assert(sizeof(MPIDR_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64MMFR0-EL1--AArch64-Memory-Model-Feature-Register-0
// ID_AA64MMFR0_EL1, AArch64 Memory Model Feature Register 0
struct alignas(u64) ID_AA64MMFR0_EL1 {
    int PARange : 4;
    int ASIDBits : 4;
    int BigEnd : 4;
    int SNSMem : 4;
    int BigEndEL0 : 4;
    int TGran16 : 4;
    int TGran64 : 4;
    int TGran4 : 4;
    int TGran16_2 : 4;
    int TGran64_2 : 4;
    int TGran4_2 : 4;
    int ExS : 4;
    int : 8;
    int FGT : 4;
    int ECV : 4;

    static inline ID_AA64MMFR0_EL1 read()
    {
        ID_AA64MMFR0_EL1 feature_register;

        asm("mrs %[value], ID_AA64MMFR0_EL1"
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64MMFR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64MMFR1-EL1--AArch64-Memory-Model-Feature-Register-1
// ID_AA64MMFR1_EL1, AArch64 Memory Model Feature Register 1
struct alignas(u64) ID_AA64MMFR1_EL1 {
    int HAFDBS : 4;
    int VMIDBits : 4;
    int VH : 4;
    int HPDS : 4;
    int LO : 4;
    int PAN : 4;
    int SpecSEI : 4;
    int XNX : 4;
    int TWED : 4;
    int ETS : 4;
    int HCX : 4;
    int AFP : 4;
    int nTLBPA : 4;
    int TIDCP1 : 4;
    int CMOW : 4;
    int ECBHB : 4;

    static inline ID_AA64MMFR1_EL1 read()
    {
        ID_AA64MMFR1_EL1 feature_register;

        asm("mrs %[value], ID_AA64MMFR1_EL1"
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64MMFR1_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64MMFR2-EL1--AArch64-Memory-Model-Feature-Register-2
// ID_AA64MMFR2_EL1, AArch64 Memory Model Feature Register 2
struct alignas(u64) ID_AA64MMFR2_EL1 {
    int CnP : 4;
    int UAO : 4;
    int LSM : 4;
    int IESB : 4;
    int VARange : 4;
    int CCIDX : 4;
    int NV : 4;
    int ST : 4;
    int AT : 4;
    int IDS : 4;
    int FWB : 4;
    int : 4;
    int TTL : 4;
    int BBM : 4;
    int EVT : 4;
    int E0PD : 4;

    static inline ID_AA64MMFR2_EL1 read()
    {
        ID_AA64MMFR2_EL1 feature_register;

        asm("mrs %[value], ID_AA64MMFR2_EL1"
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64MMFR2_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64MMFR3-EL1--AArch64-Memory-Model-Feature-Register-3
// ID_AA64MMFR3_EL1, AArch64 Memory Model Feature Register 3
struct alignas(u64) ID_AA64MMFR3_EL1 {
    int TCRX : 4;
    int SCTLRX : 4;
    int S1PIE : 4;
    int S2PIE : 4;
    int S1POE : 4;
    int S2POE : 4;
    int AIE : 4;
    int MEC : 4;
    int D128 : 4;
    int D128_2 : 4;
    int SNERR : 4;
    int ANERR : 4;
    int : 4;
    int SDERR : 4;
    int ADERR : 4;
    int Spec_FPACC : 4;

    static inline ID_AA64MMFR3_EL1 read()
    {
        ID_AA64MMFR3_EL1 feature_register;

        asm("mrs %[value], s3_0_c0_c7_3" // encoded ID_AA64MMFR3_EL1 register
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64MMFR3_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64MMFR4-EL1--AArch64-Memory-Model-Feature-Register-4
// ID_AA64MMFR4_EL1, AArch64 Memory Model Feature Register 4
struct alignas(u64) ID_AA64MMFR4_EL1 {
    int : 4;
    int EIESB : 4;
    int : 24;
    int : 32;

    static inline ID_AA64MMFR4_EL1 read()
    {
        ID_AA64MMFR4_EL1 feature_register;

        asm("mrs %[value], s3_0_c0_c7_4" // encoded ID_AA64MMFR4_EL1 register
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64MMFR4_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64SMFR0-EL1--SME-Feature-ID-register-0
// ID_AA64SMFR0_EL1, AArch64 SME Feature ID register 0
struct alignas(u64) ID_AA64SMFR0_EL1 {
    int : 32;
    int F32F32 : 1;
    int BI32I32 : 1;
    int B16F32 : 1;
    int F16F32 : 1;
    int I8I32 : 4;
    int : 2;
    int F16F16 : 1;
    int B16B16 : 1;
    int I16I32 : 4;
    int F64F64 : 1;
    int : 3;
    int I16I64 : 4;
    int SMEver : 4;
    int : 3;
    int FA64 : 1;

    static inline ID_AA64SMFR0_EL1 read()
    {
        ID_AA64SMFR0_EL1 feature_register;

        asm("mrs %[value], s3_0_c0_c4_5" // encoded ID_AA64SMFR0_EL1 register
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64SMFR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64ZFR0-EL1--SVE-Feature-ID-register-0
// ID_AA64ZFR0_EL1, AArch64 SVE Feature ID register 0
struct alignas(u64) ID_AA64ZFR0_EL1 {
    int SVEver : 4;
    int AES : 4;
    int : 8;
    int BitPerm : 4;
    int BF16 : 4;
    int B16B16 : 4;
    int : 4;
    int SHA3 : 4;
    int : 4;
    int SM4 : 4;
    int I8MM : 4;
    int : 4;
    int F32MM : 4;
    int F64MM : 4;
    int : 4;

    static inline ID_AA64ZFR0_EL1 read()
    {
        ID_AA64ZFR0_EL1 feature_register;

        asm("mrs %[value], s3_0_c0_c4_4" // encoded ID_AA64ZFR0_EL1 register
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64ZFR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64DFR0-EL1--AArch64-Debug-Feature-Register-0
// ID_AA64DFR0_EL1, AArch64 Debug Feature Register 0
struct alignas(u64) ID_AA64DFR0_EL1 {
    int DebugVer : 4;
    int TraceVer : 4;
    int PMUVer : 4;
    int BRPs : 4;
    int PMSS : 4;
    int WRPs : 4;
    int SEBEP : 4;
    int CTX_CMPs : 4;
    int PMSVer : 4;
    int DoubleLock : 4;
    int TraceFilt : 4;
    int TraceBuffer : 4;
    int MTPMU : 4;
    int BRBE : 4;
    int ExtTrcBuff : 4;
    int HPMN0 : 4;

    static inline ID_AA64DFR0_EL1 read()
    {
        ID_AA64DFR0_EL1 feature_register;

        asm("mrs %[value], ID_AA64DFR0_EL1"
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64DFR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64DFR1-EL1--AArch64-Debug-Feature-Register-1
// ID_AA64DFR1_EL1, AArch64 Debug Feature Register 1
struct alignas(u64) ID_AA64DFR1_EL1 {
    int SYSPMUID : 8;
    int BRPs : 8;
    int WRPs : 8;
    int CTX_CMPs : 8;
    int SPMU : 4;
    int PMICNTR : 4;
    int ABLE : 4;
    int ITE : 4;
    int EBEP : 4;
    int : 4;
    int ABL_CMPs : 8;

    static inline ID_AA64DFR1_EL1 read()
    {
        ID_AA64DFR1_EL1 feature_register;

        asm("mrs %[value], ID_AA64DFR1_EL1"
            : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64DFR1_EL1) == 8);

// https://developer.arm.com/documentation/ddi0595/2020-12/AArch64-Registers/CNTFRQ-EL0--Counter-timer-Frequency-register
// CNTFRQ_EL0, Counter-timer Frequency register
struct alignas(u64) CNTFRQ_EL0 {
    int : 32;
    int ClockFrequency : 32;

    static inline CNTFRQ_EL0 read()
    {
        CNTFRQ_EL0 frequency;

        asm("mrs %[value], CNTFRQ_EL0"
            : [value] "=r"(frequency));

        return frequency;
    }
};
static_assert(sizeof(CNTFRQ_EL0) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-06/AArch64-Registers/TCR-EL1--Translation-Control-Register--EL1-
// Translation Control Register
struct alignas(u64) TCR_EL1 {

    enum Shareability {
        NonSharable = 0b00,
        OuterShareable = 0b10,
        InnerShareable = 0b11,
    };
    enum OuterCacheability {
        NormalMemory_Outer_NonCacheable = 0b00,
        NormalMemory_Outer_WriteBack_ReadAllocate_WriteAllocateCacheable = 0b01,
        NormalMemory_Outer_WriteThrough_ReadAllocate_NoWriteAllocateCacheable = 0b10,
        NormalMemory_Outer_WriteBack_ReadAllocate_NoWriteAllocateCacheable = 0b11,
    };
    enum InnerCacheability {
        NormalMemory_Inner_NonCacheable = 0b00,
        NormalMemory_Inner_WriteBack_ReadAllocate_WriteAllocateCacheable = 0b01,
        NormalMemory_Inner_WriteThrough_ReadAllocate_NoWriteAllocateCacheable = 0b10,
        NormalMemory_Inner_WriteBack_ReadAllocate_NoWriteAllocateCacheable = 0b11,
    };

    // In AArch64, you have 3 possible translation granules to choose from,
    // each of which results in a different set of page sizes:
    // - 4KB granule: 4KB, 2MB, and 1GB pages.
    // - 16KB granule: 16KB and 32MB pages.
    // - 64KB granule: 64KB and 512MB pages.
    //
    // (https://stackoverflow.com/a/34269498)

    enum class TG1GranuleSize : int {
        Size_16KB = 0b01,
        Size_4KB = 0b10,
        Size_64KB = 0b11,
    };

    enum class TG0GranuleSize : int {
        Size_4KB = 0b00,
        Size_64KB = 0b01,
        Size_16KB = 0b10,
    };

    int T0SZ : 6;
    int RES0_0 : 1;
    int EPD0 : 1;
    InnerCacheability IRGN0 : 2;
    OuterCacheability ORGN0 : 2;
    Shareability SH0 : 2;
    TG0GranuleSize TG0 : 2;

    int T1SZ : 6;
    int A1 : 1;
    int EPD1 : 1;
    InnerCacheability IRGN1 : 2;
    OuterCacheability ORGN1 : 2;
    Shareability SH1 : 2;
    TG1GranuleSize TG1 : 2;

    int IPS : 3;
    int RES0_1 : 1;
    int AS : 1;
    int TBI0 : 1;
    int TBI1 : 1;
    int HA : 1;
    int HD : 1;
    int HPD0 : 1;
    int HPD1 : 1;
    int HWU059 : 1;
    int HWU060 : 1;
    int HWU061 : 1;
    int HWU062 : 1;

    int HWU159 : 1;
    int HWU160 : 1;
    int HWU161 : 1;
    int HWU162 : 1;

    int TBID0 : 1;
    int TBID1 : 1;
    int NFD0 : 1;
    int NFD1 : 1;

    int E0PD0 : 1;
    int E0PD1 : 1;
    int TCMA0 : 1;
    int TCMA1 : 1;
    int DS : 1;
    int RES0_2 : 4;

    static inline void write(TCR_EL1 tcr_el1)
    {
        asm("msr tcr_el1, %[value]" ::[value] "r"(tcr_el1));
    }

    static inline TCR_EL1 read()
    {
        TCR_EL1 tcr_el1;

        asm("mrs %[value], tcr_el1_el1"
            : [value] "=r"(tcr_el1));

        return tcr_el1;
    }

    static constexpr TCR_EL1 reset_value()
    {
        return {};
    }
};
static_assert(sizeof(TCR_EL1) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-03/AArch64-Registers/SCTLR-EL1--System-Control-Register--EL1-
// System Control Register
struct alignas(u64) SCTLR_EL1 {
    int M : 1;
    int A : 1;
    int C : 1;
    int SA : 1;
    int SA0 : 1;
    int CP15BEN : 1;
    int _reserved6 : 1 = 0;
    int ITD : 1;
    int SED : 1;
    int UMA : 1;
    int _reserved10 : 1 = 0;
    int _reserved11 : 1 = 1;
    int I : 1;
    int EnDB : 1;
    int DZE : 1;
    int UCT : 1;
    int nTWI : 1;
    int _reserved17 : 1 = 0;
    int nTWE : 1;
    int WXN : 1;
    int _reserved20 : 1 = 1;
    int IESB : 1;
    int _reserved22 : 1 = 1;
    int SPAN : 1;
    int E0E : 1;
    int EE : 1;
    int UCI : 1;
    int EnDA : 1;
    int nTLSMD : 1;
    int LSMAOE : 1;
    int EnIB : 1;
    int EnIA : 1;
    int _reserved32 : 3 = 0;
    int BT0 : 1;
    int BT1 : 1;
    int ITFSB : 1;
    int TCF0 : 2;
    int TCF : 2;
    int ATA0 : 1;
    int ATA : 1;
    int DSSBS : 1;
    int TWEDEn : 1;
    int TWEDEL : 4;
    int _reserved50 : 4 = 0;
    int EnASR : 1;
    int EnAS0 : 1;
    int EnALS : 1;
    int EPAN : 1;
    int _reserved58 : 6 = 0;

    static inline void write(SCTLR_EL1 sctlr_el1)
    {
        asm("msr sctlr_el1, %[value]" ::[value] "r"(sctlr_el1));
    }

    static inline SCTLR_EL1 read()
    {
        SCTLR_EL1 sctlr;

        asm("mrs %[value], sctlr_el1"
            : [value] "=r"(sctlr));

        return sctlr;
    }

    static constexpr SCTLR_EL1 reset_value()
    {
        SCTLR_EL1 system_control_register_el1 = {};
        system_control_register_el1.LSMAOE = 1;
        system_control_register_el1.nTLSMD = 1;
        system_control_register_el1.SPAN = 1;
        system_control_register_el1.IESB = 1;
        return system_control_register_el1;
    }
};
static_assert(sizeof(SCTLR_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-09/AArch64-Registers/MIDR-EL1--Main-ID-Register?lang=en
// MIDR_EL1, Main ID Register
struct alignas(u64) MIDR_EL1 {
    int Revision : 4;
    int PartNum : 12;
    int Architecture : 4;
    int Variant : 4;
    int Implementer : 8;
    int : 32;

    static inline MIDR_EL1 read()
    {
        MIDR_EL1 main_id_register;

        asm("mrs %[value], MIDR_EL1"
            : [value] "=r"(main_id_register));

        return main_id_register;
    }
};
static_assert(sizeof(MIDR_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-09/AArch64-Registers/AIDR-EL1--Auxiliary-ID-Register?lang=en
// AIDR_EL1, Auxiliary ID Register
struct alignas(u64) AIDR_EL1 {
    u64 AIDR : 64;

    static inline AIDR_EL1 read()
    {
        AIDR_EL1 auxiliary_id_register;

        asm("mrs %[value], AIDR_EL1"
            : [value] "=r"(auxiliary_id_register));

        return auxiliary_id_register;
    }
};
static_assert(sizeof(AIDR_EL1) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-06/AArch64-Registers/HCR-EL2--Hypervisor-Configuration-Register
// Hypervisor Configuration Register
struct alignas(u64) HCR_EL2 {
    int VM : 1;
    int SWIO : 1;
    int PTW : 1;
    int FMO : 1;
    int IMO : 1;
    int AMO : 1;
    int VF : 1;
    int VI : 1;
    int VSE : 1;
    int FB : 1;
    int BSU : 2;
    int DC : 1;
    int TWI : 1;
    int TWE : 1;
    int TID0 : 1;
    int TID1 : 1;
    int TID2 : 1;
    int TID3 : 1;
    int TSC : 1;
    int TIPDCP : 1;
    int TACR : 1;
    int TSW : 1;
    int TPCF : 1;
    int TPU : 1;
    int TTLB : 1;
    int TVM : 1;
    int TGE : 1;
    int TDZ : 1;
    int HCD : 1;
    int TRVM : 1;
    int RW : 1;
    int CD : 1;
    int ID : 1;
    int E2H : 1;
    int TLOR : 1;
    int TERR : 1;
    int MIOCNCE : 1;
    int _reserved39 : 1 = 0;
    int APK : 1 = 0;
    int API : 1 = 0;
    int NV : 1 = 0;
    int NV1 : 1 = 0;
    int AT : 1 = 0;
    int _reserved45 : 18 = 0;

    static inline void write(HCR_EL2 hcr_el2)
    {
        asm("msr hcr_el2, %[value]" ::[value] "r"(hcr_el2));
    }

    static inline HCR_EL2 read()
    {
        HCR_EL2 spsr;

        asm("mrs %[value], hcr_el2"
            : [value] "=r"(spsr));

        return spsr;
    }
};
static_assert(sizeof(HCR_EL2) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-06/AArch64-Registers/SCR-EL3--Secure-Configuration-Register
// Secure Configuration Register
struct alignas(u64) SCR_EL3 {
    int NS : 1;
    int IRQ : 1;
    int FIQ : 1;
    int EA : 1;
    int _reserved4 : 1 = 1;
    int _reserved5 : 1 = 1;
    int _reserved6 : 1 = 0;
    int SMD : 1;
    int HCE : 1;
    int SIF : 1;
    int RW : 1;
    int ST : 1;
    int TWI : 1;
    int TWE : 1;
    int TLOR : 1;
    int TERR : 1;
    int APK : 1;
    int API : 1;
    int EEL2 : 1;
    int EASE : 1;
    int NMEA : 1;
    int FIEN : 1;
    int _reserved22 : 3 = 0;
    int EnSCXT : 1;
    int ATA : 1;
    int FGTEn : 1;
    int ECVEn : 1;
    int TWEDEn : 1;
    int TWEDEL : 4;
    int _reserved34 : 1 = 0;
    int AMVOFFEN : 1;
    int EnAS0 : 1;
    int ADEn : 1;
    int HXEn : 1;
    int _reserved39 : 14 = 0;

    static inline void write(SCR_EL3 scr_el3)
    {
        asm("msr scr_el3, %[value]" ::[value] "r"(scr_el3));
    }

    static inline SCR_EL3 read()
    {
        SCR_EL3 scr;

        asm("mrs %[value], scr_el3"
            : [value] "=r"(scr));

        return scr;
    }
};
static_assert(sizeof(SCR_EL3) == 8);

// C5.2.18 SPSR_EL1, Saved Program Status Register (EL1)
struct alignas(u64) SPSR_EL1 {
    enum Mode : u8 {
        EL0t = 0b0000,
        EL1t = 0b0100,
        EL1h = 0b0101
    };

    Mode M : 4;
    int M_4 : 1 = 0;
    int _reserved5 : 1 = 0;
    int F : 1;
    int I : 1;
    int A : 1;
    int D : 1;
    int BTYPE : 2;
    int SSBS : 1;
    int _reserved13 : 7 = 0;
    int IL : 1;
    int SS : 1;
    int PAN : 1;
    int UA0 : 1;
    int DIT : 1;
    int TCO : 1;
    int _reserved26 : 2 = 0;
    int V : 1;
    int C : 1;
    int Z : 1;
    int N : 1;
    int _reserved32 : 32 = 0;

    static inline void write(SPSR_EL1 spsr_el1)
    {
        asm("msr spsr_el1, %[value]" ::[value] "r"(spsr_el1));
    }

    static inline SPSR_EL1 read()
    {
        SPSR_EL1 spsr;

        asm("mrs %[value], spsr_el1"
            : [value] "=r"(spsr));

        return spsr;
    }
};
static_assert(sizeof(SPSR_EL1) == 8);

struct alignas(u64) SPSR_EL2 {
    enum Mode : u16 {
        EL0t = 0b0000,
        EL1t = 0b0100,
        EL1h = 0b0101,
        EL2t = 0b1000,
        EL2h = 0b1001
    };

    Mode M : 4;
    int M_4 : 1 = 0;
    int _reserved5 : 1 = 0;
    int F : 1;
    int I : 1;
    int A : 1;
    int D : 1;
    int BTYPE : 2;
    int SSBS : 1;
    int _reserved13 : 7 = 0;
    int IL : 1;
    int SS : 1;
    int PAN : 1;
    int UA0 : 1;
    int DIT : 1;
    int TCO : 1;
    int _reserved26 : 2 = 0;
    int V : 1;
    int C : 1;
    int Z : 1;
    int N : 1;
    int _reserved32 : 32 = 0;

    static inline void write(SPSR_EL2 spsr_el2)
    {
        asm("msr spsr_el2, %[value]" ::[value] "r"(spsr_el2));
    }

    static inline SPSR_EL2 read()
    {
        SPSR_EL2 spsr;

        asm("mrs %[value], spsr_el2"
            : [value] "=r"(spsr));

        return spsr;
    }
};
static_assert(sizeof(SPSR_EL2) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-06/AArch64-Registers/SPSR-EL3--Saved-Program-Status-Register--EL3-
// Saved Program Status Register
struct alignas(u64) SPSR_EL3 {
    enum Mode : uint16_t {
        EL0t = 0b0000,
        EL1t = 0b0100,
        EL1h = 0b0101,
        EL2t = 0b1000,
        EL2h = 0b1001,
        EL3t = 0b1100,
        EL3h = 0b1101
    };

    Mode M : 4;
    int M_4 : 1 = 0;
    int _reserved5 : 1 = 0;
    int F : 1;
    int I : 1;
    int A : 1;
    int D : 1;
    int _reserved10 : 10 = 0;
    int IL : 1;
    int SS : 1;
    int PAN : 1;
    int UA0 : 1;
    int _reserved24 : 4 = 0;
    int V : 1;
    int C : 1;
    int Z : 1;
    int N : 1;
    int _reserved32 : 32 = 0;

    static inline void write(SPSR_EL3 spsr_el3)
    {
        asm("msr spsr_el3, %[value]" ::[value] "r"(spsr_el3));
    }

    static inline SPSR_EL3 read()
    {
        SPSR_EL3 spsr;

        asm("mrs %[value], spsr_el3"
            : [value] "=r"(spsr));

        return spsr;
    }
};
static_assert(sizeof(SPSR_EL3) == 8);

// https://developer.arm.com/documentation/ddi0595/2020-12/AArch64-Registers/MAIR-EL1--Memory-Attribute-Indirection-Register--EL1-?lang=en#fieldset_0-63_0
// Memory Attribute Indirection Register
struct alignas(u64) MAIR_EL1 {
    using AttributeEncoding = uint8_t;
    AttributeEncoding Attr[8];

    static inline void write(MAIR_EL1 mair_el1)
    {
        asm("msr mair_el1, %[value]" ::[value] "r"(mair_el1));
    }
};
static_assert(sizeof(MAIR_EL1) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-06/AArch64-Registers/ESR-EL1--Exception-Syndrome-Register--EL1-
// Exception Syndrome Register (EL1)
struct ESR_EL1 {
    u64 ISS : 25;
    u64 IL : 1;
    u64 EC : 6;
    u64 ISS2 : 5;
    u64 : 27;

    static inline ESR_EL1 read()
    {
        ESR_EL1 esr_el1;

        asm("mrs %[value], esr_el1"
            : [value] "=r"(esr_el1));

        return esr_el1;
    }
};
static_assert(sizeof(ESR_EL1) == 8);

// D17.2.40 FAR_EL1, Fault Address Register (EL1)
struct FAR_EL1 {
    u64 virtual_address;

    static inline FAR_EL1 read()
    {
        FAR_EL1 far_el1;

        asm("mrs %[value], far_el1"
            : [value] "=r"(far_el1));

        return far_el1;
    }
};
static_assert(sizeof(FAR_EL1) == 8);

// D17.2.37 ESR_EL1, Exception Syndrome Register (EL1)
static inline StringView exception_class_to_string(u8 exception_class)
{
    switch (exception_class) {
    case 0b000000:
        return "Unknown reason"sv;
    case 0b000001:
        return "Trapped WF* instruction execution"sv;
    case 0b000011:
        return "Trapped MCR or MRC access with (coproc==0b1111) that is not reported using EC 0b000000"sv;
    case 0b000100:
        return "Trapped MCRR or MRRC access with (coproc==0b1111) that is not reported using EC 0b000000"sv;
    case 0b000101:
        return "Trapped MCR or MRC access with (coproc==0b1110)"sv;
    case 0b000110:
        return "Trapped LDC or STC access"sv;
    case 0b000111:
        return "Access to SME, SVE, Advanced SIMD or floating-point functionality trapped by CPACR_EL1.FPEN, CPTR_EL2.FPEN, CPTR_EL2.TFP, or CPTR_EL3.TFP control"sv;
    case 0b001010:
        return "Trapped execution of an LD64B or ST64B* instruction"sv;
    case 0b001100:
        return "Trapped MRRC access with (coproc==0b1110)"sv;
    case 0b001101:
        return "Branch Target Exception"sv;
    case 0b001110:
        return "Illegal Execution state"sv;
    case 0b010001:
        return "SVC instruction execution in AArch32 state"sv;
    case 0b010101:
        return "SVC instruction execution in AArch64 state"sv;
    case 0b011000:
        return "Trapped MSR, MRS or System instruction execution in AArch64 state, that is not reported using EC 0b000000, 0b000001, or 0b000111"sv;
    case 0b011001:
        return "Access to SVE functionality trapped as a result of CPACR_EL1.ZEN, CPTR_EL2.ZEN, CPTR_EL2.TZ, or CPTR_EL3.EZ, that is not reported using EC 0b000000"sv;
    case 0b011011:
        return "Exception from an access to a TSTART instruction at EL0 when SCTLR_EL1.TME0 == 0, EL0 when SCTLR_EL2.TME0 == 0, at EL1 when SCTLR_EL1.TME == 0, at EL2 when SCTLR_EL2.TME == 0 or at EL3 when SCTLR_EL3.TME == 0"sv;
    case 0b011100:
        return "Exception from a Pointer Authentication instruction authentication failure"sv;
    case 0b011101:
        return "Access to SME functionality trapped as a result of CPACR_EL1.SMEN, CPTR_EL2.SMEN, CPTR_EL2.TSM, CPTR_EL3.ESM, or an attempted execution of an instruction that is illegal because of the value of PSTATE.SM or PSTATE.ZA, that is not reported using EC 0b000000"sv;
    case 0b011110:
        return "Exception from a Granule Protection Check"sv;
    case 0b100000:
        return "Instruction Abort from a lower Exception level"sv;
    case 0b100001:
        return "Instruction Abort taken without a change in Exception level"sv;
    case 0b100010:
        return "PC alignment fault exception"sv;
    case 0b100100:
        return "Data Abort exception from a lower Exception level"sv;
    case 0b100101:
        return "Data Abort exception taken without a change in Exception level"sv;
    case 0b100110:
        return "SP alignment fault exception"sv;
    case 0b100111:
        return "Memory Operation Exception"sv;
    case 0b101000:
        return "Trapped floating-point exception taken from AArch32 state"sv;
    case 0b101100:
        return "Trapped floating-point exception taken from AArch64 state"sv;
    case 0b101111:
        return "SError interrupt"sv;
    case 0b110000:
        return "Breakpoint exception from a lower Exception level"sv;
    case 0b110001:
        return "Breakpoint exception taken without a change in Exception level"sv;
    case 0b110010:
        return "Software Step exception from a lower Exception level"sv;
    case 0b110011:
        return "Software Step exception taken without a change in Exception level"sv;
    case 0b110100:
        return "Watchpoint exception from a lower Exception level"sv;
    case 0b110101:
        return "Watchpoint exception taken without a change in Exception level"sv;
    case 0b111000:
        return "BKPT instruction execution in AArch32 state"sv;
    case 0b111100:
        return "BRK instruction execution in AArch64 state"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

// D17.2.40 FAR_EL1, Fault Address Register (EL1)
static inline bool exception_class_has_set_far(u8 exception_class)
{
    // Faulting Virtual Address for synchronous exceptions taken to EL1. Exceptions that set the
    // FAR_EL1 are Instruction Aborts (EC 0x20 or 0x21), Data Aborts (EC 0x24 or 0x25), PC alignment
    // faults (EC 0x22), and Watchpoints (EC 0x34 or 0x35). ESR_EL1.EC holds the EC value for the
    // exception.
    switch (exception_class) {
    case 0x20:
    case 0x21:
    case 0x22:
    case 0x24:
    case 0x25:
    case 0x34:
    case 0x35:
        return true;
    default:
        return false;
    }
}

static inline bool exception_class_is_data_abort(u8 exception_class)
{
    return exception_class == 0x24 || exception_class == 0x25;
}

static inline bool exception_class_is_instruction_abort(u8 exception_class)
{
    return exception_class == 0x20 || exception_class == 0x21;
}

// D17.2.37 ESR_EL1, Exception Syndrome Register (EL1)
// ISS encoding for an exception from a Data Abort
// DFSC, bits [5:0]
static inline StringView data_fault_status_code_to_string(u32 instruction_specific_syndrome)
{
    u8 data_fault_status_code = instruction_specific_syndrome & 0x3f;
    switch (data_fault_status_code) {
    case 0b000000:
        return "Address size fault, level 0 of translation or translation table base register"sv;
    case 0b000001:
        return "Address size fault, level 1"sv;
    case 0b000010:
        return "Address size fault, level 2"sv;
    case 0b000011:
        return "Address size fault, level 3"sv;
    case 0b000100:
        return "Translation fault, level 0"sv;
    case 0b000101:
        return "Translation fault, level 1"sv;
    case 0b000110:
        return "Translation fault, level 2"sv;
    case 0b000111:
        return "Translation fault, level 3"sv;
    case 0b001001:
        return "Access flag fault, level 1"sv;
    case 0b001010:
        return "Access flag fault, level 2"sv;
    case 0b001011:
        return "Access flag fault, level 3"sv;
    case 0b001000:
        return "Access flag fault, level 0"sv;
    case 0b001100:
        return "Permission fault, level 0"sv;
    case 0b001101:
        return "Permission fault, level 1"sv;
    case 0b001110:
        return "Permission fault, level 2"sv;
    case 0b001111:
        return "Permission fault, level 3"sv;
    case 0b010000:
        return "Synchronous External abort, not on translation table walk or hardware update of translation table"sv;
    case 0b010001:
        return "Synchronous Tag Check Fault"sv;
    case 0b010011:
        return "Synchronous External abort on translation table walk or hardware update of translation table, level -1"sv;
    case 0b010100:
        return "Synchronous External abort on translation table walk or hardware update of translation table, level 0"sv;
    case 0b010101:
        return "Synchronous External abort on translation table walk or hardware update of translation table, level 1"sv;
    case 0b010110:
        return "Synchronous External abort on translation table walk or hardware update of translation table, level 2"sv;
    case 0b010111:
        return "Synchronous External abort on translation table walk or hardware update of translation table, level 3"sv;
    case 0b011000:
        return "Synchronous parity or ECC error on memory access, not on translation table walk"sv;
    case 0b011011:
        return "Synchronous parity or ECC error on memory access on translation table walk or hardware update of translation table, level -1"sv;
    case 0b011100:
        return "Synchronous parity or ECC error on memory access on translation table walk or hardware update of translation table, level 0"sv;
    case 0b011101:
        return "Synchronous parity or ECC error on memory access on translation table walk or hardware update of translation table, level 1"sv;
    case 0b011110:
        return "Synchronous parity or ECC error on memory access on translation table walk or hardware update of translation table, level 2"sv;
    case 0b011111:
        return "Synchronous parity or ECC error on memory access on translation table walk or hardware update of translation table, level 3"sv;
    case 0b100001:
        return "Alignment fault"sv;
    case 0b100011:
        return "Granule Protection Fault on translation table walk or hardware update of translation table, level -1"sv;
    case 0b100100:
        return "Granule Protection Fault on translation table walk or hardware update of translation table, level 0"sv;
    case 0b100101:
        return "Granule Protection Fault on translation table walk or hardware update of translation table, level 1"sv;
    case 0b100110:
        return "Granule Protection Fault on translation table walk or hardware update of translation table, level 2"sv;
    case 0b100111:
        return "Granule Protection Fault on translation table walk or hardware update of translation table, level 3"sv;
    case 0b101000:
        return "Granule Protection Fault, not on translation table walk or hardware update of translation table"sv;
    case 0b101001:
        return "Address size fault, level -1"sv;
    case 0b101011:
        return "Translation fault, level -1"sv;
    case 0b110000:
        return "TLB conflict abort"sv;
    case 0b110001:
        return "Unsupported atomic hardware update fault"sv;
    case 0b110100:
        return "IMPLEMENTATION DEFINED fault (Lockdown)"sv;
    case 0b110101:
        return "IMPLEMENTATION DEFINED fault (Unsupported Exclusive or Atomic access)"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

// https://developer.arm.com/documentation/ddi0601/2020-12/AArch64-Registers/DAIF--Interrupt-Mask-Bits?lang=en
// DAIF, Interrupt Mask Bits
struct DAIF {
    u64 : 6;
    u64 F : 1;
    u64 I : 1;
    u64 A : 1;
    u64 D : 1;
    u64 : 54;

    static inline DAIF read()
    {
        DAIF daif;

        asm("mrs %[value], daif"
            : [value] "=r"(daif));

        return daif;
    }

    // Clearing the I bit, causes interrupts to be enabled.
    static inline void clear_I()
    {
        asm volatile("msr daifclr, #2" ::
                         :);
    }

    // Setting the I bit, causes interrupts to be disabled.
    static inline void set_I()
    {
        asm volatile("msr daifset, #2" ::
                         :);
    }
};
static_assert(sizeof(DAIF) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-03/AArch64-Registers/NZCV--Condition-Flags
// NZCV, Condition Flags
struct alignas(u64) NZCV {
    u64 : 28;
    u64 V : 1;
    u64 C : 1;
    u64 Z : 1;
    u64 N : 1;
    u64 : 32;

    static inline NZCV read()
    {
        NZCV nzcv;
        asm volatile("mrs %[value], nzcv"
                     : [value] "=r"(nzcv));
        return nzcv;
    }
};
static_assert(sizeof(NZCV) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-03/AArch64-Registers/PMCCNTR-EL0--Performance-Monitors-Cycle-Count-Register
// PMCCNTR_EL0, Performance Monitors Cycle Count Register
struct alignas(u64) PMCCNTR_EL0 {
    u64 CCNT : 64;

    static inline PMCCNTR_EL0 read()
    {
        PMCCNTR_EL0 pmccntr_el0;
        asm volatile("mrs %[value], pmccntr_el0"
                     : [value] "=r"(pmccntr_el0));
        return pmccntr_el0;
    }
};
static_assert(sizeof(PMCCNTR_EL0) == 8);

}
