#include "MemoryManager.h"
#include <AK/Assertions.h>
#include <AK/kstdio.h>
#include <AK/kmalloc.h>
#include "i386.h"
#include "StdLib.h"
#include "Process.h"

//#define MM_DEBUG
#define SCRUB_DEALLOCATED_PAGE_TABLES

static MemoryManager* s_the;

MemoryManager& MM
{
    return *s_the;
}

MemoryManager::MemoryManager()
{
    m_kernel_page_directory = (PageDirectory*)0x4000;
    m_pageTableZero = (dword*)0x6000;
    m_pageTableOne = (dword*)0x7000;

    m_next_laddr.set(0xd0000000);

    initializePaging();
}

MemoryManager::~MemoryManager()
{
}

void MemoryManager::populate_page_directory(PageDirectory& page_directory)
{
    memset(&page_directory, 0, sizeof(PageDirectory));
    page_directory.entries[0] = m_kernel_page_directory->entries[0];
    page_directory.entries[1] = m_kernel_page_directory->entries[1];
}

void MemoryManager::release_page_directory(PageDirectory& page_directory)
{
    ASSERT_INTERRUPTS_DISABLED();
#ifdef MM_DEBUG
    dbgprintf("MM: release_page_directory for PD K%x\n", &page_directory);
#endif
    for (size_t i = 0; i < 1024; ++i) {
        auto& page_table = page_directory.physical_pages[i];
        if (!page_table.is_null()) {
#ifdef MM_DEBUG
            dbgprintf("MM: deallocating user page table P%x\n", page_table->paddr().get());
#endif
            deallocate_page_table(page_directory, i);
        }
    }
#ifdef SCRUB_DEALLOCATED_PAGE_TABLES
    memset(&page_directory, 0xc9, sizeof(PageDirectory));
#endif
}

void MemoryManager::initializePaging()
{
    static_assert(sizeof(MemoryManager::PageDirectoryEntry) == 4);
    static_assert(sizeof(MemoryManager::PageTableEntry) == 4);
    memset(m_pageTableZero, 0, PAGE_SIZE);
    memset(m_pageTableOne, 0, PAGE_SIZE);
    memset(m_kernel_page_directory, 0, sizeof(PageDirectory));

#ifdef MM_DEBUG
    kprintf("MM: Kernel page directory @ %p\n", m_kernel_page_directory);
#endif

    // Make null dereferences crash.
    protectMap(LinearAddress(0), PAGE_SIZE);

    // The bottom 4 MB are identity mapped & supervisor only. Every process shares these mappings.
    create_identity_mapping(LinearAddress(PAGE_SIZE), 4 * MB);
 
    // The physical pages 4 MB through 8 MB are available for allocation.
    for (size_t i = (4 * MB) + PAGE_SIZE; i < (8 * MB); i += PAGE_SIZE)
        m_free_physical_pages.append(adopt(*new PhysicalPage(PhysicalAddress(i))));

    asm volatile("movl %%eax, %%cr3"::"a"(m_kernel_page_directory));
    asm volatile(
        "movl %cr0, %eax\n"
        "orl $0x80000001, %eax\n"
        "movl %eax, %cr0\n"
    );
}

RetainPtr<PhysicalPage> MemoryManager::allocate_page_table(PageDirectory& page_directory, unsigned index)
{
    auto& page_directory_physical_ptr = page_directory.physical_pages[index];
    ASSERT(!page_directory_physical_ptr);
    auto ppages = allocate_physical_pages(1);
    ASSERT(ppages.size() == 1);
    dword address = ppages[0]->paddr().get();
    create_identity_mapping(LinearAddress(address), PAGE_SIZE);
    memset((void*)address, 0, PAGE_SIZE);
    page_directory.physical_pages[index] = move(ppages[0]);
    return page_directory.physical_pages[index];
}

void MemoryManager::deallocate_page_table(PageDirectory& page_directory, unsigned index)
{
    auto& physical_page = page_directory.physical_pages[index];
    ASSERT(physical_page);
    ASSERT(!m_free_physical_pages.contains_slow(physical_page));
    for (size_t i = 0; i < MM.m_free_physical_pages.size(); ++i) {
        ASSERT(MM.m_free_physical_pages[i].ptr() != physical_page.ptr());
    }
    remove_identity_mapping(LinearAddress(physical_page->paddr().get()), PAGE_SIZE);
    page_directory.physical_pages[index] = nullptr;
}

