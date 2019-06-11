#include <AK/Bitmap.h>
#include <AK/Retained.h>
#include <Kernel/Assertions.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/PhysicalRegion.h>

Retained<PhysicalRegion> PhysicalRegion::create(PhysicalAddress lower, PhysicalAddress upper)
{
    return adopt(*new PhysicalRegion(lower, upper));
}

PhysicalRegion::PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper)
    : m_lower(lower)
    , m_upper(upper)
    , m_pages((upper.get() - lower.get()) / PAGE_SIZE)
    , m_mask(Bitmap::create((upper.get() - lower.get()) / PAGE_SIZE, false))
{
}

PhysicalAddress PhysicalRegion::take_free_page()
{
    if (m_used == m_pages)
        return {};

    for (unsigned page = 0; page < m_pages; page++) {
        if (!m_mask.get(page)) {
            m_mask.set(page, true);
            m_used++;
            return m_lower.offset(page * PAGE_SIZE);
        }
    }

    ASSERT_NOT_REACHED();

    return {};
}

void PhysicalRegion::return_page_at(PhysicalAddress addr)
{
    if (m_used == 0) {
        ASSERT_NOT_REACHED();
    }

    int local_offset = addr.get() - m_lower.get();
    ASSERT(local_offset >= 0);
    ASSERT(local_offset < m_pages * PAGE_SIZE);

    m_mask.set(local_offset / PAGE_SIZE, false);
    m_used--;
}
