/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/AllocationStrategy.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageFaultResponse.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {

class AnonymousVMObject final : public VMObject {
public:
    virtual ~AnonymousVMObject() override;

    static RefPtr<AnonymousVMObject> try_create_with_size(size_t, AllocationStrategy);
    static RefPtr<AnonymousVMObject> try_create_for_physical_range(PhysicalAddress paddr, size_t size);
    static RefPtr<AnonymousVMObject> try_create_with_physical_pages(Span<NonnullRefPtr<PhysicalPage>>);
    static RefPtr<AnonymousVMObject> try_create_purgeable_with_size(size_t, AllocationStrategy);
    static RefPtr<AnonymousVMObject> try_create_physically_contiguous_with_size(size_t);
    virtual RefPtr<VMObject> try_clone() override;

    [[nodiscard]] NonnullRefPtr<PhysicalPage> allocate_committed_page(Badge<Region>);
    PageFaultResponse handle_cow_fault(size_t, VirtualAddress);
    size_t cow_pages() const;
    bool should_cow(size_t page_index, bool) const;
    void set_should_cow(size_t page_index, bool);

    bool is_purgeable() const { return m_purgeable; }
    bool is_volatile() const { return m_volatile; }

    KResult set_volatile(bool is_volatile, bool& was_purged);

    size_t purge();

private:
    explicit AnonymousVMObject(size_t, AllocationStrategy, Optional<CommittedPhysicalPageSet>);
    explicit AnonymousVMObject(PhysicalAddress, size_t);
    explicit AnonymousVMObject(Span<NonnullRefPtr<PhysicalPage>>);
    explicit AnonymousVMObject(AnonymousVMObject const&);

    virtual StringView class_name() const override { return "AnonymousVMObject"sv; }

    AnonymousVMObject& operator=(AnonymousVMObject const&) = delete;
    AnonymousVMObject& operator=(AnonymousVMObject&&) = delete;
    AnonymousVMObject(AnonymousVMObject&&) = delete;

    virtual bool is_anonymous() const override { return true; }

    Bitmap& ensure_cow_map();
    void ensure_or_reset_cow_map();

    Optional<CommittedPhysicalPageSet> m_unused_committed_pages;
    Bitmap m_cow_map;

    // AnonymousVMObject shares committed COW pages with cloned children (happens on fork)
    class SharedCommittedCowPages : public RefCounted<SharedCommittedCowPages> {
        AK_MAKE_NONCOPYABLE(SharedCommittedCowPages);

    public:
        SharedCommittedCowPages() = delete;

        explicit SharedCommittedCowPages(CommittedPhysicalPageSet&&);
        ~SharedCommittedCowPages();

        [[nodiscard]] NonnullRefPtr<PhysicalPage> take_one();
        [[nodiscard]] bool is_empty() const { return m_committed_pages.is_empty(); }

    public:
        SpinLock<u8> m_lock;
        CommittedPhysicalPageSet m_committed_pages;
    };

    RefPtr<SharedCommittedCowPages> m_shared_committed_cow_pages;

    bool m_purgeable { false };
    bool m_volatile { false };
    bool m_was_purged { false };
};

}
