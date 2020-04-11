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

#include <AK/String.h>
#include <AK/Types.h>

namespace X86 {

class Instruction;
struct InstructionDescriptor;

template<typename T>
struct MakeUnsigned {
    typedef T type;
};
template<>
struct MakeUnsigned<i8> {
    typedef u8 type;
};
template<>
struct MakeUnsigned<i16> {
    typedef u32 type;
};
template<>
struct MakeUnsigned<i32> {
    typedef u32 type;
};
template<>
struct MakeUnsigned<i64> {
    typedef u64 type;
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
    None = 0xFF,
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
    LogicalAddress() {}
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
    SegmentRegister segment() const
    {
        ASSERT(!is_register());
        return m_segment;
    }
    u32 offset();

private:
    MemoryOrRegisterReference() {}

    String to_string() const;
    String to_string_a16() const;
    String to_string_a32() const;

    void decode(InstructionStream&, bool a32);
    void decode16(InstructionStream&);
    void decode32(InstructionStream&);

    unsigned m_register_index { 0xffffffff };
    SegmentRegister m_segment { SegmentRegister::None };
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

class Instruction {
public:
    static Instruction from_stream(InstructionStream&, bool o32, bool a32);
    ~Instruction() {}

    MemoryOrRegisterReference& modrm()
    {
        ASSERT(has_rm());
        return m_modrm;
    }

    bool has_segment_prefix() const { return m_segment_prefix != SegmentRegister::None; }
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
    SegmentRegister segment_register() const { return static_cast<SegmentRegister>(register_index()); }

    u8 cc() const { return m_has_sub_op ? m_sub_op & 0xf : m_op & 0xf; }

    String to_string(u32 origin, bool x32 = true) const;

private:
    Instruction(InstructionStream&, bool o32, bool a32);

    String to_string_internal(u32 origin, bool x32) const;

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

    SegmentRegister m_segment_prefix { SegmentRegister::None };
    bool m_has_operand_size_override_prefix { false };
    bool m_has_address_size_override_prefix { false };
    u8 m_rep_prefix { 0 };

    MemoryOrRegisterReference m_modrm;

    InstructionDescriptor* m_descriptor { nullptr };
};

}
