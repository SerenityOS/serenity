/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Report.h"
#include <AK/Concepts.h>
#include <AK/SIMD.h>
#include <LibX86/Instruction.h>
#include <LibX86/Interpreter.h>

#include <math.h>
#include <string.h>

namespace UserspaceEmulator {
using namespace AK::SIMD;
class Emulator;
class SoftCPU;

union MMX {
    u64 raw;
    c8x8 v8;
    i16x4 v16;
    i32x2 v32;
    u16x4 v16u;
    u32x2 v32u;
};
static_assert(AssertSize<MMX, sizeof(u64)>());

class SoftFPU final {
public:
    SoftFPU(Emulator& emulator, SoftCPU& cpu)
        : m_emulator(emulator)
        , m_cpu(cpu)
    {
    }

    ALWAYS_INLINE bool c0() const { return m_fpu_c0; }
    ALWAYS_INLINE bool c1() const { return m_fpu_c1; }
    ALWAYS_INLINE bool c2() const { return m_fpu_c2; }
    ALWAYS_INLINE bool c3() const { return m_fpu_c3; }

    ALWAYS_INLINE void set_c0(bool val) { m_fpu_c0 = val; }
    ALWAYS_INLINE void set_c1(bool val) { m_fpu_c1 = val; }
    ALWAYS_INLINE void set_c2(bool val) { m_fpu_c2 = val; }
    ALWAYS_INLINE void set_c3(bool val) { m_fpu_c3 = val; }

    long double fpu_get(u8 index);

    void fpu_push(long double value);
    long double fpu_pop();
    void fpu_set_absolute(u8 index, long double value);
    void fpu_set(u8 index, long double value);

    MMX mmx_get(u8 index) const;
    void mmx_set(u8 index, MMX value);

private:
    friend class SoftCPU;

    Emulator& m_emulator;
    SoftCPU& m_cpu;

    enum class FPU_Exception : u8 {
        InvalidOperation,
        DenormalizedOperand,
        ZeroDivide,
        Overflow,
        Underflow,
        Precision,
        StackFault,
    };

    enum class FPU_Tag : u8 {
        Valid = 0b00,
        Zero = 0b01,
        Special = 0b10,
        Empty = 0b11
    };

    enum class RoundingMode : u8 {
        NEAREST = 0b00,
        DOWN = 0b01,
        UP = 0b10,
        TRUNC = 0b11
    };

    void fpu_dump_env()
    {
        reportln("Exceptions: #I:{} #D:{} #Z:{} #O:{} #U:{} #P:{} #SF:{} Summary:{}",
            m_fpu_error_invalid,
            m_fpu_error_denorm,
            m_fpu_error_zero_div,
            m_fpu_error_overflow,
            m_fpu_error_underflow,
            m_fpu_error_precision,
            m_fpu_error_stackfault,
            m_fpu_error_summary);
        reportln("Masks: #I:{} #D:{} #Z:{} #O:{} #U:{} #P:{}",
            m_fpu_mask_invalid,
            m_fpu_mask_denorm,
            m_fpu_mask_zero_div,
            m_fpu_mask_overflow,
            m_fpu_mask_underflow,
            m_fpu_mask_precision);
        reportln("C0:{} C1:{} C2:{} C3:{}", c0(), c1(), c2(), c3());
        reportln("fpu-stacktop: {}", m_fpu_stack_top);
        reportln("fpu-stack /w stacktop (real):");
        for (u8 i = 0; i < 8; ++i) {
            reportln("\t{} ({}): fp {} ({}), mmx {:016x}",
                i, (u8)((m_fpu_stack_top + i) % 8),
                m_storage[(m_fpu_stack_top + i) % 8].fp, fpu_is_set(i) ? "set" : "free",
                m_storage[(m_fpu_stack_top + i) % 8].mmx.raw);
        }
    }

    String fpu_exception_string(FPU_Exception ex)
    {
        switch (ex) {
        case FPU_Exception::StackFault:
            return "Stackfault";
        case FPU_Exception::InvalidOperation:
            return "Invalid Operation";
        case FPU_Exception::DenormalizedOperand:
            return "Denormalized Operand";
        case FPU_Exception::ZeroDivide:
            return "Divide by Zero";
        case FPU_Exception::Overflow:
            return "Overflow";
        case FPU_Exception::Underflow:
            return "Underflow";
        case FPU_Exception::Precision:
            return "Precision";
        }
        VERIFY_NOT_REACHED();
    }

