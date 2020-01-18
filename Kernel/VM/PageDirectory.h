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

class Process;

class PageDirectory : public RefCounted<PageDirectory> {
    friend class MemoryManager;

public:
    static NonnullRefPtr<PageDirectory> create_for_userspace(Process& process, const RangeAllocator* parent_range_allocator = nullptr)
    {
        return adopt(*new PageDirectory(process, parent_range_allocator));
    }
    static NonnullRefPtr<PageDirectory> create_kernel_page_directory() { return adopt(*new PageDirectory); }
    static RefPtr<PageDirectory> find_by_cr3(u32);

    ~PageDirectory();

    u32 cr3() const { return m_directory_table->paddr().get(); }

    RangeAllocator& range_allocator() { return m_range_allocator; }

    Process* process() { return m_process; }
    const Process* process() const { return m_process; }

private:
    PageDirectory(Process&, const RangeAllocator* parent_range_allocator);
    PageDirectory();

    Process* m_process { nullptr };
    RangeAllocator m_range_allocator;
    RefPtr<PhysicalPage> m_directory_table;
    RefPtr<PhysicalPage> m_directory_pages[4];
    HashMap<unsigned, RefPtr<PhysicalPage>> m_physical_pages;
};
