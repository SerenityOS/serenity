/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Heap/kmalloc.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>

namespace Kernel {

NonnullRefPtr<PhysicalPage> PhysicalPage::create(PhysicalAddress paddr, bool supervisor, bool may_return_to_freelist)
{
    auto& physical_page_entry = MM.get_physical_page_entry(paddr);
    return adopt_ref(*new (&physical_page_entry.physical_page) PhysicalPage(supervisor, may_return_to_freelist));
}

PhysicalPage::PhysicalPage(bool supervisor, bool may_return_to_freelist)
    : m_may_return_to_freelist(may_return_to_freelist)
    , m_supervisor(supervisor)
{
}

PhysicalAddress PhysicalPage::paddr() const
{
    return MM.get_physical_address(*this);
}

void PhysicalPage::return_to_freelist() const
{
    VERIFY((paddr().get() & ~PAGE_MASK) == 0);

    if (m_supervisor)
        MM.deallocate_supervisor_physical_page(*this);
    else
        MM.deallocate_user_physical_page(*this);
}

}