    // FIXME: Technically we should check for exceptions after each insn, too,
    //        this might be important for FLDENV, but otherwise it should
    //        be fine this way
    void fpu_set_exception(FPU_Exception ex);

    ALWAYS_INLINE void fpu_set_stack_overflow()
    {
        reportln("Stack Overflow");
        set_c1(1);
        fpu_set_exception(FPU_Exception::StackFault);
    }

    ALWAYS_INLINE void fpu_set_stack_underflow()
    {
        reportln("Stack Underflow");
        set_c1(0);
        fpu_set_exception(FPU_Exception::StackFault);
    }

    constexpr FPU_Tag fpu_get_tag_absolute(u8 index) const
    {
        switch (index) {
        case 0:
            return FPU_Tag(m_fpu_status_0);
        case 1:
            return FPU_Tag(m_fpu_status_1);
        case 2:
            return FPU_Tag(m_fpu_status_2);
        case 3:
            return FPU_Tag(m_fpu_status_3);
        case 4:
            return FPU_Tag(m_fpu_status_4);
        case 5:
            return FPU_Tag(m_fpu_status_5);
        case 6:
            return FPU_Tag(m_fpu_status_6);
        case 7:
            return FPU_Tag(m_fpu_status_7);
        default:
            VERIFY_NOT_REACHED();
        }
    }

    constexpr FPU_Tag fpu_get_tag(u8 index) const
    {
        VERIFY(index < 8);
        return fpu_get_tag_absolute((m_fpu_stack_top + index) % 8);
    }

