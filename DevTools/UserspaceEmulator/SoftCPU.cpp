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

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

//#define MEMORY_DEBUG

namespace UserspaceEmulator {

template<typename T, typename U>
inline constexpr T sign_extended_to(U value)
{
    if (!(value & X86::TypeTrivia<U>::sign_bit))
        return value;
    return (X86::TypeTrivia<T>::mask & ~X86::TypeTrivia<U>::mask) | value;
}

SoftCPU::SoftCPU(Emulator& emulator)
    : m_emulator(emulator)
{
    memset(m_gpr, 0, sizeof(m_gpr));

    m_segment[(int)X86::SegmentRegister::CS] = 0x18;
    m_segment[(int)X86::SegmentRegister::DS] = 0x20;
    m_segment[(int)X86::SegmentRegister::ES] = 0x20;
    m_segment[(int)X86::SegmentRegister::SS] = 0x20;
    m_segment[(int)X86::SegmentRegister::GS] = 0x28;
}

void SoftCPU::dump() const
{
    printf("eax=%08x ebx=%08x ecx=%08x edx=%08x ", eax(), ebx(), ecx(), edx());
    printf("ebp=%08x esp=%08x esi=%08x edi=%08x ", ebp(), esp(), esi(), edi());
    printf("o=%u s=%u z=%u a=%u p=%u c=%u\n", of(), sf(), zf(), af(), pf(), cf());
}

void SoftCPU::did_receive_secret_data()
{
    if (m_secret_data[0] == 1) {
        if (auto* tracer = m_emulator.malloc_tracer())
            tracer->target_did_malloc({}, m_secret_data[2], m_secret_data[1]);
    } else if (m_secret_data[0] == 2) {
        if (auto* tracer = m_emulator.malloc_tracer())
            tracer->target_did_free({}, m_secret_data[1]);
    } else {
        ASSERT_NOT_REACHED();
    }
}

void SoftCPU::update_code_cache()
{
    auto* region = m_emulator.mmu().find_region({ cs(), eip() });
    ASSERT(region);

    m_cached_code_ptr = region->cacheable_ptr(eip() - region->base());
    m_cached_code_end = region->cacheable_ptr(region->size());
}

u8 SoftCPU::read_memory8(X86::LogicalAddress address)
{
    ASSERT(address.selector() == 0x18 || address.selector() == 0x20 || address.selector() == 0x28);
    auto value = m_emulator.mmu().read8(address);
#ifdef MEMORY_DEBUG
    printf("\033[36;1mread_memory8: @%08x:%08x -> %02x\033[0m\n", address.selector(), address.offset(), value);
#endif
    return value;
}

u16 SoftCPU::read_memory16(X86::LogicalAddress address)
{
    ASSERT(address.selector() == 0x18 || address.selector() == 0x20 || address.selector() == 0x28);
    auto value = m_emulator.mmu().read16(address);
#ifdef MEMORY_DEBUG
    printf("\033[36;1mread_memory16: @%04x:%08x -> %04x\033[0m\n", address.selector(), address.offset(), value);
#endif
    return value;
}

u32 SoftCPU::read_memory32(X86::LogicalAddress address)
{
    ASSERT(address.selector() == 0x18 || address.selector() == 0x20 || address.selector() == 0x28);
    auto value = m_emulator.mmu().read32(address);
#ifdef MEMORY_DEBUG
    printf("\033[36;1mread_memory32: @%04x:%08x -> %08x\033[0m\n", address.selector(), address.offset(), value);
#endif
    return value;
}

void SoftCPU::write_memory8(X86::LogicalAddress address, u8 value)
{
    ASSERT(address.selector() == 0x20 || address.selector() == 0x28);
#ifdef MEMORY_DEBUG
    printf("\033[35;1mwrite_memory8: @%04x:%08x <- %02x\033[0m\n", address.selector(), address.offset(), value);
#endif
    m_emulator.mmu().write8(address, value);
}

void SoftCPU::write_memory16(X86::LogicalAddress address, u16 value)
{
    ASSERT(address.selector() == 0x20 || address.selector() == 0x28);
#ifdef MEMORY_DEBUG
    printf("\033[35;1mwrite_memory16: @%04x:%08x <- %04x\033[0m\n", address.selector(), address.offset(), value);
#endif
    m_emulator.mmu().write16(address, value);
}

void SoftCPU::write_memory32(X86::LogicalAddress address, u32 value)
{
    ASSERT(address.selector() == 0x20 || address.selector() == 0x28);
#ifdef MEMORY_DEBUG
    printf("\033[35;1mwrite_memory32: @%04x:%08x <- %08x\033[0m\n", address.selector(), address.offset(), value);
#endif
    m_emulator.mmu().write32(address, value);
}

void SoftCPU::push_string(const StringView& string)
{
    size_t space_to_allocate = round_up_to_power_of_two(string.length() + 1, 16);
    set_esp(esp() - space_to_allocate);
    m_emulator.mmu().copy_to_vm(esp(), string.characters_without_null_termination(), string.length());
    m_emulator.mmu().write8({ 0x20, esp() + string.length() }, '\0');
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

template<bool check_zf, typename Callback>
void SoftCPU::do_once_or_repeat(const X86::Instruction& insn, Callback callback)
{
    if (!insn.has_rep_prefix())
        return callback();

    if (insn.has_address_size_override_prefix()) {
        while (cx()) {
            callback();
            set_cx(cx() - 1);
            if constexpr (check_zf) {
                if (insn.rep_prefix() == X86::Prefix::REPZ && !zf())
                    break;
                if (insn.rep_prefix() == X86::Prefix::REPNZ && zf())
                    break;
            }
        }
        return;
    }

    while (ecx()) {
        callback();
        set_ecx(ecx() - 1);
        if constexpr (check_zf) {
            if (insn.rep_prefix() == X86::Prefix::REPZ && !zf())
                break;
            if (insn.rep_prefix() == X86::Prefix::REPNZ && zf())
                break;
        }
    }
}

template<typename T>
ALWAYS_INLINE static T op_inc(SoftCPU& cpu, T data)
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

template<typename T>
ALWAYS_INLINE static T op_dec(SoftCPU& cpu, T data)
{
    T result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("decl %%eax\n"
                     : "=a"(result)
                     : "a"(data));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("decw %%ax\n"
                     : "=a"(result)
                     : "a"(data));
    } else if constexpr (sizeof(T) == 1) {
        asm volatile("decb %%al\n"
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

template<typename T>
ALWAYS_INLINE static T op_xor(SoftCPU& cpu, const T& dest, const T& src)
{
    T result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("xorl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u32)src));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("xor %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u16)src));
    } else if constexpr (sizeof(T) == 1) {
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

template<typename T>
ALWAYS_INLINE static T op_or(SoftCPU& cpu, const T& dest, const T& src)
{
    T result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("orl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u32)src));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("or %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u16)src));
    } else if constexpr (sizeof(T) == 1) {
        asm volatile("orb %%cl, %%al\n"
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

template<typename T>
ALWAYS_INLINE static T op_sub(SoftCPU& cpu, const T& dest, const T& src)
{
    T result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("subl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u32)src));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("subw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u16)src));
    } else if constexpr (sizeof(T) == 1) {
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

    cpu.set_flags_oszapc(new_flags);
    return result;
}

template<typename T, bool cf>
ALWAYS_INLINE static T op_sbb_impl(SoftCPU& cpu, const T& dest, const T& src)
{
    T result = 0;
    u32 new_flags = 0;

    if constexpr (cf)
        asm volatile("stc");
    else
        asm volatile("clc");

    if constexpr (sizeof(T) == 4) {
        asm volatile("sbbl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u32)src));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("sbbw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u16)src));
    } else if constexpr (sizeof(T) == 1) {
        asm volatile("sbbb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u8)src));
    } else {
        ASSERT_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    return result;
}

template<typename T>
ALWAYS_INLINE static T op_sbb(SoftCPU& cpu, T& dest, const T& src)
{
    if (cpu.cf())
        return op_sbb_impl<T, true>(cpu, dest, src);
    return op_sbb_impl<T, false>(cpu, dest, src);
}

template<typename T>
ALWAYS_INLINE static T op_add(SoftCPU& cpu, T& dest, const T& src)
{
    T result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("addl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u32)src));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("addw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u16)src));
    } else if constexpr (sizeof(T) == 1) {
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

    cpu.set_flags_oszapc(new_flags);
    return result;
}

