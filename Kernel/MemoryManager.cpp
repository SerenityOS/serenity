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

void MemoryManager::populate_page_directory(Process& process)
{
    memset(process.m_page_directory, 0, sizeof(PageDirectory));
    process.m_page_directory->entries[0] = m_kernel_page_directory->entries[0];
    process.m_page_directory->entries[1] = m_kernel_page_directory->entries[1];
}

void MemoryManager::release_page_directory(Process& process)
{
    ASSERT_INTERRUPTS_DISABLED();
#ifdef MM_DEBUG
    dbgprintf("MM: release_page_directory for pid %d, PD K%x\n", process.pid(), process.m_page_directory);
#endif
    for (size_t i = 0; i < 1024; ++i) {
        auto page_table = process.m_page_directory->physical_addresses[i];
        if (!page_table.is_null()) {
#ifdef MM_DEBUG
            dbgprintf("MM: deallocating process page table [%u] P%x @ %p\n", i, page_table.get(), &process.m_page_directory->physical_addresses[i]);
#endif
            deallocate_page_table(page_table);
        }
    }
#ifdef SCRUB_DEALLOCATED_PAGE_TABLES
    memset(process.m_page_directory, 0xc9, sizeof(PageDirectory));
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
 
    // The physical pages 4 MB through 8 MB are available for Zone allocation.
    for (size_t i = (4 * MB) + PAGE_SIZE; i < (8 * MB); i += PAGE_SIZE)
        m_freePages.append(PhysicalAddress(i));

    asm volatile("movl %%eax, %%cr3"::"a"(m_kernel_page_directory));
    asm volatile(
        "movl %cr0, %eax\n"
        "orl $0x80000001, %eax\n"
        "movl %eax, %cr0\n"
    );
}

PhysicalAddress MemoryManager::allocate_page_table()
{
    auto ppages = allocatePhysicalPages(1);
    dword address = ppages[0].get();
    create_identity_mapping(LinearAddress(address), PAGE_SIZE);
    memset((void*)address, 0, PAGE_SIZE);
    return PhysicalAddress(address);
}

