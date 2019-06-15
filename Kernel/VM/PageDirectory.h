#pragma once

#include <AK/HashMap.h>
#include <AK/RetainPtr.h>
#include <AK/Retainable.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/RangeAllocator.h>

class PageDirectory : public Retainable<PageDirectory> {
    friend class MemoryManager;

public:
    static Retained<PageDirectory> create_for_userspace(const RangeAllocator* parent_range_allocator = nullptr) { return adopt(*new PageDirectory(parent_range_allocator)); }
    static Retained<PageDirectory> create_at_fixed_address(PhysicalAddress paddr) { return adopt(*new PageDirectory(paddr)); }
    ~PageDirectory();

    dword cr3() const { return m_directory_page->paddr().get(); }
    dword* entries() { return reinterpret_cast<dword*>(cr3()); }

    void flush(VirtualAddress);

    RangeAllocator& range_allocator() { return m_range_allocator; }

private:
    explicit PageDirectory(const RangeAllocator* parent_range_allocator);
    explicit PageDirectory(PhysicalAddress);

    RangeAllocator m_range_allocator;
    RetainPtr<PhysicalPage> m_directory_page;
    HashMap<unsigned, RetainPtr<PhysicalPage>> m_physical_pages;
};
