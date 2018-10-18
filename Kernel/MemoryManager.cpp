#include "MemoryManager.h"
#include <AK/Assertions.h>
#include <AK/kstdio.h>
#include <AK/kmalloc.h>
#include "i386.h"
#include "StdLib.h"
#include "Task.h"

static MemoryManager* s_the;

MemoryManager& MemoryManager::the()
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

    kprintf("MM: Page directory @ %p\n", m_pageDirectory);
    kprintf("MM: Page table zero @ %p\n", m_pageTableZero);
    kprintf("MM: Page table one @ %p\n", m_pageTableOne);

    identityMap(LinearAddress(0), 4 * MB);
 
    // Put pages between 4MB and 16MB in the page freelist.
    for (size_t i = (4 * MB) + 1024; i < (16 * MB); i += PAGE_SIZE) {
        m_freePages.append(PhysicalAddress(i));
    }

    asm volatile("movl %%eax, %%cr3"::"a"(m_pageDirectory));
    asm volatile(
        "movl %cr0, %eax\n"
        "orl $0x80000001, %eax\n"
        "movl %eax, %cr0\n"
    );
}

auto MemoryManager::ensurePTE(LinearAddress linearAddress) -> PageTableEntry
{
    dword pageDirectoryIndex = (linearAddress.get() >> 22) & 0x3ff;
    dword pageTableIndex = (linearAddress.get() >> 12) & 0x3ff;

    PageDirectoryEntry pde = PageDirectoryEntry(&m_pageDirectory[pageDirectoryIndex]);
    if (!pde.isPresent()) {
        kprintf("PDE %u !present, allocating\n", pageDirectoryIndex);
        if (pageDirectoryIndex == 0) {
            pde.setPageTableBase((dword)m_pageTableZero);
            pde.setUserAllowed(true);
            pde.setPresent(true);
            pde.setWritable(true);
        } else if (pageDirectoryIndex == 1) {
            pde.setPageTableBase((dword)m_pageTableOne);
            pde.setUserAllowed(false);
            pde.setPresent(true);
            pde.setWritable(false);
        } else {
            // FIXME: We need an allocator!
            ASSERT_NOT_REACHED();
        }
    }
    return PageTableEntry(&pde.pageTableBase()[pageTableIndex]);
}

void MemoryManager::identityMap(LinearAddress linearAddress, size_t length)
{
    // FIXME: ASSERT(linearAddress is 4KB aligned);
    for (dword offset = 0; offset < length; offset += 4096) {
        auto pteAddress = linearAddress.offset(offset);
        auto pte = ensurePTE(pteAddress);
        pte.setPhysicalPageBase(pteAddress.get());
        pte.setUserAllowed(true);
        pte.setPresent(true);
        pte.setWritable(true);
    }
}

void MemoryManager::initialize()
{
    s_the = new MemoryManager;
}

PageFaultResponse MemoryManager::handlePageFault(const PageFault& fault)
{
    kprintf("MM: handlePageFault(%w) at laddr=%p\n", fault.code(), fault.address().get());
    if (fault.isNotPresent()) { 
        kprintf("  >> NP fault!\n");
    } else if (fault.isProtectionViolation()) {
        kprintf("  >> PV fault!\n");
    }
    return PageFaultResponse::ShouldCrash;
}

RetainPtr<Zone> MemoryManager::createZone(size_t size)
{
    auto pages = allocatePhysicalPages(ceilDiv(size, PAGE_SIZE));
    if (pages.isEmpty()) {
        kprintf("MM: createZone: no physical pages for size %u", size);
        return nullptr;
    }
    return adopt(*new Zone(move(pages)));
}

Vector<PhysicalAddress> MemoryManager::allocatePhysicalPages(size_t count)
{
    kprintf("MM: alloc %u pages from %u available\n", count, m_freePages.size());
    if (count > m_freePages.size())
        return { };

    Vector<PhysicalAddress> pages;
    pages.ensureCapacity(count);
    for (size_t i = 0; i < count; ++i)
        pages.append(m_freePages.takeLast());
    kprintf("MM: returning the pages (%u of them)\n", pages.size());
    return pages;
}

byte* MemoryManager::quickMapOnePage(PhysicalAddress physicalAddress)
{
    auto pte = ensurePTE(LinearAddress(4 * MB));
    kprintf("quickmap %x @ %x {pte @ %p}\n", physicalAddress.get(), 4*MB, pte.ptr());
    pte.setPhysicalPageBase(physicalAddress.pageBase());
    pte.setPresent(true);
    pte.setWritable(true);
    return (byte*)(4 * MB);
}

bool MemoryManager::unmapZonesForTask(Task& task)
{
    return true;
}

bool MemoryManager::mapZonesForTask(Task& task)
{
    for (auto& mappedZone : task.m_mappedZones) {
        auto& zone = *mappedZone.zone;
        for (size_t i = 0; i < zone.m_pages.size(); ++i) {
            auto pte = ensurePTE(mappedZone.linearAddress.offset(i * PAGE_SIZE));
            pte.setPhysicalPageBase(zone.m_pages[i].get());
            pte.setPresent(true);
        }
    }
    return true;
}

bool copyToZone(Zone& zone, const void* data, size_t size)
{
    if (zone.size() < size) {
        kprintf("copyToZone: can't fit %u bytes into zone with size %u\n", size, zone.size());
        return false;
    }

    auto* dataptr = (const byte*)data;
    size_t remaining = size;
    for (size_t i = 0; i < zone.m_pages.size(); ++i) {
        byte* dest = MemoryManager::the().quickMapOnePage(zone.m_pages[i]);
        kprintf("memcpy(%p, %p, %u)\n", dest, dataptr, min(PAGE_SIZE, remaining));
        memcpy(dest, dataptr, min(PAGE_SIZE, remaining));
        dataptr += PAGE_SIZE;
        remaining -= PAGE_SIZE;
    }

    return true;
}