template<typename T, bool cf>
ALWAYS_INLINE static T op_adc_impl(SoftCPU& cpu, T& dest, const T& src)
{
    T result = 0;
    u32 new_flags = 0;

    if constexpr (cf)
        asm volatile("stc");
    else
        asm volatile("clc");

    if constexpr (sizeof(T) == 4) {
        asm volatile("adcl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u32)src));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("adcw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u16)src));
    } else if constexpr (sizeof(T) == 1) {
        asm volatile("adcb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u8)src));
    } else {
        ASSERT_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    return result;
}

template<typename T>
ALWAYS_INLINE static T op_adc(SoftCPU& cpu, T& dest, const T& src)
{
    if (cpu.cf())
        return op_adc_impl<T, true>(cpu, dest, src);
    return op_adc_impl<T, false>(cpu, dest, src);
}

template<typename T>
ALWAYS_INLINE static T op_and(SoftCPU& cpu, const T& dest, const T& src)
{
    T result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("andl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u32)src));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("andw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((u16)src));
    } else if constexpr (sizeof(T) == 1) {
        asm volatile("andb %%cl, %%al\n"
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

template<typename T>
ALWAYS_INLINE static T op_imul(SoftCPU& cpu, const T& dest, const T& src)
{
    T result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("imull %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((i32)src));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("imulw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest), "c"((i16)src));
    } else {
        ASSERT_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    return result;
}

template<typename T>
ALWAYS_INLINE static T op_shr(SoftCPU& cpu, T data, u8 steps)
{
    if (steps == 0)
        return data;

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("shrl %%cl, %%eax\n"
                     : "=a"(result)
                     : "a"(data), "c"(steps));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("shrw %%cl, %%ax\n"
                     : "=a"(result)
                     : "a"(data), "c"(steps));
    } else if constexpr (sizeof(T) == 1) {
        asm volatile("shrb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(data), "c"(steps));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    return result;
}

template<typename T>
ALWAYS_INLINE static T op_shl(SoftCPU& cpu, T data, u8 steps)
{
    if (steps == 0)
        return data;

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("shll %%cl, %%eax\n"
                     : "=a"(result)
                     : "a"(data), "c"(steps));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("shlw %%cl, %%ax\n"
                     : "=a"(result)
                     : "a"(data), "c"(steps));
    } else if constexpr (sizeof(T) == 1) {
        asm volatile("shlb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(data), "c"(steps));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    return result;
}

template<typename T>
ALWAYS_INLINE static T op_shrd(SoftCPU& cpu, T data, T extra_bits, u8 steps)
{
    if (steps == 0)
        return data;

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("shrd %%cl, %%edx, %%eax\n"
                     : "=a"(result)
                     : "a"(data), "d"(extra_bits), "c"(steps));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("shrb %%cl, %%dx, %%ax\n"
                     : "=a"(result)
                     : "a"(data), "d"(extra_bits), "c"(steps));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    return result;
}

template<typename T>
ALWAYS_INLINE static T op_shld(SoftCPU& cpu, T data, T extra_bits, u8 steps)
{
    if (steps == 0)
        return data;

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("shld %%cl, %%edx, %%eax\n"
                     : "=a"(result)
                     : "a"(data), "d"(extra_bits), "c"(steps));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("shlb %%cl, %%dx, %%ax\n"
                     : "=a"(result)
                     : "a"(data), "d"(extra_bits), "c"(steps));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    return result;
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_AL_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = al();
    auto src = insn.imm8();
    auto result = op(*this, dest, src);
    if (update_dest)
        set_al(result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_AX_imm16(Op op, const X86::Instruction& insn)
{
    auto dest = ax();
    auto src = insn.imm16();
    auto result = op(*this, dest, src);
    if (update_dest)
        set_ax(result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_EAX_imm32(Op op, const X86::Instruction& insn)
{
    auto dest = eax();
    auto src = insn.imm32();
    auto result = op(*this, dest, src);
    if (update_dest)
        set_eax(result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM16_imm16(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = insn.imm16();
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write16(*this, insn, result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM16_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = sign_extended_to<u16>(insn.imm8());
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write16(*this, insn, result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM16_reg16(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = gpr16(insn.reg16());
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write16(*this, insn, result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM32_imm32(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = insn.imm32();
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write32(*this, insn, result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM32_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = sign_extended_to<u32>(insn.imm8());
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write32(*this, insn, result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM32_reg32(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = gpr32(insn.reg32());
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write32(*this, insn, result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM8_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read8(*this, insn);
    auto src = insn.imm8();
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write8(*this, insn, result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM8_reg8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read8(*this, insn);
    auto src = gpr8(insn.reg8());
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write8(*this, insn, result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_reg16_RM16(Op op, const X86::Instruction& insn)
{
    auto dest = gpr16(insn.reg16());
    auto src = insn.modrm().read16(*this, insn);
    auto result = op(*this, dest, src);
    if (update_dest)
        gpr16(insn.reg16()) = result;
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_reg32_RM32(Op op, const X86::Instruction& insn)
{
    auto dest = gpr32(insn.reg32());
    auto src = insn.modrm().read32(*this, insn);
    auto result = op(*this, dest, src);
    if (update_dest)
        gpr32(insn.reg32()) = result;
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_reg8_RM8(Op op, const X86::Instruction& insn)
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
void SoftCPU::ARPL(const X86::Instruction&) { TODO(); }
void SoftCPU::BOUND(const X86::Instruction&) { TODO(); }
void SoftCPU::BSF_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::BSF_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::BSR_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::BSR_reg32_RM32(const X86::Instruction&) { TODO(); }

void SoftCPU::BSWAP_reg32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = __builtin_bswap32(gpr32(insn.reg32()));
}

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

void SoftCPU::CALL_RM32(const X86::Instruction& insn)
{
    push32(eip());
    set_eip(insn.modrm().read32(*this, insn));
}

void SoftCPU::CALL_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::CALL_imm16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::CALL_imm16_imm32(const X86::Instruction&) { TODO(); }

void SoftCPU::CALL_imm32(const X86::Instruction& insn)
{
    push32(eip());
    set_eip(eip() + (i32)insn.imm32());
}

void SoftCPU::CBW(const X86::Instruction&)
{
    set_ah((al() & 0x80) ? 0xff : 0x00);
}

void SoftCPU::CDQ(const X86::Instruction&)
{
    if (eax() & 0x80000000)
        set_edx(0xffffffff);
    else
        set_edx(0x00000000);
}

void SoftCPU::CLC(const X86::Instruction&)
{
    set_cf(false);
}

void SoftCPU::CLD(const X86::Instruction&)
{
    set_df(false);
}

void SoftCPU::CLI(const X86::Instruction&) { TODO(); }
void SoftCPU::CLTS(const X86::Instruction&) { TODO(); }
void SoftCPU::CMC(const X86::Instruction&) { TODO(); }

void SoftCPU::CMOVcc_reg16_RM16(const X86::Instruction& insn)
{
    if (evaluate_condition(insn.cc()))
        gpr16(insn.reg16()) = insn.modrm().read16(*this, insn);
}

void SoftCPU::CMOVcc_reg32_RM32(const X86::Instruction& insn)
{
    if (evaluate_condition(insn.cc()))
        gpr32(insn.reg32()) = insn.modrm().read32(*this, insn);
}

void SoftCPU::CMPSB(const X86::Instruction&) { TODO(); }
void SoftCPU::CMPSD(const X86::Instruction&) { TODO(); }
void SoftCPU::CMPSW(const X86::Instruction&) { TODO(); }

void SoftCPU::CMPXCHG_RM16_reg16(const X86::Instruction& insn)
{
    auto current = insn.modrm().read16(*this, insn);
    if (current == eax()) {
        set_zf(true);
        insn.modrm().write16(*this, insn, gpr16(insn.reg16()));
    } else {
        set_zf(false);
        set_eax(current);
    }
}

void SoftCPU::CMPXCHG_RM32_reg32(const X86::Instruction& insn)
{
    auto current = insn.modrm().read32(*this, insn);
    if (current == eax()) {
        set_zf(true);
        insn.modrm().write32(*this, insn, gpr32(insn.reg32()));
    } else {
        set_zf(false);
        set_eax(current);
    }
}

void SoftCPU::CMPXCHG_RM8_reg8(const X86::Instruction& insn)
{
    auto current = insn.modrm().read8(*this, insn);
    if (current == eax()) {
        set_zf(true);
        insn.modrm().write8(*this, insn, gpr8(insn.reg8()));
    } else {
        set_zf(false);
        set_eax(current);
    }
}

void SoftCPU::CPUID(const X86::Instruction&) { TODO(); }

void SoftCPU::CWD(const X86::Instruction&)
{
    set_dx((ax() & 0x8000) ? 0xffff : 0x0000);
}

void SoftCPU::CWDE(const X86::Instruction&)
{
    set_eax(sign_extended_to<u32>(ax()));
}

void SoftCPU::DAA(const X86::Instruction&) { TODO(); }
void SoftCPU::DAS(const X86::Instruction&) { TODO(); }

void SoftCPU::DEC_RM16(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, op_dec(*this, insn.modrm().read16(*this, insn)));
}

void SoftCPU::DEC_RM32(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, op_dec(*this, insn.modrm().read32(*this, insn)));
}

void SoftCPU::DEC_RM8(const X86::Instruction& insn)
{
    insn.modrm().write8(*this, insn, op_dec(*this, insn.modrm().read8(*this, insn)));
}

void SoftCPU::DEC_reg16(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = op_dec(*this, gpr16(insn.reg16()));
}

void SoftCPU::DEC_reg32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = op_dec(*this, gpr32(insn.reg32()));
}

void SoftCPU::DIV_RM16(const X86::Instruction&) { TODO(); }

void SoftCPU::DIV_RM32(const X86::Instruction& insn)
{
    auto divisor = insn.modrm().read32(*this, insn);
    if (divisor == 0) {
        warn() << "Divide by zero";
        TODO();
    }
    u64 dividend = ((u64)edx() << 32) | eax();
    auto result = dividend / divisor;
    if (result > NumericLimits<u32>::max()) {
        warn() << "Divide overflow";
        TODO();
    }

    set_eax(result);
    set_edx(dividend % divisor);
}

void SoftCPU::DIV_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::ENTER16(const X86::Instruction&) { TODO(); }
void SoftCPU::ENTER32(const X86::Instruction&) { TODO(); }

void SoftCPU::ESCAPE(const X86::Instruction&)
{
    dbg() << "FIXME: x87 floating-point support";
    m_emulator.dump_backtrace();
    TODO();
}

void SoftCPU::HLT(const X86::Instruction&) { TODO(); }
void SoftCPU::IDIV_RM16(const X86::Instruction&) { TODO(); }

void SoftCPU::IDIV_RM32(const X86::Instruction& insn)
{
    auto divisor = insn.modrm().read32(*this, insn);
    if (divisor == 0) {
        warn() << "Divide by zero";
        TODO();
    }
    i64 dividend = ((i64)edx() << 32) | eax();
    auto result = dividend / divisor;
    if (result > NumericLimits<i32>::max()) {
        warn() << "Divide overflow";
        TODO();
    }

    set_eax(result);
    set_edx(dividend % divisor);
}

void SoftCPU::IDIV_RM8(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::IMUL_RM8(const X86::Instruction&) { TODO(); }

void SoftCPU::IMUL_reg16_RM16(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = op_imul<i16>(*this, gpr16(insn.reg16()), insn.modrm().read16(*this, insn));
}

void SoftCPU::IMUL_reg16_RM16_imm16(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = op_imul<i16>(*this, insn.modrm().read16(*this, insn), insn.imm16());
}

void SoftCPU::IMUL_reg16_RM16_imm8(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = op_imul<i16>(*this, insn.modrm().read16(*this, insn), sign_extended_to<i16>(insn.imm8()));
}

void SoftCPU::IMUL_reg32_RM32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = op_imul<i32>(*this, gpr32(insn.reg32()), insn.modrm().read32(*this, insn));
}

void SoftCPU::IMUL_reg32_RM32_imm32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = op_imul<i32>(*this, insn.modrm().read32(*this, insn), insn.imm32());
}

void SoftCPU::IMUL_reg32_RM32_imm8(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = op_imul<i32>(*this, insn.modrm().read32(*this, insn), sign_extended_to<i32>(insn.imm8()));
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

void SoftCPU::JMP_RM32(const X86::Instruction& insn)
{
    set_eip(insn.modrm().read32(*this, insn));
}

void SoftCPU::JMP_imm16(const X86::Instruction& insn)
{
    set_eip(eip() + (i16)insn.imm16());
}

void SoftCPU::JMP_imm16_imm16(const X86::Instruction&) { TODO(); }
void SoftCPU::JMP_imm16_imm32(const X86::Instruction&) { TODO(); }

void SoftCPU::JMP_imm32(const X86::Instruction& insn)
{
    set_eip(eip() + (i32)insn.imm32());
}

void SoftCPU::JMP_short_imm8(const X86::Instruction& insn)
{
    set_eip(eip() + (i8)insn.imm8());
}

void SoftCPU::Jcc_NEAR_imm(const X86::Instruction& insn)
{
    if (evaluate_condition(insn.cc()))
        set_eip(eip() + (i32)insn.imm32());
}

void SoftCPU::Jcc_imm8(const X86::Instruction& insn)
{
    if (evaluate_condition(insn.cc()))
        set_eip(eip() + (i8)insn.imm8());
}

void SoftCPU::LAHF(const X86::Instruction&) { TODO(); }
void SoftCPU::LAR_reg16_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::LAR_reg32_RM32(const X86::Instruction&) { TODO(); }
void SoftCPU::LDS_reg16_mem16(const X86::Instruction&) { TODO(); }
void SoftCPU::LDS_reg32_mem32(const X86::Instruction&) { TODO(); }
void SoftCPU::LEAVE16(const X86::Instruction&) { TODO(); }

void SoftCPU::LEAVE32(const X86::Instruction&)
{
    u32 new_ebp = read_memory32({ ss(), ebp() });
    set_esp(ebp() + 4);
    set_ebp(new_ebp);
}

void SoftCPU::LEA_reg16_mem16(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = insn.modrm().resolve(*this, insn).offset();
}

void SoftCPU::LEA_reg32_mem32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = insn.modrm().resolve(*this, insn).offset();
}

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

void SoftCPU::MOVSB(const X86::Instruction& insn)
{
    auto src_segment = segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS));
    if (insn.has_address_size_override_prefix()) {
        do_once_or_repeat<false>(insn, [&] {
            auto src = read_memory8({ src_segment, si() });
            write_memory8({ es(), di() }, src);
            set_di(di() + (df() ? -1 : 1));
            set_si(si() + (df() ? -1 : 1));
        });
    } else {
        do_once_or_repeat<false>(insn, [&] {
            auto src = read_memory8({ src_segment, esi() });
            write_memory8({ es(), edi() }, src);
            set_edi(edi() + (df() ? -1 : 1));
            set_esi(esi() + (df() ? -1 : 1));
        });
    }
}

void SoftCPU::MOVSD(const X86::Instruction& insn)
{
    auto src_segment = segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS));
    if (insn.has_address_size_override_prefix()) {
        do_once_or_repeat<false>(insn, [&] {
            auto src = read_memory32({ src_segment, si() });
            write_memory32({ es(), di() }, src);
            set_di(di() + (df() ? -4 : 4));
            set_si(si() + (df() ? -4 : 4));
        });
    } else {
        do_once_or_repeat<false>(insn, [&] {
            auto src = read_memory32({ src_segment, esi() });
            write_memory32({ es(), edi() }, src);
            set_edi(edi() + (df() ? -4 : 4));
            set_esi(esi() + (df() ? -4 : 4));
        });
    }
}

void SoftCPU::MOVSW(const X86::Instruction& insn)
{
    auto src_segment = segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS));
    if (insn.has_address_size_override_prefix()) {
        do_once_or_repeat<false>(insn, [&] {
            auto src = read_memory16({ src_segment, si() });
            write_memory16({ es(), di() }, src);
            set_di(di() + (df() ? -2 : 2));
            set_si(si() + (df() ? -2 : 2));
        });
    } else {
        do_once_or_repeat<false>(insn, [&] {
            auto src = read_memory16({ src_segment, esi() });
            write_memory16({ es(), edi() }, src);
            set_edi(edi() + (df() ? -2 : 2));
            set_esi(esi() + (df() ? -2 : 2));
        });
    }
}
void SoftCPU::MOVSX_reg16_RM8(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = sign_extended_to<u16>(insn.modrm().read8(*this, insn));
}

void SoftCPU::MOVSX_reg32_RM16(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = sign_extended_to<u32>(insn.modrm().read16(*this, insn));
}

void SoftCPU::MOVSX_reg32_RM8(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = sign_extended_to<u32>(insn.modrm().read8(*this, insn));
}

void SoftCPU::MOVZX_reg16_RM8(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = insn.modrm().read8(*this, insn);
}

void SoftCPU::MOVZX_reg32_RM16(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = insn.modrm().read16(*this, insn);
}

void SoftCPU::MOVZX_reg32_RM8(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = insn.modrm().read8(*this, insn);
}

void SoftCPU::MOV_AL_moff8(const X86::Instruction& insn)
{
    set_al(read_memory8({ segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS)), insn.imm_address() }));
}

void SoftCPU::MOV_AX_moff16(const X86::Instruction& insn)
{
    set_ax(read_memory16({ segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS)), insn.imm_address() }));
}

void SoftCPU::MOV_CR_reg32(const X86::Instruction&) { TODO(); }
void SoftCPU::MOV_DR_reg32(const X86::Instruction&) { TODO(); }

void SoftCPU::MOV_EAX_moff32(const X86::Instruction& insn)
{
    set_eax(read_memory32({ segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS)), insn.imm_address() }));
}

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
    insn.modrm().write32(*this, insn, insn.imm32());
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
    insn.modrm().write8(*this, insn, gpr8(insn.reg8()));
}

void SoftCPU::MOV_moff16_AX(const X86::Instruction& insn)
{
    write_memory16({ segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS)), insn.imm_address() }, ax());
}

void SoftCPU::MOV_moff32_EAX(const X86::Instruction& insn)
{
    write_memory32({ segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS)), insn.imm_address() }, eax());
}

void SoftCPU::MOV_moff8_AL(const X86::Instruction& insn)
{
    write_memory8({ segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS)), insn.imm_address() }, al());
}

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

