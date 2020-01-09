#include <Kernel/Process.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>

static const u32 userspace_range_base = 0x01000000;
static const u32 kernelspace_range_base = 0xc0000000;

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

PageDirectory::PageDirectory(PhysicalAddress paddr)
    : m_range_allocator(VirtualAddress(0xc0000000), 0x3f000000)
{
    m_directory_table = PhysicalPage::create(paddr, true, false);
    m_directory_pages[0] = PhysicalPage::create(paddr.offset(PAGE_SIZE * 1), true, false);
    m_directory_pages[1] = PhysicalPage::create(paddr.offset(PAGE_SIZE * 2), true, false);
    m_directory_pages[2] = PhysicalPage::create(paddr.offset(PAGE_SIZE * 3), true, false);
    m_directory_pages[3] = PhysicalPage::create(paddr.offset(PAGE_SIZE * 4), true, false);

    table().raw[0] = (u64)m_directory_pages[0]->paddr().as_ptr() | 1;
    table().raw[1] = (u64)m_directory_pages[1]->paddr().as_ptr() | 1;
    table().raw[2] = (u64)m_directory_pages[2]->paddr().as_ptr() | 1;
    table().raw[3] = (u64)m_directory_pages[3]->paddr().as_ptr() | 1;

    InterruptDisabler disabler;
    cr3_map().set(cr3(), this);
}

PageDirectory::PageDirectory(Process& process, const RangeAllocator* parent_range_allocator)
    : m_process(&process)
    , m_range_allocator(parent_range_allocator ? RangeAllocator(*parent_range_allocator) : RangeAllocator(VirtualAddress(userspace_range_base), kernelspace_range_base - userspace_range_base))
{
    // Set up a userspace page directory

    m_directory_table = MM.allocate_supervisor_physical_page();
    m_directory_pages[0] = MM.allocate_supervisor_physical_page();
    m_directory_pages[1] = MM.allocate_supervisor_physical_page();
    m_directory_pages[2] = MM.allocate_supervisor_physical_page();
    // Share the top 1 GB of kernel-only mappings (>=3GB or >=0xc0000000)
    m_directory_pages[3] = MM.kernel_page_directory().m_directory_pages[3];

    table().raw[0] = (u64)m_directory_pages[0]->paddr().as_ptr() | 1;
    table().raw[1] = (u64)m_directory_pages[1]->paddr().as_ptr() | 1;
    table().raw[2] = (u64)m_directory_pages[2]->paddr().as_ptr() | 1;
    table().raw[3] = (u64)m_directory_pages[3]->paddr().as_ptr() | 1;

    // Clone bottom 8 MB of mappings from kernel_page_directory
    table().directory(0)[0].copy_from({}, MM.kernel_page_directory().table().directory(0)[0]);
    table().directory(0)[1].copy_from({}, MM.kernel_page_directory().table().directory(0)[1]);
    table().directory(0)[2].copy_from({}, MM.kernel_page_directory().table().directory(0)[2]);
    table().directory(0)[3].copy_from({}, MM.kernel_page_directory().table().directory(0)[3]);

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