/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FD.h"
#include <AK/Assertions.h>
#include <LibDisassembly/riscv64/Instruction.h>

namespace Disassembly::RISCV64 {

NonnullOwnPtr<InstructionImpl> parse_op_fp(u32 instruction)
{
    auto raw_parts = RawRType::parse(instruction);
    auto width = static_cast<FloatWidth>(raw_parts.funct7 & 0b11);
    auto rounding_mode = static_cast<RoundingMode>(raw_parts.funct3);

    auto operation = FloatArithmeticInstruction::Operation::Add;
    switch (raw_parts.funct7 & ~0b11) {
    case 0b0000000:
        operation = FloatArithmeticInstruction::Operation::Add;
        break;
    case 0b0000100:
        operation = FloatArithmeticInstruction::Operation::Subtract;
        break;
    case 0b0001000:
        operation = FloatArithmeticInstruction::Operation::Multiply;
        break;
    case 0b0001100:
        operation = FloatArithmeticInstruction::Operation::Divide;
        break;
    case 0b0010000:
        switch (raw_parts.funct3) {
        case 0b000:
            operation = FloatArithmeticInstruction::Operation::SignInject;
            break;
        case 0b001:
            operation = FloatArithmeticInstruction::Operation::SignInjectNegate;
            break;
        case 0b010:
            operation = FloatArithmeticInstruction::Operation::SignInjectXor;
            break;
        default:
            return adopt_own(*new (nothrow) UnknownInstruction);
        }
        rounding_mode = RoundingMode::DYN;
        break;
    case 0b0010100:
        switch (raw_parts.funct3) {
        case 0b000:
            operation = FloatArithmeticInstruction::Operation::Min;
            break;
        case 0b001:
            operation = FloatArithmeticInstruction::Operation::Max;
            break;
        default:
            return adopt_own(*new (nothrow) UnknownInstruction);
        }
        rounding_mode = RoundingMode::DYN;
        break;
    case 0b0101100:
        if (raw_parts.rs2 != 0)
            return adopt_own(*new (nothrow) UnknownInstruction);
        return adopt_own(*new FloatSquareRoot(rounding_mode, width, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rd)));
    case 0b1010000: {
        auto compare_operation = FloatCompare::Operation::Equals;
        switch (raw_parts.funct3) {
        case 0b010:
            compare_operation = FloatCompare::Operation::Equals;
            break;
        case 0b001:
            compare_operation = FloatCompare::Operation::LessThan;
            break;
        case 0b000:
            compare_operation = FloatCompare::Operation::LessThanEquals;
            break;
        default:
            return adopt_own(*new (nothrow) UnknownInstruction);
        }
        return adopt_own(*new FloatCompare(compare_operation, width, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rs2), raw_parts.rd));
    }
    case 0b0100000: {
        if (raw_parts.funct7 == 0b0100000 && raw_parts.rs2 == 1)
            return adopt_own(*new ConvertFloat(ConvertFloat::Operation::DoubleToSingle, rounding_mode, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rd)));
        if (raw_parts.funct7 == 0b0100001 && raw_parts.rs2 == 0)
            return adopt_own(*new ConvertFloat(ConvertFloat::Operation::SingleToDouble, rounding_mode, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rd)));
        return adopt_own(*new (nothrow) UnknownInstruction);
    }
    case 0b1110000: {
        if (raw_parts.rs2 != 0 || raw_parts.funct3 > 0b001)
            return adopt_own(*new (nothrow) UnknownInstruction);
        if (raw_parts.funct3 == 0b001)
            return adopt_own(*new FloatClassify(width, as_float_register(raw_parts.rs1), raw_parts.rd));
        if (raw_parts.funct3 == 0b000)
            return adopt_own(*new MoveFloatToInteger(width, as_float_register(raw_parts.rs1), raw_parts.rd));
        VERIFY_NOT_REACHED(); // Logically impossible
    }
    case 0b1100000:
    case 0b1101000: {
        if (raw_parts.rs2.value() > 0b11)
            return adopt_own(*new (nothrow) UnknownInstruction);
        // Not mentioned in the spec, but bit 3 of funct7 distinguishes between float-to-integer (0) and integer-to-float (1) conversions.
        auto is_int_to_float = (raw_parts.funct7 & (1 << 3)) > 0;
        // Not mentioned in the spec either, but all "normal" float-integer conversion functions can be distinguished by 4 specific bits:
        // - Bit 0 of "rs2" distinguishes between signed (0) and unsigned (1) integer conversions
        auto signedness = (raw_parts.rs2.value() & 1) > 0 ? Signedness::Unsigned : Signedness::Signed;
        // - Bit 1 of "rs2" distinguishes between word (0) and doubleword (1) integer conversions
        auto integer_word_width = (raw_parts.rs2.value() & 0b10) > 0 ? DataWidth::DoubleWord : DataWidth::Word;
        MemoryAccessMode integer_width { .width = integer_word_width, .signedness = signedness };
        if (is_int_to_float)
            return adopt_own(*new ConvertIntegerToFloat(rounding_mode, integer_width, width, raw_parts.rs1, as_float_register(raw_parts.rd)));
        return adopt_own(*new ConvertFloatToInteger(rounding_mode, integer_width, width, as_float_register(raw_parts.rs1), raw_parts.rd));
    }
    case 0b1111000:
        if (raw_parts.rs2 != 0)
            return adopt_own(*new (nothrow) UnknownInstruction);
        return adopt_own(*new MoveIntegerToFloat(width, raw_parts.rs1, as_float_register(raw_parts.rd)));
    default:
        return adopt_own(*new (nothrow) UnknownInstruction);
    }
    return adopt_own(*new FloatArithmeticInstruction(operation, rounding_mode, width, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rs2), as_float_register(raw_parts.rd)));
}

