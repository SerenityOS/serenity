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

    auto operation = AtomicMemoryOperation::Operation::Swap;
    switch (raw_parts.funct7 >> 2) {
    case 0b00010:
    case 0b00011: {
        auto is_sc = (raw_parts.funct7 & (1 << 2)) > 0;
        return adopt_own(*new (nothrow) LoadReserveStoreConditional(is_sc ? LoadReserveStoreConditional::Operation::StoreConditional : LoadReserveStoreConditional::Operation::LoadReserve, is_acquire, is_release, width, raw_parts.rs1, raw_parts.rs2, raw_parts.rd));
    }
    case 0b00001:
        operation = AtomicMemoryOperation::Operation::Swap;
        break;
    case 0b00000:
        operation = AtomicMemoryOperation::Operation::Add;
        break;
    case 0b00100:
        operation = AtomicMemoryOperation::Operation::Xor;
        break;
    case 0b01000:
        operation = AtomicMemoryOperation::Operation::Or;
        break;
    case 0b01100:
        operation = AtomicMemoryOperation::Operation::And;
        break;
    case 0b10000:
        operation = AtomicMemoryOperation::Operation::Min;
        break;
    case 0b10100:
        operation = AtomicMemoryOperation::Operation::Max;
        break;
    case 0b11000:
        operation = AtomicMemoryOperation::Operation::MinUnsigned;
        break;
    case 0b11100:
        operation = AtomicMemoryOperation::Operation::MaxUnsigned;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return adopt_own(*new (nothrow) AtomicMemoryOperation(operation, is_acquire, is_release, width, raw_parts.rs1, raw_parts.rs2, raw_parts.rd));
}

}
