/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJIT/X86Assembler.h>

namespace JIT {

void X86Assembler::inc_register8(X86::RegisterIndex8 reg)
{
    u8 const modrm = 0xc0 + reg;
    m_instruction_buffer.append_bytes({ 0xfe, modrm });
}

void X86Assembler::dec_register8(X86::RegisterIndex8 reg)
{
    u8 const modrm = 0xc8 + reg;
    m_instruction_buffer.append_bytes({ 0xfe, modrm });
}

void X86Assembler::inc_register32(X86::RegisterIndex32 reg)
{
    u8 const op = 0x40 + reg;
    m_instruction_buffer.append_bytes({ op });
}

void X86Assembler::dec_register32(X86::RegisterIndex32 reg)
{
    u8 const op = 0x48 + reg;
    m_instruction_buffer.append_bytes({ op });
}

void X86Assembler::add_register32_imm32(X86::RegisterIndex32 reg, u32 value)
{
    u8 const sub_op = 0xc0 + reg;
    m_instruction_buffer.append_bytes({
        0x81,
        sub_op,
    });
    m_instruction_buffer.append_le(value);
}

void X86Assembler::push_register32(X86::RegisterIndex32 reg)
{
    VERIFY(reg < num_registers());
    u8 const op = 0x50u + reg;
    m_instruction_buffer.append_bytes({ op });
}

void X86Assembler::pop_register32(X86::RegisterIndex32 reg)
{
    VERIFY(reg < num_registers());
    u8 op = 0x58u + reg;
    m_instruction_buffer.append_bytes({ op });
}

void X86Assembler::call(X86::RegisterIndex32 reg)
{
    VERIFY(reg < num_registers());
    u8 sub_op = 0xd0u + reg;
    m_instruction_buffer.append_bytes({ 0xff, sub_op });
}

void X86Assembler::jump_relative(u32 relative_offset)
{
    m_instruction_buffer.append_bytes({ 0xe9 });
    m_instruction_buffer.append_le(relative_offset - 5);
}

template<>
void X86Assembler::move<8>(InstructionDestination dst, InstructionArgument src)
{
    if (dst.has<RegisterIndex>()) {
        VERIFY(src.has<DereferencedRegisterIndex>());
        u8 dst_reg = dst.get<RegisterIndex>().value();
        VERIFY(dst_reg <= 8);
        u8 src_reg = src.get<DereferencedRegisterIndex>().value();
        VERIFY(src_reg <= 8);
        u8 modrm = (dst_reg << 3) | src_reg;
        m_instruction_buffer.append_bytes({ 0x8a, modrm });
    } else {
        VERIFY(src.has<RegisterIndex>());
        u8 dst_reg = dst.get<DereferencedRegisterIndex>().value();
        VERIFY(dst_reg <= 8);
        u8 src_reg = src.get<RegisterIndex>().value();
        VERIFY(src_reg <= 8);
        u8 modrm = (src_reg << 3) | dst_reg;
        m_instruction_buffer.append_bytes({ 0x88, modrm });
    }
}

template<>
void X86Assembler::move<32>(InstructionDestination dst, InstructionArgument src)
{
    VERIFY(dst.has<RegisterIndex>() && src.has<Immediate>());
    if (dst.has<RegisterIndex>()) {
        auto dst_reg = dst.get<RegisterIndex>().value();
        VERIFY(dst_reg <= num_registers());
        u8 op = 0xb8 + dst_reg;
        m_instruction_buffer.append_bytes({ op });
        m_instruction_buffer.append_le(src.get<Immediate>().value());
    } else {
        VERIFY_NOT_REACHED();
    }
}

template<size_t OPERAND_SIZE>
requires(is_valid_register_size(OPERAND_SIZE)) void X86Assembler::test(InstructionDestination dst, InstructionArgument src)
{
    VERIFY(!src.has<Immediate>());
    if (dst.has<RegisterIndex>()) {
        VERIFY(src.has<RegisterIndex>());
        auto dst_reg = dst.get<RegisterIndex>().value();
        auto src_reg = dst.get<RegisterIndex>().value();
        u8 modrm = 0xc0 | (src_reg << 3) | (dst_reg);
        if constexpr (OPERAND_SIZE == 8) {
            m_instruction_buffer.append_bytes({ 0x84, modrm });
        } else {
            if constexpr (OPERAND_SIZE == 16)
                m_instruction_buffer.append_bytes({ 0x66 });
            m_instruction_buffer.append_bytes({ 0x85, modrm });
        }
    } else {
        VERIFY_NOT_REACHED();
    }
}

template void X86Assembler::test<8>(InstructionDestination dst, InstructionArgument src);
template void X86Assembler::test<16>(InstructionDestination dst, InstructionArgument src);
template void X86Assembler::test<32>(InstructionDestination dst, InstructionArgument src);

JITPatchLocation X86Assembler::jump_relative_on_condition(EqualityCondition condition, u32 relative_offset)
{
    VERIFY(condition == EqualityCondition::Equal);
    m_instruction_buffer.append_bytes({
        // jz 0x00000000
        0x0f,
        0x84,
    });
    m_instruction_buffer.append_le(relative_offset);
    return m_instruction_buffer.get_relative_patch_location(-4);
}

void X86Assembler::prelude()
{
    m_instruction_buffer.append_bytes({
        // push %ebp
        0x55,
        // mov %ebp, %esp
        0x89,
        0xe5,
        // push %ebx
        0x53,
        // push %edi
        0x57,
    });
}

void X86Assembler::epilogue()
{
    m_instruction_buffer.append_bytes({
        // pop %edi
        0x5f,
        // pop %ebx
        0x5b,
        // mov %esp, %ebp
        0x89,
        0xec,
        // pop ebp
        0x5d,
    });
}

void X86Assembler::ret()
{
    m_instruction_buffer.append_bytes({ 0xc3 });
}

}
