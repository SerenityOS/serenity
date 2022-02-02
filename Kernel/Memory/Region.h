/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/IntrusiveList.h>
#include <AK/IntrusiveRedBlackTree.h>
#include <AK/Weakable.h>
#include <Kernel/Forward.h>
#include <Kernel/KString.h>
#include <Kernel/Memory/PageFaultResponse.h>
#include <Kernel/Memory/VirtualRangeAllocator.h>
#include <Kernel/Sections.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {
class PageFault;
}

namespace Kernel::Memory {

enum class ShouldFlushTLB {
    No,
    Yes,
};

class Region final
    : public Weakable<Region> {
    friend class MemoryManager;

public:
    enum Access : u8 {
        None = 0,
        Read = 1,
        Write = 2,
        Execute = 4,
        HasBeenReadable = 16,
        HasBeenWritable = 32,
        HasBeenExecutable = 64,
        ReadOnly = Read,
        ReadWrite = Read | Write,
        ReadWriteExecute = Read | Write | Execute,
    };

    enum class Cacheable {
        No = 0,
        Yes,
    };

    static ErrorOr<NonnullOwnPtr<Region>> try_create_user_accessible(VirtualRange const&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, Cacheable, bool shared);
    static ErrorOr<NonnullOwnPtr<Region>> try_create_kernel_only(VirtualRange const&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, Cacheable = Cacheable::Yes);

    ~Region();

    [[nodiscard]] VirtualRange const& range() const { return m_range; }
    [[nodiscard]] VirtualAddress vaddr() const { return m_range.base(); }
    [[nodiscard]] size_t size() const { return m_range.size(); }
    [[nodiscard]] bool is_readable() const { return (m_access & Access::Read) == Access::Read; }
    [[nodiscard]] bool is_writable() const { return (m_access & Access::Write) == Access::Write; }
    [[nodiscard]] bool is_executable() const { return (m_access & Access::Execute) == Access::Execute; }

    [[nodiscard]] bool has_been_readable() const { return (m_access & Access::HasBeenReadable) == Access::HasBeenReadable; }
    [[nodiscard]] bool has_been_writable() const { return (m_access & Access::HasBeenWritable) == Access::HasBeenWritable; }
    [[nodiscard]] bool has_been_executable() const { return (m_access & Access::HasBeenExecutable) == Access::HasBeenExecutable; }

    [[nodiscard]] bool is_cacheable() const { return m_cacheable; }
    [[nodiscard]] StringView name() const { return m_name ? m_name->view() : StringView {}; }
    [[nodiscard]] OwnPtr<KString> take_name() { return move(m_name); }
    [[nodiscard]] Region::Access access() const { return static_cast<Region::Access>(m_access); }

    void set_name(OwnPtr<KString> name) { m_name = move(name); }

    [[nodiscard]] VMObject const& vmobject() const { return *m_vmobject; }
    [[nodiscard]] VMObject& vmobject() { return *m_vmobject; }
    void set_vmobject(NonnullRefPtr<VMObject>&&);

    [[nodiscard]] bool is_shared() const { return m_shared; }
    void set_shared(bool shared) { m_shared = shared; }

    [[nodiscard]] bool is_stack() const { return m_stack; }
    void set_stack(bool stack) { m_stack = stack; }

    [[nodiscard]] bool is_mmap() const { return m_mmap; }
    void set_mmap(bool mmap) { m_mmap = mmap; }

    [[nodiscard]] bool is_write_combine() const { return m_write_combine; }
    ErrorOr<void> set_write_combine(bool);

    [[nodiscard]] bool is_user() const { return !is_kernel(); }
    [[nodiscard]] bool is_kernel() const { return vaddr().get() < USER_RANGE_BASE || vaddr().get() >= kernel_mapping_base; }

    PageFaultResponse handle_fault(PageFault const&);

    ErrorOr<NonnullOwnPtr<Region>> try_clone();

    [[nodiscard]] bool contains(VirtualAddress vaddr) const
    {
        return m_range.contains(vaddr);
    }

    [[nodiscard]] bool contains(VirtualRange const& range) const
    {
        return m_range.contains(range);
    }

    [[nodiscard]] unsigned page_index_from_address(VirtualAddress vaddr) const
    {
        return (vaddr - m_range.base()).get() / PAGE_SIZE;
    }

    [[nodiscard]] VirtualAddress vaddr_from_page_index(size_t page_index) const
    {
        return vaddr().offset(page_index * PAGE_SIZE);
    }

    [[nodiscard]] bool translate_vmobject_page(size_t& index) const
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

    [[nodiscard]] ALWAYS_INLINE size_t translate_to_vmobject_page(size_t page_index) const
    {
        return first_page_index() + page_index;
    }

    [[nodiscard]] size_t first_page_index() const
    {
        return m_offset_in_vmobject / PAGE_SIZE;
    }

    [[nodiscard]] size_t page_count() const
    {
        return size() / PAGE_SIZE;
    }

    PhysicalPage const* physical_page(size_t index) const;
    RefPtr<PhysicalPage>& physical_page_slot(size_t index);

    [[nodiscard]] size_t offset_in_vmobject() const
    {
        return m_offset_in_vmobject;
    }

    [[nodiscard]] size_t offset_in_vmobject_from_vaddr(VirtualAddress vaddr) const
    {
        return m_offset_in_vmobject + vaddr.get() - this->vaddr().get();
    }

    [[nodiscard]] size_t amount_resident() const;
    [[nodiscard]] size_t amount_shared() const;
    [[nodiscard]] size_t amount_dirty() const;

    [[nodiscard]] bool should_cow(size_t page_index) const;
    void set_should_cow(size_t page_index, bool);

    [[nodiscard]] size_t cow_pages() const;

    void set_readable(bool b) { set_access_bit(Access::Read, b); }
    void set_writable(bool b) { set_access_bit(Access::Write, b); }
    void set_executable(bool b) { set_access_bit(Access::Execute, b); }

    void unsafe_clear_access() { m_access = Region::None; }

    void set_page_directory(PageDirectory&);
    ErrorOr<void> map(PageDirectory&, ShouldFlushTLB = ShouldFlushTLB::Yes);
    enum class ShouldDeallocateVirtualRange {
        No,
        Yes,
    };
    void unmap(ShouldDeallocateVirtualRange, ShouldFlushTLB = ShouldFlushTLB::Yes);
    void unmap_with_locks_held(ShouldDeallocateVirtualRange, ShouldFlushTLB, SpinlockLocker<RecursiveSpinlock>& pd_locker, SpinlockLocker<RecursiveSpinlock>& mm_locker);

    void remap();

    void clear_to_zero();

    [[nodiscard]] bool is_syscall_region() const { return m_syscall_region; }
    void set_syscall_region(bool b) { m_syscall_region = b; }

private:
    Region(VirtualRange const&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, OwnPtr<KString>, Region::Access access, Cacheable, bool shared);

    [[nodiscard]] bool remap_vmobject_page(size_t page_index, bool with_flush = true);
    [[nodiscard]] bool do_remap_vmobject_page(size_t page_index, bool with_flush = true);

    void set_access_bit(Access access, bool b)
    {
        if (b)
            m_access |= access | (access << 4);
        else
            m_access &= ~access;
    }

    [[nodiscard]] PageFaultResponse handle_cow_fault(size_t page_index);
    [[nodiscard]] PageFaultResponse handle_inode_fault(size_t page_index);
    [[nodiscard]] PageFaultResponse handle_zero_fault(size_t page_index);

    [[nodiscard]] bool map_individual_page_impl(size_t page_index);

    RefPtr<PageDirectory> m_page_directory;
    VirtualRange m_range;
    size_t m_offset_in_vmobject { 0 };
    NonnullRefPtr<VMObject> m_vmobject;
    OwnPtr<KString> m_name;
    u8 m_access { Region::None };
    bool m_shared : 1 { false };
    bool m_cacheable : 1 { false };
    bool m_stack : 1 { false };
    bool m_mmap : 1 { false };
    bool m_syscall_region : 1 { false };
    bool m_write_combine : 1 { false };

    IntrusiveRedBlackTreeNode<FlatPtr, Region, RawPtr<Region>> m_tree_node;
    IntrusiveListNode<Region> m_vmobject_list_node;

public:
    using ListInVMObject = IntrusiveList<&Region::m_vmobject_list_node>;
};

AK_ENUM_BITWISE_OPERATORS(Region::Access)

constexpr Region::Access prot_to_region_access_flags(int prot)
{
    Region::Access access = Region::Access::None;
    if ((prot & PROT_READ) == PROT_READ)
        access |= Region::Access::Read;
    if ((prot & PROT_WRITE) == PROT_WRITE)
        access |= Region::Access::Write;
    if ((prot & PROT_EXEC) == PROT_EXEC)
        access |= Region::Access::Execute;
    return access;
}

constexpr int region_access_flags_to_prot(Region::Access access)
{
    int prot = 0;
    if ((access & Region::Access::Read) == Region::Access::Read)
        prot |= PROT_READ;
    if ((access & Region::Access::Write) == Region::Access::Write)
        prot |= PROT_WRITE;
    if ((access & Region::Access::Execute) == Region::Access::Execute)
        prot |= PROT_EXEC;
    return prot;
}

}
