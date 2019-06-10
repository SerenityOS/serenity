#include <AK/Retained.h>
#include <Kernel/Assertions.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalRegion.h>

Retained<PhysicalRegion> PhysicalRegion::create(PhysicalAddress lower, PhysicalAddress upper)
{
    return adopt(*new PhysicalRegion(lower, upper));
}

PhysicalRegion::PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper)
    : m_lower(lower), m_upper(upper), m_next(lower)
{
    MemoryManager::s_user_physical_pages_not_yet_used += size();
}

PhysicalAddress PhysicalRegion::take_next_page()
{
    ASSERT(!(m_next == m_upper));

    --MemoryManager::s_user_physical_pages_not_yet_used;

    auto addr = m_next;
    m_next.set(m_next.get() + PAGE_SIZE);
    return addr;
}
