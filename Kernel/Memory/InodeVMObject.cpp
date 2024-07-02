/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Memory/InodeVMObject.h>

namespace Kernel::Memory {

InodeVMObject::InodeVMObject(Inode& inode, FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages, Bitmap dirty_pages)
    : VMObject(move(new_physical_pages))
    , m_inode(inode)
    , m_dirty_pages(move(dirty_pages))
{
}

InodeVMObject::InodeVMObject(InodeVMObject const& other, FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages, Bitmap dirty_pages)
    : VMObject(move(new_physical_pages))
    , m_inode(other.m_inode)
    , m_dirty_pages(move(dirty_pages))
{
    for (size_t i = 0; i < page_count(); ++i)
        m_dirty_pages.set(i, other.m_dirty_pages.get(i));
}

InodeVMObject::~InodeVMObject() = default;

size_t InodeVMObject::amount_clean() const
{
    size_t count = 0;
    VERIFY(page_count() == m_dirty_pages.size());
    for (size_t i = 0; i < page_count(); ++i) {
        if (!m_dirty_pages.get(i) && m_physical_pages[i])
            ++count;
    }
    return count * PAGE_SIZE;
}

size_t InodeVMObject::amount_dirty() const
{
    size_t count = 0;
    for (size_t i = 0; i < m_dirty_pages.size(); ++i) {
        if (m_dirty_pages.get(i))
            ++count;
    }
    return count * PAGE_SIZE;
}

bool InodeVMObject::is_page_dirty(size_t page_index) const
{
    VERIFY(m_lock.is_locked());
    return m_dirty_pages.get(page_index);
}

void InodeVMObject::set_page_dirty(size_t page_index, bool is_dirty)
{
    VERIFY(m_lock.is_locked());
    m_dirty_pages.set(page_index, is_dirty);
}

int InodeVMObject::release_all_clean_pages()
{
    return try_release_clean_pages(page_count());
}

int InodeVMObject::try_release_clean_pages(int page_amount)
{
    SpinlockLocker locker(m_lock);

    int count = 0;
    for (size_t i = 0; i < page_count() && count < page_amount; ++i) {
        if (!m_dirty_pages.get(i) && m_physical_pages[i]) {
            m_physical_pages[i] = nullptr;
            ++count;
        }
    }
    if (count)
        remap_regions();
    return count;
}

u32 InodeVMObject::writable_mappings() const
{
    u32 count = 0;
    const_cast<InodeVMObject&>(*this).for_each_region([&](auto& region) {
        if (region.is_writable())
            ++count;
    });
    return count;
}

}
