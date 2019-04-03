#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

PageDirectory::PageDirectory(PhysicalAddress paddr)
{
    m_directory_page = PhysicalPage::create_eternal(paddr, true);
}

PageDirectory::PageDirectory()
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
    if (&current->process().page_directory() == this)
        MM.flush_tlb(laddr);
}
