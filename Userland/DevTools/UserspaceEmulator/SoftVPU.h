/*
 * Copyright (c) 2022, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FPControl.h>
#include <AK/SIMD.h>
#include <AK/Types.h>
#include <LibX86/Instruction.h>
#include <math.h>

namespace UserspaceEmulator {
using AK::RoundingMode;
using namespace AK::SIMD;
class Emulator;
class SoftCPU;

union XMM {
    f32x4 ps;
    f64x2 pd;
    i8x16 psb;
    u8x16 pub;
    i16x8 psw;
    u16x8 puw;
    u32x4 pudw;
    u64x2 puqw;
};

class SoftVPU {
public:
    SoftVPU(Emulator& emulator, SoftCPU& cpu)
        : m_emulator(emulator)
        , m_cpu(cpu)
        , m_mxcsr { 0x1F80 }
    {
    }

    XMM& operator[](u8 index) { return m_xmm[index]; }

    enum class RoundingMode : u8 {
        NEAREST = 0b00,
        DOWN = 0b01,
        UP = 0b10,
        TRUNC = 0b11
    };

    enum class ComparePredicate : u8 {
        EQ = 0,
        LT = 1,
        LE = 2,
        UNORD = 3,
        NEQ = 4,
        NLT = 5,
        NLE = 6,
        ORD = 7
        // FIXME: More with VEX prefix
    };

private:
    friend SoftCPU;
    Emulator& m_emulator;
    SoftCPU& m_cpu;

    XMM m_xmm[8];

    // FIXME: Maybe unimplemented features:
    // * DAZ
    // * FTZ
    AK::MXCSR m_mxcsr;

    void PREFETCHTNTA(X86::Instruction const&);
    void PREFETCHT0(X86::Instruction const&);
    void PREFETCHT1(X86::Instruction const&);
    void PREFETCHT2(X86::Instruction const&);
    void LDMXCSR(X86::Instruction const&);
    void STMXCSR(X86::Instruction const&);
    void MOVUPS_xmm1_xmm2m128(X86::Instruction const&);
    void MOVSS_xmm1_xmm2m32(X86::Instruction const&);
    void MOVUPS_xmm1m128_xmm2(X86::Instruction const&);
    void MOVSS_xmm1m32_xmm2(X86::Instruction const&);
    void MOVLPS_xmm1_xmm2m64(X86::Instruction const&);
    void MOVLPS_m64_xmm2(X86::Instruction const&);
    void UNPCKLPS_xmm1_xmm2m128(X86::Instruction const&);
    void UNPCKHPS_xmm1_xmm2m128(X86::Instruction const&);
    void MOVHPS_xmm1_xmm2m64(X86::Instruction const&);
    void MOVHPS_m64_xmm2(X86::Instruction const&);
    void MOVAPS_xmm1_xmm2m128(X86::Instruction const&);
    void MOVAPS_xmm1m128_xmm2(X86::Instruction const&);
    void CVTPI2PS_xmm1_mm2m64(X86::Instruction const&);
    void CVTSI2SS_xmm1_rm32(X86::Instruction const&);
    void MOVNTPS_xmm1m128_xmm2(X86::Instruction const&);
    void CVTTPS2PI_mm1_xmm2m64(X86::Instruction const&);
    void CVTTSS2SI_r32_xmm2m32(X86::Instruction const&);
    void CVTPS2PI_xmm1_mm2m64(X86::Instruction const&);
    void CVTSS2SI_r32_xmm2m32(X86::Instruction const&);
    void UCOMISS_xmm1_xmm2m32(X86::Instruction const&);
    void COMISS_xmm1_xmm2m32(X86::Instruction const&);
    void MOVMSKPS_reg_xmm(X86::Instruction const&);
    void SQRTPS_xmm1_xmm2m128(X86::Instruction const&);
    void SQRTSS_xmm1_xmm2m32(X86::Instruction const&);
    void RSQRTPS_xmm1_xmm2m128(X86::Instruction const&);
    void RSQRTSS_xmm1_xmm2m32(X86::Instruction const&);
    void RCPPS_xmm1_xmm2m128(X86::Instruction const&);
    void RCPSS_xmm1_xmm2m32(X86::Instruction const&);
    void ANDPS_xmm1_xmm2m128(X86::Instruction const&);
    void ANDNPS_xmm1_xmm2m128(X86::Instruction const&);
    void ORPS_xmm1_xmm2m128(X86::Instruction const&);
    void XORPS_xmm1_xmm2m128(X86::Instruction const&);
    void ADDPS_xmm1_xmm2m128(X86::Instruction const&);
    void ADDSS_xmm1_xmm2m32(X86::Instruction const&);
    void MULPS_xmm1_xmm2m128(X86::Instruction const&);
    void MULSS_xmm1_xmm2m32(X86::Instruction const&);
    void SUBPS_xmm1_xmm2m128(X86::Instruction const&);
    void SUBSS_xmm1_xmm2m32(X86::Instruction const&);
    void MINPS_xmm1_xmm2m128(X86::Instruction const&);
    void MINSS_xmm1_xmm2m32(X86::Instruction const&);
    void DIVPS_xmm1_xmm2m128(X86::Instruction const&);
    void DIVSS_xmm1_xmm2m32(X86::Instruction const&);
    void MAXPS_xmm1_xmm2m128(X86::Instruction const&);
    void MAXSS_xmm1_xmm2m32(X86::Instruction const&);
    void PSHUFW_mm1_mm2m64_imm8(X86::Instruction const&);
    void CMPPS_xmm1_xmm2m128_imm8(X86::Instruction const&);
    void CMPSS_xmm1_xmm2m32_imm8(X86::Instruction const&);
    void PINSRW_mm1_r32m16_imm8(X86::Instruction const&);
    void PINSRW_xmm1_r32m16_imm8(X86::Instruction const&);
    void PEXTRW_reg_mm1_imm8(X86::Instruction const&);
    void PEXTRW_reg_xmm1_imm8(X86::Instruction const&);
    void SHUFPS_xmm1_xmm2m128_imm8(X86::Instruction const&);
    void PMOVMSKB_reg_mm1(X86::Instruction const&);
    void PMOVMSKB_reg_xmm1(X86::Instruction const&);
    void PMINUB_mm1_mm2m64(X86::Instruction const&);
    void PMINUB_xmm1_xmm2m128(X86::Instruction const&);
    void PMAXUB_mm1_mm2m64(X86::Instruction const&);
    void PMAXUB_xmm1_xmm2m128(X86::Instruction const&);
    void PAVGB_mm1_mm2m64(X86::Instruction const&);
    void PAVGB_xmm1_xmm2m128(X86::Instruction const&);
    void PAVGW_mm1_mm2m64(X86::Instruction const&);
    void PAVGW_xmm1_xmm2m128(X86::Instruction const&);
    void PMULHUW_mm1_mm2m64(X86::Instruction const&);
    void PMULHUW_xmm1_xmm2m64(X86::Instruction const&);
    void MOVNTQ_m64_mm1(X86::Instruction const&);
    void PMINSB_mm1_mm2m64(X86::Instruction const&);
    void PMINSB_xmm1_xmm2m128(X86::Instruction const&);
    void PMAXSB_mm1_mm2m64(X86::Instruction const&);
    void PMAXSB_xmm1_xmm2m128(X86::Instruction const&);
    void PSADBB_mm1_mm2m64(X86::Instruction const&);
    void PSADBB_xmm1_xmm2m128(X86::Instruction const&);
    void MASKMOVQ_mm1_mm2m64(X86::Instruction const&);
};

}
