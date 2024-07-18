/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

namespace Kernel {

// Documentation for RISC-V Virtual Memory:
// The RISC-V Instruction Set Manual, Volume II: Privileged Architecture
// https://github.com/riscv/riscv-isa-manual/releases/download/Priv-v1.12/riscv-privileged-20211203.pdf

// Currently, only the Sv39 (3 level paging) virtual memory system is implemented

// Figure 4.19-4.21
constexpr size_t PAGE_TABLE_SHIFT = 12;
constexpr size_t PAGE_TABLE_SIZE = 1LU << PAGE_TABLE_SHIFT;

constexpr size_t PADDR_PPN_OFFSET = PAGE_TABLE_SHIFT;
constexpr size_t VADDR_VPN_OFFSET = PAGE_TABLE_SHIFT;
constexpr size_t PTE_PPN_OFFSET = 10;

constexpr size_t PPN_SIZE = 26 + 9 + 9;
constexpr size_t VPN_SIZE = 9 + 9 + 9;

constexpr size_t VPN_2_OFFSET = 30;
constexpr size_t VPN_1_OFFSET = 21;
constexpr size_t VPN_0_OFFSET = 12;

constexpr size_t PPN_MASK = (1LU << PPN_SIZE) - 1;
constexpr size_t PTE_PPN_MASK = PPN_MASK << PTE_PPN_OFFSET;

constexpr size_t PAGE_TABLE_INDEX_BITS = 9;
constexpr size_t PAGE_TABLE_INDEX_MASK = (1 << PAGE_TABLE_INDEX_BITS) - 1;

constexpr size_t PAGE_OFFSET_BITS = 12;

constexpr size_t PAGE_TABLE_LEVEL_COUNT = 3;

enum class PageTableEntryBits {
    Valid = 1 << 0,
    Readable = 1 << 1,
    Writeable = 1 << 2,
    Executable = 1 << 3,
    UserAllowed = 1 << 4,
    Global = 1 << 5,
    Accessed = 1 << 6,
    Dirty = 1 << 7,
};
AK_ENUM_BITWISE_OPERATORS(PageTableEntryBits);

}
