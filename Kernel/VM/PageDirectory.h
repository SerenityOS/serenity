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

#pragma once

#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/RangeAllocator.h>

namespace Kernel {

class Process;

class PageDirectory : public RefCounted<PageDirectory> {
    friend class MemoryManager;

public:
    static RefPtr<PageDirectory> create_for_userspace(const RangeAllocator* parent_range_allocator = nullptr)
    {
        auto page_directory = adopt(*new PageDirectory(parent_range_allocator));
        if (!page_directory->is_valid())
            return {};
        return page_directory;
    }
    static NonnullRefPtr<PageDirectory> create_kernel_page_directory() { return adopt(*new PageDirectory); }
    static RefPtr<PageDirectory> find_by_cr3(u32);

    ~PageDirectory();

    u32 cr3() const { return m_directory_table->paddr().get(); }

    RangeAllocator& range_allocator() { return m_range_allocator; }
    const RangeAllocator& range_allocator() const { return m_range_allocator; }

    RangeAllocator& identity_range_allocator() { return m_identity_range_allocator; }

    bool is_valid() const { return m_valid; }

    Space* space() { return m_space; }
    const Space* space() const { return m_space; }

    void set_space(Badge<Space>, Space& space) { m_space = &space; }

    RecursiveSpinLock& get_lock() { return m_lock; }

private:
    explicit PageDirectory(const RangeAllocator* parent_range_allocator);
    PageDirectory();

    Space* m_space { nullptr };
    RangeAllocator m_range_allocator;
    RangeAllocator m_identity_range_allocator;
    RefPtr<PhysicalPage> m_directory_table;
    RefPtr<PhysicalPage> m_directory_pages[4];
    HashMap<u32, RefPtr<PhysicalPage>> m_page_tables;
    RecursiveSpinLock m_lock;
    bool m_valid { false };
};

}
