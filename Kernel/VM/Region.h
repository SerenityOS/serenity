#pragma once

#include <AK/Bitmap.h>
#include <AK/InlineLinkedList.h>
#include <AK/String.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/RangeAllocator.h>

class Inode;
class VMObject;

enum class PageFaultResponse {
    ShouldCrash,
    Continue,
};

class Region final : public InlineLinkedListNode<Region> {
    friend class MemoryManager;

    MAKE_SLAB_ALLOCATED(Region)
public:
    enum Access {
        Read = 1,
        Write = 2,
        Execute = 4,
    };

    static NonnullOwnPtr<Region> create_user_accessible(const Range&, const StringView& name, u8 access, bool cacheable = true);
    static NonnullOwnPtr<Region> create_user_accessible(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const StringView& name, u8 access, bool cacheable = true);
    static NonnullOwnPtr<Region> create_user_accessible(const Range&, NonnullRefPtr<Inode>, const StringView& name, u8 access, bool cacheable = true);
    static NonnullOwnPtr<Region> create_kernel_only(const Range&, const StringView& name, u8 access, bool cacheable = true);
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

    const VMObject& vmobject() const { return *m_vmobject; }
    VMObject& vmobject() { return *m_vmobject; }

    bool is_shared() const { return m_shared; }
    void set_shared(bool shared) { m_shared = shared; }

    bool is_stack() const { return m_stack; }
    void set_stack(bool stack) { m_stack = stack; }

    bool is_mmap() const { return m_mmap; }
    void set_mmap(bool mmap) { m_mmap = mmap; }

    bool is_user_accessible() const { return m_user_accessible; }
    void set_user_accessible(bool b) { m_user_accessible = b; }

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

    size_t offset_in_vmobject() const
    {
        return m_offset_in_vmobject;
    }

    bool commit();
    bool commit(size_t page_index);

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
    void map(PageDirectory&);
    enum class ShouldDeallocateVirtualMemoryRange {
        No,
        Yes,
    };
    void unmap(ShouldDeallocateVirtualMemoryRange = ShouldDeallocateVirtualMemoryRange::Yes);

    void remap();
    void remap_page(size_t index);

    // For InlineLinkedListNode
    Region* m_next { nullptr };
    Region* m_prev { nullptr };

    // NOTE: These are public so we can make<> them.
    Region(const Range&, const String&, u8 access, bool cacheable);
    Region(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const String&, u8 access, bool cacheable);
    Region(const Range&, NonnullRefPtr<Inode>, const String&, u8 access, bool cacheable);

private:
    Bitmap& ensure_cow_map() const;

    void set_access_bit(Access access, bool b)
    {
        if (b)
            m_access |= access;
        else
            m_access &= ~access;
    }

    PageFaultResponse handle_cow_fault(size_t page_index);
    PageFaultResponse handle_inode_fault(size_t page_index);
    PageFaultResponse handle_zero_fault(size_t page_index);

    void map_individual_page_impl(size_t page_index);

    RefPtr<PageDirectory> m_page_directory;
    Range m_range;
    size_t m_offset_in_vmobject { 0 };
    NonnullRefPtr<VMObject> m_vmobject;
    String m_name;
    u8 m_access { 0 };
    bool m_shared { false };
    bool m_user_accessible { false };
    bool m_cacheable { false };
    bool m_stack { false };
    bool m_mmap { false };
    mutable OwnPtr<Bitmap> m_cow_map;
};
