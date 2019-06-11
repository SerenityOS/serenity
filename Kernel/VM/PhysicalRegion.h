#pragma once

#include <AK/Bitmap.h>
#include <AK/Retainable.h>
#include <AK/Retained.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>

class PhysicalRegion : public Retainable<PhysicalRegion> {
    AK_MAKE_ETERNAL

public:
    static Retained<PhysicalRegion> create(PhysicalAddress lower, PhysicalAddress upper);
    ~PhysicalRegion() {}

    PhysicalAddress lower() { return m_lower; }
    PhysicalAddress upper() { return m_upper; }
    unsigned size() const { return m_pages; }
    unsigned used() const { return m_used; }
    unsigned free() const { return m_pages - m_used; }
    bool owns_page(PhysicalPage& page) const { return page.paddr() >= m_lower && page.paddr() <= m_upper; }

    PhysicalAddress take_free_page();
    void return_page_at(PhysicalAddress addr);
    void return_page(PhysicalPage& page) { return_page_at(page.paddr()); }

private:
    PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper);

    PhysicalAddress m_lower;
    PhysicalAddress m_upper;
    unsigned m_pages{ 0 };
    unsigned m_used{ 0 };
    Bitmap m_mask;
};
