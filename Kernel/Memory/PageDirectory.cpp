/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <AK/Singleton.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PageDirectory.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/Sections.h>

extern u8 end_of_kernel_image[];

namespace Kernel::Memory {

static Singleton<IntrusiveRedBlackTree<&PageDirectory::m_tree_node>> s_cr3_map;

static IntrusiveRedBlackTree<&PageDirectory::m_tree_node>& cr3_map()
{
    VERIFY_INTERRUPTS_DISABLED();
    return *s_cr3_map;
}

RefPtr<PageDirectory> PageDirectory::find_by_cr3(FlatPtr cr3)
{
    SpinlockLocker lock(s_mm_lock);
    return cr3_map().find(cr3);
}

UNMAP_AFTER_INIT NonnullRefPtr<PageDirectory> PageDirectory::must_create_kernel_page_directory()
{
    auto directory = adopt_ref_if_nonnull(new (nothrow) PageDirectory).release_nonnull();

    // make sure this starts in a new page directory to make MemoryManager::initialize_physical_pages() happy
    FlatPtr start_of_range = ((FlatPtr)end_of_kernel_image & ~(FlatPtr)0x1fffff) + 0x200000;
    MUST(directory->m_range_allocator.initialize_with_range(VirtualAddress(start_of_range), KERNEL_PD_END - start_of_range));

    return directory;
}

ErrorOr<NonnullRefPtr<PageDirectory>> PageDirectory::try_create_for_userspace(VirtualRangeAllocator const* parent_range_allocator)
{
    constexpr FlatPtr userspace_range_base = USER_RANGE_BASE;
    FlatPtr const userspace_range_ceiling = USER_RANGE_CEILING;

    auto directory = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PageDirectory));

    if (parent_range_allocator) {
        TRY(directory->m_range_allocator.initialize_from_parent(*parent_range_allocator));
    } else {
        size_t random_offset = (get_fast_random<u8>() % 32 * MiB) & PAGE_MASK;
        u32 base = userspace_range_base + random_offset;
        TRY(directory->m_range_allocator.initialize_with_range(VirtualAddress(base), userspace_range_ceiling - base));
    }

    // NOTE: Take the MM lock since we need it for quickmap.
    SpinlockLocker lock(s_mm_lock);

#if ARCH(X86_64)
    directory->m_pml4t = TRY(MM.allocate_user_physical_page());
#endif

    directory->m_directory_table = TRY(MM.allocate_user_physical_page());
    auto kernel_pd_index = (kernel_mapping_base >> 30) & 0x1ffu;
    for (size_t i = 0; i < kernel_pd_index; i++) {
        directory->m_directory_pages[i] = TRY(MM.allocate_user_physical_page());
    }

    // Share the top 1 GiB of kernel-only mappings (>=kernel_mapping_base)
    directory->m_directory_pages[kernel_pd_index] = MM.kernel_page_directory().m_directory_pages[kernel_pd_index];

#if ARCH(X86_64)
    {
        auto& table = *(PageDirectoryPointerTable*)MM.quickmap_page(*directory->m_pml4t);
        table.raw[0] = (FlatPtr)directory->m_directory_table->paddr().as_ptr() | 7;
        MM.unquickmap_page();
    }
#endif

    {
        auto& table = *(PageDirectoryPointerTable*)MM.quickmap_page(*directory->m_directory_table);
        for (size_t i = 0; i < sizeof(m_directory_pages) / sizeof(m_directory_pages[0]); i++) {
            if (directory->m_directory_pages[i]) {
#if ARCH(I386)
                table.raw[i] = (FlatPtr)directory->m_directory_pages[i]->paddr().as_ptr() | 1;
#else
                table.raw[i] = (FlatPtr)directory->m_directory_pages[i]->paddr().as_ptr() | 7;
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

    cr3_map().insert(directory->cr3(), directory);
    return directory;
}

PageDirectory::PageDirectory()
{
}

UNMAP_AFTER_INIT void PageDirectory::allocate_kernel_directory()
{
    // Adopt the page tables already set up by boot.S
#if ARCH(X86_64)
    dmesgln("MM: boot_pml4t @ {}", boot_pml4t);
    m_pml4t = PhysicalPage::create(boot_pml4t, MayReturnToFreeList::No);
#endif
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
        SpinlockLocker lock(s_mm_lock);
        cr3_map().remove(cr3());
    }
}

}
