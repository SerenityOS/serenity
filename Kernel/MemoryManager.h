#pragma once

#include "types.h"
#include "i386.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/Vector.h>
#include <AK/HashTable.h>
#include "Process.h"

class Process;

enum class PageFaultResponse {
    ShouldCrash,
    Continue,
};

struct Zone : public Retainable<Zone> {
    friend ByteBuffer procfs$mm();
public:
    ~Zone();
    size_t size() const { return m_pages.size() * PAGE_SIZE; }

    const Vector<PhysicalAddress>& pages() const { return m_pages; }

private:
    friend class MemoryManager;
    explicit Zone(Vector<PhysicalAddress>&&);

    Vector<PhysicalAddress> m_pages;
};

#define MM MemoryManager::the()

class MemoryManager {
    AK_MAKE_ETERNAL
    friend ByteBuffer procfs$mm();
public:
    static MemoryManager& the() PURE;

    PhysicalAddress pageDirectoryBase() const { return PhysicalAddress(reinterpret_cast<dword>(m_kernel_page_directory)); }

    static void initialize();

    PageFaultResponse handlePageFault(const PageFault&);

    RetainPtr<Zone> createZone(size_t);

    bool mapSubregion(Process&, Process::Subregion&);
    bool unmapSubregion(Process&, Process::Subregion&);

    bool mapRegion(Process&, Process::Region&);
    bool unmapRegion(Process&, Process::Region&);

    void registerZone(Zone&);
    void unregisterZone(Zone&);

    void populate_page_directory(Process&);

    byte* create_kernel_alias_for_region(Process::Region&);
    void remove_kernel_alias_for_region(Process::Region&, byte*);

    void enter_kernel_paging_scope();
    void enter_process_paging_scope(Process&);

    bool validate_user_read(const Process&, LinearAddress) const;
    bool validate_user_write(const Process&, LinearAddress) const;

private:
    MemoryManager();
    ~MemoryManager();

    LinearAddress allocate_linear_address_range(size_t);
    void map_region_at_address(dword* page_directory, Process::Region&, LinearAddress, bool user_accessible);
    void unmap_range(dword* page_directory, LinearAddress, size_t);

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

    PageTableEntry ensurePTE(dword* pageDirectory, LinearAddress);

    dword* m_kernel_page_directory;
    dword* m_pageTableZero;
    dword* m_pageTableOne;

    LinearAddress m_next_laddr;

    HashTable<Zone*> m_zones;

    Vector<PhysicalAddress> m_freePages;
};

struct KernelPagingScope {
    KernelPagingScope() { MM.enter_kernel_paging_scope(); }
    ~KernelPagingScope() { MM.enter_process_paging_scope(*current); }
};

struct OtherProcessPagingScope {
    OtherProcessPagingScope(Process& process) { MM.enter_process_paging_scope(process); }
    ~OtherProcessPagingScope() { MM.enter_process_paging_scope(*current); }
};
