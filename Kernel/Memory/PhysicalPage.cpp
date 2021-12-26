/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PhysicalPage.h>

namespace Kernel::Memory {

NonnullRefPtr<PhysicalPage> PhysicalPage::create(PhysicalAddress paddr, MayReturnToFreeList may_return_to_freelist)
{
    auto& physical_page_entry = MM.get_physical_page_entry(paddr);
    return adopt_ref(*new (&physical_page_entry.allocated.physical_page) PhysicalPage(may_return_to_freelist));
}

PhysicalPage::PhysicalPage(MayReturnToFreeList may_return_to_freelist)
    : m_may_return_to_freelist(may_return_to_freelist)
{
}

PhysicalAddress PhysicalPage::paddr() const
{
    return MM.get_physical_address(*this);
}

void PhysicalPage::free_this()
{
    auto paddr = MM.get_physical_address(*this);
    if (m_may_return_to_freelist == MayReturnToFreeList::Yes) {
        auto& this_as_freelist_entry = MM.get_physical_page_entry(paddr).freelist;
        this->~PhysicalPage(); // delete in place
        this_as_freelist_entry.next_index = -1;
        this_as_freelist_entry.prev_index = -1;
        MM.deallocate_physical_page(paddr);
    } else {
        this->~PhysicalPage(); // delete in place
    }
}

}