    ALWAYS_INLINE void fpu_set_tag_absolute(u8 index, FPU_Tag tag)
    {
        switch (index) {
        case 0:
            m_fpu_status_0 = (u8)tag;
            break;
        case 1:
            m_fpu_status_1 = (u8)tag;
            break;
        case 2:
            m_fpu_status_2 = (u8)tag;
            break;
        case 3:
            m_fpu_status_3 = (u8)tag;
            break;
        case 4:
            m_fpu_status_4 = (u8)tag;
            break;
        case 5:
            m_fpu_status_5 = (u8)tag;
            break;
        case 6:
            m_fpu_status_6 = (u8)tag;
            break;
        case 7:
            m_fpu_status_7 = (u8)tag;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ALWAYS_INLINE void fpu_set_tag(u8 index, FPU_Tag tag)
    {
        VERIFY(index < 8);
        fpu_set_tag_absolute((m_fpu_stack_top + index) % 8, tag);
    }

    ALWAYS_INLINE void set_tag_from_value_absolute(u8 index, long double val)
    {
        switch (fpclassify(val)) {
        case FP_ZERO:
            fpu_set_tag_absolute(index, FPU_Tag::Zero);
            break;
        case FP_NAN:
        case FP_INFINITE:
        case FP_SUBNORMAL:
            fpu_set_tag_absolute(index, FPU_Tag::Special);
            break;
        case FP_NORMAL:
            fpu_set_tag_absolute(index, FPU_Tag::Valid);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ALWAYS_INLINE void set_tag_from_value(u8 index, long double val)
    {
        set_tag_from_value_absolute((m_fpu_stack_top + index) % 8, val);
    }

    ALWAYS_INLINE bool fpu_isnan(u8 index)
    {
        return isnan(fpu_get(index));
    }

    ALWAYS_INLINE bool fpu_is_set(u8 index) const
    {
        return fpu_get_tag_absolute((m_fpu_stack_top + index) % 8) != FPU_Tag::Empty;
    }

    ALWAYS_INLINE RoundingMode fpu_get_round_mode() const
    {
        return RoundingMode(m_fpu_round_mode);
    }

    template<Arithmetic T>
    T fpu_round(long double) const;
    template<Arithmetic T>
    T fpu_round_checked(long double);

    template<FloatingPoint T>
    T fpu_convert(long double) const;
    template<FloatingPoint T>
    T fpu_convert_checked(long double);

    ALWAYS_INLINE void fpu_set_unordered()
    {
        set_c0(1);
        set_c2(1);
        set_c3(1);
    }
    void warn_if_mmx_absolute(u8 index) const;
    void warn_if_fpu_absolute(u8 index) const;

    void mmx_common() { m_fpu_tw = 0; }

    bool m_reg_is_mmx[8] { false };

    union {
        long double fp;
        struct {
            MMX mmx;
            u16 __high;
        };
    } m_storage[8];

    union {
        u16 m_fpu_cw { 0x037F };
        struct {
            u16 m_fpu_mask_invalid : 1;
            u16 m_fpu_mask_denorm : 1;
            u16 m_fpu_mask_zero_div : 1;
            u16 m_fpu_mask_overflow : 1;
            u16 m_fpu_mask_underflow : 1;
            u16 m_fpu_mask_precision : 1;
            u16 : 2; // unused
            u16 m_fpu_precission : 2;
            u16 m_fpu_round_mode : 2;
            u16 m_fpu_infinity_control : 1;
            u16 : 3; // unused
        };
    };

    union {
        u16 m_fpu_sw { 0 };
        struct {
            u16 m_fpu_error_invalid : 1;    // pre | IE -> #I (#IS, #IA)
            u16 m_fpu_error_denorm : 1;     // pre | DE -> #D
            u16 m_fpu_error_zero_div : 1;   // pre | ZE -> #Z
            u16 m_fpu_error_overflow : 1;   // post| OE -> #O
            u16 m_fpu_error_underflow : 1;  // post| UE -> #U
            u16 m_fpu_error_precision : 1;  // post| PE -> #P
            u16 m_fpu_error_stackfault : 1; // SF
            u16 m_fpu_error_summary : 1;
            u16 m_fpu_c0 : 1;
            u16 m_fpu_c1 : 1;
            u16 m_fpu_c2 : 1;
            u16 m_fpu_stack_top : 3;
            u16 m_fpu_c3 : 1;
            u16 m_fpu_busy : 1;
        };
    };

    union {
        u16 m_fpu_tw { 0xFFFF };
        struct {
            u16 m_fpu_status_0 : 2;
            u16 m_fpu_status_1 : 2;
            u16 m_fpu_status_2 : 2;
            u16 m_fpu_status_3 : 2;
            u16 m_fpu_status_4 : 2;
            u16 m_fpu_status_5 : 2;
            u16 m_fpu_status_6 : 2;
            u16 m_fpu_status_7 : 2;
        };
    };

    u32 m_fpu_ip { 0 };
    u16 m_fpu_cs { 0 };

    u32 m_fpu_dp { 0 };
    u16 m_fpu_ds { 0 };

    u16 m_fpu_iop { 0 };

    // Instructions

    // DATA TRANSFER
    void FLD_RM32(const X86::Instruction&);
    void FLD_RM64(const X86::Instruction&);
    void FLD_RM80(const X86::Instruction&);

    void FST_RM32(const X86::Instruction&);
    void FST_RM64(const X86::Instruction&);
    void FSTP_RM32(const X86::Instruction&);
    void FSTP_RM64(const X86::Instruction&);
    void FSTP_RM80(const X86::Instruction&);

    void FILD_RM32(const X86::Instruction&);
    void FILD_RM16(const X86::Instruction&);
    void FILD_RM64(const X86::Instruction&);

    void FIST_RM16(const X86::Instruction&);
    void FIST_RM32(const X86::Instruction&);
    void FISTP_RM16(const X86::Instruction&);
    void FISTP_RM32(const X86::Instruction&);
    void FISTP_RM64(const X86::Instruction&);
    void FISTTP_RM16(const X86::Instruction&);
    void FISTTP_RM32(const X86::Instruction&);
    void FISTTP_RM64(const X86::Instruction&);

    void FBLD_M80(const X86::Instruction&);
    void FBSTP_M80(const X86::Instruction&);

    void FXCH(const X86::Instruction&);

    void FCMOVE(const X86::Instruction&);
    void FCMOVNE(const X86::Instruction&);
    void FCMOVB(const X86::Instruction&);
    void FCMOVBE(const X86::Instruction&);
    void FCMOVNB(const X86::Instruction&);
    void FCMOVNBE(const X86::Instruction&);
    void FCMOVU(const X86::Instruction&);
    void FCMOVNU(const X86::Instruction&);

    // BASIC ARITHMETIC
    void FADD_RM32(const X86::Instruction&);
    void FADD_RM64(const X86::Instruction&);
    void FADDP(const X86::Instruction&);

    void FIADD_RM16(const X86::Instruction&);
    void FIADD_RM32(const X86::Instruction&);

    void FSUB_RM32(const X86::Instruction&);
    void FSUB_RM64(const X86::Instruction&);
    void FSUBP(const X86::Instruction&);
    void FSUBR_RM32(const X86::Instruction&);
    void FSUBR_RM64(const X86::Instruction&);
    void FSUBRP(const X86::Instruction&);

    void FISUB_RM16(const X86::Instruction&);
    void FISUB_RM32(const X86::Instruction&);
    void FISUBR_RM16(const X86::Instruction&);
    void FISUBR_RM32(const X86::Instruction&);

    void FMUL_RM32(const X86::Instruction&);
    void FMUL_RM64(const X86::Instruction&);
    void FMULP(const X86::Instruction&);

    void FIMUL_RM16(const X86::Instruction&);
    void FIMUL_RM32(const X86::Instruction&);

    void FDIV_RM32(const X86::Instruction&);
    void FDIV_RM64(const X86::Instruction&);
    void FDIVP(const X86::Instruction&);
    void FDIVR_RM32(const X86::Instruction&);
    void FDIVR_RM64(const X86::Instruction&);
    void FDIVRP(const X86::Instruction&);

    void FIDIV_RM16(const X86::Instruction&);
    void FIDIV_RM32(const X86::Instruction&);
    void FIDIVR_RM16(const X86::Instruction&);
    void FIDIVR_RM32(const X86::Instruction&);

    void FPREM(const X86::Instruction&);
    void FPREM1(const X86::Instruction&);

    void FABS(const X86::Instruction&);
    void FCHS(const X86::Instruction&);

    void FRNDINT(const X86::Instruction&);

    void FSCALE(const X86::Instruction&);

    void FSQRT(const X86::Instruction&);

    void FXTRACT(const X86::Instruction&);

    // COMPARISON
    void FCOM_RM32(const X86::Instruction&);
    void FCOM_RM64(const X86::Instruction&);
    void FCOMP_RM32(const X86::Instruction&);
    void FCOMP_RM64(const X86::Instruction&);
    void FCOMPP(const X86::Instruction&);
    void FCOMI(const X86::Instruction&);
    void FCOMIP(const X86::Instruction&);

    void FUCOM(const X86::Instruction&);
    void FUCOMP(const X86::Instruction&);
    void FUCOMPP(const X86::Instruction&);
    void FUCOMI(const X86::Instruction&);
    void FUCOMIP(const X86::Instruction&);

    void FICOM_RM16(const X86::Instruction&);
    void FICOM_RM32(const X86::Instruction&);
    void FICOMP_RM16(const X86::Instruction&);
    void FICOMP_RM32(const X86::Instruction&);

    void FTST(const X86::Instruction&);
    void FXAM(const X86::Instruction&);

    // TRANSCENDENTAL
    void FSIN(const X86::Instruction&);
    void FCOS(const X86::Instruction&);
    void FSINCOS(const X86::Instruction&);
    void FPTAN(const X86::Instruction&);
    void FPATAN(const X86::Instruction&);

    void F2XM1(const X86::Instruction&);
    void FYL2X(const X86::Instruction&);
    void FYL2XP1(const X86::Instruction&);

    // CONSTANT LOAD
    void FLD1(const X86::Instruction&);
    void FLDZ(const X86::Instruction&);
    void FLDPI(const X86::Instruction&);
    void FLDL2E(const X86::Instruction&);
    void FLDLN2(const X86::Instruction&);
    void FLDL2T(const X86::Instruction&);
    void FLDLG2(const X86::Instruction&);

    // CONTROL
    void FINCSTP(const X86::Instruction&);
    void FDECSTP(const X86::Instruction&);
    void FFREE(const X86::Instruction&);
    void FFREEP(const X86::Instruction&); // undocumented

    // FIXME: Non N- versions?
    void FNINIT(const X86::Instruction&);
    void FNCLEX(const X86::Instruction&);

    void FNSTCW(const X86::Instruction&);
    void FLDCW(const X86::Instruction&);

    void FNSTENV(const X86::Instruction&);
    void FLDENV(const X86::Instruction&);

    void FNSAVE(const X86::Instruction&);
    void FRSTOR(const X86::Instruction&);

    void FNSTSW(const X86::Instruction&);
    void FNSTSW_AX(const X86::Instruction&);

    // FIXME: WAIT && FWAIT
    void FNOP(const X86::Instruction&);

    // FPU & SIMD MANAGEMENT
    // FIXME: FXSAVE && FXRSTOR

    // DO NOTHING?
    // FIXME: FENI, FDISI, FSETPM
    void FNENI(const X86::Instruction&);
    void FNDISI(const X86::Instruction&);
    void FNSETPM(const X86::Instruction&);

    // MMX
    // ARITHMETIC
    void PADDB_mm1_mm2m64(const X86::Instruction&);
    void PADDW_mm1_mm2m64(const X86::Instruction&);
    void PADDD_mm1_mm2m64(const X86::Instruction&);
    void PADDSB_mm1_mm2m64(const X86::Instruction&);
    void PADDSW_mm1_mm2m64(const X86::Instruction&);
    void PADDUSB_mm1_mm2m64(const X86::Instruction&);
    void PADDUSW_mm1_mm2m64(const X86::Instruction&);

    void PSUBB_mm1_mm2m64(const X86::Instruction&);
    void PSUBW_mm1_mm2m64(const X86::Instruction&);
    void PSUBD_mm1_mm2m64(const X86::Instruction&);
    void PSUBSB_mm1_mm2m64(const X86::Instruction&);
    void PSUBSW_mm1_mm2m64(const X86::Instruction&);
    void PSUBUSB_mm1_mm2m64(const X86::Instruction&);
    void PSUBUSW_mm1_mm2m64(const X86::Instruction&);

    void PMULHW_mm1_mm2m64(const X86::Instruction&);
    void PMULLW_mm1_mm2m64(const X86::Instruction&);

    void PMADDWD_mm1_mm2m64(const X86::Instruction&);

    // COMPARISON
    void PCMPEQB_mm1_mm2m64(const X86::Instruction&);
    void PCMPEQW_mm1_mm2m64(const X86::Instruction&);
    void PCMPEQD_mm1_mm2m64(const X86::Instruction&);

    void PCMPGTB_mm1_mm2m64(const X86::Instruction&);
    void PCMPGTW_mm1_mm2m64(const X86::Instruction&);
    void PCMPGTD_mm1_mm2m64(const X86::Instruction&);

    // CONVERSION
    void PACKSSDW_mm1_mm2m64(const X86::Instruction&);
    void PACKSSWB_mm1_mm2m64(const X86::Instruction&);
    void PACKUSWB_mm1_mm2m64(const X86::Instruction&);

    // UNPACK
    void PUNPCKHBW_mm1_mm2m64(const X86::Instruction&);
    void PUNPCKHWD_mm1_mm2m64(const X86::Instruction&);
    void PUNPCKHDQ_mm1_mm2m64(const X86::Instruction&);
    void PUNPCKLBW_mm1_mm2m32(const X86::Instruction&);
    void PUNPCKLWD_mm1_mm2m32(const X86::Instruction&);
    void PUNPCKLDQ_mm1_mm2m32(const X86::Instruction&);

    // LOGICAL
    void PAND_mm1_mm2m64(const X86::Instruction&);
    void PANDN_mm1_mm2m64(const X86::Instruction&);
    void POR_mm1_mm2m64(const X86::Instruction&);
    void PXOR_mm1_mm2m64(const X86::Instruction&);

    // SHIFT
    void PSLLW_mm1_mm2m64(const X86::Instruction&);
    void PSLLW_mm1_imm8(const X86::Instruction&);
    void PSLLD_mm1_mm2m64(const X86::Instruction&);
    void PSLLD_mm1_imm8(const X86::Instruction&);
    void PSLLQ_mm1_mm2m64(const X86::Instruction&);
    void PSLLQ_mm1_imm8(const X86::Instruction&);
    void PSRAW_mm1_mm2m64(const X86::Instruction&);
    void PSRAW_mm1_imm8(const X86::Instruction&);
    void PSRAD_mm1_mm2m64(const X86::Instruction&);
    void PSRAD_mm1_imm8(const X86::Instruction&);
    void PSRLW_mm1_mm2m64(const X86::Instruction&);
    void PSRLW_mm1_imm8(const X86::Instruction&);
    void PSRLD_mm1_mm2m64(const X86::Instruction&);
    void PSRLD_mm1_imm8(const X86::Instruction&);
    void PSRLQ_mm1_mm2m64(const X86::Instruction&);
    void PSRLQ_mm1_imm8(const X86::Instruction&);

    // DATA TRANSFER
    void MOVD_mm1_rm32(const X86::Instruction&);
    void MOVD_rm32_mm2(const X86::Instruction&);

    void MOVQ_mm1_mm2m64(const X86::Instruction&);
    void MOVQ_mm1m64_mm2(const X86::Instruction&);
    void MOVQ_mm1_rm64(const X86::Instruction&); // long mode
    void MOVQ_rm64_mm2(const X86::Instruction&); // long mode

    // EMPTY MMX STATE
    void EMMS(const X86::Instruction&);
};

}
