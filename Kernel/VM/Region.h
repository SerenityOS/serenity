/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/IntrusiveList.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/Arch/x86/PageFault.h>
#include <Kernel/Forward.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/KString.h>
#include <Kernel/Sections.h>
#include <Kernel/VM/PageFaultResponse.h>
#include <Kernel/VM/PurgeablePageRanges.h>
#include <Kernel/VM/RangeAllocator.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {

enum class ShouldFlushTLB {
    No,
    Yes,
};

class Region final
    : public Weakable<Region>
    , public PurgeablePageRanges {
    friend class MemoryManager;

    MAKE_SLAB_ALLOCATED(Region)
public:
    enum Access : u8 {
        None = 0,
        Read = 1,
        Write = 2,
        Execute = 4,
        HasBeenReadable = 16,
        HasBeenWritable = 32,
        HasBeenExecutable = 64,
    };

    enum class Cacheable {
        No = 0,
        Yes,
    };

    static NonnullOwnPtr<Region> create_user_accessible(Process*, const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, Cacheable, bool shared);
    static OwnPtr<Region> create_kernel_only(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, Cacheable = Cacheable::Yes);

    ~Region();

    const Range& range() const { return m_range; }
    VirtualAddress vaddr() const { return m_range.base(); }
    size_t size() const { return m_range.size(); }
    bool is_readable() const { return m_access & Access::Read; }
    bool is_writable() const { return m_access & Access::Write; }
    bool is_executable() const { return m_access & Access::Execute; }

    bool has_been_readable() const { return m_access & Access::HasBeenReadable; }
    bool has_been_writable() const { return m_access & Access::HasBeenWritable; }
    bool has_been_executable() const { return m_access & Access::HasBeenExecutable; }

    bool is_cacheable() const { return m_cacheable; }
    StringView name() const { return m_name ? m_name->view() : StringView {}; }
    OwnPtr<KString> take_name() { return move(m_name); }
    Region::Access access() const { return static_cast<Region::Access>(m_access); }

    void set_name(OwnPtr<KString> name) { m_name = move(name); }

    const VMObject& vmobject() const { return *m_vmobject; }
    VMObject& vmobject() { return *m_vmobject; }
    void set_vmobject(NonnullRefPtr<VMObject>&&);

    bool is_shared() const { return m_shared; }
    void set_shared(bool shared) { m_shared = shared; }

    bool is_stack() const { return m_stack; }
    void set_stack(bool stack) { m_stack = stack; }

    bool is_mmap() const { return m_mmap; }
    void set_mmap(bool mmap) { m_mmap = mmap; }

    bool is_user() const { return !is_kernel(); }
    bool is_kernel() const { return vaddr().get() < 0x00800000 || vaddr().get() >= KERNEL_BASE; }

    PageFaultResponse handle_fault(const PageFault&, ScopedSpinLock<RecursiveSpinLock>&);

    OwnPtr<Region> clone(Process&);

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

    bool translate_vmobject_page(size_t& index) const
    {
        auto first_index = first_page_index();
        if (index < first_index) {
            index = first_index;
            return false;
        }
        index -= first_index;
        auto total_page_count = this->page_count();
        if (index >= total_page_count) {
            index = first_index + total_page_count - 1;
            return false;
        }
        return true;
    }

    bool translate_vmobject_page_range(size_t& index, size_t& page_count) const
    {
        auto first_index = first_page_index();
        if (index < first_index) {
            auto delta = first_index - index;
            index = first_index;
            if (delta >= page_count) {
                page_count = 0;
                return false;
            }
            page_count -= delta;
        }
        index -= first_index;
        auto total_page_count = this->page_count();
        if (index + page_count > total_page_count) {
            page_count = total_page_count - index;
            if (page_count == 0)
                return false;
        }
        return true;
    }

    ALWAYS_INLINE size_t translate_to_vmobject_page(size_t page_index) const
    {
        return first_page_index() + page_index;
    }

    size_t first_page_index() const
    {
        return m_offset_in_vmobject / PAGE_SIZE;
    }

    size_t page_count() const
    {
        return size() / PAGE_SIZE;
    }

    const PhysicalPage* physical_page(size_t index) const
    {
        VERIFY(index < page_count());
        return vmobject().physical_pages()[first_page_index() + index];
    }

    RefPtr<PhysicalPage>& physical_page_slot(size_t index)
    {
        VERIFY(index < page_count());
        return vmobject().physical_pages()[first_page_index() + index];
    }

    size_t offset_in_vmobject() const
    {
        return m_offset_in_vmobject;
    }

    size_t offset_in_vmobject_from_vaddr(VirtualAddress vaddr) const
    {
        return m_offset_in_vmobject + vaddr.get() - this->vaddr().get();
    }

    size_t amount_resident() const;
    size_t amount_shared() const;
    size_t amount_dirty() const;

    bool should_cow(size_t page_index) const;
    void set_should_cow(size_t page_index, bool);

    size_t cow_pages() const;

    void set_readable(bool b) { set_access_bit(Access::Read, b); }
    void set_writable(bool b) { set_access_bit(Access::Write, b); }
    void set_executable(bool b) { set_access_bit(Access::Execute, b); }

    void set_page_directory(PageDirectory&);
    bool map(PageDirectory&, ShouldFlushTLB = ShouldFlushTLB::Yes);
    enum class ShouldDeallocateVirtualMemoryRange {
        No,
        Yes,
    };
    void unmap(ShouldDeallocateVirtualMemoryRange = ShouldDeallocateVirtualMemoryRange::Yes);

    void remap();

    bool remap_vmobject_page_range(size_t page_index, size_t page_count);

    bool is_volatile(VirtualAddress vaddr, size_t size) const;
    enum class SetVolatileError {
        Success = 0,
        NotPurgeable,
        OutOfMemory
    };
    SetVolatileError set_volatile(VirtualAddress vaddr, size_t size, bool is_volatile, bool& was_purged);

    RefPtr<Process> get_owner();

    bool is_syscall_region() const { return m_syscall_region; }
    void set_syscall_region(bool b) { m_syscall_region = b; }

private:
    Region(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, OwnPtr<KString>, Region::Access access, Cacheable, bool shared);

    bool do_remap_vmobject_page_range(size_t page_index, size_t page_count);

    void set_access_bit(Access access, bool b)
    {
        if (b)
            m_access |= access | (access << 4);
        else
            m_access &= ~access;
    }

    bool do_remap_vmobject_page(size_t index, bool with_flush = true);
    bool remap_vmobject_page(size_t index, bool with_flush = true);

    PageFaultResponse handle_cow_fault(size_t page_index);
    PageFaultResponse handle_inode_fault(size_t page_index, ScopedSpinLock<RecursiveSpinLock>&);
    PageFaultResponse handle_zero_fault(size_t page_index);

    bool map_individual_page_impl(size_t page_index);

    void register_purgeable_page_ranges();
    void unregister_purgeable_page_ranges();

    RefPtr<PageDirectory> m_page_directory;
    Range m_range;
    size_t m_offset_in_vmobject { 0 };
    NonnullRefPtr<VMObject> m_vmobject;
    OwnPtr<KString> m_name;
    u8 m_access { Region::None };
    bool m_shared : 1 { false };
    bool m_cacheable : 1 { false };
    bool m_stack : 1 { false };
    bool m_mmap : 1 { false };
    bool m_syscall_region : 1 { false };
    WeakPtr<Process> m_owner;
    IntrusiveListNode<Region> m_list_node;

public:
    using List = IntrusiveList<Region, RawPtr<Region>, &Region::m_list_node>;
};

AK_ENUM_BITWISE_OPERATORS(Region::Access)

inline Region::Access prot_to_region_access_flags(int prot)
{
    Region::Access access = Region::Access::None;
    if (prot & PROT_READ)
        access |= Region::Access::Read;
    if (prot & PROT_WRITE)
        access |= Region::Access::Write;
    if (prot & PROT_EXEC)
        access |= Region::Access::Execute;
    return access;
}

inline int region_access_flags_to_prot(Region::Access access)
{
    int prot = 0;
    if (access & Region::Access::Read)
        prot |= PROT_READ;
    if (access & Region::Access::Write)
        prot |= PROT_WRITE;
    if (access & Region::Access::Execute)
        prot |= PROT_EXEC;
    return prot;
}

}
