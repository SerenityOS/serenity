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
    u64 : 4;
    u64 AES : 4;
    u64 SHA1 : 4;
    u64 SHA2 : 4;
    u64 CRC32 : 4;
    u64 Atomic : 4;
    u64 TME : 4;
    u64 RDM : 4;
    u64 SHA3 : 4;
    u64 SM3 : 4;
    u64 SM4 : 4;
    u64 DP : 4;
    u64 FHM : 4;
    u64 TS : 4;
    u64 TLB : 4;
    u64 RNDR : 4;

    static inline ID_AA64ISAR0_EL1 read()
    {
        ID_AA64ISAR0_EL1 feature_register;

        asm volatile("mrs %[value], ID_AA64ISAR0_EL1"
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64ISAR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64ISAR1-EL1--AArch64-Instruction-Set-Attribute-Register-1
// ID_AA64ISAR1_EL1, AArch64 Instruction Set Attribute Register 1
struct alignas(u64) ID_AA64ISAR1_EL1 {
    u64 DPB : 4;
    u64 APA : 4;
    u64 API : 4;
    u64 JSCVT : 4;
    u64 FCMA : 4;
    u64 LRCPC : 4;
    u64 GPA : 4;
    u64 GPI : 4;
    u64 FRINTTS : 4;
    u64 SB : 4;
    u64 SPECRES : 4;
    u64 BF16 : 4;
    u64 DGH : 4;
    u64 I8MM : 4;
    u64 XS : 4;
    u64 LS64 : 4;

    static inline ID_AA64ISAR1_EL1 read()
    {
        ID_AA64ISAR1_EL1 feature_register;

        asm volatile("mrs %[value], ID_AA64ISAR1_EL1"
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64ISAR1_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64ISAR2-EL1--AArch64-Instruction-Set-Attribute-Register-2
// ID_AA64ISAR2_EL1, AArch64 Instruction Set Attribute Register 2
struct alignas(u64) ID_AA64ISAR2_EL1 {
    u64 WFxT : 4;
    u64 RPRES : 4;
    u64 GPA3 : 4;
    u64 APA3 : 4;
    u64 MOPS : 4;
    u64 BC : 4;
    u64 PAC_frac : 4;
    u64 CLRBHB : 4;
    u64 SYSREG_128 : 4;
    u64 SYSINSTR_128 : 4;
    u64 PRFMSLC : 4;
    u64 : 4;
    u64 RPRFM : 4;
    u64 CSSC : 4;
    u64 : 8;

    static inline ID_AA64ISAR2_EL1 read()
    {
        ID_AA64ISAR2_EL1 feature_register;

        asm volatile("mrs %[value], ID_AA64ISAR2_EL1"
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64ISAR2_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64PFR0-EL1--AArch64-Processor-Feature-Register-0
// ID_AA64PFR0_EL1, AArch64 Processor Feature Register 0
struct alignas(u64) ID_AA64PFR0_EL1 {
    u64 EL0 : 4;
    u64 EL1 : 4;
    u64 EL2 : 4;
    u64 EL3 : 4;
    u64 FP : 4;
    u64 AdvSIMD : 4;
    u64 GIC : 4;
    u64 RAS : 4;
    u64 SVE : 4;
    u64 SEL2 : 4;
    u64 MPAM : 4;
    u64 AMU : 4;
    u64 DIT : 4;
    u64 RME : 4;
    u64 CSV2 : 4;
    u64 CSV3 : 4;

    static inline ID_AA64PFR0_EL1 read()
    {
        ID_AA64PFR0_EL1 feature_register;

        asm volatile("mrs %[value], ID_AA64PFR0_EL1"
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64PFR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64PFR1-EL1--AArch64-Processor-Feature-Register-1
// ID_AA64PFR1_EL1, AArch64 Processor Feature Register 1
struct alignas(u64) ID_AA64PFR1_EL1 {
    u64 BT : 4;
    u64 SSBS : 4;
    u64 MTE : 4;
    u64 RAS_frac : 4;
    u64 MPAM_frac : 4;
    u64 : 4;
    u64 SME : 4;
    u64 RNDR_trap : 4;
    u64 CSV2_frac : 4;
    u64 NMI : 4;
    u64 MTE_frac : 4;
    u64 GCS : 4;
    u64 THE : 4;
    u64 MTEX : 4;
    u64 DF2 : 4;
    u64 PFAR : 4;

    static inline ID_AA64PFR1_EL1 read()
    {
        ID_AA64PFR1_EL1 feature_register;

        asm volatile("mrs %[value], ID_AA64PFR1_EL1"
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64PFR1_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64PFR2-EL1--AArch64-Processor-Feature-Register-2
// ID_AA64PFR2_EL1, AArch64 Processor Feature Register 2
struct alignas(u64) ID_AA64PFR2_EL1 {
    u64 MTEPERM : 4;
    u64 MTESTOREONLY : 4;
    u64 MTEFAR : 4;
    u64 : 20;
    u64 : 32;

    static inline ID_AA64PFR2_EL1 read()
    {
        ID_AA64PFR2_EL1 feature_register;

        asm volatile("mrs %[value], s3_0_c0_c4_2" // encoded ID_AA64PFR2_EL1 register
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64PFR2_EL1) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-12/AArch64-Registers/MPIDR-EL1--Multiprocessor-Affinity-Register?lang=en
// MPIDR_EL1, Multiprocessor Affinity Register
struct alignas(u64) MPIDR_EL1 {
    u64 Aff0 : 8;
    u64 Aff1 : 8;
    u64 Aff2 : 8;
    u64 MT : 1;
    u64 : 5;
    u64 U : 1;
    u64 : 1;
    u64 Aff3 : 8;
    u64 : 24;

    static inline MPIDR_EL1 read()
    {
        MPIDR_EL1 affinity_register;

        asm volatile("mrs %[value], MPIDR_EL1"
                     : [value] "=r"(affinity_register));

        return affinity_register;
    }
};
static_assert(sizeof(MPIDR_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64MMFR0-EL1--AArch64-Memory-Model-Feature-Register-0
// ID_AA64MMFR0_EL1, AArch64 Memory Model Feature Register 0
struct alignas(u64) ID_AA64MMFR0_EL1 {
    u64 PARange : 4;
    u64 ASIDBits : 4;
    u64 BigEnd : 4;
    u64 SNSMem : 4;
    u64 BigEndEL0 : 4;
    u64 TGran16 : 4;
    u64 TGran64 : 4;
    u64 TGran4 : 4;
    u64 TGran16_2 : 4;
    u64 TGran64_2 : 4;
    u64 TGran4_2 : 4;
    u64 ExS : 4;
    u64 : 8;
    u64 FGT : 4;
    u64 ECV : 4;

    static inline ID_AA64MMFR0_EL1 read()
    {
        ID_AA64MMFR0_EL1 feature_register;

        asm volatile("mrs %[value], ID_AA64MMFR0_EL1"
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64MMFR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64MMFR1-EL1--AArch64-Memory-Model-Feature-Register-1
// ID_AA64MMFR1_EL1, AArch64 Memory Model Feature Register 1
struct alignas(u64) ID_AA64MMFR1_EL1 {
    u64 HAFDBS : 4;
    u64 VMIDBits : 4;
    u64 VH : 4;
    u64 HPDS : 4;
    u64 LO : 4;
    u64 PAN : 4;
    u64 SpecSEI : 4;
    u64 XNX : 4;
    u64 TWED : 4;
    u64 ETS : 4;
    u64 HCX : 4;
    u64 AFP : 4;
    u64 nTLBPA : 4;
    u64 TIDCP1 : 4;
    u64 CMOW : 4;
    u64 ECBHB : 4;

    static inline ID_AA64MMFR1_EL1 read()
    {
        ID_AA64MMFR1_EL1 feature_register;

        asm volatile("mrs %[value], ID_AA64MMFR1_EL1"
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64MMFR1_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64MMFR2-EL1--AArch64-Memory-Model-Feature-Register-2
// ID_AA64MMFR2_EL1, AArch64 Memory Model Feature Register 2
struct alignas(u64) ID_AA64MMFR2_EL1 {
    u64 CnP : 4;
    u64 UAO : 4;
    u64 LSM : 4;
    u64 IESB : 4;
    u64 VARange : 4;
    u64 CCIDX : 4;
    u64 NV : 4;
    u64 ST : 4;
    u64 AT : 4;
    u64 IDS : 4;
    u64 FWB : 4;
    u64 : 4;
    u64 TTL : 4;
    u64 BBM : 4;
    u64 EVT : 4;
    u64 E0PD : 4;

    static inline ID_AA64MMFR2_EL1 read()
    {
        ID_AA64MMFR2_EL1 feature_register;

        asm volatile("mrs %[value], ID_AA64MMFR2_EL1"
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64MMFR2_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64MMFR3-EL1--AArch64-Memory-Model-Feature-Register-3
// ID_AA64MMFR3_EL1, AArch64 Memory Model Feature Register 3
struct alignas(u64) ID_AA64MMFR3_EL1 {
    u64 TCRX : 4;
    u64 SCTLRX : 4;
    u64 S1PIE : 4;
    u64 S2PIE : 4;
    u64 S1POE : 4;
    u64 S2POE : 4;
    u64 AIE : 4;
    u64 MEC : 4;
    u64 D128 : 4;
    u64 D128_2 : 4;
    u64 SNERR : 4;
    u64 ANERR : 4;
    u64 : 4;
    u64 SDERR : 4;
    u64 ADERR : 4;
    u64 Spec_FPACC : 4;

    static inline ID_AA64MMFR3_EL1 read()
    {
        ID_AA64MMFR3_EL1 feature_register;

        asm volatile("mrs %[value], s3_0_c0_c7_3" // encoded ID_AA64MMFR3_EL1 register
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64MMFR3_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64MMFR4-EL1--AArch64-Memory-Model-Feature-Register-4
// ID_AA64MMFR4_EL1, AArch64 Memory Model Feature Register 4
struct alignas(u64) ID_AA64MMFR4_EL1 {
    u64 : 4;
    u64 EIESB : 4;
    u64 : 24;
    u64 : 32;

    static inline ID_AA64MMFR4_EL1 read()
    {
        ID_AA64MMFR4_EL1 feature_register;

        asm volatile("mrs %[value], s3_0_c0_c7_4" // encoded ID_AA64MMFR4_EL1 register
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64MMFR4_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64SMFR0-EL1--SME-Feature-ID-register-0
// ID_AA64SMFR0_EL1, AArch64 SME Feature ID register 0
struct alignas(u64) ID_AA64SMFR0_EL1 {
    u64 : 32;
    u64 F32F32 : 1;
    u64 BI32I32 : 1;
    u64 B16F32 : 1;
    u64 F16F32 : 1;
    u64 I8I32 : 4;
    u64 : 2;
    u64 F16F16 : 1;
    u64 B16B16 : 1;
    u64 I16I32 : 4;
    u64 F64F64 : 1;
    u64 : 3;
    u64 I16I64 : 4;
    u64 SMEver : 4;
    u64 : 3;
    u64 FA64 : 1;

    static inline ID_AA64SMFR0_EL1 read()
    {
        ID_AA64SMFR0_EL1 feature_register;

        asm volatile("mrs %[value], s3_0_c0_c4_5" // encoded ID_AA64SMFR0_EL1 register
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64SMFR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64ZFR0-EL1--SVE-Feature-ID-register-0
// ID_AA64ZFR0_EL1, AArch64 SVE Feature ID register 0
struct alignas(u64) ID_AA64ZFR0_EL1 {
    u64 SVEver : 4;
    u64 AES : 4;
    u64 : 8;
    u64 BitPerm : 4;
    u64 BF16 : 4;
    u64 B16B16 : 4;
    u64 : 4;
    u64 SHA3 : 4;
    u64 : 4;
    u64 SM4 : 4;
    u64 I8MM : 4;
    u64 : 4;
    u64 F32MM : 4;
    u64 F64MM : 4;
    u64 : 4;

    static inline ID_AA64ZFR0_EL1 read()
    {
        ID_AA64ZFR0_EL1 feature_register;

        asm volatile("mrs %[value], s3_0_c0_c4_4" // encoded ID_AA64ZFR0_EL1 register
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64ZFR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64DFR0-EL1--AArch64-Debug-Feature-Register-0
// ID_AA64DFR0_EL1, AArch64 Debug Feature Register 0
struct alignas(u64) ID_AA64DFR0_EL1 {
    u64 DebugVer : 4;
    u64 TraceVer : 4;
    u64 PMUVer : 4;
    u64 BRPs : 4;
    u64 PMSS : 4;
    u64 WRPs : 4;
    u64 SEBEP : 4;
    u64 CTX_CMPs : 4;
    u64 PMSVer : 4;
    u64 DoubleLock : 4;
    u64 TraceFilt : 4;
    u64 TraceBuffer : 4;
    u64 MTPMU : 4;
    u64 BRBE : 4;
    u64 ExtTrcBuff : 4;
    u64 HPMN0 : 4;

    static inline ID_AA64DFR0_EL1 read()
    {
        ID_AA64DFR0_EL1 feature_register;

        asm volatile("mrs %[value], ID_AA64DFR0_EL1"
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64DFR0_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-12/AArch64-Registers/ID-AA64DFR1-EL1--AArch64-Debug-Feature-Register-1
// ID_AA64DFR1_EL1, AArch64 Debug Feature Register 1
struct alignas(u64) ID_AA64DFR1_EL1 {
    u64 SYSPMUID : 8;
    u64 BRPs : 8;
    u64 WRPs : 8;
    u64 CTX_CMPs : 8;
    u64 SPMU : 4;
    u64 PMICNTR : 4;
    u64 ABLE : 4;
    u64 ITE : 4;
    u64 EBEP : 4;
    u64 : 4;
    u64 ABL_CMPs : 8;

    static inline ID_AA64DFR1_EL1 read()
    {
        ID_AA64DFR1_EL1 feature_register;

        asm volatile("mrs %[value], ID_AA64DFR1_EL1"
                     : [value] "=r"(feature_register));

        return feature_register;
    }
};
static_assert(sizeof(ID_AA64DFR1_EL1) == 8);

// https://developer.arm.com/documentation/ddi0595/2020-12/AArch64-Registers/CNTFRQ-EL0--Counter-timer-Frequency-register
// CNTFRQ_EL0, Counter-timer Frequency register
struct alignas(u64) CNTFRQ_EL0 {
    u64 ClockFrequency : 32;
    u64 : 32;

    static inline CNTFRQ_EL0 read()
    {
        CNTFRQ_EL0 frequency;

        asm volatile("mrs %[value], CNTFRQ_EL0"
                     : [value] "=r"(frequency));

        return frequency;
    }
};
static_assert(sizeof(CNTFRQ_EL0) == 8);

// https://developer.arm.com/documentation/ddi0595/2020-12/AArch64-Registers/CNTP-TVAL-EL0--Counter-timer-Physical-Timer-TimerValue-register
// CNTP_TVAL_EL0, Counter-timer Physical Timer TimerValue register
struct alignas(u64) CNTP_TVAL_EL0 {
    u64 TimerValue : 32;
    u64 : 32;

    static inline CNTP_TVAL_EL0 read()
    {
        CNTP_TVAL_EL0 timer_value;

        asm volatile("mrs %[value], CNTP_TVAL_EL0"
                     : [value] "=r"(timer_value));

        return timer_value;
    }

    static inline void write(CNTP_TVAL_EL0 cntp_tval_el0)
    {
        asm volatile("msr CNTP_TVAL_EL0, %[value]" ::[value] "r"(cntp_tval_el0));
    }
};
static_assert(sizeof(CNTP_TVAL_EL0) == 8);

// https://developer.arm.com/documentation/ddi0595/2020-12/AArch64-Registers/CNTP-CTL-EL0--Counter-timer-Physical-Timer-Control-register
// CNTP_CTL_EL0, Counter-timer Physical Timer Control register
struct alignas(u64) CNTP_CTL_EL0 {
    u64 ENABLE : 1;
    u64 IMASK : 1;
    u64 ISTATUS : 1;
    u64 : 61;

    static inline CNTP_CTL_EL0 read()
    {
        CNTP_CTL_EL0 control_register;

        asm volatile("mrs %[value], CNTP_CTL_EL0"
                     : [value] "=r"(control_register));

        return control_register;
    }

    static inline void write(CNTP_CTL_EL0 cntp_ctl_el0)
    {
        asm volatile("msr CNTP_CTL_EL0, %[value]" ::[value] "r"(cntp_ctl_el0));
    }
};
static_assert(sizeof(CNTP_CTL_EL0) == 8);

// https://developer.arm.com/documentation/ddi0595/2020-12/AArch64-Registers/CNTPCT-EL0--Counter-timer-Physical-Count-register
// CNTPCT_EL0, Counter-timer Physical Count register
struct alignas(u64) CNTPCT_EL0 {
    u64 PhysicalCount;

    static inline CNTPCT_EL0 read()
    {
        CNTPCT_EL0 physical_count;

        asm volatile("mrs %[value], CNTPCT_EL0"
                     : [value] "=r"(physical_count));

        return physical_count;
    }
};
static_assert(sizeof(CNTPCT_EL0) == 8);

// https://developer.arm.com/documentation/ddi0595/2020-12/AArch64-Registers/CNTV-TVAL-EL0--Counter-timer-Virtual-Timer-TimerValue-register
// CNTV_TVAL_EL0, Counter-timer Virtual Timer TimerValue register
struct alignas(u64) CNTV_TVAL_EL0 {
    u64 TimerValue : 32;
    u64 : 32;

    static inline CNTV_TVAL_EL0 read()
    {
        CNTV_TVAL_EL0 timer_value;

        asm volatile("mrs %[value], CNTV_TVAL_EL0"
                     : [value] "=r"(timer_value));

        return timer_value;
    }

    static inline void write(CNTV_TVAL_EL0 cntv_tval_el0)
    {
        asm volatile("msr CNTV_TVAL_EL0, %[value]" ::[value] "r"(cntv_tval_el0));
    }
};
static_assert(sizeof(CNTV_TVAL_EL0) == 8);

// https://developer.arm.com/documentation/ddi0595/2020-12/AArch64-Registers/CNTV-CTL-EL0--Counter-timer-Virtual-Timer-Control-register
// CNTV_CTL_EL0, Counter-timer Virtual Timer Control register
struct alignas(u64) CNTV_CTL_EL0 {
    u64 ENABLE : 1;
    u64 IMASK : 1;
    u64 ISTATUS : 1;
    u64 : 61;

    static inline CNTV_CTL_EL0 read()
    {
        CNTV_CTL_EL0 control_register;

        asm volatile("mrs %[value], CNTV_CTL_EL0"
                     : [value] "=r"(control_register));

        return control_register;
    }

    static inline void write(CNTV_CTL_EL0 cntv_ctl_el0)
    {
        asm volatile("msr CNTV_CTL_EL0, %[value]" ::[value] "r"(cntv_ctl_el0));
    }
};
static_assert(sizeof(CNTV_CTL_EL0) == 8);

// https://developer.arm.com/documentation/ddi0595/2020-12/AArch64-Registers/CNTVCT-EL0--Counter-timer-Virtual-Count-register
// CNTVCT_EL0, Counter-timer Virtual Count register
struct alignas(u64) CNTVCT_EL0 {
    u64 VirtualCount;

    static inline CNTVCT_EL0 read()
    {
        CNTVCT_EL0 virtual_count;

        asm volatile("mrs %[value], CNTVCT_EL0"
                     : [value] "=r"(virtual_count));

        return virtual_count;
    }
};
static_assert(sizeof(CNTVCT_EL0) == 8);

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

    enum class TG1GranuleSize : u8 {
        Size_16KB = 0b01,
        Size_4KB = 0b10,
        Size_64KB = 0b11,
    };

    enum class TG0GranuleSize : u8 {
        Size_4KB = 0b00,
        Size_64KB = 0b01,
        Size_16KB = 0b10,
    };

    u64 T0SZ : 6;
    u64 RES0_0 : 1;
    u64 EPD0 : 1;
    InnerCacheability IRGN0 : 2;
    OuterCacheability ORGN0 : 2;
    Shareability SH0 : 2;
    TG0GranuleSize TG0 : 2;

    u64 T1SZ : 6;
    u64 A1 : 1;
    u64 EPD1 : 1;
    InnerCacheability IRGN1 : 2;
    OuterCacheability ORGN1 : 2;
    Shareability SH1 : 2;
    TG1GranuleSize TG1 : 2;

    u64 IPS : 3;
    u64 RES0_1 : 1;
    u64 AS : 1;
    u64 TBI0 : 1;
    u64 TBI1 : 1;
    u64 HA : 1;
    u64 HD : 1;
    u64 HPD0 : 1;
    u64 HPD1 : 1;
    u64 HWU059 : 1;
    u64 HWU060 : 1;
    u64 HWU061 : 1;
    u64 HWU062 : 1;

    u64 HWU159 : 1;
    u64 HWU160 : 1;
    u64 HWU161 : 1;
    u64 HWU162 : 1;

    u64 TBID0 : 1;
    u64 TBID1 : 1;
    u64 NFD0 : 1;
    u64 NFD1 : 1;

    u64 E0PD0 : 1;
    u64 E0PD1 : 1;
    u64 TCMA0 : 1;
    u64 TCMA1 : 1;
    u64 DS : 1;
    u64 RES0_2 : 4;

    static inline void write(TCR_EL1 tcr_el1)
    {
        asm volatile("msr tcr_el1, %[value]" ::[value] "r"(tcr_el1));
    }

    static inline TCR_EL1 read()
    {
        TCR_EL1 tcr_el1;

        asm volatile("mrs %[value], tcr_el1"
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
    u64 M : 1;
    u64 A : 1;
    u64 C : 1;
    u64 SA : 1;
    u64 SA0 : 1;
    u64 CP15BEN : 1;
    u64 nAA : 1;
    u64 ITD : 1;
    u64 SED : 1;
    u64 UMA : 1;
    u64 EnRCTX : 1;
    u64 EOS : 1;
    u64 I : 1;
    u64 EnDB : 1;
    u64 DZE : 1;
    u64 UCT : 1;
    u64 nTWI : 1;
    u64 _reserved17 : 1 = 0;
    u64 nTWE : 1;
    u64 WXN : 1;
    u64 TSCXT : 1;
    u64 IESB : 1;
    u64 EIS : 1;
    u64 SPAN : 1;
    u64 E0E : 1;
    u64 EE : 1;
    u64 UCI : 1;
    u64 EnDA : 1;
    u64 nTLSMD : 1;
    u64 LSMAOE : 1;
    u64 EnIB : 1;
    u64 EnIA : 1;
    u64 _reserved32 : 3 = 0;
    u64 BT0 : 1;
    u64 BT1 : 1;
    u64 ITFSB : 1;
    u64 TCF0 : 2;
    u64 TCF : 2;
    u64 ATA0 : 1;
    u64 ATA : 1;
    u64 DSSBS : 1;
    u64 TWEDEn : 1;
    u64 TWEDEL : 4;
    u64 _reserved50 : 4 = 0;
    u64 EnASR : 1;
    u64 EnAS0 : 1;
    u64 EnALS : 1;
    u64 EPAN : 1;
    u64 _reserved58 : 6 = 0;

    static inline void write(SCTLR_EL1 sctlr_el1)
    {
        asm volatile("msr sctlr_el1, %[value]" ::[value] "r"(sctlr_el1));
    }

    static inline SCTLR_EL1 read()
    {
        SCTLR_EL1 sctlr;

        asm volatile("mrs %[value], sctlr_el1"
                     : [value] "=r"(sctlr));

        return sctlr;
    }

    static constexpr SCTLR_EL1 reset_value()
    {
        SCTLR_EL1 system_control_register_el1 = {};
        system_control_register_el1.SA = 1;
        system_control_register_el1.SA0 = 1;
        system_control_register_el1.ITD = 1;
        system_control_register_el1.SED = 1;
        system_control_register_el1.EOS = 1;
        system_control_register_el1.TSCXT = 1;
        system_control_register_el1.IESB = 1;
        system_control_register_el1.EIS = 1;
        system_control_register_el1.SPAN = 1;
        system_control_register_el1.LSMAOE = 1;
        system_control_register_el1.nTLSMD = 1;
        return system_control_register_el1;
    }
};
static_assert(sizeof(SCTLR_EL1) == 8);

// https://developer.arm.com/documentation/ddi0601/2022-09/AArch64-Registers/MIDR-EL1--Main-ID-Register?lang=en
// MIDR_EL1, Main ID Register
struct alignas(u64) MIDR_EL1 {
    u8 Revision : 4;
    u16 PartNum : 12;
    u8 Architecture : 4;
    u8 Variant : 4;
    u8 Implementer : 8;
    u64 : 32;

    static inline MIDR_EL1 read()
    {
        MIDR_EL1 main_id_register;

        asm volatile("mrs %[value], MIDR_EL1"
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

        asm volatile("mrs %[value], AIDR_EL1"
                     : [value] "=r"(auxiliary_id_register));

        return auxiliary_id_register;
    }
};
static_assert(sizeof(AIDR_EL1) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-06/AArch64-Registers/HCR-EL2--Hypervisor-Configuration-Register
// Hypervisor Configuration Register
struct alignas(u64) HCR_EL2 {
    u64 VM : 1;
    u64 SWIO : 1;
    u64 PTW : 1;
    u64 FMO : 1;
    u64 IMO : 1;
    u64 AMO : 1;
    u64 VF : 1;
    u64 VI : 1;
    u64 VSE : 1;
    u64 FB : 1;
    u64 BSU : 2;
    u64 DC : 1;
    u64 TWI : 1;
    u64 TWE : 1;
    u64 TID0 : 1;
    u64 TID1 : 1;
    u64 TID2 : 1;
    u64 TID3 : 1;
    u64 TSC : 1;
    u64 TIPDCP : 1;
    u64 TACR : 1;
    u64 TSW : 1;
    u64 TPCF : 1;
    u64 TPU : 1;
    u64 TTLB : 1;
    u64 TVM : 1;
    u64 TGE : 1;
    u64 TDZ : 1;
    u64 HCD : 1;
    u64 TRVM : 1;
    u64 RW : 1;
    u64 CD : 1;
    u64 ID : 1;
    u64 E2H : 1;
    u64 TLOR : 1;
    u64 TERR : 1;
    u64 MIOCNCE : 1;
    u64 _reserved39 : 1 = 0;
    u64 APK : 1 = 0;
    u64 API : 1 = 0;
    u64 NV : 1 = 0;
    u64 NV1 : 1 = 0;
    u64 AT : 1 = 0;
    u64 _reserved45 : 18 = 0;

    static inline void write(HCR_EL2 hcr_el2)
    {
        asm volatile("msr hcr_el2, %[value]" ::[value] "r"(hcr_el2));
    }

    static inline HCR_EL2 read()
    {
        HCR_EL2 spsr;

        asm volatile("mrs %[value], hcr_el2"
                     : [value] "=r"(spsr));

        return spsr;
    }
};
static_assert(sizeof(HCR_EL2) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-06/AArch64-Registers/SCR-EL3--Secure-Configuration-Register
// Secure Configuration Register
struct alignas(u64) SCR_EL3 {
    u64 NS : 1;
    u64 IRQ : 1;
    u64 FIQ : 1;
    u64 EA : 1;
    u64 _reserved4 : 1 = 1;
    u64 _reserved5 : 1 = 1;
    u64 _reserved6 : 1 = 0;
    u64 SMD : 1;
    u64 HCE : 1;
    u64 SIF : 1;
    u64 RW : 1;
    u64 ST : 1;
    u64 TWI : 1;
    u64 TWE : 1;
    u64 TLOR : 1;
    u64 TERR : 1;
    u64 APK : 1;
    u64 API : 1;
    u64 EEL2 : 1;
    u64 EASE : 1;
    u64 NMEA : 1;
    u64 FIEN : 1;
    u64 _reserved22 : 3 = 0;
    u64 EnSCXT : 1;
    u64 ATA : 1;
    u64 FGTEn : 1;
    u64 ECVEn : 1;
    u64 TWEDEn : 1;
    u64 TWEDEL : 4;
    u64 _reserved34 : 1 = 0;
    u64 AMVOFFEN : 1;
    u64 EnAS0 : 1;
    u64 ADEn : 1;
    u64 HXEn : 1;
    u64 _reserved39 : 14 = 0;

    static inline void write(SCR_EL3 scr_el3)
    {
        asm volatile("msr scr_el3, %[value]" ::[value] "r"(scr_el3));
    }

    static inline SCR_EL3 read()
    {
        SCR_EL3 scr;

        asm volatile("mrs %[value], scr_el3"
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
    u64 M_4 : 1 = 0;
    u64 _reserved5 : 1 = 0;
    u64 F : 1;
    u64 I : 1;
    u64 A : 1;
    u64 D : 1;
    u64 BTYPE : 2;
    u64 SSBS : 1;
    u64 _reserved13 : 7 = 0;
    u64 IL : 1;
    u64 SS : 1;
    u64 PAN : 1;
    u64 UA0 : 1;
    u64 DIT : 1;
    u64 TCO : 1;
    u64 _reserved26 : 2 = 0;
    u64 V : 1;
    u64 C : 1;
    u64 Z : 1;
    u64 N : 1;
    u64 _reserved32 : 32 = 0;

    static inline void write(SPSR_EL1 spsr_el1)
    {
        asm volatile("msr spsr_el1, %[value]" ::[value] "r"(spsr_el1));
    }

    static inline SPSR_EL1 read()
    {
        SPSR_EL1 spsr;

        asm volatile("mrs %[value], spsr_el1"
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
    u64 M_4 : 1 = 0;
    u64 _reserved5 : 1 = 0;
    u64 F : 1;
    u64 I : 1;
    u64 A : 1;
    u64 D : 1;
    u64 BTYPE : 2;
    u64 SSBS : 1;
    u64 _reserved13 : 7 = 0;
    u64 IL : 1;
    u64 SS : 1;
    u64 PAN : 1;
    u64 UA0 : 1;
    u64 DIT : 1;
    u64 TCO : 1;
    u64 _reserved26 : 2 = 0;
    u64 V : 1;
    u64 C : 1;
    u64 Z : 1;
    u64 N : 1;
    u64 _reserved32 : 32 = 0;

    static inline void write(SPSR_EL2 spsr_el2)
    {
        asm volatile("msr spsr_el2, %[value]" ::[value] "r"(spsr_el2));
    }

    static inline SPSR_EL2 read()
    {
        SPSR_EL2 spsr;

        asm volatile("mrs %[value], spsr_el2"
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
    u64 M_4 : 1 = 0;
    u64 _reserved5 : 1 = 0;
    u64 F : 1;
    u64 I : 1;
    u64 A : 1;
    u64 D : 1;
    u64 _reserved10 : 10 = 0;
    u64 IL : 1;
    u64 SS : 1;
    u64 PAN : 1;
    u64 UA0 : 1;
    u64 _reserved24 : 4 = 0;
    u64 V : 1;
    u64 C : 1;
    u64 Z : 1;
    u64 N : 1;
    u64 _reserved32 : 32 = 0;

    static inline void write(SPSR_EL3 spsr_el3)
    {
        asm volatile("msr spsr_el3, %[value]" ::[value] "r"(spsr_el3));
    }

    static inline SPSR_EL3 read()
    {
        SPSR_EL3 spsr;

        asm volatile("mrs %[value], spsr_el3"
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
        asm volatile("msr mair_el1, %[value]" ::[value] "r"(mair_el1));
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

        asm volatile("mrs %[value], esr_el1"
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

        asm volatile("mrs %[value], far_el1"
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

static inline bool exception_class_is_data_or_instruction_abort_from_lower_exception_level(u8 exception_class)
{
    return exception_class == 0x20 || exception_class == 0x24;
}

static inline bool exception_class_is_svc_instruction_execution(u8 exception_class)
{
    return exception_class == 0x11 || exception_class == 0x15;
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

        asm volatile("mrs %[value], daif"
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

// D17.2.30 CPACR_EL1, Architectural Feature Access Control Register
struct alignas(u64) CPACR_EL1 {
    u64 _reserved0 : 16 = 0;
    u64 ZEN : 2;
    u64 _reserved18 : 2 = 0;
    u64 FPEN : 2;
    u64 _reserved22 : 2 = 0;
    u64 SMEN : 2;
    u64 _reserved26 : 2 = 0;
    u64 TTA : 1;
    u64 _reserved29 : 3 = 0;
    u64 _reserved32 : 32 = 0;

    static inline void write(CPACR_EL1 cpacr_el1)
    {
        asm volatile("msr cpacr_el1, %[value]" ::[value] "r"(cpacr_el1));
    }
};
static_assert(sizeof(CPACR_EL1) == 8);

}
