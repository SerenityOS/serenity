#include <Kernel/Process.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>

static const u32 userspace_range_base = 0x01000000;
static const u32 kernelspace_range_base = 0xc0800000;

static HashMap<u32, PageDirectory*>& cr3_map()
{
    ASSERT_INTERRUPTS_DISABLED();
    static HashMap<u32, PageDirectory*>* map;
    if (!map)
        map = new HashMap<u32, PageDirectory*>;
    return *map;
}

RefPtr<PageDirectory> PageDirectory::find_by_cr3(u32 cr3)
{
    InterruptDisabler disabler;
    return cr3_map().get(cr3).value_or({});
}

extern "C" PageDirectoryEntry* boot_pdpt[4];
extern "C" PageDirectoryEntry boot_pd0[1024];
extern "C" PageDirectoryEntry boot_pd3[1024];

PageDirectory::PageDirectory()
    : m_range_allocator(VirtualAddress(0xc0c00000), 0x3f000000)
{
    // Adopt the page tables already set up by boot.S
    PhysicalAddress boot_pdpt_paddr(virtual_to_low_physical((u32)boot_pdpt));
    PhysicalAddress boot_pd0_paddr(virtual_to_low_physical((u32)boot_pd0));
    PhysicalAddress boot_pd3_paddr(virtual_to_low_physical((u32)boot_pd3));
    kprintf("MM: boot_pdpt @ P%p\n", boot_pdpt_paddr.get());
    kprintf("MM: boot_pd0 @ P%p\n", boot_pd0_paddr.get());
    kprintf("MM: boot_pd3 @ P%p\n", boot_pd3_paddr.get());
    m_directory_table = PhysicalPage::create(boot_pdpt_paddr, true, false);
    m_directory_pages[0] = PhysicalPage::create(boot_pd0_paddr, true, false);
    m_directory_pages[3] = PhysicalPage::create(boot_pd3_paddr, true, false);
}

PageDirectory::PageDirectory(Process& process, const RangeAllocator* parent_range_allocator)
    : m_process(&process)
    , m_range_allocator(parent_range_allocator ? RangeAllocator(*parent_range_allocator) : RangeAllocator(VirtualAddress(userspace_range_base), kernelspace_range_base - userspace_range_base))
{
    // Set up a userspace page directory
    m_directory_table = MM.allocate_supervisor_physical_page();
    m_directory_pages[0] = MM.allocate_user_physical_page();
    m_directory_pages[1] = MM.allocate_user_physical_page();
    m_directory_pages[2] = MM.allocate_user_physical_page();
    // Share the top 1 GB of kernel-only mappings (>=3GB or >=0xc0000000)
    m_directory_pages[3] = MM.kernel_page_directory().m_directory_pages[3];

    table().raw[0] = (u64)m_directory_pages[0]->paddr().as_ptr() | 1;
    table().raw[1] = (u64)m_directory_pages[1]->paddr().as_ptr() | 1;
    table().raw[2] = (u64)m_directory_pages[2]->paddr().as_ptr() | 1;
    table().raw[3] = (u64)m_directory_pages[3]->paddr().as_ptr() | 1;

    // Clone bottom 8 MB of mappings from kernel_page_directory
    PageDirectoryEntry buffer[4];
    auto* kernel_pd = MM.quickmap_pd(MM.kernel_page_directory(), 0);
    memcpy(buffer, kernel_pd, sizeof(PageDirectoryEntry) * 4);
    auto* new_pd = MM.quickmap_pd(*this, 0);
    memcpy(new_pd, buffer, sizeof(PageDirectoryEntry) * 4);

    InterruptDisabler disabler;
    cr3_map().set(cr3(), this);
}

PageDirectory::~PageDirectory()
{
#ifdef MM_DEBUG
    dbgprintf("MM: ~PageDirectory K%x\n", this);
#endif
    InterruptDisabler disabler;
    cr3_map().remove(cr3());
}