void SoftCPU::MUL_RM32(const X86::Instruction& insn)
{
    u64 result = (u64)eax() * (u64)insn.modrm().read32(*this, insn);
    set_eax(result & 0xffffffff);
    set_edx(result >> 32);

    set_cf(edx() != 0);
    set_of(edx() != 0);
}

void SoftCPU::MUL_RM8(const X86::Instruction&) { TODO(); }

void SoftCPU::NEG_RM16(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, op_sub<u16>(*this, 0, insn.modrm().read16(*this, insn)));
}

void SoftCPU::NEG_RM32(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, op_sub<u32>(*this, 0, insn.modrm().read32(*this, insn)));
}

void SoftCPU::NEG_RM8(const X86::Instruction& insn)
{
    insn.modrm().write8(*this, insn, op_sub<u8>(*this, 0, insn.modrm().read8(*this, insn)));
}

void SoftCPU::NOP(const X86::Instruction&)
{
}

void SoftCPU::NOT_RM16(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, ~insn.modrm().read16(*this, insn));
}

void SoftCPU::NOT_RM32(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, ~insn.modrm().read32(*this, insn));
}

void SoftCPU::NOT_RM8(const X86::Instruction& insn)
{
    insn.modrm().write8(*this, insn, ~insn.modrm().read8(*this, insn));
}

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

