/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel {
struct Aarch64_SCTLR_EL1 {
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
};
static_assert(sizeof(Aarch64_SCTLR_EL1) == 8);

struct Aarch64_HCR_EL2 {
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
};
static_assert(sizeof(Aarch64_HCR_EL2) == 8);

struct Aarch64_SCR_EL3 {
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
};
static_assert(sizeof(Aarch64_SCR_EL3) == 8);

struct Aarch64_SPSR_EL3 {
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
};
static_assert(sizeof(Aarch64_SPSR_EL3) == 8);
}
