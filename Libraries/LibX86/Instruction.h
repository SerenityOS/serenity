/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Optional.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <stdio.h>

namespace X86 {

class Instruction;
class Interpreter;
typedef void (Interpreter::*InstructionHandler)(const Instruction&);

class SymbolProvider {
public:
    virtual String symbolicate(FlatPtr, u32* offset = nullptr) const = 0;
};

template<typename T>
struct TypeTrivia {
    static const size_t bits = sizeof(T) * 8;
    static const T sign_bit = 1 << (bits - 1);
    static const T mask = typename MakeUnsigned<T>::Type(-1);
};

template<typename T, typename U>
inline constexpr T sign_extended_to(U value)
{
    if (!(value & TypeTrivia<U>::sign_bit))
        return value;
    return (TypeTrivia<T>::mask & ~TypeTrivia<U>::mask) | value;
}

enum IsLockPrefixAllowed {
    LockPrefixNotAllowed = 0,
    LockPrefixAllowed
};

enum InstructionFormat {
    InvalidFormat,
    MultibyteWithSlash,
    InstructionPrefix,

    __BeginFormatsWithRMByte,
    OP_RM16_reg16,
    OP_reg8_RM8,
    OP_reg16_RM16,
    OP_RM16_seg,
    OP_RM32_seg,
    OP_RM8_imm8,
    OP_RM16_imm16,
    OP_RM16_imm8,
    OP_RM32_imm8,
    OP_RM8,
    OP_RM16,
    OP_RM32,
    OP_FPU,
    OP_FPU_reg,
    OP_FPU_mem,
    OP_FPU_AX16,
    OP_FPU_RM16,
    OP_FPU_RM32,
    OP_FPU_RM64,
    OP_FPU_M80,
    OP_RM8_reg8,
    OP_RM32_reg32,
    OP_reg32_RM32,
    OP_RM32_imm32,
    OP_reg16_RM16_imm8,
    OP_reg32_RM32_imm8,
    OP_reg16_RM16_imm16,
    OP_reg32_RM32_imm32,
    OP_reg16_mem16,
    OP_reg32_mem32,
    OP_seg_RM16,
    OP_seg_RM32,
    OP_RM8_1,
    OP_RM16_1,
    OP_RM32_1,
    OP_FAR_mem16,
    OP_FAR_mem32,
    OP_RM8_CL,
    OP_RM16_CL,
    OP_RM32_CL,
    OP_reg32_CR,
    OP_CR_reg32,
    OP_reg32_DR,
    OP_DR_reg32,
    OP_reg16_RM8,
    OP_reg32_RM8,
    OP_reg32_RM16,
    OP_RM16_reg16_imm8,
    OP_RM32_reg32_imm8,
    OP_RM16_reg16_CL,
    OP_RM32_reg32_CL,
    OP_mm1_mm2m64,
    OP_mm1m64_mm2,
    __EndFormatsWithRMByte,

    OP_reg32_imm32,
    OP_AL_imm8,
    OP_AX_imm16,
    OP_EAX_imm32,
    OP_CS,
    OP_DS,
    OP_ES,
    OP_SS,
    OP_FS,
    OP_GS,
    OP,
    OP_reg16,
    OP_imm16,
    OP_relimm16,
    OP_relimm32,
    OP_imm8,
    OP_imm16_imm16,
    OP_imm16_imm32,
    OP_AX_reg16,
    OP_EAX_reg32,
    OP_AL_moff8,
    OP_AX_moff16,
    OP_EAX_moff32,
    OP_moff8_AL,
    OP_moff16_AX,
    OP_moff32_EAX,
    OP_reg8_imm8,
    OP_reg16_imm16,
    OP_3,
    OP_AX_imm8,
    OP_EAX_imm8,
    OP_short_imm8,
    OP_AL_DX,
    OP_AX_DX,
    OP_EAX_DX,
    OP_DX_AL,
    OP_DX_AX,
    OP_DX_EAX,
    OP_imm8_AL,
    OP_imm8_AX,
    OP_imm8_EAX,
    OP_reg8_CL,

    OP_reg32,
    OP_imm32,
    OP_imm16_imm8,

    OP_NEAR_imm,
};

static const unsigned CurrentAddressSize = 0xB33FBABE;

struct InstructionDescriptor {
    InstructionHandler handler { nullptr };
    bool opcode_has_register_index { false };
    const char* mnemonic { nullptr };
    InstructionFormat format { InvalidFormat };
    bool has_rm { false };
    unsigned imm1_bytes { 0 };
    unsigned imm2_bytes { 0 };

