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

    virtual void will_be_destroyed() { }
    virtual SpinlockProtected<FixedArray<RefPtr<PhysicalPage>>, LockRank::None>& physical_pages() { return m_physical_pages; }
    virtual SpinlockProtected<FixedArray<RefPtr<PhysicalPage>>, LockRank::None> const& physical_pages() const { return m_physical_pages; }

    size_t size() const { return m_physical_pages_count * PAGE_SIZE; }
    size_t page_count() const { return m_physical_pages_count; }

    virtual StringView class_name() const = 0;

    ALWAYS_INLINE void add_region(Region& region)
    {
        m_regions.with([&](auto& list) {
            list.append(region);
        });
    }

    ALWAYS_INLINE void remove_region(Region& region)
    {
        m_regions.with([&](auto& list) {
            list.remove(region);
        });
    }

protected:
    static ErrorOr<FixedArray<RefPtr<PhysicalPage>>> try_create_physical_pages(size_t);
    ErrorOr<FixedArray<RefPtr<PhysicalPage>>> try_clone_physical_pages() const;
    explicit VMObject(FixedArray<RefPtr<PhysicalPage>>&&);

    template<typename Callback>
    void for_each_region(Callback);

    IntrusiveListNode<VMObject> m_list_node;
    size_t const m_physical_pages_count { 0 };
    SpinlockProtected<FixedArray<RefPtr<PhysicalPage>>, LockRank::None> m_physical_pages;

private:
    VMObject& operator=(VMObject const&) = delete;
    VMObject& operator=(VMObject&&) = delete;
    VMObject(VMObject&&) = delete;

    SpinlockProtected<Region::ListInVMObject, LockRank::None> m_regions;

public:
    using AllInstancesList = IntrusiveList<&VMObject::m_list_node>;
    static SpinlockProtected<VMObject::AllInstancesList, LockRank::None>& all_instances();
};

template<typename Callback>
inline void VMObject::for_each_region(Callback callback)
{
    m_regions.with([&](auto& list) {
        for (auto& region : list) {
            callback(region);
        }
    });
}

}
