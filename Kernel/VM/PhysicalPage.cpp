#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/kmalloc.h>

NonnullRefPtr<PhysicalPage> PhysicalPage::create(PhysicalAddress paddr, bool supervisor, bool may_return_to_freelist)
{
    return adopt(*new PhysicalPage(paddr, supervisor, may_return_to_freelist));
}

PhysicalPage::PhysicalPage(PhysicalAddress paddr, bool supervisor, bool may_return_to_freelist)
    : m_may_return_to_freelist(may_return_to_freelist)
    , m_supervisor(supervisor)
    , m_paddr(paddr)
{
}

void PhysicalPage::return_to_freelist() &&
{
    ASSERT((paddr().get() & ~PAGE_MASK) == 0);

    InterruptDisabler disabler;

    m_retain_count = 1;

    if (m_supervisor)
        MM.deallocate_supervisor_physical_page(move(*this));
    else
        MM.deallocate_user_physical_page(move(*this));

#ifdef MM_DEBUG
    dbgprintf("MM: P%x released to freelist\n", m_paddr.get());
#endif
}
