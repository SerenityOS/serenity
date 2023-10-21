/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "IM.h"
#include <AK/NonnullOwnPtr.h>
#include <LibDisassembly/riscv64/Instruction.h>

namespace Disassembly::RISCV64 {

NonnullOwnPtr<LoadUpperImmediate> parse_lui(u32 instruction)
{
    auto raw_parts = RawUType::parse(instruction);
    return adopt_own(*new (nothrow) LoadUpperImmediate(raw_parts.imm, raw_parts.rd));
}

NonnullOwnPtr<JumpAndLink> parse_jal(u32 instruction)
{
    auto raw_parts = RawJType::parse(instruction);
    return adopt_own(*new (nothrow) JumpAndLink(raw_parts.imm, raw_parts.rd));
}

NonnullOwnPtr<JumpAndLinkRegister> parse_jalr(u32 instruction)
{
    auto raw_parts = RawIType::parse(instruction);
    return adopt_own(*new (nothrow) JumpAndLinkRegister(raw_parts.imm, raw_parts.rs1, raw_parts.rd));
}

NonnullOwnPtr<AddUpperImmediateToProgramCounter> parse_auipc(u32 instruction)
{
    auto raw_parts = RawUType::parse(instruction);
    return adopt_own(*new (nothrow) AddUpperImmediateToProgramCounter(raw_parts.imm, raw_parts.rd));
}

NonnullOwnPtr<InstructionImpl> parse_op_imm(u32 instruction)
{
    auto raw_parts = RawIType::parse(instruction);

    auto operation = ArithmeticImmediateInstruction::Operation::Add;
    switch (raw_parts.funct3) {
    case 0b000:
        operation = ArithmeticImmediateInstruction::Operation::Add;
        break;
    case 0b010:
        operation = ArithmeticImmediateInstruction::Operation::SetLessThan;
        break;
    case 0b011:
        operation = ArithmeticImmediateInstruction::Operation::SetLessThanUnsigned;
        break;
    case 0b100:
        operation = ArithmeticImmediateInstruction::Operation::Xor;
        break;
    case 0b110:
        operation = ArithmeticImmediateInstruction::Operation::Or;
        break;
    case 0b111:
        operation = ArithmeticImmediateInstruction::Operation::And;
        break;
    case 0b001:
        operation = ArithmeticImmediateInstruction::Operation::ShiftLeftLogical;
        break;
    case 0b101:
        if ((instruction & (1 << 30)) == 0)
            operation = ArithmeticImmediateInstruction::Operation::ShiftRightLogical;
        else
            operation = ArithmeticImmediateInstruction::Operation::ShiftRightArithmetic;
        // Delete the possibly set 10th bit that distinguishes SRLI and SRAI.
        raw_parts.imm &= ~(1 << 10);
        break;
    default:
        return adopt_own(*new (nothrow) UnknownInstruction);
    }
    return adopt_own(*new (nothrow) ArithmeticImmediateInstruction(operation, raw_parts.imm, raw_parts.rs1, raw_parts.rd));
}

NonnullOwnPtr<InstructionImpl> parse_op_imm_32(u32 instruction)
{
    auto raw_parts = RawIType::parse(instruction);

    auto operation = ArithmeticImmediateInstruction::Operation::Add;
    switch (raw_parts.funct3) {
    case 0b000:
        operation = ArithmeticImmediateInstruction::Operation::AddWord;
        break;
    case 0b001:
        operation = ArithmeticImmediateInstruction::Operation::ShiftLeftLogicalWord;
        break;
    case 0b101:
        if ((instruction & (1 << 30)) == 0)
            operation = ArithmeticImmediateInstruction::Operation::ShiftRightLogicalWord;
        else
            operation = ArithmeticImmediateInstruction::Operation::ShiftRightArithmeticWord;
        // Delete the possibly set 10th bit that distinguishes SRLIW and SRAIW.
        raw_parts.imm &= ~(1 << 10);
        break;
    default:
        return adopt_own(*new (nothrow) UnknownInstruction);
    }
    return adopt_own(*new (nothrow) ArithmeticImmediateInstruction(operation, raw_parts.imm, raw_parts.rs1, raw_parts.rd));
}

NonnullOwnPtr<ArithmeticInstruction> parse_op(u32 instruction)
{
    auto raw_parts = RawRType::parse(instruction);
    // Distinguishes a few closely related operations, like add/sub.
    auto mode_switch = (raw_parts.funct7 & 0b0100000) > 0;
    auto is_m_extension = (raw_parts.funct7 & 1) == 1;

    auto operation = ArithmeticInstruction::Operation::Add;
    if (!is_m_extension) {
        switch (raw_parts.funct3) {
        case 0b000:
            if (mode_switch)
                operation = ArithmeticInstruction::Operation::Subtract;
            else
                operation = ArithmeticInstruction::Operation::Add;
            break;
        case 0b001:
            operation = ArithmeticInstruction::Operation::ShiftLeftLogical;
            break;
        case 0b010:
            operation = ArithmeticInstruction::Operation::SetLessThan;
            break;
        case 0b011:
            operation = ArithmeticInstruction::Operation::SetLessThanUnsigned;
            break;
        case 0b100:
            operation = ArithmeticInstruction::Operation::Xor;
            break;
        case 0b101:
            if (mode_switch)
                operation = ArithmeticInstruction::Operation::ShiftRightArithmetic;
            else
                operation = ArithmeticInstruction::Operation::ShiftRightLogical;
            break;
        case 0b110:
            operation = ArithmeticInstruction::Operation::Or;
            break;
        case 0b111:
            operation = ArithmeticInstruction::Operation::And;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    } else {
        switch (raw_parts.funct3) {
        case 0b000:
            operation = ArithmeticInstruction::Operation::Multiply;
            break;
        case 0b001:
            operation = ArithmeticInstruction::Operation::MultiplyHigh;
            break;
        case 0b010:
            operation = ArithmeticInstruction::Operation::MultiplyHighSignedUnsigned;
            break;
        case 0b011:
            operation = ArithmeticInstruction::Operation::MultiplyHighUnsigned;
            break;
        case 0b100:
            operation = ArithmeticInstruction::Operation::Divide;
            break;
        case 0b101:
            operation = ArithmeticInstruction::Operation::DivideUnsigned;
            break;
        case 0b110:
            operation = ArithmeticInstruction::Operation::Remainder;
            break;
        case 0b111:
            operation = ArithmeticInstruction::Operation::RemainderUnsigned;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }
    return adopt_own(*new ArithmeticInstruction(operation, raw_parts.rs1, raw_parts.rs2, raw_parts.rd));
}

NonnullOwnPtr<InstructionImpl> parse_op_32(u32 instruction)
{
    auto raw_parts = RawRType::parse(instruction);
    auto is_m_extension = (raw_parts.funct7 & 1) == 1;

    auto operation = ArithmeticInstruction::Operation::Add;
    if (!is_m_extension) {
        switch (raw_parts.funct3) {
        case 0b000:
            if ((raw_parts.funct7 & 0b0100000) == 0)
                operation = ArithmeticInstruction::Operation::AddWord;
            else
                operation = ArithmeticInstruction::Operation::SubtractWord;
            break;
        case 0b001:
            operation = ArithmeticInstruction::Operation::ShiftLeftLogicalWord;
            break;
        case 0b101:
            if ((raw_parts.funct7 & 0b0100000) == 0)
                operation = ArithmeticInstruction::Operation::ShiftRightLogicalWord;
            else
                operation = ArithmeticInstruction::Operation::ShiftRightArithmeticWord;
            break;
        default:
            return adopt_own(*new (nothrow) UnknownInstruction);
        }
    } else {
        switch (raw_parts.funct3) {
        case 0b000:
            operation = ArithmeticInstruction::Operation::MultiplyWord;
            break;
        case 0b100:
            operation = ArithmeticInstruction::Operation::DivideWord;
            break;
        case 0b101:
            operation = ArithmeticInstruction::Operation::DivideUnsignedWord;
            break;
        case 0b110:
            operation = ArithmeticInstruction::Operation::RemainderWord;
            break;
        case 0b111:
            operation = ArithmeticInstruction::Operation::RemainderUnsignedWord;
            break;
        default:
            return adopt_own(*new (nothrow) UnknownInstruction);
        }
    }
    return adopt_own(*new ArithmeticInstruction(operation, raw_parts.rs1, raw_parts.rs2, raw_parts.rd));
}

NonnullOwnPtr<MemoryLoad> parse_load(u32 instruction)
{
    auto raw_parts = RawIType::parse(instruction);
    auto width = MemoryAccessMode::from_funct3(raw_parts.funct3);
    return adopt_own(*new (nothrow) MemoryLoad(raw_parts.imm, raw_parts.rs1, width, raw_parts.rd));
}

NonnullOwnPtr<MemoryStore> parse_store(u32 instruction)
{
    auto raw_parts = RawSType::parse(instruction);
    auto width = MemoryAccessMode::from_funct3(raw_parts.funct3);
    return adopt_own(*new (nothrow) MemoryStore(raw_parts.imm, raw_parts.rs2, raw_parts.rs1, width));
}

NonnullOwnPtr<InstructionImpl> parse_branch(u32 instruction)
{
    auto raw_parts = RawBType::parse(instruction);
    if (raw_parts.funct3 == 0b010 || raw_parts.funct3 == 0b011)
        return adopt_own(*new (nothrow) UnknownInstruction);
    auto condition = static_cast<Branch::Condition>(raw_parts.funct3);
    return adopt_own(*new (nothrow) Branch(condition, raw_parts.imm, raw_parts.rs1, raw_parts.rs2));
}

NonnullOwnPtr<InstructionImpl> parse_misc_mem(u32 instruction)
{
    auto raw_parts = RawIType::parse(instruction);

    switch (raw_parts.funct3) {
    case 0b000: {
        auto successor = static_cast<Fence::AccessType>(raw_parts.imm & 0b1111);
        auto predecessor = static_cast<Fence::AccessType>((raw_parts.imm >> 4) & 0b1111);
        auto mode = static_cast<Fence::Mode>((raw_parts.imm >> 8) & 0b1111);
        return adopt_own(*new (nothrow) Fence(predecessor, successor, mode));
    }
    case 0b001: {
        return adopt_own(*new (nothrow) InstructionFetchFence);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

}
