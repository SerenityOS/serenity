/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibJS/Bytecode/BasicBlock.h>

namespace JS::JIT {

struct Assembler {
    Assembler(Vector<u8>& output)
        : m_output(output)
    {
    }

    Vector<u8>& m_output;

    enum class Reg {
        GPR0 = 0, // RAX
        GPR1 = 1, // RCX

        Arg0 = 7, // RDI
        Arg1 = 6, // RSI
        Arg2 = 2, // RDX

        Ret = 0, // RAX

        // FIXME: These are LibJS specific.
        RegisterArrayBase = 8, // R8
        LocalsArrayBase = 9,   // R9
    };

    struct Operand {
        enum class Type {
            Reg,
            Imm8,
            Imm32,
            Imm64,
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

        static Operand Imm8(u8 imm8)
        {
            Operand operand;
            operand.type = Type::Imm8;
            operand.offset_or_immediate = imm8;
            return operand;
        }

        static Operand Imm32(u32 imm32)
        {
            Operand operand;
            operand.type = Type::Imm32;
            operand.offset_or_immediate = imm32;
            return operand;
        }

        static Operand Imm64(u64 imm64)
        {
            Operand operand;
            operand.type = Type::Imm64;
            operand.offset_or_immediate = imm64;
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
    };

    static constexpr u8 encode_reg(Reg reg)
    {
        return to_underlying(reg) & 0x7;
    }

