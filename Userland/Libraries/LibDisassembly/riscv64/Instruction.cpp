/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Instruction.h"
#include "A.h"
#include "Encoding.h"
#include "FD.h"
#include "IM.h"
#include "Priviledged.h"
#include "Zicsr.h"
#include <AK/Assertions.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/TypeCasts.h>
#include <LibDisassembly/InstructionStream.h>

namespace Disassembly::RISCV64 {

MemoryAccessMode MemoryAccessMode::from_funct3(u8 funct3)
{
    auto width = static_cast<DataWidth>(funct3 & 0b11);
    bool is_signed = (funct3 & 0b100) == 0;
    return { width, is_signed ? Signedness::Signed : Signedness::Unsigned };
}

static NonnullOwnPtr<InstructionImpl> parse_full_impl(MajorOpcode opcode, u32 instruction)
{
    switch (opcode) {
    case MajorOpcode::JAL:
        return parse_jal(instruction);
    case MajorOpcode::AUIPC:
        return parse_auipc(instruction);
    case MajorOpcode::LUI:
        return parse_lui(instruction);
    case MajorOpcode::JALR:
        return parse_jalr(instruction);
    case MajorOpcode::OP_IMM:
        return parse_op_imm(instruction);
    case MajorOpcode::OP:
        return parse_op(instruction);
    case MajorOpcode::LOAD:
        return parse_load(instruction);
    case MajorOpcode::STORE:
        return parse_store(instruction);
    case MajorOpcode::BRANCH:
        return parse_branch(instruction);
    case MajorOpcode::OP_IMM_32:
        return parse_op_imm_32(instruction);
    case MajorOpcode::OP_32:
        return parse_op_32(instruction);
    case MajorOpcode::LOAD_FP:
        return parse_load_fp(instruction);
    case MajorOpcode::STORE_FP:
        return parse_store_fp(instruction);
    case MajorOpcode::OP_FP:
        return parse_op_fp(instruction);
    case MajorOpcode::SYSTEM:
        return parse_system(instruction);
    case MajorOpcode::MADD:
    case MajorOpcode::MSUB:
    case MajorOpcode::NMSUB:
    case MajorOpcode::NMADD:
        return parse_fma(instruction);
    case MajorOpcode::MISC_MEM:
        return parse_misc_mem(instruction);
    case MajorOpcode::AMO:
        return parse_amo(instruction);

    case MajorOpcode::custom_0:
    case MajorOpcode::custom_1:
    case MajorOpcode::reserved_0:
    case MajorOpcode::reserved_1:
    case MajorOpcode::reserved_2:
    case MajorOpcode::custom_2_rv128:
    case MajorOpcode::custom_3_rv128:
        return make<UnknownInstruction>();
    }
    VERIFY_NOT_REACHED();
}

static NonnullOwnPtr<InstructionImpl> parse_compressed_impl(CompressedOpcode opcode, u16 instruction)
{
    (void)opcode;
    (void)instruction;
    return make<UnknownInstruction>();
}

NonnullOwnPtr<Instruction> Instruction::parse_full(u32 instruction)
{
    auto opcode = static_cast<MajorOpcode>(instruction & 0b11111'11);
    auto instruction_data = parse_full_impl(opcode, instruction);
    return adopt_own(*new (nothrow) Instruction(move(instruction_data), instruction));
}

NonnullOwnPtr<Instruction> Instruction::parse_compressed(u16 instruction)
{
    auto opcode = extract_compressed_opcode(instruction);
    auto instruction_data = parse_compressed_impl(opcode, instruction);
    return adopt_own(*new (nothrow) Instruction(move(instruction_data), instruction, CompressedTag {}));
}

NonnullOwnPtr<Instruction> Instruction::from_stream(InstructionStream& stream)
{
    u16 first_halfword = AK::convert_between_host_and_little_endian(stream.read16());
    if (is_compressed_instruction(first_halfword))
        return Instruction::parse_compressed(first_halfword);

    u16 second_halfword = AK::convert_between_host_and_little_endian(stream.read16());
    return Instruction::parse_full(first_halfword | (second_halfword << 16));
}

ByteString Instruction::to_byte_string(u32 origin, Optional<SymbolProvider const&> symbol_provider) const
{
    return m_data->to_string(m_display_style, origin, symbol_provider).to_byte_string();
}

ByteString Instruction::mnemonic() const
{
    return m_data->mnemonic().to_byte_string();
}

template<typename InstructionType>
bool simple_instruction_equals(InstructionType const& self, InstructionImpl const& instruction)
{
    if (is<InstructionType>(instruction))
        return self == static_cast<InstructionType const&>(instruction);
    return false;
}

#define MAKE_INSTRUCTION_EQUALS(InstructionType) \
    bool InstructionType::instruction_equals(InstructionImpl const& instruction) const { return simple_instruction_equals<InstructionType>(*this, instruction); }

#define ENUMERATE_INSTRUCTION_IMPLS(M)   \
    M(UnknownInstruction)                \
    M(JumpAndLink)                       \
    M(JumpAndLinkRegister)               \
    M(LoadUpperImmediate)                \
    M(AddUpperImmediateToProgramCounter) \
    M(ArithmeticImmediateInstruction)    \
    M(ArithmeticInstruction)             \
    M(MemoryLoad)                        \
    M(MemoryStore)                       \
    M(Branch)                            \
    M(FloatArithmeticInstruction)        \
    M(FloatSquareRoot)                   \
    M(FloatFusedMultiplyAdd)             \
    M(ConvertFloatAndInteger)            \
    M(ConvertFloatToInteger)             \
    M(ConvertIntegerToFloat)             \
    M(ConvertFloat)                      \
    M(MoveFloatToInteger)                \
    M(MoveIntegerToFloat)                \
    M(FloatCompare)                      \
    M(FloatClassify)                     \
    M(FloatMemoryInstruction)            \
    M(FloatMemoryLoad)                   \
    M(FloatMemoryStore)                  \
    M(EnvironmentCall)                   \
    M(EnvironmentBreak)                  \
    M(MachineModeTrapReturn)             \
    M(SupervisorModeTrapReturn)          \
    M(WaitForInterrupt)                  \
    M(CSRInstruction)                    \
    M(CSRRegisterInstruction)            \
    M(CSRImmediateInstruction)           \
    M(Fence)                             \
    M(InstructionFetchFence)             \
    M(AtomicMemoryOperation)             \
    M(LoadReservedStoreConditional)

ENUMERATE_INSTRUCTION_IMPLS(MAKE_INSTRUCTION_EQUALS)

}
