#include <AK/Bitmap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Assertions.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/PhysicalRegion.h>

NonnullRefPtr<PhysicalRegion> PhysicalRegion::create(PhysicalAddress lower, PhysicalAddress upper)
{
    return adopt(*new PhysicalRegion(lower, upper));
}

PhysicalRegion::PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper)
    : m_lower(lower)
    , m_upper(upper)
    , m_bitmap(Bitmap::create())
{
}

void PhysicalRegion::expand(PhysicalAddress lower, PhysicalAddress upper)
{
    ASSERT(!m_pages);

    m_lower = lower;
    m_upper = upper;
}

unsigned PhysicalRegion::finalize_capacity()
{
    ASSERT(!m_pages);

    m_pages = (m_upper.get() - m_lower.get()) / PAGE_SIZE;
    m_bitmap.grow(m_pages, false);

    return size();
}

RefPtr<PhysicalPage> PhysicalRegion::take_free_page(bool supervisor)
{
    ASSERT(m_pages);

    if (m_used == m_pages)
        return nullptr;

    // search from the last page we allocated
    for (unsigned page = m_last; page < m_pages; page++) {
        if (!m_bitmap.get(page)) {
            m_bitmap.set(page, true);
            m_used++;
            m_last = page + 1;
            return PhysicalPage::create(m_lower.offset(page * PAGE_SIZE), supervisor);
        }
    }

    // wrap back around to the start in case we missed something
    for (unsigned page = 0; page < m_last; page++) {
        if (!m_bitmap.get(page)) {
            m_bitmap.set(page, true);
            m_used++;
            m_last = page + 1;
            return PhysicalPage::create(m_lower.offset(page * PAGE_SIZE), supervisor);
        }
    }

    ASSERT_NOT_REACHED();

    return nullptr;
}

void PhysicalRegion::return_page_at(PhysicalAddress addr)
{
    ASSERT(m_pages);

    if (m_used == 0) {
        ASSERT_NOT_REACHED();
    }

    int local_offset = addr.get() - m_lower.get();
    ASSERT(local_offset >= 0);
    ASSERT(local_offset < (int)(m_pages * PAGE_SIZE));

    auto page = (unsigned)local_offset / PAGE_SIZE;
    if (page < m_last)
        m_last = page;

    m_bitmap.set(page, false);
    m_used--;
}
