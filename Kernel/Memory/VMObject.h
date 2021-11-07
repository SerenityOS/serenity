/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/IntrusiveList.h>
#include <AK/RefPtr.h>
#include <AK/Weakable.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/ListedRefCounted.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/Region.h>

namespace Kernel::Memory {

class VMObject
    : public ListedRefCounted<VMObject>
    , public Weakable<VMObject> {
    friend class MemoryManager;
    friend class Region;

public:
    virtual ~VMObject();

    virtual ErrorOr<NonnullRefPtr<VMObject>> try_clone() = 0;

    virtual bool is_anonymous() const { return false; }
    virtual bool is_inode() const { return false; }
    virtual bool is_shared_inode() const { return false; }
    virtual bool is_private_inode() const { return false; }

    size_t page_count() const { return m_physical_pages.size(); }
    Span<RefPtr<PhysicalPage> const> physical_pages() const { return m_physical_pages.span(); }
    Span<RefPtr<PhysicalPage>> physical_pages() { return m_physical_pages.span(); }

    size_t size() const { return m_physical_pages.size() * PAGE_SIZE; }

    virtual StringView class_name() const = 0;

    ALWAYS_INLINE void add_region(Region& region)
    {
        SpinlockLocker locker(m_lock);
        m_regions.append(region);
    }

    ALWAYS_INLINE void remove_region(Region& region)
    {
        SpinlockLocker locker(m_lock);
        m_regions.remove(region);
    }

protected:
    explicit VMObject(size_t);
    explicit VMObject(VMObject const&);

    template<typename Callback>
    void for_each_region(Callback);

    IntrusiveListNode<VMObject> m_list_node;
    FixedArray<RefPtr<PhysicalPage>> m_physical_pages;

    mutable RecursiveSpinlock m_lock;

private:
    VMObject& operator=(VMObject const&) = delete;
    VMObject& operator=(VMObject&&) = delete;
    VMObject(VMObject&&) = delete;

    Region::ListInVMObject m_regions;

public:
    using AllInstancesList = IntrusiveList<&VMObject::m_list_node>;
    static SpinlockProtected<VMObject::AllInstancesList>& all_instances();
};

template<typename Callback>
inline void VMObject::for_each_region(Callback callback)
{
    SpinlockLocker lock(m_lock);
    for (auto& region : m_regions) {
        callback(region);
    }
}

inline PhysicalPage const* Region::physical_page(size_t index) const
{
    VERIFY(index < page_count());
    return vmobject().physical_pages()[first_page_index() + index];
}

inline RefPtr<PhysicalPage>& Region::physical_page_slot(size_t index)
{
    VERIFY(index < page_count());
    return vmobject().physical_pages()[first_page_index() + index];
}

}
