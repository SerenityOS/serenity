#pragma once

#include <AK/AKString.h>
#include <AK/Bitmap.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/RangeAllocator.h>

class Inode;
class VMObject;

class Region : public RefCounted<Region> {
    friend class MemoryManager;

public:
    enum Access {
        Read = 1,
        Write = 2,
        Execute = 4,
    };

    static NonnullRefPtr<Region> create_user_accessible(const Range&, const StringView& name, u8 access, bool cow = false);
    static NonnullRefPtr<Region> create_user_accessible(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, const StringView& name, u8 access, bool cow = false);
    static NonnullRefPtr<Region> create_user_accessible(const Range&, NonnullRefPtr<Inode>, const StringView& name, u8 access, bool cow = false);
    static NonnullRefPtr<Region> create_kernel_only(const Range&, const StringView& name, u8 access, bool cow = false);

    ~Region();

    VirtualAddress vaddr() const { return m_range.base(); }
    size_t size() const { return m_range.size(); }
    bool is_readable() const { return m_access & Access::Read; }
    bool is_writable() const { return m_access & Access::Write; }
    bool is_executable() const { return m_access & Access::Execute; }
    const String& name() const { return m_name; }

    void set_name(const String& name) { m_name = name; }

    const VMObject& vmo() const { return *m_vmo; }
    VMObject& vmo() { return *m_vmo; }

    bool is_shared() const { return m_shared; }
    void set_shared(bool shared) { m_shared = shared; }

    bool is_user_accessible() const { return m_user_accessible; }

    NonnullRefPtr<Region> clone();

    bool contains(VirtualAddress vaddr) const
    {
        return m_range.contains(vaddr);
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

    bool page_in();
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

    bool should_cow(size_t page_index) const { return m_cow_map.get(page_index); }
    void set_should_cow(size_t page_index, bool cow) { m_cow_map.set(page_index, cow); }

    void set_writable(bool b)
    {
        if (b)
            m_access |= Access::Read;
        else
            m_access &= ~Access::Write;
    }

private:
    Region(const Range&, const String&, u8 access, bool cow = false);
    Region(const Range&, NonnullRefPtr<VMObject>, size_t offset_in_vmo, const String&, u8 access, bool cow = false);
    Region(const Range&, RefPtr<Inode>&&, const String&, u8 access, bool cow = false);

    RefPtr<PageDirectory> m_page_directory;
    Range m_range;
    size_t m_offset_in_vmo { 0 };
    NonnullRefPtr<VMObject> m_vmo;
    String m_name;
    u8 m_access { 0 };
    bool m_shared { false };
    bool m_user_accessible { false };
    Bitmap m_cow_map;
};
