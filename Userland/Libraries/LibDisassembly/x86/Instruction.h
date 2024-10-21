/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Optional.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <LibDisassembly/Instruction.h>
#include <LibDisassembly/SymbolProvider.h>
#include <stdio.h>

namespace Disassembly::X86 {

class Instruction;
class Interpreter;
typedef void (Interpreter::*InstructionHandler)(Instruction const&);

template<typename T>
struct TypeTrivia {
    static constexpr size_t bits = sizeof(T) * 8;
    static constexpr T sign_bit = 1 << (bits - 1);
    static constexpr T mask = MakeUnsigned<T>(-1);
};

template<typename T, typename U>
constexpr T sign_extended_to(U value)
{
    if (!(value & TypeTrivia<U>::sign_bit))
        return value;
    return (TypeTrivia<T>::mask & ~TypeTrivia<U>::mask) | value;
}

enum class OperandSize : u8 {
    Size16,
    Size32,
    Size64,
};

enum class AddressSize : u8 {
    Size16,
    Size32,
    Size64,
};

enum class ProcessorMode : u8 {
    Protected,
    Long,
};

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
    OP_reg,
    OP_m64,
    // SSE instructions mutate on some prefixes, so we have to mark them
    // for further parsing
    __SSE,
    OP_mm1_rm32,
    OP_rm32_mm2,
    OP_mm1_mm2m64,
    OP_mm1_mm2m32,
    OP_mm1_mm2m64_imm8,
    OP_mm1_imm8,
    OP_mm1m64_mm2,
    OP_reg_mm1,
    OP_reg_mm1_imm8,
    OP_mm1_r32m16_imm8,

    OP_xmm1_imm8,
    OP_xmm1_xmm2m32,
    OP_xmm1_xmm2m64,
    OP_xmm1_xmm2m128,
    OP_xmm1_xmm2m32_imm8,
    OP_xmm1_xmm2m128_imm8,
    OP_xmm1m32_xmm2,
    OP_xmm1m64_xmm2,
    OP_xmm1m128_xmm2,
    OP_reg_xmm1,
    OP_reg_xmm1_imm8,
    OP_r32_xmm2m32,
    OP_r32_xmm2m64,
    OP_rm32_xmm2,
    OP_xmm1_rm32,
    OP_xmm1_m64,
    OP_m64_xmm2,
    OP_rm8_xmm2m32,
    OP_xmm_mm,
    OP_xmm1_mm2m64,
    OP_mm1m64_xmm2,
    OP_mm_xmm,
    OP_mm1_xmm2m64,
    OP_mm1_xmm2m128,
    OP_xmm1_r32m16_imm8,
    __EndFormatsWithRMByte,

    OP_reg32_imm32,
    OP_regW_immW,
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

static constexpr unsigned CurrentAddressSize = 0xB33FBABE;
static constexpr unsigned CurrentOperandSize = 0xB33FB00F;

struct InstructionDescriptor {
    InstructionHandler handler { nullptr };
    bool opcode_has_register_index { false };
    char const* mnemonic { nullptr };
    InstructionFormat format { InvalidFormat };
    bool has_rm { false };
    unsigned imm1_bytes { 0 };
    unsigned imm2_bytes { 0 };
    bool long_mode_default_64 { false };
    bool long_mode_force_64 { false };

    // Addressed by the 3 REG bits in the MOD-REG-R/M byte.
    // Some slash instructions have further subgroups when MOD is 11,
    // in that case the InstructionDescriptors in slashes have themselves
    // a non-null slashes member that's indexed by the three R/M bits.
    InstructionDescriptor* slashes { nullptr };