void MemoryManager::deallocate_page_table(PhysicalAddress paddr)
{
    ASSERT(!m_freePages.contains_slow(paddr));
    remove_identity_mapping(LinearAddress(paddr.get()), PAGE_SIZE);
    m_freePages.append(paddr);
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
            auto page_table = allocate_page_table();
#ifdef MM_DEBUG
            dbgprintf("MM: PD K%x (%s) allocated page table #%u (for L%x) at P%x\n",
                page_directory,
                page_directory == m_kernel_page_directory ? "Kernel" : "User",
                page_directory_index,
                laddr.get(),
                page_table);
#endif
            if (page_table.get() == 0x71d000)
                ASSERT(page_directory == m_kernel_page_directory);
            page_directory->physical_addresses[page_directory_index] = page_table;
            pde.setPageTableBase(page_table.get());
            pde.setUserAllowed(true);
            pde.setPresent(true);
            pde.setWritable(true);
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

void MemoryManager::registerZone(Zone& zone)
{
    ASSERT_INTERRUPTS_DISABLED();
    m_zones.set(&zone);
#ifdef MM_DEBUG
    for (size_t i = 0; i < zone.m_pages.size(); ++i)
        dbgprintf("MM: allocated to zone: P%x\n", zone.m_pages[i].get());
#endif
}

void MemoryManager::unregisterZone(Zone& zone)
{
    ASSERT_INTERRUPTS_DISABLED();
#ifdef MM_DEBUG
    for (size_t i = 0; i < zone.m_pages.size(); ++i)
        dbgprintf("MM: deallocated from zone: P%x\n", zone.m_pages[i].get());
#endif
    m_zones.remove(&zone);
    m_freePages.append(move(zone.m_pages));
}

Zone::Zone(Vector<PhysicalAddress>&& pages)
    : m_pages(move(pages))
{
    MM.registerZone(*this);
}

Zone::~Zone()
{
    MM.unregisterZone(*this);
}

RetainPtr<Zone> MemoryManager::createZone(size_t size)
{
    InterruptDisabler disabler;
    auto pages = allocatePhysicalPages(ceilDiv(size, PAGE_SIZE));
    if (pages.isEmpty()) {
        kprintf("MM: createZone: no physical pages for size %u\n", size);
        return nullptr;
    }
    return adopt(*new Zone(move(pages)));
}

Vector<PhysicalAddress> MemoryManager::allocatePhysicalPages(size_t count)
{
    InterruptDisabler disabler;
    if (count > m_freePages.size())
        return { };

    Vector<PhysicalAddress> pages;
    pages.ensureCapacity(count);
    for (size_t i = 0; i < count; ++i) {
        pages.append(m_freePages.takeLast());
#ifdef MM_DEBUG
        dbgprintf("MM: allocate_physical_pages vending P%x\n", pages.last());
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
    auto& zone = *region.zone;
    for (size_t i = 0; i < zone.m_pages.size(); ++i) {
        auto page_laddr = laddr.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(page_directory, page_laddr);
        pte.setPhysicalPageBase(zone.m_pages[i].get());
        pte.setPresent(true);
        pte.setWritable(true);
        pte.setUserAllowed(user_allowed);
        flushTLB(page_laddr);
#ifdef MM_DEBUG
        dbgprintf("MM: >> map_region_at_address (PD=%x) L%x => P%x\n", page_directory, page_laddr, zone.m_pages[i].get());
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
    auto& zone = *region.zone;
    for (size_t i = 0; i < zone.m_pages.size(); ++i) {
        auto laddr = region.linearAddress.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(process.m_page_directory, laddr);
        pte.setPhysicalPageBase(0);
        pte.setPresent(false);
        pte.setWritable(false);
        pte.setUserAllowed(false);
        flushTLB(laddr);
#ifdef MM_DEBUG
        //dbgprintf("MM: >> Unmapped L%x => P%x <<\n", laddr, zone.m_pages[i].get());
#endif
    }
    return true;
}

bool MemoryManager::unmapSubregion(Process& process, Subregion& subregion)
{
    InterruptDisabler disabler;
    size_t numPages = subregion.size / PAGE_SIZE;
    ASSERT(numPages);
    for (size_t i = 0; i < numPages; ++i) {
        auto laddr = subregion.linearAddress.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(process.m_page_directory, laddr);
        pte.setPhysicalPageBase(0);
        pte.setPresent(false);
        pte.setWritable(false);
        pte.setUserAllowed(false);
        flushTLB(laddr);
#ifdef MM_DEBUG
        //dbgprintf("MM: >> Unmapped subregion %s L%x => P%x <<\n", subregion.name.characters(), laddr, zone.m_pages[i].get());
#endif
    }
    return true;
}

bool MemoryManager::mapSubregion(Process& process, Subregion& subregion)
{
    InterruptDisabler disabler;
    auto& region = *subregion.region;
    auto& zone = *region.zone;
    size_t firstPage = subregion.offset / PAGE_SIZE;
    size_t numPages = subregion.size / PAGE_SIZE;
    ASSERT(numPages);
    for (size_t i = 0; i < numPages; ++i) {
        auto laddr = subregion.linearAddress.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(process.m_page_directory, laddr);
        pte.setPhysicalPageBase(zone.m_pages[firstPage + i].get());
        pte.setPresent(true);
        pte.setWritable(true);
        pte.setUserAllowed(true);
        flushTLB(laddr);
#ifdef MM_DEBUG
        //dbgprintf("MM: >> Mapped subregion %s L%x => P%x (%u into region)\n", subregion.name.characters(), laddr, zone.m_pages[firstPage + i].get(), subregion.offset);
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

    // FIXME: Implement COW regions.
    auto clone_zone = MM.createZone(zone->size());
    auto clone_region = adopt(*new Region(linearAddress, size, move(clone_zone), String(name)));

    // FIXME: It would be cool to make the src_alias a read-only mapping.
    byte* src_alias = MM.create_kernel_alias_for_region(*this);
    byte* dest_alias = MM.create_kernel_alias_for_region(*clone_region);

    memcpy(dest_alias, src_alias, size);

    MM.remove_kernel_alias_for_region(*clone_region, dest_alias);
    MM.remove_kernel_alias_for_region(*this, src_alias);
    return clone_region;
}

