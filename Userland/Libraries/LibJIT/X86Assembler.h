/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <LibJIT/InstructionBuffer.h>
#include <LibX86/Instruction.h>

namespace JIT {

TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, false, true, false, false, false, true, RegisterIndex);
TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, false, true, false, false, false, true, Immediate);
TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, false, true, false, false, false, true, DereferencedRegisterIndex);

using InstructionArgument = Variant<RegisterIndex, Immediate, DereferencedRegisterIndex>;
using InstructionDestination = Variant<RegisterIndex, DereferencedRegisterIndex>;

enum class EqualityCondition {
    Equal,
    GreaterThan,
    GreaterThanOrEqual,
    LessThan,
    LessThanOrEqual,
    NotEqual,
};

constexpr bool is_valid_register_size(size_t size)
{
    return size == 8 || size == 16 || size == 32;
}

class X86Assembler {
public:
    X86Assembler(InstructionBuffer& buffer)
        : m_instruction_buffer(buffer)
    {
    }

    size_t num_registers() const { return 8; }

    void inc_register8(X86::RegisterIndex8 reg);
    void dec_register8(X86::RegisterIndex8 reg);

    void inc_register32(X86::RegisterIndex32 reg);
    void dec_register32(X86::RegisterIndex32 reg);

    void add_register32_imm32(X86::RegisterIndex32 reg, u32 value);

    void push_register32(X86::RegisterIndex32 reg);
    void pop_register32(X86::RegisterIndex32 reg);

    void call(X86::RegisterIndex32 reg);

    void jump_relative(u32 relative_offset);

    template<size_t OPERAND_SIZE>
    requires(is_valid_register_size(OPERAND_SIZE)) void move(InstructionDestination dst, InstructionArgument src);

    template<size_t OPERAND_SIZE>
    requires(is_valid_register_size(OPERAND_SIZE)) void test(InstructionDestination dst, InstructionArgument src);

    [[nodiscard]] JITPatchLocation jump_relative_on_condition(EqualityCondition condition, u32 relative_offset);

    void prelude();
    void epilogue();
    void ret();

private:
    InstructionBuffer& m_instruction_buffer;
};

}
