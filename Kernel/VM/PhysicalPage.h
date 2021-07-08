/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <Kernel/Assertions.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class PhysicalPage {
    friend class MemoryManager;
    friend class PageDirectory;
    friend class VMObject;

public:
    PhysicalAddress paddr() const;

    void ref()
    {
        m_ref_count.fetch_add(1, AK::memory_order_acq_rel);
    }

    void unref()
    {
        if (m_ref_count.fetch_sub(1, AK::memory_order_acq_rel) == 1)
            free_this();
    }

    static NonnullRefPtr<PhysicalPage> create(PhysicalAddress, bool supervisor, bool may_return_to_freelist = true);

    u32 ref_count() const { return m_ref_count.load(AK::memory_order_consume); }

    bool is_shared_zero_page() const;
    bool is_lazy_committed_page() const;

private:
    PhysicalPage(bool supervisor, bool may_return_to_freelist = true);
    ~PhysicalPage() = default;

    void free_this();

    Atomic<u32> m_ref_count { 1 };
    bool m_may_return_to_freelist { true };
    bool m_supervisor { false };
};

struct PhysicalPageEntry {
    // This structure either holds a valid PhysicalPage
    // or a PhysicalAllocator's free list information!
    union {
        PhysicalPage physical_page;
    };
};

}