void SoftCPU::POPFD(const X86::Instruction&)
{
    m_eflags &= ~0x00fcffff;
    m_eflags |= pop32() & 0x00fcffff;
}

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

void SoftCPU::PUSHFD(const X86::Instruction&)
{
    push32(m_eflags & 0x00fcffff);
}

void SoftCPU::PUSH_CS(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_DS(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_ES(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_FS(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_GS(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_RM16(const X86::Instruction&) { TODO(); }

void SoftCPU::PUSH_RM32(const X86::Instruction& insn)
{
    push32(insn.modrm().read32(*this, insn));
}

void SoftCPU::PUSH_SP_8086_80186(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_SS(const X86::Instruction&) { TODO(); }
void SoftCPU::PUSH_imm16(const X86::Instruction&) { TODO(); }

void SoftCPU::PUSH_imm32(const X86::Instruction& insn)
{
    push32(insn.imm32());
}

void SoftCPU::PUSH_imm8(const X86::Instruction& insn)
{
    ASSERT(!insn.has_operand_size_override_prefix());
    push32(sign_extended_to<i32>(insn.imm8()));
}

void SoftCPU::PUSH_reg16(const X86::Instruction&) { TODO(); }

void SoftCPU::PUSH_reg32(const X86::Instruction& insn)
{
    push32(gpr32(insn.reg32()));

    if (m_secret_handshake_state == 2) {
        m_secret_data[0] = gpr32(insn.reg32());
        ++m_secret_handshake_state;
    } else if (m_secret_handshake_state == 3) {
        m_secret_data[1] = gpr32(insn.reg32());
        ++m_secret_handshake_state;
    } else if (m_secret_handshake_state == 4) {
        m_secret_data[2] = gpr32(insn.reg32());
        m_secret_handshake_state = 0;
        did_receive_secret_data();
    }
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

void SoftCPU::RET(const X86::Instruction& insn)
{
    ASSERT(!insn.has_operand_size_override_prefix());
    set_eip(pop32());
}

void SoftCPU::RETF(const X86::Instruction&) { TODO(); }
void SoftCPU::RETF_imm16(const X86::Instruction&) { TODO(); }

void SoftCPU::RET_imm16(const X86::Instruction& insn)
{
    ASSERT(!insn.has_operand_size_override_prefix());
    set_eip(pop32());
    set_esp(esp() + insn.imm16());
}

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

void SoftCPU::SALC(const X86::Instruction&)
{
    set_al(cf() ? 0x01 : 0x00);

    if (m_secret_handshake_state < 2)
        ++m_secret_handshake_state;
    else
        m_secret_handshake_state = 0;
}

template<typename T>
static T op_sar(SoftCPU& cpu, T data, u8 steps)
{
    if (steps == 0)
        return data;

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(T) == 4) {
        asm volatile("sarl %%cl, %%eax\n"
                     : "=a"(result)
                     : "a"(data), "c"(steps));
    } else if constexpr (sizeof(T) == 2) {
        asm volatile("sarw %%cl, %%ax\n"
                     : "=a"(result)
                     : "a"(data), "c"(steps));
    } else if constexpr (sizeof(T) == 1) {
        asm volatile("sarb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(data), "c"(steps));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

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

void SoftCPU::SCASB(const X86::Instruction&) { TODO(); }
void SoftCPU::SCASD(const X86::Instruction&) { TODO(); }
void SoftCPU::SCASW(const X86::Instruction&) { TODO(); }

void SoftCPU::SETcc_RM8(const X86::Instruction& insn)
{
    insn.modrm().write8(*this, insn, evaluate_condition(insn.cc()));
}

void SoftCPU::SGDT(const X86::Instruction&) { TODO(); }
void SoftCPU::SHLD_RM16_reg16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHLD_RM16_reg16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHLD_RM32_reg32_CL(const X86::Instruction&) { TODO(); }

void SoftCPU::SHLD_RM32_reg32_imm8(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, op_shld(*this, gpr32(insn.reg32()), insn.modrm().read32(*this, insn), insn.imm8()));
}

void SoftCPU::SHL_RM16_1(const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op_shl(*this, data, 1));
}

void SoftCPU::SHL_RM16_CL(const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op_shl(*this, data, cl()));
}

void SoftCPU::SHL_RM16_imm8(const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op_shl(*this, data, insn.imm8()));
}

void SoftCPU::SHL_RM32_1(const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op_shl(*this, data, 1));
}

void SoftCPU::SHL_RM32_CL(const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op_shl(*this, data, cl()));
}

void SoftCPU::SHL_RM32_imm8(const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op_shl(*this, data, insn.imm8()));
}

void SoftCPU::SHL_RM8_1(const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op_shl(*this, data, 1));
}

void SoftCPU::SHL_RM8_CL(const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op_shl(*this, data, cl()));
}

