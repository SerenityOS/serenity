/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Nikodem Rabuliński <1337-serenity@nrab.lol>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <AK/Vector.h>

namespace JIT {

struct Aarch64Assembler {
    Aarch64Assembler(Vector<u8>& output)
        : m_output(output)
    {
    }

    enum class Reg {
        X0,
        X1,
        X2,
        X3,
        X4,
        X5,
        X6,
        X7,
        X8,
        X9,
        X10,
        X11,
        X12,
        X13,
        X14,
        X15,
        X16,
        X17,
        X18,
        X19,
        X20,
        X21,
        X22,
        X23,
        X24,
        X25,
        X26,
        X27,
        X28,
        X29,
        X30,
        // NOTE: Register 31 is also XZR (the zero-register).
        //       All of the public API treats it as SP and will never use it as XZR,
        //       but the internals may use it to refer to XZR.
        SP,
        Q0 = 0,
        Q1,
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
        EqualTo = 0x0,
        NotEqualTo = 0x1,
        UnsignedGreaterThanOrEqualTo = 0x2, // Carry set
        UnsignedLessThan = 0x3,             // Carry clear
        Overflow = 0x6,
        UnsignedGreaterThan = 0x8,
        UnsignedLessThanOrEqualTo = 0x9,
        SignedGreaterThanOrEqualTo = 0xA,
        SignedLessThan = 0xB,
        SignedGreaterThan = 0xC,
        SignedLessThanOrEqualTo = 0xD,

        Unordered = Overflow,
        NotUnordered = 0x7, // No overflow

        Below = UnsignedLessThan,
        BelowOrEqual = UnsignedLessThanOrEqualTo,
        Above = UnsignedGreaterThan,
        AboveOrEqual = UnsignedGreaterThanOrEqualTo,
    };

    enum class Patchable {
        Yes,
        No,
    };

    void shift_right([[maybe_unused]] Operand dst, [[maybe_unused]] Operand count)
    {
        TODO_AARCH64();
    }

    void mov(Operand dst, Operand src)
    {
        if (dst.type == Operand::Type::Reg)
            switch (src.type) {
            case Operand::Type::Reg:
                if (src.reg == Reg::SP || dst.reg == Reg::SP)
                    add(dst.reg, src.reg, 0);
                else
                    orr(dst.reg, /* XZR */ Reg::SP, src.reg);
                break;
            case Operand::Type::Imm:
                if (dst.reg == Reg::SP)
                    TODO_AARCH64();
                else {
                    auto imm = src.offset_or_immediate;
                    mov_imm(dst.reg, imm & 0xffff, 0, MovType::Zero);
                    imm >>= 16;
                    for (int shift = 16; imm != 0; shift += 16, imm >>= 16)
                        mov_imm(dst.reg, imm & 0xffff, shift, MovType::Keep);
                }
                break;
            case Operand::Type::Mem64BaseAndOffset:
                ldr(dst.reg, src.reg, static_cast<i16>(src.offset_or_immediate), AddressingMode::Offset);
                break;
            case Operand::Type::FReg:
                TODO_AARCH64();
                break;
            }
        else if (dst.type == Operand::Type::Mem64BaseAndOffset && src.type == Operand::Type::Reg)
            str(src.reg, dst.reg, static_cast<i16>(dst.offset_or_immediate), AddressingMode::Offset);
        else
            VERIFY_NOT_REACHED();
    }

    enum Extension {
        ZeroExtend,
        SignExtend,
    };

