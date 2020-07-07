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

#include "SoftCPU.h"
#include "Emulator.h"
#include <AK/Assertions.h>
#include <stdio.h>

namespace UserspaceEmulator {

SoftCPU::SoftCPU(Emulator& emulator)
    : m_emulator(emulator)
{
    m_reg32_table[X86::RegisterEAX] = &m_eax;
    m_reg32_table[X86::RegisterEBX] = &m_ebx;
    m_reg32_table[X86::RegisterECX] = &m_ecx;
    m_reg32_table[X86::RegisterEDX] = &m_edx;
    m_reg32_table[X86::RegisterEBP] = &m_ebp;
    m_reg32_table[X86::RegisterESP] = &m_esp;
    m_reg32_table[X86::RegisterESI] = &m_esi;
    m_reg32_table[X86::RegisterEDI] = &m_edi;
}

void SoftCPU::dump() const
{
    printf("eax=%08x ebx=%08x ecx=%08x edx=%08x ", m_eax, m_ebx, m_ecx, m_edx);
    printf("ebp=%08x esp=%08x esi=%08x edi=%08x ", m_ebp, m_esp, m_esi, m_edi);
    printf("o=%u s=%u z=%u a=%u p=%u c=%u\n", m_of, m_sf, m_zf, m_af, m_pf, m_cf);
}

void SoftCPU::AAA(const X86::Instruction&) { TODO(); }
void SoftCPU::AAD(const X86::Instruction&) { TODO(); }
void SoftCPU::AAM(const X86::Instruction&) { TODO(); }
void SoftCPU::AAS(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_AL_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_AX_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_EAX_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_RM32_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::ADC_reg8_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_AL_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_AX_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_EAX_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_RM32_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::ADD_reg8_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_AL_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_AX_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_EAX_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_RM32_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::AND_reg8_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::ARPL(const X86::Instruction&) { TODO(); }
void SoftCPU::BOUND(const X86::Instruction&) { TODO(); }
void SoftCPU::BSF_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::BSF_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::BSR_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::BSR_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::BSWAP_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::BTC_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::BTC_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::BTC_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::BTC_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::BTR_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::BTR_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::BTR_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::BTR_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::BTS_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::BTS_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::BTS_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::BTS_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::BT_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::BT_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::BT_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::BT_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::CALL_FAR_mem16(const X86::Instruction&) { TODO(); }
void SoftCPU::CALL_FAR_mem32(const X86::Instruction&) { TODO(); }
void SoftCPU::CALL_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::CALL_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::CALL_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::CALL_imm16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::CALL_imm16_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::CALL_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::CBW(const X86::Instruction&) { TODO(); }
void SoftCPU::CDQ(const X86::Instruction&) { TODO(); }
void SoftCPU::CLC(const X86::Instruction&) { TODO(); }
void SoftCPU::CLD(const X86::Instruction&) { TODO(); }
void SoftCPU::CLI(const X86::Instruction&) { TODO(); }
void SoftCPU::CLTS(const X86::Instruction&) { TODO(); }
void SoftCPU::CMC(const X86::Instruction&) { TODO(); }
void SoftCPU::CMOVcc_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::CMOVcc_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::CMPSB(const X86::Instruction&) { TODO(); }
void SoftCPU::CMPSD(const X86::Instruction&) { TODO(); }
void SoftCPU::CMPSW(const X86::Instruction&) { TODO(); }
void SoftCPU::CMPXCHG_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::CMPXCHG_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::CMPXCHG_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_AL_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_AX_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_EAX_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_RM32_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::CMP_reg8_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::CPUID(const X86::Instruction&) { TODO(); }
void SoftCPU::CWD(const X86::Instruction&) { TODO(); }
void SoftCPU::CWDE(const X86::Instruction&) { TODO(); }
void SoftCPU::DAA(const X86::Instruction&) { TODO(); }
void SoftCPU::DAS(const X86::Instruction&) { TODO(); }
void SoftCPU::DEC_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::DEC_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::DEC_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::DEC_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::DEC_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::DIV_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::DIV_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::DIV_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::ENTER16(const X86::Instruction&) { TODO(); }
void SoftCPU::ENTER32(const X86::Instruction&) { TODO(); }
void SoftCPU::ESCAPE(const X86::Instruction&) { TODO(); }
void SoftCPU::HLT(const X86::Instruction&) { TODO(); }
void SoftCPU::IDIV_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::IDIV_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::IDIV_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_reg16_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_reg16_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_reg32_RM32_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_reg32_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::INC_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::INC_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::INC_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::INC_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::INC_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::INSB(const X86::Instruction&) { TODO(); }
void SoftCPU::INSD(const X86::Instruction&) { TODO(); }
void SoftCPU::INSW(const X86::Instruction&) { TODO(); }
void SoftCPU::INT3(const X86::Instruction&) { TODO(); }
void SoftCPU::INTO(const X86::Instruction&) { TODO(); }

void SoftCPU::INT_imm8(const X86::Instruction& insn)
{
    ASSERT(insn.imm8() == 0x82);
    m_eax = m_emulator.virt_syscall(m_eax, m_edx, m_ecx, m_ebx);
}

void SoftCPU::INVLPG(const X86::Instruction&) { TODO(); }
void SoftCPU::IN_AL_DX(const X86::Instruction&) { TODO(); }
void SoftCPU::IN_AL_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::IN_AX_DX(const X86::Instruction&) { TODO(); }
void SoftCPU::IN_AX_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::IN_EAX_DX(const X86::Instruction&) { TODO(); }
void SoftCPU::IN_EAX_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::IRET(const X86::Instruction&) { TODO(); }
void SoftCPU::JCXZ_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::JMP_FAR_mem16(const X86::Instruction&) { TODO(); }
void SoftCPU::JMP_FAR_mem32(const X86::Instruction&) { TODO(); }
void SoftCPU::JMP_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::JMP_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::JMP_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::JMP_imm16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::JMP_imm16_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::JMP_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::JMP_short_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::Jcc_NEAR_imm(const X86::Instruction&) { TODO(); }
void SoftCPU::Jcc_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::LAHF(const X86::Instruction&) { TODO(); }
void SoftCPU::LAR_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::LAR_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::LDS_reg16_mem16(const X86::Instruction&) { TODO(); }
void SoftCPU::LDS_reg32_mem32(const X86::Instruction&) { TODO(); }
void SoftCPU::LEAVE16(const X86::Instruction&) { TODO(); }
void SoftCPU::LEAVE32(const X86::Instruction&) { TODO(); }
void SoftCPU::LEA_reg16_mem16(const X86::Instruction&) { TODO(); }
void SoftCPU::LEA_reg32_mem32(const X86::Instruction&) { TODO(); }
void SoftCPU::LES_reg16_mem16(const X86::Instruction&) { TODO(); }
void SoftCPU::LES_reg32_mem32(const X86::Instruction&) { TODO(); }
void SoftCPU::LFS_reg16_mem16(const X86::Instruction&) { TODO(); }
void SoftCPU::LFS_reg32_mem32(const X86::Instruction&) { TODO(); }
void SoftCPU::LGDT(const X86::Instruction&) { TODO(); }
void SoftCPU::LGS_reg16_mem16(const X86::Instruction&) { TODO(); }
void SoftCPU::LGS_reg32_mem32(const X86::Instruction&) { TODO(); }
void SoftCPU::LIDT(const X86::Instruction&) { TODO(); }
void SoftCPU::LLDT_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::LMSW_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::LODSB(const X86::Instruction&) { TODO(); }
void SoftCPU::LODSD(const X86::Instruction&) { TODO(); }
void SoftCPU::LODSW(const X86::Instruction&) { TODO(); }
void SoftCPU::LOOPNZ_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::LOOPZ_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::LOOP_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::LSL_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::LSL_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::LSS_reg16_mem16(const X86::Instruction&) { TODO(); }
void SoftCPU::LSS_reg32_mem32(const X86::Instruction&) { TODO(); }
void SoftCPU::LTR_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVSB(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVSD(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVSW(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVSX_reg16_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVSX_reg32_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVSX_reg32_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVZX_reg16_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVZX_reg32_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVZX_reg32_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_AL_moff8(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_AX_moff16(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_CR_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_DR_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_EAX_moff32(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_RM16_seg(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_RM32_imm32(const X86::Instruction&) { TODO(); }

void SoftCPU::MOV_RM32_reg32(const X86::Instruction& insn)
{
    ASSERT(insn.modrm().is_register());
    *m_reg32_table[insn.modrm().register_index()] = *m_reg32_table[insn.register_index()];
}

void SoftCPU::MOV_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_moff16_AX(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_moff32_EAX(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_moff8_AL(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_reg16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_reg32_CR(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_reg32_DR(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_reg32_RM32(const X86::Instruction&) { TODO(); }

void SoftCPU::MOV_reg32_imm32(const X86::Instruction& insn)
{
    *m_reg32_table[insn.register_index()] = insn.imm32();
}

void SoftCPU::MOV_reg8_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_reg8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_seg_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_seg_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::MUL_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::MUL_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::MUL_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::NEG_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::NEG_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::NEG_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::NOP(const X86::Instruction&) { TODO(); }
void SoftCPU::NOT_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::NOT_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::NOT_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_AL_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_AX_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_EAX_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_RM32_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::OR_reg8_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::OUTSB(const X86::Instruction&) { TODO(); }
void SoftCPU::OUTSD(const X86::Instruction&) { TODO(); }
void SoftCPU::OUTSW(const X86::Instruction&) { TODO(); }
void SoftCPU::OUT_DX_AL(const X86::Instruction&) { TODO(); }
void SoftCPU::OUT_DX_AX(const X86::Instruction&) { TODO(); }
void SoftCPU::OUT_DX_EAX(const X86::Instruction&) { TODO(); }
void SoftCPU::OUT_imm8_AL(const X86::Instruction&) { TODO(); }
void SoftCPU::OUT_imm8_AX(const X86::Instruction&) { TODO(); }
void SoftCPU::OUT_imm8_EAX(const X86::Instruction&) { TODO(); }
void SoftCPU::PADDB_mm1_mm2m64(const X86::Instruction&) { TODO(); }
void SoftCPU::PADDW_mm1_mm2m64(const X86::Instruction&) { TODO(); }
void SoftCPU::PADDD_mm1_mm2m64(const X86::Instruction&) { TODO(); }
void SoftCPU::POPA(const X86::Instruction&) { TODO(); }
void SoftCPU::POPAD(const X86::Instruction&) { TODO(); }
void SoftCPU::POPF(const X86::Instruction&) { TODO(); }
void SoftCPU::POPFD(const X86::Instruction&) { TODO(); }
void SoftCPU::POP_DS(const X86::Instruction&) { TODO(); }
void SoftCPU::POP_ES(const X86::Instruction&) { TODO(); }
void SoftCPU::POP_FS(const X86::Instruction&) { TODO(); }
void SoftCPU::POP_GS(const X86::Instruction&) { TODO(); }
void SoftCPU::POP_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::POP_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::POP_SS(const X86::Instruction&) { TODO(); }
void SoftCPU::POP_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::POP_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSHA(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSHAD(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSHF(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSHFD(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_CS(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_DS(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_ES(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_FS(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_GS(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_SP_8086_80186(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_SS(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::RCL_RM16_1(const X86::Instruction&) { TODO(); }
void SoftCPU::RCL_RM16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::RCL_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::RCL_RM32_1(const X86::Instruction&) { TODO(); }
void SoftCPU::RCL_RM32_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::RCL_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::RCL_RM8_1(const X86::Instruction&) { TODO(); }
void SoftCPU::RCL_RM8_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::RCL_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::RCR_RM16_1(const X86::Instruction&) { TODO(); }
void SoftCPU::RCR_RM16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::RCR_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::RCR_RM32_1(const X86::Instruction&) { TODO(); }
void SoftCPU::RCR_RM32_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::RCR_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::RCR_RM8_1(const X86::Instruction&) { TODO(); }
void SoftCPU::RCR_RM8_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::RCR_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::RDTSC(const X86::Instruction&) { TODO(); }
void SoftCPU::RET(const X86::Instruction&) { TODO(); }
void SoftCPU::RETF(const X86::Instruction&) { TODO(); }
void SoftCPU::RETF_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::RET_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::ROL_RM16_1(const X86::Instruction&) { TODO(); }
void SoftCPU::ROL_RM16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::ROL_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ROL_RM32_1(const X86::Instruction&) { TODO(); }
void SoftCPU::ROL_RM32_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::ROL_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ROL_RM8_1(const X86::Instruction&) { TODO(); }
void SoftCPU::ROL_RM8_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::ROL_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ROR_RM16_1(const X86::Instruction&) { TODO(); }
void SoftCPU::ROR_RM16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::ROR_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ROR_RM32_1(const X86::Instruction&) { TODO(); }
void SoftCPU::ROR_RM32_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::ROR_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::ROR_RM8_1(const X86::Instruction&) { TODO(); }
void SoftCPU::ROR_RM8_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::ROR_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SAHF(const X86::Instruction&) { TODO(); }
void SoftCPU::SALC(const X86::Instruction&) { TODO(); }
void SoftCPU::SAR_RM16_1(const X86::Instruction&) { TODO(); }
void SoftCPU::SAR_RM16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SAR_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SAR_RM32_1(const X86::Instruction&) { TODO(); }
void SoftCPU::SAR_RM32_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SAR_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SAR_RM8_1(const X86::Instruction&) { TODO(); }
void SoftCPU::SAR_RM8_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SAR_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_AL_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_AX_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_EAX_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_RM32_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::SBB_reg8_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::SCASB(const X86::Instruction&) { TODO(); }
void SoftCPU::SCASD(const X86::Instruction&) { TODO(); }
void SoftCPU::SCASW(const X86::Instruction&) { TODO(); }
void SoftCPU::SETcc_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::SGDT(const X86::Instruction&) { TODO(); }
void SoftCPU::SHLD_RM16_reg16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHLD_RM16_reg16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHLD_RM32_reg32_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHLD_RM32_reg32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHL_RM16_1(const X86::Instruction&) { TODO(); }
void SoftCPU::SHL_RM16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHL_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHL_RM32_1(const X86::Instruction&) { TODO(); }
void SoftCPU::SHL_RM32_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHL_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHL_RM8_1(const X86::Instruction&) { TODO(); }
void SoftCPU::SHL_RM8_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHL_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHRD_RM16_reg16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHRD_RM16_reg16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHRD_RM32_reg32_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHRD_RM32_reg32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHR_RM16_1(const X86::Instruction&) { TODO(); }
void SoftCPU::SHR_RM16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHR_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHR_RM32_1(const X86::Instruction&) { TODO(); }
void SoftCPU::SHR_RM32_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHR_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHR_RM8_1(const X86::Instruction&) { TODO(); }
void SoftCPU::SHR_RM8_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHR_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SIDT(const X86::Instruction&) { TODO(); }
void SoftCPU::SLDT_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::SMSW_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::STC(const X86::Instruction&) { TODO(); }
void SoftCPU::STD(const X86::Instruction&) { TODO(); }
void SoftCPU::STI(const X86::Instruction&) { TODO(); }
void SoftCPU::STOSB(const X86::Instruction&) { TODO(); }
void SoftCPU::STOSD(const X86::Instruction&) { TODO(); }
void SoftCPU::STOSW(const X86::Instruction&) { TODO(); }
void SoftCPU::STR_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_AL_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_AX_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_EAX_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_RM32_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_RM32_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::SUB_reg8_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::TEST_AL_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::TEST_AX_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::TEST_EAX_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::TEST_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::TEST_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::TEST_RM32_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::TEST_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::TEST_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::TEST_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::UD0(const X86::Instruction&) { TODO(); }
void SoftCPU::UD1(const X86::Instruction&) { TODO(); }
void SoftCPU::UD2(const X86::Instruction&) { TODO(); }
void SoftCPU::VERR_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::VERW_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::WAIT(const X86::Instruction&) { TODO(); }
void SoftCPU::WBINVD(const X86::Instruction&) { TODO(); }
void SoftCPU::XADD_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::XADD_RM32_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::XADD_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::XCHG_AX_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::XCHG_EAX_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::XCHG_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::XCHG_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::XCHG_reg8_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::XLAT(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_AL_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_AX_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_EAX_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_RM16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_RM16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_RM16_reg16(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_RM32_imm32(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_RM32_imm8(const X86::Instruction&) { TODO(); }

void SoftCPU::XOR_RM32_reg32(const X86::Instruction& insn)
{
    ASSERT(insn.modrm().is_register());
    auto& dest = *m_reg32_table[insn.modrm().register_index()];
    auto src = *m_reg32_table[insn.register_index()];
    dest ^= src;

    set_cf(false);
    set_of(false);
    set_zf(dest == 0);
    set_sf(dest & 0x80000000);
    // FIXME: set_pf
}

void SoftCPU::XOR_RM8_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_RM8_reg8(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::XOR_reg8_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVQ_mm1_mm2m64(const X86::Instruction&) { TODO(); }
void SoftCPU::EMMS(const X86::Instruction&) { TODO(); }
void SoftCPU::MOVQ_mm1_m64_mm2(const X86::Instruction&) { TODO(); }
void SoftCPU::wrap_0xC0(const X86::Instruction&) { TODO(); }
void SoftCPU::wrap_0xC1_16(const X86::Instruction&) { TODO(); }
void SoftCPU::wrap_0xC1_32(const X86::Instruction&) { TODO(); }
void SoftCPU::wrap_0xD0(const X86::Instruction&) { TODO(); }
void SoftCPU::wrap_0xD1_16(const X86::Instruction&) { TODO(); }
void SoftCPU::wrap_0xD1_32(const X86::Instruction&) { TODO(); }
void SoftCPU::wrap_0xD2(const X86::Instruction&) { TODO(); }
void SoftCPU::wrap_0xD3_16(const X86::Instruction&) { TODO(); }
void SoftCPU::wrap_0xD3_32(const X86::Instruction&) { TODO(); }

}