    unsigned imm1_bytes_for(AddressSize address_size, OperandSize operand_size) const
    {
        if (imm1_bytes == CurrentAddressSize) {
            switch (address_size) {
            case AddressSize::Size64:
                return 8;
            case AddressSize::Size32:
                return 4;
            case AddressSize::Size16:
                return 2;
            }
            VERIFY_NOT_REACHED();
        }
        if (imm1_bytes == CurrentOperandSize) {
            switch (operand_size) {
            case OperandSize::Size64:
                return 8;
            case OperandSize::Size32:
                return 4;
            case OperandSize::Size16:
                return 2;
            }
            VERIFY_NOT_REACHED();
        }
        return imm1_bytes;
    }

    unsigned imm2_bytes_for(AddressSize address_size, OperandSize operand_size) const
    {
        if (imm2_bytes == CurrentAddressSize) {
            switch (address_size) {
            case AddressSize::Size64:
                return 8;
            case AddressSize::Size32:
                return 4;
            case AddressSize::Size16:
                return 2;
            }
            VERIFY_NOT_REACHED();
        }
        if (imm2_bytes == CurrentOperandSize) {
            switch (operand_size) {
            case OperandSize::Size64:
                return 8;
            case OperandSize::Size32:
                return 4;
            case OperandSize::Size16:
                return 2;
            }
            VERIFY_NOT_REACHED();
        }
        return imm2_bytes;
    }

    IsLockPrefixAllowed lock_prefix_allowed { LockPrefixNotAllowed };
};

extern InstructionDescriptor s_table[3][256];
extern InstructionDescriptor s_0f_table[3][256];
extern InstructionDescriptor s_sse_table_np[256];
extern InstructionDescriptor s_sse_table_66[256];
extern InstructionDescriptor s_sse_table_f2[256];
extern InstructionDescriptor s_sse_table_f3[256];

struct Prefix {
    enum Op {
        REX_Mask = 0xf0,
        REX_Base = 0x40,
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
    RegisterBH,
    RegisterR8B,
    RegisterR9B,
    RegisterR10B,
    RegisterR11B,
    RegisterR12B,
    RegisterR13B,
    RegisterR14B,
    RegisterR15B,
};

enum RegisterIndex16 {
    RegisterAX = 0,
    RegisterCX,
    RegisterDX,
    RegisterBX,
    RegisterSP,
    RegisterBP,
    RegisterSI,
    RegisterDI,
    RegisterR8W,
    RegisterR9W,
    RegisterR10W,
    RegisterR11W,
    RegisterR12W,
    RegisterR13W,
    RegisterR14W,
    RegisterR15W,
};

enum RegisterIndex32 {
    RegisterEAX = 0,
    RegisterECX,
    RegisterEDX,
    RegisterEBX,
    RegisterESP,
    RegisterEBP,
    RegisterESI,
    RegisterEDI,
    RegisterR8D,
    RegisterR9D,
    RegisterR10D,
    RegisterR11D,
    RegisterR12D,
    RegisterR13D,
    RegisterR14D,
    RegisterR15D,
};

enum RegisterIndex64 {
    RegisterRAX = 0,
    RegisterRCX,
    RegisterRDX,
    RegisterRBX,
    RegisterRSP,
    RegisterRBP,
    RegisterRSI,
    RegisterRDI,
    RegisterR8,
    RegisterR9,
    RegisterR10,
    RegisterR11,
    RegisterR12,
    RegisterR13,
    RegisterR14,
    RegisterR15,
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

enum XMMRegisterIndex {
    RegisterXMM0 = 0,
    RegisterXMM1,
    RegisterXMM2,
    RegisterXMM3,
    RegisterXMM4,
    RegisterXMM5,
    RegisterXMM6,
    RegisterXMM7,
    RegisterXMM8,
    RegisterXMM9,
    RegisterXMM10,
    RegisterXMM11,
    RegisterXMM12,
    RegisterXMM13,
    RegisterXMM14,
    RegisterXMM15,
};

class LogicalAddress {
public:
    LogicalAddress() = default;
    LogicalAddress(u16 selector, FlatPtr offset)
        : m_selector(selector)
        , m_offset(offset)
    {
    }

