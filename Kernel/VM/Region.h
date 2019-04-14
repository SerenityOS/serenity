#pragma once

#include <AK/AKString.h>
#include <AK/Bitmap.h>
#include <Kernel/VM/PageDirectory.h>

class Inode;
class VMObject;

class Region : public Retainable<Region> {
    friend class MemoryManager;
public:
    Region(LinearAddress, size_t, String&&, bool r, bool w, bool cow = false);
    Region(LinearAddress, size_t, Retained<VMObject>&&, size_t offset_in_vmo, String&&, bool r, bool w, bool cow = false);
    Region(LinearAddress, size_t, RetainPtr<Inode>&&, String&&, bool r, bool w);
    ~Region();

    LinearAddress laddr() const { return m_laddr; }
    size_t size() const { return m_size; }
    bool is_readable() const { return m_readable; }
    bool is_writable() const { return m_writable; }
    String name() const { return m_name; }

    void set_name(String&& name) { m_name = move(name); }

    const VMObject& vmo() const { return *m_vmo; }
    VMObject& vmo() { return *m_vmo; }

    bool is_shared() const { return m_shared; }
    void set_shared(bool shared) { m_shared = shared; }

    bool is_bitmap() const { return m_is_bitmap; }
    void set_is_bitmap(bool b) { m_is_bitmap = b; }

    Retained<Region> clone();
    bool contains(LinearAddress laddr) const
    {
        return laddr >= m_laddr && laddr < m_laddr.offset(size());
    }

    unsigned page_index_from_address(LinearAddress laddr) const
    {
        return (laddr - m_laddr).get() / PAGE_SIZE;
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
        return m_size / PAGE_SIZE;
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

    const Bitmap& cow_map() const { return m_cow_map; }

    void set_writable(bool b) { m_writable = b; }

private:
    RetainPtr<PageDirectory> m_page_directory;
    LinearAddress m_laddr;
    size_t m_size { 0 };
    size_t m_offset_in_vmo { 0 };
    Retained<VMObject> m_vmo;
    String m_name;
    bool m_readable { true };
    bool m_writable { true };
    bool m_shared { false };
    bool m_is_bitmap { false };
    Bitmap m_cow_map;
};