void SoftCPU::SHL_RM8_imm8(const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op_shl(*this, data, insn.imm8()));
}

void SoftCPU::SHRD_RM16_reg16_CL(const X86::Instruction&) { TODO(); }
void SoftCPU::SHRD_RM16_reg16_imm8(const X86::Instruction&) { TODO(); }
void SoftCPU::SHRD_RM32_reg32_CL(const X86::Instruction&) { TODO(); }

void SoftCPU::SHRD_RM32_reg32_imm8(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, op_shrd(*this, gpr32(insn.reg32()), insn.modrm().read32(*this, insn), insn.imm8()));
}

void SoftCPU::SHR_RM16_1(const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op_shr(*this, data, 1));
}

void SoftCPU::SHR_RM16_CL(const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op_shr(*this, data, cl()));
}

void SoftCPU::SHR_RM16_imm8(const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op_shr(*this, data, insn.imm8()));
}

void SoftCPU::SHR_RM32_1(const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op_shr(*this, data, 1));
}

void SoftCPU::SHR_RM32_CL(const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op_shr(*this, data, cl()));
}

void SoftCPU::SHR_RM32_imm8(const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op_shr(*this, data, insn.imm8()));
}

void SoftCPU::SHR_RM8_1(const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op_shr(*this, data, 1));
}

