/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/riscv64/VirtualMemoryDefinitions.h>

#include <Kernel/EFIPrekernel/Arch/MMU.h>
#include <Kernel/EFIPrekernel/Globals.h>

namespace Kernel {

EFIErrorOr<void*> allocate_empty_root_page_table()
{
    EFI::PhysicalAddress root_page_table_paddr = 0;
    if (auto status = g_efi_system_table->boot_services->allocate_pages(EFI::AllocateType::AnyPages, EFI::MemoryType::LoaderData, 1, &root_page_table_paddr); status != EFI::Status::Success)
        return status;

    auto* root_page_table = bit_cast<void*>(root_page_table_paddr);
    __builtin_memset(root_page_table, 0, PAGE_TABLE_SIZE);

    return root_page_table;
}

static u64* get_pte(u64* page_table, FlatPtr vaddr, size_t level)
{
    size_t pte_index_offset = (PAGE_TABLE_INDEX_BITS * level) + PAGE_OFFSET_BITS;
    size_t pte_index = (vaddr >> pte_index_offset) & PAGE_TABLE_INDEX_MASK;

    return &page_table[pte_index];
}

EFIErrorOr<void*> get_or_insert_page_table(void* root_page_table, FlatPtr vaddr, size_t level, bool has_to_be_new)
{
    VERIFY(root_page_table != nullptr);

    if (level >= PAGE_TABLE_LEVEL_COUNT - 1)
        return EFI::Status::InvalidParameter;

    u64* current_page_table = static_cast<u64*>(root_page_table);

    for (size_t current_level = PAGE_TABLE_LEVEL_COUNT - 1; current_level > level; current_level--) {
        u64* pte = get_pte(current_page_table, vaddr, current_level);

        if ((*pte & to_underlying(PageTableEntryBits::Valid)) != 0) {
            if (current_level - 1 == level && has_to_be_new)
                return EFI::Status::InvalidParameter;

            current_page_table = bit_cast<u64*>((*pte >> PTE_PPN_OFFSET) << PADDR_PPN_OFFSET);
        } else {
            EFI::PhysicalAddress new_page_table_paddr = 0;
            if (auto status = g_efi_system_table->boot_services->allocate_pages(EFI::AllocateType::AnyPages, EFI::MemoryType::LoaderData, 1, &new_page_table_paddr); status != EFI::Status::Success)
                return status;

            __builtin_memset(bit_cast<void*>(new_page_table_paddr), 0, PAGE_TABLE_SIZE);

            *pte = ((new_page_table_paddr >> PADDR_PPN_OFFSET) << PTE_PPN_OFFSET) | to_underlying(PageTableEntryBits::Valid);

            current_page_table = bit_cast<u64*>(new_page_table_paddr);
        }
    }

    return current_page_table;
}

static EFIErrorOr<void> map_single_page(void* root_page_table, FlatPtr vaddr, PhysicalPtr paddr, Access access)
{
    auto* page_table = TRY(get_or_insert_page_table(root_page_table, vaddr));
    u64* pte = get_pte(bit_cast<u64*>(page_table), vaddr, 0);

    if ((*pte & to_underlying(PageTableEntryBits::Valid)) != 0)
        return EFI::Status::InvalidParameter; // already mapped

    // Always set the A/D bits as we don't know if the hardware updates them automatically (i.e. if Svadu is supported).
    // If the hardware doesn't update them automatically they act like additional permission bits.
    PageTableEntryBits flags = PageTableEntryBits::Valid | PageTableEntryBits::Accessed | PageTableEntryBits::Dirty;
    if (has_flag(access, Access::Read))
        flags |= PageTableEntryBits::Readable;
    if (has_flag(access, Access::Write))
        flags |= PageTableEntryBits::Writeable;
    if (has_flag(access, Access::Execute))
        flags |= PageTableEntryBits::Executable;

    *pte = ((paddr >> PADDR_PPN_OFFSET) << PTE_PPN_OFFSET) | to_underlying(flags);

    return {};
}

EFIErrorOr<void> map_pages(void* root_page_table, FlatPtr start_vaddr, PhysicalPtr start_paddr, size_t page_count, Access access)
{
    for (size_t i = 0; i < page_count; i++)
        TRY(map_single_page(root_page_table, start_vaddr + i * PAGE_SIZE, start_paddr + i * PAGE_SIZE, access));

    return {};
}

}
