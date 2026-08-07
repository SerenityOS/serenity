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
    auto kernel_pt1024_base = VirtualAddress(g_boot_info.kernel_mapping_base + KERNEL_PT1024_OFFSET);

    auto quickmap_page_table = PhysicalAddress((PhysicalPtr)insert_page_table(allocator, root_table, kernel_pt1024_base));
    g_boot_info.boot_pd_kernel_pt1023 = (PageTableEntry*)quickmap_page_table.offset(calculate_physical_to_link_time_address_offset()).get();
}

static void build_mappings(PageBumpAllocator& allocator, u64* root_table)
{
    u64 normal_memory_flags = ACCESS_FLAG | PAGE_DESCRIPTOR | INNER_SHAREABLE | NORMAL_MEMORY;

    auto start_of_kernel_range = VirtualAddress { KERNEL_MAPPING_BASE };
    auto end_of_kernel_range = start_of_kernel_range.offset((+end_of_kernel_image) - (+start_of_kernel_image));

    auto start_of_physical_kernel_range = PhysicalAddress { bit_cast<PhysicalPtr>(+start_of_kernel_image) };

    // Map kernel into high virtual memory
    insert_entries_for_memory_range(allocator, root_table, start_of_kernel_range, end_of_kernel_range, start_of_physical_kernel_range, normal_memory_flags);
}

static void switch_to_page_table(u8* page_table)
{
    Aarch64::Asm::set_ttbr0_el1((FlatPtr)page_table);
    Aarch64::Asm::set_ttbr1_el1((FlatPtr)page_table);
}

static void configure_mmu()
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
    auto kernel_page_directory = (PhysicalPtr)get_page_directory(root_table, VirtualAddress { g_boot_info.kernel_mapping_base });
    if (!kernel_page_directory)
        panic_without_mmu("Could not find kernel page directory!"sv);

    g_boot_info.boot_pd_kernel = PhysicalAddress(kernel_page_directory);

    // FIXME: Rename boot_pml4t to something architecture agnostic.
    g_boot_info.boot_pml4t = PhysicalAddress((PhysicalPtr)root_table);

    // FIXME: Rename to directory_table or similar
    g_boot_info.boot_pdpt = PhysicalAddress((PhysicalPtr)get_page_directory_table(root_table, VirtualAddress { g_boot_info.kernel_mapping_base }));
}

// This function has to fit into one page as it will be identity mapped.
[[gnu::aligned(PAGE_SIZE)]] [[noreturn]] UNMAP_AFTER_INIT static void activate_mmu(BootInfo const& info, u64* activate_mmu_pte)
{
    // Enable the MMU. This will immediately take effect, but we won't crash as this function is identity mapped.
    auto offset = calculate_physical_to_link_time_address_offset();
    register FlatPtr x0 asm("x0") = bit_cast<FlatPtr>(&info) + offset;
    asm volatile(
        R"(
            // Invalidate the TLBs before enabling the MMU, as the TLBs might still contain old values.
            tlbi vmalle1
            dsb ish
            isb

            // Enable the MMU, data cache, and instruction cache.
            mrs x1, sctlr_el1
            mov w2, #(1 << 0) | (1 << 2) | (1 << 12)
            orr x1, x1, x2
            msr sctlr_el1, x1
            isb

            // Continue execution at high virtual address.
            adrp x1, 1f
            add x1, x1, :lo12:1f
            add x1, x1, %[offset]
            br x1
        1:

            // Add offset to the stack pointer, such that it is also using the mapping in high virtual memory.
            add sp, sp, %[offset]

            // Zero the PTE which identity maps this function.
            add x1, %[activate_mmu_pte], %[offset]
            str xzr, [x1]
            dsb ishst
            tlbi vmalle1
            dsb ish
            isb

            mov lr, xzr
            mov fp, xzr

            b init
        )"
        :
        : "r"(x0), [offset] "r"(offset), [activate_mmu_pte] "r"(activate_mmu_pte)
        : "x1", "x2", "memory");

    VERIFY_NOT_REACHED();
}

[[noreturn]] void init_page_tables_and_jump_to_init(PhysicalPtr flattened_devicetree_paddr)
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
        DeviceTree::s_fdt_storage[o] = fdt_storage[o];
    }

    g_boot_info.boot_method = BootMethod::PreInit;

    g_boot_info.flattened_devicetree_paddr = PhysicalAddress { flattened_devicetree_paddr };
    g_boot_info.flattened_devicetree_size = fdt_header->totalsize;
    g_boot_info.physical_to_virtual_offset = calculate_physical_to_link_time_address_offset();
    g_boot_info.kernel_mapping_base = KERNEL_MAPPING_BASE;
    g_boot_info.kernel_load_base = KERNEL_MAPPING_BASE;

    PageBumpAllocator allocator((u64*)page_tables_phys_start, (u64*)page_tables_phys_end);
    auto root_table = allocator.take_page();
    build_mappings(allocator, root_table);
    setup_quickmap_page_table(allocator, root_table);
    setup_kernel_page_directory(root_table);

    // Identity map the `activate_mmu` function and save the level 3 table address in order to remove the identity mapping in `activate_mmu` again.
    auto const activate_mmu_vaddr = VirtualAddress { bit_cast<FlatPtr>(&activate_mmu) };
    auto const activate_mmu_paddr = PhysicalAddress { bit_cast<PhysicalPtr>(&activate_mmu) };

    u64* activate_mmu_level3_table = insert_page_table(allocator, root_table, activate_mmu_vaddr);

    size_t activate_mmu_level3_index = (activate_mmu_vaddr.get() >> 12) & 0x1ff;
    activate_mmu_level3_table[activate_mmu_level3_index] = activate_mmu_paddr.get();
    activate_mmu_level3_table[activate_mmu_level3_index] |= ACCESS_FLAG | PAGE_DESCRIPTOR | INNER_SHAREABLE | NORMAL_MEMORY;

    switch_to_page_table(page_tables_phys_start);
    configure_mmu();
    activate_mmu(g_boot_info, &activate_mmu_level3_table[activate_mmu_level3_index]);
}

}