    u16 selector() const { return m_selector; }
    FlatPtr offset() const { return m_offset; }
    void set_selector(u16 selector) { m_selector = selector; }
    void set_offset(FlatPtr offset) { m_offset = offset; }

private:
    u16 m_selector { 0 };
    FlatPtr m_offset { 0 };
};

class MemoryOrRegisterReference {
    friend class Instruction;

public:
    ByteString to_byte_string_o8(Instruction const&) const;
    ByteString to_byte_string_o16(Instruction const&) const;
    ByteString to_byte_string_o32(Instruction const&) const;
    ByteString to_byte_string_o64(Instruction const&) const;
    ByteString to_byte_string_fpu_reg() const;
    ByteString to_byte_string_fpu_mem(Instruction const&) const;
    ByteString to_byte_string_fpu_ax16() const;
    ByteString to_byte_string_fpu16(Instruction const&) const;
    ByteString to_byte_string_fpu32(Instruction const&) const;
    ByteString to_byte_string_fpu64(Instruction const&) const;
    ByteString to_byte_string_fpu80(Instruction const&) const;
    ByteString to_byte_string_mm(Instruction const&) const;
    ByteString to_byte_string_xmm(Instruction const&) const;
    ByteString sib_to_byte_string(ProcessorMode) const;

    bool is_register() const { return m_register_index != 0x7f; }

    unsigned register_index() const { return m_register_index; }
    RegisterIndex64 reg64() const { return static_cast<RegisterIndex64>(register_index()); }
    RegisterIndex32 reg32() const { return static_cast<RegisterIndex32>(register_index()); }
    RegisterIndex16 reg16() const { return static_cast<RegisterIndex16>(register_index()); }
    RegisterIndex8 reg8() const { return static_cast<RegisterIndex8>(register_index()); }
    FpuRegisterIndex reg_fpu() const { return static_cast<FpuRegisterIndex>(register_index()); }

    // helpers to get the parts by name as in the spec
    u8 mod() const { return m_mod; }
    u8 reg() const { return m_reg; }
    u8 rm() const { return m_rm; }
    u8 modrm_byte() const { return (m_mod << 6) | ((m_reg & 7) << 3) | (m_rm & 7); }

    template<typename CPU, typename T>
    void write8(CPU&, Instruction const&, T);
    template<typename CPU, typename T>
    void write16(CPU&, Instruction const&, T);
    template<typename CPU, typename T>
    void write32(CPU&, Instruction const&, T);
    template<typename CPU, typename T>
    void write64(CPU&, Instruction const&, T);
    template<typename CPU, typename T>
    void write128(CPU&, Instruction const&, T);
    template<typename CPU, typename T>
    void write256(CPU&, Instruction const&, T);

    template<typename CPU>
    typename CPU::ValueWithShadowType8 read8(CPU&, Instruction const&);
    template<typename CPU>
    typename CPU::ValueWithShadowType16 read16(CPU&, Instruction const&);
    template<typename CPU>
    typename CPU::ValueWithShadowType32 read32(CPU&, Instruction const&);
    template<typename CPU>
    typename CPU::ValueWithShadowType64 read64(CPU&, Instruction const&);
    template<typename CPU>
    typename CPU::ValueWithShadowType128 read128(CPU&, Instruction const&);
    template<typename CPU>
    typename CPU::ValueWithShadowType256 read256(CPU&, Instruction const&);

    template<typename CPU>
    LogicalAddress resolve(const CPU&, Instruction const&);

private:
    MemoryOrRegisterReference() = default;

    ByteString to_byte_string(Instruction const&) const;
    ByteString to_byte_string_a16() const;
    ByteString to_byte_string_a32() const;
    ByteString to_byte_string_a64() const;

    template<typename InstructionStreamType>
    void decode(InstructionStreamType&, AddressSize, bool has_rex_r, bool has_rex_x, bool has_rex_b);
    template<typename InstructionStreamType>
    void decode16(InstructionStreamType&);
    template<typename InstructionStreamType>
    void decode32(InstructionStreamType&, bool has_rex_r, bool has_rex_x, bool has_rex_b);
    template<typename CPU>
    LogicalAddress resolve16(const CPU&, Optional<SegmentRegister>);
    template<typename CPU>
    LogicalAddress resolve32(const CPU&, Optional<SegmentRegister>);

