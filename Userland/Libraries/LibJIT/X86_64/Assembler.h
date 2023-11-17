/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <AK/Vector.h>

namespace JIT {

struct X86_64Assembler {
    X86_64Assembler(Vector<u8>& output)
        : m_output(output)
    {
    }

    Vector<u8>& m_output;

    enum class Reg {
        RAX = 0,
        RCX = 1,
        RDX = 2,
        RBX = 3,
        RSP = 4,
        RBP = 5,
        RSI = 6,
        RDI = 7,
        R8 = 8,
        R9 = 9,
        R10 = 10,
        R11 = 11,
        R12 = 12,
        R13 = 13,
        R14 = 14,
        R15 = 15,
        XMM0 = 0,
        XMM1 = 1,
        XMM2 = 2,
        XMM3 = 3,
        XMM4 = 4,
        XMM5 = 5,
        XMM6 = 6,
        XMM7 = 7,
        XMM8 = 8,
        XMM9 = 9,
        XMM10 = 10,
        XMM11 = 11,
        XMM12 = 12,
        XMM13 = 13,
        XMM14 = 14,
        XMM15 = 15,
    };

    struct Operand {
        enum class Type {
            Reg,
            FReg,
            Imm,
            Mem64BaseAndOffset,
        };

        Type type {};

        Reg reg {};
        u64 offset_or_immediate { 0 };

        static Operand Register(Reg reg)
        {
            Operand operand;
            operand.type = Type::Reg;
            operand.reg = reg;
            return operand;
        }

        static Operand FloatRegister(Reg reg)
        {
            Operand operand;
            operand.type = Type::FReg;
            operand.reg = reg;
            return operand;
        }

        static Operand Imm(u64 imm)
        {
            Operand operand;
            operand.type = Type::Imm;
            operand.offset_or_immediate = imm;
            return operand;
        }

        static Operand Mem64BaseAndOffset(Reg base, u64 offset)
        {
            Operand operand;
            operand.type = Type::Mem64BaseAndOffset;
            operand.reg = base;
            operand.offset_or_immediate = offset;
            return operand;
        }

        bool is_register_or_memory() const
        {
            return type == Type::Reg || type == Type::Mem64BaseAndOffset;
        }

        bool fits_in_u8() const
        {
            VERIFY(type == Type::Imm);
            return offset_or_immediate <= NumericLimits<u8>::max();
        }
        bool fits_in_u32() const
        {
            VERIFY(type == Type::Imm);
            return offset_or_immediate <= NumericLimits<u32>::max();
        }
        bool fits_in_i8() const
        {
            VERIFY(type == Type::Imm);
            return (offset_or_immediate <= NumericLimits<i8>::max()) || (((~offset_or_immediate) & NumericLimits<i8>::min()) == 0);
        }
        bool fits_in_i32() const
        {
            VERIFY(type == Type::Imm);
            return (offset_or_immediate <= NumericLimits<i32>::max()) || (((~offset_or_immediate) & NumericLimits<i32>::min()) == 0);
        }
    };

    enum class Condition {
        Overflow = 0x0,
        EqualTo = 0x4,
        NotEqualTo = 0x5,
        UnsignedGreaterThan = 0x7,
        UnsignedGreaterThanOrEqualTo = 0x3,
        UnsignedLessThan = 0x2,
        UnsignedLessThanOrEqualTo = 0x6,
        ParityEven = 0xA,
        ParityOdd = 0xB,
        SignedGreaterThan = 0xF,
        SignedGreaterThanOrEqualTo = 0xD,
        SignedLessThan = 0xC,
        SignedLessThanOrEqualTo = 0xE,

        Unordered = ParityEven,
        NotUnordered = ParityOdd,

        Below = UnsignedLessThan,
        BelowOrEqual = UnsignedLessThanOrEqualTo,
        Above = UnsignedGreaterThan,
        AboveOrEqual = UnsignedGreaterThanOrEqualTo,
    };

    static constexpr u8 encode_reg(Reg reg)
    {
        return to_underlying(reg) & 0x7;
    }

    enum class Patchable {
        Yes,
        No,
    };

    union ModRM {
        static constexpr u8 Mem = 0b00;
        static constexpr u8 MemDisp8 = 0b01;
        static constexpr u8 MemDisp32 = 0b10;
        static constexpr u8 Reg = 0b11;
        struct {
            u8 rm : 3;
            u8 reg : 3;
            u8 mode : 2;
        };
        u8 raw;
    };

