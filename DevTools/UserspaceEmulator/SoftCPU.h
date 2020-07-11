/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibX86/Instruction.h>
#include <LibX86/Interpreter.h>

namespace UserspaceEmulator {

class Emulator;

union PartAddressableRegister {
    struct {
        u32 full_u32;
    };
    struct {
        u16 low_u16;
        u16 high_u16;
    };
    struct {
        u8 low_u8;
        u8 high_u8;
        u16 also_high_u16;
    };
};

class SoftCPU final
    : public X86::Interpreter
    , public X86::InstructionStream {
public:
    explicit SoftCPU(Emulator&);
    void dump() const;

    u32 eip() const { return m_eip; }
    void set_eip(u32 eip) { m_eip = eip; }

    struct Flags {
        enum Flag {
            CF = 0x0001,
            PF = 0x0004,
            AF = 0x0010,
            ZF = 0x0040,
            SF = 0x0080,
            TF = 0x0100,
            IF = 0x0200,
            DF = 0x0400,
            OF = 0x0800,
        };
    };

    void push32(u32);
    u32 pop32();

    u16 segment(X86::SegmentRegister seg) const { return m_segment[(int)seg]; }
    u16& segment(X86::SegmentRegister seg) { return m_segment[(int)seg]; }

    u8& gpr8(X86::RegisterIndex8 reg)
    {
        switch (reg) {
        case X86::RegisterAL:
            return m_gpr[X86::RegisterEAX].low_u8;
        case X86::RegisterAH:
            return m_gpr[X86::RegisterEAX].high_u8;
        case X86::RegisterBL:
            return m_gpr[X86::RegisterEBX].low_u8;
        case X86::RegisterBH:
            return m_gpr[X86::RegisterEBX].high_u8;
        case X86::RegisterCL:
            return m_gpr[X86::RegisterECX].low_u8;
        case X86::RegisterCH:
            return m_gpr[X86::RegisterECX].high_u8;
        case X86::RegisterDL:
            return m_gpr[X86::RegisterEDX].low_u8;
        case X86::RegisterDH:
            return m_gpr[X86::RegisterEDX].high_u8;
        }
        ASSERT_NOT_REACHED();
    }

    u8 gpr8(X86::RegisterIndex8 reg) const
    {
        switch (reg) {
        case X86::RegisterAL:
            return m_gpr[X86::RegisterEAX].low_u8;
        case X86::RegisterAH:
            return m_gpr[X86::RegisterEAX].high_u8;
        case X86::RegisterBL:
            return m_gpr[X86::RegisterEBX].low_u8;
        case X86::RegisterBH:
            return m_gpr[X86::RegisterEBX].high_u8;
        case X86::RegisterCL:
            return m_gpr[X86::RegisterECX].low_u8;
        case X86::RegisterCH:
            return m_gpr[X86::RegisterECX].high_u8;
        case X86::RegisterDL:
            return m_gpr[X86::RegisterEDX].low_u8;
        case X86::RegisterDH:
            return m_gpr[X86::RegisterEDX].high_u8;
        }
        ASSERT_NOT_REACHED();
    }

    u16 gpr16(X86::RegisterIndex16 reg) const { return m_gpr[reg].low_u16; }
    u16& gpr16(X86::RegisterIndex16 reg) { return m_gpr[reg].low_u16; }

    u32 gpr32(X86::RegisterIndex32 reg) const { return m_gpr[reg].full_u32; }
    u32& gpr32(X86::RegisterIndex32 reg) { return m_gpr[reg].full_u32; }

    u32 eax() const { return gpr32(X86::RegisterEAX); }
    u32 ebx() const { return gpr32(X86::RegisterEBX); }
    u32 ecx() const { return gpr32(X86::RegisterECX); }
    u32 edx() const { return gpr32(X86::RegisterEDX); }
    u32 esp() const { return gpr32(X86::RegisterESP); }
    u32 ebp() const { return gpr32(X86::RegisterEBP); }
    u32 esi() const { return gpr32(X86::RegisterESI); }
    u32 edi() const { return gpr32(X86::RegisterEDI); }

    u16 ax() const { return gpr16(X86::RegisterAX); }
    u16 bx() const { return gpr16(X86::RegisterBX); }
    u16 cx() const { return gpr16(X86::RegisterCX); }
    u16 dx() const { return gpr16(X86::RegisterDX); }
    u16 sp() const { return gpr16(X86::RegisterSP); }
    u16 bp() const { return gpr16(X86::RegisterBP); }
    u16 si() const { return gpr16(X86::RegisterSI); }
    u16 di() const { return gpr16(X86::RegisterDI); }

    u8 al() const { return gpr8(X86::RegisterAL); }
    u8 ah() const { return gpr8(X86::RegisterAH); }
    u8 bl() const { return gpr8(X86::RegisterBL); }
    u8 bh() const { return gpr8(X86::RegisterBH); }
    u8 cl() const { return gpr8(X86::RegisterCL); }
    u8 ch() const { return gpr8(X86::RegisterCH); }
    u8 dl() const { return gpr8(X86::RegisterDL); }
    u8 dh() const { return gpr8(X86::RegisterDH); }

    void set_eax(u32 value) { gpr32(X86::RegisterEAX) = value; }
    void set_ebx(u32 value) { gpr32(X86::RegisterEBX) = value; }
    void set_ecx(u32 value) { gpr32(X86::RegisterECX) = value; }
    void set_edx(u32 value) { gpr32(X86::RegisterEDX) = value; }
    void set_esp(u32 value) { gpr32(X86::RegisterESP) = value; }
    void set_ebp(u32 value) { gpr32(X86::RegisterEBP) = value; }
    void set_esi(u32 value) { gpr32(X86::RegisterESI) = value; }
    void set_edi(u32 value) { gpr32(X86::RegisterEDI) = value; }

    void set_ax(u16 value) { gpr16(X86::RegisterAX) = value; }
    void set_bx(u16 value) { gpr16(X86::RegisterBX) = value; }
    void set_cx(u16 value) { gpr16(X86::RegisterCX) = value; }
    void set_dx(u16 value) { gpr16(X86::RegisterDX) = value; }
    void set_sp(u16 value) { gpr16(X86::RegisterSP) = value; }
    void set_bp(u16 value) { gpr16(X86::RegisterBP) = value; }
    void set_si(u16 value) { gpr16(X86::RegisterSI) = value; }
    void set_di(u16 value) { gpr16(X86::RegisterDI) = value; }

    void set_al(u8 value) { gpr8(X86::RegisterAL) = value; }
    void set_ah(u8 value) { gpr8(X86::RegisterAH) = value; }
    void set_bl(u8 value) { gpr8(X86::RegisterBL) = value; }
    void set_bh(u8 value) { gpr8(X86::RegisterBH) = value; }
    void set_cl(u8 value) { gpr8(X86::RegisterCL) = value; }
    void set_ch(u8 value) { gpr8(X86::RegisterCH) = value; }
    void set_dl(u8 value) { gpr8(X86::RegisterDL) = value; }
    void set_dh(u8 value) { gpr8(X86::RegisterDH) = value; }

    bool of() const { return m_eflags & Flags::OF; }
    bool sf() const { return m_eflags & Flags::SF; }
    bool zf() const { return m_eflags & Flags::ZF; }
    bool af() const { return m_eflags & Flags::AF; }
    bool pf() const { return m_eflags & Flags::PF; }
    bool cf() const { return m_eflags & Flags::CF; }

    void set_flag(Flags::Flag flag, bool value)
    {
        if (value)
            m_eflags |= flag;
        else
            m_eflags &= ~flag;
    }

    void set_of(bool value) { set_flag(Flags::OF, value); }
    void set_sf(bool value) { set_flag(Flags::SF, value); }
    void set_zf(bool value) { set_flag(Flags::ZF, value); }
    void set_af(bool value) { set_flag(Flags::AF, value); }
    void set_pf(bool value) { set_flag(Flags::PF, value); }
    void set_cf(bool value) { set_flag(Flags::CF, value); }

    void set_flags_oszapc(u32 new_flags)
    {
        m_eflags &= ~(Flags::OF | Flags::SF | Flags::ZF | Flags::AF | Flags::PF | Flags::CF);
        m_eflags |= new_flags & (Flags::OF | Flags::SF | Flags::ZF | Flags::AF | Flags::PF | Flags::CF);
    }

    void set_flags_oszap(u32 new_flags)
    {
        m_eflags &= ~(Flags::OF | Flags::SF | Flags::ZF | Flags::AF | Flags::PF);
        m_eflags |= new_flags & (Flags::OF | Flags::SF | Flags::ZF | Flags::AF | Flags::PF);
    }

    void set_flags_oszpc(u32 new_flags)
    {
        m_eflags &= ~(Flags::OF | Flags::SF | Flags::ZF | Flags::PF | Flags::CF);
        m_eflags |= new_flags & (Flags::OF | Flags::SF | Flags::ZF | Flags::PF | Flags::CF);
    }

    u16 cs() const { return m_segment[(int)X86::SegmentRegister::CS]; }
    u16 ds() const { return m_segment[(int)X86::SegmentRegister::DS]; }
    u16 es() const { return m_segment[(int)X86::SegmentRegister::ES]; }
    u16 ss() const { return m_segment[(int)X86::SegmentRegister::SS]; }

    u8 read_memory8(X86::LogicalAddress);
    u16 read_memory16(X86::LogicalAddress);
    u32 read_memory32(X86::LogicalAddress);

    void write_memory8(X86::LogicalAddress, u8);
    void write_memory16(X86::LogicalAddress, u16);
    void write_memory32(X86::LogicalAddress, u32);

    bool evaluate_condition(u8 condition) const
    {
        switch (condition) {
        case 0:
            return of(); // O
        case 1:
            return !of(); // NO
        case 2:
            return cf(); // B, C, NAE
        case 3:
            return !cf(); // NB, NC, AE
        case 4:
            return zf(); // E, Z
        case 5:
            return !zf(); // NE, NZ
        case 6:
            return (cf() | zf()); // BE, NA
        case 7:
            return !(cf() | zf()); // NBE, A
        case 8:
            return sf(); // S
        case 9:
            return !sf(); // NS
        case 10:
            return pf(); // P, PE
        case 11:
            return !pf(); // NP, PO
        case 12:
            return sf() ^ of(); // L, NGE
        case 13:
            return !(sf() ^ of()); // NL, GE
        case 14:
            return (sf() ^ of()) | zf(); // LE, NG
        case 15:
            return !((sf() ^ of()) | zf()); // NLE, G
        default:
            ASSERT_NOT_REACHED();
        }
        return 0;
    }

private:
    // ^X86::InstructionStream
    virtual bool can_read() override { return false; }
    virtual u8 read8() override;
    virtual u16 read16() override;
    virtual u32 read32() override;

    // ^X86::Interpreter
    virtual void AAA(const X86::Instruction&) override;
    virtual void AAD(const X86::Instruction&) override;
    virtual void AAM(const X86::Instruction&) override;
    virtual void AAS(const X86::Instruction&) override;
    virtual void ADC_AL_imm8(const X86::Instruction&) override;
    virtual void ADC_AX_imm16(const X86::Instruction&) override;
    virtual void ADC_EAX_imm32(const X86::Instruction&) override;
    virtual void ADC_RM16_imm16(const X86::Instruction&) override;
    virtual void ADC_RM16_imm8(const X86::Instruction&) override;
    virtual void ADC_RM16_reg16(const X86::Instruction&) override;
    virtual void ADC_RM32_imm32(const X86::Instruction&) override;
    virtual void ADC_RM32_imm8(const X86::Instruction&) override;
    virtual void ADC_RM32_reg32(const X86::Instruction&) override;
    virtual void ADC_RM8_imm8(const X86::Instruction&) override;
    virtual void ADC_RM8_reg8(const X86::Instruction&) override;
    virtual void ADC_reg16_RM16(const X86::Instruction&) override;
    virtual void ADC_reg32_RM32(const X86::Instruction&) override;
    virtual void ADC_reg8_RM8(const X86::Instruction&) override;
    virtual void ADD_AL_imm8(const X86::Instruction&) override;
    virtual void ADD_AX_imm16(const X86::Instruction&) override;
    virtual void ADD_EAX_imm32(const X86::Instruction&) override;
    virtual void ADD_RM16_imm16(const X86::Instruction&) override;
    virtual void ADD_RM16_imm8(const X86::Instruction&) override;
    virtual void ADD_RM16_reg16(const X86::Instruction&) override;
    virtual void ADD_RM32_imm32(const X86::Instruction&) override;
    virtual void ADD_RM32_imm8(const X86::Instruction&) override;
    virtual void ADD_RM32_reg32(const X86::Instruction&) override;
    virtual void ADD_RM8_imm8(const X86::Instruction&) override;
    virtual void ADD_RM8_reg8(const X86::Instruction&) override;
    virtual void ADD_reg16_RM16(const X86::Instruction&) override;
    virtual void ADD_reg32_RM32(const X86::Instruction&) override;
    virtual void ADD_reg8_RM8(const X86::Instruction&) override;
    virtual void AND_AL_imm8(const X86::Instruction&) override;
    virtual void AND_AX_imm16(const X86::Instruction&) override;
    virtual void AND_EAX_imm32(const X86::Instruction&) override;
    virtual void AND_RM16_imm16(const X86::Instruction&) override;
    virtual void AND_RM16_imm8(const X86::Instruction&) override;
    virtual void AND_RM16_reg16(const X86::Instruction&) override;
    virtual void AND_RM32_imm32(const X86::Instruction&) override;
    virtual void AND_RM32_imm8(const X86::Instruction&) override;
    virtual void AND_RM32_reg32(const X86::Instruction&) override;
    virtual void AND_RM8_imm8(const X86::Instruction&) override;
    virtual void AND_RM8_reg8(const X86::Instruction&) override;
    virtual void AND_reg16_RM16(const X86::Instruction&) override;
    virtual void AND_reg32_RM32(const X86::Instruction&) override;
    virtual void AND_reg8_RM8(const X86::Instruction&) override;
    virtual void ARPL(const X86::Instruction&) override;
    virtual void BOUND(const X86::Instruction&) override;
    virtual void BSF_reg16_RM16(const X86::Instruction&) override;
    virtual void BSF_reg32_RM32(const X86::Instruction&) override;
    virtual void BSR_reg16_RM16(const X86::Instruction&) override;
    virtual void BSR_reg32_RM32(const X86::Instruction&) override;
    virtual void BSWAP_reg32(const X86::Instruction&) override;
    virtual void BTC_RM16_imm8(const X86::Instruction&) override;
    virtual void BTC_RM16_reg16(const X86::Instruction&) override;
    virtual void BTC_RM32_imm8(const X86::Instruction&) override;
    virtual void BTC_RM32_reg32(const X86::Instruction&) override;
    virtual void BTR_RM16_imm8(const X86::Instruction&) override;
    virtual void BTR_RM16_reg16(const X86::Instruction&) override;
    virtual void BTR_RM32_imm8(const X86::Instruction&) override;
    virtual void BTR_RM32_reg32(const X86::Instruction&) override;
    virtual void BTS_RM16_imm8(const X86::Instruction&) override;
    virtual void BTS_RM16_reg16(const X86::Instruction&) override;
    virtual void BTS_RM32_imm8(const X86::Instruction&) override;
    virtual void BTS_RM32_reg32(const X86::Instruction&) override;
    virtual void BT_RM16_imm8(const X86::Instruction&) override;
    virtual void BT_RM16_reg16(const X86::Instruction&) override;
    virtual void BT_RM32_imm8(const X86::Instruction&) override;
    virtual void BT_RM32_reg32(const X86::Instruction&) override;
    virtual void CALL_FAR_mem16(const X86::Instruction&) override;
    virtual void CALL_FAR_mem32(const X86::Instruction&) override;
    virtual void CALL_RM16(const X86::Instruction&) override;
    virtual void CALL_RM32(const X86::Instruction&) override;
    virtual void CALL_imm16(const X86::Instruction&) override;
    virtual void CALL_imm16_imm16(const X86::Instruction&) override;
    virtual void CALL_imm16_imm32(const X86::Instruction&) override;
    virtual void CALL_imm32(const X86::Instruction&) override;
    virtual void CBW(const X86::Instruction&) override;
    virtual void CDQ(const X86::Instruction&) override;
    virtual void CLC(const X86::Instruction&) override;
    virtual void CLD(const X86::Instruction&) override;
    virtual void CLI(const X86::Instruction&) override;
    virtual void CLTS(const X86::Instruction&) override;
    virtual void CMC(const X86::Instruction&) override;
    virtual void CMOVcc_reg16_RM16(const X86::Instruction&) override;
    virtual void CMOVcc_reg32_RM32(const X86::Instruction&) override;
    virtual void CMPSB(const X86::Instruction&) override;
    virtual void CMPSD(const X86::Instruction&) override;
    virtual void CMPSW(const X86::Instruction&) override;
    virtual void CMPXCHG_RM16_reg16(const X86::Instruction&) override;
    virtual void CMPXCHG_RM32_reg32(const X86::Instruction&) override;
    virtual void CMPXCHG_RM8_reg8(const X86::Instruction&) override;
    virtual void CMP_AL_imm8(const X86::Instruction&) override;
    virtual void CMP_AX_imm16(const X86::Instruction&) override;
    virtual void CMP_EAX_imm32(const X86::Instruction&) override;
    virtual void CMP_RM16_imm16(const X86::Instruction&) override;
    virtual void CMP_RM16_imm8(const X86::Instruction&) override;
    virtual void CMP_RM16_reg16(const X86::Instruction&) override;
    virtual void CMP_RM32_imm32(const X86::Instruction&) override;
    virtual void CMP_RM32_imm8(const X86::Instruction&) override;
    virtual void CMP_RM32_reg32(const X86::Instruction&) override;
    virtual void CMP_RM8_imm8(const X86::Instruction&) override;
    virtual void CMP_RM8_reg8(const X86::Instruction&) override;
    virtual void CMP_reg16_RM16(const X86::Instruction&) override;
    virtual void CMP_reg32_RM32(const X86::Instruction&) override;
    virtual void CMP_reg8_RM8(const X86::Instruction&) override;
    virtual void CPUID(const X86::Instruction&) override;
    virtual void CWD(const X86::Instruction&) override;
    virtual void CWDE(const X86::Instruction&) override;
    virtual void DAA(const X86::Instruction&) override;
    virtual void DAS(const X86::Instruction&) override;
    virtual void DEC_RM16(const X86::Instruction&) override;
    virtual void DEC_RM32(const X86::Instruction&) override;
    virtual void DEC_RM8(const X86::Instruction&) override;
    virtual void DEC_reg16(const X86::Instruction&) override;
    virtual void DEC_reg32(const X86::Instruction&) override;
    virtual void DIV_RM16(const X86::Instruction&) override;
    virtual void DIV_RM32(const X86::Instruction&) override;
    virtual void DIV_RM8(const X86::Instruction&) override;
    virtual void ENTER16(const X86::Instruction&) override;
    virtual void ENTER32(const X86::Instruction&) override;
    virtual void ESCAPE(const X86::Instruction&) override;
    virtual void HLT(const X86::Instruction&) override;
    virtual void IDIV_RM16(const X86::Instruction&) override;
    virtual void IDIV_RM32(const X86::Instruction&) override;
    virtual void IDIV_RM8(const X86::Instruction&) override;
    virtual void IMUL_RM16(const X86::Instruction&) override;
    virtual void IMUL_RM32(const X86::Instruction&) override;
    virtual void IMUL_RM8(const X86::Instruction&) override;
    virtual void IMUL_reg16_RM16(const X86::Instruction&) override;
    virtual void IMUL_reg16_RM16_imm16(const X86::Instruction&) override;
    virtual void IMUL_reg16_RM16_imm8(const X86::Instruction&) override;
    virtual void IMUL_reg32_RM32(const X86::Instruction&) override;
    virtual void IMUL_reg32_RM32_imm32(const X86::Instruction&) override;
    virtual void IMUL_reg32_RM32_imm8(const X86::Instruction&) override;
    virtual void INC_RM16(const X86::Instruction&) override;
    virtual void INC_RM32(const X86::Instruction&) override;
    virtual void INC_RM8(const X86::Instruction&) override;
    virtual void INC_reg16(const X86::Instruction&) override;
    virtual void INC_reg32(const X86::Instruction&) override;
    virtual void INSB(const X86::Instruction&) override;
    virtual void INSD(const X86::Instruction&) override;
    virtual void INSW(const X86::Instruction&) override;
    virtual void INT3(const X86::Instruction&) override;
    virtual void INTO(const X86::Instruction&) override;
    virtual void INT_imm8(const X86::Instruction&) override;
    virtual void INVLPG(const X86::Instruction&) override;
    virtual void IN_AL_DX(const X86::Instruction&) override;
    virtual void IN_AL_imm8(const X86::Instruction&) override;
    virtual void IN_AX_DX(const X86::Instruction&) override;
    virtual void IN_AX_imm8(const X86::Instruction&) override;
    virtual void IN_EAX_DX(const X86::Instruction&) override;
    virtual void IN_EAX_imm8(const X86::Instruction&) override;
    virtual void IRET(const X86::Instruction&) override;
    virtual void JCXZ_imm8(const X86::Instruction&) override;
    virtual void JMP_FAR_mem16(const X86::Instruction&) override;
    virtual void JMP_FAR_mem32(const X86::Instruction&) override;
    virtual void JMP_RM16(const X86::Instruction&) override;
    virtual void JMP_RM32(const X86::Instruction&) override;
    virtual void JMP_imm16(const X86::Instruction&) override;
    virtual void JMP_imm16_imm16(const X86::Instruction&) override;
    virtual void JMP_imm16_imm32(const X86::Instruction&) override;
    virtual void JMP_imm32(const X86::Instruction&) override;
    virtual void JMP_short_imm8(const X86::Instruction&) override;
    virtual void Jcc_NEAR_imm(const X86::Instruction&) override;
    virtual void Jcc_imm8(const X86::Instruction&) override;
    virtual void LAHF(const X86::Instruction&) override;
    virtual void LAR_reg16_RM16(const X86::Instruction&) override;
    virtual void LAR_reg32_RM32(const X86::Instruction&) override;
    virtual void LDS_reg16_mem16(const X86::Instruction&) override;
    virtual void LDS_reg32_mem32(const X86::Instruction&) override;
    virtual void LEAVE16(const X86::Instruction&) override;
    virtual void LEAVE32(const X86::Instruction&) override;
    virtual void LEA_reg16_mem16(const X86::Instruction&) override;
    virtual void LEA_reg32_mem32(const X86::Instruction&) override;
    virtual void LES_reg16_mem16(const X86::Instruction&) override;
    virtual void LES_reg32_mem32(const X86::Instruction&) override;
    virtual void LFS_reg16_mem16(const X86::Instruction&) override;
    virtual void LFS_reg32_mem32(const X86::Instruction&) override;
    virtual void LGDT(const X86::Instruction&) override;
    virtual void LGS_reg16_mem16(const X86::Instruction&) override;
    virtual void LGS_reg32_mem32(const X86::Instruction&) override;
    virtual void LIDT(const X86::Instruction&) override;
    virtual void LLDT_RM16(const X86::Instruction&) override;
    virtual void LMSW_RM16(const X86::Instruction&) override;
    virtual void LODSB(const X86::Instruction&) override;
    virtual void LODSD(const X86::Instruction&) override;
    virtual void LODSW(const X86::Instruction&) override;
    virtual void LOOPNZ_imm8(const X86::Instruction&) override;
    virtual void LOOPZ_imm8(const X86::Instruction&) override;
    virtual void LOOP_imm8(const X86::Instruction&) override;
    virtual void LSL_reg16_RM16(const X86::Instruction&) override;
    virtual void LSL_reg32_RM32(const X86::Instruction&) override;
    virtual void LSS_reg16_mem16(const X86::Instruction&) override;
    virtual void LSS_reg32_mem32(const X86::Instruction&) override;
    virtual void LTR_RM16(const X86::Instruction&) override;
    virtual void MOVSB(const X86::Instruction&) override;
    virtual void MOVSD(const X86::Instruction&) override;
    virtual void MOVSW(const X86::Instruction&) override;
    virtual void MOVSX_reg16_RM8(const X86::Instruction&) override;
    virtual void MOVSX_reg32_RM16(const X86::Instruction&) override;
    virtual void MOVSX_reg32_RM8(const X86::Instruction&) override;
    virtual void MOVZX_reg16_RM8(const X86::Instruction&) override;
    virtual void MOVZX_reg32_RM16(const X86::Instruction&) override;
    virtual void MOVZX_reg32_RM8(const X86::Instruction&) override;
    virtual void MOV_AL_moff8(const X86::Instruction&) override;
    virtual void MOV_AX_moff16(const X86::Instruction&) override;
    virtual void MOV_CR_reg32(const X86::Instruction&) override;
    virtual void MOV_DR_reg32(const X86::Instruction&) override;
    virtual void MOV_EAX_moff32(const X86::Instruction&) override;
    virtual void MOV_RM16_imm16(const X86::Instruction&) override;
    virtual void MOV_RM16_reg16(const X86::Instruction&) override;
    virtual void MOV_RM16_seg(const X86::Instruction&) override;
    virtual void MOV_RM32_imm32(const X86::Instruction&) override;
    virtual void MOV_RM32_reg32(const X86::Instruction&) override;
    virtual void MOV_RM8_imm8(const X86::Instruction&) override;
    virtual void MOV_RM8_reg8(const X86::Instruction&) override;
    virtual void MOV_moff16_AX(const X86::Instruction&) override;
    virtual void MOV_moff32_EAX(const X86::Instruction&) override;
    virtual void MOV_moff8_AL(const X86::Instruction&) override;
    virtual void MOV_reg16_RM16(const X86::Instruction&) override;
    virtual void MOV_reg16_imm16(const X86::Instruction&) override;
    virtual void MOV_reg32_CR(const X86::Instruction&) override;
    virtual void MOV_reg32_DR(const X86::Instruction&) override;
    virtual void MOV_reg32_RM32(const X86::Instruction&) override;
    virtual void MOV_reg32_imm32(const X86::Instruction&) override;
    virtual void MOV_reg8_RM8(const X86::Instruction&) override;
    virtual void MOV_reg8_imm8(const X86::Instruction&) override;
    virtual void MOV_seg_RM16(const X86::Instruction&) override;
    virtual void MOV_seg_RM32(const X86::Instruction&) override;
    virtual void MUL_RM16(const X86::Instruction&) override;
    virtual void MUL_RM32(const X86::Instruction&) override;
    virtual void MUL_RM8(const X86::Instruction&) override;
    virtual void NEG_RM16(const X86::Instruction&) override;
    virtual void NEG_RM32(const X86::Instruction&) override;
    virtual void NEG_RM8(const X86::Instruction&) override;
    virtual void NOP(const X86::Instruction&) override;
    virtual void NOT_RM16(const X86::Instruction&) override;
    virtual void NOT_RM32(const X86::Instruction&) override;
    virtual void NOT_RM8(const X86::Instruction&) override;
    virtual void OR_AL_imm8(const X86::Instruction&) override;
    virtual void OR_AX_imm16(const X86::Instruction&) override;
    virtual void OR_EAX_imm32(const X86::Instruction&) override;
    virtual void OR_RM16_imm16(const X86::Instruction&) override;
    virtual void OR_RM16_imm8(const X86::Instruction&) override;
    virtual void OR_RM16_reg16(const X86::Instruction&) override;
    virtual void OR_RM32_imm32(const X86::Instruction&) override;
    virtual void OR_RM32_imm8(const X86::Instruction&) override;
    virtual void OR_RM32_reg32(const X86::Instruction&) override;
    virtual void OR_RM8_imm8(const X86::Instruction&) override;
    virtual void OR_RM8_reg8(const X86::Instruction&) override;
    virtual void OR_reg16_RM16(const X86::Instruction&) override;
    virtual void OR_reg32_RM32(const X86::Instruction&) override;
    virtual void OR_reg8_RM8(const X86::Instruction&) override;
    virtual void OUTSB(const X86::Instruction&) override;
    virtual void OUTSD(const X86::Instruction&) override;
    virtual void OUTSW(const X86::Instruction&) override;
    virtual void OUT_DX_AL(const X86::Instruction&) override;
    virtual void OUT_DX_AX(const X86::Instruction&) override;
    virtual void OUT_DX_EAX(const X86::Instruction&) override;
    virtual void OUT_imm8_AL(const X86::Instruction&) override;
    virtual void OUT_imm8_AX(const X86::Instruction&) override;
    virtual void OUT_imm8_EAX(const X86::Instruction&) override;
    virtual void PADDB_mm1_mm2m64(const X86::Instruction&) override;
    virtual void PADDW_mm1_mm2m64(const X86::Instruction&) override;
    virtual void PADDD_mm1_mm2m64(const X86::Instruction&) override;
    virtual void POPA(const X86::Instruction&) override;
    virtual void POPAD(const X86::Instruction&) override;
    virtual void POPF(const X86::Instruction&) override;
    virtual void POPFD(const X86::Instruction&) override;
    virtual void POP_DS(const X86::Instruction&) override;
    virtual void POP_ES(const X86::Instruction&) override;
    virtual void POP_FS(const X86::Instruction&) override;
    virtual void POP_GS(const X86::Instruction&) override;
    virtual void POP_RM16(const X86::Instruction&) override;
    virtual void POP_RM32(const X86::Instruction&) override;
    virtual void POP_SS(const X86::Instruction&) override;
    virtual void POP_reg16(const X86::Instruction&) override;
    virtual void POP_reg32(const X86::Instruction&) override;
    virtual void PUSHA(const X86::Instruction&) override;
    virtual void PUSHAD(const X86::Instruction&) override;
    virtual void PUSHF(const X86::Instruction&) override;
    virtual void PUSHFD(const X86::Instruction&) override;
    virtual void PUSH_CS(const X86::Instruction&) override;
    virtual void PUSH_DS(const X86::Instruction&) override;
    virtual void PUSH_ES(const X86::Instruction&) override;
    virtual void PUSH_FS(const X86::Instruction&) override;
    virtual void PUSH_GS(const X86::Instruction&) override;
    virtual void PUSH_RM16(const X86::Instruction&) override;
    virtual void PUSH_RM32(const X86::Instruction&) override;
    virtual void PUSH_SP_8086_80186(const X86::Instruction&) override;
    virtual void PUSH_SS(const X86::Instruction&) override;
    virtual void PUSH_imm16(const X86::Instruction&) override;
    virtual void PUSH_imm32(const X86::Instruction&) override;
    virtual void PUSH_imm8(const X86::Instruction&) override;
    virtual void PUSH_reg16(const X86::Instruction&) override;
    virtual void PUSH_reg32(const X86::Instruction&) override;
    virtual void RCL_RM16_1(const X86::Instruction&) override;
    virtual void RCL_RM16_CL(const X86::Instruction&) override;
    virtual void RCL_RM16_imm8(const X86::Instruction&) override;
    virtual void RCL_RM32_1(const X86::Instruction&) override;
    virtual void RCL_RM32_CL(const X86::Instruction&) override;
    virtual void RCL_RM32_imm8(const X86::Instruction&) override;
    virtual void RCL_RM8_1(const X86::Instruction&) override;
    virtual void RCL_RM8_CL(const X86::Instruction&) override;
    virtual void RCL_RM8_imm8(const X86::Instruction&) override;
    virtual void RCR_RM16_1(const X86::Instruction&) override;
    virtual void RCR_RM16_CL(const X86::Instruction&) override;
    virtual void RCR_RM16_imm8(const X86::Instruction&) override;
    virtual void RCR_RM32_1(const X86::Instruction&) override;
    virtual void RCR_RM32_CL(const X86::Instruction&) override;
    virtual void RCR_RM32_imm8(const X86::Instruction&) override;
    virtual void RCR_RM8_1(const X86::Instruction&) override;
    virtual void RCR_RM8_CL(const X86::Instruction&) override;
    virtual void RCR_RM8_imm8(const X86::Instruction&) override;
    virtual void RDTSC(const X86::Instruction&) override;
    virtual void RET(const X86::Instruction&) override;
    virtual void RETF(const X86::Instruction&) override;
    virtual void RETF_imm16(const X86::Instruction&) override;
    virtual void RET_imm16(const X86::Instruction&) override;
    virtual void ROL_RM16_1(const X86::Instruction&) override;
    virtual void ROL_RM16_CL(const X86::Instruction&) override;
    virtual void ROL_RM16_imm8(const X86::Instruction&) override;
    virtual void ROL_RM32_1(const X86::Instruction&) override;
    virtual void ROL_RM32_CL(const X86::Instruction&) override;
    virtual void ROL_RM32_imm8(const X86::Instruction&) override;
    virtual void ROL_RM8_1(const X86::Instruction&) override;
    virtual void ROL_RM8_CL(const X86::Instruction&) override;
    virtual void ROL_RM8_imm8(const X86::Instruction&) override;
    virtual void ROR_RM16_1(const X86::Instruction&) override;
    virtual void ROR_RM16_CL(const X86::Instruction&) override;
    virtual void ROR_RM16_imm8(const X86::Instruction&) override;
    virtual void ROR_RM32_1(const X86::Instruction&) override;
    virtual void ROR_RM32_CL(const X86::Instruction&) override;
    virtual void ROR_RM32_imm8(const X86::Instruction&) override;
    virtual void ROR_RM8_1(const X86::Instruction&) override;
    virtual void ROR_RM8_CL(const X86::Instruction&) override;
    virtual void ROR_RM8_imm8(const X86::Instruction&) override;
    virtual void SAHF(const X86::Instruction&) override;
    virtual void SALC(const X86::Instruction&) override;
    virtual void SAR_RM16_1(const X86::Instruction&) override;
    virtual void SAR_RM16_CL(const X86::Instruction&) override;
    virtual void SAR_RM16_imm8(const X86::Instruction&) override;
    virtual void SAR_RM32_1(const X86::Instruction&) override;
    virtual void SAR_RM32_CL(const X86::Instruction&) override;
    virtual void SAR_RM32_imm8(const X86::Instruction&) override;
    virtual void SAR_RM8_1(const X86::Instruction&) override;
    virtual void SAR_RM8_CL(const X86::Instruction&) override;
    virtual void SAR_RM8_imm8(const X86::Instruction&) override;
    virtual void SBB_AL_imm8(const X86::Instruction&) override;
    virtual void SBB_AX_imm16(const X86::Instruction&) override;
    virtual void SBB_EAX_imm32(const X86::Instruction&) override;
    virtual void SBB_RM16_imm16(const X86::Instruction&) override;
    virtual void SBB_RM16_imm8(const X86::Instruction&) override;
    virtual void SBB_RM16_reg16(const X86::Instruction&) override;
    virtual void SBB_RM32_imm32(const X86::Instruction&) override;
    virtual void SBB_RM32_imm8(const X86::Instruction&) override;
    virtual void SBB_RM32_reg32(const X86::Instruction&) override;
    virtual void SBB_RM8_imm8(const X86::Instruction&) override;
    virtual void SBB_RM8_reg8(const X86::Instruction&) override;
    virtual void SBB_reg16_RM16(const X86::Instruction&) override;
    virtual void SBB_reg32_RM32(const X86::Instruction&) override;
    virtual void SBB_reg8_RM8(const X86::Instruction&) override;
    virtual void SCASB(const X86::Instruction&) override;
    virtual void SCASD(const X86::Instruction&) override;
    virtual void SCASW(const X86::Instruction&) override;
    virtual void SETcc_RM8(const X86::Instruction&) override;
    virtual void SGDT(const X86::Instruction&) override;
    virtual void SHLD_RM16_reg16_CL(const X86::Instruction&) override;
    virtual void SHLD_RM16_reg16_imm8(const X86::Instruction&) override;
    virtual void SHLD_RM32_reg32_CL(const X86::Instruction&) override;
    virtual void SHLD_RM32_reg32_imm8(const X86::Instruction&) override;
    virtual void SHL_RM16_1(const X86::Instruction&) override;
    virtual void SHL_RM16_CL(const X86::Instruction&) override;
    virtual void SHL_RM16_imm8(const X86::Instruction&) override;
    virtual void SHL_RM32_1(const X86::Instruction&) override;
    virtual void SHL_RM32_CL(const X86::Instruction&) override;
    virtual void SHL_RM32_imm8(const X86::Instruction&) override;
    virtual void SHL_RM8_1(const X86::Instruction&) override;
    virtual void SHL_RM8_CL(const X86::Instruction&) override;
    virtual void SHL_RM8_imm8(const X86::Instruction&) override;
    virtual void SHRD_RM16_reg16_CL(const X86::Instruction&) override;
    virtual void SHRD_RM16_reg16_imm8(const X86::Instruction&) override;
    virtual void SHRD_RM32_reg32_CL(const X86::Instruction&) override;
    virtual void SHRD_RM32_reg32_imm8(const X86::Instruction&) override;
    virtual void SHR_RM16_1(const X86::Instruction&) override;
    virtual void SHR_RM16_CL(const X86::Instruction&) override;
    virtual void SHR_RM16_imm8(const X86::Instruction&) override;
    virtual void SHR_RM32_1(const X86::Instruction&) override;
    virtual void SHR_RM32_CL(const X86::Instruction&) override;
    virtual void SHR_RM32_imm8(const X86::Instruction&) override;
    virtual void SHR_RM8_1(const X86::Instruction&) override;
    virtual void SHR_RM8_CL(const X86::Instruction&) override;
    virtual void SHR_RM8_imm8(const X86::Instruction&) override;
    virtual void SIDT(const X86::Instruction&) override;
    virtual void SLDT_RM16(const X86::Instruction&) override;
    virtual void SMSW_RM16(const X86::Instruction&) override;
    virtual void STC(const X86::Instruction&) override;
    virtual void STD(const X86::Instruction&) override;
    virtual void STI(const X86::Instruction&) override;
    virtual void STOSB(const X86::Instruction&) override;
    virtual void STOSD(const X86::Instruction&) override;
    virtual void STOSW(const X86::Instruction&) override;
    virtual void STR_RM16(const X86::Instruction&) override;
    virtual void SUB_AL_imm8(const X86::Instruction&) override;
    virtual void SUB_AX_imm16(const X86::Instruction&) override;
    virtual void SUB_EAX_imm32(const X86::Instruction&) override;
    virtual void SUB_RM16_imm16(const X86::Instruction&) override;
    virtual void SUB_RM16_imm8(const X86::Instruction&) override;
    virtual void SUB_RM16_reg16(const X86::Instruction&) override;
    virtual void SUB_RM32_imm32(const X86::Instruction&) override;
    virtual void SUB_RM32_imm8(const X86::Instruction&) override;
    virtual void SUB_RM32_reg32(const X86::Instruction&) override;
    virtual void SUB_RM8_imm8(const X86::Instruction&) override;
    virtual void SUB_RM8_reg8(const X86::Instruction&) override;
    virtual void SUB_reg16_RM16(const X86::Instruction&) override;
    virtual void SUB_reg32_RM32(const X86::Instruction&) override;
    virtual void SUB_reg8_RM8(const X86::Instruction&) override;
    virtual void TEST_AL_imm8(const X86::Instruction&) override;
    virtual void TEST_AX_imm16(const X86::Instruction&) override;
    virtual void TEST_EAX_imm32(const X86::Instruction&) override;
    virtual void TEST_RM16_imm16(const X86::Instruction&) override;
    virtual void TEST_RM16_reg16(const X86::Instruction&) override;
    virtual void TEST_RM32_imm32(const X86::Instruction&) override;
    virtual void TEST_RM32_reg32(const X86::Instruction&) override;
    virtual void TEST_RM8_imm8(const X86::Instruction&) override;
    virtual void TEST_RM8_reg8(const X86::Instruction&) override;
    virtual void UD0(const X86::Instruction&) override;
    virtual void UD1(const X86::Instruction&) override;
    virtual void UD2(const X86::Instruction&) override;
    virtual void VERR_RM16(const X86::Instruction&) override;
    virtual void VERW_RM16(const X86::Instruction&) override;
    virtual void WAIT(const X86::Instruction&) override;
    virtual void WBINVD(const X86::Instruction&) override;
    virtual void XADD_RM16_reg16(const X86::Instruction&) override;
    virtual void XADD_RM32_reg32(const X86::Instruction&) override;
    virtual void XADD_RM8_reg8(const X86::Instruction&) override;
    virtual void XCHG_AX_reg16(const X86::Instruction&) override;
    virtual void XCHG_EAX_reg32(const X86::Instruction&) override;
    virtual void XCHG_reg16_RM16(const X86::Instruction&) override;
    virtual void XCHG_reg32_RM32(const X86::Instruction&) override;
    virtual void XCHG_reg8_RM8(const X86::Instruction&) override;
    virtual void XLAT(const X86::Instruction&) override;
    virtual void XOR_AL_imm8(const X86::Instruction&) override;
    virtual void XOR_AX_imm16(const X86::Instruction&) override;
    virtual void XOR_EAX_imm32(const X86::Instruction&) override;
    virtual void XOR_RM16_imm16(const X86::Instruction&) override;
    virtual void XOR_RM16_imm8(const X86::Instruction&) override;
    virtual void XOR_RM16_reg16(const X86::Instruction&) override;
    virtual void XOR_RM32_imm32(const X86::Instruction&) override;
    virtual void XOR_RM32_imm8(const X86::Instruction&) override;
    virtual void XOR_RM32_reg32(const X86::Instruction&) override;
    virtual void XOR_RM8_imm8(const X86::Instruction&) override;
    virtual void XOR_RM8_reg8(const X86::Instruction&) override;
    virtual void XOR_reg16_RM16(const X86::Instruction&) override;
    virtual void XOR_reg32_RM32(const X86::Instruction&) override;
    virtual void XOR_reg8_RM8(const X86::Instruction&) override;
    virtual void MOVQ_mm1_mm2m64(const X86::Instruction&) override;
    virtual void EMMS(const X86::Instruction&) override;
    virtual void MOVQ_mm1_m64_mm2(const X86::Instruction&) override;
    virtual void wrap_0xC0(const X86::Instruction&) override;
    virtual void wrap_0xC1_16(const X86::Instruction&) override;
    virtual void wrap_0xC1_32(const X86::Instruction&) override;
    virtual void wrap_0xD0(const X86::Instruction&) override;
    virtual void wrap_0xD1_16(const X86::Instruction&) override;
    virtual void wrap_0xD1_32(const X86::Instruction&) override;
    virtual void wrap_0xD2(const X86::Instruction&) override;
    virtual void wrap_0xD3_16(const X86::Instruction&) override;
    virtual void wrap_0xD3_32(const X86::Instruction&) override;

    template<bool update_dest, typename Op>
    void generic_AL_imm8(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_AX_imm16(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_EAX_imm32(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_RM16_imm16(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_RM16_imm8(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_RM16_reg16(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_RM32_imm32(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_RM32_imm8(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_RM32_reg32(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_RM8_imm8(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_RM8_reg8(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_reg16_RM16(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_reg32_RM32(Op, const X86::Instruction&);
    template<bool update_dest, typename Op>
    void generic_reg8_RM8(Op, const X86::Instruction&);

private:
    Emulator& m_emulator;

    PartAddressableRegister m_gpr[8];
    u16 m_segment[8] { 0 };
    u32 m_eflags { 0 };

    u32 m_eip { 0 };
};

}
