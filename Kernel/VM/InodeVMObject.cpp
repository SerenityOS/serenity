/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/VM/InodeVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Region.h>

namespace Kernel {

InodeVMObject::InodeVMObject(Inode& inode, size_t size)
    : VMObject(size)
    , m_inode(inode)
    , m_dirty_pages(page_count(), false)
{
}

InodeVMObject::InodeVMObject(const InodeVMObject& other)
    : VMObject(other)
    , m_inode(other.m_inode)
    , m_dirty_pages(page_count(), false)
{
    for (size_t i = 0; i < page_count(); ++i)
        m_dirty_pages.set(i, other.m_dirty_pages.get(i));
}

InodeVMObject::~InodeVMObject()
{
}

size_t InodeVMObject::amount_clean() const
{
    size_t count = 0;
    ASSERT(page_count() == m_dirty_pages.size());
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

void InodeVMObject::inode_size_changed(Badge<Inode>, size_t old_size, size_t new_size)
{
    dbg() << "VMObject::inode_size_changed: {" << m_inode->fsid() << ":" << m_inode->index() << "} " << old_size << " -> " << new_size;

    InterruptDisabler disabler;

    auto new_page_count = PAGE_ROUND_UP(new_size) / PAGE_SIZE;
    m_physical_pages.resize(new_page_count);

    m_dirty_pages.grow(new_page_count, false);

    // FIXME: Consolidate with inode_contents_changed() so we only do a single walk.
    for_each_region([](auto& region) {
        region.remap();
    });
}

void InodeVMObject::inode_contents_changed(Badge<Inode>, off_t offset, ssize_t size, const UserOrKernelBuffer& data)
{
    (void)size;
    (void)data;
    InterruptDisabler disabler;
    ASSERT(offset >= 0);

    // FIXME: Only invalidate the parts that actually changed.
    for (auto& physical_page : m_physical_pages)
        physical_page = nullptr;

#if 0
    size_t current_offset = offset;
    size_t remaining_bytes = size;
    const u8* data_ptr = data;

    auto to_page_index = [] (size_t offset) -> size_t {
        return offset / PAGE_SIZE;
    };

    if (current_offset & PAGE_MASK) {
        size_t page_index = to_page_index(current_offset);
        size_t bytes_to_copy = min(size, PAGE_SIZE - (current_offset & PAGE_MASK));
        if (m_physical_pages[page_index]) {
            auto* ptr = MM.quickmap_page(*m_physical_pages[page_index]);
            memcpy(ptr, data_ptr, bytes_to_copy);
            MM.unquickmap_page();
        }
        current_offset += bytes_to_copy;
        data += bytes_to_copy;
        remaining_bytes -= bytes_to_copy;
    }

    for (size_t page_index = to_page_index(current_offset); page_index < m_physical_pages.size(); ++page_index) {
        size_t bytes_to_copy = PAGE_SIZE - (current_offset & PAGE_MASK);
        if (m_physical_pages[page_index]) {
            auto* ptr = MM.quickmap_page(*m_physical_pages[page_index]);
            memcpy(ptr, data_ptr, bytes_to_copy);
            MM.unquickmap_page();
        }
        current_offset += bytes_to_copy;
        data += bytes_to_copy;
    }
#endif

    // FIXME: Consolidate with inode_size_changed() so we only do a single walk.
    for_each_region([](auto& region) {
        region.remap();
    });
}

int InodeVMObject::release_all_clean_pages()
{
    LOCKER(m_paging_lock);
    return release_all_clean_pages_impl();
}

int InodeVMObject::release_all_clean_pages_impl()
{
    int count = 0;
    InterruptDisabler disabler;
    for (size_t i = 0; i < page_count(); ++i) {
        if (!m_dirty_pages.get(i) && m_physical_pages[i]) {
            m_physical_pages[i] = nullptr;
            ++count;
        }
    }
    for_each_region([](auto& region) {
        region.remap();
    });
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

u32 InodeVMObject::executable_mappings() const
{
    u32 count = 0;
    const_cast<InodeVMObject&>(*this).for_each_region([&](auto& region) {
        if (region.is_executable())
            ++count;
    });
    return count;
}

}
