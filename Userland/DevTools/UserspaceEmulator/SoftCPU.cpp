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
#include <AK/Debug.h>
#include <math.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#if defined(__GNUC__) && !defined(__clang__)
#    pragma GCC optimize("O3")
#endif

#define TODO_INSN()                                                                   \
    do {                                                                              \
        reportln("\n=={}== Unimplemented instruction: {}\n", getpid(), __FUNCTION__); \
        m_emulator.dump_backtrace();                                                  \
        _exit(0);                                                                     \
    } while (0)

#define DEFINE_GENERIC_SHIFT_ROTATE_INSN_HANDLERS(mnemonic, op)                                                                            \
    void SoftCPU::mnemonic##_RM8_1(const X86::Instruction& insn) { generic_RM8_1(op<ValueWithShadow<u8>>, insn); }                         \
    void SoftCPU::mnemonic##_RM8_CL(const X86::Instruction& insn) { generic_RM8_CL(op<ValueWithShadow<u8>>, insn); }                       \
    void SoftCPU::mnemonic##_RM8_imm8(const X86::Instruction& insn) { generic_RM8_imm8<true, false>(op<ValueWithShadow<u8>>, insn); }      \
    void SoftCPU::mnemonic##_RM16_1(const X86::Instruction& insn) { generic_RM16_1(op<ValueWithShadow<u16>>, insn); }                      \
    void SoftCPU::mnemonic##_RM16_CL(const X86::Instruction& insn) { generic_RM16_CL(op<ValueWithShadow<u16>>, insn); }                    \
    void SoftCPU::mnemonic##_RM16_imm8(const X86::Instruction& insn) { generic_RM16_unsigned_imm8<true>(op<ValueWithShadow<u16>>, insn); } \
    void SoftCPU::mnemonic##_RM32_1(const X86::Instruction& insn) { generic_RM32_1(op<ValueWithShadow<u32>>, insn); }                      \
    void SoftCPU::mnemonic##_RM32_CL(const X86::Instruction& insn) { generic_RM32_CL(op<ValueWithShadow<u32>>, insn); }                    \
    void SoftCPU::mnemonic##_RM32_imm8(const X86::Instruction& insn) { generic_RM32_unsigned_imm8<true>(op<ValueWithShadow<u32>>, insn); }