    void mov8([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src, [[maybe_unused]] Extension extension = Extension::ZeroExtend)
    {
        TODO_AARCH64();
    }

    void mov16([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src, [[maybe_unused]] Extension extension = Extension::ZeroExtend)
    {
        TODO_AARCH64();
    }

    void mov32([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src, [[maybe_unused]] Extension extension = Extension::ZeroExtend)
    {
        TODO_AARCH64();
    }

    void emit32(u32 value)
    {
        m_output.append((value >> 0) & 0xff);
        m_output.append((value >> 8) & 0xff);
        m_output.append((value >> 16) & 0xff);
        m_output.append((value >> 24) & 0xff);
    }

    struct Label {
        friend struct Aarch64Assembler;

        void link(Aarch64Assembler& assembler)
        {
            link_to(assembler, assembler.m_output.size());
        }

        void link_to(Aarch64Assembler& assembler, size_t link_offset)
        {
            VERIFY(!offset_of_label_in_instruction_stream.has_value());
            offset_of_label_in_instruction_stream = link_offset;
            for (auto offset_in_instruction_stream : jump_slot_offsets_in_instruction_stream)
                link_jump(assembler, offset_in_instruction_stream);
        }

    private:
        struct LabelOffset {
            size_t offset;
            u32 offset_in_instruction;
            u8 max_bits;
        };

        Optional<size_t> offset_of_label_in_instruction_stream;
        Vector<LabelOffset> jump_slot_offsets_in_instruction_stream;

        void add_jump(Aarch64Assembler& assembler, LabelOffset offset)
        {
            jump_slot_offsets_in_instruction_stream.append(offset);
            if (offset_of_label_in_instruction_stream.has_value())
                link_jump(assembler, offset);
        }

        void link_jump(Aarch64Assembler& assembler, LabelOffset offset_in_instruction_stream)
        {
            auto offset = bit_cast<ssize_t>(offset_of_label_in_instruction_stream.value() + 4 - offset_in_instruction_stream.offset);
            ssize_t const max = 1 << (offset_in_instruction_stream.max_bits - 1);
            VERIFY(offset < max && offset >= -max);
            // Sanity check that we don't want to try jumping to an unaligned offset
            VERIFY((offset & 3) == 0);

            // Since all ARM instructions are 32-bit wide, the jump offset is encoded as target / 4
            offset >>= 2;
            offset &= (1 << offset_in_instruction_stream.max_bits) - 1;
            offset <<= offset_in_instruction_stream.offset_in_instruction;

            auto jump_slot = offset_in_instruction_stream.offset - 4;
            assembler.m_output[jump_slot + 0] |= (offset >> 0) & 0xff;
            assembler.m_output[jump_slot + 1] |= (offset >> 8) & 0xff;
            assembler.m_output[jump_slot + 2] |= (offset >> 16) & 0xff;
            assembler.m_output[jump_slot + 3] |= (offset >> 24) & 0xff;
        }
    };

    [[nodiscard]] Label jump()
    {
        b(0);
        Aarch64Assembler::Label label {};
        label.add_jump(*this, { m_output.size(), 0, 26 });
        return label;
    }

    void jump(Label& label)
    {
        b(0);
        label.add_jump(*this, { m_output.size(), 0, 26 });
    }

    void jump(Operand op)
    {
        if (op.type == Operand::Type::Reg)
            br(op.reg);
        else
            TODO_AARCH64();
    }

    void verify_not_reached()
    {
        TODO_AARCH64();
    }

    void cmp(Operand lhs, Operand rhs)
    {
        if (lhs.type != Operand::Type::Reg || rhs.type != Operand::Type::Imm)
            TODO_AARCH64();

        subs(/* XZR */ Reg::SP, lhs.reg, static_cast<u16>(rhs.offset_or_immediate));
    }

    void jump_if(Condition condition, Label& label)
    {
        b_cond(0, condition);
        label.add_jump(*this, { m_output.size(), 5, 19 });
    }

    void jump_if(Operand lhs, Condition condition, Operand rhs, Label& label)
    {
        // TODO: Use CBZ and CBNZ for cases where reg is compared with an immediate 0
        cmp(lhs, rhs);
        jump_if(condition, label);
    }

    void set_if([[maybe_unused]] Condition condition, [[maybe_unused]] Operand dst)
    {
        TODO_AARCH64();
    }

    void mov_if([[maybe_unused]] Condition condition, [[maybe_unused]] Operand dst, [[maybe_unused]] Operand src)
    {
        TODO_AARCH64();
    }

    void sign_extend_32_to_64_bits([[maybe_unused]] Reg reg)
    {
        TODO_AARCH64();
    }

    void bitwise_and([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src)
    {
        TODO_AARCH64();
    }

    void bitwise_or([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src)
    {
        TODO_AARCH64();
    }

    void bitwise_xor32([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src)
    {
        TODO_AARCH64();
    }

    void mul([[maybe_unused]] Operand dest, [[maybe_unused]] Operand src)
    {
        TODO_AARCH64();
    }

    void mul32([[maybe_unused]] Operand dest, [[maybe_unused]] Operand src, [[maybe_unused]] Optional<Label&> overflow_label)
    {
        TODO_AARCH64();
    }

    void shift_left([[maybe_unused]] Operand dest, [[maybe_unused]] Operand count)
    {
        TODO_AARCH64();
    }

    void shift_left32([[maybe_unused]] Operand dest, [[maybe_unused]] Operand count)
    {
        TODO_AARCH64();
    }

    void shift_right32([[maybe_unused]] Operand dest, [[maybe_unused]] Operand count)
    {
        TODO_AARCH64();
    }

    void arithmetic_right_shift([[maybe_unused]] Operand dest, [[maybe_unused]] Operand count)
    {
        TODO_AARCH64();
    }

    void arithmetic_right_shift32([[maybe_unused]] Operand dest, [[maybe_unused]] Operand count)
    {
        TODO_AARCH64();
    }

    void enter()
    {
        push({ Operand::Register(Reg::X29), Operand::Register(Reg::X30) });
        mov(Operand::Register(Reg::X29), Operand::Register(Reg::SP));

        push_callee_saved_registers();
    }

    void exit()
    {
        pop_callee_saved_registers();

        pop({ Operand::Register(Reg::X30), Operand::Register(Reg::X29) });

        ret(Reg::X30);
    }

    void push_callee_saved_registers()
    {
        push({
            Operand::Register(Reg::X19),
            Operand::Register(Reg::X20),
            Operand::Register(Reg::X21),
            Operand::Register(Reg::X22),
            Operand::Register(Reg::X23),
            Operand::Register(Reg::X24),
            Operand::Register(Reg::X25),
            Operand::Register(Reg::X26),
            Operand::Register(Reg::X27),
            Operand::Register(Reg::X28),
        });
    }

    void pop_callee_saved_registers()
    {
        pop({
            Operand::Register(Reg::X28),
            Operand::Register(Reg::X27),
            Operand::Register(Reg::X26),
            Operand::Register(Reg::X25),
            Operand::Register(Reg::X24),
            Operand::Register(Reg::X23),
            Operand::Register(Reg::X22),
            Operand::Register(Reg::X21),
            Operand::Register(Reg::X20),
            Operand::Register(Reg::X19),
        });
    }

    // Storing a single register is wasteful as ARMv8 requires SP to be 16bit aligned.
    // It is possible to use another register as a stack pointer,
    // but for now it's easier to just keep the stack aligned at all times,
    // so please use push(Vector<Operand>) whenever possible.
    void push(Operand op)
    {
        if (op.type != Operand::Type::Reg)
            TODO_AARCH64();

        // str REG, [sp, #-16]!
        str(op.reg, Reg::SP, -16, AddressingMode::PreIndexed);
    }

    // NOTE: When pushing multiple operands, make sure to also pop them together!
    //       As SP needs to be 16-bit aligned, trying to pop different amount of registers
    //       than you pushed will result in potentially corrupted data.
    void push(Vector<Operand> const& ops)
    {
        auto i = 0;

        while (ops.size() - i >= 2) {
            auto src1 = ops[i];
            auto src2 = ops[i + 1];
            if (src1.type != Operand::Type::Reg || src2.type != Operand::Type::Reg)
                TODO_AARCH64();

            // stp ops_i, ops_i+1, [sp, #-16]!
            stp(src1.reg, src2.reg, Reg::SP, -16, AddressingMode::PreIndexed);

            i += 2;
        }

        if (ops.size() - i == 1)
            push(ops[i]);
    }

    void pop([[maybe_unused]] Operand op)
    {
        if (op.type != Operand::Type::Reg)
            TODO_AARCH64();

        // ldr REG, [sp], #16
        ldr(op.reg, Reg::SP, 16, AddressingMode::PostIndexed);
    }

    // See: NOTE about push
    void pop(Vector<Operand> const& ops)
    {
        auto i = 0;

        if ((ops.size() & 1) == 1)
            pop(ops[i++]);

        while (ops.size() - i >= 2) {
            auto src1 = ops[i + 1];
            auto src2 = ops[i];
            if (src1.type != Operand::Type::Reg || src2.type != Operand::Type::Reg)
                TODO_AARCH64();

            // ldp ops_i+1, ops_i, [sp], #16
            ldp(src1.reg, src2.reg, Reg::SP, 16, AddressingMode::PostIndexed);

            i += 2;
        }
    }

    void inc32([[maybe_unused]] Operand op, [[maybe_unused]] Optional<Label&> overflow_label)
    {
        TODO_AARCH64();
    }

    void dec32([[maybe_unused]] Operand op, [[maybe_unused]] Optional<Label&> overflow_label)
    {
        TODO_AARCH64();
    }

    void add([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src)
    {
        TODO_AARCH64();
    }

    void add32([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src, [[maybe_unused]] Optional<Label&> overflow_label)
    {
        TODO_AARCH64();
    }

    void sub([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src)
    {
        TODO_AARCH64();
    }

    void sub32([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src, [[maybe_unused]] Optional<Label&> overflow_label)
    {
        TODO_AARCH64();
    }

    void neg32([[maybe_unused]] Operand reg)
    {
        TODO_AARCH64();
    }

    void convert_i32_to_double([[maybe_unused]] Operand dst, [[maybe_unused]] Operand src)
    {
        TODO_AARCH64();
    }

    void native_call(
        [[maybe_unused]] u64 callee,
        [[maybe_unused]] Vector<Operand> const& arguments,
        [[maybe_unused]] Vector<Operand> const& preserved_registers = {})
    {
        TODO_AARCH64();
    }

    void trap()
    {
        TODO_AARCH64();
    }

private:
    Vector<u8>& m_output;

    enum class AddressingMode {
        PostIndexed = 1,
        Offset,
        PreIndexed,
    };

    void str(Reg src, Reg base, i16 offset, AddressingMode mode)
    {
        ldr_or_str(src, base, offset, mode, false);
    }

    void ldr(Reg dst, Reg base, i16 offset, AddressingMode mode)
    {
        ldr_or_str(dst, base, offset, mode, true);
    }

    void ldr_or_str(Reg reg, Reg base, i16 offset, AddressingMode mode, bool is_load)
    {
        auto prefix = 0b11'111 << 27;
        auto l_bit = is_load ? 1 << 22 : 0;
        u32 mode_bits;
        u32 offset_bits;
        if (mode == AddressingMode::Offset) {
            VERIFY((offset & ~0x7ff8) == 0);
            offset_bits = static_cast<u32>(bit_cast<u16>(offset)) << 7;
            mode_bits = 1 << 24;
        } else {
            VERIFY(offset < 256 && offset >= -256);
            offset_bits = static_cast<u32>(bit_cast<u16>(offset) & 0x1ff) << 12;
            mode_bits = to_underlying(mode) << 10;
        }
        auto base_bits = to_underlying(base) << 5;
        auto src_bits = to_underlying(reg);

        emit32(prefix | l_bit | offset_bits | mode_bits | base_bits | src_bits);
    }

    void stp(Reg src1, Reg src2, Reg base, i16 offset, AddressingMode mode)
    {
        ldp_or_stp(src1, src2, base, offset, mode, false);
    }

    void ldp(Reg src1, Reg src2, Reg base, i16 offset, AddressingMode mode)
    {
        ldp_or_stp(src1, src2, base, offset, mode, true);
    }

    void ldp_or_stp(Reg src1, Reg src2, Reg base, i16 offset, AddressingMode mode, bool is_load)
    {
        if (mode == AddressingMode::Offset)
            TODO_AARCH64();

        VERIFY((offset & 7) == 0);

        auto prefix = 0b10'101 << 27;
        auto mode_bits = to_underlying(mode) << 23;
        auto l_bit = is_load ? (1 << 22) : 0;
        auto offset_bits = (static_cast<u32>(bit_cast<u16>(offset)) & 0x3f8) << 12;
        auto src2_bits = to_underlying(src2) << 10;
        auto base_bits = to_underlying(base) << 5;
        auto src1_bits = to_underlying(src1);

        emit32(prefix | l_bit | mode_bits | offset_bits | src2_bits | base_bits | src1_bits);
    }

    void add(Reg dst, Reg src, u16 imm)
    {
        if (imm > 4095)
            VERIFY_NOT_REACHED();

        auto prefix = 0b1'0'0'10001 << 24;
        auto imm_bits = static_cast<u32>(imm) << 10;
        auto src_bits = to_underlying(src) << 5;
        auto dst_bits = to_underlying(dst);

        emit32(prefix | imm_bits | src_bits | dst_bits);
    }

    // FIXME: ORR (shifted register) also allows shifting the second source register.
    //        This is not implemented here (yet).
    void orr(Reg dst, Reg src1, Reg src2)
    {
        auto prefix = 0b1'01'0101 << 25;
        auto src2_bits = to_underlying(src2) << 16;
        auto src1_bits = to_underlying(src1) << 5;
        auto dst_bits = to_underlying(dst);

        emit32(prefix | src2_bits | src1_bits | dst_bits);
    }

    enum class MovType {
        Not = 0,
        Zero = 2,
        Keep = 3,
    };

    // NOTE: Shift has to be 0, 16, 32, or 48
    void mov_imm(Reg dst, u16 imm, u8 shift, MovType type)
    {
        VERIFY((shift & ~0x30) == 0);

        auto prefix = 0b1'00'100101 << 23;
        auto opc = to_underlying(type) << 29;
        auto shift_bits = static_cast<u32>(shift) << 17;
        auto imm_bits = static_cast<u32>(imm) << 5;
        auto dst_bits = to_underlying(dst);

        emit32(prefix | opc | shift_bits | imm_bits | dst_bits);
    }

    void br(Reg dst, bool is_ret = false)
    {
        auto prefix = 0b1101011'0'0'00'11111 << 16;
        auto ret_bit = is_ret ? 1 << 22 : 0;
        auto dst_bits = to_underlying(dst) << 5;

        emit32(prefix | ret_bit | dst_bits);
    }

    void b(i32 dst)
    {
        i32 max = 128 * MiB;
        VERIFY(dst <= max && dst >= -max);
        VERIFY((dst & 3) == 0);

        auto prefix = 0b0'00101 << 26;
        auto dst_bits = bit_cast<u32>(dst >> 2) & 0x3ffffff;

        emit32(prefix | dst_bits);
    }

    void ret(Reg dst)
    {
        br(dst, true);
    }

    void ret()
    {
        ret(Reg::X30);
    }

    void b_cond(i32 dst, Condition cond)
    {
        i32 max = MiB;
        VERIFY(dst <= max && dst >= -max);
        VERIFY((dst & 3) == 0);

        auto prefix = 0b0101010 << 25;
        auto dst_bits = (bit_cast<u32>(dst >> 2) & 0x7ffff) << 5;
        auto cond_bits = to_underlying(cond);

        emit32(prefix | dst_bits | cond_bits);
    }

    // FIXME: Shift?
    void subs(Reg dst, Reg src, u16 imm)
    {
        auto prefix = 0b111'100010 << 23;
        auto imm_bits = static_cast<u32>(imm) << 10;
        auto src_bits = to_underlying(src) << 5;
        auto dst_bits = to_underlying(dst);

        emit32(prefix | imm_bits | src_bits | dst_bits);
    }
};

}