NonnullOwnPtr<InstructionImpl> parse_fma(u32 instruction)
{
    auto raw_parts = RawR4Type::parse(instruction);
    auto width = static_cast<FloatWidth>(raw_parts.fmt);
    auto rounding_mode = static_cast<RoundingMode>(raw_parts.funct3);

    auto operation = FloatFusedMultiplyAdd::Operation::MultiplyAdd;
    switch (static_cast<MajorOpcode>(raw_parts.opcode)) {
    case MajorOpcode::MADD:
        operation = FloatFusedMultiplyAdd::Operation::MultiplyAdd;
        break;
    case MajorOpcode::MSUB:
        operation = FloatFusedMultiplyAdd::Operation::MultiplySubtract;
        break;
    case MajorOpcode::NMADD:
        operation = FloatFusedMultiplyAdd::Operation::NegatedMultiplyAdd;
        break;
    case MajorOpcode::NMSUB:
        operation = FloatFusedMultiplyAdd::Operation::NegatedMultiplySubtract;
        break;
    default:
        return adopt_own(*new (nothrow) UnknownInstruction);
    }
    return adopt_own(*new FloatFusedMultiplyAdd(operation, rounding_mode, width, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rs2), as_float_register(raw_parts.rs3), as_float_register(raw_parts.rd)));
}

NonnullOwnPtr<InstructionImpl> parse_load_fp(u32 instruction)
{
    auto raw_parts = RawIType::parse(instruction);
    if (raw_parts.funct3 > 0b11)
        return adopt_own(*new (nothrow) UnknownInstruction);

    auto width = data_width_to_float_width(static_cast<DataWidth>(raw_parts.funct3 & 0b11));
    return adopt_own(*new (nothrow) FloatMemoryLoad(raw_parts.imm, raw_parts.rs1, width, as_float_register(raw_parts.rd)));
}

NonnullOwnPtr<InstructionImpl> parse_store_fp(u32 instruction)
{
    auto raw_parts = RawSType::parse(instruction);
    if (raw_parts.funct3 > 0b11)
        return adopt_own(*new (nothrow) UnknownInstruction);

    auto width = data_width_to_float_width(static_cast<DataWidth>(raw_parts.funct3 & 0b11));
    return adopt_own(*new (nothrow) FloatMemoryStore(raw_parts.imm, as_float_register(raw_parts.rs2), raw_parts.rs1, width));
}

}
