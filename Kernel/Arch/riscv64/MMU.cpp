/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/Arch/riscv64/CPU.h>
#include <Kernel/Arch/riscv64/MMU.h>
#include <Kernel/Arch/riscv64/SBI.h>
#include <Kernel/Arch/riscv64/VirtualMemoryDefinitions.h>
#include <Kernel/Arch/riscv64/pre_init.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Sections.h>
#include <LibDeviceTree/FlattenedDeviceTree.h>

// These come from the linker script
extern u8 page_tables_phys_start[];
extern u8 page_tables_phys_end[];
extern u8 start_of_kernel_image[];
extern u8 end_of_kernel_image[];

namespace Kernel::Memory {

class PageBumpAllocator {
public:
    PageBumpAllocator(u64* start, u64 const* end)
        : m_start(start)
        , m_end(end)
        , m_current(start)
    {
        if (m_start >= m_end) {
            panic_without_mmu("Invalid memory range passed to PageBumpAllocator"sv);
        }
        if (bit_cast<FlatPtr>(m_start) % PAGE_TABLE_SIZE != 0 || bit_cast<FlatPtr>(m_end) % PAGE_TABLE_SIZE != 0) {
            panic_without_mmu("Memory range passed into PageBumpAllocator not aligned to PAGE_TABLE_SIZE"sv);
        }
    }

