/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::Aarch64 {

// https://developer.arm.com/documentation/ddi0595/2021-06/AArch64-Registers/ID-AA64MMFR0-EL1--AArch64-Memory-Model-Feature-Register-0
// Memory Model Feature Register 0
struct ID_AA64MMFR0_EL1 {
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
    int RES0 : 8;
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

// https://developer.arm.com/documentation/ddi0595/2021-06/AArch64-Registers/TCR-EL1--Translation-Control-Register--EL1-
// Translation Control Register
struct TCR_EL1 {

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

    static inline constexpr TCR_EL1 reset_value()
    {
        return {};
    }
};
static_assert(sizeof(TCR_EL1) == 8);

// https://developer.arm.com/documentation/ddi0595/2021-03/AArch64-Registers/SCTLR-EL1--System-Control-Register--EL1-
// System Control Register
struct SCTLR_EL1 {
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

    static inline constexpr SCTLR_EL1 reset_value()
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

// https://developer.arm.com/documentation/ddi0595/2021-06/AArch64-Registers/HCR-EL2--Hypervisor-Configuration-Register
// Hypervisor Configuration Register
struct HCR_EL2 {
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
struct SCR_EL3 {
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

struct SPSR_EL2 {
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
struct SPSR_EL3 {
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
struct MAIR_EL1 {
    using AttributeEncoding = uint8_t;
    AttributeEncoding Attr[8];

    static inline void write(MAIR_EL1 mair_el1)
    {
        asm("msr mair_el1, %[value]" ::[value] "r"(mair_el1));
    }
};
static_assert(sizeof(MAIR_EL1) == 8);

}