namespace UserspaceEmulator {

template<class Dest, class Source>
static inline Dest bit_cast(Source source)
{
    static_assert(sizeof(Dest) == sizeof(Source));
    Dest dest;
    memcpy(&dest, &source, sizeof(dest));
    return dest;
}

template<typename T>
ALWAYS_INLINE void warn_if_uninitialized(T value_with_shadow, const char* message)
{
    if (value_with_shadow.is_uninitialized()) [[unlikely]] {
        reportln("\033[31;1mWarning! Use of uninitialized value: {}\033[0m\n", message);
        Emulator::the().dump_backtrace();
    }
}

ALWAYS_INLINE void SoftCPU::warn_if_flags_tainted(const char* message) const
{
    if (m_flags_tainted) [[unlikely]] {
        reportln("\n=={}==  \033[31;1mConditional depends on uninitialized data\033[0m ({})\n", getpid(), message);
        Emulator::the().dump_backtrace();
    }
}

template<typename T, typename U>
constexpr T sign_extended_to(U value)
{
    if (!(value & X86::TypeTrivia<U>::sign_bit))
        return value;
    return (X86::TypeTrivia<T>::mask & ~X86::TypeTrivia<U>::mask) | value;
}

SoftCPU::SoftCPU(Emulator& emulator)
    : m_emulator(emulator)
{
    memset(m_gpr, 0, sizeof(m_gpr));
    memset(m_gpr_shadow, 1, sizeof(m_gpr_shadow));

    m_segment[(int)X86::SegmentRegister::CS] = 0x1b;
    m_segment[(int)X86::SegmentRegister::DS] = 0x23;
    m_segment[(int)X86::SegmentRegister::ES] = 0x23;
    m_segment[(int)X86::SegmentRegister::SS] = 0x23;
    m_segment[(int)X86::SegmentRegister::GS] = 0x2b;
}

void SoftCPU::dump() const
{
    outln(" eax={:08x}  ebx={:08x}  ecx={:08x}  edx={:08x}  ebp={:08x}  esp={:08x}  esi={:08x}  edi={:08x} o={:d} s={:d} z={:d} a={:d} p={:d} c={:d}",
        eax(), ebx(), ecx(), edx(), ebp(), esp(), esi(), edi(), of(), sf(), zf(), af(), pf(), cf());
    outln("#eax={:08x} #ebx={:08x} #ecx={:08x} #edx={:08x} #ebp={:08x} #esp={:08x} #esi={:08x} #edi={:08x} #f={}",
        eax().shadow(), ebx().shadow(), ecx().shadow(), edx().shadow(), ebp().shadow(), esp().shadow(), esi().shadow(), edi().shadow(), m_flags_tainted);
    fflush(stdout);
}

void SoftCPU::update_code_cache()
{
    auto* region = m_emulator.mmu().find_region({ cs(), eip() });
    VERIFY(region);

    if (!region->is_executable()) {
        reportln("SoftCPU::update_code_cache: Non-executable region @ {:p}", eip());
        Emulator::the().dump_backtrace();
        TODO();
    }

    // FIXME: This cache needs to be invalidated if the code region is ever unmapped.
    m_cached_code_region = region;
    m_cached_code_base_ptr = region->data();
}

ValueWithShadow<u8> SoftCPU::read_memory8(X86::LogicalAddress address)
{
    VERIFY(address.selector() == 0x1b || address.selector() == 0x23 || address.selector() == 0x2b);
    auto value = m_emulator.mmu().read8(address);
#if MEMORY_DEBUG
    outln("\033[36;1mread_memory8: @{:04x}:{:08x} -> {:02x} ({:02x})\033[0m", address.selector(), address.offset(), value, value.shadow());
#endif
    return value;
}

ValueWithShadow<u16> SoftCPU::read_memory16(X86::LogicalAddress address)
{
    VERIFY(address.selector() == 0x1b || address.selector() == 0x23 || address.selector() == 0x2b);
    auto value = m_emulator.mmu().read16(address);
#if MEMORY_DEBUG
    outln("\033[36;1mread_memory16: @{:04x}:{:08x} -> {:04x} ({:04x})\033[0m", address.selector(), address.offset(), value, value.shadow());
#endif
    return value;
}

ValueWithShadow<u32> SoftCPU::read_memory32(X86::LogicalAddress address)
{
    VERIFY(address.selector() == 0x1b || address.selector() == 0x23 || address.selector() == 0x2b);
    auto value = m_emulator.mmu().read32(address);
#if MEMORY_DEBUG
    outln("\033[36;1mread_memory32: @{:04x}:{:08x} -> {:08x} ({:08x})\033[0m", address.selector(), address.offset(), value, value.shadow());
#endif
    return value;
}

ValueWithShadow<u64> SoftCPU::read_memory64(X86::LogicalAddress address)
{
    VERIFY(address.selector() == 0x1b || address.selector() == 0x23 || address.selector() == 0x2b);
    auto value = m_emulator.mmu().read64(address);
#if MEMORY_DEBUG
    outln("\033[36;1mread_memory64: @{:04x}:{:08x} -> {:016x} ({:016x})\033[0m", address.selector(), address.offset(), value, value.shadow());
#endif
    return value;
}

void SoftCPU::write_memory8(X86::LogicalAddress address, ValueWithShadow<u8> value)
{
    VERIFY(address.selector() == 0x23 || address.selector() == 0x2b);
#if MEMORY_DEBUG
    outln("\033[36;1mwrite_memory8: @{:04x}:{:08x} <- {:02x} ({:02x})\033[0m", address.selector(), address.offset(), value, value.shadow());
#endif
    m_emulator.mmu().write8(address, value);
}

void SoftCPU::write_memory16(X86::LogicalAddress address, ValueWithShadow<u16> value)
{
    VERIFY(address.selector() == 0x23 || address.selector() == 0x2b);
#if MEMORY_DEBUG
    outln("\033[36;1mwrite_memory16: @{:04x}:{:08x} <- {:04x} ({:04x})\033[0m", address.selector(), address.offset(), value, value.shadow());
#endif
    m_emulator.mmu().write16(address, value);
}

void SoftCPU::write_memory32(X86::LogicalAddress address, ValueWithShadow<u32> value)
{
    VERIFY(address.selector() == 0x23 || address.selector() == 0x2b);
#if MEMORY_DEBUG
    outln("\033[36;1mwrite_memory32: @{:04x}:{:08x} <- {:08x} ({:08x})\033[0m", address.selector(), address.offset(), value, value.shadow());
#endif
    m_emulator.mmu().write32(address, value);
}

void SoftCPU::write_memory64(X86::LogicalAddress address, ValueWithShadow<u64> value)
{
    VERIFY(address.selector() == 0x23 || address.selector() == 0x2b);
#if MEMORY_DEBUG
    outln("\033[36;1mwrite_memory64: @{:04x}:{:08x} <- {:016x} ({:016x})\033[0m", address.selector(), address.offset(), value, value.shadow());
#endif
    m_emulator.mmu().write64(address, value);
}

void SoftCPU::push_string(const StringView& string)
{
    size_t space_to_allocate = round_up_to_power_of_two(string.length() + 1, 16);
    set_esp({ esp().value() - space_to_allocate, esp().shadow() });
    m_emulator.mmu().copy_to_vm(esp().value(), string.characters_without_null_termination(), string.length());
    m_emulator.mmu().write8({ 0x23, esp().value() + string.length() }, shadow_wrap_as_initialized((u8)'\0'));
}

void SoftCPU::push_buffer(const u8* data, size_t size)
{
    set_esp({ esp().value() - size, esp().shadow() });
    warn_if_uninitialized(esp(), "push_buffer");
    m_emulator.mmu().copy_to_vm(esp().value(), data, size);
}

void SoftCPU::push32(ValueWithShadow<u32> value)
{
    set_esp({ esp().value() - sizeof(u32), esp().shadow() });
    warn_if_uninitialized(esp(), "push32");
    write_memory32({ ss(), esp().value() }, value);
}

ValueWithShadow<u32> SoftCPU::pop32()
{
    warn_if_uninitialized(esp(), "pop32");
    auto value = read_memory32({ ss(), esp().value() });
    set_esp({ esp().value() + sizeof(u32), esp().shadow() });
    return value;
}

void SoftCPU::push16(ValueWithShadow<u16> value)
{
    warn_if_uninitialized(esp(), "push16");
    set_esp({ esp().value() - sizeof(u16), esp().shadow() });
    write_memory16({ ss(), esp().value() }, value);
}

ValueWithShadow<u16> SoftCPU::pop16()
{
    warn_if_uninitialized(esp(), "pop16");
    auto value = read_memory16({ ss(), esp().value() });
    set_esp({ esp().value() + sizeof(u16), esp().shadow() });
    return value;
}

template<bool check_zf, typename Callback>
void SoftCPU::do_once_or_repeat(const X86::Instruction& insn, Callback callback)
{
    if (!insn.has_rep_prefix())
        return callback();

    while (loop_index(insn.a32()).value()) {
        callback();
        decrement_loop_index(insn.a32());
        if constexpr (check_zf) {
            warn_if_flags_tainted("repz/repnz");
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
    typename T::ValueType result;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("incl %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("incw %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("incb %%al\n"
                     : "=a"(result)
                     : "a"(data.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszap(new_flags);
    cpu.taint_flags_from(data);
    return shadow_wrap_with_taint_from(result, data);
}

template<typename T>
ALWAYS_INLINE static T op_dec(SoftCPU& cpu, T data)
{
    typename T::ValueType result;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("decl %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("decw %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("decb %%al\n"
                     : "=a"(result)
                     : "a"(data.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszap(new_flags);
    cpu.taint_flags_from(data);
    return shadow_wrap_with_taint_from(result, data);
}

template<typename T>
ALWAYS_INLINE static T op_xor(SoftCPU& cpu, const T& dest, const T& src)
{
    typename T::ValueType result;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("xorl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("xor %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("xorb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else {
        VERIFY_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszpc(new_flags);
    cpu.taint_flags_from(dest, src);
    return shadow_wrap_with_taint_from(result, dest, src);
}

template<typename T>
ALWAYS_INLINE static T op_or(SoftCPU& cpu, const T& dest, const T& src)
{
    typename T::ValueType result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("orl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("or %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("orb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else {
        VERIFY_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszpc(new_flags);
    cpu.taint_flags_from(dest, src);
    return shadow_wrap_with_taint_from(result, dest, src);
}

template<typename T>
ALWAYS_INLINE static T op_sub(SoftCPU& cpu, const T& dest, const T& src)
{
    typename T::ValueType result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("subl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("subw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("subb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else {
        VERIFY_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    cpu.taint_flags_from(dest, src);
    return shadow_wrap_with_taint_from(result, dest, src);
}

template<typename T, bool cf>
ALWAYS_INLINE static T op_sbb_impl(SoftCPU& cpu, const T& dest, const T& src)
{
    typename T::ValueType result = 0;
    u32 new_flags = 0;

    if constexpr (cf)
        asm volatile("stc");
    else
        asm volatile("clc");

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("sbbl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("sbbw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("sbbb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else {
        VERIFY_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    cpu.taint_flags_from(dest, src);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, dest, src);
}

template<typename T>
ALWAYS_INLINE static T op_sbb(SoftCPU& cpu, T& dest, const T& src)
{
    cpu.warn_if_flags_tainted("sbb");
    if (cpu.cf())
        return op_sbb_impl<T, true>(cpu, dest, src);
    return op_sbb_impl<T, false>(cpu, dest, src);
}

template<typename T>
ALWAYS_INLINE static T op_add(SoftCPU& cpu, T& dest, const T& src)
{
    typename T::ValueType result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("addl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("addw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("addb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else {
        VERIFY_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    cpu.taint_flags_from(dest, src);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, dest, src);
}

template<typename T, bool cf>
ALWAYS_INLINE static T op_adc_impl(SoftCPU& cpu, T& dest, const T& src)
{
    typename T::ValueType result = 0;
    u32 new_flags = 0;

    if constexpr (cf)
        asm volatile("stc");
    else
        asm volatile("clc");

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("adcl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("adcw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("adcb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else {
        VERIFY_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    cpu.taint_flags_from(dest, src);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, dest, src);
}

template<typename T>
ALWAYS_INLINE static T op_adc(SoftCPU& cpu, T& dest, const T& src)
{
    cpu.warn_if_flags_tainted("adc");
    if (cpu.cf())
        return op_adc_impl<T, true>(cpu, dest, src);
    return op_adc_impl<T, false>(cpu, dest, src);
}

template<typename T>
ALWAYS_INLINE static T op_and(SoftCPU& cpu, const T& dest, const T& src)
{
    typename T::ValueType result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("andl %%ecx, %%eax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("andw %%cx, %%ax\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("andb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(dest.value()), "c"(src.value()));
    } else {
        VERIFY_NOT_REACHED();
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszpc(new_flags);
    cpu.taint_flags_from(dest, src);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, dest, src);
}

template<typename T>
ALWAYS_INLINE static void op_imul(SoftCPU& cpu, const T& dest, const T& src, T& result_high, T& result_low)
{
    bool did_overflow = false;
    if constexpr (sizeof(T) == 4) {
        i64 result = (i64)src * (i64)dest;
        result_low = result & 0xffffffff;
        result_high = result >> 32;
        did_overflow = (result > NumericLimits<T>::max() || result < NumericLimits<T>::min());
    } else if constexpr (sizeof(T) == 2) {
        i32 result = (i32)src * (i32)dest;
        result_low = result & 0xffff;
        result_high = result >> 16;
        did_overflow = (result > NumericLimits<T>::max() || result < NumericLimits<T>::min());
    } else if constexpr (sizeof(T) == 1) {
        i16 result = (i16)src * (i16)dest;
        result_low = result & 0xff;
        result_high = result >> 8;
        did_overflow = (result > NumericLimits<T>::max() || result < NumericLimits<T>::min());
    }

    if (did_overflow) {
        cpu.set_cf(true);
        cpu.set_of(true);
    } else {
        cpu.set_cf(false);
        cpu.set_of(false);
    }
}

template<typename T>
ALWAYS_INLINE static T op_shr(SoftCPU& cpu, T data, ValueWithShadow<u8> steps)
{
    if (steps.value() == 0)
        return shadow_wrap_with_taint_from(data.value(), data, steps);

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("shrl %%cl, %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("shrw %%cl, %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("shrb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    cpu.taint_flags_from(data, steps);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, data, steps);
}

template<typename T>
ALWAYS_INLINE static T op_shl(SoftCPU& cpu, T data, ValueWithShadow<u8> steps)
{
    if (steps.value() == 0)
        return shadow_wrap_with_taint_from(data.value(), data, steps);

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("shll %%cl, %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("shlw %%cl, %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("shlb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    cpu.taint_flags_from(data, steps);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, data, steps);
}

template<typename T>
ALWAYS_INLINE static T op_shrd(SoftCPU& cpu, T data, T extra_bits, ValueWithShadow<u8> steps)
{
    if (steps.value() == 0)
        return shadow_wrap_with_taint_from(data.value(), data, steps);

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("shrd %%cl, %%edx, %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()), "d"(extra_bits.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("shrd %%cl, %%dx, %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()), "d"(extra_bits.value()), "c"(steps.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    cpu.taint_flags_from(data, steps);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, data, steps);
}

template<typename T>
ALWAYS_INLINE static T op_shld(SoftCPU& cpu, T data, T extra_bits, ValueWithShadow<u8> steps)
{
    if (steps.value() == 0)
        return shadow_wrap_with_taint_from(data.value(), data, steps);

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("shld %%cl, %%edx, %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()), "d"(extra_bits.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("shld %%cl, %%dx, %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()), "d"(extra_bits.value()), "c"(steps.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    cpu.taint_flags_from(data, steps);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, data, steps);
}

template<bool update_dest, bool is_or, typename Op>
ALWAYS_INLINE void SoftCPU::generic_AL_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = al();
    auto src = shadow_wrap_as_initialized(insn.imm8());
    auto result = op(*this, dest, src);
    if (is_or && insn.imm8() == 0xff)
        result.set_initialized();
    if (update_dest)
        set_al(result);
}

template<bool update_dest, bool is_or, typename Op>
ALWAYS_INLINE void SoftCPU::generic_AX_imm16(Op op, const X86::Instruction& insn)
{
    auto dest = ax();
    auto src = shadow_wrap_as_initialized(insn.imm16());
    auto result = op(*this, dest, src);
    if (is_or && insn.imm16() == 0xffff)
        result.set_initialized();
    if (update_dest)
        set_ax(result);
}

template<bool update_dest, bool is_or, typename Op>
ALWAYS_INLINE void SoftCPU::generic_EAX_imm32(Op op, const X86::Instruction& insn)
{
    auto dest = eax();
    auto src = shadow_wrap_as_initialized(insn.imm32());
    auto result = op(*this, dest, src);
    if (is_or && insn.imm32() == 0xffffffff)
        result.set_initialized();
    if (update_dest)
        set_eax(result);
}

template<bool update_dest, bool is_or, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM16_imm16(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = shadow_wrap_as_initialized(insn.imm16());
    auto result = op(*this, dest, src);
    if (is_or && insn.imm16() == 0xffff)
        result.set_initialized();
    if (update_dest)
        insn.modrm().write16(*this, insn, result);
}

template<bool update_dest, bool is_or, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM16_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = shadow_wrap_as_initialized<u16>(sign_extended_to<u16>(insn.imm8()));
    auto result = op(*this, dest, src);
    if (is_or && src.value() == 0xffff)
        result.set_initialized();
    if (update_dest)
        insn.modrm().write16(*this, insn, result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM16_unsigned_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = shadow_wrap_as_initialized(insn.imm8());
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write16(*this, insn, result);
}

template<bool update_dest, bool dont_taint_for_same_operand, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM16_reg16(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = const_gpr16(insn.reg16());
    auto result = op(*this, dest, src);
    if (dont_taint_for_same_operand && insn.modrm().is_register() && insn.modrm().register_index() == insn.register_index()) {
        result.set_initialized();
        m_flags_tainted = false;
    }
    if (update_dest)
        insn.modrm().write16(*this, insn, result);
}

template<bool update_dest, bool is_or, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM32_imm32(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = insn.imm32();
    auto result = op(*this, dest, shadow_wrap_as_initialized(src));
    if (is_or && src == 0xffffffff)
        result.set_initialized();
    if (update_dest)
        insn.modrm().write32(*this, insn, result);
}

template<bool update_dest, bool is_or, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM32_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = sign_extended_to<u32>(insn.imm8());
    auto result = op(*this, dest, shadow_wrap_as_initialized(src));
    if (is_or && src == 0xffffffff)
        result.set_initialized();
    if (update_dest)
        insn.modrm().write32(*this, insn, result);
}

template<bool update_dest, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM32_unsigned_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = shadow_wrap_as_initialized(insn.imm8());
    auto result = op(*this, dest, src);
    if (update_dest)
        insn.modrm().write32(*this, insn, result);
}

template<bool update_dest, bool dont_taint_for_same_operand, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM32_reg32(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = const_gpr32(insn.reg32());
    auto result = op(*this, dest, src);
    if (dont_taint_for_same_operand && insn.modrm().is_register() && insn.modrm().register_index() == insn.register_index()) {
        result.set_initialized();
        m_flags_tainted = false;
    }
    if (update_dest)
        insn.modrm().write32(*this, insn, result);
}

template<bool update_dest, bool is_or, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM8_imm8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read8(*this, insn);
    auto src = insn.imm8();
    auto result = op(*this, dest, shadow_wrap_as_initialized(src));
    if (is_or && src == 0xff)
        result.set_initialized();
    if (update_dest)
        insn.modrm().write8(*this, insn, result);
}

template<bool update_dest, bool dont_taint_for_same_operand, typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM8_reg8(Op op, const X86::Instruction& insn)
{
    auto dest = insn.modrm().read8(*this, insn);
    auto src = const_gpr8(insn.reg8());
    auto result = op(*this, dest, src);
    if (dont_taint_for_same_operand && insn.modrm().is_register() && insn.modrm().register_index() == insn.register_index()) {
        result.set_initialized();
        m_flags_tainted = false;
    }
    if (update_dest)
        insn.modrm().write8(*this, insn, result);
}

template<bool update_dest, bool dont_taint_for_same_operand, typename Op>
ALWAYS_INLINE void SoftCPU::generic_reg16_RM16(Op op, const X86::Instruction& insn)
{
    auto dest = const_gpr16(insn.reg16());
    auto src = insn.modrm().read16(*this, insn);
    auto result = op(*this, dest, src);
    if (dont_taint_for_same_operand && insn.modrm().is_register() && insn.modrm().register_index() == insn.register_index()) {
        result.set_initialized();
        m_flags_tainted = false;
    }
    if (update_dest)
        gpr16(insn.reg16()) = result;
}

template<bool update_dest, bool dont_taint_for_same_operand, typename Op>
ALWAYS_INLINE void SoftCPU::generic_reg32_RM32(Op op, const X86::Instruction& insn)
{
    auto dest = const_gpr32(insn.reg32());
    auto src = insn.modrm().read32(*this, insn);
    auto result = op(*this, dest, src);
    if (dont_taint_for_same_operand && insn.modrm().is_register() && insn.modrm().register_index() == insn.register_index()) {
        result.set_initialized();
        m_flags_tainted = false;
    }
    if (update_dest)
        gpr32(insn.reg32()) = result;
}

template<bool update_dest, bool dont_taint_for_same_operand, typename Op>
ALWAYS_INLINE void SoftCPU::generic_reg8_RM8(Op op, const X86::Instruction& insn)
{
    auto dest = const_gpr8(insn.reg8());
    auto src = insn.modrm().read8(*this, insn);
    auto result = op(*this, dest, src);
    if (dont_taint_for_same_operand && insn.modrm().is_register() && insn.modrm().register_index() == insn.register_index()) {
        result.set_initialized();
        m_flags_tainted = false;
    }
    if (update_dest)
        gpr8(insn.reg8()) = result;
}

template<typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM8_1(Op op, const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op(*this, data, shadow_wrap_as_initialized<u8>(1)));
}

template<typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM8_CL(Op op, const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, op(*this, data, cl()));
}

template<typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM16_1(Op op, const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op(*this, data, shadow_wrap_as_initialized<u8>(1)));
}

template<typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM16_CL(Op op, const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, op(*this, data, cl()));
}

template<typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM32_1(Op op, const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op(*this, data, shadow_wrap_as_initialized<u8>(1)));
}

template<typename Op>
ALWAYS_INLINE void SoftCPU::generic_RM32_CL(Op op, const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, op(*this, data, cl()));
}

void SoftCPU::AAA(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::AAD(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::AAM(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::AAS(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::ARPL(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::BOUND(const X86::Instruction&) { TODO_INSN(); }

template<typename T>
ALWAYS_INLINE static T op_bsf(SoftCPU&, T value)
{
    return { (typename T::ValueType)__builtin_ctz(value.value()), value.shadow() };
}

template<typename T>
ALWAYS_INLINE static T op_bsr(SoftCPU&, T value)
{
    typename T::ValueType bit_index = 0;
    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("bsrl %%eax, %%edx"
                     : "=d"(bit_index)
                     : "a"(value.value()));
    }
    if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("bsrw %%ax, %%dx"
                     : "=d"(bit_index)
                     : "a"(value.value()));
    }
    return shadow_wrap_with_taint_from(bit_index, value);
}

void SoftCPU::BSF_reg16_RM16(const X86::Instruction& insn)
{
    auto src = insn.modrm().read16(*this, insn);
    set_zf(!src.value());
    if (src.value())
        gpr16(insn.reg16()) = op_bsf(*this, src);
    taint_flags_from(src);
}

void SoftCPU::BSF_reg32_RM32(const X86::Instruction& insn)
{
    auto src = insn.modrm().read32(*this, insn);
    set_zf(!src.value());
    if (src.value()) {
        gpr32(insn.reg32()) = op_bsf(*this, src);
        taint_flags_from(src);
    }
}

void SoftCPU::BSR_reg16_RM16(const X86::Instruction& insn)
{
    auto src = insn.modrm().read16(*this, insn);
    set_zf(!src.value());
    if (src.value()) {
        gpr16(insn.reg16()) = op_bsr(*this, src);
        taint_flags_from(src);
    }
}

void SoftCPU::BSR_reg32_RM32(const X86::Instruction& insn)
{
    auto src = insn.modrm().read32(*this, insn);
    set_zf(!src.value());
    if (src.value()) {
        gpr32(insn.reg32()) = op_bsr(*this, src);
        taint_flags_from(src);
    }
}

void SoftCPU::BSWAP_reg32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = { __builtin_bswap32(gpr32(insn.reg32()).value()), __builtin_bswap32(gpr32(insn.reg32()).shadow()) };
}

template<typename T>
ALWAYS_INLINE static T op_bt(T value, T)
{
    return value;
}

template<typename T>
ALWAYS_INLINE static T op_bts(T value, T bit_mask)
{
    return value | bit_mask;
}

template<typename T>
ALWAYS_INLINE static T op_btr(T value, T bit_mask)
{
    return value & ~bit_mask;
}

template<typename T>
ALWAYS_INLINE static T op_btc(T value, T bit_mask)
{
    return value ^ bit_mask;
}

template<bool should_update, typename Op>
ALWAYS_INLINE void BTx_RM16_reg16(SoftCPU& cpu, const X86::Instruction& insn, Op op)
{
    if (insn.modrm().is_register()) {
        unsigned bit_index = cpu.const_gpr16(insn.reg16()).value() & (X86::TypeTrivia<u16>::bits - 1);
        auto original = insn.modrm().read16(cpu, insn);
        u16 bit_mask = 1 << bit_index;
        u16 result = op(original.value(), bit_mask);
        cpu.set_cf((original.value() & bit_mask) != 0);
        cpu.taint_flags_from(cpu.gpr16(insn.reg16()), original);
        if (should_update)
            insn.modrm().write16(cpu, insn, shadow_wrap_with_taint_from(result, cpu.gpr16(insn.reg16()), original));
        return;
    }
    // FIXME: Is this supposed to perform a full 16-bit read/modify/write?
    unsigned bit_offset_in_array = cpu.const_gpr16(insn.reg16()).value() / 8;
    unsigned bit_offset_in_byte = cpu.const_gpr16(insn.reg16()).value() & 7;
    auto address = insn.modrm().resolve(cpu, insn);
    address.set_offset(address.offset() + bit_offset_in_array);
    auto dest = cpu.read_memory8(address);
    u8 bit_mask = 1 << bit_offset_in_byte;
    u8 result = op(dest.value(), bit_mask);
    cpu.set_cf((dest.value() & bit_mask) != 0);
    cpu.taint_flags_from(cpu.gpr16(insn.reg16()), dest);
    if (should_update)
        cpu.write_memory8(address, shadow_wrap_with_taint_from(result, cpu.gpr16(insn.reg16()), dest));
}

template<bool should_update, typename Op>
ALWAYS_INLINE void BTx_RM32_reg32(SoftCPU& cpu, const X86::Instruction& insn, Op op)
{
    if (insn.modrm().is_register()) {
        unsigned bit_index = cpu.const_gpr32(insn.reg32()).value() & (X86::TypeTrivia<u32>::bits - 1);
        auto original = insn.modrm().read32(cpu, insn);
        u32 bit_mask = 1 << bit_index;
        u32 result = op(original.value(), bit_mask);
        cpu.set_cf((original.value() & bit_mask) != 0);
        cpu.taint_flags_from(cpu.gpr32(insn.reg32()), original);
        if (should_update)
            insn.modrm().write32(cpu, insn, shadow_wrap_with_taint_from(result, cpu.gpr32(insn.reg32()), original));
        return;
    }
    // FIXME: Is this supposed to perform a full 32-bit read/modify/write?
    unsigned bit_offset_in_array = cpu.const_gpr32(insn.reg32()).value() / 8;
    unsigned bit_offset_in_byte = cpu.const_gpr32(insn.reg32()).value() & 7;
    auto address = insn.modrm().resolve(cpu, insn);
    address.set_offset(address.offset() + bit_offset_in_array);
    auto dest = cpu.read_memory8(address);
    u8 bit_mask = 1 << bit_offset_in_byte;
    u8 result = op(dest.value(), bit_mask);
    cpu.set_cf((dest.value() & bit_mask) != 0);
    cpu.taint_flags_from(cpu.gpr32(insn.reg32()), dest);
    if (should_update)
        cpu.write_memory8(address, shadow_wrap_with_taint_from(result, cpu.gpr32(insn.reg32()), dest));
}

template<bool should_update, typename Op>
ALWAYS_INLINE void BTx_RM16_imm8(SoftCPU& cpu, const X86::Instruction& insn, Op op)
{
    unsigned bit_index = insn.imm8() & (X86::TypeTrivia<u16>::mask);

    // FIXME: Support higher bit indices
    VERIFY(bit_index < 16);

    auto original = insn.modrm().read16(cpu, insn);
    u16 bit_mask = 1 << bit_index;
    auto result = op(original.value(), bit_mask);
    cpu.set_cf((original.value() & bit_mask) != 0);
    cpu.taint_flags_from(original);
    if (should_update)
        insn.modrm().write16(cpu, insn, shadow_wrap_with_taint_from(result, original));
}

template<bool should_update, typename Op>
ALWAYS_INLINE void BTx_RM32_imm8(SoftCPU& cpu, const X86::Instruction& insn, Op op)
{
    unsigned bit_index = insn.imm8() & (X86::TypeTrivia<u32>::mask);

    // FIXME: Support higher bit indices
    VERIFY(bit_index < 32);

    auto original = insn.modrm().read32(cpu, insn);
    u32 bit_mask = 1 << bit_index;
    auto result = op(original.value(), bit_mask);
    cpu.set_cf((original.value() & bit_mask) != 0);
    cpu.taint_flags_from(original);
    if (should_update)
        insn.modrm().write32(cpu, insn, shadow_wrap_with_taint_from(result, original));
}

#define DEFINE_GENERIC_BTx_INSN_HANDLERS(mnemonic, op, update_dest)                                                          \
    void SoftCPU::mnemonic##_RM32_reg32(const X86::Instruction& insn) { BTx_RM32_reg32<update_dest>(*this, insn, op<u32>); } \
    void SoftCPU::mnemonic##_RM16_reg16(const X86::Instruction& insn) { BTx_RM16_reg16<update_dest>(*this, insn, op<u16>); } \
    void SoftCPU::mnemonic##_RM32_imm8(const X86::Instruction& insn) { BTx_RM32_imm8<update_dest>(*this, insn, op<u32>); }   \
    void SoftCPU::mnemonic##_RM16_imm8(const X86::Instruction& insn) { BTx_RM16_imm8<update_dest>(*this, insn, op<u16>); }

DEFINE_GENERIC_BTx_INSN_HANDLERS(BTS, op_bts, true);
DEFINE_GENERIC_BTx_INSN_HANDLERS(BTR, op_btr, true);
DEFINE_GENERIC_BTx_INSN_HANDLERS(BTC, op_btc, true);
DEFINE_GENERIC_BTx_INSN_HANDLERS(BT, op_bt, false);

void SoftCPU::CALL_FAR_mem16(const X86::Instruction&)
{
    TODO();
}
void SoftCPU::CALL_FAR_mem32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::CALL_RM16(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::CALL_RM32(const X86::Instruction& insn)
{
    push32(shadow_wrap_as_initialized(eip()));
    auto address = insn.modrm().read32(*this, insn);
    warn_if_uninitialized(address, "call rm32");
    set_eip(address.value());
}

void SoftCPU::CALL_imm16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::CALL_imm16_imm16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::CALL_imm16_imm32(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::CALL_imm32(const X86::Instruction& insn)
{
    push32(shadow_wrap_as_initialized(eip()));
    set_eip(eip() + (i32)insn.imm32());
}

void SoftCPU::CBW(const X86::Instruction&)
{
    set_ah(shadow_wrap_with_taint_from<u8>((al().value() & 0x80) ? 0xff : 0x00, al()));
}

void SoftCPU::CDQ(const X86::Instruction&)
{
    if (eax().value() & 0x80000000)
        set_edx(shadow_wrap_with_taint_from<u32>(0xffffffff, eax()));
    else
        set_edx(shadow_wrap_with_taint_from<u32>(0, eax()));
}

void SoftCPU::CLC(const X86::Instruction&)
{
    set_cf(false);
}

void SoftCPU::CLD(const X86::Instruction&)
{
    set_df(false);
}

void SoftCPU::CLI(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::CLTS(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::CMC(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::CMOVcc_reg16_RM16(const X86::Instruction& insn)
{
    warn_if_flags_tainted("cmovcc reg16, rm16");
    if (evaluate_condition(insn.cc()))
        gpr16(insn.reg16()) = insn.modrm().read16(*this, insn);
}

void SoftCPU::CMOVcc_reg32_RM32(const X86::Instruction& insn)
{
    warn_if_flags_tainted("cmovcc reg32, rm32");
    if (evaluate_condition(insn.cc()))
        gpr32(insn.reg32()) = insn.modrm().read32(*this, insn);
}

template<typename T>
ALWAYS_INLINE static void do_cmps(SoftCPU& cpu, const X86::Instruction& insn)
{
    auto src_segment = cpu.segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS));
    cpu.do_once_or_repeat<true>(insn, [&] {
        auto src = cpu.read_memory<T>({ src_segment, cpu.source_index(insn.a32()).value() });
        auto dest = cpu.read_memory<T>({ cpu.es(), cpu.destination_index(insn.a32()).value() });
        op_sub(cpu, dest, src);
        cpu.step_source_index(insn.a32(), sizeof(T));
        cpu.step_destination_index(insn.a32(), sizeof(T));
    });
}

void SoftCPU::CMPSB(const X86::Instruction& insn)
{
    do_cmps<u8>(*this, insn);
}

void SoftCPU::CMPSD(const X86::Instruction& insn)
{
    do_cmps<u32>(*this, insn);
}

void SoftCPU::CMPSW(const X86::Instruction& insn)
{
    do_cmps<u16>(*this, insn);
}

void SoftCPU::CMPXCHG_RM16_reg16(const X86::Instruction& insn)
{
    auto current = insn.modrm().read16(*this, insn);
    taint_flags_from(current, ax());
    if (current.value() == ax().value()) {
        set_zf(true);
        insn.modrm().write16(*this, insn, const_gpr16(insn.reg16()));
    } else {
        set_zf(false);
        set_ax(current);
    }
}

void SoftCPU::CMPXCHG_RM32_reg32(const X86::Instruction& insn)
{
    auto current = insn.modrm().read32(*this, insn);
    taint_flags_from(current, eax());
    if (current.value() == eax().value()) {
        set_zf(true);
        insn.modrm().write32(*this, insn, const_gpr32(insn.reg32()));
    } else {
        set_zf(false);
        set_eax(current);
    }
}

void SoftCPU::CMPXCHG_RM8_reg8(const X86::Instruction& insn)
{
    auto current = insn.modrm().read8(*this, insn);
    taint_flags_from(current, al());
    if (current.value() == al().value()) {
        set_zf(true);
        insn.modrm().write8(*this, insn, const_gpr8(insn.reg8()));
    } else {
        set_zf(false);
        set_al(current);
    }
}

void SoftCPU::CPUID(const X86::Instruction&)
{
    if (eax().value() == 0) {
        set_eax(shadow_wrap_as_initialized<u32>(1));
        set_ebx(shadow_wrap_as_initialized<u32>(0x6c6c6548));
        set_edx(shadow_wrap_as_initialized<u32>(0x6972466f));
        set_ecx(shadow_wrap_as_initialized<u32>(0x73646e65));
        return;
    }

    if (eax().value() == 1) {
        u32 stepping = 0;
        u32 model = 1;
        u32 family = 3;
        u32 type = 0;
        set_eax(shadow_wrap_as_initialized<u32>(stepping | (model << 4) | (family << 8) | (type << 12)));
        set_ebx(shadow_wrap_as_initialized<u32>(0));
        set_edx(shadow_wrap_as_initialized<u32>((1 << 15))); // Features (CMOV)
        set_ecx(shadow_wrap_as_initialized<u32>(0));
        return;
    }

    dbgln("Unhandled CPUID with eax={:08x}", eax().value());
}

void SoftCPU::CWD(const X86::Instruction&)
{
    set_dx(shadow_wrap_with_taint_from<u16>((ax().value() & 0x8000) ? 0xffff : 0x0000, ax()));
}

void SoftCPU::CWDE(const X86::Instruction&)
{
    set_eax(shadow_wrap_with_taint_from(sign_extended_to<u32>(ax().value()), ax()));
}

void SoftCPU::DAA(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::DAS(const X86::Instruction&) { TODO_INSN(); }

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
    gpr16(insn.reg16()) = op_dec(*this, const_gpr16(insn.reg16()));
}

void SoftCPU::DEC_reg32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = op_dec(*this, const_gpr32(insn.reg32()));
}

void SoftCPU::DIV_RM16(const X86::Instruction& insn)
{
    auto divisor = insn.modrm().read16(*this, insn);
    if (divisor.value() == 0) {
        reportln("Divide by zero");
        TODO();
    }
    u32 dividend = ((u32)dx().value() << 16) | ax().value();
    auto quotient = dividend / divisor.value();
    if (quotient > NumericLimits<u16>::max()) {
        reportln("Divide overflow");
        TODO();
    }

    auto remainder = dividend % divisor.value();
    auto original_ax = ax();

    set_ax(shadow_wrap_with_taint_from<u16>(quotient, original_ax, dx()));
    set_dx(shadow_wrap_with_taint_from<u16>(remainder, original_ax, dx()));
}

void SoftCPU::DIV_RM32(const X86::Instruction& insn)
{
    auto divisor = insn.modrm().read32(*this, insn);
    if (divisor.value() == 0) {
        reportln("Divide by zero");
        TODO();
    }
    u64 dividend = ((u64)edx().value() << 32) | eax().value();
    auto quotient = dividend / divisor.value();
    if (quotient > NumericLimits<u32>::max()) {
        reportln("Divide overflow");
        TODO();
    }

    auto remainder = dividend % divisor.value();
    auto original_eax = eax();

    set_eax(shadow_wrap_with_taint_from<u32>(quotient, original_eax, edx(), divisor));
    set_edx(shadow_wrap_with_taint_from<u32>(remainder, original_eax, edx(), divisor));
}

void SoftCPU::DIV_RM8(const X86::Instruction& insn)
{
    auto divisor = insn.modrm().read8(*this, insn);
    if (divisor.value() == 0) {
        reportln("Divide by zero");
        TODO();
    }
    u16 dividend = ax().value();
    auto quotient = dividend / divisor.value();
    if (quotient > NumericLimits<u8>::max()) {
        reportln("Divide overflow");
        TODO();
    }

    auto remainder = dividend % divisor.value();
    auto original_ax = ax();
    set_al(shadow_wrap_with_taint_from<u8>(quotient, original_ax, divisor));
    set_ah(shadow_wrap_with_taint_from<u8>(remainder, original_ax, divisor));
}

void SoftCPU::ENTER16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::ENTER32(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::ESCAPE(const X86::Instruction&)
{
    reportln("FIXME: x87 floating-point support");
    m_emulator.dump_backtrace();
    TODO();
}

void SoftCPU::FADD_RM32(const X86::Instruction& insn)
{
    // XXX look at ::INC_foo for how mem/reg stuff is handled, and use that here too to make sure this is only called for mem32 ops
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(insn.modrm().register_index()) + fpu_get(0));
    } else {
        auto new_f32 = insn.modrm().read32(*this, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        fpu_set(0, fpu_get(0) + f32);
    }
}

void SoftCPU::FMUL_RM32(const X86::Instruction& insn)
{
    // XXX look at ::INC_foo for how mem/reg stuff is handled, and use that here too to make sure this is only called for mem32 ops
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(0) * fpu_get(insn.modrm().register_index()));
    } else {
        auto new_f32 = insn.modrm().read32(*this, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        fpu_set(0, fpu_get(0) * f32);
    }
}

void SoftCPU::FCOM_RM32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FCOMP_RM32(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FSUB_RM32(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(0) - fpu_get(insn.modrm().register_index()));
    } else {
        auto new_f32 = insn.modrm().read32(*this, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        fpu_set(0, fpu_get(0) - f32);
    }
}

void SoftCPU::FSUBR_RM32(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(insn.modrm().register_index()) - fpu_get(0));
    } else {
        auto new_f32 = insn.modrm().read32(*this, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        fpu_set(0, f32 - fpu_get(0));
    }
}

void SoftCPU::FDIV_RM32(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(0) / fpu_get(insn.modrm().register_index()));
    } else {
        auto new_f32 = insn.modrm().read32(*this, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
        fpu_set(0, fpu_get(0) / f32);
    }
}

void SoftCPU::FDIVR_RM32(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(0, fpu_get(insn.modrm().register_index()) / fpu_get(0));
    } else {
        auto new_f32 = insn.modrm().read32(*this, insn);
        // FIXME: Respect shadow values
        auto f32 = bit_cast<float>(new_f32.value());
        // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
        fpu_set(0, f32 / fpu_get(0));
    }
}

void SoftCPU::FLD_RM32(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_push(fpu_get(insn.modrm().register_index()));
    } else {
        auto new_f32 = insn.modrm().read32(*this, insn);
        // FIXME: Respect shadow values
        fpu_push(bit_cast<float>(new_f32.value()));
    }
}

void SoftCPU::FXCH(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    auto tmp = fpu_get(0);
    fpu_set(0, fpu_get(insn.modrm().register_index()));
    fpu_set(insn.modrm().register_index(), tmp);
}

void SoftCPU::FST_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    float f32 = (float)fpu_get(0);
    // FIXME: Respect shadow values
    insn.modrm().write32(*this, insn, shadow_wrap_as_initialized(bit_cast<u32>(f32)));
}

void SoftCPU::FNOP(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FSTP_RM32(const X86::Instruction& insn)
{
    FST_RM32(insn);
    fpu_pop();
}

void SoftCPU::FLDENV(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FCHS(const X86::Instruction&)
{
    fpu_set(0, -fpu_get(0));
}

void SoftCPU::FABS(const X86::Instruction&)
{
    fpu_set(0, __builtin_fabs(fpu_get(0)));
}

void SoftCPU::FTST(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FXAM(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FLDCW(const X86::Instruction& insn)
{
    m_fpu_cw = insn.modrm().read16(*this, insn);
}

void SoftCPU::FLD1(const X86::Instruction&)
{
    fpu_push(1.0);
}

void SoftCPU::FLDL2T(const X86::Instruction&)
{
    fpu_push(log2f(10.0f));
}

void SoftCPU::FLDL2E(const X86::Instruction&)
{
    fpu_push(log2f(M_E));
}

void SoftCPU::FLDPI(const X86::Instruction&)
{
    fpu_push(M_PI);
}

void SoftCPU::FLDLG2(const X86::Instruction&)
{
    fpu_push(log10f(2.0f));
}

void SoftCPU::FLDLN2(const X86::Instruction&)
{
    fpu_push(M_LN2);
}

void SoftCPU::FLDZ(const X86::Instruction&)
{
    fpu_push(0.0);
}

void SoftCPU::FNSTENV(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::F2XM1(const X86::Instruction&)
{
    // FIXME: validate ST(0) is in range –1.0 to +1.0
    auto f32 = fpu_get(0);
    // FIXME: Set C0, C2, C3 in FPU status word.
    fpu_set(0, powf(2, f32) - 1.0f);
}

void SoftCPU::FYL2X(const X86::Instruction&)
{
    // FIXME: Raise IA on +-infinity, +-0, raise Z on +-0
    auto f32 = fpu_get(0);
    // FIXME: Set C0, C2, C3 in FPU status word.
    fpu_set(1, fpu_get(1) * log2f(f32));
    fpu_pop();
}

void SoftCPU::FYL2XP1(const X86::Instruction&)
{
    // FIXME: validate ST(0) range
    auto f32 = fpu_get(0);
    // FIXME: Set C0, C2, C3 in FPU status word.
    fpu_set(1, (fpu_get(1) * log2f(f32 + 1.0f)));
    fpu_pop();
}

void SoftCPU::FPTAN(const X86::Instruction&)
{
    // FIXME: set C1 upon stack overflow or if result was rounded
    // FIXME: Set C2 to 1 if ST(0) is outside range of -2^63 to +2^63; else set to 0
    fpu_set(0, tanf(fpu_get(0)));
    fpu_push(1.0f);
}

void SoftCPU::FPATAN(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FXTRACT(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FPREM1(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FDECSTP(const X86::Instruction&)
{
    m_fpu_top = (m_fpu_top == 0) ? 7 : m_fpu_top - 1;
    set_cf(0);
}

void SoftCPU::FINCSTP(const X86::Instruction&)
{
    m_fpu_top = (m_fpu_top == 7) ? 0 : m_fpu_top + 1;
    set_cf(0);
}

void SoftCPU::FNSTCW(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, m_fpu_cw);
}

void SoftCPU::FPREM(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FSQRT(const X86::Instruction&)
{
    fpu_set(0, sqrt(fpu_get(0)));
}

void SoftCPU::FSINCOS(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FRNDINT(const X86::Instruction&)
{
    // FIXME: support rounding mode
    fpu_set(0, round(fpu_get(0)));
}

void SoftCPU::FSCALE(const X86::Instruction&)
{
    // FIXME: set C1 upon stack overflow or if result was rounded
    fpu_set(0, fpu_get(0) * powf(2, floorf(fpu_get(1))));
}

void SoftCPU::FSIN(const X86::Instruction&)
{
    fpu_set(0, sin(fpu_get(0)));
}

void SoftCPU::FCOS(const X86::Instruction&)
{
    fpu_set(0, cos(fpu_get(0)));
}

void SoftCPU::FIADD_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) + (long double)m32int);
}

void SoftCPU::FCMOVB(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FIMUL_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) * (long double)m32int);
}

void SoftCPU::FCMOVE(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FICOM_RM32(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FCMOVBE(const X86::Instruction& insn)
{
    if (evaluate_condition(6))
        fpu_set(0, fpu_get(insn.rm() & 7));
}

void SoftCPU::FICOMP_RM32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FCMOVU(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FISUB_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) - (long double)m32int);
}

void SoftCPU::FISUBR_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, (long double)m32int - fpu_get(0));
}

void SoftCPU::FIDIV_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(*this, insn).value();
    // FIXME: Respect shadow values
    // FIXME: Raise IA on 0 / _=0, raise Z on finite / +-0
    fpu_set(0, fpu_get(0) / (long double)m32int);
}

void SoftCPU::FIDIVR_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(*this, insn).value();
    // FIXME: Respect shadow values
    // FIXME: Raise IA on 0 / _=0, raise Z on finite / +-0
    fpu_set(0, (long double)m32int / fpu_get(0));
}

void SoftCPU::FILD_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m32int = (i32)insn.modrm().read32(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_push((long double)m32int);
}

void SoftCPU::FCMOVNB(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FISTTP_RM32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FCMOVNE(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FIST_RM32(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto f = fpu_get(0);
    // FIXME: Respect rounding mode in m_fpu_cw.
    auto i32 = static_cast<int32_t>(f);
    // FIXME: Respect shadow values
    insn.modrm().write32(*this, insn, shadow_wrap_as_initialized(bit_cast<u32>(i32)));
}

void SoftCPU::FCMOVNBE(const X86::Instruction& insn)
{
    if (evaluate_condition(7))
        fpu_set(0, fpu_get(insn.rm() & 7));
}

void SoftCPU::FISTP_RM32(const X86::Instruction& insn)
{
    FIST_RM32(insn);
    fpu_pop();
}

void SoftCPU::FCMOVNU(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FNENI(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FNDISI(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FNCLEX(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FNINIT(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FNSETPM(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FLD_RM80(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FUCOMI(const X86::Instruction& insn)
{
    auto i = insn.rm() & 7;
    // FIXME: Unordered comparison checks.
    // FIXME: QNaN / exception handling.
    // FIXME: Set C0, C2, C3 in FPU status word.
    if (__builtin_isnan(fpu_get(0)) || __builtin_isnan(fpu_get(i))) {
        set_zf(true);
        set_pf(true);
        set_cf(true);
    } else {
        set_zf(fpu_get(0) == fpu_get(i));
        set_pf(false);
        set_cf(fpu_get(0) < fpu_get(i));
        set_of(false);
    }

    // FIXME: Taint should be based on ST(0) and ST(i)
    m_flags_tainted = false;
}

void SoftCPU::FCOMI(const X86::Instruction& insn)
{
    auto i = insn.rm() & 7;
    // FIXME: QNaN / exception handling.
    // FIXME: Set C0, C2, C3 in FPU status word.
    set_zf(fpu_get(0) == fpu_get(i));
    set_pf(false);
    set_cf(fpu_get(0) < fpu_get(i));
    set_of(false);

    // FIXME: Taint should be based on ST(0) and ST(i)
    m_flags_tainted = false;
}

void SoftCPU::FSTP_RM80(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FADD_RM64(const X86::Instruction& insn)
{
    // XXX look at ::INC_foo for how mem/reg stuff is handled, and use that here too to make sure this is only called for mem64 ops
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) + fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(*this, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        fpu_set(0, fpu_get(0) + f64);
    }
}

void SoftCPU::FMUL_RM64(const X86::Instruction& insn)
{
    // XXX look at ::INC_foo for how mem/reg stuff is handled, and use that here too to make sure this is only called for mem64 ops
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) * fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(*this, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        fpu_set(0, fpu_get(0) * f64);
    }
}

void SoftCPU::FCOM_RM64(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FCOMP_RM64(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FSUB_RM64(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) - fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(*this, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        fpu_set(0, fpu_get(0) - f64);
    }
}

void SoftCPU::FSUBR_RM64(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) - fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(*this, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        fpu_set(0, f64 - fpu_get(0));
    }
}

void SoftCPU::FDIV_RM64(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) / fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(*this, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
        fpu_set(0, fpu_get(0) / f64);
    }
}

void SoftCPU::FDIVR_RM64(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        // XXX this is FDIVR, Instruction decodes this weirdly
        //fpu_set(insn.modrm().register_index(), fpu_get(0) / fpu_get(insn.modrm().register_index()));
        fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) / fpu_get(0));
    } else {
        auto new_f64 = insn.modrm().read64(*this, insn);
        // FIXME: Respect shadow values
        auto f64 = bit_cast<double>(new_f64.value());
        // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
        fpu_set(0, f64 / fpu_get(0));
    }
}

void SoftCPU::FLD_RM64(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto new_f64 = insn.modrm().read64(*this, insn);
    // FIXME: Respect shadow values
    fpu_push(bit_cast<double>(new_f64.value()));
}

void SoftCPU::FFREE(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FISTTP_RM64(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FST_RM64(const X86::Instruction& insn)
{
    if (insn.modrm().is_register()) {
        fpu_set(insn.modrm().register_index(), fpu_get(0));
    } else {
        // FIXME: Respect shadow values
        double f64 = (double)fpu_get(0);
        insn.modrm().write64(*this, insn, shadow_wrap_as_initialized(bit_cast<u64>(f64)));
    }
}

void SoftCPU::FSTP_RM64(const X86::Instruction& insn)
{
    FST_RM64(insn);
    fpu_pop();
}

void SoftCPU::FRSTOR(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FUCOM(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FUCOMP(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FUCOMPP(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FNSAVE(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FNSTSW(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FIADD_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) + (long double)m16int);
}

void SoftCPU::FADDP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) + fpu_get(0));
    fpu_pop();
}

void SoftCPU::FIMUL_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) * (long double)m16int);
}

void SoftCPU::FMULP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) * fpu_get(0));
    fpu_pop();
}

void SoftCPU::FICOM_RM16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FICOMP_RM16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FCOMPP(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FISUB_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, fpu_get(0) - (long double)m16int);
}

void SoftCPU::FSUBRP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    fpu_set(insn.modrm().register_index(), fpu_get(0) - fpu_get(insn.modrm().register_index()));
    fpu_pop();
}

void SoftCPU::FISUBR_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_set(0, (long double)m16int - fpu_get(0));
}

void SoftCPU::FSUBP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) - fpu_get(0));
    fpu_pop();
}

void SoftCPU::FIDIV_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(*this, insn).value();
    // FIXME: Respect shadow values
    // FIXME: Raise IA on 0 / _=0, raise Z on finite / +-0
    fpu_set(0, fpu_get(0) / (long double)m16int);
}

void SoftCPU::FDIVRP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
    fpu_set(insn.modrm().register_index(), fpu_get(0) / fpu_get(insn.modrm().register_index()));
    fpu_pop();
}

void SoftCPU::FIDIVR_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(*this, insn).value();
    // FIXME: Respect shadow values
    // FIXME: Raise IA on 0 / _=0, raise Z on finite / +-0
    fpu_set(0, (long double)m16int / fpu_get(0));
}

void SoftCPU::FDIVP(const X86::Instruction& insn)
{
    VERIFY(insn.modrm().is_register());
    // FIXME: Raise IA on + infinity / +-infinity, +-0 / +-0, raise Z on finite / +-0
    fpu_set(insn.modrm().register_index(), fpu_get(insn.modrm().register_index()) / fpu_get(0));
    fpu_pop();
}

void SoftCPU::FILD_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m16int = (i16)insn.modrm().read16(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_push((long double)m16int);
}

void SoftCPU::FFREEP(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FISTTP_RM16(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FIST_RM16(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto f = fpu_get(0);
    // FIXME: Respect rounding mode in m_fpu_cw.
    auto i16 = static_cast<int16_t>(f);
    // FIXME: Respect shadow values
    insn.modrm().write16(*this, insn, shadow_wrap_as_initialized(bit_cast<u16>(i16)));
}

void SoftCPU::FISTP_RM16(const X86::Instruction& insn)
{
    FIST_RM16(insn);
    fpu_pop();
}

void SoftCPU::FBLD_M80(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::FNSTSW_AX(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FILD_RM64(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto m64int = (i64)insn.modrm().read64(*this, insn).value();
    // FIXME: Respect shadow values
    fpu_push((long double)m64int);
}

void SoftCPU::FUCOMIP(const X86::Instruction& insn)
{
    FUCOMI(insn);
    fpu_pop();
}

void SoftCPU::FBSTP_M80(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::FCOMIP(const X86::Instruction& insn)
{
    FCOMI(insn);
    fpu_pop();
}

void SoftCPU::FISTP_RM64(const X86::Instruction& insn)
{
    VERIFY(!insn.modrm().is_register());
    auto f = fpu_pop();
    // FIXME: Respect rounding mode in m_fpu_cw.
    auto i64 = static_cast<int64_t>(f);
    // FIXME: Respect shadow values
    insn.modrm().write64(*this, insn, shadow_wrap_as_initialized(bit_cast<u64>(i64)));
}
void SoftCPU::HLT(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::IDIV_RM16(const X86::Instruction& insn)
{
    auto divisor_with_shadow = insn.modrm().read16(*this, insn);
    auto divisor = (i16)divisor_with_shadow.value();
    if (divisor == 0) {
        reportln("Divide by zero");
        TODO();
    }
    i32 dividend = (i32)(((u32)dx().value() << 16) | (u32)ax().value());
    i32 result = dividend / divisor;
    if (result > NumericLimits<i16>::max() || result < NumericLimits<i16>::min()) {
        reportln("Divide overflow");
        TODO();
    }

    auto original_ax = ax();
    set_ax(shadow_wrap_with_taint_from<u16>(result, original_ax, dx(), divisor_with_shadow));
    set_dx(shadow_wrap_with_taint_from<u16>(dividend % divisor, original_ax, dx(), divisor_with_shadow));
}

void SoftCPU::IDIV_RM32(const X86::Instruction& insn)
{
    auto divisor_with_shadow = insn.modrm().read32(*this, insn);
    auto divisor = (i32)divisor_with_shadow.value();
    if (divisor == 0) {
        reportln("Divide by zero");
        TODO();
    }
    i64 dividend = (i64)(((u64)edx().value() << 32) | (u64)eax().value());
    i64 result = dividend / divisor;
    if (result > NumericLimits<i32>::max() || result < NumericLimits<i32>::min()) {
        reportln("Divide overflow");
        TODO();
    }

    auto original_eax = eax();
    set_eax(shadow_wrap_with_taint_from<u32>(result, original_eax, edx(), divisor_with_shadow));
    set_edx(shadow_wrap_with_taint_from<u32>(dividend % divisor, original_eax, edx(), divisor_with_shadow));
}

void SoftCPU::IDIV_RM8(const X86::Instruction& insn)
{
    auto divisor_with_shadow = insn.modrm().read8(*this, insn);
    auto divisor = (i8)divisor_with_shadow.value();
    if (divisor == 0) {
        reportln("Divide by zero");
        TODO();
    }
    i16 dividend = ax().value();
    i16 result = dividend / divisor;
    if (result > NumericLimits<i8>::max() || result < NumericLimits<i8>::min()) {
        reportln("Divide overflow");
        TODO();
    }

    auto original_ax = ax();
    set_al(shadow_wrap_with_taint_from<u8>(result, divisor_with_shadow, original_ax));
    set_ah(shadow_wrap_with_taint_from<u8>(dividend % divisor, divisor_with_shadow, original_ax));
}

void SoftCPU::IMUL_RM16(const X86::Instruction& insn)
{
    i16 result_high;
    i16 result_low;
    auto src = insn.modrm().read16(*this, insn);
    op_imul<i16>(*this, src.value(), ax().value(), result_high, result_low);
    gpr16(X86::RegisterDX) = shadow_wrap_with_taint_from<u16>(result_high, src, ax());
    gpr16(X86::RegisterAX) = shadow_wrap_with_taint_from<u16>(result_low, src, ax());
}

void SoftCPU::IMUL_RM32(const X86::Instruction& insn)
{
    i32 result_high;
    i32 result_low;
    auto src = insn.modrm().read32(*this, insn);
    op_imul<i32>(*this, src.value(), eax().value(), result_high, result_low);
    gpr32(X86::RegisterEDX) = shadow_wrap_with_taint_from<u32>(result_high, src, eax());
    gpr32(X86::RegisterEAX) = shadow_wrap_with_taint_from<u32>(result_low, src, eax());
}

void SoftCPU::IMUL_RM8(const X86::Instruction& insn)
{
    i8 result_high;
    i8 result_low;
    auto src = insn.modrm().read8(*this, insn);
    op_imul<i8>(*this, src.value(), al().value(), result_high, result_low);
    gpr8(X86::RegisterAH) = shadow_wrap_with_taint_from<u8>(result_high, src, al());
    gpr8(X86::RegisterAL) = shadow_wrap_with_taint_from<u8>(result_low, src, al());
}

void SoftCPU::IMUL_reg16_RM16(const X86::Instruction& insn)
{
    i16 result_high;
    i16 result_low;
    auto src = insn.modrm().read16(*this, insn);
    op_imul<i16>(*this, gpr16(insn.reg16()).value(), src.value(), result_high, result_low);
    gpr16(insn.reg16()) = shadow_wrap_with_taint_from<u16>(result_low, src, gpr16(insn.reg16()));
}

void SoftCPU::IMUL_reg16_RM16_imm16(const X86::Instruction& insn)
{
    i16 result_high;
    i16 result_low;
    auto src = insn.modrm().read16(*this, insn);
    op_imul<i16>(*this, src.value(), insn.imm16(), result_high, result_low);
    gpr16(insn.reg16()) = shadow_wrap_with_taint_from<u16>(result_low, src);
}

void SoftCPU::IMUL_reg16_RM16_imm8(const X86::Instruction& insn)
{
    i16 result_high;
    i16 result_low;
    auto src = insn.modrm().read16(*this, insn);
    op_imul<i16>(*this, src.value(), sign_extended_to<i16>(insn.imm8()), result_high, result_low);
    gpr16(insn.reg16()) = shadow_wrap_with_taint_from<u16>(result_low, src);
}

void SoftCPU::IMUL_reg32_RM32(const X86::Instruction& insn)
{
    i32 result_high;
    i32 result_low;
    auto src = insn.modrm().read32(*this, insn);
    op_imul<i32>(*this, gpr32(insn.reg32()).value(), src.value(), result_high, result_low);
    gpr32(insn.reg32()) = shadow_wrap_with_taint_from<u32>(result_low, src, gpr32(insn.reg32()));
}

void SoftCPU::IMUL_reg32_RM32_imm32(const X86::Instruction& insn)
{
    i32 result_high;
    i32 result_low;
    auto src = insn.modrm().read32(*this, insn);
    op_imul<i32>(*this, src.value(), insn.imm32(), result_high, result_low);
    gpr32(insn.reg32()) = shadow_wrap_with_taint_from<u32>(result_low, src);
}

void SoftCPU::IMUL_reg32_RM32_imm8(const X86::Instruction& insn)
{
    i32 result_high;
    i32 result_low;
    auto src = insn.modrm().read32(*this, insn);
    op_imul<i32>(*this, src.value(), sign_extended_to<i32>(insn.imm8()), result_high, result_low);
    gpr32(insn.reg32()) = shadow_wrap_with_taint_from<u32>(result_low, src);
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
    gpr16(insn.reg16()) = op_inc(*this, const_gpr16(insn.reg16()));
}

void SoftCPU::INC_reg32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = op_inc(*this, const_gpr32(insn.reg32()));
}

void SoftCPU::INSB(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::INSD(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::INSW(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::INT3(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::INTO(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::INT_imm8(const X86::Instruction& insn)
{
    VERIFY(insn.imm8() == 0x82);
    // FIXME: virt_syscall should take ValueWithShadow and whine about uninitialized arguments
    set_eax(shadow_wrap_as_initialized(m_emulator.virt_syscall(eax().value(), edx().value(), ecx().value(), ebx().value())));
}

void SoftCPU::INVLPG(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::IN_AL_DX(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::IN_AL_imm8(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::IN_AX_DX(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::IN_AX_imm8(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::IN_EAX_DX(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::IN_EAX_imm8(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::IRET(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::JCXZ_imm8(const X86::Instruction& insn)
{
    if (insn.a32()) {
        warn_if_uninitialized(ecx(), "jecxz imm8");
        if (ecx().value() == 0)
            set_eip(eip() + (i8)insn.imm8());
    } else {
        warn_if_uninitialized(cx(), "jcxz imm8");
        if (cx().value() == 0)
            set_eip(eip() + (i8)insn.imm8());
    }
}

void SoftCPU::JMP_FAR_mem16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::JMP_FAR_mem32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::JMP_RM16(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::JMP_RM32(const X86::Instruction& insn)
{
    set_eip(insn.modrm().read32(*this, insn).value());
}

void SoftCPU::JMP_imm16(const X86::Instruction& insn)
{
    set_eip(eip() + (i16)insn.imm16());
}

void SoftCPU::JMP_imm16_imm16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::JMP_imm16_imm32(const X86::Instruction&) { TODO_INSN(); }

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
    warn_if_flags_tainted("jcc near imm32");
    if (evaluate_condition(insn.cc()))
        set_eip(eip() + (i32)insn.imm32());
}

void SoftCPU::Jcc_imm8(const X86::Instruction& insn)
{
    warn_if_flags_tainted("jcc imm8");
    if (evaluate_condition(insn.cc()))
        set_eip(eip() + (i8)insn.imm8());
}

void SoftCPU::LAHF(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LAR_reg16_RM16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LAR_reg32_RM32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LDS_reg16_mem16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LDS_reg32_mem32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LEAVE16(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::LEAVE32(const X86::Instruction&)
{
    auto new_ebp = read_memory32({ ss(), ebp().value() });
    set_esp({ ebp().value() + 4, ebp().shadow() });
    set_ebp(new_ebp);
}

void SoftCPU::LEA_reg16_mem16(const X86::Instruction& insn)
{
    // FIXME: Respect shadow values
    gpr16(insn.reg16()) = shadow_wrap_as_initialized<u16>(insn.modrm().resolve(*this, insn).offset());
}

void SoftCPU::LEA_reg32_mem32(const X86::Instruction& insn)
{
    // FIXME: Respect shadow values
    gpr32(insn.reg32()) = shadow_wrap_as_initialized<u32>(insn.modrm().resolve(*this, insn).offset());
}

void SoftCPU::LES_reg16_mem16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LES_reg32_mem32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LFS_reg16_mem16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LFS_reg32_mem32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LGDT(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LGS_reg16_mem16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LGS_reg32_mem32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LIDT(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LLDT_RM16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LMSW_RM16(const X86::Instruction&) { TODO_INSN(); }

template<typename T>
ALWAYS_INLINE static void do_lods(SoftCPU& cpu, const X86::Instruction& insn)
{
    auto src_segment = cpu.segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS));
    cpu.do_once_or_repeat<true>(insn, [&] {
        auto src = cpu.read_memory<T>({ src_segment, cpu.source_index(insn.a32()).value() });
        cpu.gpr<T>(X86::RegisterAL) = src;
        cpu.step_source_index(insn.a32(), sizeof(T));
    });
}

void SoftCPU::LODSB(const X86::Instruction& insn)
{
    do_lods<u8>(*this, insn);
}

void SoftCPU::LODSD(const X86::Instruction& insn)
{
    do_lods<u32>(*this, insn);
}

void SoftCPU::LODSW(const X86::Instruction& insn)
{
    do_lods<u16>(*this, insn);
}

void SoftCPU::LOOPNZ_imm8(const X86::Instruction& insn)
{
    warn_if_flags_tainted("loopnz");
    if (insn.a32()) {
        set_ecx({ ecx().value() - 1, ecx().shadow() });
        if (ecx().value() != 0 && !zf())
            set_eip(eip() + (i8)insn.imm8());
    } else {
        set_cx({ (u16)(cx().value() - 1), cx().shadow() });
        if (cx().value() != 0 && !zf())
            set_eip(eip() + (i8)insn.imm8());
    }
}
void SoftCPU::LOOPZ_imm8(const X86::Instruction& insn)
{
    warn_if_flags_tainted("loopz");
    if (insn.a32()) {
        set_ecx({ ecx().value() - 1, ecx().shadow() });
        if (ecx().value() != 0 && zf())
            set_eip(eip() + (i8)insn.imm8());
    } else {
        set_cx({ (u16)(cx().value() - 1), cx().shadow() });
        if (cx().value() != 0 && zf())
            set_eip(eip() + (i8)insn.imm8());
    }
}

void SoftCPU::LOOP_imm8(const X86::Instruction& insn)
{
    if (insn.a32()) {
        set_ecx({ ecx().value() - 1, ecx().shadow() });
        if (ecx().value() != 0)
            set_eip(eip() + (i8)insn.imm8());
    } else {
        set_cx({ (u16)(cx().value() - 1), cx().shadow() });
        if (cx().value() != 0)
            set_eip(eip() + (i8)insn.imm8());
    }
}

void SoftCPU::LSL_reg16_RM16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LSL_reg32_RM32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LSS_reg16_mem16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LSS_reg32_mem32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::LTR_RM16(const X86::Instruction&) { TODO_INSN(); }

template<typename T>
ALWAYS_INLINE static void do_movs(SoftCPU& cpu, const X86::Instruction& insn)
{
    auto src_segment = cpu.segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS));
    cpu.do_once_or_repeat<false>(insn, [&] {
        auto src = cpu.read_memory<T>({ src_segment, cpu.source_index(insn.a32()).value() });
        cpu.write_memory<T>({ cpu.es(), cpu.destination_index(insn.a32()).value() }, src);
        cpu.step_source_index(insn.a32(), sizeof(T));
        cpu.step_destination_index(insn.a32(), sizeof(T));
    });
}

void SoftCPU::MOVSB(const X86::Instruction& insn)
{
    do_movs<u8>(*this, insn);
}

void SoftCPU::MOVSD(const X86::Instruction& insn)
{
    do_movs<u32>(*this, insn);
}

void SoftCPU::MOVSW(const X86::Instruction& insn)
{
    do_movs<u16>(*this, insn);
}

void SoftCPU::MOVSX_reg16_RM8(const X86::Instruction& insn)
{
    auto src = insn.modrm().read8(*this, insn);
    gpr16(insn.reg16()) = shadow_wrap_with_taint_from<u16>(sign_extended_to<u16>(src.value()), src);
}

void SoftCPU::MOVSX_reg32_RM16(const X86::Instruction& insn)
{
    auto src = insn.modrm().read16(*this, insn);
    gpr32(insn.reg32()) = shadow_wrap_with_taint_from<u32>(sign_extended_to<u32>(src.value()), src);
}

void SoftCPU::MOVSX_reg32_RM8(const X86::Instruction& insn)
{
    auto src = insn.modrm().read8(*this, insn);
    gpr32(insn.reg32()) = shadow_wrap_with_taint_from<u32>(sign_extended_to<u32>(src.value()), src);
}

void SoftCPU::MOVZX_reg16_RM8(const X86::Instruction& insn)
{
    auto src = insn.modrm().read8(*this, insn);
    gpr16(insn.reg16()) = ValueWithShadow<u16>(src.value(), 0x0100 | (src.shadow() & 0xff));
}

void SoftCPU::MOVZX_reg32_RM16(const X86::Instruction& insn)
{
    auto src = insn.modrm().read16(*this, insn);
    gpr32(insn.reg32()) = ValueWithShadow<u32>(src.value(), 0x01010000 | (src.shadow() & 0xffff));
}

void SoftCPU::MOVZX_reg32_RM8(const X86::Instruction& insn)
{
    auto src = insn.modrm().read8(*this, insn);
    gpr32(insn.reg32()) = ValueWithShadow<u32>(src.value(), 0x01010100 | (src.shadow() & 0xff));
}

void SoftCPU::MOV_AL_moff8(const X86::Instruction& insn)
{
    set_al(read_memory8({ segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS)), insn.imm_address() }));
}

void SoftCPU::MOV_AX_moff16(const X86::Instruction& insn)
{
    set_ax(read_memory16({ segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS)), insn.imm_address() }));
}

void SoftCPU::MOV_CR_reg32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::MOV_DR_reg32(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::MOV_EAX_moff32(const X86::Instruction& insn)
{
    set_eax(read_memory32({ segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS)), insn.imm_address() }));
}

void SoftCPU::MOV_RM16_imm16(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, shadow_wrap_as_initialized(insn.imm16()));
}

void SoftCPU::MOV_RM16_reg16(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, const_gpr16(insn.reg16()));
}

void SoftCPU::MOV_RM16_seg(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::MOV_RM32_imm32(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, shadow_wrap_as_initialized(insn.imm32()));
}

void SoftCPU::MOV_RM32_reg32(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, const_gpr32(insn.reg32()));
}

void SoftCPU::MOV_RM8_imm8(const X86::Instruction& insn)
{
    insn.modrm().write8(*this, insn, shadow_wrap_as_initialized(insn.imm8()));
}

void SoftCPU::MOV_RM8_reg8(const X86::Instruction& insn)
{
    insn.modrm().write8(*this, insn, const_gpr8(insn.reg8()));
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
    gpr16(insn.reg16()) = shadow_wrap_as_initialized(insn.imm16());
}

void SoftCPU::MOV_reg32_CR(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::MOV_reg32_DR(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::MOV_reg32_RM32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = insn.modrm().read32(*this, insn);
}

void SoftCPU::MOV_reg32_imm32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = shadow_wrap_as_initialized(insn.imm32());
}

void SoftCPU::MOV_reg8_RM8(const X86::Instruction& insn)
{
    gpr8(insn.reg8()) = insn.modrm().read8(*this, insn);
}

void SoftCPU::MOV_reg8_imm8(const X86::Instruction& insn)
{
    gpr8(insn.reg8()) = shadow_wrap_as_initialized(insn.imm8());
}

void SoftCPU::MOV_seg_RM16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::MOV_seg_RM32(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::MUL_RM16(const X86::Instruction& insn)
{
    auto src = insn.modrm().read16(*this, insn);
    u32 result = (u32)ax().value() * (u32)src.value();
    auto original_ax = ax();
    set_ax(shadow_wrap_with_taint_from<u16>(result & 0xffff, src, original_ax));
    set_dx(shadow_wrap_with_taint_from<u16>(result >> 16, src, original_ax));
    taint_flags_from(src, original_ax);

    set_cf(dx().value() != 0);
    set_of(dx().value() != 0);
}

void SoftCPU::MUL_RM32(const X86::Instruction& insn)
{
    auto src = insn.modrm().read32(*this, insn);
    u64 result = (u64)eax().value() * (u64)src.value();
    auto original_eax = eax();
    set_eax(shadow_wrap_with_taint_from<u32>(result, src, original_eax));
    set_edx(shadow_wrap_with_taint_from<u32>(result >> 32, src, original_eax));
    taint_flags_from(src, original_eax);

    set_cf(edx().value() != 0);
    set_of(edx().value() != 0);
}

void SoftCPU::MUL_RM8(const X86::Instruction& insn)
{
    auto src = insn.modrm().read8(*this, insn);
    u16 result = (u16)al().value() * src.value();
    auto original_al = al();
    set_ax(shadow_wrap_with_taint_from(result, src, original_al));
    taint_flags_from(src, original_al);

    set_cf((result & 0xff00) != 0);
    set_of((result & 0xff00) != 0);
}

void SoftCPU::NEG_RM16(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, op_sub<ValueWithShadow<u16>>(*this, shadow_wrap_as_initialized<u16>(0), insn.modrm().read16(*this, insn)));
}

void SoftCPU::NEG_RM32(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, op_sub<ValueWithShadow<u32>>(*this, shadow_wrap_as_initialized<u32>(0), insn.modrm().read32(*this, insn)));
}

void SoftCPU::NEG_RM8(const X86::Instruction& insn)
{
    insn.modrm().write8(*this, insn, op_sub<ValueWithShadow<u8>>(*this, shadow_wrap_as_initialized<u8>(0), insn.modrm().read8(*this, insn)));
}

void SoftCPU::NOP(const X86::Instruction&)
{
}

void SoftCPU::NOT_RM16(const X86::Instruction& insn)
{
    auto data = insn.modrm().read16(*this, insn);
    insn.modrm().write16(*this, insn, ValueWithShadow<u16>(~data.value(), data.shadow()));
}

void SoftCPU::NOT_RM32(const X86::Instruction& insn)
{
    auto data = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, ValueWithShadow<u32>(~data.value(), data.shadow()));
}

void SoftCPU::NOT_RM8(const X86::Instruction& insn)
{
    auto data = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, ValueWithShadow<u8>(~data.value(), data.shadow()));
}

void SoftCPU::OUTSB(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::OUTSD(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::OUTSW(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::OUT_DX_AL(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::OUT_DX_AX(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::OUT_DX_EAX(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::OUT_imm8_AL(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::OUT_imm8_AX(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::OUT_imm8_EAX(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PADDB_mm1_mm2m64(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PADDW_mm1_mm2m64(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PADDD_mm1_mm2m64(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::POPA(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::POPAD(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::POPF(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::POPFD(const X86::Instruction&)
{
    auto popped_value = pop32();
    m_eflags &= ~0x00fcffff;
    m_eflags |= popped_value.value() & 0x00fcffff;
    taint_flags_from(popped_value);
}

void SoftCPU::POP_DS(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::POP_ES(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::POP_FS(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::POP_GS(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::POP_RM16(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, pop16());
}

void SoftCPU::POP_RM32(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, pop32());
}

void SoftCPU::POP_SS(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::POP_reg16(const X86::Instruction& insn)
{
    gpr16(insn.reg16()) = pop16();
}

void SoftCPU::POP_reg32(const X86::Instruction& insn)
{
    gpr32(insn.reg32()) = pop32();
}

void SoftCPU::PUSHA(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PUSHAD(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PUSHF(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::PUSHFD(const X86::Instruction&)
{
    // FIXME: Respect shadow flags when they exist!
    push32(shadow_wrap_as_initialized(m_eflags & 0x00fcffff));
}

void SoftCPU::PUSH_CS(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PUSH_DS(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PUSH_ES(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PUSH_FS(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PUSH_GS(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PUSH_RM16(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::PUSH_RM32(const X86::Instruction& insn)
{
    push32(insn.modrm().read32(*this, insn));
}

void SoftCPU::PUSH_SP_8086_80186(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::PUSH_SS(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::PUSH_imm16(const X86::Instruction& insn)
{
    push16(shadow_wrap_as_initialized(insn.imm16()));
}

void SoftCPU::PUSH_imm32(const X86::Instruction& insn)
{
    push32(shadow_wrap_as_initialized(insn.imm32()));
}

void SoftCPU::PUSH_imm8(const X86::Instruction& insn)
{
    VERIFY(!insn.has_operand_size_override_prefix());
    push32(shadow_wrap_as_initialized<u32>(sign_extended_to<i32>(insn.imm8())));
}

void SoftCPU::PUSH_reg16(const X86::Instruction& insn)
{
    push16(gpr16(insn.reg16()));
}

void SoftCPU::PUSH_reg32(const X86::Instruction& insn)
{
    push32(gpr32(insn.reg32()));
}

template<typename T, bool cf>
ALWAYS_INLINE static T op_rcl_impl(SoftCPU& cpu, T data, ValueWithShadow<u8> steps)
{
    if (steps.value() == 0)
        return shadow_wrap_with_taint_from(data.value(), data, steps);

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (cf)
        asm volatile("stc");
    else
        asm volatile("clc");

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("rcll %%cl, %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("rclw %%cl, %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("rclb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oc(new_flags);
    cpu.taint_flags_from(data, steps);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, data, steps);
}

template<typename T>
ALWAYS_INLINE static T op_rcl(SoftCPU& cpu, T data, ValueWithShadow<u8> steps)
{
    cpu.warn_if_flags_tainted("rcl");
    if (cpu.cf())
        return op_rcl_impl<T, true>(cpu, data, steps);
    return op_rcl_impl<T, false>(cpu, data, steps);
}

DEFINE_GENERIC_SHIFT_ROTATE_INSN_HANDLERS(RCL, op_rcl)

template<typename T, bool cf>
ALWAYS_INLINE static T op_rcr_impl(SoftCPU& cpu, T data, ValueWithShadow<u8> steps)
{
    if (steps.value() == 0)
        return shadow_wrap_with_taint_from(data.value(), data, steps);

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (cf)
        asm volatile("stc");
    else
        asm volatile("clc");

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("rcrl %%cl, %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("rcrw %%cl, %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("rcrb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oc(new_flags);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, data, steps);
}

template<typename T>
ALWAYS_INLINE static T op_rcr(SoftCPU& cpu, T data, ValueWithShadow<u8> steps)
{
    cpu.warn_if_flags_tainted("rcr");
    if (cpu.cf())
        return op_rcr_impl<T, true>(cpu, data, steps);
    return op_rcr_impl<T, false>(cpu, data, steps);
}

DEFINE_GENERIC_SHIFT_ROTATE_INSN_HANDLERS(RCR, op_rcr)

void SoftCPU::RDTSC(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::RET(const X86::Instruction& insn)
{
    VERIFY(!insn.has_operand_size_override_prefix());
    auto ret_address = pop32();
    warn_if_uninitialized(ret_address, "ret");
    set_eip(ret_address.value());
}

void SoftCPU::RETF(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::RETF_imm16(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::RET_imm16(const X86::Instruction& insn)
{
    VERIFY(!insn.has_operand_size_override_prefix());
    auto ret_address = pop32();
    warn_if_uninitialized(ret_address, "ret imm16");
    set_eip(ret_address.value());
    set_esp({ esp().value() + insn.imm16(), esp().shadow() });
}

template<typename T>
ALWAYS_INLINE static T op_rol(SoftCPU& cpu, T data, ValueWithShadow<u8> steps)
{
    if (steps.value() == 0)
        return shadow_wrap_with_taint_from(data.value(), data, steps);

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("roll %%cl, %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("rolw %%cl, %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("rolb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oc(new_flags);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, data, steps);
}

DEFINE_GENERIC_SHIFT_ROTATE_INSN_HANDLERS(ROL, op_rol)

template<typename T>
ALWAYS_INLINE static T op_ror(SoftCPU& cpu, T data, ValueWithShadow<u8> steps)
{
    if (steps.value() == 0)
        return shadow_wrap_with_taint_from(data.value(), data, steps);

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("rorl %%cl, %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("rorw %%cl, %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("rorb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oc(new_flags);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, data, steps);
}

DEFINE_GENERIC_SHIFT_ROTATE_INSN_HANDLERS(ROR, op_ror)

void SoftCPU::SAHF(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::SALC(const X86::Instruction&)
{
    // FIXME: Respect shadow flags once they exists!
    set_al(shadow_wrap_as_initialized<u8>(cf() ? 0xff : 0x00));
}

template<typename T>
static T op_sar(SoftCPU& cpu, T data, ValueWithShadow<u8> steps)
{
    if (steps.value() == 0)
        return shadow_wrap_with_taint_from(data.value(), data, steps);

    u32 result = 0;
    u32 new_flags = 0;

    if constexpr (sizeof(typename T::ValueType) == 4) {
        asm volatile("sarl %%cl, %%eax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 2) {
        asm volatile("sarw %%cl, %%ax\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    } else if constexpr (sizeof(typename T::ValueType) == 1) {
        asm volatile("sarb %%cl, %%al\n"
                     : "=a"(result)
                     : "a"(data.value()), "c"(steps.value()));
    }

    asm volatile(
        "pushf\n"
        "pop %%ebx"
        : "=b"(new_flags));

    cpu.set_flags_oszapc(new_flags);
    return shadow_wrap_with_taint_from<typename T::ValueType>(result, data, steps);
}

DEFINE_GENERIC_SHIFT_ROTATE_INSN_HANDLERS(SAR, op_sar)

template<typename T>
ALWAYS_INLINE static void do_scas(SoftCPU& cpu, const X86::Instruction& insn)
{
    cpu.do_once_or_repeat<true>(insn, [&] {
        auto src = cpu.const_gpr<T>(X86::RegisterAL);
        auto dest = cpu.read_memory<T>({ cpu.es(), cpu.destination_index(insn.a32()).value() });
        op_sub(cpu, dest, src);
        cpu.step_destination_index(insn.a32(), sizeof(T));
    });
}

void SoftCPU::SCASB(const X86::Instruction& insn)
{
    do_scas<u8>(*this, insn);
}

void SoftCPU::SCASD(const X86::Instruction& insn)
{
    do_scas<u32>(*this, insn);
}

void SoftCPU::SCASW(const X86::Instruction& insn)
{
    do_scas<u16>(*this, insn);
}

void SoftCPU::SETcc_RM8(const X86::Instruction& insn)
{
    warn_if_flags_tainted("setcc");
    insn.modrm().write8(*this, insn, shadow_wrap_as_initialized<u8>(evaluate_condition(insn.cc())));
}

void SoftCPU::SGDT(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::SHLD_RM16_reg16_CL(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, op_shld(*this, insn.modrm().read16(*this, insn), const_gpr16(insn.reg16()), cl()));
}

void SoftCPU::SHLD_RM16_reg16_imm8(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, op_shld(*this, insn.modrm().read16(*this, insn), const_gpr16(insn.reg16()), shadow_wrap_as_initialized(insn.imm8())));
}

void SoftCPU::SHLD_RM32_reg32_CL(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, op_shld(*this, insn.modrm().read32(*this, insn), const_gpr32(insn.reg32()), cl()));
}

void SoftCPU::SHLD_RM32_reg32_imm8(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, op_shld(*this, insn.modrm().read32(*this, insn), const_gpr32(insn.reg32()), shadow_wrap_as_initialized(insn.imm8())));
}

DEFINE_GENERIC_SHIFT_ROTATE_INSN_HANDLERS(SHL, op_shl)

void SoftCPU::SHRD_RM16_reg16_CL(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, op_shrd(*this, insn.modrm().read16(*this, insn), const_gpr16(insn.reg16()), cl()));
}

void SoftCPU::SHRD_RM16_reg16_imm8(const X86::Instruction& insn)
{
    insn.modrm().write16(*this, insn, op_shrd(*this, insn.modrm().read16(*this, insn), const_gpr16(insn.reg16()), shadow_wrap_as_initialized(insn.imm8())));
}

void SoftCPU::SHRD_RM32_reg32_CL(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, op_shrd(*this, insn.modrm().read32(*this, insn), const_gpr32(insn.reg32()), cl()));
}

void SoftCPU::SHRD_RM32_reg32_imm8(const X86::Instruction& insn)
{
    insn.modrm().write32(*this, insn, op_shrd(*this, insn.modrm().read32(*this, insn), const_gpr32(insn.reg32()), shadow_wrap_as_initialized(insn.imm8())));
}

DEFINE_GENERIC_SHIFT_ROTATE_INSN_HANDLERS(SHR, op_shr)

void SoftCPU::SIDT(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::SLDT_RM16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::SMSW_RM16(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::STC(const X86::Instruction&)
{
    set_cf(true);
}

void SoftCPU::STD(const X86::Instruction&)
{
    set_df(true);
}

void SoftCPU::STI(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::STOSB(const X86::Instruction& insn)
{
    if (insn.has_rep_prefix() && !df()) {
        // Fast path for 8-bit forward memory fill.
        if (m_emulator.mmu().fast_fill_memory8({ es(), destination_index(insn.a32()).value() }, ecx().value(), al())) {
            if (insn.a32()) {
                // FIXME: Should an uninitialized ECX taint EDI here?
                set_edi({ (u32)(edi().value() + ecx().value()), edi().shadow() });
                set_ecx(shadow_wrap_as_initialized<u32>(0));
            } else {
                // FIXME: Should an uninitialized CX taint DI here?
                set_di({ (u16)(di().value() + cx().value()), di().shadow() });
                set_cx(shadow_wrap_as_initialized<u16>(0));
            }
            return;
        }
    }

    do_once_or_repeat<false>(insn, [&] {
        write_memory8({ es(), destination_index(insn.a32()).value() }, al());
        step_destination_index(insn.a32(), 1);
    });
}

void SoftCPU::STOSD(const X86::Instruction& insn)
{
    if (insn.has_rep_prefix() && !df()) {
        // Fast path for 32-bit forward memory fill.
        if (m_emulator.mmu().fast_fill_memory32({ es(), destination_index(insn.a32()).value() }, ecx().value(), eax())) {
            if (insn.a32()) {
                // FIXME: Should an uninitialized ECX taint EDI here?
                set_edi({ (u32)(edi().value() + (ecx().value() * sizeof(u32))), edi().shadow() });
                set_ecx(shadow_wrap_as_initialized<u32>(0));
            } else {
                // FIXME: Should an uninitialized CX taint DI here?
                set_di({ (u16)(di().value() + (cx().value() * sizeof(u32))), di().shadow() });
                set_cx(shadow_wrap_as_initialized<u16>(0));
            }
            return;
        }
    }

    do_once_or_repeat<false>(insn, [&] {
        write_memory32({ es(), destination_index(insn.a32()).value() }, eax());
        step_destination_index(insn.a32(), 4);
    });
}

void SoftCPU::STOSW(const X86::Instruction& insn)
{
    do_once_or_repeat<false>(insn, [&] {
        write_memory16({ es(), destination_index(insn.a32()).value() }, ax());
        step_destination_index(insn.a32(), 2);
    });
}

void SoftCPU::STR_RM16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::UD0(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::UD1(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::UD2(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::VERR_RM16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::VERW_RM16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::WAIT(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::WBINVD(const X86::Instruction&) { TODO_INSN(); }

void SoftCPU::XADD_RM16_reg16(const X86::Instruction& insn)
{
    auto dest = insn.modrm().read16(*this, insn);
    auto src = const_gpr16(insn.reg16());
    auto result = op_add(*this, dest, src);
    gpr16(insn.reg16()) = dest;
    insn.modrm().write16(*this, insn, result);
}

void SoftCPU::XADD_RM32_reg32(const X86::Instruction& insn)
{
    auto dest = insn.modrm().read32(*this, insn);
    auto src = const_gpr32(insn.reg32());
    auto result = op_add(*this, dest, src);
    gpr32(insn.reg32()) = dest;
    insn.modrm().write32(*this, insn, result);
}

void SoftCPU::XADD_RM8_reg8(const X86::Instruction& insn)
{
    auto dest = insn.modrm().read8(*this, insn);
    auto src = const_gpr8(insn.reg8());
    auto result = op_add(*this, dest, src);
    gpr8(insn.reg8()) = dest;
    insn.modrm().write8(*this, insn, result);
}

void SoftCPU::XCHG_AX_reg16(const X86::Instruction& insn)
{
    auto temp = gpr16(insn.reg16());
    gpr16(insn.reg16()) = ax();
    set_ax(temp);
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
    insn.modrm().write16(*this, insn, const_gpr16(insn.reg16()));
    gpr16(insn.reg16()) = temp;
}

void SoftCPU::XCHG_reg32_RM32(const X86::Instruction& insn)
{
    auto temp = insn.modrm().read32(*this, insn);
    insn.modrm().write32(*this, insn, const_gpr32(insn.reg32()));
    gpr32(insn.reg32()) = temp;
}

void SoftCPU::XCHG_reg8_RM8(const X86::Instruction& insn)
{
    auto temp = insn.modrm().read8(*this, insn);
    insn.modrm().write8(*this, insn, const_gpr8(insn.reg8()));
    gpr8(insn.reg8()) = temp;
}

void SoftCPU::XLAT(const X86::Instruction& insn)
{
    if (insn.a32())
        warn_if_uninitialized(ebx(), "xlat ebx");
    else
        warn_if_uninitialized(bx(), "xlat bx");
    warn_if_uninitialized(al(), "xlat al");
    u32 offset = (insn.a32() ? ebx().value() : bx().value()) + al().value();
    set_al(read_memory8({ segment(insn.segment_prefix().value_or(X86::SegmentRegister::DS)), offset }));
}

#define DEFINE_GENERIC_INSN_HANDLERS_PARTIAL(mnemonic, op, update_dest, is_zero_idiom_if_both_operands_same, is_or)                                                             \
    void SoftCPU::mnemonic##_AL_imm8(const X86::Instruction& insn) { generic_AL_imm8<update_dest, is_or>(op<ValueWithShadow<u8>>, insn); }                                      \
    void SoftCPU::mnemonic##_AX_imm16(const X86::Instruction& insn) { generic_AX_imm16<update_dest, is_or>(op<ValueWithShadow<u16>>, insn); }                                   \
    void SoftCPU::mnemonic##_EAX_imm32(const X86::Instruction& insn) { generic_EAX_imm32<update_dest, is_or>(op<ValueWithShadow<u32>>, insn); }                                 \
    void SoftCPU::mnemonic##_RM16_imm16(const X86::Instruction& insn) { generic_RM16_imm16<update_dest, is_or>(op<ValueWithShadow<u16>>, insn); }                               \
    void SoftCPU::mnemonic##_RM16_reg16(const X86::Instruction& insn) { generic_RM16_reg16<update_dest, is_zero_idiom_if_both_operands_same>(op<ValueWithShadow<u16>>, insn); } \
    void SoftCPU::mnemonic##_RM32_imm32(const X86::Instruction& insn) { generic_RM32_imm32<update_dest, is_or>(op<ValueWithShadow<u32>>, insn); }                               \
    void SoftCPU::mnemonic##_RM32_reg32(const X86::Instruction& insn) { generic_RM32_reg32<update_dest, is_zero_idiom_if_both_operands_same>(op<ValueWithShadow<u32>>, insn); } \
    void SoftCPU::mnemonic##_RM8_imm8(const X86::Instruction& insn) { generic_RM8_imm8<update_dest, is_or>(op<ValueWithShadow<u8>>, insn); }                                    \
    void SoftCPU::mnemonic##_RM8_reg8(const X86::Instruction& insn) { generic_RM8_reg8<update_dest, is_zero_idiom_if_both_operands_same>(op<ValueWithShadow<u8>>, insn); }

#define DEFINE_GENERIC_INSN_HANDLERS(mnemonic, op, update_dest, is_zero_idiom_if_both_operands_same, is_or)                                                                     \
    DEFINE_GENERIC_INSN_HANDLERS_PARTIAL(mnemonic, op, update_dest, is_zero_idiom_if_both_operands_same, is_or)                                                                 \
    void SoftCPU::mnemonic##_RM16_imm8(const X86::Instruction& insn) { generic_RM16_imm8<update_dest, is_or>(op<ValueWithShadow<u16>>, insn); }                                 \
    void SoftCPU::mnemonic##_RM32_imm8(const X86::Instruction& insn) { generic_RM32_imm8<update_dest, is_or>(op<ValueWithShadow<u32>>, insn); }                                 \
    void SoftCPU::mnemonic##_reg16_RM16(const X86::Instruction& insn) { generic_reg16_RM16<update_dest, is_zero_idiom_if_both_operands_same>(op<ValueWithShadow<u16>>, insn); } \
    void SoftCPU::mnemonic##_reg32_RM32(const X86::Instruction& insn) { generic_reg32_RM32<update_dest, is_zero_idiom_if_both_operands_same>(op<ValueWithShadow<u32>>, insn); } \
    void SoftCPU::mnemonic##_reg8_RM8(const X86::Instruction& insn) { generic_reg8_RM8<update_dest, is_zero_idiom_if_both_operands_same>(op<ValueWithShadow<u8>>, insn); }

DEFINE_GENERIC_INSN_HANDLERS(XOR, op_xor, true, true, false)
DEFINE_GENERIC_INSN_HANDLERS(OR, op_or, true, false, true)
DEFINE_GENERIC_INSN_HANDLERS(ADD, op_add, true, false, false)
DEFINE_GENERIC_INSN_HANDLERS(ADC, op_adc, true, false, false)
DEFINE_GENERIC_INSN_HANDLERS(SUB, op_sub, true, true, false)
DEFINE_GENERIC_INSN_HANDLERS(SBB, op_sbb, true, false, false)
DEFINE_GENERIC_INSN_HANDLERS(AND, op_and, true, false, false)
DEFINE_GENERIC_INSN_HANDLERS(CMP, op_sub, false, false, false)
DEFINE_GENERIC_INSN_HANDLERS_PARTIAL(TEST, op_and, false, false, false)

void SoftCPU::MOVQ_mm1_mm2m64(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::EMMS(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::MOVQ_mm1_m64_mm2(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::wrap_0xC0(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::wrap_0xC1_16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::wrap_0xC1_32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::wrap_0xD0(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::wrap_0xD1_16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::wrap_0xD1_32(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::wrap_0xD2(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::wrap_0xD3_16(const X86::Instruction&) { TODO_INSN(); }
void SoftCPU::wrap_0xD3_32(const X86::Instruction&) { TODO_INSN(); }
}
