/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Report.h"
#include <AK/Concepts.h>
#include <AK/FPControl.h>
#include <AK/SIMD.h>
#include <LibDisassembly/Interpreter.h>
#include <LibDisassembly/x86/Instruction.h>

#include <math.h>
#include <string.h>

namespace UserspaceEmulator {
using namespace AK::SIMD;
using AK::RoundingMode;

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
        , m_fpu_cw { 0x037F }
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

    void fpu_dump_env()
    {
        reportln("Exceptions: #I:{} #D:{} #Z:{} #O:{} #U:{} #P:{} #SF:{} Summary:{}"sv,
            m_fpu_error_invalid,
            m_fpu_error_denorm,
            m_fpu_error_zero_div,
            m_fpu_error_overflow,
            m_fpu_error_underflow,
            m_fpu_error_precision,
            m_fpu_error_stackfault,
            m_fpu_error_summary);
        reportln("Masks: #I:{} #D:{} #Z:{} #O:{} #U:{} #P:{}"sv,
            m_fpu_cw.mask_invalid,
            m_fpu_cw.mask_denorm,
            m_fpu_cw.mask_zero_div,
            m_fpu_cw.mask_overflow,
            m_fpu_cw.mask_underflow,
            m_fpu_cw.mask_precision);
        reportln("C0:{} C1:{} C2:{} C3:{}"sv, c0(), c1(), c2(), c3());
        reportln("fpu-stacktop: {}"sv, m_fpu_stack_top);
        reportln("fpu-stack /w stacktop (real):"sv);
        for (u8 i = 0; i < 8; ++i) {
            reportln("\t{} ({}): fp {} ({}), mmx {:016x}"sv,
                i, (u8)((m_fpu_stack_top + i) % 8),
                m_storage[(m_fpu_stack_top + i) % 8].fp, fpu_is_set(i) ? "set" : "free",
                m_storage[(m_fpu_stack_top + i) % 8].mmx.raw);
        }
    }

