/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "C.h"

namespace Disassembly::RISCV64 {

NonnullOwnPtr<MemoryLoad> parse_c_lw(u16 instruction)
{
    auto raw_parts = RawCLType::parse(instruction);
    return adopt_own(*new (nothrow) MemoryLoad(
        raw_parts.imm, raw_parts.rs1, MemoryAccessMode { DataWidth::Word, Signedness::Signed }, raw_parts.rd));
}

NonnullOwnPtr<MemoryStore> parse_c_sw(u16 instruction)
{
    auto raw_parts = RawCSType::parse(instruction);
    return adopt_own(*new (nothrow) MemoryStore(
        raw_parts.imm, raw_parts.rs2, raw_parts.rs1, MemoryAccessMode { DataWidth::Word, Signedness::Signed }));
}

NonnullOwnPtr<MemoryLoad> parse_c_ldsp(u16 instruction)
{
    auto raw_parts = RawCIType::parse(instruction);
    return adopt_own(*new (nothrow) MemoryLoad(
        raw_parts.imm, static_cast<Register>(to_underlying(RegisterABINames::sp)), MemoryAccessMode { DataWidth::DoubleWord, Signedness::Signed }, raw_parts.rd_or_rs1));
}

NonnullOwnPtr<FloatMemoryLoad> parse_c_fldsp(u16 instruction)
{
    auto raw_parts = RawCIType::parse(instruction);
    return adopt_own(*new (nothrow) FloatMemoryLoad(
        raw_parts.imm, static_cast<Register>(to_underlying(RegisterABINames::sp)), FloatWidth::Double, as_float_register(raw_parts.rd_or_rs1)));
}

NonnullOwnPtr<MemoryStore> parse_c_sdsp(u16 instruction)
{
    auto raw_parts = RawCSSType::parse(instruction);
    return adopt_own(*new (nothrow) MemoryStore(
        raw_parts.imm, raw_parts.rs2, static_cast<Register>(to_underlying(RegisterABINames::sp)), MemoryAccessMode { DataWidth::DoubleWord, Signedness::Signed }));
}

NonnullOwnPtr<FloatMemoryStore> parse_c_fsdsp(u16 instruction)
{
    auto raw_parts = RawCSSType::parse(instruction);
    return adopt_own(*new (nothrow) FloatMemoryStore(
        raw_parts.imm, as_float_register(raw_parts.rs2), static_cast<Register>(to_underlying(RegisterABINames::sp)), FloatWidth::Double));
}

NonnullOwnPtr<MemoryLoad> parse_c_lwsp(u16 instruction)
{
    auto raw_parts = RawCIType::parse(instruction);
    return adopt_own(*new (nothrow) MemoryLoad(
        raw_parts.imm, static_cast<Register>(to_underlying(RegisterABINames::sp)), MemoryAccessMode { DataWidth::Word, Signedness::Signed }, raw_parts.rd_or_rs1));
}

NonnullOwnPtr<MemoryStore> parse_c_swsp(u16 instruction)
{
    auto raw_parts = RawCSSType::parse(instruction);
    return adopt_own(*new (nothrow) MemoryStore(
        raw_parts.imm, raw_parts.rs2, static_cast<Register>(to_underlying(RegisterABINames::sp)), MemoryAccessMode { DataWidth::Word, Signedness::Signed }));
}

NonnullOwnPtr<InstructionImpl> parse_c_lui_or_addi16sp(u16 instruction)
{
    auto raw_parts = RawCIType::parse(instruction);
    if (raw_parts.rd_or_rs1 == RegisterABINames::sp) {
        // C.ADDI16SP
        return adopt_own(*new (nothrow) ArithmeticImmediateInstruction(ArithmeticImmediateInstruction::Operation::Add,
            raw_parts.imm, raw_parts.rd_or_rs1, raw_parts.rd_or_rs1));
    } else {
        // C.LUI
        return adopt_own(*new (nothrow) LoadUpperImmediate(raw_parts.imm, raw_parts.rd_or_rs1));
    }
}

NonnullOwnPtr<ArithmeticImmediateInstruction> parse_c_addi(u16 instruction)
{
    auto raw_parts = RawCIType::parse(instruction);
    return adopt_own(*new (nothrow) ArithmeticImmediateInstruction(ArithmeticImmediateInstruction::Operation::Add,
        raw_parts.imm, raw_parts.rd_or_rs1, raw_parts.rd_or_rs1));
}

NonnullOwnPtr<ArithmeticImmediateInstruction> parse_c_addi4spn(u16 instruction)
{
    auto raw_parts = RawCIWType::parse(instruction);
    return adopt_own(*new (nothrow) ArithmeticImmediateInstruction(ArithmeticImmediateInstruction::Operation::Add,
        raw_parts.imm, static_cast<Register>(to_underlying(RegisterABINames::sp)), raw_parts.rd));
}

NonnullOwnPtr<ArithmeticImmediateInstruction> parse_c_alu_imm(u16 instruction)
{
    auto raw_parts = RawCBType::parse(instruction);
    auto operation = ArithmeticImmediateInstruction::Operation::Add;
    switch (raw_parts.funct2) {
    case 0b00:
        operation = ArithmeticImmediateInstruction::Operation::ShiftRightLogical;
        // Remove sign-extension for shifts.
        raw_parts.offset &= 0b111111;
        break;
    case 0b01:
        operation = ArithmeticImmediateInstruction::Operation::ShiftRightArithmetic;
        raw_parts.offset &= 0b111111;
        break;
    case 0b10:
        operation = ArithmeticImmediateInstruction::Operation::And;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    return adopt_own(*new (nothrow) ArithmeticImmediateInstruction(operation,
        raw_parts.offset, raw_parts.rs1, raw_parts.rs1));
}

NonnullOwnPtr<Branch> parse_c_beqz(u16 instruction)
{
    auto raw_parts = RawCBType::parse(instruction);
    return adopt_own(*new (nothrow) Branch(Branch::Condition::Equals,
        raw_parts.offset, raw_parts.rs1, static_cast<Register>(to_underlying(RegisterABINames::zero))));
}

NonnullOwnPtr<Branch> parse_c_bnez(u16 instruction)
{
    auto raw_parts = RawCBType::parse(instruction);
    return adopt_own(*new (nothrow) Branch(Branch::Condition::NotEquals,
        raw_parts.offset, raw_parts.rs1, static_cast<Register>(to_underlying(RegisterABINames::zero))));
}

NonnullOwnPtr<InstructionImpl> parse_c_alu(u16 instruction)
{
    auto raw_parts = RawCAType::parse(instruction);

    auto is_immediate = (raw_parts.funct6 & 0b11) != 0b11;
    if (is_immediate)
        return parse_c_alu_imm(instruction);

    auto is_word_instruction = (raw_parts.funct6 & 0b100) > 0;
    auto operation = ArithmeticInstruction::Operation::Add;
    switch (raw_parts.funct2) {
    case 0b00:
        operation = is_word_instruction ? ArithmeticInstruction::Operation::SubtractWord : ArithmeticInstruction::Operation::Subtract;
        break;
    case 0b01:
        operation = is_word_instruction ? ArithmeticInstruction::Operation::AddWord : ArithmeticInstruction::Operation::Xor;
        break;
    case 0b10:
        operation = ArithmeticInstruction::Operation::Or;
        break;
    case 0b11:
        operation = ArithmeticInstruction::Operation::And;
        break;
    }

    return adopt_own(*new (nothrow) ArithmeticInstruction(operation,
        raw_parts.rd_or_rs1, raw_parts.rs2, raw_parts.rd_or_rs1));
}

NonnullOwnPtr<ArithmeticImmediateInstruction> parse_c_slli(u16 instruction)
{
    auto raw_parts = RawCIType::parse(instruction);
    return adopt_own(*new (nothrow) ArithmeticImmediateInstruction(ArithmeticImmediateInstruction::Operation::ShiftLeftLogical,
        raw_parts.imm, raw_parts.rd_or_rs1, raw_parts.rd_or_rs1));
}

NonnullOwnPtr<ArithmeticImmediateInstruction> parse_c_li(u16 instruction)
{
    auto raw_parts = RawCIType::parse(instruction);
    return adopt_own(*new (nothrow) ArithmeticImmediateInstruction(ArithmeticImmediateInstruction::Operation::Add,
        raw_parts.imm, static_cast<Register>(to_underlying(RegisterABINames::zero)), raw_parts.rd_or_rs1));
}

NonnullOwnPtr<JumpAndLink> parse_c_j(u16 instruction)
{
    auto raw_parts = RawCJType::parse(instruction);
    return adopt_own(*new (nothrow) JumpAndLink(
        raw_parts.jump_target, static_cast<Register>(to_underlying(RegisterABINames::zero))));
}

NonnullOwnPtr<MemoryLoad> parse_c_ld(u16 instruction)
{
    auto raw_parts = RawCLType::parse(instruction);
    return adopt_own(*new (nothrow) MemoryLoad(
        raw_parts.imm, raw_parts.rs1, MemoryAccessMode { .width = DataWidth::DoubleWord, .signedness = Signedness::Signed }, raw_parts.rd));
}

NonnullOwnPtr<FloatMemoryLoad> parse_c_fld(u16 instruction)
{
    auto raw_parts = RawCLType::parse(instruction);
    return adopt_own(*new (nothrow) FloatMemoryLoad(
        raw_parts.imm, raw_parts.rs1, FloatWidth::Double, as_float_register(raw_parts.rd)));
}

NonnullOwnPtr<MemoryStore> parse_c_sd(u16 instruction)
{
    auto raw_parts = RawCSType::parse(instruction);
    return adopt_own(*new (nothrow) MemoryStore(
        raw_parts.imm, raw_parts.rs2, raw_parts.rs1, MemoryAccessMode { DataWidth::DoubleWord, Signedness::Signed }));
}

NonnullOwnPtr<FloatMemoryStore> parse_c_fsd(u16 instruction)
{
    auto raw_parts = RawCSType::parse(instruction);
    return adopt_own(*new (nothrow) FloatMemoryStore(
        raw_parts.imm, as_float_register(raw_parts.rs2), raw_parts.rs1, FloatWidth::Double));
}

NonnullOwnPtr<InstructionImpl> parse_c_jalr_mv_add(u16 instruction)
{
    auto raw_parts = RawCRType::parse(instruction);
    if (raw_parts.funct4 == 0b1000) {
        // C.MV
        if (raw_parts.rs2 != 0) {
            return adopt_own(*new (nothrow) ArithmeticInstruction(ArithmeticInstruction::Operation::Add,
                static_cast<Register>(to_underlying(RegisterABINames::zero)), raw_parts.rs2, raw_parts.rd_or_rs1));
        } else {
            // C.JR
            return adopt_own(*new (nothrow) JumpAndLinkRegister(
                0, raw_parts.rd_or_rs1, static_cast<Register>(to_underlying(RegisterABINames::zero))));
        }
    } else {
        // C.EBREAK
        if (raw_parts.rd_or_rs1 == 0 && raw_parts.rs2 == 0) {
            return adopt_own(*new (nothrow) EnvironmentBreak);
        } else if (raw_parts.rs2 == 0) {
            // C.JALR
            return adopt_own(*new (nothrow) JumpAndLinkRegister(
                0, raw_parts.rd_or_rs1, static_cast<Register>(to_underlying(RegisterABINames::ra))));
        } else {
            // C.ADD
            return adopt_own(*new (nothrow) ArithmeticInstruction(ArithmeticInstruction::Operation::Add,
                raw_parts.rd_or_rs1, raw_parts.rs2, raw_parts.rd_or_rs1));
        }
    }
}

NonnullOwnPtr<InstructionImpl> parse_c_addiw(u16 instruction)
{
    auto raw_parts = RawCIType::parse(instruction);
    return adopt_own(*new (nothrow) ArithmeticImmediateInstruction(ArithmeticImmediateInstruction::Operation::Add,
        raw_parts.imm, raw_parts.rd_or_rs1, raw_parts.rd_or_rs1));
}

}
