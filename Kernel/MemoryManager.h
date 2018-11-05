#pragma once

#include "types.h"
#include "i386.h"
#include <AK/ByteBuffer.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/Vector.h>
#include <AK/HashTable.h>
#include <AK/String.h>

class Process;
extern Process* current;

enum class PageFaultResponse {
    ShouldCrash,
    Continue,
};

class PhysicalPage {
    friend class MemoryManager;
public:
    ~PhysicalPage() { }
    PhysicalAddress paddr() const { return m_paddr; }

    void retain()
    {
        ASSERT(m_retain_count);
        ++m_retain_count;
    }

    void release()
    {
        ASSERT(m_retain_count);
        if (!--m_retain_count)
            return_to_freelist();
    }

private:
    PhysicalPage(PhysicalAddress paddr)
        : m_paddr(paddr)
    {
    }

    void return_to_freelist();

    unsigned m_retain_count { 1 };
    PhysicalAddress m_paddr;
};

struct PageDirectory {
    dword entries[1024];
    RetainPtr<PhysicalPage> physical_pages[1024];
};

struct Region : public Retainable<Region> {
    Region(LinearAddress, size_t, Vector<RetainPtr<PhysicalPage>>, String&&, bool r, bool w);
    ~Region();

    RetainPtr<Region> clone();
    LinearAddress linearAddress;
    size_t size { 0 };
    Vector<RetainPtr<PhysicalPage>> physical_pages;
    String name;
    bool is_readable { true };
    bool is_writable { true };
};

#define MM MemoryManager::the()

class MemoryManager {
    AK_MAKE_ETERNAL
    friend class PhysicalPage;
    friend ByteBuffer procfs$mm();
public:
    static MemoryManager& the() PURE;

    PhysicalAddress pageDirectoryBase() const { return PhysicalAddress(reinterpret_cast<dword>(m_kernel_page_directory)); }

    static void initialize();

    PageFaultResponse handlePageFault(const PageFault&);

    bool mapRegion(Process&, Region&);
    bool unmapRegion(Process&, Region&);

    void populate_page_directory(PageDirectory&);
    void release_page_directory(PageDirectory&);

    byte* create_kernel_alias_for_region(Region&);
    void remove_kernel_alias_for_region(Region&, byte*);

    void enter_kernel_paging_scope();
    void enter_process_paging_scope(Process&);

    bool validate_user_read(const Process&, LinearAddress) const;
    bool validate_user_write(const Process&, LinearAddress) const;

    Vector<RetainPtr<PhysicalPage>> allocate_physical_pages(size_t count);

private:
    MemoryManager();
    ~MemoryManager();

    LinearAddress allocate_linear_address_range(size_t);
    void map_region_at_address(PageDirectory*, Region&, LinearAddress, bool user_accessible);
    void unmap_range(PageDirectory*, LinearAddress, size_t);

    void initializePaging();
    void flushEntireTLB();
    void flushTLB(LinearAddress);

    RetainPtr<PhysicalPage> allocate_page_table(PageDirectory&, unsigned index);
    void deallocate_page_table(PageDirectory&, unsigned index);

    void protectMap(LinearAddress, size_t length);

    void create_identity_mapping(LinearAddress, size_t length);
    void remove_identity_mapping(LinearAddress, size_t);

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

    PageTableEntry ensurePTE(PageDirectory*, LinearAddress);

    PageDirectory* m_kernel_page_directory;
    dword* m_pageTableZero;
    dword* m_pageTableOne;

    LinearAddress m_next_laddr;

    Vector<RetainPtr<PhysicalPage>> m_free_physical_pages;
};

struct KernelPagingScope {
    KernelPagingScope() { MM.enter_kernel_paging_scope(); }
    ~KernelPagingScope() { MM.enter_process_paging_scope(*current); }
};

struct ProcessPagingScope {
    ProcessPagingScope(Process& process) { MM.enter_process_paging_scope(process); }
    ~ProcessPagingScope() { MM.enter_process_paging_scope(*current); }
};
