/*
 * Copyright (c) 2025, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/String.h>
#include <LibDisassembly/Architecture.h>
#include <LibDisassembly/Disassembler.h>
#include <LibDisassembly/InstructionStream.h>
#include <LibDisassembly/riscv64/A.h>
#include <LibDisassembly/riscv64/FD.h>
#include <LibDisassembly/riscv64/IM.h>
#include <LibDisassembly/riscv64/Instruction.h>
#include <LibDisassembly/riscv64/Priviledged.h>
#include <LibDisassembly/riscv64/Registers.h>
#include <LibDisassembly/riscv64/Zicsr.h>
#include <LibTest/TestCase.h>

// Make the long manual disassembly listings more readable.
using namespace Disassembly::RISCV64;
constexpr Register operator""_x(unsigned long long register_number)
{
    return static_cast<Register>(register_number);
}
constexpr FloatRegister operator""_f(unsigned long long register_number)
{
    return static_cast<FloatRegister>(register_number);
}

static void check_disassembly(ReadonlySpan<NonnullOwnPtr<InstructionImpl>> expected_instructions, ReadonlySpan<u8> machine_code, size_t start_address)
{
    auto expected_instruction_iter = expected_instructions.begin();
    Disassembly::SimpleInstructionStream instruction_stream { machine_code.data(), machine_code.size() };
    Disassembly::Disassembler disassembler { instruction_stream, Disassembly::Architecture::RISCV64 };

    while (true) {
        auto offset = instruction_stream.offset() + start_address;
        auto maybe_disassembled_instruction = disassembler.next();
        if (!maybe_disassembled_instruction.has_value())
            break;

        if (expected_instruction_iter.is_end()) {
            FAIL("Disassembler provided too many instructions");
            // Return to avoid crashing the test program so that other tests can run.
            break;
        }
        auto const& expected_instruction = **expected_instruction_iter;

        auto disassembled_instruction = maybe_disassembled_instruction.release_value().release_nonnull<Instruction>();
        auto const& instruction_data = disassembled_instruction->instruction_data();
        if (!instruction_data.instruction_equals(expected_instruction))
            FAIL(MUST(String::formatted("Disassembled incorrect instruction {:08x} at {:#04x}:\n  Expected: {}\n    Actual: {}",
                disassembled_instruction->raw_instruction(), offset, expected_instruction.to_string({}, offset, {}), instruction_data.to_string({}, offset, {}))));

        ++expected_instruction_iter;
    }
    if (!expected_instruction_iter.is_end())
        FAIL("Disassembler didn't read entire instruction stream");
}

// Based on the RISC-V instruction set listing, these tests check that all categories of instructions are implemented.
// In particular, complex immediate encoding tests are not a goal of these tests; they're checked as part of end-to-end tests with real-world code.
// Re-generate the machine code with the following commands:
//
// cd Tests/LibDisassembly
// ../../Toolchain/Local/riscv64/bin/riscv64-pc-serenity-gcc -march=rv64imafd_zicsr_zifencei -fpic -mno-relax -ffreestanding -nostdlib -nostartfiles -mno-csr-check all_riscv_instructions.s -o all_riscv_instructions.elf
// ../../Toolchain/Local/riscv64/bin/riscv64-pc-serenity-objcopy -O binary -j .text all_riscv_instructions.elf all_riscv_instructions.bin
// xxd -i all_riscv_instructions.bin
TEST_CASE(check_all_instructions)
{
    Array all_instructions = {
        // RV32I
        static_cast<NonnullOwnPtr<InstructionImpl>>(make<LoadUpperImmediate>(0x74a05000, 1_x)),
        make<AddUpperImmediateToProgramCounter>(0x6a7000, 2_x),
        make<JumpAndLink>(-8, 3_x),
        make<JumpAndLinkRegister>(2, 5_x, 4_x),
        make<Branch>(Branch::Condition::Equals, -16, 6_x, 7_x),
        make<Branch>(Branch::Condition::NotEquals, -20, 8_x, 9_x),
        make<Branch>(Branch::Condition::LessThan, -24, 10_x, 11_x),
        make<Branch>(Branch::Condition::GreaterEquals, -28, 12_x, 13_x),
        make<Branch>(Branch::Condition::LessThanUnsigned, -32, 14_x, 15_x),
        make<Branch>(Branch::Condition::GreaterEqualsUnsigned, -36, 16_x, 17_x),
        make<MemoryLoad>(1, 19_x, MemoryAccessMode { .width = DataWidth::Byte, .signedness = Signedness::Signed }, 18_x),
        make<MemoryLoad>(2, 21_x, MemoryAccessMode { .width = DataWidth::Halfword, .signedness = Signedness::Signed }, 20_x),
        make<MemoryLoad>(4, 23_x, MemoryAccessMode { .width = DataWidth::Word, .signedness = Signedness::Signed }, 22_x),
        make<MemoryLoad>(5, 25_x, MemoryAccessMode { .width = DataWidth::Byte, .signedness = Signedness::Unsigned }, 24_x),
        make<MemoryLoad>(6, 27_x, MemoryAccessMode { .width = DataWidth::Halfword, .signedness = Signedness::Unsigned }, 26_x),
        make<MemoryStore>(1, 28_x, 29_x, MemoryAccessMode { .width = DataWidth::Byte, .signedness = Signedness::Signed }),
        make<MemoryStore>(2, 30_x, 31_x, MemoryAccessMode { .width = DataWidth::Halfword, .signedness = Signedness::Signed }),
        make<MemoryStore>(4, 1_x, 2_x, MemoryAccessMode { .width = DataWidth::Word, .signedness = Signedness::Signed }),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::Add, 5, 4_x, 3_x),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::SetLessThan, 7, 6_x, 5_x),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::SetLessThanUnsigned, 9, 8_x, 7_x),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::Xor, 11, 10_x, 9_x),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::Or, 13, 12_x, 11_x),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::And, 15, 14_x, 13_x),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::ShiftLeftLogical, 17, 16_x, 15_x),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::ShiftRightLogical, 19, 18_x, 17_x),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::ShiftRightArithmetic, 21, 20_x, 19_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::Add, 4_x, 5_x, 3_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::Subtract, 23_x, 24_x, 22_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::ShiftLeftLogical, 16_x, 17_x, 15_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::SetLessThan, 6_x, 7_x, 5_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::SetLessThanUnsigned, 8_x, 9_x, 7_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::Xor, 10_x, 11_x, 9_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::ShiftRightLogical, 18_x, 19_x, 17_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::ShiftRightArithmetic, 20_x, 21_x, 19_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::Or, 12_x, 13_x, 11_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::And, 14_x, 15_x, 13_x),
        make<Fence>(Fence::AccessType::Input | Fence::AccessType::Read | Fence::AccessType::Write, Fence::AccessType::Write, Fence::Mode::Normal),
        make<Fence>(Fence::AccessType::Output, Fence::AccessType::Input | Fence::AccessType::Write, Fence::Mode::Normal),
        make<Fence>(Fence::AccessType::Read | Fence::AccessType::Write, Fence::AccessType::Read | Fence::AccessType::Write, Fence::Mode::NoStoreToLoadOrdering),
        make<EnvironmentCall>(),
        make<EnvironmentBreak>(),
        // RV64I
        make<MemoryLoad>(4, 2_x, MemoryAccessMode { .width = DataWidth::Word, .signedness = Signedness::Unsigned }, 1_x),
        make<MemoryLoad>(8, 4_x, MemoryAccessMode { .width = DataWidth::DoubleWord, .signedness = Signedness::Signed }, 3_x),
        make<MemoryStore>(16, 5_x, 6_x, MemoryAccessMode { .width = DataWidth::DoubleWord, .signedness = Signedness::Signed }),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::AddWord, -9, 8_x, 7_x),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::ShiftLeftLogicalWord, 11, 10_x, 9_x),
        make<ArithmeticImmediateInstruction>(ArithmeticImmediateInstruction::Operation::ShiftRightArithmeticWord, 13, 12_x, 11_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::AddWord, 14_x, 15_x, 13_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::SubtractWord, 17_x, 18_x, 16_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::ShiftLeftLogicalWord, 20_x, 21_x, 19_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::ShiftRightLogicalWord, 23_x, 24_x, 22_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::ShiftRightArithmeticWord, 26_x, 27_x, 25_x),
        // Zifencei
        make<InstructionFetchFence>(),
        // Zicsr
        make<CSRRegisterInstruction>(CSRInstruction::Operation::ReadWrite, 3, 2_x, 1_x),
        make<CSRRegisterInstruction>(CSRInstruction::Operation::ReadSet, 5, 4_x, 3_x),
        make<CSRRegisterInstruction>(CSRInstruction::Operation::ReadClear, 7, 6_x, 5_x),
        make<CSRImmediateInstruction>(CSRInstruction::Operation::ReadWrite, 8, 9, 7_x),
        make<CSRImmediateInstruction>(CSRInstruction::Operation::ReadSet, 9, 10, 8_x),
        make<CSRImmediateInstruction>(CSRInstruction::Operation::ReadClear, 10, 11, 9_x),
        // RV32M
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::Multiply, 2_x, 3_x, 1_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::MultiplyHigh, 5_x, 6_x, 4_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::MultiplyHighSignedUnsigned, 8_x, 9_x, 7_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::MultiplyHighUnsigned, 11_x, 12_x, 10_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::Divide, 14_x, 15_x, 13_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::DivideUnsigned, 17_x, 18_x, 16_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::Remainder, 20_x, 21_x, 19_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::RemainderUnsigned, 23_x, 24_x, 22_x),
        // RV64M
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::MultiplyWord, 2_x, 3_x, 1_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::DivideWord, 14_x, 15_x, 13_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::DivideUnsignedWord, 17_x, 18_x, 16_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::RemainderWord, 20_x, 21_x, 19_x),
        make<ArithmeticInstruction>(ArithmeticInstruction::Operation::RemainderUnsignedWord, 23_x, 24_x, 22_x),
        // RV32A
        make<LoadReservedStoreConditional>(LoadReservedStoreConditional::Operation::LoadReserved, true, false, DataWidth::Word, 2_x, 0_x, 1_x),
        make<LoadReservedStoreConditional>(LoadReservedStoreConditional::Operation::StoreConditional, false, true, DataWidth::Word, 5_x, 4_x, 3_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Swap, false, false, DataWidth::Word, 8_x, 7_x, 6_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Add, false, false, DataWidth::Word, 11_x, 10_x, 9_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Xor, false, false, DataWidth::Word, 14_x, 13_x, 12_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::And, false, false, DataWidth::Word, 17_x, 16_x, 15_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Or, false, false, DataWidth::Word, 20_x, 19_x, 18_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Min, false, false, DataWidth::Word, 23_x, 22_x, 21_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Max, false, false, DataWidth::Word, 26_x, 25_x, 24_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::MinUnsigned, false, false, DataWidth::Word, 29_x, 28_x, 27_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::MaxUnsigned, false, false, DataWidth::Word, 1_x, 31_x, 30_x),
        // RV64A
        make<LoadReservedStoreConditional>(LoadReservedStoreConditional::Operation::LoadReserved, false, true, DataWidth::DoubleWord, 2_x, 0_x, 1_x),
        make<LoadReservedStoreConditional>(LoadReservedStoreConditional::Operation::StoreConditional, false, false, DataWidth::DoubleWord, 5_x, 4_x, 3_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Swap, false, false, DataWidth::DoubleWord, 8_x, 7_x, 6_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Add, false, false, DataWidth::DoubleWord, 11_x, 10_x, 9_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Xor, false, false, DataWidth::DoubleWord, 14_x, 13_x, 12_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::And, false, false, DataWidth::DoubleWord, 17_x, 16_x, 15_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Or, false, false, DataWidth::DoubleWord, 20_x, 19_x, 18_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Min, false, false, DataWidth::DoubleWord, 23_x, 22_x, 21_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::Max, false, false, DataWidth::DoubleWord, 26_x, 25_x, 24_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::MinUnsigned, false, false, DataWidth::DoubleWord, 29_x, 28_x, 27_x),
        make<AtomicMemoryOperation>(AtomicMemoryOperation::Operation::MaxUnsigned, false, false, DataWidth::DoubleWord, 1_x, 31_x, 30_x),
        // RV32F
        make<FloatMemoryLoad>(4, 2_x, FloatWidth::Single, 1_f),
        make<FloatMemoryStore>(8, 3_f, 4_x, FloatWidth::Single),
        make<FloatFusedMultiplyAdd>(FloatFusedMultiplyAdd::Operation::MultiplyAdd, RoundingMode::DYN, FloatWidth::Single, 6_f, 7_f, 8_f, 5_f),
        make<FloatFusedMultiplyAdd>(FloatFusedMultiplyAdd::Operation::MultiplySubtract, RoundingMode::RNE, FloatWidth::Single, 10_f, 11_f, 12_f, 9_f),
        make<FloatFusedMultiplyAdd>(FloatFusedMultiplyAdd::Operation::NegatedMultiplySubtract, RoundingMode::RDN, FloatWidth::Single, 14_f, 15_f, 16_f, 13_f),
        make<FloatFusedMultiplyAdd>(FloatFusedMultiplyAdd::Operation::NegatedMultiplyAdd, RoundingMode::RUP, FloatWidth::Single, 18_f, 19_f, 20_f, 17_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Add, RoundingMode::RMM, FloatWidth::Single, 22_f, 23_f, 21_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Subtract, RoundingMode::DYN, FloatWidth::Single, 25_f, 26_f, 24_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Multiply, RoundingMode::DYN, FloatWidth::Single, 28_f, 29_f, 27_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Divide, RoundingMode::DYN, FloatWidth::Single, 31_f, 1_f, 30_f),
        make<FloatSquareRoot>(RoundingMode::DYN, FloatWidth::Single, 2_f, 1_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::SignInject, RoundingMode::DYN, FloatWidth::Single, 4_f, 5_f, 3_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::SignInjectNegate, RoundingMode::DYN, FloatWidth::Single, 7_f, 8_f, 6_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::SignInjectXor, RoundingMode::DYN, FloatWidth::Single, 10_f, 11_f, 9_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Min, RoundingMode::DYN, FloatWidth::Single, 13_f, 14_f, 12_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Max, RoundingMode::DYN, FloatWidth::Single, 16_f, 17_f, 15_f),
        make<ConvertFloatToInteger>(RoundingMode::DYN, MemoryAccessMode { DataWidth::Word, Signedness::Signed }, FloatWidth::Single, 19_f, 18_x),
        make<ConvertFloatToInteger>(RoundingMode::DYN, MemoryAccessMode { DataWidth::Word, Signedness::Unsigned }, FloatWidth::Single, 21_f, 20_x),
        make<MoveFloatToInteger>(FloatWidth::Single, 23_f, 22_x),
        make<FloatCompare>(FloatCompare::Operation::Equals, FloatWidth::Single, 24_f, 25_f, 23_x),
        make<FloatCompare>(FloatCompare::Operation::LessThan, FloatWidth::Single, 27_f, 28_f, 26_x),
        make<FloatCompare>(FloatCompare::Operation::LessThanEquals, FloatWidth::Single, 30_f, 31_f, 29_x),
        make<FloatClassify>(FloatWidth::Single, 0_f, 1_x),
        make<ConvertIntegerToFloat>(RoundingMode::DYN, MemoryAccessMode { DataWidth::Word, Signedness::Signed }, FloatWidth::Single, 2_x, 1_f),
        make<ConvertIntegerToFloat>(RoundingMode::DYN, MemoryAccessMode { DataWidth::Word, Signedness::Unsigned }, FloatWidth::Single, 4_x, 3_f),
        make<MoveIntegerToFloat>(FloatWidth::Single, 6_x, 5_f),
        // RV64F
        make<ConvertFloatToInteger>(RoundingMode::DYN, MemoryAccessMode { DataWidth::DoubleWord, Signedness::Signed }, FloatWidth::Single, 2_f, 1_x),
        make<ConvertFloatToInteger>(RoundingMode::DYN, MemoryAccessMode { DataWidth::DoubleWord, Signedness::Unsigned }, FloatWidth::Single, 4_f, 3_x),
        make<ConvertIntegerToFloat>(RoundingMode::DYN, MemoryAccessMode { DataWidth::DoubleWord, Signedness::Signed }, FloatWidth::Single, 6_x, 5_f),
        make<ConvertIntegerToFloat>(RoundingMode::DYN, MemoryAccessMode { DataWidth::DoubleWord, Signedness::Unsigned }, FloatWidth::Single, 8_x, 7_f),
        // RV32D

        make<FloatMemoryLoad>(4, 2_x, FloatWidth::Double, 1_f),
        make<FloatMemoryStore>(8, 3_f, 4_x, FloatWidth::Double),
        make<FloatFusedMultiplyAdd>(FloatFusedMultiplyAdd::Operation::MultiplyAdd, RoundingMode::DYN, FloatWidth::Double, 6_f, 7_f, 8_f, 5_f),
        make<FloatFusedMultiplyAdd>(FloatFusedMultiplyAdd::Operation::MultiplySubtract, RoundingMode::RNE, FloatWidth::Double, 10_f, 11_f, 12_f, 9_f),
        make<FloatFusedMultiplyAdd>(FloatFusedMultiplyAdd::Operation::NegatedMultiplySubtract, RoundingMode::RDN, FloatWidth::Double, 14_f, 15_f, 16_f, 13_f),
        make<FloatFusedMultiplyAdd>(FloatFusedMultiplyAdd::Operation::NegatedMultiplyAdd, RoundingMode::RUP, FloatWidth::Double, 18_f, 19_f, 20_f, 17_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Add, RoundingMode::RMM, FloatWidth::Double, 22_f, 23_f, 21_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Subtract, RoundingMode::DYN, FloatWidth::Double, 25_f, 26_f, 24_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Multiply, RoundingMode::DYN, FloatWidth::Double, 28_f, 29_f, 27_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Divide, RoundingMode::DYN, FloatWidth::Double, 31_f, 1_f, 30_f),
        make<FloatSquareRoot>(RoundingMode::DYN, FloatWidth::Double, 2_f, 1_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::SignInject, RoundingMode::DYN, FloatWidth::Double, 4_f, 5_f, 3_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::SignInjectNegate, RoundingMode::DYN, FloatWidth::Double, 7_f, 8_f, 6_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::SignInjectXor, RoundingMode::DYN, FloatWidth::Double, 10_f, 11_f, 9_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Min, RoundingMode::DYN, FloatWidth::Double, 13_f, 14_f, 12_f),
        make<FloatArithmeticInstruction>(FloatArithmeticInstruction::Operation::Max, RoundingMode::DYN, FloatWidth::Double, 16_f, 17_f, 15_f),
        make<ConvertFloat>(ConvertFloat::Operation::DoubleToSingle, RoundingMode::DYN, 19_f, 18_f),
        make<ConvertFloat>(ConvertFloat::Operation::SingleToDouble, RoundingMode::RNE, 21_f, 20_f),
        make<FloatCompare>(FloatCompare::Operation::Equals, FloatWidth::Double, 24_f, 25_f, 23_x),
        make<FloatCompare>(FloatCompare::Operation::LessThan, FloatWidth::Double, 27_f, 28_f, 26_x),
        make<FloatCompare>(FloatCompare::Operation::LessThanEquals, FloatWidth::Double, 30_f, 31_f, 29_x),
        make<FloatClassify>(FloatWidth::Double, 0_f, 1_x),
        make<ConvertFloatToInteger>(RoundingMode::DYN, MemoryAccessMode { DataWidth::Word, Signedness::Signed }, FloatWidth::Double, 3_f, 2_x),
        make<ConvertFloatToInteger>(RoundingMode::DYN, MemoryAccessMode { DataWidth::Word, Signedness::Unsigned }, FloatWidth::Double, 5_f, 4_x),
        make<ConvertIntegerToFloat>(RoundingMode::RNE, MemoryAccessMode { DataWidth::Word, Signedness::Signed }, FloatWidth::Double, 7_x, 6_f),
        make<ConvertIntegerToFloat>(RoundingMode::RNE, MemoryAccessMode { DataWidth::Word, Signedness::Unsigned }, FloatWidth::Double, 9_x, 8_f),
        // RV64D
        make<ConvertFloatToInteger>(RoundingMode::DYN, MemoryAccessMode { DataWidth::DoubleWord, Signedness::Signed }, FloatWidth::Double, 11_f, 10_x),
        make<ConvertFloatToInteger>(RoundingMode::DYN, MemoryAccessMode { DataWidth::DoubleWord, Signedness::Unsigned }, FloatWidth::Double, 12_f, 11_x),
        make<MoveFloatToInteger>(FloatWidth::Double, 14_f, 13_x),
        make<ConvertIntegerToFloat>(RoundingMode::DYN, MemoryAccessMode { DataWidth::DoubleWord, Signedness::Signed }, FloatWidth::Double, 16_x, 15_f),
        make<ConvertIntegerToFloat>(RoundingMode::DYN, MemoryAccessMode { DataWidth::DoubleWord, Signedness::Unsigned }, FloatWidth::Double, 18_x, 17_f),
        make<MoveIntegerToFloat>(FloatWidth::Double, 20_x, 19_f),
    };

    constexpr auto all_instructions_machine_code = to_array<u8>(
        { 0xb7, 0x50, 0xa0, 0x74, 0x17, 0x71, 0x6a, 0x00, 0xef, 0xf1, 0x9f, 0xff,
            0x67, 0x82, 0x22, 0x00, 0xe3, 0x08, 0x73, 0xfe, 0xe3, 0x16, 0x94, 0xfe,
            0xe3, 0x44, 0xb5, 0xfe, 0xe3, 0x52, 0xd6, 0xfe, 0xe3, 0x60, 0xf7, 0xfe,
            0xe3, 0x7e, 0x18, 0xfd, 0x03, 0x89, 0x19, 0x00, 0x03, 0x9a, 0x2a, 0x00,
            0x03, 0xab, 0x4b, 0x00, 0x03, 0xcc, 0x5c, 0x00, 0x03, 0xdd, 0x6d, 0x00,
            0xa3, 0x80, 0xce, 0x01, 0x23, 0x91, 0xef, 0x01, 0x23, 0x22, 0x11, 0x00,
            0x93, 0x01, 0x52, 0x00, 0x93, 0x22, 0x73, 0x00, 0x93, 0x33, 0x94, 0x00,
            0x93, 0x44, 0xb5, 0x00, 0x93, 0x65, 0xd6, 0x00, 0x93, 0x76, 0xf7, 0x00,
            0x93, 0x17, 0x18, 0x01, 0x93, 0x58, 0x39, 0x01, 0x93, 0x59, 0x5a, 0x41,
            0xb3, 0x01, 0x52, 0x00, 0x33, 0x8b, 0x8b, 0x41, 0xb3, 0x17, 0x18, 0x01,
            0xb3, 0x22, 0x73, 0x00, 0xb3, 0x33, 0x94, 0x00, 0xb3, 0x44, 0xb5, 0x00,
            0xb3, 0x58, 0x39, 0x01, 0xb3, 0x59, 0x5a, 0x41, 0xb3, 0x65, 0xd6, 0x00,
            0xb3, 0x76, 0xf7, 0x00, 0x0f, 0x00, 0x10, 0x0b, 0x0f, 0x00, 0x90, 0x04,
            0x0f, 0x00, 0x30, 0x83, 0x73, 0x00, 0x00, 0x00, 0x73, 0x00, 0x10, 0x00,
            0x83, 0x60, 0x41, 0x00, 0x83, 0x31, 0x82, 0x00, 0x23, 0x38, 0x53, 0x00,
            0x9b, 0x03, 0x74, 0xff, 0x9b, 0x14, 0xb5, 0x00, 0x9b, 0x55, 0xd6, 0x40,
            0xbb, 0x06, 0xf7, 0x00, 0x3b, 0x88, 0x28, 0x41, 0xbb, 0x19, 0x5a, 0x01,
            0x3b, 0xdb, 0x8b, 0x01, 0xbb, 0x5c, 0xbd, 0x41, 0x0f, 0x10, 0x00, 0x00,
            0xf3, 0x10, 0x31, 0x00, 0xf3, 0x21, 0x52, 0x00, 0xf3, 0x32, 0x73, 0x00,
            0xf3, 0xd3, 0x84, 0x00, 0x73, 0x64, 0x95, 0x00, 0xf3, 0xf4, 0xa5, 0x00,
            0xb3, 0x00, 0x31, 0x02, 0x33, 0x92, 0x62, 0x02, 0xb3, 0x23, 0x94, 0x02,
            0x33, 0xb5, 0xc5, 0x02, 0xb3, 0x46, 0xf7, 0x02, 0x33, 0xd8, 0x28, 0x03,
            0xb3, 0x69, 0x5a, 0x03, 0x33, 0xfb, 0x8b, 0x03, 0xbb, 0x00, 0x31, 0x02,
            0xbb, 0x46, 0xf7, 0x02, 0x3b, 0xd8, 0x28, 0x03, 0xbb, 0x69, 0x5a, 0x03,
            0x3b, 0xfb, 0x8b, 0x03, 0xaf, 0x20, 0x01, 0x14, 0xaf, 0xa1, 0x42, 0x1a,
            0x2f, 0x23, 0x74, 0x08, 0xaf, 0xa4, 0xa5, 0x00, 0x2f, 0x26, 0xd7, 0x20,
            0xaf, 0xa7, 0x08, 0x61, 0x2f, 0x29, 0x3a, 0x41, 0xaf, 0xaa, 0x6b, 0x81,
            0x2f, 0x2c, 0x9d, 0xa1, 0xaf, 0xad, 0xce, 0xc1, 0x2f, 0xaf, 0xf0, 0xe1,
            0xaf, 0x30, 0x01, 0x12, 0xaf, 0xb1, 0x42, 0x18, 0x2f, 0x33, 0x74, 0x08,
            0xaf, 0xb4, 0xa5, 0x00, 0x2f, 0x36, 0xd7, 0x20, 0xaf, 0xb7, 0x08, 0x61,
            0x2f, 0x39, 0x3a, 0x41, 0xaf, 0xba, 0x6b, 0x81, 0x2f, 0x3c, 0x9d, 0xa1,
            0xaf, 0xbd, 0xce, 0xc1, 0x2f, 0xbf, 0xf0, 0xe1, 0x87, 0x20, 0x41, 0x00,
            0x27, 0x24, 0x32, 0x00, 0xc3, 0x72, 0x73, 0x40, 0xc7, 0x04, 0xb5, 0x60,
            0xcb, 0x26, 0xf7, 0x80, 0xcf, 0x38, 0x39, 0xa1, 0xd3, 0x4a, 0x7b, 0x01,
            0x53, 0xfc, 0xac, 0x09, 0xd3, 0x7d, 0xde, 0x11, 0x53, 0xff, 0x1f, 0x18,
            0xd3, 0x70, 0x01, 0x58, 0xd3, 0x01, 0x52, 0x20, 0x53, 0x93, 0x83, 0x20,
            0xd3, 0x24, 0xb5, 0x20, 0x53, 0x86, 0xe6, 0x28, 0xd3, 0x17, 0x18, 0x29,
            0x53, 0xf9, 0x09, 0xc0, 0x53, 0xfa, 0x1a, 0xc0, 0x53, 0x8b, 0x0b, 0xe0,
            0xd3, 0x2b, 0x9c, 0xa1, 0x53, 0x9d, 0xcd, 0xa1, 0xd3, 0x0e, 0xff, 0xa1,
            0xd3, 0x10, 0x00, 0xe0, 0xd3, 0x70, 0x01, 0xd0, 0xd3, 0x71, 0x12, 0xd0,
            0xd3, 0x02, 0x03, 0xf0, 0xd3, 0x70, 0x21, 0xc0, 0xd3, 0x71, 0x32, 0xc0,
            0xd3, 0x72, 0x23, 0xd0, 0xd3, 0x73, 0x34, 0xd0, 0x87, 0x30, 0x41, 0x00,
            0x27, 0x34, 0x32, 0x00, 0xc3, 0x72, 0x73, 0x42, 0xc7, 0x04, 0xb5, 0x62,
            0xcb, 0x26, 0xf7, 0x82, 0xcf, 0x38, 0x39, 0xa3, 0xd3, 0x4a, 0x7b, 0x03,
            0x53, 0xfc, 0xac, 0x0b, 0xd3, 0x7d, 0xde, 0x13, 0x53, 0xff, 0x1f, 0x1a,
            0xd3, 0x70, 0x01, 0x5a, 0xd3, 0x01, 0x52, 0x22, 0x53, 0x93, 0x83, 0x22,
            0xd3, 0x24, 0xb5, 0x22, 0x53, 0x86, 0xe6, 0x2a, 0xd3, 0x17, 0x18, 0x2b,
            0x53, 0xf9, 0x19, 0x40, 0x53, 0x8a, 0x0a, 0x42, 0xd3, 0x2b, 0x9c, 0xa3,
            0x53, 0x9d, 0xcd, 0xa3, 0xd3, 0x0e, 0xff, 0xa3, 0xd3, 0x10, 0x00, 0xe2,
            0x53, 0xf1, 0x01, 0xc2, 0x53, 0xf2, 0x12, 0xc2, 0x53, 0x83, 0x03, 0xd2,
            0x53, 0x84, 0x14, 0xd2, 0x53, 0xf5, 0x25, 0xc2, 0xd3, 0x75, 0x36, 0xc2,
            0xd3, 0x06, 0x07, 0xe2, 0xd3, 0x77, 0x28, 0xd2, 0xd3, 0x78, 0x39, 0xd2,
            0xd3, 0x09, 0x0a, 0xf2 });

    check_disassembly(all_instructions.span(), all_instructions_machine_code.span(), 0);
}
