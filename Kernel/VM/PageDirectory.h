#pragma once

#include <Kernel/VM/PhysicalPage.h>
#include <AK/HashMap.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

class PageDirectory : public Retainable<PageDirectory> {
    friend class MemoryManager;
public:
    static Retained<PageDirectory> create() { return adopt(*new PageDirectory); }
    static Retained<PageDirectory> create_at_fixed_address(PhysicalAddress paddr) { return adopt(*new PageDirectory(paddr)); }
    ~PageDirectory();

    dword cr3() const { return m_directory_page->paddr().get(); }
    dword* entries() { return reinterpret_cast<dword*>(cr3()); }

    void flush(LinearAddress);

private:
    PageDirectory();
    explicit PageDirectory(PhysicalAddress);

    RetainPtr<PhysicalPage> m_directory_page;
    HashMap<unsigned, RetainPtr<PhysicalPage>> m_physical_pages;
};
