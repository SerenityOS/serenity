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

namespace X86 {

class Instruction;
struct InstructionDescriptor;

class SymbolProvider {
public:
    virtual String symbolicate(FlatPtr, u32* offset = nullptr) const = 0;
};

template<typename T>
struct TypeTrivia {
    static const size_t bits = sizeof(T) * 8;
    static const T sign_bit = 1 << (bits - 1);
    static const T mask = typename MakeUnsigned<T>::type(-1);
};

template<typename T, typename U>
inline constexpr T sign_extended_to(U value)
{
    if (!(value & TypeTrivia<U>::sign_bit))
        return value;
    return (TypeTrivia<T>::mask & ~TypeTrivia<U>::mask) | value;
}

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
    u32 read(unsigned count);
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

    size_t offset() const { return m_offset; }

private:
    const u8* m_data { nullptr };
    size_t m_offset { 0 };
    size_t m_size { 0 };
};

class MemoryOrRegisterReference {
    friend class Instruction;

public:
    String to_string_o8() const;
    String to_string_o16() const;
    String to_string_o32() const;
    String to_string_mm() const;

    bool is_register() const { return m_register_index != 0xffffffff; }

    unsigned register_index() const { return m_register_index; }
    RegisterIndex32 reg32() const { return static_cast<RegisterIndex32>(register_index()); }
    RegisterIndex16 reg16() const { return static_cast<RegisterIndex16>(register_index()); }
    RegisterIndex8 reg8() const { return static_cast<RegisterIndex8>(register_index()); }

    template<typename CPU>
    void write8(CPU&, const Instruction&, u8);
    template<typename CPU>
    void write16(CPU&, const Instruction&, u16);
    template<typename CPU>
    void write32(CPU&, const Instruction&, u32);

    template<typename CPU>
    u8 read8(CPU&, const Instruction&);
    template<typename CPU>
    u16 read16(CPU&, const Instruction&);
    template<typename CPU>
    u32 read32(CPU&, const Instruction&);

private:
    MemoryOrRegisterReference() { }

    String to_string() const;
    String to_string_a16() const;
    String to_string_a32() const;

    void decode(InstructionStream&, bool a32);
    void decode16(InstructionStream&);
    void decode32(InstructionStream&);

    template<typename CPU>
    LogicalAddress resolve16(const CPU&, Optional<SegmentRegister>);
    template<typename CPU>
    LogicalAddress resolve32(const CPU&, Optional<SegmentRegister>);

    template<typename CPU>
    LogicalAddress resolve(const CPU& cpu, Optional<SegmentRegister> segment_prefix)
    {
        if (m_a32)
            return resolve32(cpu, segment_prefix);
        return resolve16(cpu, segment_prefix);
    }

    template<typename CPU>
    u32 evaluate_sib(const CPU&, SegmentRegister& default_segment) const;

    unsigned m_register_index { 0xffffffff };
    SegmentRegister m_segment;
    union {
        u32 m_offset32 { 0 };
        u16 m_offset16;
    };

    u8 m_a32 { false };

    u8 m_rm { 0 };
    u8 m_sib { 0 };
    u8 m_displacement_bytes { 0 };

    union {
        u32 m_displacement32 { 0 };
        u16 m_displacement16;
    };

    bool m_has_sib { false };
};

class Interpreter;
typedef void (Interpreter::*InstructionHandler)(const Instruction&);

class Instruction {
public:
    static Instruction from_stream(InstructionStream&, bool o32, bool a32);
    ~Instruction() { }

    MemoryOrRegisterReference& modrm() const
    {
        ASSERT(has_rm());
        return m_modrm;
    }

    InstructionHandler handler() const;

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

    u8 imm8() const
    {
        ASSERT(m_imm1_bytes == 1);
        return m_imm1;
    }
    u16 imm16() const
    {
        ASSERT(m_imm1_bytes == 2);
        return m_imm1;
    }
    u32 imm32() const
    {
        ASSERT(m_imm1_bytes == 4);
        return m_imm1;
    }

