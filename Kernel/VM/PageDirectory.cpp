#include <Kernel/Process.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>

static const u32 userspace_range_base = 0x01000000;
static const u32 kernelspace_range_base = 0xc0000000;

static HashMap<u32, PageDirectory*>& pdb_map()
{
    ASSERT_INTERRUPTS_DISABLED();
    static HashMap<u32, PageDirectory*>* map;
    if (!map)
        map = new HashMap<u32, PageDirectory*>;
    return *map;
}

RefPtr<PageDirectory> PageDirectory::find_by_pdb(u32 pdb)
{
    InterruptDisabler disabler;
    return pdb_map().get(pdb).value_or({});
}

PageDirectory::PageDirectory(PhysicalAddress paddr)
    : m_range_allocator(VirtualAddress(0xc0000000), 0x3f000000)
{
    m_directory_page = PhysicalPage::create(paddr, true, false);
    InterruptDisabler disabler;
    pdb_map().set(m_directory_page->paddr().get(), this);
}

PageDirectory::PageDirectory(Process& process, const RangeAllocator* parent_range_allocator)
    : m_process(&process)
    , m_range_allocator(parent_range_allocator ? RangeAllocator(*parent_range_allocator) : RangeAllocator(VirtualAddress(userspace_range_base), kernelspace_range_base - userspace_range_base))
{
    MM.populate_page_directory(*this);
    InterruptDisabler disabler;
    pdb_map().set(m_directory_page->paddr().get(), this);
}

PageDirectory::~PageDirectory()
{
#ifdef MM_DEBUG
    dbgprintf("MM: ~PageDirectory K%x\n", this);
#endif
    InterruptDisabler disabler;
    pdb_map().remove(m_directory_page->paddr().get());
}

void PageDirectory::flush(VirtualAddress vaddr)
{
#ifdef MM_DEBUG
    dbgprintf("MM: Flush page V%p\n", vaddr.get());
#endif
    if (!current)
        return;
    if (this == &MM.kernel_page_directory() || &current->process().page_directory() == this)
        MM.flush_tlb(vaddr);
}

void PageDirectory::update_kernel_mappings()
{
    // This ensures that the kernel virtual address space is up-to-date in this page directory.
    // This may be necessary to avoid triple faulting when entering a process's paging scope
    // whose mappings are out-of-date.
    memcpy(entries() + 768, MM.kernel_page_directory().entries() + 768, sizeof(PageDirectoryEntry) * 256);
}