    // Addressed by the 3 REG bits in the MOD-REG-R/M byte.
    // Some slash instructions have further subgroups when MOD is 11,
    // in that case the InstructionDescriptors in slashes have themselves
    // a non-null slashes member that's indexed by the three R/M bits.
    InstructionDescriptor* slashes { nullptr };

    unsigned imm1_bytes_for_address_size(bool a32)
    {
        if (imm1_bytes == CurrentAddressSize)
            return a32 ? 4 : 2;
        return imm1_bytes;
    }

    unsigned imm2_bytes_for_address_size(bool a32)
    {
        if (imm2_bytes == CurrentAddressSize)
            return a32 ? 4 : 2;
        return imm2_bytes;
    }

    IsLockPrefixAllowed lock_prefix_allowed { LockPrefixNotAllowed };
};

extern InstructionDescriptor s_table16[256];
extern InstructionDescriptor s_table32[256];
extern InstructionDescriptor s_0f_table16[256];
extern InstructionDescriptor s_0f_table32[256];

struct Prefix {
    enum Op {
        OperandSizeOverride = 0x66,
        AddressSizeOverride = 0x67,
        REP = 0xf3,
        REPZ = 0xf3,
        REPNZ = 0xf2,
        LOCK = 0xf0,
    };
};

enum class SegmentRegister {
    ES = 0,
    CS,
    SS,
    DS,
    FS,
    GS,
    SegR6,
    SegR7,
};

enum RegisterIndex8 {
    RegisterAL = 0,
    RegisterCL,
    RegisterDL,
    RegisterBL,
    RegisterAH,
    RegisterCH,
    RegisterDH,
    RegisterBH
};

enum RegisterIndex16 {
    RegisterAX = 0,
    RegisterCX,
    RegisterDX,
    RegisterBX,
    RegisterSP,
    RegisterBP,
    RegisterSI,
    RegisterDI
};

enum RegisterIndex32 {
    RegisterEAX = 0,
    RegisterECX,
    RegisterEDX,
    RegisterEBX,
    RegisterESP,
    RegisterEBP,
    RegisterESI,
    RegisterEDI
};

enum FpuRegisterIndex {
    ST0 = 0,
    ST1,
    ST2,
    ST3,
    ST4,
    ST5,
    ST6,
    ST7
};

enum MMXRegisterIndex {
    RegisterMM0 = 0,
    RegisterMM1,
    RegisterMM2,
    RegisterMM3,
    RegisterMM4,
    RegisterMM5,
    RegisterMM6,
    RegisterMM7
};

class LogicalAddress {
public:
    LogicalAddress() { }
    LogicalAddress(u16 selector, u32 offset)
        : m_selector(selector)
        , m_offset(offset)
    {
    }

    u16 selector() const { return m_selector; }
    u32 offset() const { return m_offset; }
    void set_selector(u16 selector) { m_selector = selector; }
    void set_offset(u32 offset) { m_offset = offset; }

private:
    u16 m_selector { 0 };
    u32 m_offset { 0 };
};

class InstructionStream {
public:
    virtual bool can_read() = 0;
    virtual u8 read8() = 0;
    virtual u16 read16() = 0;
    virtual u32 read32() = 0;
    virtual u64 read64() = 0;
};

class SimpleInstructionStream final : public InstructionStream {
public:
    SimpleInstructionStream(const u8* data, size_t size)
        : m_data(data)
        , m_size(size)
    {
    }

    virtual bool can_read() override { return m_offset < m_size; }

    virtual u8 read8() override
    {
        if (!can_read())
            return 0;
        return m_data[m_offset++];
    }
    virtual u16 read16() override
    {
        u8 lsb = read8();
        u8 msb = read8();
        return ((u16)msb << 8) | (u16)lsb;
    }

    virtual u32 read32() override
    {
        u16 lsw = read16();
        u16 msw = read16();
        return ((u32)msw << 16) | (u32)lsw;
    }

