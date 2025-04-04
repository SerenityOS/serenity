/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FD.h"

namespace Disassembly::RISCV64 {

NonnullOwnPtr<InstructionImpl> parse_op_fp(u32 instruction)
{
    auto raw_parts = RawRType::parse(instruction);
    auto width = static_cast<FloatWidth>(raw_parts.funct7 & 0b11);
    auto rounding_mode = static_cast<RoundingMode>(raw_parts.funct3);
    // Not mentioned in the spec, but bit 3 of funct7 distinguishes between float-to-integer (0) and integer-to-float (1) conversions in relevant instructions.
    auto is_int_to_float = (raw_parts.funct7 & (1 << 3)) > 0;

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
            VERIFY_NOT_REACHED();
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
            VERIFY_NOT_REACHED();
        }
        rounding_mode = RoundingMode::DYN;
        break;
    case 0b0101100:
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
            VERIFY_NOT_REACHED();
        }
        return adopt_own(*new FloatCompare(compare_operation, width, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rs2), raw_parts.rd));
    }
    case 0b0100000: {
        if ((raw_parts.funct7 & 1) > 0)
            return adopt_own(*new ConvertFloat(ConvertFloat::Operation::SingleToDouble, rounding_mode, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rd)));
        else
            return adopt_own(*new ConvertFloat(ConvertFloat::Operation::DoubleToSingle, rounding_mode, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rd)));
    }
    case 0b1110000: {
        if (raw_parts.funct3 == 0b001)
            return adopt_own(*new FloatClassify(width, as_float_register(raw_parts.rs1), raw_parts.rd));
        else
            return adopt_own(*new MoveFloatToInteger(width, as_float_register(raw_parts.rs1), raw_parts.rd));
    }
    case 0b1100000:
    case 0b1101000: {
        // This is not explicitly mentioned in the spec, but all "normal" float-integer conversion functions can be distinguished by 4 specific bits:
        // - Bit 0 of "rs2" distinguishes between signed (0) and unsigned (1) integer conversions
        auto signedness = (raw_parts.rs2.value() & 1) > 0 ? Signedness::Unsigned : Signedness::Signed;
        // - Bit 1 of "rs2" distinguishes between word (0) and doubleword (1) integer conversions
        auto integer_word_width = (raw_parts.rs2.value() & 0b10) > 0 ? DataWidth::DoubleWord : DataWidth::Word;
        MemoryAccessMode integer_width { .width = integer_word_width, .signedness = signedness };
        if (is_int_to_float)
            return adopt_own(*new ConvertIntegerToFloat(rounding_mode, integer_width, width, raw_parts.rs1, as_float_register(raw_parts.rd)));
        else
            return adopt_own(*new ConvertFloatToInteger(rounding_mode, integer_width, width, as_float_register(raw_parts.rs1), raw_parts.rd));
    }
    case 0b1111000:
        return adopt_own(*new MoveIntegerToFloat(width, raw_parts.rs1, as_float_register(raw_parts.rd)));
    default:
        VERIFY_NOT_REACHED();
    }
    return adopt_own(*new FloatArithmeticInstruction(operation, rounding_mode, width, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rs2), as_float_register(raw_parts.rd)));
}

NonnullOwnPtr<FloatFusedMultiplyAdd> parse_fma(u32 instruction)
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
        VERIFY_NOT_REACHED();
    }
    return adopt_own(*new FloatFusedMultiplyAdd(operation, rounding_mode, width, as_float_register(raw_parts.rs1), as_float_register(raw_parts.rs2), as_float_register(raw_parts.rs3), as_float_register(raw_parts.rd)));
}

NonnullOwnPtr<FloatMemoryLoad> parse_load_fp(u32 instruction)
{
    auto raw_parts = RawIType::parse(instruction);
    auto width = data_width_to_float_width(static_cast<DataWidth>(raw_parts.funct3 & 0b11));
    return adopt_own(*new (nothrow) FloatMemoryLoad(raw_parts.imm, raw_parts.rs1, width, as_float_register(raw_parts.rd)));
}

NonnullOwnPtr<FloatMemoryStore> parse_store_fp(u32 instruction)
{
    auto raw_parts = RawSType::parse(instruction);
    auto width = data_width_to_float_width(static_cast<DataWidth>(raw_parts.funct3 & 0b11));
    return adopt_own(*new (nothrow) FloatMemoryStore(raw_parts.imm, as_float_register(raw_parts.rs2), raw_parts.rs1, width));
}

}