void MemoryManager::remove_identity_mapping(LinearAddress laddr, size_t size)
{
    InterruptDisabler disabler;
    // FIXME: ASSERT(laddr is 4KB aligned);
    for (dword offset = 0; offset < size; offset += PAGE_SIZE) {
        auto pte_address = laddr.offset(offset);
        auto pte = ensurePTE(m_kernel_page_directory, pte_address);
        pte.setPhysicalPageBase(0);
        pte.setUserAllowed(false);
        pte.setPresent(true);
        pte.setWritable(true);
        flushTLB(pte_address);
    }
}

auto MemoryManager::ensurePTE(PageDirectory* page_directory, LinearAddress laddr) -> PageTableEntry
{
    ASSERT_INTERRUPTS_DISABLED();
    dword page_directory_index = (laddr.get() >> 22) & 0x3ff;
    dword page_table_index = (laddr.get() >> 12) & 0x3ff;

    PageDirectoryEntry pde = PageDirectoryEntry(&page_directory->entries[page_directory_index]);
    if (!pde.isPresent()) {
#ifdef MM_DEBUG
        dbgprintf("MM: PDE %u not present, allocating\n", page_directory_index);
#endif
        if (page_directory_index == 0) {
            ASSERT(page_directory == m_kernel_page_directory);
            pde.setPageTableBase((dword)m_pageTableZero);
            pde.setUserAllowed(false);
            pde.setPresent(true);
            pde.setWritable(true);
        } else if (page_directory_index == 1) {
            ASSERT(page_directory == m_kernel_page_directory);
            pde.setPageTableBase((dword)m_pageTableOne);
            pde.setUserAllowed(false);
            pde.setPresent(true);
            pde.setWritable(true);
        } else {
            auto page_table = allocate_page_table(*page_directory, page_directory_index);
#ifdef MM_DEBUG
            dbgprintf("MM: PD K%x (%s) allocated page table #%u (for L%x) at P%x\n",
                page_directory,
                page_directory == m_kernel_page_directory ? "Kernel" : "User",
                page_directory_index,
                laddr.get(),
                page_table->paddr().get());
#endif

            pde.setPageTableBase(page_table->paddr().get());
            pde.setUserAllowed(true);
            pde.setPresent(true);
            pde.setWritable(true);
            page_directory->physical_pages[page_directory_index] = move(page_table);
        }
    }
    return PageTableEntry(&pde.pageTableBase()[page_table_index]);
}

void MemoryManager::protectMap(LinearAddress linearAddress, size_t length)
{
    InterruptDisabler disabler;
    // FIXME: ASSERT(linearAddress is 4KB aligned);
    for (dword offset = 0; offset < length; offset += PAGE_SIZE) {
        auto pteAddress = linearAddress.offset(offset);
        auto pte = ensurePTE(m_kernel_page_directory, pteAddress);
        pte.setPhysicalPageBase(pteAddress.get());
        pte.setUserAllowed(false);
        pte.setPresent(false);
        pte.setWritable(false);
        flushTLB(pteAddress);
    }
}

void MemoryManager::create_identity_mapping(LinearAddress laddr, size_t size)
{
    InterruptDisabler disabler;
    // FIXME: ASSERT(laddr is 4KB aligned);
    for (dword offset = 0; offset < size; offset += PAGE_SIZE) {
        auto pteAddress = laddr.offset(offset);
        auto pte = ensurePTE(m_kernel_page_directory, pteAddress);
        pte.setPhysicalPageBase(pteAddress.get());
        pte.setUserAllowed(false);
        pte.setPresent(true);
        pte.setWritable(true);
        flushTLB(pteAddress);
    }
}

void MemoryManager::initialize()
{
    s_the = new MemoryManager;
}

PageFaultResponse MemoryManager::handlePageFault(const PageFault& fault)
{
    ASSERT_INTERRUPTS_DISABLED();
    kprintf("MM: handlePageFault(%w) at L%x\n", fault.code(), fault.address().get());
    if (fault.isNotPresent()) { 
        kprintf("  >> NP fault!\n");
    } else if (fault.isProtectionViolation()) {
        kprintf("  >> PV fault!\n");
    }
    return PageFaultResponse::ShouldCrash;
}