    void emit_modrm_slash(u8 slash, Operand rm, Patchable patchable = Patchable::No)
    {
        ModRM raw;
        raw.rm = encode_reg(rm.reg);
        raw.reg = slash;
        emit_modrm(raw, rm, patchable);
    }

    void emit_modrm_rm(Operand dst, Operand src, Patchable patchable = Patchable::No)
    {
        VERIFY(dst.type == Operand::Type::Reg || dst.type == Operand::Type::FReg);
        ModRM raw {};
        raw.reg = encode_reg(dst.reg);
        raw.rm = encode_reg(src.reg);
        emit_modrm(raw, src, patchable);
    }

    void emit_modrm_mr(Operand dst, Operand src, Patchable patchable = Patchable::No)
    {
        VERIFY(src.type == Operand::Type::Reg || src.type == Operand::Type::FReg);
        ModRM raw {};
        raw.reg = encode_reg(src.reg);
        raw.rm = encode_reg(dst.reg);
        emit_modrm(raw, dst, patchable);
    }

    void emit_modrm(ModRM raw, Operand rm, Patchable patchable)
    {
        // FIXME: rm:100 (RSP) is reserved as the SIB marker
        VERIFY(rm.type != Operand::Type::Imm);

        switch (rm.type) {
        case Operand::Type::FReg:
        case Operand::Type::Reg:
            // FIXME: There is mod:00,rm:101(EBP?) -> disp32, that might be something else
            raw.mode = ModRM::Reg;
            emit8(raw.raw);
            break;
        case Operand::Type::Mem64BaseAndOffset: {
            auto disp = rm.offset_or_immediate;
            if (patchable == Patchable::Yes) {
                raw.mode = ModRM::MemDisp32;
                emit8(raw.raw);
                emit32(disp);
            } else if (disp == 0) {
                raw.mode = ModRM::Mem;
                emit8(raw.raw);
            } else if (static_cast<i64>(disp) >= -128 && disp <= 127) {
                raw.mode = ModRM::MemDisp8;
                emit8(raw.raw);
                emit8(disp & 0xff);
            } else {
                raw.mode = ModRM::MemDisp32;
                emit8(raw.raw);
                emit32(disp);
            }
            break;
        }
        case Operand::Type::Imm:
            VERIFY_NOT_REACHED();
        }
    }

    union REX {
        struct {
            u8 B : 1; // ModRM::RM
            u8 X : 1; // SIB::Index
            u8 R : 1; // ModRM::Reg
            u8 W : 1; // Operand size override
            u8 _ : 4 { 0b0100 };
        };
        u8 raw;
    };

    enum class REX_W : bool {
        No = 0,
        Yes = 1
    };

    void emit_rex_for_OI(Operand arg, REX_W W)
    {
        emit_rex_for_slash(arg, W);
    }

    void emit_rex_for_slash(Operand arg, REX_W W)
    {
        VERIFY(arg.is_register_or_memory());

        if (W == REX_W::No && to_underlying(arg.reg) < 8)
            return;

        REX rex {
            .B = to_underlying(arg.reg) >= 8,
            .X = 0,
            .R = 0,
            .W = to_underlying(W)
        };
        emit8(rex.raw);
    }
    void emit_rex_for_mr(Operand dst, Operand src, REX_W W)
    {
        VERIFY(dst.is_register_or_memory() || dst.type == Operand::Type::FReg);
        VERIFY(src.type == Operand::Type::Reg || src.type == Operand::Type::FReg);
        if (W == REX_W::No && to_underlying(dst.reg) < 8 && to_underlying(src.reg) < 8)
            return;
        REX rex {
            .B = to_underlying(dst.reg) >= 8,
            .X = 0,
            .R = to_underlying(src.reg) >= 8,
            .W = to_underlying(W)
        };
        emit8(rex.raw);
    }

    void emit_rex_for_rm(Operand dst, Operand src, REX_W W)
    {
        VERIFY(src.is_register_or_memory() || src.type == Operand::Type::FReg);
        VERIFY(dst.type == Operand::Type::Reg || dst.type == Operand::Type::FReg);
        if (W == REX_W::No && to_underlying(dst.reg) < 8 && to_underlying(src.reg) < 8)
            return;
        REX rex {
            .B = to_underlying(src.reg) >= 8,
            .X = 0,
            .R = to_underlying(dst.reg) >= 8,
            .W = to_underlying(W)
        };
        emit8(rex.raw);
    }

