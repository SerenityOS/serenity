/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/NonnullRefPtr.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Assertions.h>
#include <Kernel/Forward.h>
#include <Kernel/Heap/SlabAllocator.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class PhysicalPage {
    friend class MemoryManager;
    friend class PageDirectory;
    friend class SwapArea;
    friend class UserPhysicalRegion;
    friend class VMObject;

    MAKE_SLAB_ALLOCATED(PhysicalPage);
    AK_MAKE_NONMOVABLE(PhysicalPage);

public:
    PhysicalAddress paddr() const { return m_paddr; }

    void ref()
    {
        if (m_is_eternal)
            return;
        auto refs = m_ref_count.fetch_add(1, AK::memory_order_acq_rel);
        ASSERT(refs < 10000);
    }

    void unref()
    {
        if (m_is_eternal)
            return;
        auto refs = m_ref_count.fetch_sub(1, AK::memory_order_acq_rel);
        if (refs == 1)
            delete this;
        else
            ASSERT(refs != 0);
    }

    static NonnullRefPtr<PhysicalPage> create(PhysicalAddress, bool supervisor, bool may_return_to_freelist = true);

    void was_accessed(bool mark_dirty);

    bool has_swap_entry() const { return !m_swap_entry.is_null(); }
    PageTableEntry& swap_entry() { return m_swap_entry; }
    const PageTableEntry& swap_entry() const { return m_swap_entry; }

    u32 ref_count() const { return m_ref_count.load(AK::memory_order_consume); }

    bool is_shared_zero_page() const;
    bool is_lazy_committed_page() const;

    void make_eternal();
    bool is_eternal() const { return m_is_eternal; }

    bool in_active_inactive_list(Badge<UserPhysicalRegion>) const
    {
        return m_in_active_inactive_list;
    }

    void set_in_active_inactive_list(bool value, Badge<UserPhysicalRegion>)
    {
        ASSERT(!m_is_eternal);
        m_in_active_inactive_list = value;
    }
public: u32 m_tag { 0x12345678 };
private:
    PhysicalPage(PhysicalAddress paddr, bool supervisor, bool may_return_to_freelist = true);

    ~PhysicalPage()
    {
        ASSERT(!m_is_eternal);
        if (m_may_return_to_freelist)
            return_to_freelist();
        ASSERT(!m_list.is_in_list());
        m_tag = 0x87654321;
    }

    void return_to_freelist();

    Atomic<u32> m_ref_count { 1 };

    // m_list is a node in one of these lists (if !m_supervisor):
    // - UserPhysicalRegion::m_active_pages
    // - UserPhysicalRegion::m_inactive_pages
    // - SwapArea::m_pending_swap_out
    IntrusiveListNode m_list;

    // This seems weird, but when we convert a PTE to a swap entry, we
    // need to be able to find the same swap entry for any other PTE
    // referencing this phyiscal page. Since there is no easy way to find
    // all PTEs referencing this page, we don't convert all of them at
    // the same time but rather check this field to see if we already
    // have a swap entry that we can reference.
    PageTableEntry m_swap_entry;

    bool m_is_eternal { false };
    bool m_may_return_to_freelist : 1 { true };
    bool m_supervisor : 1 { false };
    bool m_dirty : 1 { false };
    bool m_in_active_inactive_list : 1 { false };
    PhysicalAddress m_paddr;
};

}
