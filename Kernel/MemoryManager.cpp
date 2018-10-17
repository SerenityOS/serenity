#include "MemoryManager.h"
#include <AK/Assertions.h>
#include <AK/kstdio.h>
#include <AK/kmalloc.h>
#include "i386.h"
#include "StdLib.h"

static MemoryManager* s_the;

MemoryManager& MemoryManager::the()
{
    return *s_the;
}

MemoryManager::MemoryManager()
{
    m_pageDirectory = (dword*)0x5000;
    m_pageTableZero = (dword*)0x6000;

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
    memset(m_pageDirectory, 0, 4096);

    kprintf("MM: Page directory @ %p\n", m_pageDirectory);
    kprintf("MM: Page table zero @ %p [0]=%x\n", m_pageTableZero, m_pageTableZero[0]);
    // Build a basic PDB that identity maps the first 1MB.
    identityMap(LinearAddress(0), 4 * MB);

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
        if (pteAddress.get() == 0x6023) {
            kprintf("kek\n");
            HANG;
        }
    }
}

void MemoryManager::initialize()
{
    s_the = new MemoryManager;
}


