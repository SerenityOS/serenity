#include <AK/StringView.h>
#include <Kernel/StdLib.h>
#include <Kernel/Tasks/SwapTask.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/SwapArea.h>

#define SWAP_AREA_DEBUG

namespace Kernel {

#define PAGE_REF_PENDING (1u << 31)

#define PAGE_REF_COUNT_MASK (~(PAGE_REF_PENDING))

OwnPtr<SwapArea> SwapArea::create(size_t page_count, u8 area_index)
{
    // page_count is the minimum number of pages, round up to use full pages
    auto region = MM.allocate_kernel_region(PAGE_ROUND_UP(sizeof(m_page_refs[0]) * page_count), "SwapArea", Region::Access::Read | Region::Access::Write, false, AllocationStrategy::AllocateNow);
    if (!region)
        return {};
    return make<SwapArea>(move(region), area_index);
}

SwapArea::SwapArea(OwnPtr<Region>&& region, u8 area_index)
    : m_page_refs_region(move(region))
    , m_page_refs((u32*)m_page_refs_region->vaddr().as_ptr())
    , m_page_count(m_page_refs_region->size() / sizeof(m_page_refs[0]))
    , m_pages_available(m_page_count)
    , m_area_index(area_index)
{
#ifdef SWAP_AREA_DEBUG
    dbg() << "SwapArea[" << m_area_index << " allocated for " << m_page_count << " pages";
#endif
}

Optional<u32> SwapArea::allocate_entry()
{
    // TODO: optimize
    ScopedSpinLock lock(m_lock);
    if (m_pages_available == 0)
        return {};
    for (size_t i = 0; i < m_page_count; i++) {
        auto& refs = m_page_refs[i];
        if (refs == 0) {
            m_pages_available--;
            refs++;
            return i;
        }
    }
    ASSERT_NOT_REACHED();
}

u32 SwapArea::ref_entry(u32 page_index)
{
    ASSERT(page_index < m_page_count);

    ScopedSpinLock lock(m_lock);
    auto& refs = m_page_refs[page_index];
    ASSERT(refs > 0);
    return ++refs;
}

bool SwapArea::unref_entry(u32 page_index)
{
    ASSERT(page_index < m_page_count);

    ScopedSpinLock lock(m_lock);
    auto& refs = m_page_refs[page_index];
    if ((--refs & PAGE_REF_COUNT_MASK) == 0) {
        if (refs & PAGE_REF_PENDING)
            cancel_page_swap_out(page_index);
        ASSERT(refs == 0);
        ASSERT(m_pages_available < m_page_count);
        m_pages_available++;
        return true;
    }
    return false;
}

void SwapArea::queue_page_swap_out(RefPtr<PhysicalPage>&& page)
{
    ScopedSpinLock lock(m_lock);
    ASSERT(page->ref_count() == 1);
    ASSERT(page->swap_entry().is_swap_entry());
    ASSERT(page->swap_entry().get_swap_area() == m_area_index);
    ASSERT(page->swap_entry().get_swap_index() <= m_page_count);
    ASSERT(!page->m_list.is_in_list());
    m_pending_swap_out.append(*page.leak_ref()); // the list owns it now

    SwapTask::notify_pending_swap_out(m_area_index);
}

void SwapArea::cancel_page_swap_out(u32 page_index)
{
    ASSERT(m_lock.is_locked());
    PhysicalPage* page = nullptr;
    auto& refs = m_page_refs[page_index];
    ASSERT(refs & PAGE_REF_PENDING);
    // TODO: This could use some optimization
    for (auto& p : m_pending_swap_out) {
        ASSERT(p.swap_entry().is_swap_entry());
        ASSERT(p.swap_entry().get_swap_area() == m_area_index);
        u32 index = p.swap_entry().get_swap_index();
        ASSERT(index < m_page_count);
        if (index == page_index) {
            page = &p;
            break;
        }
    }

    ASSERT(page);
    refs &= ~PAGE_REF_PENDING;
    m_pending_swap_out.remove(*page);

    ASSERT(page->ref_count() == 1);
    page->unref(); // drop the last reference
}

void SwapArea::page_was_swapped_out(PhysicalPage* page)
{
    // Destroy, but to not free the PhysicalPage instance
    ASSERT(page->ref_count() == 1);
    page->~PhysicalPage();

    if constexpr(KFREE_SCRUB_BYTE != 0)
        memset((void*)page, KFREE_SCRUB_BYTE, sizeof(*page));

    auto* free_entry = reinterpret_cast<FreePhysicalPageListEntry*>(page);
    FreePhysicalPageListEntry* next_free = nullptr;
    do {
        free_entry->next = next_free;
    } while (!m_free_physical_page_list.compare_exchange_strong(next_free, free_entry, AK::memory_order_acq_rel));
    
}

}
