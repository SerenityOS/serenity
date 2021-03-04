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
