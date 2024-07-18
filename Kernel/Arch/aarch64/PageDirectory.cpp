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
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel::Memory {

struct TTBR0Map {
    SpinlockProtected<IntrusiveRedBlackTree<&PageDirectory::m_tree_node>, LockRank::None> map {};
};

static Singleton<TTBR0Map> s_ttbr0_map;

void PageDirectory::register_page_directory(PageDirectory* directory)
{
    s_ttbr0_map->map.with([&](auto& map) {
        map.insert(directory->ttbr0(), *directory);
    });
}

void PageDirectory::deregister_page_directory(PageDirectory* directory)
{
    s_ttbr0_map->map.with([&](auto& map) {
        map.remove(directory->ttbr0());
    });
}

LockRefPtr<PageDirectory> PageDirectory::find_current()
{
    return s_ttbr0_map->map.with([&](auto& map) {
        return map.find(Aarch64::Asm::get_ttbr0_el1());
    });
}

void activate_kernel_page_directory(PageDirectory const& page_directory)
{
    Aarch64::Asm::set_ttbr0_el1(page_directory.ttbr0());
    Processor::flush_entire_tlb_local();
}

void activate_page_directory(PageDirectory const& page_directory, Thread* current_thread)
{
    current_thread->regs().ttbr0_el1 = page_directory.ttbr0();
    Aarch64::Asm::set_ttbr0_el1(page_directory.ttbr0());
    Processor::flush_entire_tlb_local();
}

UNMAP_AFTER_INIT NonnullLockRefPtr<PageDirectory> PageDirectory::must_create_kernel_page_directory()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) PageDirectory).release_nonnull();
}

ErrorOr<NonnullLockRefPtr<PageDirectory>> PageDirectory::try_create_for_userspace(Process& process)
{
    auto directory = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) PageDirectory));

    directory->m_process = &process;

    directory->m_root_table = TRY(MM.allocate_physical_page());

    directory->m_directory_table = TRY(MM.allocate_physical_page());
    auto kernel_pd_index = (g_boot_info.kernel_mapping_base >> 30) & 0x1ffu;
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
    dmesgln("MM: boot_pml4t @ {}", g_boot_info.boot_pml4t);
    dmesgln("MM: boot_pdpt @ {}", g_boot_info.boot_pdpt);
    dmesgln("MM: boot_pd_kernel @ {}", g_boot_info.boot_pd_kernel);

    m_root_table = PhysicalRAMPage::create(g_boot_info.boot_pml4t, MayReturnToFreeList::No);
    m_directory_table = PhysicalRAMPage::create(g_boot_info.boot_pdpt, MayReturnToFreeList::No);
    m_directory_pages[(g_boot_info.kernel_mapping_base >> 30) & 0x1ff] = PhysicalRAMPage::create(g_boot_info.boot_pd_kernel, MayReturnToFreeList::No);

    if (g_boot_info.boot_method == BootMethod::EFI) {
        dmesgln("MM: bootstrap_page_page_directory @ {}", g_boot_info.boot_method_specific.efi.bootstrap_page_page_directory_paddr);
        m_directory_pages[(g_boot_info.boot_method_specific.efi.bootstrap_page_vaddr.get() >> 30) & 0x1ff] = PhysicalRAMPage::create(g_boot_info.boot_method_specific.efi.bootstrap_page_page_directory_paddr, MayReturnToFreeList::No);
    }
}

PageDirectory::~PageDirectory()
{
    if (is_root_table_initialized()) {
        deregister_page_directory(this);
    }
}

}
