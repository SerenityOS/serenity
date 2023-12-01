/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Singleton.h>

#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel::Memory {

struct SatpMap {
    SpinlockProtected<IntrusiveRedBlackTree<&PageDirectory::m_tree_node>, LockRank::None> map {};
};

static Singleton<SatpMap> s_satp_map;

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

ErrorOr<NonnullLockRefPtr<PageDirectory>> PageDirectory::try_create_for_userspace(Process&)
{
    TODO_RISCV64();
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
    dmesgln("MM: boot_pdpt @ {}", boot_pdpt);
    dmesgln("MM: boot_pd0 @ {}", boot_pd0);
    dmesgln("MM: boot_pd_kernel @ {}", boot_pd_kernel);
    m_directory_table = PhysicalPage::create(boot_pdpt, MayReturnToFreeList::No);
    m_directory_pages[0] = PhysicalPage::create(boot_pd0, MayReturnToFreeList::No);
    m_directory_pages[(kernel_mapping_base >> VPN_2_OFFSET) & PAGE_TABLE_INDEX_MASK] = PhysicalPage::create(boot_pd_kernel, MayReturnToFreeList::No);
}

PageDirectory::~PageDirectory()
{
    if (is_root_table_initialized()) {
        deregister_page_directory(this);
    }
}

}
