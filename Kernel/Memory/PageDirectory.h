/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/IntrusiveRedBlackTree.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <Kernel/Forward.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/Memory/VirtualRangeAllocator.h>

namespace Kernel::Memory {

class PageDirectory : public RefCounted<PageDirectory> {
    friend class MemoryManager;

public:
    static ErrorOr<NonnullRefPtr<PageDirectory>> try_create_for_userspace(VirtualRangeAllocator const* parent_range_allocator = nullptr);
    static NonnullRefPtr<PageDirectory> must_create_kernel_page_directory();
    static RefPtr<PageDirectory> find_by_cr3(FlatPtr);

    ~PageDirectory();

    void allocate_kernel_directory();

    FlatPtr cr3() const
    {
#if ARCH(X86_64)
        return m_pml4t->paddr().get();
#else
        return m_directory_table->paddr().get();
#endif
    }

    VirtualRangeAllocator& range_allocator() { return m_range_allocator; }
    VirtualRangeAllocator const& range_allocator() const { return m_range_allocator; }

    AddressSpace* address_space() { return m_space; }
    const AddressSpace* address_space() const { return m_space; }

    void set_space(Badge<AddressSpace>, AddressSpace& space) { m_space = &space; }

    RecursiveSpinlock& get_lock() { return m_lock; }

    // This has to be public to let the global singleton access the member pointer
    IntrusiveRedBlackTreeNode<FlatPtr, PageDirectory, RawPtr<PageDirectory>> m_tree_node;

private:
    PageDirectory();

    AddressSpace* m_space { nullptr };
    VirtualRangeAllocator m_range_allocator;
#if ARCH(X86_64)
    RefPtr<PhysicalPage> m_pml4t;
#endif
    RefPtr<PhysicalPage> m_directory_table;
#if ARCH(X86_64)
    RefPtr<PhysicalPage> m_directory_pages[512];
#else
    RefPtr<PhysicalPage> m_directory_pages[4];
#endif
    HashMap<FlatPtr, NonnullRefPtr<PhysicalPage>> m_page_tables;
    RecursiveSpinlock m_lock;
};

}
