/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2022, James Mintram <me@jamesrm.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Arch/CPU.h>
#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel::Memory {

struct CR3Map {
    SpinlockProtected<IntrusiveRedBlackTree<&PageDirectory::m_tree_node>, LockRank::None> map {};
};

static Singleton<CR3Map> s_cr3_map;

void PageDirectory::register_page_directory(PageDirectory* directory)
{
    s_cr3_map->map.with([&](auto& map) {
        map.insert(directory->cr3(), *directory);
    });
}

void PageDirectory::deregister_page_directory(PageDirectory* directory)
{
    s_cr3_map->map.with([&](auto& map) {
        map.remove(directory->cr3());
    });
}

LockRefPtr<PageDirectory> PageDirectory::find_current()
{
    return s_cr3_map->map.with([&](auto& map) {
        return map.find(read_cr3());
    });
}

void activate_kernel_page_directory(PageDirectory const& pgd)
{
    write_cr3(pgd.cr3());
}

void activate_page_directory(PageDirectory const& pgd, Thread* current_thread)
{
    current_thread->regs().cr3 = pgd.cr3();
    write_cr3(pgd.cr3());
}

UNMAP_AFTER_INIT NonnullLockRefPtr<PageDirectory> PageDirectory::must_create_kernel_page_directory()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) PageDirectory).release_nonnull();
}

ErrorOr<NonnullLockRefPtr<PageDirectory>> PageDirectory::try_create_for_userspace(Process& process)
{
    auto directory = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) PageDirectory));

    directory->m_process = &process;

    directory->m_pml4t = TRY(MM.allocate_physical_page());

    directory->m_directory_table = TRY(MM.allocate_physical_page());
    auto kernel_pd_index = (kernel_mapping_base >> 30) & 0x1ffu;
    for (size_t i = 0; i < kernel_pd_index; i++) {
        directory->m_directory_pages[i] = TRY(MM.allocate_physical_page());
    }

    // Share the top 1 GiB of kernel-only mappings (>=kernel_mapping_base)
    directory->m_directory_pages[kernel_pd_index] = MM.kernel_page_directory().m_directory_pages[kernel_pd_index];

    {
        InterruptDisabler disabler;
        auto& table = *(PageDirectoryPointerTable*)MM.quickmap_page(*directory->m_pml4t);
        table.raw[0] = (FlatPtr)directory->m_directory_table->paddr().as_ptr() | 7;
        MM.unquickmap_page();
    }

    {
        InterruptDisabler disabler;
        auto& table = *(PageDirectoryPointerTable*)MM.quickmap_page(*directory->m_directory_table);
        for (size_t i = 0; i < sizeof(m_directory_pages) / sizeof(m_directory_pages[0]); i++) {
            if (directory->m_directory_pages[i]) {
                table.raw[i] = (FlatPtr)directory->m_directory_pages[i]->paddr().as_ptr() | 7;
            }
        }

        // 2 ** MAXPHYADDR - 1
        // Where MAXPHYADDR = physical_address_bit_width
        u64 max_physical_address = (1ULL << Processor::current().physical_address_bit_width()) - 1;

        // bit 63 = no execute
        // bit 7 = page size
        // bit 5 = accessed
        // bit 4 = cache disable
        // bit 3 = write through
        // bit 2 = user/supervisor
        // bit 1 = read/write
        // bit 0 = present
        constexpr u64 pdpte_bit_flags = 0x80000000000000BF;

        // This is to notify us of bugs where we're:
        // 1. Going over what the processor is capable of.
        // 2. Writing into the reserved bits (51:MAXPHYADDR), where doing so throws a GPF
        //    when writing out the PDPT pointer to CR3.
        // The reason we're not checking the page directory's physical address directly is because
        // we're checking for sign extension when putting it into a PDPTE. See issue #4584.
        for (auto table_entry : table.raw)
            VERIFY((table_entry & ~pdpte_bit_flags) <= max_physical_address);

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
    m_pml4t = PhysicalPage::create(boot_pml4t, MayReturnToFreeList::No);
    dmesgln("MM: boot_pdpt @ {}", boot_pdpt);
    dmesgln("MM: boot_pd0 @ {}", boot_pd0);
    dmesgln("MM: boot_pd_kernel @ {}", boot_pd_kernel);
    m_directory_table = PhysicalPage::create(boot_pdpt, MayReturnToFreeList::No);
    m_directory_pages[0] = PhysicalPage::create(boot_pd0, MayReturnToFreeList::No);
    m_directory_pages[(kernel_mapping_base >> 30) & 0x1ff] = PhysicalPage::create(boot_pd_kernel, MayReturnToFreeList::No);
}

PageDirectory::~PageDirectory()
{
    if (is_cr3_initialized()) {
        deregister_page_directory(this);
    }
}

}
