/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Singleton.h>

#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel::Memory {

struct SATPMap {
    SpinlockProtected<IntrusiveRedBlackTree<&PageDirectory::m_tree_node>, LockRank::None> map {};
};

static Singleton<SATPMap> s_satp_map;

void PageDirectory::register_page_directory(PageDirectory* page_directory)
{
    s_satp_map->map.with([&](auto& map) {
        map.insert(bit_cast<FlatPtr>(page_directory->satp()), *page_directory);
    });
}

void PageDirectory::deregister_page_directory(PageDirectory* page_directory)
{
    s_satp_map->map.with([&](auto& map) {
        map.remove(bit_cast<FlatPtr>(page_directory->satp()));
    });
}

ErrorOr<NonnullLockRefPtr<PageDirectory>> PageDirectory::try_create_for_userspace(Process& process)
{
    auto directory = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) PageDirectory));

    directory->m_process = &process;

    directory->m_directory_table = TRY(MM.allocate_physical_page());
    auto kernel_pd_index = (g_boot_info.kernel_mapping_base >> VPN_2_OFFSET) & PAGE_TABLE_INDEX_MASK;
    for (size_t i = 0; i < kernel_pd_index; i++) {
        directory->m_directory_pages[i] = TRY(MM.allocate_physical_page());
    }

    // Share the top 1 GiB of kernel-only mappings (>=kernel_mapping_base)
    directory->m_directory_pages[kernel_pd_index] = MM.kernel_page_directory().m_directory_pages[kernel_pd_index];

    {
        InterruptDisabler disabler;
        auto& table = *reinterpret_cast<PageDirectoryPointerTable*>(MM.quickmap_page(*directory->m_directory_table));
        for (size_t i = 0; i < array_size(directory->m_directory_pages); i++) {
            if (directory->m_directory_pages[i] != nullptr) {
                table.raw[i] = ((directory->m_directory_pages[i]->paddr().get()) >> PADDR_PPN_OFFSET) << PTE_PPN_OFFSET;
                table.raw[i] |= to_underlying(PageTableEntryBits::Valid);
            }
        }
        MM.unquickmap_page();
    }

    register_page_directory(directory);
    return directory;
}

LockRefPtr<PageDirectory> PageDirectory::find_current()
{
    return s_satp_map->map.with([&](auto& map) {
        return map.find(bit_cast<FlatPtr>(RISCV64::CSR::SATP::read()));
    });
}

void activate_kernel_page_directory(PageDirectory const& page_directory)
{
    RISCV64::CSR::SATP::write(page_directory.satp());
    Processor::flush_entire_tlb_local();
}

void activate_page_directory(PageDirectory const& page_directory, Thread* thread)
{
    auto const satp = page_directory.satp();
    thread->regs().satp = satp;
    RISCV64::CSR::SATP::write(satp);
    Processor::flush_entire_tlb_local();
}

UNMAP_AFTER_INIT NonnullLockRefPtr<PageDirectory> PageDirectory::must_create_kernel_page_directory()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) PageDirectory).release_nonnull();
}

PageDirectory::PageDirectory() = default;

UNMAP_AFTER_INIT void PageDirectory::allocate_kernel_directory()
{
    dmesgln("MM: boot_pdpt @ {}", g_boot_info.boot_pdpt);
    dmesgln("MM: boot_pd_kernel @ {}", g_boot_info.boot_pd_kernel);

    m_directory_table = PhysicalRAMPage::create(g_boot_info.boot_pdpt, MayReturnToFreeList::No);
    m_directory_pages[(g_boot_info.kernel_mapping_base >> VPN_2_OFFSET) & PAGE_TABLE_INDEX_MASK] = PhysicalRAMPage::create(g_boot_info.boot_pd_kernel, MayReturnToFreeList::No);

    if (g_boot_info.boot_method == BootMethod::EFI) {
        dmesgln("MM: bootstrap_page_page_directory @ {}", g_boot_info.boot_method_specific.efi.bootstrap_page_page_directory_paddr);
        m_directory_pages[(g_boot_info.boot_method_specific.efi.bootstrap_page_vaddr.get() >> VPN_2_OFFSET) & PAGE_TABLE_INDEX_MASK] = PhysicalRAMPage::create(g_boot_info.boot_method_specific.efi.bootstrap_page_page_directory_paddr, MayReturnToFreeList::No);
    }
}

PageDirectory::~PageDirectory()
{
    if (is_root_table_initialized()) {
        deregister_page_directory(this);
    }
}

}
