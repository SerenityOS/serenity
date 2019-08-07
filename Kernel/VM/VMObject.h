#pragma once

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/FixedArray.h>
#include <AK/Weakable.h>
#include <Kernel/Lock.h>

class Inode;
class PhysicalPage;

class VMObject : public RefCounted<VMObject>
    , public Weakable<VMObject> {
    friend class MemoryManager;

public:
    virtual ~VMObject();

    virtual NonnullRefPtr<VMObject> clone() = 0;

    virtual bool is_anonymous() const { return false; }
    virtual bool is_inode() const { return false; }

    size_t page_count() const { return m_physical_pages.size(); }
    const FixedArray<RefPtr<PhysicalPage>>& physical_pages() const { return m_physical_pages; }
    FixedArray<RefPtr<PhysicalPage>>& physical_pages() { return m_physical_pages; }

    size_t size() const { return m_physical_pages.size() * PAGE_SIZE; }

protected:
    explicit VMObject(size_t);
    explicit VMObject(const VMObject&);

    template<typename Callback>
    void for_each_region(Callback);

    FixedArray<RefPtr<PhysicalPage>> m_physical_pages;

private:
    VMObject& operator=(const VMObject&) = delete;
    VMObject& operator=(VMObject&&) = delete;
    VMObject(VMObject&&) = delete;

    Lock m_paging_lock { "VMObject" };
};