    void shift_right(Operand dst, Operand count)
    {
        VERIFY(dst.type == Operand::Type::Reg);
        VERIFY(count.type == Operand::Type::Imm8);
        emit8(0x48 | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
        emit8(0xc1);
        emit8(0xe8 | encode_reg(dst.reg));
        emit8(count.offset_or_immediate);
    }

    void mov(Operand dst, Operand src)
    {
        if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Reg) {
            if (src.reg == dst.reg)
                return;
            emit8(0x48
                | ((to_underlying(src.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x89);
            emit8(0xc0 | (encode_reg(src.reg) << 3) | encode_reg(dst.reg));
            return;
        }

        if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm64) {
            emit8(0x48 | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0xb8 | encode_reg(dst.reg));
            emit64(src.offset_or_immediate);
            return;
        }

        if (dst.type == Operand::Type::Mem64BaseAndOffset && src.type == Operand::Type::Reg) {
            emit8(0x48
                | ((to_underlying(src.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x89);
            emit8(0x80 | (encode_reg(src.reg) << 3) | encode_reg(dst.reg));
            emit32(dst.offset_or_immediate);
            return;
        }

        if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Mem64BaseAndOffset) {
            emit8(0x48
                | ((to_underlying(dst.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(src.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x8b);
            emit8(0x80 | (encode_reg(dst.reg) << 3) | encode_reg(src.reg));
            emit32(src.offset_or_immediate);
            return;
        }

        VERIFY_NOT_REACHED();
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

    void load_immediate64(Reg dst, u64 imm)
    {
        mov(Operand::Register(dst), Operand::Imm64(imm));
    }

    void increment(Reg dst)
    {
        emit8(0x48);
        emit8(0xff);
        emit8(0xc0 | to_underlying(dst));
    }

    void less_than(Reg dst, Reg src)
    {
        // cmp src, dst
        emit8(0x48);
        emit8(0x39);
        emit8(0xc0 | (to_underlying(src) << 3) | to_underlying(dst));

        // setl dst
        emit8(0x0f);
        emit8(0x9c);
        emit8(0xc0 | to_underlying(dst));

        // movzx dst, dst
        emit8(0x48);
        emit8(0x0f);
        emit8(0xb6);
        emit8(0xc0 | (to_underlying(dst) << 3) | to_underlying(dst));
    }

    struct Label {
        size_t offset_in_instruction_stream { 0 };

        void link(Assembler& assembler)
        {
            auto offset = assembler.m_output.size() - offset_in_instruction_stream;
            auto jump_slot = offset_in_instruction_stream - 4;
            assembler.m_output[jump_slot + 0] = (offset >> 0) & 0xff;
            assembler.m_output[jump_slot + 1] = (offset >> 8) & 0xff;
            assembler.m_output[jump_slot + 2] = (offset >> 16) & 0xff;
            assembler.m_output[jump_slot + 3] = (offset >> 24) & 0xff;
        }
    };

    [[nodiscard]] Label make_label()
    {
        return { .offset_in_instruction_stream = m_output.size() };
    }

    [[nodiscard]] Label jump()
    {
        // jmp target (RIP-relative 32-bit offset)
        emit8(0xe9);
        emit32(0xdeadbeef);
        return make_label();
    }

    void jump(Bytecode::BasicBlock& target)
    {
        // jmp target (RIP-relative 32-bit offset)
        emit8(0xe9);
        target.jumps_to_here.append(m_output.size());
        emit32(0xdeadbeef);
    }

    void jump_conditional(Reg reg, Bytecode::BasicBlock& true_target, Bytecode::BasicBlock& false_target)
    {
        // if (reg & 1) is 0, jump to false_target, else jump to true_target
        // test reg, 1
        emit8(0x48 | ((to_underlying(reg) >= 8) ? 1 << 2 : 0));
        emit8(0xf7);
        emit8(0xc0 | encode_reg(reg));
        emit32(0x01);

        // jz false_target (RIP-relative 32-bit offset)
        emit8(0x0f);
        emit8(0x84);
        false_target.jumps_to_here.append(m_output.size());
        emit32(0xdeadbeef);

        // jmp true_target (RIP-relative 32-bit offset)
        jump(true_target);
    }

    void jump_if_not_equal(Operand lhs, Operand rhs, Label& label)
    {
        // cmp lhs, rhs
        if (lhs.type == Operand::Type::Reg && rhs.type == Operand::Type::Reg) {
            emit8(0x48
                | ((to_underlying(lhs.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(rhs.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x39);
            emit8(0xc0 | (encode_reg(lhs.reg) << 3) | encode_reg(rhs.reg));
        } else if (lhs.type == Operand::Type::Reg && rhs.type == Operand::Type::Imm32) {
            emit8(0x48 | ((to_underlying(lhs.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x81);
            emit8(0xf8 | encode_reg(lhs.reg));
            emit32(rhs.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }

        // jne label (RIP-relative 32-bit offset)
        emit8(0x0f);
        emit8(0x85);
        emit32(0xdeadbeef);
        label.offset_in_instruction_stream = m_output.size();
    }

    void bitwise_and(Operand dst, Operand src)
    {
        // and dst,src
        if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Reg) {
            emit8(0x48
                | ((to_underlying(dst.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(src.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x21);
            emit8(0xc0 | (encode_reg(dst.reg) << 3) | encode_reg(src.reg));
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm32) {
            emit8(0x48 | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x81);
            emit8(0xe0 | encode_reg(dst.reg));
            emit32(src.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void exit()
    {
        // ret
        emit8(0xc3);
    }

    void native_call(void* callee)
    {
        // push caller-saved registers on the stack
        // (callee-saved registers: RBX, RSP, RBP, and R12–R15)

        // push RCX, RDX, RSI, RDI, R8, R9, R10, R11
        emit8(0x51);
        emit8(0x52);
        emit8(0x56);
        emit8(0x57);
        emit8(0x41);
        emit8(0x50);
        emit8(0x41);
        emit8(0x51);
        emit8(0x41);
        emit8(0x52);
        emit8(0x41);
        emit8(0x53);

        // align the stack to 16-byte boundary
        emit8(0x48);
        emit8(0x83);
        emit8(0xec);
        emit8(0x08);

        // load callee into RAX and make indirect call
        emit8(0x48);
        emit8(0xb8);
        emit64((u64)callee);
        emit8(0xff);
        emit8(0xd0);

        // adjust stack pointer
        emit8(0x48);
        emit8(0x83);
        emit8(0xc4);
        emit8(0x08);

        // restore caller-saved registers from the stack
        // pop R11, R10, R9, R8, RDI, RSI, RDX, RCX
        emit8(0x41);
        emit8(0x5b);
        emit8(0x41);
        emit8(0x5a);
        emit8(0x41);
        emit8(0x59);
        emit8(0x41);
        emit8(0x58);
        emit8(0x5f);
        emit8(0x5e);
        emit8(0x5a);
        emit8(0x59);
    }
};

}