Vector<RetainPtr<PhysicalPage>> MemoryManager::allocate_physical_pages(size_t count)
{
    InterruptDisabler disabler;
    if (count > m_free_physical_pages.size())
        return { };

    Vector<RetainPtr<PhysicalPage>> pages;
    pages.ensureCapacity(count);
    for (size_t i = 0; i < count; ++i) {
        pages.append(m_free_physical_pages.takeLast());
#ifdef MM_DEBUG
        dbgprintf("MM: allocate_physical_pages vending P%x\n", pages.last()->paddr().get());
#endif
    }
    return pages;
}

void MemoryManager::enter_kernel_paging_scope()
{
    InterruptDisabler disabler;
    current->m_tss.cr3 = (dword)m_kernel_page_directory;
    asm volatile("movl %%eax, %%cr3"::"a"(m_kernel_page_directory):"memory");
}

void MemoryManager::enter_process_paging_scope(Process& process)
{
    InterruptDisabler disabler;
    current->m_tss.cr3 = (dword)process.m_page_directory;
    asm volatile("movl %%eax, %%cr3"::"a"(process.m_page_directory):"memory");
}

void MemoryManager::flushEntireTLB()
{
    asm volatile(
        "mov %cr3, %eax\n"
        "mov %eax, %cr3\n"
     );
}

void MemoryManager::flushTLB(LinearAddress laddr)
{
    asm volatile("invlpg %0": :"m" (*(char*)laddr.get()) : "memory");
}

void MemoryManager::map_region_at_address(PageDirectory* page_directory, Region& region, LinearAddress laddr, bool user_allowed)
{
    InterruptDisabler disabler;
    for (size_t i = 0; i < region.physical_pages.size(); ++i) {
        auto page_laddr = laddr.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(page_directory, page_laddr);
        auto& physical_page = region.physical_pages[i];
        if (physical_page) {
            pte.setPhysicalPageBase(physical_page->paddr().get());
            pte.setPresent(true); // FIXME: Maybe we should use the is_readable flag here?
        } else {
            pte.setPhysicalPageBase(0);
            pte.setPresent(false);
        }
        pte.setWritable(region.is_writable);
        pte.setUserAllowed(user_allowed);
        flushTLB(page_laddr);
#ifdef MM_DEBUG
        dbgprintf("MM: >> map_region_at_address (PD=%x) '%s' L%x => P%x (@%p)\n", page_directory, region.name.characters(), page_laddr, physical_page ? physical_page->paddr().get() : 0, physical_page.ptr());
#endif
    }
}

void MemoryManager::unmap_range(PageDirectory* page_directory, LinearAddress laddr, size_t size)
{
    ASSERT((size % PAGE_SIZE) == 0);

    InterruptDisabler disabler;
    size_t numPages = size / PAGE_SIZE;
    for (size_t i = 0; i < numPages; ++i) {
        auto page_laddr = laddr.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(page_directory, page_laddr);
        pte.setPhysicalPageBase(0);
        pte.setPresent(false);
        pte.setWritable(false);
        pte.setUserAllowed(false);
        flushTLB(page_laddr);
#ifdef MM_DEBUG
        dbgprintf("MM: << unmap_range L%x =/> 0\n", page_laddr);
#endif
    }
}

LinearAddress MemoryManager::allocate_linear_address_range(size_t size)
{
    ASSERT((size % PAGE_SIZE) == 0);

    // FIXME: Recycle ranges!
    auto laddr = m_next_laddr;
    m_next_laddr.set(m_next_laddr.get() + size);
    return laddr;
}

byte* MemoryManager::create_kernel_alias_for_region(Region& region)
{
    InterruptDisabler disabler;
#ifdef MM_DEBUG
    dbgprintf("MM: create_kernel_alias_for_region region=%p (L%x size=%u)\n", &region, region.linearAddress.get(), region.size);
#endif
    auto laddr = allocate_linear_address_range(region.size);
    map_region_at_address(m_kernel_page_directory, region, laddr, false);
#ifdef MM_DEBUG
    dbgprintf("MM: Created alias L%x for L%x\n", laddr.get(), region.linearAddress.get());
#endif
    return laddr.asPtr();
}

