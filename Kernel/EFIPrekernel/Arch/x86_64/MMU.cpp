/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/EFIPrekernel/Arch/MMU.h>
#include <Kernel/EFIPrekernel/Globals.h>

namespace Kernel {

static constexpr size_t PAGE_TABLE_SIZE = PAGE_SIZE;

EFIErrorOr<void*> allocate_empty_root_page_table()
{
    EFI::PhysicalAddress root_page_table_paddr = 0;
    if (auto status = g_efi_system_table->boot_services->allocate_pages(EFI::AllocateType::AnyPages, EFI::MemoryType::LoaderData, 1, &root_page_table_paddr); status != EFI::Status::Success)
        return status;

    auto* root_page_table = bit_cast<void*>(root_page_table_paddr);
    __builtin_memset(root_page_table, 0, PAGE_TABLE_SIZE);

    return root_page_table;
}

static EFIErrorOr<void> map_single_page(void* root_page_table, FlatPtr vaddr, PhysicalPtr paddr, Access access)
{
    (void)root_page_table;
    (void)vaddr;
    (void)paddr;
    (void)access;
    TODO();
}

EFIErrorOr<void*> get_or_insert_page_table(void* root_page_table, FlatPtr vaddr, size_t level, bool has_to_be_new)
{
    (void)root_page_table;
    (void)vaddr;
    (void)level;
    (void)has_to_be_new;
    TODO();
}

EFIErrorOr<void> map_pages(void* root_page_table, FlatPtr start_vaddr, PhysicalPtr start_paddr, size_t page_count, Access access)
{
    for (size_t i = 0; i < page_count; i++)
        TRY(map_single_page(root_page_table, start_vaddr + i * PAGE_SIZE, start_paddr + i * PAGE_SIZE, access));

    return {};
}

}
