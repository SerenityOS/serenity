/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "A.h"

namespace Disassembly::RISCV64 {

NonnullOwnPtr<InstructionImpl> parse_amo(u32 instruction)
{
    auto raw_parts = RawRType::parse(instruction);
    auto is_acquire = (raw_parts.funct7 & 0b10) > 0;
    auto is_release = (raw_parts.funct7 & 1) > 0;
    auto width = MemoryAccessMode::from_funct3(raw_parts.funct3).width;

    auto numeric_operation = raw_parts.funct7 >> 2;
    auto operation = static_cast<AtomicMemoryOperation::Operation>(numeric_operation);
    switch (numeric_operation) {
    case 0b00010:
    case 0b00011: {
        auto is_sc = (raw_parts.funct7 & 0b100) > 0;
        return adopt_own(*new (nothrow) LoadReservedStoreConditional(is_sc ? LoadReservedStoreConditional::Operation::StoreConditional : LoadReservedStoreConditional::Operation::LoadReserved, is_acquire, is_release, width, raw_parts.rs1, raw_parts.rs2, raw_parts.rd));
    }
    case 0b00001:
    case 0b00000:
    case 0b00100:
    case 0b01000:
    case 0b01100:
    case 0b10000:
    case 0b10100:
    case 0b11000:
    case 0b11100:
        return adopt_own(*new (nothrow) AtomicMemoryOperation(operation, is_acquire, is_release, width, raw_parts.rs1, raw_parts.rs2, raw_parts.rd));
    default:
        return adopt_own(*new (nothrow) UnknownInstruction);
    }
}

}
