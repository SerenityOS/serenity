/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <AK/Vector.h>

#if ARCH(X86_64)

namespace JIT {

struct Assembler {
    Assembler(Vector<u8>& output)
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

    enum class Patchable {
        Yes,
        No,
    };

    void mov(Operand dst, Operand src, Patchable patchable = Patchable::No)
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
            if (patchable == Patchable::No && src.offset_or_immediate == 0) {
                // xor dst, dst
                emit8(0x48 | ((to_underlying(dst.reg) >= 8) ? (1 << 0 | 1 << 2) : 0));
                emit8(0x31);
                emit8(0xc0 | (encode_reg(dst.reg) << 3) | encode_reg(dst.reg));
                return;
            }
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
            if (dst.offset_or_immediate <= 127) {
                emit8(0x40 | (encode_reg(src.reg) << 3) | encode_reg(dst.reg));
                emit8(dst.offset_or_immediate);
            } else {
                emit8(0x80 | (encode_reg(src.reg) << 3) | encode_reg(dst.reg));
                emit32(dst.offset_or_immediate);
            }
            return;
        }

        if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Mem64BaseAndOffset) {
            emit8(0x48
                | ((to_underlying(dst.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(src.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x8b);
            if (src.offset_or_immediate <= 127) {
                emit8(0x40 | (encode_reg(dst.reg) << 3) | encode_reg(src.reg));
                emit8(src.offset_or_immediate);
            } else {
                emit8(0x80 | (encode_reg(dst.reg) << 3) | encode_reg(src.reg));
                emit32(src.offset_or_immediate);
            }
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

    struct Label {
        size_t offset_of_label_in_instruction_stream { 0 };
        Vector<size_t> jump_slot_offsets_in_instruction_stream;

        void add_jump(size_t offset)
        {
            jump_slot_offsets_in_instruction_stream.append(offset);
        }

        void link(Assembler& assembler)
        {
            link_to(assembler, assembler.m_output.size());
        }

        void link_to(Assembler& assembler, size_t link_offset)
        {
            offset_of_label_in_instruction_stream = link_offset;
            for (auto offset_in_instruction_stream : jump_slot_offsets_in_instruction_stream) {
                auto offset = offset_of_label_in_instruction_stream - offset_in_instruction_stream;
                auto jump_slot = offset_in_instruction_stream - 4;
                assembler.m_output[jump_slot + 0] = (offset >> 0) & 0xff;
                assembler.m_output[jump_slot + 1] = (offset >> 8) & 0xff;
                assembler.m_output[jump_slot + 2] = (offset >> 16) & 0xff;
                assembler.m_output[jump_slot + 3] = (offset >> 24) & 0xff;
            }
        }
    };

    [[nodiscard]] Label make_label()
    {
        return Label {
            .offset_of_label_in_instruction_stream = m_output.size(),
            .jump_slot_offsets_in_instruction_stream = {},
        };
    }

    [[nodiscard]] Label jump()
    {
        // jmp target (RIP-relative 32-bit offset)
        emit8(0xe9);
        emit32(0xdeadbeef);
        auto label = make_label();
        label.add_jump(m_output.size());
        return label;
    }

    void jump(Label& label)
    {
        // jmp target (RIP-relative 32-bit offset)
        emit8(0xe9);
        emit32(0xdeadbeef);
        label.add_jump(m_output.size());
    }

    void jump(Operand op)
    {
        if (op.type == Operand::Type::Reg) {
            if (to_underlying(op.reg) >= 8)
                emit8(0x41);
            emit8(0xff);
            emit8(0xe0 | encode_reg(op.reg));
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void verify_not_reached()
    {
        // ud2
        emit8(0x0f);
        emit8(0x0b);
    }

    void cmp(Operand lhs, Operand rhs)
    {
        if (lhs.type == Operand::Type::Reg && rhs.type == Operand::Type::Reg) {
            emit8(0x48
                | ((to_underlying(rhs.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(lhs.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x39);
            emit8(0xc0 | (encode_reg(rhs.reg) << 3) | encode_reg(lhs.reg));
        } else if (lhs.type == Operand::Type::Reg && rhs.type == Operand::Type::Imm32) {
            emit8(0x48 | ((to_underlying(lhs.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x81);
            emit8(0xf8 | encode_reg(lhs.reg));
            emit32(rhs.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void test(Operand lhs, Operand rhs)
    {
        if (lhs.type == Operand::Type::Reg && rhs.type == Operand::Type::Reg) {
            emit8(0x48
                | ((to_underlying(rhs.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(lhs.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x85);
            emit8(0xc0 | (encode_reg(rhs.reg) << 3) | encode_reg(lhs.reg));
        } else if (lhs.type == Operand::Type::Reg && rhs.type == Operand::Type::Imm32) {
            emit8(0x48 | ((to_underlying(lhs.reg) >= 8) ? 1 << 0 : 0));
            emit8(0xf7);
            emit8(0xc0 | encode_reg(lhs.reg));
            emit32(rhs.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void jump_if_zero(Operand reg, Label& label)
    {
        test(reg, reg);

        // jz label (RIP-relative 32-bit offset)
        emit8(0x0f);
        emit8(0x84);
        emit32(0xdeadbeef);
        label.add_jump(m_output.size());
    }

    void jump_if_not_zero(Operand reg, Label& label)
    {
        test(reg, reg);

        // jnz label (RIP-relative 32-bit offset)
        emit8(0x0f);
        emit8(0x85);
        emit32(0xdeadbeef);
        label.add_jump(m_output.size());
    }

    void jump_if_equal(Operand lhs, Operand rhs, Label& label)
    {
        if (rhs.type == Operand::Type::Imm32 && rhs.offset_or_immediate == 0) {
            jump_if_zero(lhs, label);
            return;
        }

        cmp(lhs, rhs);

        // je label (RIP-relative 32-bit offset)
        emit8(0x0f);
        emit8(0x84);
        emit32(0xdeadbeef);
        label.add_jump(m_output.size());
    }

    void jump_if_not_equal(Operand lhs, Operand rhs, Label& label)
    {
        if (rhs.type == Operand::Type::Imm32 && rhs.offset_or_immediate == 0) {
            jump_if_not_zero(lhs, label);
            return;
        }

        cmp(lhs, rhs);

        // jne label (RIP-relative 32-bit offset)
        emit8(0x0f);
        emit8(0x85);
        emit32(0xdeadbeef);
        label.add_jump(m_output.size());
    }

    void jump_if_less_than(Operand lhs, Operand rhs, Label& label)
    {
        cmp(lhs, rhs);

        // jl label (RIP-relative 32-bit offset)
        emit8(0x0f);
        emit8(0x8c);
        emit32(0xdeadbeef);
        label.add_jump(m_output.size());
    }

    void sign_extend_32_to_64_bits(Reg reg)
    {
        // movsxd (reg as 64-bit), (reg as 32-bit)
        emit8(0x48 | ((to_underlying(reg) >= 8) ? 1 << 0 : 0));
        emit8(0x63);
        emit8(0xc0 | (encode_reg(reg) << 3) | encode_reg(reg));
    }

    void bitwise_and(Operand dst, Operand src)
    {
        // and dst,src
        if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Reg) {
            emit8(0x48
                | ((to_underlying(src.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x21);
            emit8(0xc0 | (encode_reg(src.reg) << 3) | encode_reg(dst.reg));
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm32) {
            emit8(0x48 | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x81);
            emit8(0xe0 | encode_reg(dst.reg));
            emit32(src.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void bitwise_or(Operand dst, Operand src)
    {
        // or dst,src
        if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Reg) {
            emit8(0x48
                | ((to_underlying(src.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x09);
            emit8(0xc0 | (encode_reg(src.reg) << 3) | encode_reg(dst.reg));
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm32) {
            emit8(0x48 | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x81);
            emit8(0xc8 | encode_reg(dst.reg));
            emit32(src.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void enter()
    {
        push_callee_saved_registers();

        push(Operand::Register(Reg::RBP));
        mov(Operand::Register(Reg::RBP), Operand::Register(Reg::RSP));
        sub(Operand::Register(Reg::RSP), Operand::Imm8(8));
    }

    void exit()
    {
        // leave
        emit8(0xc9);

        pop_callee_saved_registers();

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
            if (to_underlying(op.reg) >= 8)
                emit8(0x49);
            emit8(0x50 | encode_reg(op.reg));
        } else if (op.type == Operand::Type::Imm32) {
            emit8(0x68);
            emit32(op.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void pop(Operand op)
    {
        if (op.type == Operand::Type::Reg) {
            if (to_underlying(op.reg) >= 8)
                emit8(0x49);
            emit8(0x58 | encode_reg(op.reg));
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void add(Operand dst, Operand src)
    {
        if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Reg) {
            emit8(0x48
                | ((to_underlying(dst.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(src.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x01);
            emit8(0xc0 | (encode_reg(dst.reg) << 3) | encode_reg(src.reg));
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm32) {
            emit8(0x48 | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x81);
            emit8(0xc0 | encode_reg(dst.reg));
            emit32(src.offset_or_immediate);
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm8) {
            emit8(0x48 | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x83);
            emit8(0xc0 | encode_reg(dst.reg));
            emit8(src.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void sub(Operand dst, Operand src)
    {
        if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Reg) {
            emit8(0x48
                | ((to_underlying(dst.reg) >= 8) ? 1 << 2 : 0)
                | ((to_underlying(src.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x29);
            emit8(0xc0 | (encode_reg(dst.reg) << 3) | encode_reg(src.reg));
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm32) {
            emit8(0x48 | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x81);
            emit8(0xe8 | encode_reg(dst.reg));
            emit32(src.offset_or_immediate);
        } else if (dst.type == Operand::Type::Reg && src.type == Operand::Type::Imm8) {
            emit8(0x48 | ((to_underlying(dst.reg) >= 8) ? 1 << 0 : 0));
            emit8(0x83);
            emit8(0xe8 | encode_reg(dst.reg));
            emit8(src.offset_or_immediate);
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    void native_call(void* callee)
    {
        // push caller-saved registers on the stack
        // (callee-saved registers: RBX, RSP, RBP, and R12–R15)
        push(Operand::Register(Reg::RCX));
        push(Operand::Register(Reg::RDX));
        push(Operand::Register(Reg::RSI));
        push(Operand::Register(Reg::RDI));
        push(Operand::Register(Reg::R8));
        push(Operand::Register(Reg::R9));
        push(Operand::Register(Reg::R10));
        push(Operand::Register(Reg::R11));

        // align the stack to 16-byte boundary
        sub(Operand::Register(Reg::RSP), Operand::Imm8(8));

        // load callee into RAX
        mov(Operand::Register(Reg::RAX), Operand::Imm64(bit_cast<u64>(callee)));

        // call RAX
        emit8(0xff);
        emit8(0xd0);

        // adjust stack pointer
        add(Operand::Register(Reg::RSP), Operand::Imm8(8));

        // restore caller-saved registers from the stack
        pop(Operand::Register(Reg::R11));
        pop(Operand::Register(Reg::R10));
        pop(Operand::Register(Reg::R9));
        pop(Operand::Register(Reg::R8));
        pop(Operand::Register(Reg::RDI));
        pop(Operand::Register(Reg::RSI));
        pop(Operand::Register(Reg::RDX));
        pop(Operand::Register(Reg::RCX));
    }

    void trap()
    {
        // int3
        emit8(0xcc);
    }
};

}

#endif