    void shift_right(Operand dst, Operand count)
    {
        VERIFY(dst.type == Operand::Type::Reg);
        VERIFY(count.type == Operand::Type::Imm);
        VERIFY(count.fits_in_u8());
        emit_rex_for_slash(dst, REX_W::Yes);
        emit8(0xc1);
        emit_modrm_slash(5, dst);
        emit8(count.offset_or_immediate);
    }

    void mov(Operand dst, Operand src, Patchable patchable = Patchable::No)
    {
        if (dst.is_register_or_memory() && src.type == Operand::Type::Reg) {
            if (src.type == Operand::Type::Reg && src.reg == dst.reg)
                return;
            emit_rex_for_mr(dst, src, REX_W::Yes);
            emit8(0x89);
            emit_modrm_mr(dst, src, patchable);
            return;
        }

        if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm) {
            if (patchable == Patchable::No) {
                if (src.offset_or_immediate == 0) {
                    // xor dst, dst
                    // Note: Operand size does not matter here, as the result is 0-extended to 64 bit,
                    //       so we don't have to set the W flag in the REX prefix,
                    //       or use it at all in case we dont use REX addressed registers (elision is implemented in the helper)
                    emit_rex_for_mr(dst, dst, REX_W::No);
                    emit8(0x31);
                    emit_modrm_mr(dst, dst, patchable);
                    return;
                }
                if (src.fits_in_u32()) {
                    emit_rex_for_OI(dst, REX_W::No);
                    emit8(0xb8 | encode_reg(dst.reg));
                    emit32(src.offset_or_immediate);
                    return;
                }
            }
            emit_rex_for_OI(dst, REX_W::Yes);
            emit8(0xb8 | encode_reg(dst.reg));
            emit64(src.offset_or_immediate);
            return;
        }

        if (dst.type == Operand::Type::Reg && src.is_register_or_memory()) {
            emit_rex_for_rm(dst, src, REX_W::Yes);
            emit8(0x8b);
            emit_modrm_rm(dst, src, patchable);
            return;
        }

        if (dst.type == Operand::Type::FReg && src.is_register_or_memory()) {
            emit8(0x66);
            emit_rex_for_rm(dst, src, REX_W::Yes);
            emit8(0x0f);
            emit8(0x6e);
            emit_modrm_rm(dst, src, patchable);
            return;
        }

        if (dst.is_register_or_memory() && src.type == Operand::Type::FReg) {
            emit8(0x66);
            emit_rex_for_mr(dst, src, REX_W::Yes);
            emit8(0x0f);
            emit8(0x7e);
            emit_modrm_mr(dst, src, patchable);
            return;
        }

        VERIFY_NOT_REACHED();
    }

    enum Extension {
        ZeroExtend,
        SignExtend,
    };

    void mov8(Operand dst, Operand src, Extension extension = Extension::ZeroExtend)
    {
        VERIFY(dst.type == Operand::Type::Reg && src.type == Operand::Type::Mem64BaseAndOffset);
        // mov[sz]x r32, r/m8
        emit_rex_for_rm(dst, src, REX_W::No);
        emit8(0x0f);
        emit8(extension == Extension::ZeroExtend ? 0xb6 : 0xbe);
        emit_modrm_rm(dst, src);
    }

    void mov16(Operand dst, Operand src, Extension extension = Extension::ZeroExtend)
    {
        VERIFY(dst.type == Operand::Type::Reg && src.is_register_or_memory());
        // mov[sz]x r32, r/m16
        emit_rex_for_rm(dst, src, REX_W::No);
        emit8(0x0f);
        emit8(extension == Extension::ZeroExtend ? 0xb7 : 0xbf);
        emit_modrm_rm(dst, src);
    }

    void mov32(Operand dst, Operand src, Extension extension = Extension::ZeroExtend)
    {
        VERIFY(dst.type == Operand::Type::Reg && src.is_register_or_memory());
        if (extension == Extension::ZeroExtend) {
            // mov r32, r/m32
            emit_rex_for_rm(dst, src, REX_W::No);
            emit8(0x8b);
            emit_modrm_rm(dst, src);
            return;
        }
        VERIFY(extension == Extension::SignExtend);
        // movsxd r64, r/m32
        emit_rex_for_rm(dst, src, REX_W::Yes);
        emit8(0x63);
        emit_modrm_rm(dst, src);
    }