    u8 imm8_1() const { return imm8(); }
    u8 imm8_2() const
    {
        ASSERT(m_imm2_bytes == 1);
        return m_imm2;
    }
    u16 imm16_1() const { return imm16(); }
    u16 imm16_2() const
    {
        ASSERT(m_imm2_bytes == 2);
        return m_imm2;
    }
    u32 imm32_1() const { return imm32(); }
    u32 imm32_2() const
    {
        ASSERT(m_imm2_bytes == 4);
        return m_imm2;
    }

    u32 imm_address() const { return m_a32 ? imm32() : imm16(); }

    LogicalAddress imm_address16_16() const { return LogicalAddress(imm16_1(), imm16_2()); }
    LogicalAddress imm_address16_32() const { return LogicalAddress(imm16_1(), imm32_2()); }

    bool has_rm() const { return m_has_rm; }
    bool has_sub_op() const { return m_has_sub_op; }

    unsigned register_index() const { return m_register_index; }
    RegisterIndex32 reg32() const { return static_cast<RegisterIndex32>(register_index()); }
    RegisterIndex16 reg16() const { return static_cast<RegisterIndex16>(register_index()); }
    RegisterIndex8 reg8() const { return static_cast<RegisterIndex8>(register_index()); }

    SegmentRegister segment_register() const { return static_cast<SegmentRegister>(register_index()); }

    u8 cc() const { return m_has_sub_op ? m_sub_op & 0xf : m_op & 0xf; }

    String to_string(u32 origin, const SymbolProvider* = nullptr, bool x32 = true) const;

private:
    Instruction(InstructionStream&, bool o32, bool a32);

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

    bool m_has_sub_op { false };
    bool m_has_rm { false };

    unsigned m_imm1_bytes { 0 };
    unsigned m_imm2_bytes { 0 };
    unsigned m_prefix_bytes { 0 };

    Optional<SegmentRegister> m_segment_prefix;
    bool m_has_operand_size_override_prefix { false };
    bool m_has_address_size_override_prefix { false };
    u8 m_rep_prefix { 0 };

    mutable MemoryOrRegisterReference m_modrm;

    InstructionDescriptor* m_descriptor { nullptr };
};

template<typename CPU>
LogicalAddress MemoryOrRegisterReference::resolve16(const CPU& cpu, Optional<SegmentRegister> segment_prefix)
{
    ASSERT(!m_a32);

    auto default_segment = SegmentRegister::DS;
    u16 offset = 0;

    switch (m_rm & 7) {
    case 0:
        offset = cpu.bx() + cpu.si() + m_displacement16;
        break;
    case 1:
        offset = cpu.bx() + cpu.di() + m_displacement16;
        break;
    case 2:
        default_segment = SegmentRegister::SS;
        offset = cpu.bp() + cpu.si() + m_displacement16;
        break;
    case 3:
        default_segment = SegmentRegister::SS;
        offset = cpu.bp() + cpu.di() + m_displacement16;
        break;
    case 4:
        offset = cpu.si() + m_displacement16;
        break;
    case 5:
        offset = cpu.di() + m_displacement16;
        break;
    case 6:
        if ((m_rm & 0xc0) == 0)
            offset = m_displacement16;
        else {
            default_segment = SegmentRegister::SS;
            offset = cpu.bp() + m_displacement16;
        }
        break;
    default:
        offset = cpu.bx() + m_displacement16;
        break;
    }

    u16 segment = cpu.segment(segment_prefix.value_or(default_segment));
    return { segment, offset };
}

