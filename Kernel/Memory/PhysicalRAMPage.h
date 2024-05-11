/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel::Memory {

enum class MayReturnToFreeList : bool {
    No,
    Yes
};

class PhysicalRAMPage {
    AK_MAKE_NONCOPYABLE(PhysicalRAMPage);
    AK_MAKE_NONMOVABLE(PhysicalRAMPage);

    friend class MemoryManager;

public:
    PhysicalAddress paddr() const;

    void ref() const
    {
        m_ref_count.fetch_add(1, AK::memory_order_relaxed);
    }

    void unref() const
    {
        if (m_ref_count.fetch_sub(1, AK::memory_order_acq_rel) == 1)
            free_this();
    }

    static NonnullRefPtr<PhysicalRAMPage> create(PhysicalAddress, MayReturnToFreeList may_return_to_freelist = MayReturnToFreeList::Yes);

    u32 ref_count() const { return m_ref_count.load(AK::memory_order_consume); }

    bool is_shared_zero_page() const;
    bool is_lazy_committed_page() const;

private:
    explicit PhysicalRAMPage(MayReturnToFreeList may_return_to_freelist);
    ~PhysicalRAMPage() = default;

    void free_this() const;

    mutable Atomic<u32> m_ref_count { 1 };
    MayReturnToFreeList m_may_return_to_freelist { MayReturnToFreeList::Yes };
};

struct PhysicalPageEntry {
    union {
        // If it's a live PhysicalPage object:
        struct {
            PhysicalRAMPage physical_page;
        } allocated;

        // If it's an entry in a PhysicalZone::Bucket's freelist.
        struct {
            i16 next_index;
            i16 prev_index;
        } freelist;
    };
};

}