    void emit8(u8 value)
    {
        m_output.append(value);
    }

    void emit32(u32 value)
    {
        m_output.append((value >> 0) & 0xff);
        m_output.append((value >> 8) & 0xff);
        m_output.append((value >> 16) & 0xff);
        m_output.append((value >> 24) & 0xff);
    }

    void emit64(u64 value)
    {
        m_output.append((value >> 0) & 0xff);
        m_output.append((value >> 8) & 0xff);
        m_output.append((value >> 16) & 0xff);
        m_output.append((value >> 24) & 0xff);
        m_output.append((value >> 32) & 0xff);
        m_output.append((value >> 40) & 0xff);
        m_output.append((value >> 48) & 0xff);
        m_output.append((value >> 56) & 0xff);
    }

    struct Label {
        Optional<size_t> offset_of_label_in_instruction_stream;
        Vector<size_t> jump_slot_offsets_in_instruction_stream;

        void add_jump(X86_64Assembler& assembler, size_t offset)
        {
            jump_slot_offsets_in_instruction_stream.append(offset);
            if (offset_of_label_in_instruction_stream.has_value())
                link_jump(assembler, offset);
        }

        void link(X86_64Assembler& assembler)
        {
            link_to(assembler, assembler.m_output.size());
        }

        void link_to(X86_64Assembler& assembler, size_t link_offset)
        {
            VERIFY(!offset_of_label_in_instruction_stream.has_value());
            offset_of_label_in_instruction_stream = link_offset;
            for (auto offset_in_instruction_stream : jump_slot_offsets_in_instruction_stream)
                link_jump(assembler, offset_in_instruction_stream);
        }

    private:
        void link_jump(X86_64Assembler& assembler, size_t offset_in_instruction_stream)
        {
            auto offset = offset_of_label_in_instruction_stream.value() - offset_in_instruction_stream;
            auto jump_slot = offset_in_instruction_stream - 4;
            assembler.m_output[jump_slot + 0] = (offset >> 0) & 0xff;
            assembler.m_output[jump_slot + 1] = (offset >> 8) & 0xff;
            assembler.m_output[jump_slot + 2] = (offset >> 16) & 0xff;
            assembler.m_output[jump_slot + 3] = (offset >> 24) & 0xff;
        }
    };

    [[nodiscard]] Label jump()
    {
        // jmp target (RIP-relative 32-bit offset)
        emit8(0xe9);
        emit32(0xdeadbeef);
        X86_64Assembler::Label label {};
        label.add_jump(*this, m_output.size());
        return label;
    }

    void jump(Label& label)
    {
        // jmp target (RIP-relative 32-bit offset)
        emit8(0xe9);
        emit32(0xdeadbeef);
        label.add_jump(*this, m_output.size());
    }

    void jump(Operand op)
    {
        emit_rex_for_slash(op, REX_W::No);
        emit8(0xff);
        emit_modrm_slash(4, op);
    }

    void verify_not_reached()
    {
        // ud2
        emit8(0x0f);
        emit8(0x0b);
    }

