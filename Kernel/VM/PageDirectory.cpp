/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <AK/Singleton.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/Sections.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>

extern u8* end_of_kernel_image;

namespace Kernel {

static AK::Singleton<HashMap<FlatPtr, PageDirectory*>> s_cr3_map;

static HashMap<FlatPtr, PageDirectory*>& cr3_map()
{
    VERIFY_INTERRUPTS_DISABLED();
    return *s_cr3_map;
}

RefPtr<PageDirectory> PageDirectory::find_by_cr3(FlatPtr cr3)
{
    ScopedSpinLock lock(s_mm_lock);
    return cr3_map().get(cr3).value_or({});
}

extern "C" FlatPtr kernel_base;
#if ARCH(X86_64)
extern "C" void* boot_pml4t;
#endif
extern "C" void* boot_pdpt;
extern "C" void* boot_pd0;
extern "C" void* boot_pd_kernel;

UNMAP_AFTER_INIT PageDirectory::PageDirectory()
{
    // make sure this starts in a new page directory to make MemoryManager::initialize_physical_pages() happy
    FlatPtr start_of_range = ((FlatPtr)&end_of_kernel_image & ~(FlatPtr)0x1fffff) + 0x200000;
    m_range_allocator.initialize_with_range(VirtualAddress(start_of_range), KERNEL_PD_END - start_of_range);
    m_identity_range_allocator.initialize_with_range(VirtualAddress(FlatPtr(0x00000000)), 0x00200000);
}

UNMAP_AFTER_INIT void PageDirectory::allocate_kernel_directory()
{
    // Adopt the page tables already set up by boot.S
#if ARCH(X86_64)
    PhysicalAddress boot_pml4t_paddr(virtual_to_low_physical((FlatPtr)boot_pml4t));
    dmesgln("MM: boot_pml4t @ {}", boot_pml4t_paddr);
    m_pml4t = PhysicalPage::create(boot_pml4t_paddr, MayReturnToFreeList::No);
#endif
    PhysicalAddress boot_pdpt_paddr(virtual_to_low_physical((FlatPtr)boot_pdpt));
    PhysicalAddress boot_pd0_paddr(virtual_to_low_physical((FlatPtr)boot_pd0));
    PhysicalAddress boot_pd_kernel_paddr(virtual_to_low_physical((FlatPtr)boot_pd_kernel));
    dmesgln("MM: boot_pdpt @ {}", boot_pdpt_paddr);
    dmesgln("MM: boot_pd0 @ {}", boot_pd0_paddr);
    dmesgln("MM: boot_pd_kernel @ {}", boot_pd_kernel_paddr);
    m_directory_table = PhysicalPage::create(boot_pdpt_paddr, MayReturnToFreeList::No);
    m_directory_pages[0] = PhysicalPage::create(boot_pd0_paddr, MayReturnToFreeList::No);
    m_directory_pages[(kernel_base >> 30) & 0x1ff] = PhysicalPage::create(boot_pd_kernel_paddr, MayReturnToFreeList::No);
}

PageDirectory::PageDirectory(const RangeAllocator* parent_range_allocator)
{
    constexpr FlatPtr userspace_range_base = 0x00800000;
    constexpr FlatPtr userspace_range_ceiling = USER_RANGE_CEILING;

    ScopedSpinLock lock(s_mm_lock);
    if (parent_range_allocator) {
        m_range_allocator.initialize_from_parent(*parent_range_allocator);
    } else {
        size_t random_offset = (get_fast_random<u8>() % 32 * MiB) & PAGE_MASK;
        u32 base = userspace_range_base + random_offset;
        m_range_allocator.initialize_with_range(VirtualAddress(base), userspace_range_ceiling - base);
    }

    // Set up a userspace page directory
#if ARCH(X86_64)
    m_pml4t = MM.allocate_user_physical_page();
    if (!m_pml4t)
        return;
#endif
    m_directory_table = MM.allocate_user_physical_page();
    if (!m_directory_table)
        return;
    auto kernel_pd_index = (kernel_base >> 30) & 0x1ffu;
    for (size_t i = 0; i < kernel_pd_index; i++) {
        m_directory_pages[i] = MM.allocate_user_physical_page();
        if (!m_directory_pages[i])
            return;
    }
    // Share the top 1 GiB of kernel-only mappings (>=kernel_base)
    m_directory_pages[kernel_pd_index] = MM.kernel_page_directory().m_directory_pages[kernel_pd_index];

#if ARCH(X86_64)
    {
        auto& table = *(PageDirectoryPointerTable*)MM.quickmap_page(*m_pml4t);
        table.raw[0] = (FlatPtr)m_directory_table->paddr().as_ptr() | 7;
        MM.unquickmap_page();
    }
#endif

    {
        auto& table = *(PageDirectoryPointerTable*)MM.quickmap_page(*m_directory_table);
        for (size_t i = 0; i < sizeof(m_directory_pages) / sizeof(m_directory_pages[0]); i++) {
            if (m_directory_pages[i]) {
#if ARCH(I386)
                table.raw[i] = (FlatPtr)m_directory_pages[i]->paddr().as_ptr() | 1;
#else
                table.raw[i] = (FlatPtr)m_directory_pages[i]->paddr().as_ptr() | 7;
#endif
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

    // Clone bottom 2 MiB of mappings from kernel_page_directory
    PageDirectoryEntry buffer;
    auto* kernel_pd = MM.quickmap_pd(MM.kernel_page_directory(), 0);
    memcpy(&buffer, kernel_pd, sizeof(PageDirectoryEntry));
    auto* new_pd = MM.quickmap_pd(*this, 0);
    memcpy(new_pd, &buffer, sizeof(PageDirectoryEntry));

    // If we got here, we successfully created it. Set m_space now
    m_valid = true;

    cr3_map().set(cr3(), this);
}

PageDirectory::~PageDirectory()
{
    ScopedSpinLock lock(s_mm_lock);
    if (m_space)
        cr3_map().remove(cr3());
}

}
