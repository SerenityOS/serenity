#pragma once

#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/RangeAllocator.h>
#include <AK/HashMap.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

class PageDirectory : public Retainable<PageDirectory> {
    friend class MemoryManager;
public:
    static Retained<PageDirectory> create_for_userspace() { return adopt(*new PageDirectory); }
    static Retained<PageDirectory> create_at_fixed_address(PhysicalAddress paddr) { return adopt(*new PageDirectory(paddr)); }
    ~PageDirectory();

    dword cr3() const { return m_directory_page->paddr().get(); }
    dword* entries() { return reinterpret_cast<dword*>(cr3()); }

    void flush(LinearAddress);

    RangeAllocator& range_allocator() { return m_range_allocator; }

private:
    PageDirectory();
    explicit PageDirectory(PhysicalAddress);

    RangeAllocator m_range_allocator;
    RetainPtr<PhysicalPage> m_directory_page;
    HashMap<unsigned, RetainPtr<PhysicalPage>> m_physical_pages;
};