    virtual u64 read64() override
    {
        u32 lsw = read32();
        u32 msw = read32();
        return ((u64)msw << 32) | (u64)lsw;
    }
    size_t offset() const { return m_offset; }

private:
    const u8* m_data { nullptr };
    size_t m_offset { 0 };
    size_t m_size { 0 };
};

class MemoryOrRegisterReference {
    friend class Instruction;

public:
    String to_string_o8(const Instruction&) const;
    String to_string_o16(const Instruction&) const;
    String to_string_o32(const Instruction&) const;
    String to_string_fpu_reg() const;
    String to_string_fpu_mem(const Instruction&) const;
    String to_string_fpu_ax16() const;
    String to_string_fpu16(const Instruction&) const;
    String to_string_fpu32(const Instruction&) const;
    String to_string_fpu64(const Instruction&) const;
    String to_string_fpu80(const Instruction&) const;
    String to_string_mm(const Instruction&) const;

    bool is_register() const { return m_register_index != 0xffffffff; }

    unsigned register_index() const { return m_register_index; }
    RegisterIndex32 reg32() const { return static_cast<RegisterIndex32>(register_index()); }
    RegisterIndex16 reg16() const { return static_cast<RegisterIndex16>(register_index()); }
    RegisterIndex8 reg8() const { return static_cast<RegisterIndex8>(register_index()); }
    FpuRegisterIndex reg_fpu() const { return static_cast<FpuRegisterIndex>(register_index()); }

    template<typename CPU, typename T>
    void write8(CPU&, const Instruction&, T);
    template<typename CPU, typename T>
    void write16(CPU&, const Instruction&, T);
    template<typename CPU, typename T>
    void write32(CPU&, const Instruction&, T);
    template<typename CPU, typename T>
    void write64(CPU&, const Instruction&, T);

    template<typename CPU>
    typename CPU::ValueWithShadowType8 read8(CPU&, const Instruction&);
    template<typename CPU>
    typename CPU::ValueWithShadowType16 read16(CPU&, const Instruction&);
    template<typename CPU>
    typename CPU::ValueWithShadowType32 read32(CPU&, const Instruction&);
    template<typename CPU>
    typename CPU::ValueWithShadowType64 read64(CPU&, const Instruction&);

    template<typename CPU>
    LogicalAddress resolve(const CPU&, const Instruction&);

private:
    MemoryOrRegisterReference() { }

    String to_string(const Instruction&) const;
    String to_string_a16() const;
    String to_string_a32() const;

    template<typename InstructionStreamType>
    void decode(InstructionStreamType&, bool a32);
    template<typename InstructionStreamType>
    void decode16(InstructionStreamType&);
    template<typename InstructionStreamType>
    void decode32(InstructionStreamType&);

    template<typename CPU>
    LogicalAddress resolve16(const CPU&, Optional<SegmentRegister>);
    template<typename CPU>
    LogicalAddress resolve32(const CPU&, Optional<SegmentRegister>);

    template<typename CPU>
    u32 evaluate_sib(const CPU&, SegmentRegister& default_segment) const;

    unsigned m_register_index { 0xffffffff };
    union {
        u32 m_offset32 { 0 };
        u16 m_offset16;
    };

    u8 m_rm { 0 };
    u8 m_sib { 0 };
    u8 m_displacement_bytes { 0 };

    union {
        u32 m_displacement32 { 0 };
        u16 m_displacement16;
    };

    bool m_has_sib { false };
};

class Instruction {
public:
    template<typename InstructionStreamType>
    static Instruction from_stream(InstructionStreamType&, bool o32, bool a32);
    ~Instruction() { }

    ALWAYS_INLINE MemoryOrRegisterReference& modrm() const
    {
        ASSERT(has_rm());
        return m_modrm;
    }

    ALWAYS_INLINE InstructionHandler handler() const { return m_descriptor->handler; }

    bool has_segment_prefix() const { return m_segment_prefix.has_value(); }
    Optional<SegmentRegister> segment_prefix() const { return m_segment_prefix; }

    bool has_address_size_override_prefix() const { return m_has_address_size_override_prefix; }
    bool has_operand_size_override_prefix() const { return m_has_operand_size_override_prefix; }
    bool has_lock_prefix() const { return m_has_lock_prefix; }
    bool has_rep_prefix() const { return m_rep_prefix; }
    u8 rep_prefix() const { return m_rep_prefix; }

    bool is_valid() const { return m_descriptor; }

    unsigned length() const;

    String mnemonic() const;

    u8 op() const { return m_op; }
    u8 sub_op() const { return m_sub_op; }
    u8 rm() const { return m_modrm.m_rm; }
    u8 slash() const
    {
        ASSERT(has_rm());
        return (rm() >> 3) & 7;
    }