void SoftCPU::SHR_RM8_CL(const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op_shr(*this, data, cl()));
}

void SoftCPU::SHR_RM8_imm8(const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op_shr(*this, data, insn.imm8()));
}

void SoftCPU::SIDT(const X86::Instruction&) { TODO(); }
void SoftCPU::SLDT_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::SMSW_RM16(const X86::Instruction&) { TODO(); }

void SoftCPU::STC(const X86::Instruction&)
{
    set_cf(true);
}

void SoftCPU::STD(const X86::Instruction&)
{
    set_df(true);
}

void SoftCPU::STI(const X86::Instruction&) { TODO(); }

void SoftCPU::STOSB(const X86::Instruction& insn)
{
    if (insn.has_address_size_override_prefix()) {
        do_once_or_repeat<false>(insn, [&] {
            write_memory8({ es(), di() }, al());
            set_di(di() + (df() ? -1 : 1));
        });
    } else {
        do_once_or_repeat<false>(insn, [&] {
            write_memory8({ es(), edi() }, al());
            set_edi(edi() + (df() ? -1 : 1));
        });
    }
}

void SoftCPU::STOSD(const X86::Instruction& insn)
{
    if (insn.has_address_size_override_prefix()) {
        do_once_or_repeat<false>(insn, [&] {
            write_memory32({ es(), di() }, eax());
            set_di(di() + (df() ? -4 : 4));
        });
    } else {
        do_once_or_repeat<false>(insn, [&] {
            write_memory32({ es(), edi() }, eax());
            set_edi(edi() + (df() ? -4 : 4));
        });
    }
}