    void cmp(Operand lhs, Operand rhs)
    {
        if (lhs.type == Operand::Type::Reg && rhs.type == Operand::Type::Imm && rhs.offset_or_immediate == 0) {
            test(lhs, lhs);
        } else if (lhs.is_register_or_memory() && rhs.type == Operand::Type::Reg) {
            emit_rex_for_mr(lhs, rhs, REX_W::Yes);
            emit8(0x39);
            emit_modrm_mr(lhs, rhs);
        } else if (lhs.is_register_or_memory() && rhs.type == Operand::Type::Imm && rhs.fits_in_i8()) {
            emit_rex_for_slash(lhs, REX_W::Yes);
            emit8(0x83);
            emit_modrm_slash(7, lhs);
            emit8(rhs.offset_or_immediate);
        } else if (lhs.is_register_or_memory() && rhs.type == Operand::Type::Imm && rhs.fits_in_i32()) {
            emit_rex_for_slash(lhs, REX_W::Yes);
            emit8(0x81);
            emit_modrm_slash(7, lhs);
            emit32(rhs.offset_or_immediate);
        } else if (lhs.type == Operand::Type::FReg && (rhs.type == Operand::Type::FReg || rhs.type == Operand::Type::Mem64BaseAndOffset)) {
            // ucomisd lhs, rhs
            emit8(0x66);
            emit_rex_for_rm(lhs, rhs, REX_W::No);
            emit8(0x0f);
            emit8(0x2e);
            emit_modrm_rm(lhs, rhs);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void test(Operand lhs, Operand rhs)
    {
        if (lhs.is_register_or_memory() && rhs.type == Operand::Type::Reg) {
            emit_rex_for_mr(lhs, rhs, REX_W::Yes);
            emit8(0x85);
            emit_modrm_mr(lhs, rhs);
        } else if (lhs.type != Operand::Type::Imm && rhs.type == Operand::Type::Imm) {
            VERIFY(rhs.fits_in_i32());
            emit_rex_for_slash(lhs, REX_W::Yes);
            emit8(0xf7);
            emit_modrm_slash(0, lhs);
            emit32(rhs.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void jump_if(Condition condition, Label& label)
    {
        emit8(0x0F);
        emit8(0x80 | to_underlying(condition));
        emit32(0xdeadbeef);
        label.add_jump(*this, m_output.size());
    }

    void jump_if(Operand lhs, Condition condition, Operand rhs, Label& label)
    {
        cmp(lhs, rhs);
        jump_if(condition, label);
    }

    void set_if(Condition condition, Operand dst)
    {
        emit_rex_for_slash(dst, REX_W::No);
        emit8(0x0f);
        emit8(0x90 | to_underlying(condition));
        emit_modrm_slash(0, dst);
    }

    void mov_if(Condition condition, Operand dst, Operand src)
    {
        VERIFY(dst.type == Operand::Type::Reg && src.type == Operand::Type::Reg);
        emit_rex_for_rm(dst, src, REX_W::Yes);
        emit8(0x0f);
        emit8(0x40 | to_underlying(condition));
        emit_modrm_rm(dst, src);
    }

    void sign_extend_32_to_64_bits(Reg reg)
    {
        mov32(Operand::Register(reg), Operand::Register(reg), Extension::SignExtend);
    }

    void bitwise_and(Operand dst, Operand src)
    {
        // and dst,src
        if (dst.is_register_or_memory() && src.type == Operand::Type::Reg) {
            emit_rex_for_mr(dst, src, REX_W::Yes);
            emit8(0x21);
            emit_modrm_mr(dst, src);
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm && src.fits_in_i8()) {
            emit_rex_for_slash(dst, REX_W::Yes);
            emit8(0x83);
            emit_modrm_slash(4, dst);
            emit8(src.offset_or_immediate);
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm && src.fits_in_i32()) {
            emit_rex_for_slash(dst, REX_W::Yes);
            emit8(0x81);
            emit_modrm_slash(4, dst);
            emit32(src.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void bitwise_or(Operand dst, Operand src)
    {
        // or dst,src
        if (dst.is_register_or_memory() && src.type == Operand::Type::Reg) {
            emit_rex_for_mr(dst, src, REX_W::Yes);
            emit8(0x09);
            emit_modrm_mr(dst, src);
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm && src.fits_in_i8()) {
            emit_rex_for_slash(dst, REX_W::Yes);
            emit8(0x83);
            emit_modrm_slash(1, dst);
            emit8(src.offset_or_immediate);
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm && src.fits_in_i32()) {
            emit_rex_for_slash(dst, REX_W::Yes);
            emit8(0x81);
            emit_modrm_slash(1, dst);
            emit32(src.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void bitwise_xor32(Operand dst, Operand src)
    {
        if (dst.is_register_or_memory() && src.type == Operand::Type::Reg) {
            emit_rex_for_mr(dst, src, REX_W::No);
            emit8(0x31);
            emit_modrm_mr(dst, src);
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm && src.fits_in_i8()) {
            emit_rex_for_slash(dst, REX_W::No);
            emit8(0x83);
            emit_modrm_slash(6, dst);
            emit8(src.offset_or_immediate);
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm && src.fits_in_i32()) {
            emit_rex_for_slash(dst, REX_W::No);
            emit8(0x81);
            emit_modrm_slash(6, dst);
            emit32(src.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void mul(Operand dest, Operand src)
    {
        if (dest.type == Operand::Type::FReg && src.type == Operand::Type::FReg) {
            emit8(0xf2);
            emit8(0x0f);
            emit8(0x59);
            emit_modrm_rm(dest, src);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void mul32(Operand dest, Operand src, Optional<Label&> overflow_label)
    {
        // imul32 dest, src (32-bit signed)
        if (dest.type == Operand::Type::Reg && src.type == Operand::Type::Reg) {
            emit_rex_for_rm(dest, src, REX_W::No);
            emit8(0x0f);
            emit8(0xaf);
            emit_modrm_rm(dest, src);
        } else if (dest.type == Operand::Type::Reg && src.type == Operand::Type::Mem64BaseAndOffset) {
            emit_rex_for_rm(dest, src, REX_W::No);
            emit8(0x0f);
            emit8(0xaf);
            emit_modrm_rm(dest, src);
        } else if (dest.type == Operand::Type::Reg && src.type == Operand::Type::Imm) {
            if (src.fits_in_i8()) {
                emit_rex_for_rm(dest, dest, REX_W::No);
                emit8(0x6b);
                emit_modrm_rm(dest, dest);
                emit8(src.offset_or_immediate);
            } else if (src.fits_in_i32()) {
                emit_rex_for_rm(dest, dest, REX_W::No);
                emit8(0x69);
                emit_modrm_rm(dest, dest);
                emit32(src.offset_or_immediate);
            } else {
                VERIFY_NOT_REACHED();
            }
        } else {
            VERIFY_NOT_REACHED();
        }

        if (overflow_label.has_value()) {
            jump_if(Condition::Overflow, *overflow_label);
        }
    }

    void shift_left(Operand dest, Optional<Operand> count)
    {
        VERIFY(dest.type == Operand::Type::Reg);
        if (count.has_value()) {
            VERIFY(count->type == Operand::Type::Imm);
            VERIFY(count->fits_in_u8());
            emit_rex_for_slash(dest, REX_W::Yes);
            emit8(0xc1);
            emit_modrm_slash(4, dest);
            emit8(count->offset_or_immediate);
        } else {
            emit_rex_for_slash(dest, REX_W::Yes);
            emit8(0xd3);
            emit_modrm_slash(4, dest);
        }
    }

    void shift_left32(Operand dest, Optional<Operand> count)
    {
        VERIFY(dest.type == Operand::Type::Reg);
        if (count.has_value()) {
            VERIFY(count->type == Operand::Type::Imm);
            VERIFY(count->fits_in_u8());
            emit_rex_for_slash(dest, REX_W::No);
            emit8(0xc1);
            emit_modrm_slash(4, dest);
            emit8(count->offset_or_immediate);
        } else {
            emit_rex_for_slash(dest, REX_W::No);
            emit8(0xd3);
            emit_modrm_slash(4, dest);
        }
    }

    void shift_right32(Operand dest, Optional<Operand> count)
    {
        VERIFY(dest.type == Operand::Type::Reg);
        if (count.has_value()) {
            VERIFY(count->type == Operand::Type::Imm);
            VERIFY(count->fits_in_u8());
            emit_rex_for_slash(dest, REX_W::No);
            emit8(0xc1);
            emit_modrm_slash(5, dest);
            emit8(count->offset_or_immediate);
        } else {
            emit_rex_for_slash(dest, REX_W::No);
            emit8(0xd3);
            emit_modrm_slash(5, dest);
        }
    }

    void arithmetic_right_shift(Operand dest, Optional<Operand> count)
    {
        VERIFY(dest.type == Operand::Type::Reg);
        if (count.has_value()) {
            VERIFY(count->type == Operand::Type::Imm);
            VERIFY(count->fits_in_u8());
            emit_rex_for_slash(dest, REX_W::Yes);
            emit8(0xc1);
            emit_modrm_slash(7, dest);
            emit8(count->offset_or_immediate);
        } else {
            emit_rex_for_slash(dest, REX_W::Yes);
            emit8(0xd3);
            emit_modrm_slash(7, dest);
        }
    }

    void arithmetic_right_shift32(Operand dest, Optional<Operand> count)
    {
        VERIFY(dest.type == Operand::Type::Reg);
        if (count.has_value()) {
            VERIFY(count->type == Operand::Type::Imm);
            VERIFY(count->fits_in_u8());
            emit_rex_for_slash(dest, REX_W::No);
            emit8(0xc1);
            emit_modrm_slash(7, dest);
            emit8(count->offset_or_immediate);
        } else {
            emit_rex_for_slash(dest, REX_W::No);
            emit8(0xd3);
            emit_modrm_slash(7, dest);
        }
    }

    void enter()
    {
        push(Operand::Register(Reg::RBP));
        mov(Operand::Register(Reg::RBP), Operand::Register(Reg::RSP));

        push_callee_saved_registers();
    }

    void exit()
    {
        pop_callee_saved_registers();

        // leave
        emit8(0xc9);

        // ret
        emit8(0xc3);
    }

    void push_callee_saved_registers()
    {
        // FIXME: Don't push RBX twice :^)
        push(Operand::Register(Reg::RBX));
        push(Operand::Register(Reg::RBX));
        push(Operand::Register(Reg::R12));
        push(Operand::Register(Reg::R13));
        push(Operand::Register(Reg::R14));
        push(Operand::Register(Reg::R15));
    }

    void pop_callee_saved_registers()
    {
        pop(Operand::Register(Reg::R15));
        pop(Operand::Register(Reg::R14));
        pop(Operand::Register(Reg::R13));
        pop(Operand::Register(Reg::R12));

        // FIXME: Don't pop RBX twice :^)
        pop(Operand::Register(Reg::RBX));
        pop(Operand::Register(Reg::RBX));
    }

    void push(Operand op)
    {
        if (op.type == Operand::Type::Reg) {
            emit_rex_for_OI(op, REX_W::No);
            emit8(0x50 | encode_reg(op.reg));
        } else if (op.type == Operand::Type::Imm) {
            if (op.fits_in_i8()) {
                emit8(0x6a);
                emit8(op.offset_or_immediate);
            } else if (op.fits_in_i32()) {
                emit8(0x68);
                emit32(op.offset_or_immediate);
            } else {
                VERIFY_NOT_REACHED();
            }
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void pop(Operand op)
    {
        if (op.type == Operand::Type::Reg) {
            emit_rex_for_OI(op, REX_W::No);
            emit8(0x58 | encode_reg(op.reg));
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void inc32(Operand op, Optional<Label&> overflow_label)
    {
        if (op.is_register_or_memory()) {
            emit_rex_for_slash(op, REX_W::No);
            emit8(0xff);
            emit_modrm_slash(0, op);
        } else {
            VERIFY_NOT_REACHED();
        }

        if (overflow_label.has_value()) {
            jump_if(Condition::Overflow, *overflow_label);
        }
    }

    void dec32(Operand op, Optional<Label&> overflow_label)
    {
        if (op.is_register_or_memory()) {
            emit_rex_for_slash(op, REX_W::No);
            emit8(0xff);
            emit_modrm_slash(1, op);
        } else {
            VERIFY_NOT_REACHED();
        }

        if (overflow_label.has_value()) {
            jump_if(Condition::Overflow, *overflow_label);
        }
    }

    void add(Operand dst, Operand src)
    {
        if (dst.is_register_or_memory() && src.type == Operand::Type::Reg) {
            emit_rex_for_mr(dst, src, REX_W::Yes);
            emit8(0x01);
            emit_modrm_mr(dst, src);
        } else if (dst.is_register_or_memory() && src.type == Operand::Type::Imm && src.fits_in_i8()) {
            emit_rex_for_slash(dst, REX_W::Yes);
            emit8(0x83);
            emit_modrm_slash(0, dst);
            emit8(src.offset_or_immediate);
        } else if (dst.is_register_or_memory() && src.type == Operand::Type::Imm && src.fits_in_i32()) {
            emit_rex_for_slash(dst, REX_W::Yes);
            emit8(0x81);
            emit_modrm_slash(0, dst);
            emit32(src.offset_or_immediate);
        } else if (dst.type == Operand::Type::FReg && src.type == Operand::Type::FReg) {
            emit8(0xf2);
            emit8(0x0f);
            emit8(0x58);
            emit_modrm_rm(dst, src);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void add32(Operand dst, Operand src, Optional<Label&> overflow_label)
    {
        if (dst.is_register_or_memory() && src.type == Operand::Type::Reg) {
            emit_rex_for_mr(dst, src, REX_W::No);
            emit8(0x01);
            emit_modrm_mr(dst, src);
        } else if (dst.is_register_or_memory() && src.type == Operand::Type::Imm && src.fits_in_i8()) {
            emit_rex_for_slash(dst, REX_W::No);
            emit8(0x83);
            emit_modrm_slash(0, dst);
            emit8(src.offset_or_immediate);
        } else if (dst.is_register_or_memory() && src.type == Operand::Type::Imm && src.fits_in_i32()) {
            emit_rex_for_slash(dst, REX_W::No);
            emit8(0x81);
            emit_modrm_slash(0, dst);
            emit32(src.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }

        if (overflow_label.has_value()) {
            jump_if(Condition::Overflow, *overflow_label);
        }
    }

    void sub(Operand dst, Operand src)
    {
        if (dst.is_register_or_memory() && src.type == Operand::Type::Reg) {
            emit_rex_for_mr(dst, src, REX_W::Yes);
            emit8(0x29);
            emit_modrm_mr(dst, src);
        } else if (dst.is_register_or_memory() && src.type == Operand::Type::Imm && src.fits_in_i8()) {
            emit_rex_for_slash(dst, REX_W::Yes);
            emit8(0x83);
            emit_modrm_slash(5, dst);
            emit8(src.offset_or_immediate);
        } else if (dst.is_register_or_memory() && src.type == Operand::Type::Imm && src.fits_in_i32()) {
            emit_rex_for_slash(dst, REX_W::Yes);
            emit8(0x81);
            emit_modrm_slash(5, dst);
            emit32(src.offset_or_immediate);
        } else if (dst.type == Operand::Type::FReg && src.type == Operand::Type::FReg) {
            emit8(0xf2);
            emit8(0x0f);
            emit8(0x5c);
            emit_modrm_rm(dst, src);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void sub32(Operand dst, Operand src, Optional<Label&> overflow_label)
    {
        if (dst.is_register_or_memory() && src.type == Operand::Type::Reg) {
            emit_rex_for_mr(dst, src, REX_W::No);
            emit8(0x29);
            emit_modrm_mr(dst, src);
        } else if (dst.is_register_or_memory() && src.type == Operand::Type::Imm && src.fits_in_i8()) {
            emit_rex_for_slash(dst, REX_W::No);
            emit8(0x83);
            emit_modrm_slash(5, dst);
            emit8(src.offset_or_immediate);
        } else if (dst.is_register_or_memory() && src.type == Operand::Type::Imm && src.fits_in_i32()) {
            emit_rex_for_slash(dst, REX_W::No);
            emit8(0x81);
            emit_modrm_slash(5, dst);
            emit32(src.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }

        if (overflow_label.has_value()) {
            jump_if(Condition::Overflow, *overflow_label);
        }
    }

    void neg32(Operand reg)
    {
        VERIFY(reg.type == Operand::Type::Reg);
        emit_rex_for_slash(reg, REX_W::No);
        emit8(0xf7);
        emit_modrm_slash(3, reg);
    }

    void convert_i32_to_double(Operand dst, Operand src)
    {
        VERIFY(dst.type == Operand::Type::FReg);
        VERIFY(src.is_register_or_memory());

        emit8(0xf2);
        emit8(0x0f);
        emit8(0x2a);
        emit_modrm_rm(dst, src);
    }

    void native_call(
        u64 callee,
        Vector<Operand> const& preserved_registers = {},
        Vector<Operand> const& stack_arguments = {})
    {
        for (auto const& reg : preserved_registers.in_reverse())
            push(reg);

        // Preserve 16-byte stack alignment for non-even amount of stack-passed arguments
        auto needs_aligning = ((stack_arguments.size() + preserved_registers.size()) % 2) == 1;
        if (needs_aligning)
            push(Operand::Imm(0));
        for (auto const& stack_argument : stack_arguments.in_reverse())
            push(stack_argument);

        // load callee into RAX
        mov(Operand::Register(Reg::RAX), Operand::Imm(callee));

        // call RAX
        emit8(0xff);
        emit_modrm_slash(2, Operand::Register(Reg::RAX));

        if (!stack_arguments.is_empty() || needs_aligning)
            add(Operand::Register(Reg::RSP), Operand::Imm((stack_arguments.size() + (needs_aligning ? 1 : 0)) * sizeof(u64)));

        for (auto const& reg : preserved_registers)
            pop(reg);
    }

    void trap()
    {
        // int3
        emit8(0xcc);
    }
};

}