    u64* take_page()
    {
        if (m_current >= m_end) {
            panic_without_mmu("pre_init page table memory exhausted"sv);
        }

        u64* page = m_current;
        m_current += (PAGE_TABLE_SIZE / sizeof(FlatPtr));

        // We can't use [__builtin_]memset here, as that would call into code which has stack protectors enabled,
        // resulting in an access to an absolute address.
        for (u64* p = page; p < page + (PAGE_TABLE_SIZE / sizeof(u64)); p++)
            *p = 0;

        return page;
    }

private:
    u64 const* m_start;
    u64 const* m_end;
    u64* m_current;
};

static UNMAP_AFTER_INIT FlatPtr calculate_physical_to_link_time_address_offset()
{
    FlatPtr physical_address;
    FlatPtr link_time_address;

    asm volatile(
        "   lla %[physical_address], 1f\n"
        "1: la %[link_time_address], 1b\n"
        : [physical_address] "=r"(physical_address),
        [link_time_address] "=r"(link_time_address));

    return link_time_address - physical_address;
}

template<typename T>
inline T* adjust_by_mapping_base(T* ptr)
{
    return bit_cast<T*>(bit_cast<FlatPtr>(ptr) - calculate_physical_to_link_time_address_offset());
}

static UNMAP_AFTER_INIT bool page_table_entry_valid(u64 entry)
{
    return (entry & to_underlying(PageTableEntryBits::Valid)) != 0;
}

static UNMAP_AFTER_INIT u64* insert_page_table(PageBumpAllocator& allocator, u64* root_table, VirtualAddress virtual_addr)
{
    size_t vpn_1 = (virtual_addr.get() >> VPN_1_OFFSET) & PAGE_TABLE_INDEX_MASK;
    size_t vpn_2 = (virtual_addr.get() >> VPN_2_OFFSET) & PAGE_TABLE_INDEX_MASK;

    u64* level2_table = root_table;

    if (!page_table_entry_valid(level2_table[vpn_2])) {
        level2_table[vpn_2] = (bit_cast<FlatPtr>(allocator.take_page()) >> PADDR_PPN_OFFSET) << PTE_PPN_OFFSET;
        level2_table[vpn_2] |= to_underlying(PageTableEntryBits::Valid);
    }

    u64* level1_table = bit_cast<u64*>((level2_table[vpn_2] >> PTE_PPN_OFFSET) << PADDR_PPN_OFFSET);

    if (!page_table_entry_valid(level1_table[vpn_1])) {
        level1_table[vpn_1] = (bit_cast<FlatPtr>(allocator.take_page()) >> PADDR_PPN_OFFSET) << PTE_PPN_OFFSET;
        level1_table[vpn_1] |= to_underlying(PageTableEntryBits::Valid);
    }

    return bit_cast<u64*>((level1_table[vpn_1] >> PTE_PPN_OFFSET) << PADDR_PPN_OFFSET);
}

static UNMAP_AFTER_INIT u64* get_page_directory(u64* root_table, VirtualAddress virtual_addr)
{
    size_t vpn_2 = (virtual_addr.get() >> VPN_2_OFFSET) & PAGE_TABLE_INDEX_MASK;

    u64* level2_table = root_table;

    if (!page_table_entry_valid(level2_table[vpn_2]))
        return nullptr;

    return bit_cast<u64*>((level2_table[vpn_2] >> PTE_PPN_OFFSET) << PADDR_PPN_OFFSET);
}

static UNMAP_AFTER_INIT void insert_entry(PageBumpAllocator& allocator, u64* root_table, VirtualAddress vaddr, PhysicalAddress paddr, PageTableEntryBits flags)
{
    u64* level0_table = insert_page_table(allocator, root_table, vaddr);

    size_t vpn_0 = (vaddr.get() >> VPN_0_OFFSET) & PAGE_TABLE_INDEX_MASK;
    level0_table[vpn_0] = (paddr.get() >> PADDR_PPN_OFFSET) << PTE_PPN_OFFSET;
    level0_table[vpn_0] |= to_underlying(PageTableEntryBits::Valid | PageTableEntryBits::Accessed | PageTableEntryBits::Dirty | flags);
}

static UNMAP_AFTER_INIT void insert_entries_for_memory_range(PageBumpAllocator& allocator, u64* root_table, VirtualAddress start, VirtualAddress end, PhysicalAddress paddr, PageTableEntryBits flags)
{
    // Not very efficient, but simple and it works.
    for (VirtualAddress vaddr = start; vaddr < end;) {
        insert_entry(allocator, root_table, vaddr, paddr, flags);
        vaddr = vaddr.offset(PAGE_SIZE);
        paddr = paddr.offset(PAGE_SIZE);
    }
}

static UNMAP_AFTER_INIT void setup_quickmap_page_table(PageBumpAllocator& allocator, u64* root_table)
{
    auto kernel_pt1024_base = VirtualAddress { *adjust_by_mapping_base(&g_boot_info.kernel_mapping_base) + KERNEL_PT1024_OFFSET };

    auto quickmap_page_table = PhysicalAddress { bit_cast<PhysicalPtr>(insert_page_table(allocator, root_table, kernel_pt1024_base)) };
    *adjust_by_mapping_base(&g_boot_info.boot_pd_kernel_pt1023) = bit_cast<PageTableEntry*>(quickmap_page_table.offset(calculate_physical_to_link_time_address_offset()).get());
}

static UNMAP_AFTER_INIT void build_mappings(PageBumpAllocator& allocator, u64* root_table)
{
    auto start_of_kernel_range = VirtualAddress { bit_cast<FlatPtr>(+start_of_kernel_image) };
    auto end_of_kernel_range = VirtualAddress { bit_cast<FlatPtr>(+end_of_kernel_image) };

    auto start_of_physical_kernel_range = PhysicalAddress { start_of_kernel_range.get() }.offset(-calculate_physical_to_link_time_address_offset());

    // FIXME: dont map everything RWX

    // Map kernel into high virtual memory
    insert_entries_for_memory_range(allocator, root_table, start_of_kernel_range, end_of_kernel_range, start_of_physical_kernel_range, PageTableEntryBits::Readable | PageTableEntryBits::Writeable | PageTableEntryBits::Executable);
}

static UNMAP_AFTER_INIT void setup_kernel_page_directory(u64* root_table)
{
    auto kernel_page_directory = bit_cast<PhysicalPtr>(get_page_directory(root_table, VirtualAddress { *adjust_by_mapping_base(&g_boot_info.kernel_mapping_base) }));
    if (kernel_page_directory == 0)
        panic_without_mmu("Could not find kernel page directory!"sv);

    *adjust_by_mapping_base(&g_boot_info.boot_pd_kernel) = PhysicalAddress { kernel_page_directory };

    // There is no level 4 table in Sv39
    *adjust_by_mapping_base(&g_boot_info.boot_pml4t) = PhysicalAddress { 0 };

    *adjust_by_mapping_base(&g_boot_info.boot_pdpt) = PhysicalAddress { bit_cast<PhysicalPtr>(root_table) };
}

// This function has to fit into one page as it will be identity mapped.
[[gnu::aligned(PAGE_SIZE)]] [[noreturn]] UNMAP_AFTER_INIT static void enable_paging(BootInfo const& info, FlatPtr satp, u64* enable_paging_pte)
{
    // Switch current root page table to argument 1. This will immediately take effect, but we won't not crash as this function is identity mapped.
    // Also, set up a temporary trap handler to catch any traps while switching page tables.
    auto offset = calculate_physical_to_link_time_address_offset();
    register FlatPtr a0 asm("a0") = bit_cast<FlatPtr>(&info);
    asm volatile(
        "   lla t0, 1f \n"
        "   csrw stvec, t0 \n"

        "   csrw satp, %[satp] \n"
        "   sfence.vma \n"

        // Continue execution at high virtual address.
        "   lla t0, 2f \n"
        "   add t0, t0, %[offset] \n"
        "   jr t0 \n"
        "2: \n"

        // Add kernel_mapping_base to the stack pointer, such that it is also using the mapping in high virtual memory.
        "   add sp, sp, %[offset] \n"

        // Zero the PTE which identity maps this function
        "   add t0, %[offset], %[enable_paging_pte] \n"
        "   sd zero, (t0) \n"
        "   sfence.vma \n"

        "   li ra, 0 \n"
        "   li fp, 0 \n"
        "   tail init \n"

        ".p2align 2 \n"
        "1: csrw sie, zero \n"
        "   wfi \n"
        "   j 1b \n"
        :
        : "r"(a0), [satp] "r"(satp), [offset] "r"(offset), [enable_paging_pte] "r"(enable_paging_pte)
        : "t0");

    VERIFY_NOT_REACHED();
}

[[noreturn]] UNMAP_AFTER_INIT void init_page_tables_and_jump_to_init(FlatPtr boot_hart_id, PhysicalPtr flattened_devicetree_paddr)
{
    if (RISCV64::CSR::SATP::read().MODE != RISCV64::CSR::SATP::Mode::Bare)
        panic_without_mmu("Kernel booted with MMU enabled"sv);

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

    *adjust_by_mapping_base(&g_boot_info.arch_specific.boot_hart_id) = boot_hart_id;

    PageBumpAllocator allocator(adjust_by_mapping_base(reinterpret_cast<u64*>(page_tables_phys_start)), adjust_by_mapping_base(reinterpret_cast<u64*>(page_tables_phys_end)));
    auto* root_table = allocator.take_page();
    build_mappings(allocator, root_table);
    setup_quickmap_page_table(allocator, root_table);
    setup_kernel_page_directory(root_table);

    // Identity map the `enable_paging` function and save the level 0 table address in order to remove the identity mapping in `enable_paging` again
    auto const enable_paging_vaddr = VirtualAddress { bit_cast<FlatPtr>(&enable_paging) };
    auto const enable_paging_paddr = PhysicalAddress { bit_cast<PhysicalPtr>(&enable_paging) };

    u64* enable_paging_level0_table = insert_page_table(allocator, root_table, enable_paging_vaddr);

    size_t enable_paging_vpn_0 = (enable_paging_vaddr.get() >> VPN_0_OFFSET) & PAGE_TABLE_INDEX_MASK;
    enable_paging_level0_table[enable_paging_vpn_0] = (enable_paging_paddr.get() >> PADDR_PPN_OFFSET) << PTE_PPN_OFFSET;
    enable_paging_level0_table[enable_paging_vpn_0] |= to_underlying(PageTableEntryBits::Valid | PageTableEntryBits::Accessed | PageTableEntryBits::Dirty | PageTableEntryBits::Readable | PageTableEntryBits::Executable);

    RISCV64::CSR::SATP satp = {
        .PPN = bit_cast<FlatPtr>(root_table) >> PADDR_PPN_OFFSET,
        .ASID = 0,
        .MODE = RISCV64::CSR::SATP::Mode::Sv39,
    };

    enable_paging(g_boot_info, bit_cast<FlatPtr>(satp), &enable_paging_level0_table[enable_paging_vpn_0]);
}

}
