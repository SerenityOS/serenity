#include "MemoryManager.h"
#include <AK/Assertions.h>
#include <AK/kstdio.h>
#include <AK/kmalloc.h>
#include "i386.h"
#include "StdLib.h"
#include "Task.h"

static MemoryManager* s_the;

MemoryManager& MM
{
    return *s_the;
}

MemoryManager::MemoryManager()
{
    m_pageDirectory = (dword*)0x5000;
    m_pageTableZero = (dword*)0x6000;
    m_pageTableOne = (dword*)0x7000;

    initializePaging();
}

MemoryManager::~MemoryManager()
{
}

void MemoryManager::initializePaging()
{
    static_assert(sizeof(MemoryManager::PageDirectoryEntry) == 4);
    static_assert(sizeof(MemoryManager::PageTableEntry) == 4);
    memset(m_pageTableZero, 0, 4096);
    memset(m_pageTableOne, 0, 4096);
    memset(m_pageDirectory, 0, 4096);

#ifdef MM_DEBUG
    kprintf("MM: Page directory @ %p\n", m_pageDirectory);
#endif

    // Make null dereferences crash.
    protectMap(LinearAddress(0), 4 * KB);

    identityMap(LinearAddress(4096), 4 * MB);
 
    for (size_t i = (4 * MB) + PAGE_SIZE; i < (8 * MB); i += PAGE_SIZE) {
        m_freePages.append(PhysicalAddress(i));
    }

    asm volatile("movl %%eax, %%cr3"::"a"(m_pageDirectory));
    asm volatile(
        "movl %cr0, %eax\n"
        "orl $0x80000001, %eax\n"
        "movl %eax, %cr0\n"
    );
}

void* MemoryManager::allocatePageTable()
{
    auto ppages = allocatePhysicalPages(1);
    dword address = ppages[0].get();
    identityMap(LinearAddress(address), 4096);
    return (void*)address;
}

auto MemoryManager::ensurePTE(LinearAddress linearAddress) -> PageTableEntry
{
    ASSERT_INTERRUPTS_DISABLED();
    dword pageDirectoryIndex = (linearAddress.get() >> 22) & 0x3ff;
    dword pageTableIndex = (linearAddress.get() >> 12) & 0x3ff;

    PageDirectoryEntry pde = PageDirectoryEntry(&m_pageDirectory[pageDirectoryIndex]);
    if (!pde.isPresent()) {
#ifdef MM_DEBUG
        kprintf("MM: PDE %u not present, allocating\n", pageDirectoryIndex);
#endif
        if (pageDirectoryIndex == 0) {
            pde.setPageTableBase((dword)m_pageTableZero);
            pde.setUserAllowed(true);
            pde.setPresent(true);
            pde.setWritable(true);
        } else if (pageDirectoryIndex == 1) {
            pde.setPageTableBase((dword)m_pageTableOne);
            pde.setUserAllowed(true);
            pde.setPresent(true);
            pde.setWritable(true);
        } else {
            auto* pageTable = allocatePageTable();
            kprintf("MM: Allocated page table #%u (for laddr=%p) at %p\n", pageDirectoryIndex, linearAddress.get(), pageTable);
            memset(pageTable, 0, 4096);
            pde.setPageTableBase((dword)pageTable);
            pde.setUserAllowed(true);
            pde.setPresent(true);
            pde.setWritable(true);
        }
    }
    return PageTableEntry(&pde.pageTableBase()[pageTableIndex]);
}

void MemoryManager::protectMap(LinearAddress linearAddress, size_t length)
{
    InterruptDisabler disabler;
    // FIXME: ASSERT(linearAddress is 4KB aligned);
    for (dword offset = 0; offset < length; offset += 4096) {
        auto pteAddress = linearAddress.offset(offset);
        auto pte = ensurePTE(pteAddress);
        pte.setPhysicalPageBase(pteAddress.get());
        pte.setUserAllowed(false);
        pte.setPresent(false);
        pte.setWritable(false);
        flushTLB(pteAddress);
    }
}

