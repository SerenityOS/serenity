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

#include <AK/InlineLinkedList.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/TypeCasts.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>
#include <Kernel/Lock.h>

namespace Kernel {

class Inode;
class PhysicalPage;

struct PhysicalPageSlotTraits {
    // We want to hold either a pointer to an actual PhysicalPage, or
    // alternatively enough information to locate that physical page
    // in a SwapArea. So rather than comparing the full pointer against
    // nullptr, we'll just check the least significant bit (which should
    // be zero because the PhysicalPage pointer should be aligned properly).
    // If that bit is set, we'll treat it as a nullptr, and store the
    // swap information in the remaining bits
    static PhysicalPage* as_ptr(FlatPtr bits)
    {
        return !(bits & 1) ? (PhysicalPage*)bits : nullptr;
    }
    static FlatPtr as_bits(PhysicalPage* ptr)
    {
        ASSERT(!((FlatPtr)ptr & 1));
        return ptr ? (FlatPtr)ptr : 1;
    }
    static bool is_null(FlatPtr bits)
    {
        return (bits & 1) != 0;
    }

    // Set the PAT bit by default so it doesn't become a swap entry unless
    // we explicitly make it one.
    static constexpr FlatPtr default_null_value = 1 | (1 << 7);

    static PageTableEntry to_null_value(FlatPtr bits)
    {
        PageTableEntry entry;
        entry.set_raw(bits & ~(FlatPtr)1);
        // The PAT bit may be set or unset. If it is set, it is not a swap entry!
        return entry;
    }

    static FlatPtr from_null_value(PageTableEntry null_value)
    {
        auto bits = (FlatPtr)null_value.raw();
        ASSERT(!(bits & 1));
        return bits | 1;
    }

    typedef PageTableEntry NullType;
};

class VMObject : public RefCounted<VMObject>
    , public Weakable<VMObject>
    , public InlineLinkedListNode<VMObject> {
    friend class MemoryManager;
    friend class Region;

public:
    typedef RefPtr<PhysicalPage, PhysicalPageSlotTraits> PhysicalPageSlotType;

    virtual ~VMObject();

    virtual RefPtr<VMObject> clone() = 0;

    virtual bool is_anonymous() const { return false; }
    virtual bool is_inode() const { return false; }
    virtual bool is_shared_inode() const { return false; }
    virtual bool is_private_inode() const { return false; }
    virtual bool is_contiguous() const { return false; }
    virtual bool is_swappable() const { return false; }

    virtual bool try_swap_out_page(PhysicalPage&) { return false; }

    size_t page_count() const { return m_physical_pages.size(); }
    const Vector<PhysicalPageSlotType>& physical_pages() const { return m_physical_pages; }
    Vector<PhysicalPageSlotType>& physical_pages() { return m_physical_pages; }

    size_t size() const { return m_physical_pages.size() * PAGE_SIZE; }

    virtual const char* class_name() const = 0;

    // For InlineLinkedListNode
    VMObject* m_next { nullptr };
    VMObject* m_prev { nullptr };

protected:
    explicit VMObject(size_t);
    explicit VMObject(const VMObject&);

    template<typename Callback>
    void for_each_region(Callback);

    Vector<PhysicalPageSlotType> m_physical_pages;
    Lock m_paging_lock { "VMObject" };

    mutable SpinLock<u8> m_lock;

private:
    VMObject& operator=(const VMObject&) = delete;
    VMObject& operator=(VMObject&&) = delete;
    VMObject(VMObject&&) = delete;
};

}
