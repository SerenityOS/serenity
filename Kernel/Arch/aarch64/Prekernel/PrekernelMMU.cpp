/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/Arch/aarch64/Prekernel/Prekernel.h>

#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Arch/aarch64/RPi/UART.h>
#include <Kernel/Arch/aarch64/Registers.h>

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

// 4KiB page size was chosen for the prekernel to make this code slightly simpler
constexpr u32 GRANULE_SIZE = 0x1000;
constexpr u32 PAGE_TABLE_SIZE = 0x1000;

// Documentation for translation table format
// https://developer.arm.com/documentation/101811/0101/Controlling-address-translation
constexpr u32 PAGE_DESCRIPTOR = 0b11;
constexpr u32 TABLE_DESCRIPTOR = 0b11;
constexpr u32 DESCRIPTOR_MASK = ~0b11;

constexpr u32 ACCESS_FLAG = 1 << 10;

// shareability
constexpr u32 OUTER_SHAREABLE = (2 << 8);
constexpr u32 INNER_SHAREABLE = (3 << 8);

// these index into the MAIR attribute table
constexpr u32 NORMAL_MEMORY = (0 << 2);
constexpr u32 DEVICE_MEMORY = (1 << 2);

ALWAYS_INLINE static u64* descriptor_to_pointer(FlatPtr descriptor)
{
    return (u64*)(descriptor & DESCRIPTOR_MASK);
}

namespace {
class PageBumpAllocator {
public:
    PageBumpAllocator(u64* start, u64* end)
        : m_start(start)
        , m_end(end)
        , m_current(start)
    {
        if (m_start >= m_end) {
            Prekernel::panic("Invalid memory range passed to PageBumpAllocator");
        }
        if ((FlatPtr)m_start % PAGE_TABLE_SIZE != 0 || (FlatPtr)m_end % PAGE_TABLE_SIZE != 0) {
            Prekernel::panic("Memory range passed into PageBumpAllocator not aligned to PAGE_TABLE_SIZE");
        }
    }

    u64* take_page()
    {
        if (m_current == m_end) {
            Prekernel::panic("Prekernel pagetable memory exhausted");
        }

        u64* page = m_current;
        m_current += (PAGE_TABLE_SIZE / sizeof(FlatPtr));

        zero_page(page);
        return page;
    }

private:
    void zero_page(u64* page)
    {
        // Memset all page table memory to zero
        for (u64* p = page; p < page + (PAGE_TABLE_SIZE / sizeof(u64)); p++) {
            *p = 0;
        }
    }

    u64 const* m_start;
    u64 const* m_end;
    u64* m_current;
};
}

static void insert_identity_entries_for_physical_memory_range(PageBumpAllocator& allocator, u64* page_table, FlatPtr start, FlatPtr end, u64 flags)
{
    // Not very efficient, but simple and it works.
    for (FlatPtr addr = start; addr < end; addr += GRANULE_SIZE) {
        // Each level has 9 bits (512 entries)
        u64 level0_idx = (addr >> 39) & 0x1FF;
        u64 level1_idx = (addr >> 30) & 0x1FF;
        u64 level2_idx = (addr >> 21) & 0x1FF;
        u64 level3_idx = (addr >> 12) & 0x1FF;

        u64* level1_table = page_table;

        if (level1_table[level0_idx] == 0) {
            level1_table[level0_idx] = (FlatPtr)allocator.take_page();
            level1_table[level0_idx] |= TABLE_DESCRIPTOR;
        }

        u64* level2_table = descriptor_to_pointer(level1_table[level0_idx]);

        if (level2_table[level1_idx] == 0) {
            level2_table[level1_idx] = (FlatPtr)allocator.take_page();
            level2_table[level1_idx] |= TABLE_DESCRIPTOR;
        }

        u64* level3_table = descriptor_to_pointer(level2_table[level1_idx]);

        if (level3_table[level2_idx] == 0) {
            level3_table[level2_idx] = (FlatPtr)allocator.take_page();
            level3_table[level2_idx] |= TABLE_DESCRIPTOR;
        }

        u64* level4_table = descriptor_to_pointer(level3_table[level2_idx]);
        u64* l4_entry = &level4_table[level3_idx];
        *l4_entry = addr;
        *l4_entry |= flags;
    }
}

static void build_identity_map(PageBumpAllocator& allocator)
{
    u64* level1_table = allocator.take_page();

    u64 normal_memory_flags = ACCESS_FLAG | PAGE_DESCRIPTOR | INNER_SHAREABLE | NORMAL_MEMORY;
    u64 device_memory_flags = ACCESS_FLAG | PAGE_DESCRIPTOR | OUTER_SHAREABLE | DEVICE_MEMORY;

    insert_identity_entries_for_physical_memory_range(allocator, level1_table, START_OF_NORMAL_MEMORY, END_OF_NORMAL_MEMORY, normal_memory_flags);
    insert_identity_entries_for_physical_memory_range(allocator, level1_table, MMIO::the().peripheral_base_address(), MMIO::the().peripheral_end_address(), device_memory_flags);
}

static void switch_to_page_table(u8* page_table)
{
    Aarch64::Asm::set_ttbr0_el1((FlatPtr)page_table);
    Aarch64::Asm::set_ttbr1_el1((FlatPtr)page_table);
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
    sctlr_el1.M = 1; // Enable MMU
    Aarch64::SCTLR_EL1::write(sctlr_el1);

    Aarch64::Asm::flush();
}

void init_prekernel_page_tables()
{
    PageBumpAllocator allocator((u64*)page_tables_phys_start, (u64*)page_tables_phys_end);
    build_identity_map(allocator);
    switch_to_page_table(page_tables_phys_start);
    activate_mmu();
}

}
