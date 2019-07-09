#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>
#include <AK/RefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>
#include <Kernel/Lock.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/VM/PhysicalAddress.h>

class Inode;
class PhysicalPage;

class VMObject : public RefCounted<VMObject>
    , public Weakable<VMObject> {
    friend class MemoryManager;

public:
    static NonnullRefPtr<VMObject> create_file_backed(RefPtr<Inode>&&);
    static NonnullRefPtr<VMObject> create_anonymous(size_t);
    static NonnullRefPtr<VMObject> create_for_physical_range(PhysicalAddress, size_t);
    NonnullRefPtr<VMObject> clone();

    ~VMObject();
    bool is_anonymous() const { return !m_inode; }

    Inode* inode() { return m_inode.ptr(); }
    const Inode* inode() const { return m_inode.ptr(); }
    size_t inode_offset() const { return m_inode_offset; }

    const String& name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

    int page_count() const { return m_size / PAGE_SIZE; }
    const Vector<RefPtr<PhysicalPage>>& physical_pages() const { return m_physical_pages; }
    Vector<RefPtr<PhysicalPage>>& physical_pages() { return m_physical_pages; }

    void inode_contents_changed(Badge<Inode>, off_t, ssize_t, const u8*);
    void inode_size_changed(Badge<Inode>, size_t old_size, size_t new_size);

    size_t size() const { return m_size; }

private:
    VMObject(RefPtr<Inode>&&);
    explicit VMObject(VMObject&);
    explicit VMObject(size_t);
    VMObject(PhysicalAddress, size_t);

    template<typename Callback>
    void for_each_region(Callback);

    String m_name;
    bool m_allow_cpu_caching { true };
    off_t m_inode_offset { 0 };
    size_t m_size { 0 };
    RefPtr<Inode> m_inode;
    Vector<RefPtr<PhysicalPage>> m_physical_pages;
    Lock m_paging_lock { "VMObject" };
};
