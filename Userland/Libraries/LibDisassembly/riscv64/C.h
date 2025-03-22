/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "FD.h"
#include "IM.h"
#include "Instruction.h"

// C extension.
namespace Disassembly::RISCV64 {

NonnullOwnPtr<MemoryLoad> parse_c_lw(u16 instruction);
NonnullOwnPtr<MemoryStore> parse_c_sw(u16 instruction);
NonnullOwnPtr<MemoryLoad> parse_c_ldsp(u16 instruction);
NonnullOwnPtr<FloatMemoryLoad> parse_c_fldsp(u16 instruction);
NonnullOwnPtr<MemoryStore> parse_c_sdsp(u16 instruction);
NonnullOwnPtr<FloatMemoryStore> parse_c_fsdsp(u16 instruction);
NonnullOwnPtr<MemoryLoad> parse_c_lwsp(u16 instruction);
NonnullOwnPtr<MemoryStore> parse_c_swsp(u16 instruction);
NonnullOwnPtr<InstructionImpl> parse_c_lui_or_addi16sp(u16 instruction);
NonnullOwnPtr<ArithmeticImmediateInstruction> parse_c_addi(u16 instruction);
NonnullOwnPtr<ArithmeticImmediateInstruction> parse_c_addi4spn(u16 instruction);
NonnullOwnPtr<ArithmeticImmediateInstruction> parse_c_alu_imm(u16 instruction);
NonnullOwnPtr<Branch> parse_c_beqz(u16 instruction);
NonnullOwnPtr<Branch> parse_c_bnez(u16 instruction);
NonnullOwnPtr<InstructionImpl> parse_c_alu(u16 instruction);
NonnullOwnPtr<ArithmeticImmediateInstruction> parse_c_slli(u16 instruction);
NonnullOwnPtr<ArithmeticImmediateInstruction> parse_c_li(u16 instruction);
NonnullOwnPtr<JumpAndLink> parse_c_j(u16 instruction);
NonnullOwnPtr<MemoryLoad> parse_c_ld(u16 instruction);
NonnullOwnPtr<FloatMemoryLoad> parse_c_fld(u16 instruction);
NonnullOwnPtr<MemoryStore> parse_c_sd(u16 instruction);
NonnullOwnPtr<FloatMemoryStore> parse_c_fsd(u16 instruction);
NonnullOwnPtr<InstructionImpl> parse_c_jalr_mv_add(u16 instruction);
NonnullOwnPtr<InstructionImpl> parse_c_addiw(u16 instruction);

}
