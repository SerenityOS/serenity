#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

static const dword userspace_range_base = 0x01000000;
static const dword kernelspace_range_base = 0xc0000000;

PageDirectory::PageDirectory(PhysicalAddress paddr)
    : m_range_allocator(LinearAddress(0xc0000000), 0x3f000000)
{
    m_directory_page = PhysicalPage::create_eternal(paddr, true);
}

PageDirectory::PageDirectory(const RangeAllocator* parent_range_allocator)
    : m_range_allocator(parent_range_allocator ? RangeAllocator(*parent_range_allocator) : RangeAllocator(LinearAddress(userspace_range_base), kernelspace_range_base - userspace_range_base))
{
    MM.populate_page_directory(*this);
}

PageDirectory::~PageDirectory()
{
#ifdef MM_DEBUG
    dbgprintf("MM: ~PageDirectory K%x\n", this);
#endif
}

void PageDirectory::flush(LinearAddress laddr)
{
#ifdef MM_DEBUG
    dbgprintf("MM: Flush page L%x\n", laddr.get());
#endif
    if (!current)
        return;
    if (this == &MM.kernel_page_directory() || &current->process().page_directory() == this)
        MM.flush_tlb(laddr);
}
