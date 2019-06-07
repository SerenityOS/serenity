#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>
#include <AK/RetainPtr.h>
#include <AK/Retainable.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>
#include <Kernel/Lock.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/UnixTypes.h>

class Inode;
class PhysicalPage;

class VMObject : public Retainable<VMObject>
    , public Weakable<VMObject> {
    friend class MemoryManager;

public:
    static Retained<VMObject> create_file_backed(RetainPtr<Inode>&&);
    static Retained<VMObject> create_anonymous(size_t);
    static Retained<VMObject> create_for_physical_range(PhysicalAddress, size_t);
    Retained<VMObject> clone();

    ~VMObject();
    bool is_anonymous() const { return !m_inode; }

    Inode* inode() { return m_inode.ptr(); }
    const Inode* inode() const { return m_inode.ptr(); }
    size_t inode_offset() const { return m_inode_offset; }

    const String& name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

    size_t page_count() const { return m_size / PAGE_SIZE; }
    const Vector<RetainPtr<PhysicalPage>>& physical_pages() const { return m_physical_pages; }
    Vector<RetainPtr<PhysicalPage>>& physical_pages() { return m_physical_pages; }

    void inode_contents_changed(Badge<Inode>, off_t, ssize_t, const byte*);
    void inode_size_changed(Badge<Inode>, size_t old_size, size_t new_size);

    size_t size() const { return m_size; }

private:
    VMObject(RetainPtr<Inode>&&);
    explicit VMObject(VMObject&);
    explicit VMObject(size_t);
    VMObject(PhysicalAddress, size_t);

    template<typename Callback>
    void for_each_region(Callback);

    String m_name;
    bool m_allow_cpu_caching { true };
    off_t m_inode_offset { 0 };
    size_t m_size { 0 };
    RetainPtr<Inode> m_inode;
    Vector<RetainPtr<PhysicalPage>> m_physical_pages;
    Lock m_paging_lock { "VMObject" };
};
