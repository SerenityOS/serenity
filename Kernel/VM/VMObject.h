/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/IntrusiveList.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>
#include <Kernel/Lock.h>

namespace Kernel {

class Inode;
class PhysicalPage;

class VMObjectDeletedHandler {
public:
    virtual ~VMObjectDeletedHandler() = default;
    virtual void vmobject_deleted(VMObject&) = 0;
};

class VMObject : public RefCounted<VMObject>
    , public Weakable<VMObject> {
    friend class MemoryManager;
    friend class Region;

public:
    virtual ~VMObject();

    virtual RefPtr<VMObject> clone() = 0;

    virtual bool is_anonymous() const { return false; }
    virtual bool is_inode() const { return false; }
    virtual bool is_shared_inode() const { return false; }
    virtual bool is_private_inode() const { return false; }
    virtual bool is_contiguous() const { return false; }

    size_t page_count() const { return m_physical_pages.size(); }
    const Vector<RefPtr<PhysicalPage>, 16>& physical_pages() const { return m_physical_pages; }
    Vector<RefPtr<PhysicalPage>, 16>& physical_pages() { return m_physical_pages; }

    size_t size() const { return m_physical_pages.size() * PAGE_SIZE; }

    virtual const char* class_name() const = 0;

    ALWAYS_INLINE void ref_region() { m_regions_count++; }
    ALWAYS_INLINE void unref_region() { m_regions_count--; }
    ALWAYS_INLINE bool is_shared_by_multiple_regions() const { return m_regions_count > 1; }

    void register_on_deleted_handler(VMObjectDeletedHandler& handler)
    {
        m_on_deleted.set(&handler);
    }
    void unregister_on_deleted_handler(VMObjectDeletedHandler& handler)
    {
        m_on_deleted.remove(&handler);
    }

protected:
    VMObject();
    explicit VMObject(size_t);
    explicit VMObject(const VMObject&);

    template<typename Callback>
    void for_each_region(Callback);

    IntrusiveListNode<VMObject> m_list_node;
    Vector<RefPtr<PhysicalPage>, 16> m_physical_pages;
    Lock m_paging_lock { "VMObject" };

    mutable SpinLock<u8> m_lock;

private:
    VMObject& operator=(const VMObject&) = delete;
    VMObject& operator=(VMObject&&) = delete;
    VMObject(VMObject&&) = delete;

    Atomic<u32, AK::MemoryOrder::memory_order_relaxed> m_regions_count { 0 };
    HashTable<VMObjectDeletedHandler*> m_on_deleted;
    SpinLock<u8> m_on_deleted_lock;

public:
    using List = IntrusiveList<VMObject, RawPtr<VMObject>, &VMObject::m_list_node>;
};

}
