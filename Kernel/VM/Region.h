#pragma once

#include <AK/Bitmap.h>
#include <AK/InlineLinkedList.h>
#include <AK/String.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/RangeAllocator.h>

class Inode;
class VMObject;

class Region final : public InlineLinkedListNode<Region> {
    friend class MemoryManager;

    MAKE_SLAB_ALLOCATED(Region)
public:
    enum Access {
        Read = 1,
        Write = 2,
        Execute = 4,
    };

    static NonnullOwnPtr<Region> create_user_accessible(const Range&, const StringView& name, u8 access);
    static NonnullOwnPtr<Region> create_user_accessible(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const StringView& name, u8 access);
    static NonnullOwnPtr<Region> create_user_accessible(const Range&, NonnullRefPtr<Inode>, const StringView& name, u8 access);
    static NonnullOwnPtr<Region> create_kernel_only(const Range&, const StringView& name, u8 access);

    ~Region();

    const Range& range() const { return m_range; }
    VirtualAddress vaddr() const { return m_range.base(); }
    size_t size() const { return m_range.size(); }
    bool is_readable() const { return m_access & Access::Read; }
    bool is_writable() const { return m_access & Access::Write; }
    bool is_executable() const { return m_access & Access::Execute; }
    const String& name() const { return m_name; }
    unsigned access() const { return m_access; }

    void set_name(const String& name) { m_name = name; }

    const VMObject& vmobject() const { return *m_vmobject; }
    VMObject& vmobject() { return *m_vmobject; }

    bool is_shared() const { return m_shared; }
    void set_shared(bool shared) { m_shared = shared; }

    bool is_user_accessible() const { return m_user_accessible; }

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
        return m_offset_in_vmo / PAGE_SIZE;
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
        return m_offset_in_vmo;
    }

    int commit();

    size_t amount_resident() const;
    size_t amount_shared() const;

    PageDirectory* page_directory() { return m_page_directory.ptr(); }

    void set_page_directory(PageDirectory& page_directory)
    {
        ASSERT(!m_page_directory || m_page_directory == &page_directory);
        m_page_directory = page_directory;
    }

    void release_page_directory()
    {
        ASSERT(m_page_directory);
        m_page_directory.clear();
    }

    bool should_cow(size_t page_index) const;
    void set_should_cow(size_t page_index, bool);

    void set_writable(bool b)
    {
        if (b)
            m_access |= Access::Read;
        else
            m_access &= ~Access::Write;
    }

    void remap_page(size_t index);

    // For InlineLinkedListNode
    Region* m_next { nullptr };
    Region* m_prev { nullptr };

    // NOTE: These are public so we can make<> them.
    Region(const Range&, const String&, u8 access);
    Region(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmo, const String&, u8 access);
    Region(const Range&, RefPtr<Inode>&&, const String&, u8 access);

private:
    Bitmap& ensure_cow_map() const;

    RefPtr<PageDirectory> m_page_directory;
    Range m_range;
    size_t m_offset_in_vmo { 0 };
    NonnullRefPtr<VMObject> m_vmobject;
    String m_name;
    u8 m_access { 0 };
    bool m_shared { false };
    bool m_user_accessible { false };
    mutable OwnPtr<Bitmap> m_cow_map;
};
