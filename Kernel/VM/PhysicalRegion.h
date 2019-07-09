#pragma once

#include <AK/Bitmap.h>
#include <AK/RefCounted.h>
#include <AK/NonnullRefPtr.h>
#include <Kernel/VM/PhysicalPage.h>

class PhysicalRegion : public RefCounted<PhysicalRegion> {
    AK_MAKE_ETERNAL

public:
    static NonnullRefPtr<PhysicalRegion> create(PhysicalAddress lower, PhysicalAddress upper);
    ~PhysicalRegion() {}

    void expand(PhysicalAddress lower, PhysicalAddress upper);
    unsigned finalize_capacity();

    PhysicalAddress lower() const { return m_lower; }
    PhysicalAddress upper() const { return m_upper; }
    unsigned size() const { return m_pages; }
    unsigned used() const { return m_used; }
    unsigned free() const { return m_pages - m_used; }
    bool contains(PhysicalPage& page) const { return page.paddr() >= m_lower && page.paddr() <= m_upper; }

    RefPtr<PhysicalPage> take_free_page(bool supervisor);
    void return_page_at(PhysicalAddress addr);
    void return_page(PhysicalPage&& page) { return_page_at(page.paddr()); }

private:
    PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper);

    PhysicalAddress m_lower;
    PhysicalAddress m_upper;
    unsigned m_pages { 0 };
    unsigned m_used { 0 };
    unsigned m_last { 0 };
    Bitmap m_bitmap;
};
