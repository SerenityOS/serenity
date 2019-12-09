#pragma once

#include <AK/FixedArray.h>
#include <AK/InlineLinkedList.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Weakable.h>
#include <Kernel/Lock.h>

class Inode;
class PhysicalPage;

class VMObject : public RefCounted<VMObject>
    , public Weakable<VMObject>
    , public InlineLinkedListNode<VMObject> {
    friend class MemoryManager;
    friend class Region;

public:
    virtual ~VMObject();

    virtual NonnullRefPtr<VMObject> clone() = 0;

    virtual bool is_anonymous() const { return false; }
    virtual bool is_purgeable() const { return false; }
    virtual bool is_inode() const { return false; }

    size_t page_count() const { return m_physical_pages.size(); }
    const FixedArray<RefPtr<PhysicalPage>>& physical_pages() const { return m_physical_pages; }
    FixedArray<RefPtr<PhysicalPage>>& physical_pages() { return m_physical_pages; }

    size_t size() const { return m_physical_pages.size() * PAGE_SIZE; }

    // For InlineLinkedListNode
    VMObject* m_next { nullptr };
    VMObject* m_prev { nullptr };

protected:
    explicit VMObject(size_t);
    explicit VMObject(const VMObject&);

    template<typename Callback>
    void for_each_region(Callback);

    FixedArray<RefPtr<PhysicalPage>> m_physical_pages;
    Lock m_paging_lock { "VMObject" };

private:
    VMObject& operator=(const VMObject&) = delete;
    VMObject& operator=(VMObject&&) = delete;
    VMObject(VMObject&&) = delete;
};
