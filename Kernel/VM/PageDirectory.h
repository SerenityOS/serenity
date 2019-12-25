#pragma once

#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/RangeAllocator.h>

class Process;

class PageDirectory : public RefCounted<PageDirectory> {
    friend class MemoryManager;

public:
    static NonnullRefPtr<PageDirectory> create_for_userspace(Process& process, const RangeAllocator* parent_range_allocator = nullptr)
    {
        return adopt(*new PageDirectory(process, parent_range_allocator));
    }
    static NonnullRefPtr<PageDirectory> create_at_fixed_address(PhysicalAddress paddr) { return adopt(*new PageDirectory(paddr)); }
    static RefPtr<PageDirectory> find_by_cr3(u32);

    ~PageDirectory();

    u32 cr3() const { return m_directory_table->paddr().get(); }
    PageDirectoryPointerTable& table() { return *reinterpret_cast<PageDirectoryPointerTable*>(cr3()); }

    void flush(VirtualAddress);

    RangeAllocator& range_allocator() { return m_range_allocator; }

    Process* process() { return m_process; }
    const Process* process() const { return m_process; }

private:
    PageDirectory(Process&, const RangeAllocator* parent_range_allocator);
    explicit PageDirectory(PhysicalAddress);

    Process* m_process { nullptr };
    RangeAllocator m_range_allocator;
    RefPtr<PhysicalPage> m_directory_table;
    RefPtr<PhysicalPage> m_directory_pages[4];
    HashMap<unsigned, RefPtr<PhysicalPage>> m_physical_pages;
};
