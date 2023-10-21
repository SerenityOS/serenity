/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/String.h>
#include <LibDisassembly/riscv64/Encoding.h>
#include <LibDisassembly/riscv64/IM.h>
#include <LibDisassembly/riscv64/Instruction.h>

template<>
struct AK::Formatter<Disassembly::RISCV64::Register> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::Register reg)
    {
        return AK::Formatter<FormatString>::format(builder, "x{}"sv, static_cast<u8>(reg));
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::FloatRegister> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::FloatRegister reg)
    {
        return AK::Formatter<FormatString>::format(builder, "f{}"sv, static_cast<u8>(reg));
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::RegisterABINames> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::RegisterABINames reg)
    {
        auto formatted = ""sv;
        switch (reg) {
        case Disassembly::RISCV64::RegisterABINames::zero:
            formatted = "zero"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::ra:
            formatted = "ra"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::sp:
            formatted = "sp"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::gp:
            formatted = "gp"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::tp:
            formatted = "tp"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t0:
            formatted = "t0"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t1:
            formatted = "t1"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t2:
            formatted = "t2"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s0:
            formatted = "s0"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s1:
            formatted = "s1"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a0:
            formatted = "a0"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a1:
            formatted = "a1"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a2:
            formatted = "a2"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a3:
            formatted = "a3"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a4:
            formatted = "a4"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a5:
            formatted = "a5"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a6:
            formatted = "a6"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::a7:
            formatted = "a7"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s2:
            formatted = "s2"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s3:
            formatted = "s3"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s4:
            formatted = "s4"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s5:
            formatted = "s5"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s6:
            formatted = "s6"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s7:
            formatted = "s7"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s8:
            formatted = "s8"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s9:
            formatted = "s9"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s10:
            formatted = "s10"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::s11:
            formatted = "s11"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t3:
            formatted = "t3"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t4:
            formatted = "t4"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t5:
            formatted = "t5"sv;
            break;
        case Disassembly::RISCV64::RegisterABINames::t6:
            formatted = "t6"sv;
            break;
        }
        return AK::Formatter<StringView>::format(builder, formatted);
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::RegisterABINamesWithFP> : AK::Formatter<Disassembly::RISCV64::RegisterABINames> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::RegisterABINamesWithFP reg)
    {
        if (reg == Disassembly::RISCV64::RegisterABINamesWithFP::fp)
            return AK::Formatter<StringView>::format(builder, "fp"sv);
        return AK::Formatter<Disassembly::RISCV64::RegisterABINames>::format(builder, static_cast<Disassembly::RISCV64::RegisterABINames>(reg));
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::FloatRegisterABINames> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::FloatRegisterABINames reg)
    {
        auto formatted = ""sv;
        switch (reg) {
        case Disassembly::RISCV64::FloatRegisterABINames::ft0:
            formatted = "ft0"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft1:
            formatted = "ft1"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft2:
            formatted = "ft2"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft3:
            formatted = "ft3"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft4:
            formatted = "ft4"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft5:
            formatted = "ft5"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft6:
            formatted = "ft6"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft7:
            formatted = "ft7"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs0:
            formatted = "fs0"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs1:
            formatted = "fs1"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa0:
            formatted = "fa0"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa1:
            formatted = "fa1"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa2:
            formatted = "fa2"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa3:
            formatted = "fa3"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa4:
            formatted = "fa4"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa5:
            formatted = "fa5"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa6:
            formatted = "fa6"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fa7:
            formatted = "fa7"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs2:
            formatted = "fs2"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs3:
            formatted = "fs3"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs4:
            formatted = "fs4"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs5:
            formatted = "fs5"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs6:
            formatted = "fs6"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs7:
            formatted = "fs7"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs8:
            formatted = "fs8"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs9:
            formatted = "fs9"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs10:
            formatted = "fs10"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::fs11:
            formatted = "fs11"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft8:
            formatted = "ft8"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft9:
            formatted = "ft9"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft10:
            formatted = "ft10"sv;
            break;
        case Disassembly::RISCV64::FloatRegisterABINames::ft11:
            formatted = "ft11"sv;
            break;
        }
        return AK::Formatter<StringView>::format(builder, formatted);
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::ArithmeticInstruction::Operation> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::ArithmeticInstruction::Operation op)
    {
        auto op_name = ""sv;
        switch (op) {
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::Add:
            op_name = "add"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::Subtract:
            op_name = "sub"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::SetLessThan:
            op_name = "slt"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::SetLessThanUnsigned:
            op_name = "sltu"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::Xor:
            op_name = "xor"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::Or:
            op_name = "or"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::And:
            op_name = "and"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::ShiftLeftLogical:
            op_name = "sll"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::ShiftRightLogical:
            op_name = "srl"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::ShiftRightArithmetic:
            op_name = "sra"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::AddWord:
            op_name = "addw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::SubtractWord:
            op_name = "subw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::ShiftLeftLogicalWord:
            op_name = "sllw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::ShiftRightLogicalWord:
            op_name = "srlw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::ShiftRightArithmeticWord:
            op_name = "sraw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::Multiply:
            op_name = "mul"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::MultiplyHigh:
            op_name = "mulh"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::MultiplyHighSignedUnsigned:
            op_name = "mulhsu"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::MultiplyHighUnsigned:
            op_name = "mulhu"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::Divide:
            op_name = "div"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::DivideUnsigned:
            op_name = "divu"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::Remainder:
            op_name = "rem"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::RemainderUnsigned:
            op_name = "remu"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::MultiplyWord:
            op_name = "mulw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::DivideWord:
            op_name = "divw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::DivideUnsignedWord:
            op_name = "divuw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::RemainderWord:
            op_name = "remw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticInstruction::Operation::RemainderUnsignedWord:
            op_name = "remuw"sv;
            break;
        }
        return AK::Formatter<StringView>::format(builder, op_name);
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation op)
    {
        auto op_name = ""sv;
        switch (op) {
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::Add:
            op_name = "addi"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::SetLessThan:
            op_name = "slti"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::SetLessThanUnsigned:
            op_name = "sltiu"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::Xor:
            op_name = "xori"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::Or:
            op_name = "ori"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::And:
            op_name = "andi"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::ShiftLeftLogical:
            op_name = "slli"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::ShiftRightLogical:
            op_name = "srli"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::ShiftRightArithmetic:
            op_name = "srai"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::AddWord:
            op_name = "addiw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::ShiftLeftLogicalWord:
            op_name = "slliw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::ShiftRightLogicalWord:
            op_name = "srliw"sv;
            break;
        case Disassembly::RISCV64::ArithmeticImmediateInstruction::Operation::ShiftRightArithmeticWord:
            op_name = "sraiw"sv;
            break;
        }
        return AK::Formatter<StringView>::format(builder, op_name);
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::Branch::Condition> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::Branch::Condition op)
    {
        auto op_name = ""sv;
        switch (op) {
        case Disassembly::RISCV64::Branch::Condition::Equals:
            op_name = "beq"sv;
            break;
        case Disassembly::RISCV64::Branch::Condition::NotEquals:
            op_name = "bne"sv;
            break;
        case Disassembly::RISCV64::Branch::Condition::LessThan:
            op_name = "blt"sv;
            break;
        case Disassembly::RISCV64::Branch::Condition::GreaterEquals:
            op_name = "bge"sv;
            break;
        case Disassembly::RISCV64::Branch::Condition::LessThanUnsigned:
            op_name = "bltu"sv;
            break;
        case Disassembly::RISCV64::Branch::Condition::GreaterEqualsUnsigned:
            op_name = "bgeu"sv;
            break;
        }
        return AK::Formatter<StringView>::format(builder, op_name);
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::Fence::AccessType> : AK::Formatter<String> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::Fence::AccessType op)
    {
        StringBuilder op_name;
        if (Disassembly::RISCV64::has_flag(op, Disassembly::RISCV64::Fence::AccessType::Input))
            op_name.append_code_point('i');
        if (Disassembly::RISCV64::has_flag(op, Disassembly::RISCV64::Fence::AccessType::Output))
            op_name.append_code_point('o');
        if (Disassembly::RISCV64::has_flag(op, Disassembly::RISCV64::Fence::AccessType::Read))
            op_name.append_code_point('r');
        if (Disassembly::RISCV64::has_flag(op, Disassembly::RISCV64::Fence::AccessType::Write))
            op_name.append_code_point('w');

        return AK::Formatter<String>::format(builder, MUST(op_name.to_string()));
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::MemoryAccessMode> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::MemoryAccessMode mode)
    {
        auto width_str = ""sv;
        switch (mode.width) {
        case Disassembly::RISCV64::DataWidth::Byte:
            width_str = "b"sv;
            break;
        case Disassembly::RISCV64::DataWidth::Halfword:
            width_str = "h"sv;
            break;
        case Disassembly::RISCV64::DataWidth::Word:
            width_str = "w"sv;
            break;
        case Disassembly::RISCV64::DataWidth::DoubleWord:
            width_str = "d"sv;
            break;
        case Disassembly::RISCV64::DataWidth::QuadWord:
            width_str = "q"sv;
            break;
        }
        auto signedness_str = ""sv;
        if (mode.signedness == Disassembly::RISCV64::Signedness::Unsigned)
            signedness_str = "u"sv;

        return AK::Formatter<FormatString>::format(builder, "{}{}"sv, width_str, signedness_str);
    }
};

template<>
struct AK::Formatter<Disassembly::RISCV64::RoundingMode> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Disassembly::RISCV64::RoundingMode reg)
    {
        auto formatted = ""sv;
        switch (reg) {
        case Disassembly::RISCV64::RoundingMode::RNE:
            formatted = "rne"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::RTZ:
            formatted = "rtz"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::RDN:
            formatted = "rdn"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::RUP:
            formatted = "rup"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::RMM:
            formatted = "rmm"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::Invalid1:
        case Disassembly::RISCV64::RoundingMode::Invalid2:
            formatted = "invalid"sv;
            break;
        case Disassembly::RISCV64::RoundingMode::DYN:
            formatted = "dyn"sv;
            break;
        }
        return AK::Formatter<StringView>::format(builder, formatted);
    }
};
