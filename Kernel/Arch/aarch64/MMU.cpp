/*
 * Copyright (c) 2021, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/Arch/aarch64/CPU.h>

#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Arch/aarch64/Registers.h>
#include <Kernel/Boot/BootInfo.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Sections.h>
#include <LibDeviceTree/FlattenedDeviceTree.h>

// Documentation here for Aarch64 Address Translations
// https://documentation-service.arm.com/static/5efa1d23dbdee951c1ccdec5?token=

// These come from the linker script
extern u8 page_tables_phys_start[];
extern u8 page_tables_phys_end[];
extern u8 start_of_kernel_image[];
extern u8 end_of_kernel_image[];

namespace Kernel::Memory {

// physical memory
constexpr u32 START_OF_NORMAL_MEMORY = 0x00000000;
constexpr u32 END_OF_NORMAL_MEMORY = 0x3EFFFFFF;

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
            panic_without_mmu("Invalid memory range passed to PageBumpAllocator"sv);
        }
        if ((FlatPtr)m_start % PAGE_TABLE_SIZE != 0 || (FlatPtr)m_end % PAGE_TABLE_SIZE != 0) {
            panic_without_mmu("Memory range passed into PageBumpAllocator not aligned to PAGE_TABLE_SIZE"sv);
        }
    }

    u64* take_page()
    {
        if (m_current == m_end) {
            panic_without_mmu("Prekernel pagetable memory exhausted"sv);
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

static UNMAP_AFTER_INIT FlatPtr calculate_physical_to_link_time_address_offset()
{
    FlatPtr physical_address;

    asm volatile(
        "adrp %[physical_address], start_of_kernel_image"
        : [physical_address] "=r"(physical_address));

    return KERNEL_MAPPING_BASE - physical_address;
}

// NOTE: To access global variables while the MMU is not yet enabled, we need
//       to convert the address of a global variable to a physical address by
//       subtracting calculate_physical_to_link_time_address_offset(). This is because the kernel is linked
//       for virtual memory at KERNEL_MAPPING_BASE, so a regular access to global variables
//       will use the high virtual memory address. This does not work when the MMU is not yet
//       enabled, so this function must be used for accessing global variables.
template<typename T>
inline T* adjust_by_mapping_base(T* ptr)
{
    return (T*)((FlatPtr)ptr - calculate_physical_to_link_time_address_offset());
}

static u64* insert_page_table(PageBumpAllocator& allocator, u64* page_table, VirtualAddress virtual_addr)
{
    // Each level has 9 bits (512 entries)
    u64 level0_idx = (virtual_addr.get() >> 39) & 0x1FF;
    u64 level1_idx = (virtual_addr.get() >> 30) & 0x1FF;
    u64 level2_idx = (virtual_addr.get() >> 21) & 0x1FF;

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

    return descriptor_to_pointer(level3_table[level2_idx]);
}

static void insert_entries_for_memory_range(PageBumpAllocator& allocator, u64* page_table, VirtualAddress start, VirtualAddress end, PhysicalAddress paddr, u64 flags)
{
    // Not very efficient, but simple and it works.
    for (VirtualAddress addr = start; addr < end;) {
        u64* level4_table = insert_page_table(allocator, page_table, addr);

        u64 level3_idx = (addr.get() >> 12) & 0x1FF;
        u64* l4_entry = &level4_table[level3_idx];
        *l4_entry = paddr.get();
        *l4_entry |= flags;

        addr = addr.offset(GRANULE_SIZE);
        paddr = paddr.offset(GRANULE_SIZE);
    }
}

static void setup_quickmap_page_table(PageBumpAllocator& allocator, u64* root_table)
{
    // FIXME: Rename boot_pd_kernel_pt1023 to quickmap_page_table
    // FIXME: Rename KERNEL_PT1024_BASE to quickmap_page_table_address
    auto kernel_pt1024_base = VirtualAddress(*adjust_by_mapping_base(&g_boot_info.kernel_mapping_base) + KERNEL_PT1024_OFFSET);

    auto quickmap_page_table = PhysicalAddress((PhysicalPtr)insert_page_table(allocator, root_table, kernel_pt1024_base));
    *adjust_by_mapping_base(&g_boot_info.boot_pd_kernel_pt1023) = (PageTableEntry*)quickmap_page_table.offset(calculate_physical_to_link_time_address_offset()).get();
}

static void build_mappings(PageBumpAllocator& allocator, u64* root_table)
{
    u64 normal_memory_flags = ACCESS_FLAG | PAGE_DESCRIPTOR | INNER_SHAREABLE | NORMAL_MEMORY;

    // Align the identity mapping of the kernel image to 2 MiB, the rest of the memory is initially not mapped.
    auto start_of_kernel_range = VirtualAddress((FlatPtr)start_of_kernel_image & ~(FlatPtr)0x1fffff);
    auto end_of_kernel_range = VirtualAddress(((FlatPtr)end_of_kernel_image & ~(FlatPtr)0x1fffff) + 0x200000 - 1);

    auto start_of_physical_kernel_range = PhysicalAddress(start_of_kernel_range.get()).offset(-calculate_physical_to_link_time_address_offset());

    // Insert identity mapping
    insert_entries_for_memory_range(allocator, root_table, start_of_kernel_range.offset(-calculate_physical_to_link_time_address_offset()), end_of_kernel_range.offset(-calculate_physical_to_link_time_address_offset()), start_of_physical_kernel_range, normal_memory_flags);

    // Map kernel into high virtual memory
    insert_entries_for_memory_range(allocator, root_table, start_of_kernel_range, end_of_kernel_range, start_of_physical_kernel_range, normal_memory_flags);
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
    mair_el1.Attr[2] = 0b01000100; // Normal (non-cacheable)
    Aarch64::MAIR_EL1::write(mair_el1);

    // Configure cacheability attributes for memory associated with translation table walks
    Aarch64::TCR_EL1 tcr_el1 = {};

    tcr_el1.SH1 = Aarch64::TCR_EL1::InnerShareable;
    tcr_el1.ORGN1 = Aarch64::TCR_EL1::NormalMemory_Outer_WriteBack_ReadAllocate_WriteAllocateCacheable;
    tcr_el1.IRGN1 = Aarch64::TCR_EL1::NormalMemory_Inner_WriteBack_ReadAllocate_WriteAllocateCacheable;
    tcr_el1.T1SZ = 16;

    tcr_el1.SH0 = Aarch64::TCR_EL1::InnerShareable;
    tcr_el1.ORGN0 = Aarch64::TCR_EL1::NormalMemory_Outer_WriteBack_ReadAllocate_WriteAllocateCacheable;
    tcr_el1.IRGN0 = Aarch64::TCR_EL1::NormalMemory_Inner_WriteBack_ReadAllocate_WriteAllocateCacheable;
    tcr_el1.T0SZ = 16;

    tcr_el1.TG1 = Aarch64::TCR_EL1::TG1GranuleSize::Size_4KB;
    tcr_el1.TG0 = Aarch64::TCR_EL1::TG0GranuleSize::Size_4KB;

    // Auto detect the Intermediate Physical Address Size
    Aarch64::ID_AA64MMFR0_EL1 feature_register = Aarch64::ID_AA64MMFR0_EL1::read();
    tcr_el1.IPS = feature_register.PARange;

    Aarch64::TCR_EL1::write(tcr_el1);

    // Enable MMU in the system control register
    Aarch64::SCTLR_EL1 sctlr_el1 = Aarch64::SCTLR_EL1::read();
    sctlr_el1.M = 1; // Enable MMU
    sctlr_el1.C = 1; // Enable data cache
    sctlr_el1.I = 1; // Enable instruction cache
    Aarch64::SCTLR_EL1::write(sctlr_el1);

    Aarch64::Asm::flush();
}

static u64* get_page_directory(u64* root_table, VirtualAddress virtual_addr)
{
    u64 level0_idx = (virtual_addr.get() >> 39) & 0x1FF;
    u64 level1_idx = (virtual_addr.get() >> 30) & 0x1FF;

    u64* level1_table = root_table;

    if (level1_table[level0_idx] == 0)
        return nullptr;

    u64* level2_table = descriptor_to_pointer(level1_table[level0_idx]);

    if (level2_table[level1_idx] == 0)
        return nullptr;

    return descriptor_to_pointer(level2_table[level1_idx]);
}

static u64* get_page_directory_table(u64* root_table, VirtualAddress virtual_addr)
{
    u64 level0_idx = (virtual_addr.get() >> 39) & 0x1FF;
    u64* level1_table = root_table;

    if (level1_table[level0_idx] == 0)
        return nullptr;

    return descriptor_to_pointer(level1_table[level0_idx]);
}

static void setup_kernel_page_directory(u64* root_table)
{
    auto kernel_page_directory = (PhysicalPtr)get_page_directory(root_table, VirtualAddress { *adjust_by_mapping_base(&g_boot_info.kernel_mapping_base) });
    if (!kernel_page_directory)
        panic_without_mmu("Could not find kernel page directory!"sv);

    *adjust_by_mapping_base(&g_boot_info.boot_pd_kernel) = PhysicalAddress(kernel_page_directory);

    // FIXME: Rename boot_pml4t to something architecture agnostic.
    *adjust_by_mapping_base(&g_boot_info.boot_pml4t) = PhysicalAddress((PhysicalPtr)root_table);

    // FIXME: Rename to directory_table or similar
    *adjust_by_mapping_base(&g_boot_info.boot_pdpt) = PhysicalAddress((PhysicalPtr)get_page_directory_table(root_table, VirtualAddress { *adjust_by_mapping_base(&g_boot_info.kernel_mapping_base) }));
}

void init_page_tables(PhysicalPtr flattened_devicetree_paddr)
{
    ::DeviceTree::FlattenedDeviceTreeHeader* fdt_header = bit_cast<::DeviceTree::FlattenedDeviceTreeHeader*>(flattened_devicetree_paddr);
    if (fdt_header->magic != 0xd00dfeed)
        panic_without_mmu("Invalid FDT passed"sv);

    // Copy the FDT to a known location
    u8* fdt_storage = bit_cast<u8*>(flattened_devicetree_paddr);
    if (fdt_header->totalsize > DeviceTree::fdt_storage_size)
        panic_without_mmu("Passed FDT is bigger than the internal storage"sv);
    for (size_t o = 0; o < fdt_header->totalsize; o += 1) {
        // FIXME: Maybe increase the IO size here
        adjust_by_mapping_base(DeviceTree::s_fdt_storage)[o] = fdt_storage[o];
    }

    *adjust_by_mapping_base(&g_boot_info.boot_method) = BootMethod::PreInit;

    *adjust_by_mapping_base(&g_boot_info.flattened_devicetree_paddr) = PhysicalAddress { flattened_devicetree_paddr };
    *adjust_by_mapping_base(&g_boot_info.flattened_devicetree_size) = fdt_header->totalsize;
    *adjust_by_mapping_base(&g_boot_info.physical_to_virtual_offset) = calculate_physical_to_link_time_address_offset();
    *adjust_by_mapping_base(&g_boot_info.kernel_mapping_base) = KERNEL_MAPPING_BASE;
    *adjust_by_mapping_base(&g_boot_info.kernel_load_base) = KERNEL_MAPPING_BASE;

    PageBumpAllocator allocator(adjust_by_mapping_base((u64*)page_tables_phys_start), adjust_by_mapping_base((u64*)page_tables_phys_end));
    auto root_table = allocator.take_page();
    build_mappings(allocator, root_table);
    setup_quickmap_page_table(allocator, root_table);
    setup_kernel_page_directory(root_table);

    switch_to_page_table(adjust_by_mapping_base(page_tables_phys_start));
    activate_mmu();
}

void unmap_identity_map()
{
    auto start_of_physical_memory = FlatPtr(START_OF_NORMAL_MEMORY);

    u64 level0_idx = (start_of_physical_memory >> 39) & 0x1FF;
    u64 level1_idx = (start_of_physical_memory >> 30) & 0x1FF;

    u64* level1_table = (u64*)page_tables_phys_start;

    auto level2_table = FlatPtr(descriptor_to_pointer(level1_table[level0_idx]));
    if (!level2_table)
        panic_without_mmu("Could not find table!"sv);

    // NOTE: The function descriptor_to_pointer returns a physical address, but we want to unmap that range
    //       so, the pointer must be converted to a virtual address by adding calculate_physical_to_link_time_address_offset().
    level2_table += calculate_physical_to_link_time_address_offset();

    // Unmap the complete identity map
    ((u64*)level2_table)[level1_idx] = 0;
}

}