    ByteString fpu_exception_string(FPU_Exception ex)
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
        reportln("Stack Overflow"sv);
        set_c1(1);
        fpu_set_exception(FPU_Exception::StackFault);
    }

    ALWAYS_INLINE void fpu_set_stack_underflow()
    {
        reportln("Stack Underflow"sv);
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
        return m_fpu_cw.rounding_control;
    }

    template<Arithmetic T>
    T round_checked(long double);

    template<FloatingPoint T>
    T convert_checked(long double);

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

    AK::X87ControlWord m_fpu_cw;

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
    void FLD_RM32(Disassembly::X86::Instruction const&);
    void FLD_RM64(Disassembly::X86::Instruction const&);
    void FLD_RM80(Disassembly::X86::Instruction const&);

    void FST_RM32(Disassembly::X86::Instruction const&);
    void FST_RM64(Disassembly::X86::Instruction const&);
    void FSTP_RM32(Disassembly::X86::Instruction const&);
    void FSTP_RM64(Disassembly::X86::Instruction const&);
    void FSTP_RM80(Disassembly::X86::Instruction const&);

    void FILD_RM32(Disassembly::X86::Instruction const&);
    void FILD_RM16(Disassembly::X86::Instruction const&);
    void FILD_RM64(Disassembly::X86::Instruction const&);

    void FIST_RM16(Disassembly::X86::Instruction const&);
    void FIST_RM32(Disassembly::X86::Instruction const&);
    void FISTP_RM16(Disassembly::X86::Instruction const&);
    void FISTP_RM32(Disassembly::X86::Instruction const&);
    void FISTP_RM64(Disassembly::X86::Instruction const&);
    void FISTTP_RM16(Disassembly::X86::Instruction const&);
    void FISTTP_RM32(Disassembly::X86::Instruction const&);
    void FISTTP_RM64(Disassembly::X86::Instruction const&);

    void FBLD_M80(Disassembly::X86::Instruction const&);
    void FBSTP_M80(Disassembly::X86::Instruction const&);

    void FXCH(Disassembly::X86::Instruction const&);

    void FCMOVE(Disassembly::X86::Instruction const&);
    void FCMOVNE(Disassembly::X86::Instruction const&);
    void FCMOVB(Disassembly::X86::Instruction const&);
    void FCMOVBE(Disassembly::X86::Instruction const&);
    void FCMOVNB(Disassembly::X86::Instruction const&);
    void FCMOVNBE(Disassembly::X86::Instruction const&);
    void FCMOVU(Disassembly::X86::Instruction const&);
    void FCMOVNU(Disassembly::X86::Instruction const&);

    // BASIC ARITHMETIC
    void FADD_RM32(Disassembly::X86::Instruction const&);
    void FADD_RM64(Disassembly::X86::Instruction const&);
    void FADDP(Disassembly::X86::Instruction const&);

    void FIADD_RM16(Disassembly::X86::Instruction const&);
    void FIADD_RM32(Disassembly::X86::Instruction const&);

    void FSUB_RM32(Disassembly::X86::Instruction const&);
    void FSUB_RM64(Disassembly::X86::Instruction const&);
    void FSUBP(Disassembly::X86::Instruction const&);
    void FSUBR_RM32(Disassembly::X86::Instruction const&);
    void FSUBR_RM64(Disassembly::X86::Instruction const&);
    void FSUBRP(Disassembly::X86::Instruction const&);

    void FISUB_RM16(Disassembly::X86::Instruction const&);
    void FISUB_RM32(Disassembly::X86::Instruction const&);
    void FISUBR_RM16(Disassembly::X86::Instruction const&);
    void FISUBR_RM32(Disassembly::X86::Instruction const&);

    void FMUL_RM32(Disassembly::X86::Instruction const&);
    void FMUL_RM64(Disassembly::X86::Instruction const&);
    void FMULP(Disassembly::X86::Instruction const&);

    void FIMUL_RM16(Disassembly::X86::Instruction const&);
    void FIMUL_RM32(Disassembly::X86::Instruction const&);

    void FDIV_RM32(Disassembly::X86::Instruction const&);
    void FDIV_RM64(Disassembly::X86::Instruction const&);
    void FDIVP(Disassembly::X86::Instruction const&);
    void FDIVR_RM32(Disassembly::X86::Instruction const&);
    void FDIVR_RM64(Disassembly::X86::Instruction const&);
    void FDIVRP(Disassembly::X86::Instruction const&);

    void FIDIV_RM16(Disassembly::X86::Instruction const&);
    void FIDIV_RM32(Disassembly::X86::Instruction const&);
    void FIDIVR_RM16(Disassembly::X86::Instruction const&);
    void FIDIVR_RM32(Disassembly::X86::Instruction const&);

    void FPREM(Disassembly::X86::Instruction const&);
    void FPREM1(Disassembly::X86::Instruction const&);

    void FABS(Disassembly::X86::Instruction const&);
    void FCHS(Disassembly::X86::Instruction const&);

    void FRNDINT(Disassembly::X86::Instruction const&);

    void FSCALE(Disassembly::X86::Instruction const&);

    void FSQRT(Disassembly::X86::Instruction const&);

    void FXTRACT(Disassembly::X86::Instruction const&);

    // COMPARISON
    void FCOM_RM32(Disassembly::X86::Instruction const&);
    void FCOM_RM64(Disassembly::X86::Instruction const&);
    void FCOMP_RM32(Disassembly::X86::Instruction const&);
    void FCOMP_RM64(Disassembly::X86::Instruction const&);
    void FCOMPP(Disassembly::X86::Instruction const&);
    void FCOMI(Disassembly::X86::Instruction const&);
    void FCOMIP(Disassembly::X86::Instruction const&);

    void FUCOM(Disassembly::X86::Instruction const&);
    void FUCOMP(Disassembly::X86::Instruction const&);
    void FUCOMPP(Disassembly::X86::Instruction const&);
    void FUCOMI(Disassembly::X86::Instruction const&);
    void FUCOMIP(Disassembly::X86::Instruction const&);

    void FICOM_RM16(Disassembly::X86::Instruction const&);
    void FICOM_RM32(Disassembly::X86::Instruction const&);
    void FICOMP_RM16(Disassembly::X86::Instruction const&);
    void FICOMP_RM32(Disassembly::X86::Instruction const&);

    void FTST(Disassembly::X86::Instruction const&);
    void FXAM(Disassembly::X86::Instruction const&);

    // TRANSCENDENTAL
    void FSIN(Disassembly::X86::Instruction const&);
    void FCOS(Disassembly::X86::Instruction const&);
    void FSINCOS(Disassembly::X86::Instruction const&);
    void FPTAN(Disassembly::X86::Instruction const&);
    void FPATAN(Disassembly::X86::Instruction const&);

    void F2XM1(Disassembly::X86::Instruction const&);
    void FYL2X(Disassembly::X86::Instruction const&);
    void FYL2XP1(Disassembly::X86::Instruction const&);

    // CONSTANT LOAD
    void FLD1(Disassembly::X86::Instruction const&);
    void FLDZ(Disassembly::X86::Instruction const&);
    void FLDPI(Disassembly::X86::Instruction const&);
    void FLDL2E(Disassembly::X86::Instruction const&);
    void FLDLN2(Disassembly::X86::Instruction const&);
    void FLDL2T(Disassembly::X86::Instruction const&);
    void FLDLG2(Disassembly::X86::Instruction const&);

    // CONTROL
    void FINCSTP(Disassembly::X86::Instruction const&);
    void FDECSTP(Disassembly::X86::Instruction const&);
    void FFREE(Disassembly::X86::Instruction const&);
    void FFREEP(Disassembly::X86::Instruction const&); // undocumented

    // FIXME: Non N- versions?
    void FNINIT(Disassembly::X86::Instruction const&);
    void FNCLEX(Disassembly::X86::Instruction const&);

    void FNSTCW(Disassembly::X86::Instruction const&);
    void FLDCW(Disassembly::X86::Instruction const&);

    void FNSTENV(Disassembly::X86::Instruction const&);
    void FLDENV(Disassembly::X86::Instruction const&);

    void FNSAVE(Disassembly::X86::Instruction const&);
    void FRSTOR(Disassembly::X86::Instruction const&);

    void FNSTSW(Disassembly::X86::Instruction const&);
    void FNSTSW_AX(Disassembly::X86::Instruction const&);

    // FIXME: WAIT && FWAIT
    void FNOP(Disassembly::X86::Instruction const&);

    // FPU & SIMD MANAGEMENT
    // FIXME: FXSAVE && FXRSTOR

    // DO NOTHING?
    // FIXME: FENI, FDISI, FSETPM
    void FNENI(Disassembly::X86::Instruction const&);
    void FNDISI(Disassembly::X86::Instruction const&);
    void FNSETPM(Disassembly::X86::Instruction const&);

    // MMX
    // ARITHMETIC
    void PADDB_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PADDW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PADDD_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PADDSB_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PADDSW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PADDUSB_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PADDUSW_mm1_mm2m64(Disassembly::X86::Instruction const&);

    void PSUBB_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSUBW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSUBD_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSUBSB_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSUBSW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSUBUSB_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSUBUSW_mm1_mm2m64(Disassembly::X86::Instruction const&);

    void PMULHW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PMULLW_mm1_mm2m64(Disassembly::X86::Instruction const&);

    void PMADDWD_mm1_mm2m64(Disassembly::X86::Instruction const&);

    // COMPARISON
    void PCMPEQB_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PCMPEQW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PCMPEQD_mm1_mm2m64(Disassembly::X86::Instruction const&);

    void PCMPGTB_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PCMPGTW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PCMPGTD_mm1_mm2m64(Disassembly::X86::Instruction const&);

    // CONVERSION
    void PACKSSDW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PACKSSWB_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PACKUSWB_mm1_mm2m64(Disassembly::X86::Instruction const&);

    // UNPACK
    void PUNPCKHBW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PUNPCKHWD_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PUNPCKHDQ_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PUNPCKLBW_mm1_mm2m32(Disassembly::X86::Instruction const&);
    void PUNPCKLWD_mm1_mm2m32(Disassembly::X86::Instruction const&);
    void PUNPCKLDQ_mm1_mm2m32(Disassembly::X86::Instruction const&);

    // LOGICAL
    void PAND_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PANDN_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void POR_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PXOR_mm1_mm2m64(Disassembly::X86::Instruction const&);

    // SHIFT
    void PSLLW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSLLW_mm1_imm8(Disassembly::X86::Instruction const&);
    void PSLLD_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSLLD_mm1_imm8(Disassembly::X86::Instruction const&);
    void PSLLQ_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSLLQ_mm1_imm8(Disassembly::X86::Instruction const&);
    void PSRAW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSRAW_mm1_imm8(Disassembly::X86::Instruction const&);
    void PSRAD_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSRAD_mm1_imm8(Disassembly::X86::Instruction const&);
    void PSRLW_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSRLW_mm1_imm8(Disassembly::X86::Instruction const&);
    void PSRLD_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSRLD_mm1_imm8(Disassembly::X86::Instruction const&);
    void PSRLQ_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void PSRLQ_mm1_imm8(Disassembly::X86::Instruction const&);

    // DATA TRANSFER
    void MOVD_mm1_rm32(Disassembly::X86::Instruction const&);
    void MOVD_rm32_mm2(Disassembly::X86::Instruction const&);

    void MOVQ_mm1_mm2m64(Disassembly::X86::Instruction const&);
    void MOVQ_mm1m64_mm2(Disassembly::X86::Instruction const&);
    void MOVQ_mm1_rm64(Disassembly::X86::Instruction const&); // long mode
    void MOVQ_rm64_mm2(Disassembly::X86::Instruction const&); // long mode

    // EMPTY MMX STATE
    void EMMS(Disassembly::X86::Instruction const&);
};

}
