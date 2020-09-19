#pragma once

#include <Kernel/SpinLock.h>
#include <Kernel/VM/Region.h>

namespace Kernel {

class SwapArea
{
public:
    typedef IntrusiveList<PhysicalPage, &PhysicalPage::m_list> PhysicalPageList;

    static OwnPtr<SwapArea> create(size_t page_count, u8 area_index);
    explicit SwapArea(OwnPtr<Region>&& region, u8 area_index);

    Optional<u32> allocate_entry();
    u32 ref_entry(u32 page_index);
    bool unref_entry(u32 page_index);
    size_t page_count() const { return m_page_count; }

    void queue_page_swap_out(RefPtr<PhysicalPage>&&);

    template<size_t COUNT>
    Vector<PhysicalPage*, COUNT> dequeue_pending_swap_out_pages()
    {
        ScopedSpinLock lock(m_lock);
        size_t pages_left = COUNT;
        Vector<PhysicalPage*, COUNT> pages;
        while (pages_left > 0) {
            auto* page = m_pending_swap_out.take_first();
            if (!page)
                break;
            ASSERT(page->ref_count() == 1);
            pages.append(page);
            pages_left--;
        }
        return pages;
    }

    void page_was_swapped_out(PhysicalPage*);

private:
    void cancel_page_swap_out(u32 page_index);
    
    struct FreePhysicalPageListEntry {
        FreePhysicalPageListEntry* next;
    };
    static_assert(sizeof(FreePhysicalPageListEntry) < sizeof(PhysicalPage));

    PhysicalPageList m_pending_swap_out;
    OwnPtr<Region> m_page_refs_region;
    u32* m_page_refs;
    size_t m_page_count;
    size_t m_pages_available;
    Atomic<FreePhysicalPageListEntry*> m_free_physical_page_list { nullptr };
    SpinLock<u8> m_lock;
    const u8 m_area_index;
};

}
