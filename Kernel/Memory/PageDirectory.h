/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/IntrusiveRedBlackTree.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/PhysicalPage.h>

namespace Kernel::Memory {

class PageDirectory : public RefCounted<PageDirectory> {
    friend class MemoryManager;

public:
    static ErrorOr<NonnullRefPtr<PageDirectory>> try_create_for_userspace();
    static NonnullRefPtr<PageDirectory> must_create_kernel_page_directory();
    static RefPtr<PageDirectory> find_current();

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

    bool is_cr3_initialized() const
    {
#if ARCH(X86_64)
        return m_pml4t;
#else
        return m_directory_table;
#endif
    }

    AddressSpace* address_space() { return m_space; }
    AddressSpace const* address_space() const { return m_space; }

    void set_space(Badge<AddressSpace>, AddressSpace& space) { m_space = &space; }

    RecursiveSpinlock& get_lock() { return m_lock; }

    // This has to be public to let the global singleton access the member pointer
    IntrusiveRedBlackTreeNode<FlatPtr, PageDirectory, RawPtr<PageDirectory>> m_tree_node;

private:
    PageDirectory();
    static void register_page_directory(PageDirectory* directory);
    static void deregister_page_directory(PageDirectory* directory);

    AddressSpace* m_space { nullptr };
#if ARCH(X86_64)
    RefPtr<PhysicalPage> m_pml4t;
#endif
    RefPtr<PhysicalPage> m_directory_table;
#if ARCH(X86_64)
    RefPtr<PhysicalPage> m_directory_pages[512];
#else
    RefPtr<PhysicalPage> m_directory_pages[4];
#endif
    RecursiveSpinlock m_lock;
};

void activate_kernel_page_directory(PageDirectory const& pgd);
void activate_page_directory(PageDirectory const& pgd, Thread* current_thread);

}