void SoftCPU::STOSW(const X86::Instruction& insn)
{
    if (insn.has_address_size_override_prefix()) {
        do_once_or_repeat<false>(insn, [&] {
            write_memory16({ es(), di() }, ax());
            set_di(di() + (df() ? -2 : 2));
        });
    } else {
        do_once_or_repeat<false>(insn, [&] {
            write_memory16({ es(), edi() }, ax());
            set_edi(edi() + (df() ? -2 : 2));
        });
    }
}

void SoftCPU::STR_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::UD0(const X86::Instruction&) { TODO(); }
void SoftCPU::UD1(const X86::Instruction&) { TODO(); }
void SoftCPU::UD2(const X86::Instruction&) { TODO(); }
void SoftCPU::VERR_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::VERW_RM16(const X86::Instruction&) { TODO(); }
void SoftCPU::WAIT(const X86::Instruction&) { TODO(); }
void SoftCPU::WBINVD(const X86::Instruction&) { TODO(); }

void SoftCPU::XADD_RM16_reg16(const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = gpr16(insn.reg16());
    auto result = op_add(*this, dest, src);
    gpr16(insn.reg16()) = dest;
    insn.modrm().write16(*this, insn, result);
}

void SoftCPU::XADD_RM32_reg32(const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = gpr32(insn.reg32());
    auto result = op_add(*this, dest, src);
    gpr32(insn.reg32()) = dest;
    insn.modrm().write32(*this, insn, result);
}