    template<typename CPU>
    u32 evaluate_sib(const CPU&, SegmentRegister& default_segment) const;

    union {
        u32 m_displacement32 { 0 };
        u16 m_displacement16;
    };

    u8 m_mod : 2 { 0 };
    u8 m_reg : 4 { 0 };
    u8 : 2;
    u8 m_rm : 4 { 0 };
    u8 m_sib_scale : 2 { 0 };
    u8 : 2;
    u8 m_sib_index : 4 { 0 };
    u8 m_sib_base : 4 { 0 };
    u8 m_displacement_bytes { 0 };
    u8 m_register_index : 7 { 0x7f };
    bool m_has_sib : 1 { false };
};

class Instruction : public Disassembly::Instruction {
public:
    template<typename InstructionStreamType>
    static Instruction from_stream(InstructionStreamType&, ProcessorMode);
    virtual ~Instruction() = default;

    ALWAYS_INLINE MemoryOrRegisterReference& modrm() const { return m_modrm; }

    ALWAYS_INLINE InstructionHandler handler() const { return m_descriptor->handler; }

    bool has_segment_prefix() const { return m_segment_prefix != 0xff; }
    ALWAYS_INLINE Optional<SegmentRegister> segment_prefix() const
    {
        if (has_segment_prefix())
            return static_cast<SegmentRegister>(m_segment_prefix);
        return {};
    }

    bool has_address_size_override_prefix() const { return m_has_address_size_override_prefix; }
    bool has_operand_size_override_prefix() const { return m_has_operand_size_override_prefix; }
    bool has_lock_prefix() const { return m_has_lock_prefix; }
    bool has_rep_prefix() const { return m_rep_prefix; }
    u8 rep_prefix() const { return m_rep_prefix; }

    bool is_valid() const { return m_descriptor; }

    virtual size_t length() const override;

    virtual ByteString mnemonic() const override;

    u8 op() const { return m_op; }
    u8 modrm_byte() const { return m_modrm.modrm_byte(); }
    u8 slash() const { return m_modrm.reg() & 7; }

    u8 imm8() const { return m_imm1; }
    u16 imm16() const { return m_imm1; }
    u32 imm32() const { return m_imm1; }
    u64 imm64() const { return m_imm1; }
    u8 imm8_1() const { return imm8(); }
    u8 imm8_2() const { return m_imm2; }
    u16 imm16_1() const { return imm16(); }
    u16 imm16_2() const { return m_imm2; }
    u32 imm32_1() const { return imm32(); }
    u32 imm32_2() const { return m_imm2; }
    u64 imm64_1() const { return imm64(); }
    u64 imm64_2() const { return m_imm2; }
    u32 imm_address() const
    {
        switch (m_address_size) {
        case AddressSize::Size64:
            return imm64();
        case AddressSize::Size32:
            return imm32();
        case AddressSize::Size16:
            return imm16();
        }
        VERIFY_NOT_REACHED();
    }

    LogicalAddress imm_address16_16() const { return LogicalAddress(imm16_1(), imm16_2()); }
    LogicalAddress imm_address16_32() const { return LogicalAddress(imm16_1(), imm32_2()); }

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

    AddressSize address_size() const { return m_address_size; }
    OperandSize operand_size() const { return m_operand_size; }
    ProcessorMode mode() const { return m_mode; }

    virtual ByteString to_byte_string(u32 origin, Optional<SymbolProvider const&> = {}) const override;

private:
    template<typename InstructionStreamType>
    Instruction(InstructionStreamType&, ProcessorMode);

    void to_byte_string_internal(StringBuilder&, u32 origin, Optional<SymbolProvider const&>, bool x32) const;

    StringView reg8_name() const;
    StringView reg16_name() const;
    StringView reg32_name() const;
    StringView reg64_name() const;

