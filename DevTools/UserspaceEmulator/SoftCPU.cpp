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
#include <string.h>

namespace UserspaceEmulator {

template<typename T>
struct TypeDoubler {
};
template<>
struct TypeDoubler<u8> {
    typedef u16 type;
};
template<>
struct TypeDoubler<u16> {
    typedef u32 type;
};
template<>
struct TypeDoubler<u32> {
    typedef u64 type;
};
template<>
struct TypeDoubler<i8> {
    typedef i16 type;
};
template<>
struct TypeDoubler<i16> {
    typedef i32 type;
};
template<>
struct TypeDoubler<i32> {
    typedef i64 type;
};

SoftCPU::SoftCPU(Emulator& emulator)
    : m_emulator(emulator)
{
    memset(m_gpr, 0, sizeof(m_gpr));

    m_segment[(int)X86::SegmentRegister::CS] = 0x18;
    m_segment[(int)X86::SegmentRegister::DS] = 0x20;
    m_segment[(int)X86::SegmentRegister::ES] = 0x20;
    m_segment[(int)X86::SegmentRegister::SS] = 0x20;
}

void SoftCPU::dump() const
{
    printf("eax=%08x ebx=%08x ecx=%08x edx=%08x ", eax(), ebx(), ecx(), edx());
    printf("ebp=%08x esp=%08x esi=%08x edi=%08x ", ebp(), esp(), esi(), edi());
    printf("o=%u s=%u z=%u a=%u p=%u c=%u\n", of(), sf(), zf(), af(), pf(), cf());
}

u8 SoftCPU::read8()
{
    auto value = read_memory8({ cs(), eip() });
    m_eip += 1;
    return value;
}

u16 SoftCPU::read16()
{
    auto value = read_memory16({ cs(), eip() });
    m_eip += 2;
    return value;
}

u32 SoftCPU::read32()
{
    auto value = read_memory32({ cs(), eip() });
    m_eip += 4;
    return value;
}

u8 SoftCPU::read_memory8(X86::LogicalAddress address)
{
    ASSERT(address.selector() == 0x18 || address.selector() == 0x20);
    auto value = m_emulator.mmu().read8(address.offset());
    printf("\033[36;1mread_memory8: @%08x -> %02x\033[0m\n", address.offset(), value);
    return value;
}

u16 SoftCPU::read_memory16(X86::LogicalAddress address)
{
    ASSERT(address.selector() == 0x18 || address.selector() == 0x20);
    auto value = m_emulator.mmu().read16(address.offset());
    printf("\033[36;1mread_memory16: @%08x -> %04x\033[0m\n", address.offset(), value);
    return value;
}

u32 SoftCPU::read_memory32(X86::LogicalAddress address)
{
    ASSERT(address.selector() == 0x18 || address.selector() == 0x20);
    auto value = m_emulator.mmu().read32(address.offset());
    printf("\033[36;1mread_memory32: @%08x -> %08x\033[0m\n", address.offset(), value);
    return value;
}

void SoftCPU::write_memory8(X86::LogicalAddress address, u8 value)
{
    ASSERT(address.selector() == 0x20);
    printf("\033[35;1mwrite_memory8: @%08x <- %02x\033[0m\n", address.offset(), value);
    m_emulator.mmu().write8(address.offset(), value);
}

void SoftCPU::write_memory16(X86::LogicalAddress address, u16 value)
{
    ASSERT(address.selector() == 0x20);
    printf("\033[35;1mwrite_memory16: @%08x <- %04x\033[0m\n", address.offset(), value);
    m_emulator.mmu().write16(address.offset(), value);
}

void SoftCPU::write_memory32(X86::LogicalAddress address, u32 value)
{
    ASSERT(address.selector() == 0x20);
    printf("\033[35;1mwrite_memory32: @%08x <- %08x\033[0m\n", address.offset(), value);
    m_emulator.mmu().write32(address.offset(), value);
}

void SoftCPU::push32(u32 value)
{
    set_esp(esp() - sizeof(value));
    write_memory32({ ss(), esp() }, value);
}

u32 SoftCPU::pop32()
{
    auto value = read_memory32({ ss(), esp() });
    set_esp(esp() + sizeof(value));
    return value;
}

template<typename Destination, typename Source>
static typename TypeDoubler<Destination>::type op_xor(SoftCPU& cpu, const Destination& dest, const Source& src)
{
    Destination result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(Destination) == 4) {
        asm volatile("xorl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u32)src));
    } else if constexpr (sizeof(Destination) == 2) {
        asm volatile("xor %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u16)src));
    } else if constexpr (sizeof(Destination) == 1) {
        asm volatile("xorb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u8)src));
    } else {
        ASSERT_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszpc(new_flags);
    return result;
}

template<typename Destination, typename Source>
static typename TypeDoubler<Destination>::type op_sub(SoftCPU& cpu, const Destination& dest, const Source& src)
{
    Destination result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(Destination) == 4) {
        asm volatile("subl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u32)src));
    } else if constexpr (sizeof(Destination) == 2) {
        asm volatile("subw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u16)src));
    } else if constexpr (sizeof(Destination) == 1) {
        asm volatile("subb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u8)src));
    } else {
        ASSERT_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszap(new_flags);
    return result;
}

template<typename Destination, typename Source>
static Destination op_add(SoftCPU& cpu, Destination& dest, const Source& src)
{
    Destination result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(Destination) == 4) {
        asm volatile("addl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u32)src));
    } else if constexpr (sizeof(Destination) == 2) {
        asm volatile("addw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u16)src));
    } else if constexpr (sizeof(Destination) == 1) {
        asm volatile("addb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u8)src));
    } else {
        ASSERT_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszap(new_flags);
    return result;
}

template<bool update_dest, typename Op>
void SoftCPU::generic_AL_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = al();
    auto src = insn.imm8();
    auto result = op(*this, dest, src);
    if (update_dest)
        set_al(result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_AX_imm16(Op op, const X86::Instruction& insn)
{
    auto dest = ax();
    auto src = insn.imm16();
    auto result = op(*this, dest, src);
    if (update_dest)
        set_ax(result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_EAX_imm32(Op op, const X86::Instruction& insn)
{
    auto dest = eax();
    auto src = insn.imm32();
    auto result = op(*this, dest, src);
    if (update_dest)
        set_eax(result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_RM16_imm16(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = insn.imm16();
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write16(*this, insn, result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_RM16_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = insn.imm8();
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write16(*this, insn, result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_RM16_reg16(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = gpr16(insn.reg16());
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write16(*this, insn, result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_RM32_imm32(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = insn.imm32();
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write32(*this, insn, result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_RM32_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = insn.imm8();
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write32(*this, insn, result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_RM32_reg32(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = gpr32(insn.reg32());
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write32(*this, insn, result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_RM8_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read8(*this, insn);
    auto src = insn.imm8();
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write8(*this, insn, result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_RM8_reg8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read8(*this, insn);
    auto src = gpr8(insn.reg8());
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write8(*this, insn, result);
}

template<bool update_dest, typename Op>
void SoftCPU::generic_reg16_RM16(Op op, const X86::Instruction& insn)
{
    auto dest = gpr16(insn.reg16());
    auto src = insn.modrm().read16(*this, insn);
    auto result = op(*this, dest, src);
    if (update_dest)
        gpr16(insn.reg16()) = result;
}

template<bool update_dest, typename Op>
void SoftCPU::generic_reg32_RM32(Op op, const X86::Instruction& insn)
{
    auto dest = gpr32(insn.reg32());
    auto src = insn.modrm().read32(*this, insn);
    auto result = op(*this, dest, src);
    if (update_dest)
        gpr32(insn.reg32()) = result;
}

template<bool update_dest, typename Op>
void SoftCPU::generic_reg8_RM8(Op op, const X86::Instruction& insn)
{
    auto dest = gpr8(insn.reg8());
    auto src = insn.modrm().read8(*this, insn);
    auto result = op(*this, dest, src);
    if (update_dest)
        gpr8(insn.reg8()) = result;
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

template<typename T>
static T op_inc(SoftCPU& cpu, T data)
{
    T result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("incl %%eax\n"
                     : "=a"(result)
                     : "a"(data));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("incw %%ax\n"
                     : "=a"(result)
                     : "a"(data));
    } else if constexpr (sizeof(T) == 1) {
        asm volatile("incb %%al\n"
                     : "=a"(result)
                     : "a"(data));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszap(new_flags);
    return result;
}

void SoftCPU::INC_RM16(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, op_inc(*this, insn.modrm().read16(*this, insn)));
}

void SoftCPU::INC_RM32(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, op_inc(*this, insn.modrm().read32(*this, insn)));
}

void SoftCPU::INC_RM8(const X86::Instruction& insn)
{
    insn.modrm().write8(*this, insn, op_inc(*this, insn.modrm().read8(*this, insn)));
}

void SoftCPU::INC_reg16(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = op_inc(*this, gpr16(insn.reg16()));
}

void SoftCPU::INC_reg32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = op_inc(*this, gpr32(insn.reg32()));
}

void SoftCPU::INSB(const X86::Instruction&) { TODO(); }
void SoftCPU::INSD(const X86::Instruction&) { TODO(); }
void SoftCPU::INSW(const X86::Instruction&) { TODO(); }
void SoftCPU::INT3(const X86::Instruction&) { TODO(); }
void SoftCPU::INTO(const X86::Instruction&) { TODO(); }

void SoftCPU::INT_imm8(const X86::Instruction& insn)
{
    ASSERT(insn.imm8() == 0x82);
    set_eax(m_emulator.virt_syscall(eax(), edx(), ecx(), ebx()));
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

void SoftCPU::MOV_RM16_imm16(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, insn.imm16());
}

void SoftCPU::MOV_RM16_reg16(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, gpr16(insn.reg16()));
}

void SoftCPU::MOV_RM16_seg(const X86::Instruction&) { TODO(); }

void SoftCPU::MOV_RM32_imm32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = insn.imm32();
}

void SoftCPU::MOV_RM32_reg32(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, gpr32(insn.reg32()));
}

void SoftCPU::MOV_RM8_imm8(const X86::Instruction& insn)
{
    insn.modrm().write8(*this, insn, insn.imm8());
}

void SoftCPU::MOV_RM8_reg8(const X86::Instruction& insn)
{
    insn.modrm().write8(*this, insn, insn.modrm().read8(*this, insn));
}

void SoftCPU::MOV_moff16_AX(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_moff32_EAX(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_moff8_AL(const X86::Instruction&) { TODO(); }

void SoftCPU::MOV_reg16_RM16(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = insn.modrm().read16(*this, insn);
}

void SoftCPU::MOV_reg16_imm16(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = insn.imm16();
}

void SoftCPU::MOV_reg32_CR(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_reg32_DR(const X86::Instruction&) { TODO(); }

void SoftCPU::MOV_reg32_RM32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = insn.modrm().read32(*this, insn);
}

void SoftCPU::MOV_reg32_imm32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = insn.imm32();
}

void SoftCPU::MOV_reg8_RM8(const X86::Instruction& insn)
{
    gpr8(insn.reg8()) = insn.modrm().read8(*this, insn);
}

void SoftCPU::MOV_reg8_imm8(const X86::Instruction& insn)
{
    gpr8(insn.reg8()) = insn.imm8();
}

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

void SoftCPU::POP_reg32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = pop32();
}

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

void SoftCPU::PUSH_reg32(const X86::Instruction& insn)
{
    push32(gpr32(insn.reg32()));
}

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

template<typename T>
static T op_sar(SoftCPU& cpu, T data, u8 steps)
{
    if (steps == 0)
        return data;

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4)
        asm volatile("sarl %%cl, %%eax\n" ::"a"(data), "c"(steps));
    else if constexpr (sizeof(T) == 2)
        asm volatile("sarw %%cl, %%ax\n" ::"a"(data), "c"(steps));
    else if constexpr (sizeof(T) == 1)
        asm volatile("sarb %%cl, %%al\n" ::"a"(data), "c"(steps));

    asm volatile(
        "mov %%eax, %%ebx\n"
        : "=b"(result));
    asm volatile(
        "pushf\n"
        "pop %%eax"
        : "=a"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    return result;
}

void SoftCPU::SAR_RM16_1(const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op_sar(*this, data, 1));
}

void SoftCPU::SAR_RM16_CL(const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op_sar(*this, data, cl()));
}

void SoftCPU::SAR_RM16_imm8(const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op_sar(*this, data, insn.imm8()));
}

void SoftCPU::SAR_RM32_1(const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op_sar(*this, data, 1));
}

void SoftCPU::SAR_RM32_CL(const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op_sar(*this, data, cl()));
}

void SoftCPU::SAR_RM32_imm8(const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op_sar(*this, data, insn.imm8()));
}

void SoftCPU::SAR_RM8_1(const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op_sar(*this, data, 1));
}

void SoftCPU::SAR_RM8_CL(const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op_sar(*this, data, cl()));
}

void SoftCPU::SAR_RM8_imm8(const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op_sar(*this, data, insn.imm8()));
}

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

#define DEFINE_GENERIC_INSN_HANDLERS(mnemonic, op, update_dest)                                                                \
    void SoftCPU::mnemonic##_AL_imm8(const X86::Instruction& insn) { generic_AL_imm8<update_dest>(op<u8, u8>, insn); }         \
    void SoftCPU::mnemonic##_AX_imm16(const X86::Instruction& insn) { generic_AX_imm16<update_dest>(op<u16, u16>, insn); }     \
    void SoftCPU::mnemonic##_EAX_imm32(const X86::Instruction& insn) { generic_EAX_imm32<update_dest>(op<u32, u32>, insn); }   \
    void SoftCPU::mnemonic##_RM16_imm16(const X86::Instruction& insn) { generic_RM16_imm16<update_dest>(op<u16, u16>, insn); } \
    void SoftCPU::mnemonic##_RM16_imm8(const X86::Instruction& insn) { generic_RM16_imm8<update_dest>(op<u16, u8>, insn); }    \
    void SoftCPU::mnemonic##_RM16_reg16(const X86::Instruction& insn) { generic_RM16_reg16<update_dest>(op<u16, u16>, insn); } \
    void SoftCPU::mnemonic##_RM32_imm32(const X86::Instruction& insn) { generic_RM32_imm32<update_dest>(op<u32, u32>, insn); } \
    void SoftCPU::mnemonic##_RM32_imm8(const X86::Instruction& insn) { generic_RM32_imm8<update_dest>(op<u32, u8>, insn); }    \
    void SoftCPU::mnemonic##_RM32_reg32(const X86::Instruction& insn) { generic_RM32_reg32<update_dest>(op<u32, u32>, insn); } \
    void SoftCPU::mnemonic##_RM8_imm8(const X86::Instruction& insn) { generic_RM8_imm8<update_dest>(op<u8, u8>, insn); }       \
    void SoftCPU::mnemonic##_RM8_reg8(const X86::Instruction& insn) { generic_RM8_reg8<update_dest>(op<u8, u8>, insn); }       \
    void SoftCPU::mnemonic##_reg16_RM16(const X86::Instruction& insn) { generic_reg16_RM16<update_dest>(op<u16, u16>, insn); } \
    void SoftCPU::mnemonic##_reg32_RM32(const X86::Instruction& insn) { generic_reg32_RM32<update_dest>(op<u32, u32>, insn); } \
    void SoftCPU::mnemonic##_reg8_RM8(const X86::Instruction& insn) { generic_reg8_RM8<update_dest>(op<u8, u8>, insn); }

DEFINE_GENERIC_INSN_HANDLERS(XOR, op_xor, true)
DEFINE_GENERIC_INSN_HANDLERS(ADD, op_add, true)
DEFINE_GENERIC_INSN_HANDLERS(SUB, op_sub, true)
DEFINE_GENERIC_INSN_HANDLERS(CMP, op_sub, false)

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
