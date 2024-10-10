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
#include <AK/SetOnce.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/KString.h>
#include <Kernel/Library/LockWeakable.h>
#include <Kernel/Locking/LockRank.h>
#include <Kernel/Memory/MemoryType.h>
#include <Kernel/Memory/PageFaultResponse.h>
#include <Kernel/Memory/VirtualRange.h>
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
    : public LockWeakable<Region> {
    friend class AddressSpace;
    friend class MemoryManager;
    friend class RegionTree;
    friend class AnonymousVMObject;
    friend class VMObject;

public:
    enum Access : u8 {
        None = 0,
        Read = 1,
        Write = 2,
        Execute = 4,
        ReadOnly = Read,
        ReadWrite = Read | Write,
        ReadWriteExecute = Read | Write | Execute,
    };

    static ErrorOr<NonnullOwnPtr<Region>> try_create_user_accessible(VirtualRange const&, NonnullLockRefPtr<VMObject>, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, MemoryType, bool shared);
    static ErrorOr<NonnullOwnPtr<Region>> create_unbacked();
    static ErrorOr<NonnullOwnPtr<Region>> create_unplaced(NonnullLockRefPtr<VMObject>, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, MemoryType = MemoryType::Normal, bool shared = false);

    ~Region();

    [[nodiscard]] VirtualRange const& range() const { return m_range; }
    [[nodiscard]] VirtualAddress vaddr() const { return m_range.base(); }
    [[nodiscard]] size_t size() const { return m_range.size(); }
    [[nodiscard]] bool is_readable() const { return (m_access & Access::Read) == Access::Read; }
    [[nodiscard]] bool is_writable() const { return (m_access & Access::Write) == Access::Write; }
    [[nodiscard]] bool is_executable() const { return (m_access & Access::Execute) == Access::Execute; }

    [[nodiscard]] bool has_been_readable() const { return m_has_been_readable.was_set(); }
    [[nodiscard]] bool has_been_writable() const { return m_has_been_writable.was_set(); }
    [[nodiscard]] bool has_been_executable() const { return m_has_been_executable.was_set(); }

    [[nodiscard]] MemoryType memory_type() const { return m_memory_type; }
    [[nodiscard]] StringView name() const { return m_name ? m_name->view() : StringView {}; }
    [[nodiscard]] OwnPtr<KString> take_name() { return move(m_name); }
    [[nodiscard]] Region::Access access() const { return static_cast<Region::Access>(m_access); }

    void set_name(OwnPtr<KString> name) { m_name = move(name); }

    [[nodiscard]] VMObject const& vmobject() const { return *m_vmobject; }
    [[nodiscard]] VMObject& vmobject() { return *m_vmobject; }
    void set_vmobject(NonnullLockRefPtr<VMObject>&&);

    [[nodiscard]] bool is_shared() const { return m_shared; }
    void set_shared(bool shared) { m_shared = shared; }

    [[nodiscard]] bool is_stack() const { return m_stack; }
    void set_stack(bool stack) { m_stack = stack; }

    [[nodiscard]] bool is_immutable() const { return m_immutable.was_set(); }
    void set_immutable() { m_immutable.set(); }

    [[nodiscard]] bool is_mmap() const { return m_mmap; }

    void set_mmap(bool mmap, bool description_was_readable, bool description_was_writable)
    {
        m_mmap = mmap;
        m_mmapped_from_readable = description_was_readable;
        m_mmapped_from_writable = description_was_writable;
    }

    [[nodiscard]] bool is_initially_loaded_executable_segment() const { return m_initially_loaded_executable_segment.was_set(); }
    void set_initially_loaded_executable_segment() { m_initially_loaded_executable_segment.set(); }

    [[nodiscard]] bool is_user() const { return !is_kernel(); }
    [[nodiscard]] bool is_kernel() const { return vaddr().get() < USER_RANGE_BASE || vaddr().get() >= g_boot_info.kernel_mapping_base; }

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

    RefPtr<PhysicalRAMPage> physical_page(size_t index) const;
    RefPtr<PhysicalRAMPage>& physical_page_slot(size_t index);

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

    [[nodiscard]] size_t cow_pages() const;

    [[nodiscard]] bool should_dirty_on_write(size_t page_index) const;

    void set_readable(bool b)
    {
        set_access_bit(Access::Read, b);
        if (b)
            m_has_been_readable.set();
    }
    void set_writable(bool b)
    {
        set_access_bit(Access::Write, b);
        if (b)
            m_has_been_writable.set();
    }
    void set_executable(bool b)
    {
        set_access_bit(Access::Execute, b);
        if (b)
            m_has_been_executable.set();
    }

    void unsafe_clear_access() { m_access = Region::None; }

    void set_page_directory(PageDirectory&);
    ErrorOr<void> map(PageDirectory&, ShouldFlushTLB = ShouldFlushTLB::Yes);
    ErrorOr<void> map(PageDirectory&, PhysicalAddress, ShouldFlushTLB = ShouldFlushTLB::Yes);
    void unmap(ShouldFlushTLB = ShouldFlushTLB::Yes);
    void unmap_with_locks_held(ShouldFlushTLB, SpinlockLocker<RecursiveSpinlock<LockRank::None>>& pd_locker);

    void remap();

    [[nodiscard]] bool is_mapped() const { return m_page_directory != nullptr; }

    void clear_to_zero();

    [[nodiscard]] bool is_syscall_region() const { return m_syscall_region; }
    void set_syscall_region(bool b) { m_syscall_region = b; }

    [[nodiscard]] bool mmapped_from_readable() const { return m_mmapped_from_readable; }
    [[nodiscard]] bool mmapped_from_writable() const { return m_mmapped_from_writable; }

    void start_handling_page_fault(Badge<MemoryManager>) { m_in_progress_page_faults++; }
    void finish_handling_page_fault(Badge<MemoryManager>) { m_in_progress_page_faults--; }

private:
    Region();
    Region(NonnullLockRefPtr<VMObject>, size_t offset_in_vmobject, OwnPtr<KString>, Region::Access access, MemoryType, bool shared);
    Region(VirtualRange const&, NonnullLockRefPtr<VMObject>, size_t offset_in_vmobject, OwnPtr<KString>, Region::Access access, MemoryType, bool shared);

    [[nodiscard]] bool remap_vmobject_page(size_t page_index, NonnullRefPtr<PhysicalRAMPage>);

    void set_access_bit(Access access, bool b)
    {
        if (b)
            m_access |= access;
        else
            m_access &= ~access;
    }

    [[nodiscard]] PageFaultResponse handle_cow_fault(size_t page_index);
    [[nodiscard]] PageFaultResponse handle_inode_fault(size_t page_index, bool mark_page_dirty = false);
    [[nodiscard]] PageFaultResponse handle_zero_fault(size_t page_index, PhysicalRAMPage& page_in_slot_at_time_of_fault);
    [[nodiscard]] PageFaultResponse handle_dirty_on_write_fault(size_t page_index);

    [[nodiscard]] bool map_individual_page_impl(size_t page_index);
    [[nodiscard]] bool map_individual_page_impl(size_t page_index, RefPtr<PhysicalRAMPage>);
    [[nodiscard]] bool map_individual_page_impl(size_t page_index, PhysicalAddress);
    [[nodiscard]] bool map_individual_page_impl(size_t page_index, PhysicalAddress, bool readable, bool writeable);

    LockRefPtr<PageDirectory> m_page_directory;
    VirtualRange m_range;
    size_t m_offset_in_vmobject { 0 };
    LockRefPtr<VMObject> m_vmobject;
    OwnPtr<KString> m_name;
    Atomic<u32> m_in_progress_page_faults;
    u8 m_access { Region::None };
    bool m_shared : 1 { false };
    bool m_stack : 1 { false };
    bool m_mmap : 1 { false };
    bool m_syscall_region : 1 { false };
    bool m_mmapped_from_readable : 1 { false };
    bool m_mmapped_from_writable : 1 { false };

    MemoryType m_memory_type;

    SetOnce m_immutable;
    SetOnce m_initially_loaded_executable_segment;
    SetOnce m_has_been_readable;
    SetOnce m_has_been_writable;
    SetOnce m_has_been_executable;

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
