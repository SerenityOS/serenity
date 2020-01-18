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

#include <AK/FixedArray.h>
#include <AK/InlineLinkedList.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Weakable.h>
#include <Kernel/Lock.h>

class Inode;
class PhysicalPage;

class VMObject : public RefCounted<VMObject>
    , public Weakable<VMObject>
    , public InlineLinkedListNode<VMObject> {
    friend class MemoryManager;
    friend class Region;

public:
    virtual ~VMObject();

    virtual NonnullRefPtr<VMObject> clone() = 0;

    virtual bool is_anonymous() const { return false; }
    virtual bool is_purgeable() const { return false; }
    virtual bool is_inode() const { return false; }

    size_t page_count() const { return m_physical_pages.size(); }
    const FixedArray<RefPtr<PhysicalPage>>& physical_pages() const { return m_physical_pages; }
    FixedArray<RefPtr<PhysicalPage>>& physical_pages() { return m_physical_pages; }

    size_t size() const { return m_physical_pages.size() * PAGE_SIZE; }

    // For InlineLinkedListNode
    VMObject* m_next { nullptr };
    VMObject* m_prev { nullptr };

protected:
    explicit VMObject(size_t);
    explicit VMObject(const VMObject&);

    template<typename Callback>
    void for_each_region(Callback);

    FixedArray<RefPtr<PhysicalPage>> m_physical_pages;
    Lock m_paging_lock { "VMObject" };

private:
    VMObject& operator=(const VMObject&) = delete;
    VMObject& operator=(VMObject&&) = delete;
    VMObject(VMObject&&) = delete;
};