    u8 imm8() const { return m_imm1; }
    u16 imm16() const { return m_imm1; }
    u32 imm32() const { return m_imm1; }
    u8 imm8_1() const { return imm8(); }
    u8 imm8_2() const { return m_imm2; }
    u16 imm16_1() const { return imm16(); }
    u16 imm16_2() const { return m_imm2; }
    u32 imm32_1() const { return imm32(); }
    u32 imm32_2() const { return m_imm2; }
    u32 imm_address() const { return m_a32 ? imm32() : imm16(); }

    LogicalAddress imm_address16_16() const { return LogicalAddress(imm16_1(), imm16_2()); }
    LogicalAddress imm_address16_32() const { return LogicalAddress(imm16_1(), imm32_2()); }

    bool has_rm() const { return m_has_rm; }
    bool has_sub_op() const
    {
        return m_op == 0x0f;
    }

    unsigned register_index() const { return m_register_index; }
    RegisterIndex32 reg32() const { return static_cast<RegisterIndex32>(register_index()); }
    RegisterIndex16 reg16() const { return static_cast<RegisterIndex16>(register_index()); }
    RegisterIndex8 reg8() const { return static_cast<RegisterIndex8>(register_index()); }

    SegmentRegister segment_register() const { return static_cast<SegmentRegister>(register_index()); }

    u8 cc() const { return has_sub_op() ? m_sub_op & 0xf : m_op & 0xf; }

    bool a32() const { return m_a32; }

    String to_string(u32 origin, const SymbolProvider* = nullptr, bool x32 = true) const;

private:
    template<typename InstructionStreamType>
    Instruction(InstructionStreamType&, bool o32, bool a32);

    String to_string_internal(u32 origin, const SymbolProvider*, bool x32) const;

    const char* reg8_name() const;
    const char* reg16_name() const;
    const char* reg32_name() const;

    u8 m_op { 0 };
    u8 m_sub_op { 0 };
    u32 m_imm1 { 0 };
    u32 m_imm2 { 0 };
    u8 m_register_index { 0 };
    bool m_a32 { false };
    bool m_o32 { false };
    bool m_has_lock_prefix { false };

    bool m_has_rm { false };

    u8 m_extra_bytes { 0 };

    Optional<SegmentRegister> m_segment_prefix;
    bool m_has_operand_size_override_prefix { false };
    bool m_has_address_size_override_prefix { false };
    u8 m_rep_prefix { 0 };

    mutable MemoryOrRegisterReference m_modrm;