void MemoryManager::identityMap(LinearAddress linearAddress, size_t length)
{
    InterruptDisabler disabler;
    // FIXME: ASSERT(linearAddress is 4KB aligned);
    for (dword offset = 0; offset < length; offset += 4096) {
        auto pteAddress = linearAddress.offset(offset);
        auto pte = ensurePTE(pteAddress);
        pte.setPhysicalPageBase(pteAddress.get());
        pte.setUserAllowed(true);
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
    kprintf("MM: handlePageFault(%w) at laddr=%p\n", fault.code(), fault.address().get());
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
}

void MemoryManager::unregisterZone(Zone& zone)
{
    ASSERT_INTERRUPTS_DISABLED();
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
    for (size_t i = 0; i < count; ++i)
        pages.append(m_freePages.takeLast());
    return pages;
}

byte* MemoryManager::quickMapOnePage(PhysicalAddress physicalAddress)
{
    ASSERT_INTERRUPTS_DISABLED();
    auto pte = ensurePTE(LinearAddress(4 * MB));
    kprintf("MM: quickmap %x @ %x {pte @ %p}\n", physicalAddress.get(), 4*MB, pte.ptr());
    pte.setPhysicalPageBase(physicalAddress.pageBase());
    pte.setPresent(true);
    pte.setWritable(true);
    flushTLB(LinearAddress(4 * MB));
    return (byte*)(4 * MB);
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
    asm volatile("invlpg %0": :"m" (*(char*)laddr.get()));
}

bool MemoryManager::unmapRegion(Task& task, Task::Region& region)
{
    InterruptDisabler disabler;
    auto& zone = *region.zone;
    for (size_t i = 0; i < zone.m_pages.size(); ++i) {
        auto laddr = region.linearAddress.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(laddr);
        pte.setPhysicalPageBase(0);
        pte.setPresent(false);
        pte.setWritable(false);
        pte.setUserAllowed(false);
        flushTLB(laddr);
        //kprintf("MM: >> Unmapped L%x => P%x <<\n", laddr, zone.m_pages[i].get());
    }
    return true;
}

bool MemoryManager::unmapSubregion(Task& task, Task::Subregion& subregion)
{
    InterruptDisabler disabler;
    auto& region = *subregion.region;
    auto& zone = *region.zone;
    size_t numPages = subregion.size / 4096;
    ASSERT(numPages);
    for (size_t i = 0; i < numPages; ++i) {
        auto laddr = subregion.linearAddress.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(laddr);
        pte.setPhysicalPageBase(0);
        pte.setPresent(false);
        pte.setWritable(false);
        pte.setUserAllowed(false);
        flushTLB(laddr);
        //kprintf("MM: >> Unmapped subregion %s L%x => P%x <<\n", subregion.name.characters(), laddr, zone.m_pages[i].get());
    }
    return true;
}

bool MemoryManager::unmapRegionsForTask(Task& task)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto& region : task.m_regions) {
        if (!unmapRegion(task, *region))
            return false;
    }
    for (auto& subregion : task.m_subregions) {
        if (!unmapSubregion(task, *subregion))
            return false;
    }
    return true;
}

bool MemoryManager::mapSubregion(Task& task, Task::Subregion& subregion)
{
    InterruptDisabler disabler;
    auto& region = *subregion.region;
    auto& zone = *region.zone;
    size_t firstPage = subregion.offset / 4096;
    size_t numPages = subregion.size / 4096;
    ASSERT(numPages);
    for (size_t i = 0; i < numPages; ++i) {
        auto laddr = subregion.linearAddress.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(laddr);
        pte.setPhysicalPageBase(zone.m_pages[firstPage + i].get());
        pte.setPresent(true);
        pte.setWritable(true);
        pte.setUserAllowed(!task.isRing0());
        flushTLB(laddr);
        //kprintf("MM: >> Mapped subregion %s L%x => P%x (%u into region)<<\n", subregion.name.characters(), laddr, zone.m_pages[firstPage + i].get(), subregion.offset);
    }
    return true;
}

bool MemoryManager::mapRegion(Task& task, Task::Region& region)
{
    InterruptDisabler disabler;
    auto& zone = *region.zone;
    for (size_t i = 0; i < zone.m_pages.size(); ++i) {
        auto laddr = region.linearAddress.offset(i * PAGE_SIZE);
        auto pte = ensurePTE(laddr);
        pte.setPhysicalPageBase(zone.m_pages[i].get());
        pte.setPresent(true);
        pte.setWritable(true);
        pte.setUserAllowed(!task.isRing0());
        flushTLB(laddr);
        //kprintf("MM: >> Mapped L%x => P%x <<\n", laddr, zone.m_pages[i].get());
    }
    return true;
}

bool MemoryManager::mapRegionsForTask(Task& task)
{
    ASSERT_INTERRUPTS_DISABLED();
    for (auto& region : task.m_regions) {
        if (!mapRegion(task, *region))
            return false;
    }
    for (auto& subregion : task.m_subregions) {
        if (!mapSubregion(task, *subregion))
            return false;
    }
    return true;
}

bool copyToZone(Zone& zone, const void* data, size_t size)
{
    if (zone.size() < size) {
        kprintf("MM: copyToZone: can't fit %u bytes into zone with size %u\n", size, zone.size());
        return false;
    }

    InterruptDisabler disabler;
    auto* dataptr = (const byte*)data;
    size_t remaining = size;
    for (size_t i = 0; i < zone.m_pages.size(); ++i) {
        byte* dest = MM.quickMapOnePage(zone.m_pages[i]);
        kprintf("memcpy(%p, %p, %u)\n", dest, dataptr, min(PAGE_SIZE, remaining));
        memcpy(dest, dataptr, min(PAGE_SIZE, remaining));
        dataptr += PAGE_SIZE;
        remaining -= PAGE_SIZE;
    }

    return true;
}
