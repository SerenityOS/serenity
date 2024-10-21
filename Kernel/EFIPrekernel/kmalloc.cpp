/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/kmalloc.h>

#include <Kernel/EFIPrekernel/Globals.h>

static size_t s_kmalloc_call_count;
static size_t s_kfree_call_count;

void kfree_sized(void* ptr, size_t)
{
    if (Kernel::g_efi_system_table == nullptr || Kernel::g_efi_system_table->boot_services == nullptr)
        return;

    s_kfree_call_count++;
    Kernel::g_efi_system_table->boot_services->free_pool(ptr);
}

void* kmalloc(size_t size)
{
    if (Kernel::g_efi_system_table == nullptr || Kernel::g_efi_system_table->boot_services == nullptr)
        return nullptr;

    void* ret;

    s_kmalloc_call_count++;

    if (Kernel::g_efi_system_table->boot_services->allocate_pool(Kernel::EFI::MemoryType::LoaderData, size, &ret) != Kernel::EFI::Status::Success)
        return nullptr;

    return ret;
}

size_t kmalloc_good_size(size_t size)
{
    return size;
}

void get_kmalloc_stats(kmalloc_stats& stats)
{
    stats.bytes_allocated = 0;
    stats.bytes_free = 0;
    stats.kmalloc_call_count = s_kmalloc_call_count;
    stats.kfree_call_count = s_kfree_call_count;
}