template<typename CPU>
inline LogicalAddress MemoryOrRegisterReference::resolve32(const CPU& cpu, Optional<SegmentRegister> segment_prefix)
{
    ASSERT(m_a32);

    auto default_segment = SegmentRegister::DS;
    u32 offset = 0;

    switch (m_rm & 0x07) {
    case 0:
        offset = cpu.eax() + m_displacement32;
        break;
    case 1:
        offset = cpu.ecx() + m_displacement32;
        break;
    case 2:
        offset = cpu.edx() + m_displacement32;
        break;
    case 3:
        offset = cpu.ebx() + m_displacement32;
        break;
    case 4:
        offset = evaluate_sib(cpu, default_segment);
        break;
    case 6:
        offset = cpu.esi() + m_displacement32;
        break;
    case 7:
        offset = cpu.edi() + m_displacement32;
        break;
    default: // 5
        if ((m_rm & 0xc0) == 0x00) {
            offset = m_displacement32;
            break;
        } else {
            default_segment = SegmentRegister::SS;
            offset = cpu.ebp() + m_displacement32;
            break;
        }
        break;
    }
    u16 segment = cpu.segment(segment_prefix.value_or(default_segment));
    return { segment, offset };
}

template<typename CPU>
inline u32 MemoryOrRegisterReference::evaluate_sib(const CPU& cpu, SegmentRegister& default_segment) const
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
        index = cpu.eax();
        break;
    case 1:
        index = cpu.ecx();
        break;
    case 2:
        index = cpu.edx();
        break;
    case 3:
        index = cpu.ebx();
        break;
    case 4:
        index = 0;
        break;
    case 5:
        index = cpu.ebp();
        break;
    case 6:
        index = cpu.esi();
        break;
    case 7:
        index = cpu.edi();
        break;
    }

    u32 base = m_displacement32;
    switch (m_sib & 0x07) {
    case 0:
        base += cpu.eax();
        break;
    case 1:
        base += cpu.ecx();
        break;
    case 2:
        base += cpu.edx();
        break;
    case 3:
        base += cpu.ebx();
        break;
    case 4:
        default_segment = SegmentRegister::SS;
        base += cpu.esp();
        break;
    case 6:
        base += cpu.esi();
        break;
    case 7:
        base += cpu.edi();
        break;
    default: // 5
        switch ((m_rm >> 6) & 3) {
        case 0:
            break;
        case 1:
        case 2:
            default_segment = SegmentRegister::SS;
            base += cpu.ebp();
            break;
        default:
            ASSERT_NOT_REACHED();
            break;
        }
        break;
    }

    return (scale * index) + base;
}

template<typename CPU>
inline void MemoryOrRegisterReference::write8(CPU& cpu, const Instruction& insn, u8 value)
{
    if (is_register()) {
        cpu.gpr8(reg8()) = value;
        return;
    }

    auto address = resolve(cpu, insn.segment_prefix());
    cpu.write_memory8(address, value);
}

template<typename CPU>
inline void MemoryOrRegisterReference::write16(CPU& cpu, const Instruction& insn, u16 value)
{
    if (is_register()) {
        cpu.gpr16(reg16()) = value;
        return;
    }

    auto address = resolve(cpu, insn.segment_prefix());
    cpu.write_memory16(address, value);
}

template<typename CPU>
inline void MemoryOrRegisterReference::write32(CPU& cpu, const Instruction& insn, u32 value)
{
    if (is_register()) {
        cpu.gpr32(reg32()) = value;
        return;
    }

    auto address = resolve(cpu, insn.segment_prefix());
    cpu.write_memory32(address, value);
}

template<typename CPU>
inline u8 MemoryOrRegisterReference::read8(CPU& cpu, const Instruction& insn)
{
    if (is_register())
        return cpu.gpr8(reg8());

    auto address = resolve(cpu, insn.segment_prefix());
    return cpu.read_memory8(address);
}

template<typename CPU>
inline u16 MemoryOrRegisterReference::read16(CPU& cpu, const Instruction& insn)
{
    if (is_register())
        return cpu.gpr16(reg16());

    auto address = resolve(cpu, insn.segment_prefix());
    return cpu.read_memory16(address);
}

template<typename CPU>
inline u32 MemoryOrRegisterReference::read32(CPU& cpu, const Instruction& insn)
{
    if (is_register())
        return cpu.gpr32(reg32());

    auto address = resolve(cpu, insn.segment_prefix());
    return cpu.read_memory32(address);
}

}