void SoftCPU::XADD_RM8_reg8(const X86::Instruction& insn)
{
    auto dest = insn.modrm().read8(*this, insn);
    auto src = gpr8(insn.reg8());
    auto result = op_add(*this, dest, src);
    gpr8(insn.reg8()) = dest;
    insn.modrm().write8(*this, insn, result);
}

void SoftCPU::XCHG_AX_reg16(const X86::Instruction& insn)
{
    auto temp = gpr16(insn.reg16());
    gpr16(insn.reg16()) = eax();
    set_eax(temp);
}

void SoftCPU::XCHG_EAX_reg32(const X86::Instruction& insn)
{
    auto temp = gpr32(insn.reg32());
    gpr32(insn.reg32()) = eax();
    set_eax(temp);
}

void SoftCPU::XCHG_reg16_RM16(const X86::Instruction& insn)
{
    auto temp = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, gpr16(insn.reg16()));
    gpr16(insn.reg16()) = temp;
}

void SoftCPU::XCHG_reg32_RM32(const X86::Instruction& insn)
{
    auto temp = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, gpr32(insn.reg32()));
    gpr32(insn.reg32()) = temp;
}

void SoftCPU::XCHG_reg8_RM8(const X86::Instruction& insn)
{
    auto temp = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, gpr8(insn.reg8()));
    gpr8(insn.reg8()) = temp;
}

void SoftCPU::XLAT(const X86::Instruction&) { TODO(); }

#define DEFINE_GENERIC_INSN_HANDLERS_PARTIAL(mnemonic, op, update_dest)                                                   \
    void SoftCPU::mnemonic##_AL_imm8(const X86::Instruction& insn) { generic_AL_imm8<update_dest>(op<u8>, insn); }        \
    void SoftCPU::mnemonic##_AX_imm16(const X86::Instruction& insn) { generic_AX_imm16<update_dest>(op<u16>, insn); }     \
    void SoftCPU::mnemonic##_EAX_imm32(const X86::Instruction& insn) { generic_EAX_imm32<update_dest>(op<u32>, insn); }   \
    void SoftCPU::mnemonic##_RM16_imm16(const X86::Instruction& insn) { generic_RM16_imm16<update_dest>(op<u16>, insn); } \
    void SoftCPU::mnemonic##_RM16_reg16(const X86::Instruction& insn) { generic_RM16_reg16<update_dest>(op<u16>, insn); } \
    void SoftCPU::mnemonic##_RM32_imm32(const X86::Instruction& insn) { generic_RM32_imm32<update_dest>(op<u32>, insn); } \
    void SoftCPU::mnemonic##_RM32_reg32(const X86::Instruction& insn) { generic_RM32_reg32<update_dest>(op<u32>, insn); } \
    void SoftCPU::mnemonic##_RM8_imm8(const X86::Instruction& insn) { generic_RM8_imm8<update_dest>(op<u8>, insn); }      \
    void SoftCPU::mnemonic##_RM8_reg8(const X86::Instruction& insn) { generic_RM8_reg8<update_dest>(op<u8>, insn); }

#define DEFINE_GENERIC_INSN_HANDLERS(mnemonic, op, update_dest)                                                           \
    DEFINE_GENERIC_INSN_HANDLERS_PARTIAL(mnemonic, op, update_dest)                                                       \
    void SoftCPU::mnemonic##_RM16_imm8(const X86::Instruction& insn) { generic_RM16_imm8<update_dest>(op<u16>, insn); }   \
    void SoftCPU::mnemonic##_RM32_imm8(const X86::Instruction& insn) { generic_RM32_imm8<update_dest>(op<u32>, insn); }   \
    void SoftCPU::mnemonic##_reg16_RM16(const X86::Instruction& insn) { generic_reg16_RM16<update_dest>(op<u16>, insn); } \
    void SoftCPU::mnemonic##_reg32_RM32(const X86::Instruction& insn) { generic_reg32_RM32<update_dest>(op<u32>, insn); } \
    void SoftCPU::mnemonic##_reg8_RM8(const X86::Instruction& insn) { generic_reg8_RM8<update_dest>(op<u8>, insn); }

DEFINE_GENERIC_INSN_HANDLERS(XOR, op_xor, true)
DEFINE_GENERIC_INSN_HANDLERS(OR, op_or, true)
DEFINE_GENERIC_INSN_HANDLERS(ADD, op_add, true)
DEFINE_GENERIC_INSN_HANDLERS(ADC, op_adc, true)
DEFINE_GENERIC_INSN_HANDLERS(SUB, op_sub, true)
DEFINE_GENERIC_INSN_HANDLERS(SBB, op_sbb, true)
DEFINE_GENERIC_INSN_HANDLERS(AND, op_and, true)
DEFINE_GENERIC_INSN_HANDLERS(CMP, op_sub, false)
DEFINE_GENERIC_INSN_HANDLERS_PARTIAL(TEST, op_and, false)

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
