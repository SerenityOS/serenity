/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/Prekernel/Arch/aarch64/Prekernel.h>

#include <Kernel/Arch/aarch64/Aarch64Asm.h>
#include <Kernel/Arch/aarch64/Registers.h>
#include <Kernel/Prekernel/Arch/aarch64/UART.h>

// Documentation here for Aarch64 Address Translations
// https://documentation-service.arm.com/static/5efa1d23dbdee951c1ccdec5?token=

using namespace Kernel;

// These come from the linker script
extern u8 page_tables_phys_start[];
extern u8 page_tables_phys_end[];

namespace Prekernel {

// physical memory
constexpr u32 START_OF_NORMAL_MEMORY = 0x00000000;
constexpr u32 END_OF_NORMAL_MEMORY = 0x3EFFFFFF;
constexpr u32 START_OF_DEVICE_MEMORY = 0x3F000000;
constexpr u32 END_OF_DEVICE_MEMORY = 0x3FFFFFFF;

// 4KiB page size was chosen for the prekernel to make this code slightly simpler
constexpr u32 GRANULE_SIZE = 0x1000;
constexpr u32 PAGE_TABLE_SIZE = 0x1000;

// Documentation for translation table format
// https://developer.arm.com/documentation/101811/0101/Controlling-address-translation
constexpr u32 PAGE_DESCRIPTOR = 0b11;
constexpr u32 TABLE_DESCRIPTOR = 0b11;

constexpr u32 ACCESS_FLAG = 1 << 10;

// shareability
constexpr u32 OUTER_SHAREABLE = (2 << 8);
constexpr u32 INNER_SHAREABLE = (3 << 8);

// these index into the MAIR attribute table
constexpr u32 NORMAL_MEMORY = (0 << 2);
constexpr u32 DEVICE_MEMORY = (1 << 2);

using page_table_t = u8*;

static void zero_identity_map(page_table_t page_table_start, page_table_t page_table_end)
{
    // Memset all page table memory to zero
    for (uint64_t* p = (uint64_t*)page_table_start;
         p < (uint64_t*)page_table_end;
         p++) {

        *p = 0;
    }
}

static void build_identity_map(page_table_t page_table)
{
    // Set up first entry of level 1
    uint64_t* level1_entry = (uint64_t*)page_table;
    *level1_entry = (uint64_t)&page_table[PAGE_TABLE_SIZE];
    *level1_entry |= TABLE_DESCRIPTOR;

    // Set up first entry of level 2
    uint64_t* level2_entry = (uint64_t*)&page_table[PAGE_TABLE_SIZE];
    *level2_entry = (uint64_t)&page_table[PAGE_TABLE_SIZE * 2];
    *level2_entry |= TABLE_DESCRIPTOR;

    // Set up L3 entries
    for (uint32_t l3_idx = 0; l3_idx < 512; l3_idx++) {
        uint64_t* l3_entry = (uint64_t*)&page_table[PAGE_TABLE_SIZE * 2 + (l3_idx * sizeof(uint64_t))];

        *l3_entry = (uint64_t)(page_table + (PAGE_TABLE_SIZE * 3) + (l3_idx * PAGE_TABLE_SIZE));
        *l3_entry |= TABLE_DESCRIPTOR;
    }

    // Set up L4 entries
    size_t page_index = 0;

    for (size_t addr = START_OF_NORMAL_MEMORY; addr < END_OF_NORMAL_MEMORY; addr += GRANULE_SIZE, page_index++) {
        uint64_t* l4_entry = (uint64_t*)&page_table[PAGE_TABLE_SIZE * 3 + (page_index * sizeof(uint64_t))];

        *l4_entry = addr;
        *l4_entry |= ACCESS_FLAG;
        *l4_entry |= PAGE_DESCRIPTOR;
        *l4_entry |= INNER_SHAREABLE;
        *l4_entry |= NORMAL_MEMORY;
    }

    // Set up entries for last 16MB of memory (MMIO)
    for (size_t addr = START_OF_DEVICE_MEMORY; addr < END_OF_DEVICE_MEMORY; addr += GRANULE_SIZE, page_index++) {
        uint64_t* l4_entry = (uint64_t*)&page_table[PAGE_TABLE_SIZE * 3 + (page_index * sizeof(uint64_t))];

        *l4_entry = addr;
        *l4_entry |= ACCESS_FLAG;
        *l4_entry |= PAGE_DESCRIPTOR;
        *l4_entry |= OUTER_SHAREABLE;
        *l4_entry |= DEVICE_MEMORY;
    }
}

static void switch_to_page_table(u8* page_table)
{
    set_ttbr0_el1((FlatPtr)page_table);
    set_ttbr1_el1((FlatPtr)page_table);
}

static void activate_mmu()
{
    Aarch64::MAIR_EL1 mair_el1 = {};
    mair_el1.Attr[0] = 0xFF;       // Normal memory
    mair_el1.Attr[1] = 0b00000100; // Device-nGnRE memory (non-cacheble)
    Aarch64::MAIR_EL1::write(mair_el1);

    // Configure cacheability attributes for memory associated with translation table walks
    Aarch64::TCR_EL1 tcr_el1 = {};

    tcr_el1.SH1 = Aarch64::TCR_EL1::InnerShareable;
    tcr_el1.ORGN1 = Aarch64::TCR_EL1::NormalMemory_Outer_WriteBack_ReadAllocate_WriteAllocateCacheable;
    tcr_el1.IRGN1 = Aarch64::TCR_EL1::NormalMemory_Inner_WriteBack_ReadAllocate_WriteAllocateCacheable;

    tcr_el1.SH0 = Aarch64::TCR_EL1::InnerShareable;
    tcr_el1.ORGN0 = Aarch64::TCR_EL1::NormalMemory_Outer_WriteBack_ReadAllocate_WriteAllocateCacheable;
    tcr_el1.IRGN0 = Aarch64::TCR_EL1::NormalMemory_Inner_WriteBack_ReadAllocate_WriteAllocateCacheable;

    tcr_el1.TG1 = Aarch64::TCR_EL1::TG1GranuleSize::Size_4KB;
    tcr_el1.TG0 = Aarch64::TCR_EL1::TG0GranuleSize::Size_4KB;

    // Auto detect the Intermediate Physical Address Size
    Aarch64::ID_AA64MMFR0_EL1 feature_register = Aarch64::ID_AA64MMFR0_EL1::read();
    tcr_el1.IPS = feature_register.PARange;

    Aarch64::TCR_EL1::write(tcr_el1);

    // Enable MMU in the system control register
    Aarch64::SCTLR_EL1 sctlr_el1 = Aarch64::SCTLR_EL1::read();
    sctlr_el1.M = 1; //Enable MMU
    Aarch64::SCTLR_EL1::write(sctlr_el1);

    flush();
}

void init_prekernel_page_tables()
{
    zero_identity_map(page_tables_phys_start, page_tables_phys_end);
    build_identity_map(page_tables_phys_start);
    switch_to_page_table(page_tables_phys_start);
    activate_mmu();
}

}