    InstructionDescriptor* m_descriptor { nullptr };
};

template<typename CPU>
ALWAYS_INLINE LogicalAddress MemoryOrRegisterReference::resolve16(const CPU& cpu, Optional<SegmentRegister> segment_prefix)
{
    auto default_segment = SegmentRegister::DS;
    u16 offset = 0;

    switch (m_rm & 7) {
    case 0:
        offset = cpu.bx().value() + cpu.si().value() + m_displacement16;
        break;
    case 1:
        offset = cpu.bx().value() + cpu.di().value() + m_displacement16;
        break;
    case 2:
        default_segment = SegmentRegister::SS;
        offset = cpu.bp().value() + cpu.si().value() + m_displacement16;
        break;
    case 3:
        default_segment = SegmentRegister::SS;
        offset = cpu.bp().value() + cpu.di().value() + m_displacement16;
        break;
    case 4:
        offset = cpu.si().value() + m_displacement16;
        break;
    case 5:
        offset = cpu.di().value() + m_displacement16;
        break;
    case 6:
        if ((m_rm & 0xc0) == 0)
            offset = m_displacement16;
        else {
            default_segment = SegmentRegister::SS;
            offset = cpu.bp().value() + m_displacement16;
        }
        break;
    default:
        offset = cpu.bx().value() + m_displacement16;
        break;
    }

    u16 segment = cpu.segment(segment_prefix.value_or(default_segment));
    return { segment, offset };
}

template<typename CPU>
ALWAYS_INLINE LogicalAddress MemoryOrRegisterReference::resolve32(const CPU& cpu, Optional<SegmentRegister> segment_prefix)
{
    auto default_segment = SegmentRegister::DS;
    u32 offset = 0;

    switch (m_rm & 0x07) {
    case 0:
        offset = cpu.eax().value() + m_displacement32;
        break;
    case 1:
        offset = cpu.ecx().value() + m_displacement32;
        break;
    case 2:
        offset = cpu.edx().value() + m_displacement32;
        break;
    case 3:
        offset = cpu.ebx().value() + m_displacement32;
        break;
    case 4:
        offset = evaluate_sib(cpu, default_segment);
        break;
    case 6:
        offset = cpu.esi().value() + m_displacement32;
        break;
    case 7:
        offset = cpu.edi().value() + m_displacement32;
        break;
    default: // 5
        if ((m_rm & 0xc0) == 0x00) {
            offset = m_displacement32;
            break;
        } else {
            default_segment = SegmentRegister::SS;
            offset = cpu.ebp().value() + m_displacement32;
            break;
        }
        break;
    }
    u16 segment = cpu.segment(segment_prefix.value_or(default_segment));
    return { segment, offset };
}

template<typename CPU>
ALWAYS_INLINE u32 MemoryOrRegisterReference::evaluate_sib(const CPU& cpu, SegmentRegister& default_segment) const
{
    u32 scale = 0;
    switch (m_sib & 0xc0) {
    case 0x00:
        scale = 1;
        break;
    case 0x40:
        scale = 2;
        break;
    case 0x80:
        scale = 4;
        break;
    case 0xc0:
        scale = 8;
        break;
    }
    u32 index = 0;
    switch ((m_sib >> 3) & 0x07) {
    case 0:
        index = cpu.eax().value();
        break;
    case 1:
        index = cpu.ecx().value();
        break;
    case 2:
        index = cpu.edx().value();
        break;
    case 3:
        index = cpu.ebx().value();
        break;
    case 4:
        index = 0;
        break;
    case 5:
        index = cpu.ebp().value();
        break;
    case 6:
        index = cpu.esi().value();
        break;
    case 7:
        index = cpu.edi().value();
        break;
    }

    u32 base = m_displacement32;
    switch (m_sib & 0x07) {
    case 0:
        base += cpu.eax().value();
        break;
    case 1:
        base += cpu.ecx().value();
        break;
    case 2:
        base += cpu.edx().value();
        break;
    case 3:
        base += cpu.ebx().value();
        break;
    case 4:
        default_segment = SegmentRegister::SS;
        base += cpu.esp().value();
        break;
    case 6:
        base += cpu.esi().value();
        break;
    case 7:
        base += cpu.edi().value();
        break;
    default: // 5
        switch ((m_rm >> 6) & 3) {
        case 0:
            break;
        case 1:
        case 2:
            default_segment = SegmentRegister::SS;
            base += cpu.ebp().value();
            break;
        default:
            ASSERT_NOT_REACHED();
            break;
        }
        break;
    }

    return (scale * index) + base;
}

template<typename CPU, typename T>
ALWAYS_INLINE void MemoryOrRegisterReference::write8(CPU& cpu, const Instruction& insn, T value)
{
    if (is_register()) {
        cpu.gpr8(reg8()) = value;
        return;
    }

    auto address = resolve(cpu, insn);
    cpu.write_memory8(address, value);
}

template<typename CPU, typename T>
ALWAYS_INLINE void MemoryOrRegisterReference::write16(CPU& cpu, const Instruction& insn, T value)
{
    if (is_register()) {
        cpu.gpr16(reg16()) = value;
        return;
    }

    auto address = resolve(cpu, insn);
    cpu.write_memory16(address, value);
}

template<typename CPU, typename T>
ALWAYS_INLINE void MemoryOrRegisterReference::write32(CPU& cpu, const Instruction& insn, T value)
{
    if (is_register()) {
        cpu.gpr32(reg32()) = value;
        return;
    }

    auto address = resolve(cpu, insn);
    cpu.write_memory32(address, value);
}

template<typename CPU, typename T>
ALWAYS_INLINE void MemoryOrRegisterReference::write64(CPU& cpu, const Instruction& insn, T value)
{
    ASSERT(!is_register());
    auto address = resolve(cpu, insn);
    cpu.write_memory64(address, value);
}

template<typename CPU>
ALWAYS_INLINE typename CPU::ValueWithShadowType8 MemoryOrRegisterReference::read8(CPU& cpu, const Instruction& insn)
{
    if (is_register())
        return cpu.const_gpr8(reg8());

    auto address = resolve(cpu, insn);
    return cpu.read_memory8(address);
}

template<typename CPU>
ALWAYS_INLINE typename CPU::ValueWithShadowType16 MemoryOrRegisterReference::read16(CPU& cpu, const Instruction& insn)
{
    if (is_register())
        return cpu.const_gpr16(reg16());

    auto address = resolve(cpu, insn);
    return cpu.read_memory16(address);
}

template<typename CPU>
ALWAYS_INLINE typename CPU::ValueWithShadowType32 MemoryOrRegisterReference::read32(CPU& cpu, const Instruction& insn)
{
    if (is_register())
        return cpu.const_gpr32(reg32());

    auto address = resolve(cpu, insn);
    return cpu.read_memory32(address);
}

template<typename CPU>
ALWAYS_INLINE typename CPU::ValueWithShadowType64 MemoryOrRegisterReference::read64(CPU& cpu, const Instruction& insn)
{
    ASSERT(!is_register());
    auto address = resolve(cpu, insn);
    return cpu.read_memory64(address);
}

template<typename InstructionStreamType>
ALWAYS_INLINE Instruction Instruction::from_stream(InstructionStreamType& stream, bool o32, bool a32)
{
    return Instruction(stream, o32, a32);
}

ALWAYS_INLINE unsigned Instruction::length() const
{
    unsigned len = 1;
    if (has_sub_op())
        ++len;
    if (m_has_rm) {
        ++len;
        if (m_modrm.m_has_sib)
            ++len;
        len += m_modrm.m_displacement_bytes;
    }
    len += m_extra_bytes;
    return len;
}

ALWAYS_INLINE Optional<SegmentRegister> to_segment_prefix(u8 op)
{
    switch (op) {
    case 0x26:
        return SegmentRegister::ES;
    case 0x2e:
        return SegmentRegister::CS;
    case 0x36:
        return SegmentRegister::SS;
    case 0x3e:
        return SegmentRegister::DS;
    case 0x64:
        return SegmentRegister::FS;
    case 0x65:
        return SegmentRegister::GS;
    default:
        return {};
    }
}

template<typename InstructionStreamType>
ALWAYS_INLINE Instruction::Instruction(InstructionStreamType& stream, bool o32, bool a32)
    : m_a32(a32)
    , m_o32(o32)
{
    u8 prefix_bytes = 0;
    for (;; ++prefix_bytes) {
        u8 opbyte = stream.read8();
        if (opbyte == Prefix::OperandSizeOverride) {
            m_o32 = !o32;
            m_has_operand_size_override_prefix = true;
            continue;
        }
        if (opbyte == Prefix::AddressSizeOverride) {
            m_a32 = !a32;
            m_has_address_size_override_prefix = true;
            continue;
        }
        if (opbyte == Prefix::REPZ || opbyte == Prefix::REPNZ) {
            m_rep_prefix = opbyte;
            continue;
        }
        if (opbyte == Prefix::LOCK) {
            m_has_lock_prefix = true;
            continue;
        }
        auto segment_prefix = to_segment_prefix(opbyte);
        if (segment_prefix.has_value()) {
            m_segment_prefix = segment_prefix;
            continue;
        }
        m_op = opbyte;
        break;
    }

    if (m_op == 0x0f) {
        m_sub_op = stream.read8();
        m_descriptor = m_o32 ? &s_0f_table32[m_sub_op] : &s_0f_table16[m_sub_op];
    } else {
        m_descriptor = m_o32 ? &s_table32[m_op] : &s_table16[m_op];
    }

    m_has_rm = m_descriptor->has_rm;
    if (m_has_rm) {
        // Consume ModR/M (may include SIB and displacement.)
        m_modrm.decode(stream, m_a32);
        m_register_index = (m_modrm.m_rm >> 3) & 7;
    } else {
        if (has_sub_op())
            m_register_index = m_sub_op & 7;
        else
            m_register_index = m_op & 7;
    }

    bool has_slash = m_descriptor->format == MultibyteWithSlash;
    if (has_slash) {
        m_descriptor = &m_descriptor->slashes[slash()];
        if ((rm() & 0xc0) == 0xc0 && m_descriptor->slashes)
            m_descriptor = &m_descriptor->slashes[rm() & 7];
    }

    if (!m_descriptor->mnemonic) {
        if (has_sub_op()) {
            if (has_slash)
                fprintf(stderr, "Instruction %02X %02X /%u not understood\n", m_op, m_sub_op, slash());
            else
                fprintf(stderr, "Instruction %02X %02X not understood\n", m_op, m_sub_op);
        } else {
            if (has_slash)
                fprintf(stderr, "Instruction %02X /%u not understood\n", m_op, slash());
            else
                fprintf(stderr, "Instruction %02X not understood\n", m_op);
        }
        m_descriptor = nullptr;
        return;
    }

    auto imm1_bytes = m_descriptor->imm1_bytes_for_address_size(m_a32);
    auto imm2_bytes = m_descriptor->imm2_bytes_for_address_size(m_a32);

    // Consume immediates if present.
    switch (imm2_bytes) {
    case 1:
        m_imm2 = stream.read8();
        break;
    case 2:
        m_imm2 = stream.read16();
        break;
    case 4:
        m_imm2 = stream.read32();
        break;
    }

    switch (imm1_bytes) {
    case 1:
        m_imm1 = stream.read8();
        break;
    case 2:
        m_imm1 = stream.read16();
        break;
    case 4:
        m_imm1 = stream.read32();
        break;
    }

    m_extra_bytes = prefix_bytes + imm1_bytes + imm2_bytes;

#ifdef DISALLOW_INVALID_LOCK_PREFIX
    if (m_has_lock_prefix && !m_descriptor->lock_prefix_allowed) {
        fprintf(stderr, "Instruction not allowed with LOCK prefix, this will raise #UD\n");
        m_descriptor = nullptr;
    }
#endif
}

template<typename InstructionStreamType>
ALWAYS_INLINE void MemoryOrRegisterReference::decode(InstructionStreamType& stream, bool a32)
{
    m_rm = stream.read8();

    if (a32) {
        decode32(stream);
        switch (m_displacement_bytes) {
        case 0:
            break;
        case 1:
            m_displacement32 = sign_extended_to<u32>(stream.read8());
            break;
        case 4:
            m_displacement32 = stream.read32();
            break;
        default:
            ASSERT_NOT_REACHED();
            break;
        }
    } else {
        decode16(stream);
        switch (m_displacement_bytes) {
        case 0:
            break;
        case 1:
            m_displacement16 = sign_extended_to<u16>(stream.read8());
            break;
        case 2:
            m_displacement16 = stream.read16();
            break;
        default:
            ASSERT_NOT_REACHED();
            break;
        }
    }
}

template<typename InstructionStreamType>
ALWAYS_INLINE void MemoryOrRegisterReference::decode16(InstructionStreamType&)
{
    switch (m_rm & 0xc0) {
    case 0:
        if ((m_rm & 0x07) == 6)
            m_displacement_bytes = 2;
        else
            ASSERT(m_displacement_bytes == 0);
        break;
    case 0x40:
        m_displacement_bytes = 1;
        break;
    case 0x80:
        m_displacement_bytes = 2;
        break;
    case 0xc0:
        m_register_index = m_rm & 7;
        break;
    }
}

template<typename InstructionStreamType>
ALWAYS_INLINE void MemoryOrRegisterReference::decode32(InstructionStreamType& stream)
{
    switch (m_rm & 0xc0) {
    case 0:
        if ((m_rm & 0x07) == 5)
            m_displacement_bytes = 4;
        break;
    case 0x40:
        m_displacement_bytes = 1;
        break;
    case 0x80:
        m_displacement_bytes = 4;
        break;
    case 0xc0:
        m_register_index = m_rm & 7;
        return;
    }

    m_has_sib = (m_rm & 0x07) == 4;
    if (m_has_sib) {
        m_sib = stream.read8();
        if ((m_sib & 0x07) == 5) {
            switch ((m_rm >> 6) & 0x03) {
            case 0:
                ASSERT(!m_displacement_bytes || m_displacement_bytes == 4);
                m_displacement_bytes = 4;
                break;
            case 1:
                ASSERT(!m_displacement_bytes || m_displacement_bytes == 1);
                m_displacement_bytes = 1;
                break;
            case 2:
                ASSERT(!m_displacement_bytes || m_displacement_bytes == 4);
                m_displacement_bytes = 4;
                break;
            default:
                ASSERT_NOT_REACHED();
                break;
            }
        }
    }
}

template<typename CPU>
ALWAYS_INLINE LogicalAddress MemoryOrRegisterReference::resolve(const CPU& cpu, const Instruction& insn)
{
    if (insn.a32())
        return resolve32(cpu, insn.segment_prefix());
    return resolve16(cpu, insn.segment_prefix());
}

}
