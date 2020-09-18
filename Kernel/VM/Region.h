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

#include <AK/InlineLinkedList.h>
#include <AK/String.h>
#include <AK/Weakable.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/VM/RangeAllocator.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {

class Inode;
class VMObject;

enum class PageFaultResponse {
    ShouldCrash,
    OutOfMemory,
    Continue,
};

class Region final
    : public InlineLinkedListNode<Region>
    , public Weakable<Region> {
    friend class MemoryManager;

    MAKE_SLAB_ALLOCATED(Region)
public:
    enum Access {
        Read = 1,
        Write = 2,
        Execute = 4,
    };

    enum class InheritMode {
        Default,
        ZeroedOnFork,
    };

    static NonnullOwnPtr<Region> create_user_accessible(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const StringView& name, u8 access, bool cacheable = true);
    static NonnullOwnPtr<Region> create_kernel_only(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const StringView& name, u8 access, bool cacheable = true);

    ~Region();

    const Range& range() const { return m_range; }
    VirtualAddress vaddr() const { return m_range.base(); }
    size_t size() const { return m_range.size(); }
    bool is_readable() const { return m_access & Access::Read; }
    bool is_writable() const { return m_access & Access::Write; }
    bool is_executable() const { return m_access & Access::Execute; }
    bool is_cacheable() const { return m_cacheable; }
    const String& name() const { return m_name; }
    unsigned access() const { return m_access; }

    void set_name(const String& name) { m_name = name; }
    void set_name(String&& name) { m_name = move(name); }

    const VMObject& vmobject() const { return *m_vmobject; }
    VMObject& vmobject() { return *m_vmobject; }
    void set_vmobject(NonnullRefPtr<VMObject>&& obj) { m_vmobject = obj; }

    bool is_shared() const { return m_shared; }
    void set_shared(bool shared) { m_shared = shared; }

    bool is_stack() const { return m_stack; }
    void set_stack(bool stack) { m_stack = stack; }

    bool is_mmap() const { return m_mmap; }
    void set_mmap(bool mmap) { m_mmap = mmap; }

    bool is_user_accessible() const { return m_user_accessible; }
    void set_user_accessible(bool b) { m_user_accessible = b; }

    bool is_kernel() const { return m_kernel || vaddr().get() >= 0xc0000000; }
    void set_kernel(bool kernel) { m_kernel = kernel; }

    PageFaultResponse handle_fault(const PageFault&);

    NonnullOwnPtr<Region> clone();

    bool contains(VirtualAddress vaddr) const
    {
        return m_range.contains(vaddr);
    }

    bool contains(const Range& range) const
    {
        return m_range.contains(range);
    }

    unsigned page_index_from_address(VirtualAddress vaddr) const
    {
        return (vaddr - m_range.base()).get() / PAGE_SIZE;
    }

    VirtualAddress vaddr_from_page_index(size_t page_index) const
    {
        return vaddr().offset(page_index * PAGE_SIZE);
    }

    size_t first_page_index() const
    {
        return m_offset_in_vmobject / PAGE_SIZE;
    }

    size_t last_page_index() const
    {
        return (first_page_index() + page_count()) - 1;
    }

    size_t page_count() const
    {
        return size() / PAGE_SIZE;
    }

    const PhysicalPage* physical_page(size_t index) const
    {
        ASSERT(index < page_count());
        return vmobject().physical_pages()[first_page_index() + index];
    }

    RefPtr<PhysicalPage>& physical_page_slot(size_t index)
    {
        ASSERT(index < page_count());
        return vmobject().physical_pages()[first_page_index() + index];
    }

    size_t offset_in_vmobject() const
    {
        return m_offset_in_vmobject;
    }

    bool commit();

    size_t amount_resident() const;
    size_t amount_shared() const;
    size_t amount_dirty() const;

    bool should_cow(size_t page_index) const;
    void set_should_cow(size_t page_index, bool);

    u32 cow_pages() const;

    void set_readable(bool b) { set_access_bit(Access::Read, b); }
    void set_writable(bool b) { set_access_bit(Access::Write, b); }
    void set_executable(bool b) { set_access_bit(Access::Execute, b); }

    void set_page_directory(PageDirectory&);
    bool map(PageDirectory&);
    enum class ShouldDeallocateVirtualMemoryRange {
        No,
        Yes,
    };
    void unmap(ShouldDeallocateVirtualMemoryRange = ShouldDeallocateVirtualMemoryRange::Yes);

    void remap();

    // For InlineLinkedListNode
    Region* m_next { nullptr };
    Region* m_prev { nullptr };

    // NOTE: These are public so we can make<> them.
    Region(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const String&, u8 access, bool cacheable, bool kernel);

    void set_inherit_mode(InheritMode inherit_mode) { m_inherit_mode = inherit_mode; }

private:
    Bitmap& ensure_cow_map() const;

    void set_access_bit(Access access, bool b)
    {
        if (b)
            m_access |= access;
        else
            m_access &= ~access;
    }

    bool commit(size_t page_index);
    bool remap_page(size_t index, bool with_flush = true);

    PageFaultResponse handle_cow_fault(size_t page_index);
    PageFaultResponse handle_inode_fault(size_t page_index);
    PageFaultResponse handle_zero_fault(size_t page_index);

    bool map_individual_page_impl(size_t page_index);

    RefPtr<PageDirectory> m_page_directory;
    Range m_range;
    size_t m_offset_in_vmobject { 0 };
    NonnullRefPtr<VMObject> m_vmobject;
    String m_name;
    u8 m_access { 0 };
    InheritMode m_inherit_mode : 3 { InheritMode::Default };
    bool m_shared : 1 { false };
    bool m_user_accessible : 1 { false };
    bool m_cacheable : 1 { false };
    bool m_stack : 1 { false };
    bool m_mmap : 1 { false };
    bool m_kernel : 1 { false };
    mutable OwnPtr<Bitmap> m_cow_map;
};

inline unsigned prot_to_region_access_flags(int prot)
{
    unsigned access = 0;
    if (prot & PROT_READ)
        access |= Region::Access::Read;
    if (prot & PROT_WRITE)
        access |= Region::Access::Write;
    if (prot & PROT_EXEC)
        access |= Region::Access::Execute;
    return access;
}

}