void MemoryManager::remove_kernel_alias_for_region(Region& region, byte* addr)
{
#ifdef MM_DEBUG
    dbgprintf("remove_kernel_alias_for_region region=%p, addr=L%x\n", &region, addr);
#endif
    unmap_range(m_kernel_page_directory, LinearAddress((dword)addr), region.size);
}

bool MemoryManager::unmapRegion(Process& process, Region& region)
{
    InterruptDisabler disabler;
    for (size_t i = 0; i < region.physical_pages.size(); ++i) {
        auto laddr = region.linearAddress.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(process.m_page_directory, laddr);
        pte.setPhysicalPageBase(0);
        pte.setPresent(false);
        pte.setWritable(false);
        pte.setUserAllowed(false);
        flushTLB(laddr);
#ifdef MM_DEBUG
        auto& physical_page = region.physical_pages[i];
        dbgprintf("MM: >> Unmapped L%x => P%x <<\n", laddr, physical_page ? physical_page->paddr().get() : 0);
#endif
    }
    return true;
}

bool MemoryManager::mapRegion(Process& process, Region& region)
{
    map_region_at_address(process.m_page_directory, region, region.linearAddress, true);
    return true;
}

bool MemoryManager::validate_user_read(const Process& process, LinearAddress laddr) const
{
    dword pageDirectoryIndex = (laddr.get() >> 22) & 0x3ff;
    dword pageTableIndex = (laddr.get() >> 12) & 0x3ff;
    auto pde = PageDirectoryEntry(&process.m_page_directory->entries[pageDirectoryIndex]);
    if (!pde.isPresent())
        return false;
    auto pte = PageTableEntry(&pde.pageTableBase()[pageTableIndex]);
    if (!pte.isPresent())
        return false;
    if (!pte.isUserAllowed())
        return false;
    return true;
}

bool MemoryManager::validate_user_write(const Process& process, LinearAddress laddr) const
{
    dword pageDirectoryIndex = (laddr.get() >> 22) & 0x3ff;
    dword pageTableIndex = (laddr.get() >> 12) & 0x3ff;
    auto pde = PageDirectoryEntry(&process.m_page_directory->entries[pageDirectoryIndex]);
    if (!pde.isPresent())
        return false;
    auto pte = PageTableEntry(&pde.pageTableBase()[pageTableIndex]);
    if (!pte.isPresent())
        return false;
    if (!pte.isUserAllowed())
        return false;
    if (!pte.isWritable())
        return false;
    return true;
}

RetainPtr<Region> Region::clone()
{
    InterruptDisabler disabler;
    KernelPagingScope pagingScope;

    if (is_readable && !is_writable) {
        // Create a new region backed by the same physical pages.
        return adopt(*new Region(linearAddress, size, physical_pages, String(name), is_readable, is_writable));
    }
    // FIXME: Implement COW regions.
    auto clone_physical_pages = MM.allocate_physical_pages(physical_pages.size());
    auto clone_region = adopt(*new Region(linearAddress, size, move(clone_physical_pages), String(name), is_readable, is_writable));

    // FIXME: It would be cool to make the src_alias a read-only mapping.
    byte* src_alias = MM.create_kernel_alias_for_region(*this);
    byte* dest_alias = MM.create_kernel_alias_for_region(*clone_region);

    memcpy(dest_alias, src_alias, size);

    MM.remove_kernel_alias_for_region(*clone_region, dest_alias);
    MM.remove_kernel_alias_for_region(*this, src_alias);
    return clone_region;
}

Region::Region(LinearAddress a, size_t s, Vector<RetainPtr<PhysicalPage>> pp, String&& n, bool r, bool w)
    : linearAddress(a)
    , size(s)
    , physical_pages(move(pp))
    , name(move(n))
    , is_readable(r)
    , is_writable(w)
{
}

Region::~Region()
{
}

void PhysicalPage::return_to_freelist()
{
    InterruptDisabler disabler;
    m_retain_count = 1;
    MM.m_free_physical_pages.append(adopt(*this));
#ifdef MM_DEBUG
    dbgprintf("MM: P%x released to freelist\n", m_paddr.get());
#endif
}
