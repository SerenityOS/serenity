#pragma once

#include "types.h"
#include "i386.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/Vector.h>
#include <AK/HashMap.h>
#include "Task.h"

class Task;

enum class PageFaultResponse {
    ShouldCrash,
    Continue,
};

struct Zone : public Retainable<Zone> {
public:
    ~Zone() { }
    size_t size() const { return m_pages.size() * PAGE_SIZE; }

    const Vector<PhysicalAddress>& pages() const { return m_pages; }

private:
    friend class MemoryManager;
    friend bool copyToZone(Zone&, const void* data, size_t);
    explicit Zone(Vector<PhysicalAddress>&& pages)
        : m_pages(move(pages))
    {
    }

    Vector<PhysicalAddress> m_pages;
};

bool copyToZone(Zone&, const void* data, size_t);

#define MM MemoryManager::the()

class MemoryManager {
public:
    static MemoryManager& the() PURE;

    PhysicalAddress pageDirectoryBase() const { return PhysicalAddress(reinterpret_cast<dword>(m_pageDirectory)); }

    static void initialize();

    PageFaultResponse handlePageFault(const PageFault&);

    RetainPtr<Zone> createZone(size_t);

    // HACK: don't use this jeez :(
    byte* quickMapOnePage(PhysicalAddress);

    bool mapSubregion(Task&, Task::Subregion&);
    bool unmapSubregion(Task&, Task::Subregion&);
    bool mapSubregionsForTask(Task&);
    bool unmapSubregionsForTask(Task&);

    bool mapRegion(Task&, Task::Region&);
    bool unmapRegion(Task&, Task::Region&);
    bool mapRegionsForTask(Task&);
    bool unmapRegionsForTask(Task&);

private:
    MemoryManager();
    ~MemoryManager();

    void initializePaging();
    void flushEntireTLB();
    void flushTLB(LinearAddress);

    void* allocatePageTable();

    void protectMap(LinearAddress, size_t length);
    void identityMap(LinearAddress, size_t length);

    Vector<PhysicalAddress> allocatePhysicalPages(size_t count);

    struct PageDirectoryEntry {
        explicit PageDirectoryEntry(dword* pde) : m_pde(pde) { }

        dword* pageTableBase() { return reinterpret_cast<dword*>(raw() & 0xfffff000u); }
        void setPageTableBase(dword value)
        {
            *m_pde &= 0xfff;
            *m_pde |= value & 0xfffff000;
        }

        dword raw() const { return *m_pde; }
        dword* ptr() { return m_pde; }

        enum Flags {
            Present = 1 << 0,
            ReadWrite = 1 << 1,
            UserSupervisor = 1 << 2,
        };

        bool isPresent() const { return raw() & Present; }
        void setPresent(bool b) { setBit(Present, b); }

        bool isUserAllowed() const { return raw() & UserSupervisor; }
        void setUserAllowed(bool b) { setBit(UserSupervisor, b); }

        bool isWritable() const { return raw() & ReadWrite; }
        void setWritable(bool b) { setBit(ReadWrite, b); }

        void setBit(byte bit, bool value)
        {
            if (value)
                *m_pde |= bit;
            else
                *m_pde &= ~bit;
        }

        dword* m_pde;
    };

    struct PageTableEntry {
        explicit PageTableEntry(dword* pte) : m_pte(pte) { }

        dword* physicalPageBase() { return reinterpret_cast<dword*>(raw() & 0xfffff000u); }
        void setPhysicalPageBase(dword value)
        {
            *m_pte &= 0xfffu;
            *m_pte |= value & 0xfffff000u;
        }

        dword raw() const { return *m_pte; }
        dword* ptr() { return m_pte; }

        enum Flags {
            Present = 1 << 0,
            ReadWrite = 1 << 1,
            UserSupervisor = 1 << 2,
        };

        bool isPresent() const { return raw() & Present; }
        void setPresent(bool b) { setBit(Present, b); }

        bool isUserAllowed() const { return raw() & UserSupervisor; }
        void setUserAllowed(bool b) { setBit(UserSupervisor, b); }

        bool isWritable() const { return raw() & ReadWrite; }
        void setWritable(bool b) { setBit(ReadWrite, b); }

        void setBit(byte bit, bool value)
        {
            if (value)
                *m_pte |= bit;
            else
                *m_pte &= ~bit;
        }

        dword* m_pte;
    };

    PageTableEntry ensurePTE(LinearAddress);

    dword* m_pageDirectory;
    dword* m_pageTableZero;
    dword* m_pageTableOne;

    HashMap<int, RetainPtr<Zone>> m_zones;

    Vector<PhysicalAddress> m_freePages;
};
