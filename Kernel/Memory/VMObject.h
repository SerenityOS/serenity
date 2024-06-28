/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/IntrusiveList.h>
#include <AK/RefPtr.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/ListedRefCounted.h>
#include <Kernel/Library/LockWeakable.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/Region.h>

namespace Kernel::Memory {

class VMObject
    : public ListedRefCounted<VMObject, LockType::Spinlock>
    , public LockWeakable<VMObject> {
    friend class MemoryManager;
    friend class Region;

public:
    virtual ~VMObject();

    virtual ErrorOr<NonnullLockRefPtr<VMObject>> try_clone() = 0;

    virtual bool is_anonymous() const { return false; }
    virtual bool is_inode() const { return false; }
    virtual bool is_shared_inode() const { return false; }
    virtual bool is_private_inode() const { return false; }
    virtual bool is_mmio() const { return false; }

    size_t page_count() const { return m_physical_pages.size(); }

    virtual ReadonlySpan<RefPtr<PhysicalRAMPage>> physical_pages() const { return m_physical_pages.span(); }
    virtual Span<RefPtr<PhysicalRAMPage>> physical_pages() { return m_physical_pages.span(); }

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
    static ErrorOr<FixedArray<RefPtr<PhysicalRAMPage>>> try_create_physical_pages(size_t);
    ErrorOr<FixedArray<RefPtr<PhysicalRAMPage>>> try_clone_physical_pages() const;
    explicit VMObject(FixedArray<RefPtr<PhysicalRAMPage>>&&);

    template<typename Callback>
    void for_each_region(Callback);

    void remap_regions();
    bool remap_regions_one_page(size_t page_index, NonnullRefPtr<PhysicalRAMPage> page);

    IntrusiveListNode<VMObject> m_list_node;
    FixedArray<RefPtr<PhysicalRAMPage>> m_physical_pages;

    mutable RecursiveSpinlock<LockRank::None> m_lock {};

private:
    VMObject& operator=(VMObject const&) = delete;
    VMObject& operator=(VMObject&&) = delete;
    VMObject(VMObject&&) = delete;

    Region::ListInVMObject m_regions;

public:
    using AllInstancesList = IntrusiveList<&VMObject::m_list_node>;
    static SpinlockProtected<VMObject::AllInstancesList, LockRank::None>& all_instances();
};

template<typename Callback>
inline void VMObject::for_each_region(Callback callback)
{
    SpinlockLocker lock(m_lock);
    for (auto& region : m_regions) {
        callback(region);
    }
}

}
