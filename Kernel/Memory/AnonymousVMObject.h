/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/AllocationStrategy.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PageFaultResponse.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/VMObject.h>

namespace Kernel::Memory {

class AnonymousVMObject final : public VMObject {
public:
    virtual ~AnonymousVMObject() override;

    static ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> try_create_with_size(size_t, AllocationStrategy);
    static ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> try_create_for_physical_range(PhysicalAddress paddr, size_t size);
    static ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> try_create_with_physical_pages(Span<NonnullRefPtr<PhysicalRAMPage>>);
    static ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> try_create_purgeable_with_size(size_t, AllocationStrategy);
    static ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> try_create_physically_contiguous_with_size(size_t, MemoryType memory_type_for_zero_fill);
    virtual ErrorOr<NonnullLockRefPtr<VMObject>> try_clone() override;

    [[nodiscard]] NonnullRefPtr<PhysicalRAMPage> allocate_committed_page(Badge<Region>);
    PageFaultResponse handle_cow_fault(size_t, VirtualAddress);
    size_t cow_pages() const;
    bool should_cow(size_t page_index, bool) const;
    ErrorOr<void> set_should_cow(size_t page_index, bool);

    bool is_purgeable() const { return m_purgeable; }
    bool is_volatile() const { return m_volatile; }

    ErrorOr<void> set_volatile(bool is_volatile, bool& was_purged);

    size_t purge();

private:
    class SharedCommittedCowPages;

    static ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> try_create_with_shared_cow(AnonymousVMObject const&, NonnullLockRefPtr<SharedCommittedCowPages>, FixedArray<RefPtr<PhysicalRAMPage>>&&);

    explicit AnonymousVMObject(FixedArray<RefPtr<PhysicalRAMPage>>&&, AllocationStrategy, Optional<CommittedPhysicalPageSet>);
    explicit AnonymousVMObject(PhysicalAddress, FixedArray<RefPtr<PhysicalRAMPage>>&&);
    explicit AnonymousVMObject(FixedArray<RefPtr<PhysicalRAMPage>>&&);
    explicit AnonymousVMObject(LockWeakPtr<AnonymousVMObject>, NonnullLockRefPtr<SharedCommittedCowPages>, FixedArray<RefPtr<PhysicalRAMPage>>&&);

    virtual StringView class_name() const override { return "AnonymousVMObject"sv; }

    AnonymousVMObject& operator=(AnonymousVMObject const&) = delete;
    AnonymousVMObject& operator=(AnonymousVMObject&&) = delete;
    AnonymousVMObject(AnonymousVMObject&&) = delete;

    virtual bool is_anonymous() const override { return true; }

    ErrorOr<void> ensure_cow_map();
    ErrorOr<void> ensure_or_reset_cow_map();
    void reset_cow_map();

    Optional<CommittedPhysicalPageSet> m_unused_committed_pages;
    Bitmap m_cow_map;

    // AnonymousVMObject shares committed COW pages with cloned children (happens on fork)
    class SharedCommittedCowPages final : public AtomicRefCounted<SharedCommittedCowPages> {
        AK_MAKE_NONCOPYABLE(SharedCommittedCowPages);

    public:
        SharedCommittedCowPages() = delete;

        explicit SharedCommittedCowPages(CommittedPhysicalPageSet&&);
        ~SharedCommittedCowPages();

        [[nodiscard]] bool is_empty() const { return m_committed_pages.is_empty(); }

        [[nodiscard]] NonnullRefPtr<PhysicalRAMPage> take_one();
        void uncommit_one();

    private:
        Spinlock<LockRank::None> m_lock {};
        CommittedPhysicalPageSet m_committed_pages;
    };

    LockWeakPtr<AnonymousVMObject> m_cow_parent;
    LockRefPtr<SharedCommittedCowPages> m_shared_committed_cow_pages;

    bool m_purgeable { false };
    bool m_volatile { false };
    bool m_was_purged { false };
};

}
