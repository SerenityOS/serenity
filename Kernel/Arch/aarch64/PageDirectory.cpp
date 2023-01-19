/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/InterruptDisabler.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/Sections.h>
#include <Kernel/Thread.h>

namespace Kernel::Memory {

void PageDirectory::register_page_directory(PageDirectory*)
{
    dbgln("FIXME: PageDirectory: Actually implement registering a page directory!");
}

void PageDirectory::deregister_page_directory(PageDirectory*)
{
    TODO_AARCH64();
}

LockRefPtr<PageDirectory> PageDirectory::find_current()
{
    TODO_AARCH64();
    return nullptr;
}

void activate_kernel_page_directory(PageDirectory const&)
{
    dbgln("FIXME: PageDirectory: Actually implement activating a kernel page directory!");
}

void activate_page_directory(PageDirectory const&, Thread*)
{
    TODO_AARCH64();
}

UNMAP_AFTER_INIT NonnullLockRefPtr<PageDirectory> PageDirectory::must_create_kernel_page_directory()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) PageDirectory).release_nonnull();
}

ErrorOr<NonnullLockRefPtr<PageDirectory>> PageDirectory::try_create_for_userspace()
{
    auto directory = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) PageDirectory));

    directory->m_root_table = TRY(MM.allocate_physical_page());

    directory->m_directory_table = TRY(MM.allocate_physical_page());
    auto kernel_pd_index = (kernel_mapping_base >> 30) & 0x1ffu;
    for (size_t i = 0; i < kernel_pd_index; i++) {
        directory->m_directory_pages[i] = TRY(MM.allocate_physical_page());
    }

    // Share the top 1 GiB of kernel-only mappings (>=kernel_mapping_base)
    directory->m_directory_pages[kernel_pd_index] = MM.kernel_page_directory().m_directory_pages[kernel_pd_index];

    {
        InterruptDisabler disabler;
        auto& table = *(PageDirectoryPointerTable*)MM.quickmap_page(*directory->m_root_table);
        table.raw[0] = (FlatPtr)directory->m_directory_table->paddr().as_ptr() | TABLE_DESCRIPTOR;
        MM.unquickmap_page();
    }

    {
        InterruptDisabler disabler;
        auto& table = *(PageDirectoryPointerTable*)MM.quickmap_page(*directory->m_directory_table);
        for (size_t i = 0; i < sizeof(m_directory_pages) / sizeof(m_directory_pages[0]); i++) {
            if (directory->m_directory_pages[i]) {
                table.raw[i] = (FlatPtr)directory->m_directory_pages[i]->paddr().as_ptr() | PAGE_DESCRIPTOR;
            }
        }
        MM.unquickmap_page();
    }

    register_page_directory(directory);
    return directory;
}

PageDirectory::PageDirectory() = default;

UNMAP_AFTER_INIT void PageDirectory::allocate_kernel_directory()
{
    // Adopt the page tables already set up by boot.S
    dmesgln("MM: boot_pml4t @ {}", boot_pml4t);
    m_root_table = PhysicalPage::create(boot_pml4t, MayReturnToFreeList::No);
    dmesgln("MM: boot_pdpt @ {}", boot_pdpt);
    dmesgln("MM: boot_pd0 @ {}", boot_pd0);
    dmesgln("MM: boot_pd_kernel @ {}", boot_pd_kernel);
    m_directory_table = PhysicalPage::create(boot_pdpt, MayReturnToFreeList::No);
    m_directory_pages[0] = PhysicalPage::create(boot_pd0, MayReturnToFreeList::No);
    m_directory_pages[(kernel_mapping_base >> 30) & 0x1ff] = PhysicalPage::create(boot_pd_kernel, MayReturnToFreeList::No);
}

PageDirectory::~PageDirectory()
{
    if (is_root_table_initialized()) {
        deregister_page_directory(this);
    }
}

}