    InstructionDescriptor* m_descriptor { nullptr };
    mutable MemoryOrRegisterReference m_modrm;
    u64 m_imm1 { 0 };
    u64 m_imm2 { 0 };
    u8 m_segment_prefix { 0xff };
    u8 m_register_index { 0xff };
    u8 m_op { 0 };
    u8 m_sub_op { 0 };
    u8 m_extra_bytes { 0 };
    u8 m_rep_prefix { 0 };
    OperandSize m_operand_size { OperandSize::Size16 };
    AddressSize m_address_size { AddressSize::Size16 };
    ProcessorMode m_mode { ProcessorMode::Protected };
    bool m_has_lock_prefix : 1 { false };
    bool m_has_operand_size_override_prefix : 1 { false };
    bool m_has_address_size_override_prefix : 1 { false };
    bool m_has_rex_w : 1 { false };
    bool m_has_rex_r : 1 { false };
    bool m_has_rex_x : 1 { false };
    bool m_has_rex_b : 1 { false };
};

template<typename CPU>
ALWAYS_INLINE LogicalAddress MemoryOrRegisterReference::resolve16(const CPU& cpu, Optional<SegmentRegister> segment_prefix)
{
    auto default_segment = SegmentRegister::DS;
    u16 offset = 0;

    switch (rm()) {
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
        if (mod() == 0)
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

    switch (rm()) {
    case 0 ... 3:
    case 6 ... 7:
        offset = cpu.const_gpr32((RegisterIndex32)(rm())).value() + m_displacement32;
        break;
    case 4:
        offset = evaluate_sib(cpu, default_segment);
        break;
    default: // 5
        if (mod() == 0) {
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
    u32 scale_shift = m_sib_scale;
    u32 index = 0;
    switch (m_sib_index) {
    case 0 ... 3:
    case 5 ... 15:
        index = cpu.const_gpr32((RegisterIndex32)m_sib_index).value();
        break;
    case 4:
        index = 0;
        break;
    }

    u32 base = m_displacement32;
    switch (m_sib_base) {
    case 0 ... 3:
    case 6 ... 15:
        base += cpu.const_gpr32((RegisterIndex32)m_sib_base).value();
        break;
    case 4:
        default_segment = SegmentRegister::SS;
        base += cpu.esp().value();
        break;
    default: // 5
        switch (mod()) {
        case 0:
            break;
        case 1:
        case 2:
            default_segment = SegmentRegister::SS;
            base += cpu.ebp().value();
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    }

    return (index << scale_shift) + base;
}

template<typename CPU, typename T>
ALWAYS_INLINE void MemoryOrRegisterReference::write8(CPU& cpu, Instruction const& insn, T value)
{
    if (is_register()) {
        cpu.gpr8(reg8()) = value;
        return;
    }

    auto address = resolve(cpu, insn);
    cpu.write_memory8(address, value);
}

template<typename CPU, typename T>
ALWAYS_INLINE void MemoryOrRegisterReference::write16(CPU& cpu, Instruction const& insn, T value)
{
    if (is_register()) {
        cpu.gpr16(reg16()) = value;
        return;
    }

    auto address = resolve(cpu, insn);
    cpu.write_memory16(address, value);
}

template<typename CPU, typename T>
ALWAYS_INLINE void MemoryOrRegisterReference::write32(CPU& cpu, Instruction const& insn, T value)
{
    if (is_register()) {
        cpu.gpr32(reg32()) = value;
        return;
    }

    auto address = resolve(cpu, insn);
    cpu.write_memory32(address, value);
}

template<typename CPU, typename T>
ALWAYS_INLINE void MemoryOrRegisterReference::write64(CPU& cpu, Instruction const& insn, T value)
{
    VERIFY(!is_register());
    auto address = resolve(cpu, insn);
    cpu.write_memory64(address, value);
}

template<typename CPU, typename T>
ALWAYS_INLINE void MemoryOrRegisterReference::write128(CPU& cpu, Instruction const& insn, T value)
{
    VERIFY(!is_register());
    auto address = resolve(cpu, insn);
    cpu.write_memory128(address, value);
}

template<typename CPU, typename T>
ALWAYS_INLINE void MemoryOrRegisterReference::write256(CPU& cpu, Instruction const& insn, T value)
{
    VERIFY(!is_register());
    auto address = resolve(cpu, insn);
    cpu.write_memory256(address, value);
}

template<typename CPU>
ALWAYS_INLINE typename CPU::ValueWithShadowType8 MemoryOrRegisterReference::read8(CPU& cpu, Instruction const& insn)
{
    if (is_register())
        return cpu.const_gpr8(reg8());

    auto address = resolve(cpu, insn);
    return cpu.read_memory8(address);
}

template<typename CPU>
ALWAYS_INLINE typename CPU::ValueWithShadowType16 MemoryOrRegisterReference::read16(CPU& cpu, Instruction const& insn)
{
    if (is_register())
        return cpu.const_gpr16(reg16());

    auto address = resolve(cpu, insn);
    return cpu.read_memory16(address);
}

template<typename CPU>
ALWAYS_INLINE typename CPU::ValueWithShadowType32 MemoryOrRegisterReference::read32(CPU& cpu, Instruction const& insn)
{
    if (is_register())
        return cpu.const_gpr32(reg32());

    auto address = resolve(cpu, insn);
    return cpu.read_memory32(address);
}

template<typename CPU>
ALWAYS_INLINE typename CPU::ValueWithShadowType64 MemoryOrRegisterReference::read64(CPU& cpu, Instruction const& insn)
{
    VERIFY(!is_register());
    auto address = resolve(cpu, insn);
    return cpu.read_memory64(address);
}

template<typename CPU>
ALWAYS_INLINE typename CPU::ValueWithShadowType128 MemoryOrRegisterReference::read128(CPU& cpu, Instruction const& insn)
{
    VERIFY(!is_register());
    auto address = resolve(cpu, insn);
    return cpu.read_memory128(address);
}

template<typename CPU>
ALWAYS_INLINE typename CPU::ValueWithShadowType256 MemoryOrRegisterReference::read256(CPU& cpu, Instruction const& insn)
{
    VERIFY(!is_register());
    auto address = resolve(cpu, insn);
    return cpu.read_memory256(address);
}

template<typename InstructionStreamType>
ALWAYS_INLINE Instruction Instruction::from_stream(InstructionStreamType& stream, ProcessorMode mode)
{
    return Instruction(stream, mode);
}

ALWAYS_INLINE size_t Instruction::length() const
{
    size_t len = 1;
    if (has_sub_op())
        ++len;
    if (m_descriptor && m_descriptor->has_rm) {
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
ALWAYS_INLINE Instruction::Instruction(InstructionStreamType& stream, ProcessorMode mode)
    : m_mode(mode)
{
    m_operand_size = OperandSize::Size32;
    // m_address_size refers to the default size of displacements/immediates, which is 32 even in long mode (2.2.1.3 Displacement, 2.2.1.5 Immediates),
    // with the exception of moffset (see below).
    m_address_size = AddressSize::Size32;

    u8 prefix_bytes = 0;
    for (;; ++prefix_bytes) {
        u8 opbyte = stream.read8();
        if (opbyte == Prefix::OperandSizeOverride) {
            if (m_operand_size == OperandSize::Size32)
                m_operand_size = OperandSize::Size16;
            else if (m_operand_size == OperandSize::Size16)
                m_operand_size = OperandSize::Size32;
            m_has_operand_size_override_prefix = true;
            continue;
        }
        if (opbyte == Prefix::AddressSizeOverride) {
            if (m_address_size == AddressSize::Size32)
                m_address_size = AddressSize::Size16;
            else if (m_address_size == AddressSize::Size16)
                m_address_size = AddressSize::Size32;
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
        if (m_mode == ProcessorMode::Long && (opbyte & Prefix::REX_Mask) == Prefix::REX_Base) {
            m_has_rex_w = opbyte & 8;
            if (m_has_rex_w)
                m_operand_size = OperandSize::Size64;
            m_has_rex_r = opbyte & 4;
            m_has_rex_x = opbyte & 2;
            m_has_rex_b = opbyte & 1;
            continue;
        }
        auto segment_prefix = to_segment_prefix(opbyte);
        if (segment_prefix.has_value()) {
            m_segment_prefix = (u8)segment_prefix.value();
            continue;
        }
        m_op = opbyte;
        break;
    }

    u8 table_index = to_underlying(m_operand_size);
    if (m_mode == ProcessorMode::Long && m_operand_size == OperandSize::Size32)
        table_index = to_underlying(OperandSize::Size64);
    if (m_op == 0x0f) {
        m_sub_op = stream.read8();
        m_descriptor = &s_0f_table[table_index][m_sub_op];
    } else {
        m_descriptor = &s_table[table_index][m_op];
    }

    if (m_descriptor->format == __SSE) {
        if (m_rep_prefix == 0xF2) {
            m_descriptor = &s_sse_table_f2[m_sub_op];
        } else if (m_rep_prefix == 0xF3) {
            m_descriptor = &s_sse_table_f3[m_sub_op];
        } else if (m_has_operand_size_override_prefix) {
            // This was unset while parsing the prefix initially
            m_operand_size = OperandSize::Size32;
            m_descriptor = &s_sse_table_66[m_sub_op];
        } else {
            m_descriptor = &s_sse_table_np[m_sub_op];
        }
    }

    if (m_descriptor->has_rm) {
        // Consume ModR/M (may include SIB and displacement.)
        m_modrm.decode(stream, m_address_size, m_has_rex_r, m_has_rex_x, m_has_rex_b);
        m_register_index = m_modrm.reg();
    } else {
        if (has_sub_op())
            m_register_index = m_sub_op & 7;
        else
            m_register_index = m_op & 7;
        if (m_has_rex_b)
            m_register_index |= 8;
    }

    if (m_mode == ProcessorMode::Long && (m_descriptor->long_mode_force_64 || m_descriptor->long_mode_default_64)) {
        m_operand_size = OperandSize::Size64;
        if (!m_descriptor->long_mode_force_64 && m_has_operand_size_override_prefix)
            m_operand_size = OperandSize::Size32;
    }

    bool has_slash = m_descriptor->format == MultibyteWithSlash;
    if (has_slash) {
        m_descriptor = &m_descriptor->slashes[slash()];
        if ((modrm_byte() & 0xc0) == 0xc0 && m_descriptor->slashes)
            m_descriptor = &m_descriptor->slashes[modrm_byte() & 7];
    }

    if (!m_descriptor->mnemonic) {
        if (has_sub_op()) {
            if (has_slash)
                warnln("Instruction {:02X} {:02X} /{} not understood", m_op, m_sub_op, slash());
            else
                warnln("Instruction {:02X} {:02X} not understood", m_op, m_sub_op);
        } else {
            if (has_slash)
                warnln("Instruction {:02X} /{} not understood", m_op, slash());
            else
                warnln("Instruction {:02X} not understood", m_op);
        }
        m_descriptor = nullptr;
        m_extra_bytes = prefix_bytes;
        return;
    }

    // 2.2.1.4 Direct Memory-Offset MOVs
    auto effective_address_size = m_address_size;
    if (m_mode == ProcessorMode::Long) {
        switch (m_descriptor->format) {
        case OP_AL_moff8:   // A0 MOV AL, moffset
        case OP_EAX_moff32: // A1 MOV EAX, moffset
        case OP_moff8_AL:   // A2 MOV moffset, AL
        case OP_moff32_EAX: // A3 MOV moffset, EAX
            effective_address_size = AddressSize::Size64;
            break;
        default:
            break;
        }
    }
    auto imm1_bytes = m_descriptor->imm1_bytes_for(effective_address_size, m_operand_size);
    auto imm2_bytes = m_descriptor->imm2_bytes_for(effective_address_size, m_operand_size);

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
    case 8:
        m_imm2 = stream.read64();
        break;
    default:
        VERIFY(imm2_bytes == 0);
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
    case 8:
        m_imm1 = stream.read64();
        break;
    default:
        VERIFY(imm1_bytes == 0);
        break;
    }

    m_extra_bytes = prefix_bytes + imm1_bytes + imm2_bytes;

#ifdef DISALLOW_INVALID_LOCK_PREFIX
    if (m_has_lock_prefix && !m_descriptor->lock_prefix_allowed) {
        warnln("Instruction not allowed with LOCK prefix, this will raise #UD");
        m_descriptor = nullptr;
    }
#endif
}

template<typename InstructionStreamType>
ALWAYS_INLINE void MemoryOrRegisterReference::decode(InstructionStreamType& stream, AddressSize address_size, bool has_rex_r, bool has_rex_x, bool has_rex_b)
{
    u8 mod_rm_byte = stream.read8();
    m_mod = mod_rm_byte >> 6;
    m_reg = (mod_rm_byte >> 3) & 7;
    m_rm = mod_rm_byte & 7;

    if (address_size == AddressSize::Size32) {
        decode32(stream, has_rex_r, has_rex_x, has_rex_b);
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
            VERIFY_NOT_REACHED();
        }
    } else if (address_size == AddressSize::Size16) {
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
            VERIFY_NOT_REACHED();
        }
    } else {
        VERIFY_NOT_REACHED();
    }
}

template<typename InstructionStreamType>
ALWAYS_INLINE void MemoryOrRegisterReference::decode16(InstructionStreamType&)
{
    switch (mod()) {
    case 0b00:
        if (rm() == 6)
            m_displacement_bytes = 2;
        else
            VERIFY(m_displacement_bytes == 0);
        break;
    case 0b01:
        m_displacement_bytes = 1;
        break;
    case 0b10:
        m_displacement_bytes = 2;
        break;
    case 0b11:
        m_register_index = rm();
        break;
    }
}

template<typename InstructionStreamType>
ALWAYS_INLINE void MemoryOrRegisterReference::decode32(InstructionStreamType& stream, bool has_rex_r, bool has_rex_x, bool has_rex_b)
{
    m_reg |= has_rex_r << 3;

    switch (m_mod) {
    case 0b00:
        if (m_rm == 5) {
            m_displacement_bytes = 4;
            return;
        }
        break;
    case 0b01:
        m_displacement_bytes = 1;
        break;
    case 0b10:
        m_displacement_bytes = 4;
        break;
    case 0b11:
        m_rm |= has_rex_b << 3;
        m_register_index = rm();
        return;
    }

    m_has_sib = m_rm == 4;
    if (m_has_sib) {
        u8 sib_byte = stream.read8();
        m_sib_scale = sib_byte >> 6;
        m_sib_index = (has_rex_x << 3) | ((sib_byte >> 3) & 7);
        m_sib_base = (has_rex_b << 3) | (sib_byte & 7);
        if (m_sib_base == 5) {
            switch (mod()) {
            case 0b00:
                m_displacement_bytes = 4;
                break;
            case 0b01:
                m_displacement_bytes = 1;
                break;
            case 0b10:
                m_displacement_bytes = 4;
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        }
    } else {
        m_rm |= has_rex_b << 3;
    }
}

template<typename CPU>
ALWAYS_INLINE LogicalAddress MemoryOrRegisterReference::resolve(const CPU& cpu, Instruction const& insn)
{
    switch (insn.address_size()) {
    case AddressSize::Size16:
        return resolve16(cpu, insn.segment_prefix());
    case AddressSize::Size32:
        return resolve32(cpu, insn.segment_prefix());
    default:
        VERIFY_NOT_REACHED();
    }
}

}
