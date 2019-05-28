#pragma once

#include "i386.h"
#include <AK/AKString.h>
#include <AK/Badge.h>
#include <AK/Bitmap.h>
#include <AK/ByteBuffer.h>
#include <AK/HashTable.h>
#include <AK/RetainPtr.h>
#include <AK/Retainable.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/LinearAddress.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/VMObject.h>

#define PAGE_ROUND_UP(x) ((((dword)(x)) + PAGE_SIZE - 1) & (~(PAGE_SIZE - 1)))

class SynthFSInode;

enum class PageFaultResponse
{
    ShouldCrash,
    Continue,
};

#define MM MemoryManager::the()

class MemoryManager {
    AK_MAKE_ETERNAL
    friend class PageDirectory;
    friend class PhysicalPage;
    friend class Region;
    friend class VMObject;
    friend ByteBuffer procfs$mm(InodeIdentifier);
    friend ByteBuffer procfs$memstat(InodeIdentifier);

public:
    [[gnu::pure]] static MemoryManager& the();

    static void initialize();

    PageFaultResponse handle_page_fault(const PageFault&);

    bool map_region(Process&, Region&);
    bool unmap_region(Region&);

    void populate_page_directory(PageDirectory&);

    void enter_process_paging_scope(Process&);
    void enter_kernel_paging_scope();

    bool validate_user_read(const Process&, LinearAddress) const;
    bool validate_user_write(const Process&, LinearAddress) const;

    enum class ShouldZeroFill
    {
        No,
        Yes
    };

    RetainPtr<PhysicalPage> allocate_physical_page(ShouldZeroFill);
    RetainPtr<PhysicalPage> allocate_supervisor_physical_page();

    void remap_region(PageDirectory&, Region&);

    size_t ram_size() const { return m_ram_size; }

    int user_physical_pages_in_existence() const { return s_user_physical_pages_in_existence; }
    int super_physical_pages_in_existence() const { return s_super_physical_pages_in_existence; }

    void map_for_kernel(LinearAddress, PhysicalAddress);

    RetainPtr<Region> allocate_kernel_region(size_t, String&& name);
    void map_region_at_address(PageDirectory&, Region&, LinearAddress, bool user_accessible);

private:
    MemoryManager();
    ~MemoryManager();

    void register_vmo(VMObject&);
    void unregister_vmo(VMObject&);
    void register_region(Region&);
    void unregister_region(Region&);

    void remap_region_page(Region&, unsigned page_index_in_region, bool user_allowed);

    void initialize_paging();
    void flush_entire_tlb();
    void flush_tlb(LinearAddress);

    RetainPtr<PhysicalPage> allocate_page_table(PageDirectory&, unsigned index);

    void map_protected(LinearAddress, size_t length);

    void create_identity_mapping(PageDirectory&, LinearAddress, size_t length);
    void remove_identity_mapping(PageDirectory&, LinearAddress, size_t);

    static Region* region_from_laddr(Process&, LinearAddress);
    static const Region* region_from_laddr(const Process&, LinearAddress);

    bool copy_on_write(Region&, unsigned page_index_in_region);
    bool page_in_from_inode(Region&, unsigned page_index_in_region);
    bool zero_page(Region& region, unsigned page_index_in_region);

    byte* quickmap_page(PhysicalPage&);
    void unquickmap_page();

    PageDirectory& kernel_page_directory() { return *m_kernel_page_directory; }

    struct PageDirectoryEntry {
        explicit PageDirectoryEntry(dword* pde)
            : m_pde(pde)
        {
        }

        dword* page_table_base() { return reinterpret_cast<dword*>(raw() & 0xfffff000u); }
        void set_page_table_base(dword value)
        {
            *m_pde &= 0xfff;
            *m_pde |= value & 0xfffff000;
        }

        dword raw() const { return *m_pde; }
        dword* ptr() { return m_pde; }

        enum Flags
        {
            Present = 1 << 0,
            ReadWrite = 1 << 1,
            UserSupervisor = 1 << 2,
            WriteThrough = 1 << 3,
            CacheDisabled = 1 << 4,
        };

        bool is_present() const { return raw() & Present; }
        void set_present(bool b) { set_bit(Present, b); }

        bool is_user_allowed() const { return raw() & UserSupervisor; }
        void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

        bool is_writable() const { return raw() & ReadWrite; }
        void set_writable(bool b) { set_bit(ReadWrite, b); }

        bool is_write_through() const { return raw() & WriteThrough; }
        void set_write_through(bool b) { set_bit(WriteThrough, b); }

        bool is_cache_disabled() const { return raw() & CacheDisabled; }
        void set_cache_disabled(bool b) { set_bit(CacheDisabled, b); }

        void set_bit(byte bit, bool value)
        {
            if (value)
                *m_pde |= bit;
            else
                *m_pde &= ~bit;
        }

        dword* m_pde;
    };

    struct PageTableEntry {
        explicit PageTableEntry(dword* pte)
            : m_pte(pte)
        {
        }

        dword* physical_page_base() { return reinterpret_cast<dword*>(raw() & 0xfffff000u); }
        void set_physical_page_base(dword value)
        {
            *m_pte &= 0xfffu;
            *m_pte |= value & 0xfffff000u;
        }

        dword raw() const { return *m_pte; }
        dword* ptr() { return m_pte; }

        enum Flags
        {
            Present = 1 << 0,
            ReadWrite = 1 << 1,
            UserSupervisor = 1 << 2,
            WriteThrough = 1 << 3,
            CacheDisabled = 1 << 4,
        };

        bool is_present() const { return raw() & Present; }
        void set_present(bool b) { set_bit(Present, b); }

        bool is_user_allowed() const { return raw() & UserSupervisor; }
        void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

        bool is_writable() const { return raw() & ReadWrite; }
        void set_writable(bool b) { set_bit(ReadWrite, b); }

        bool is_write_through() const { return raw() & WriteThrough; }
        void set_write_through(bool b) { set_bit(WriteThrough, b); }

        bool is_cache_disabled() const { return raw() & CacheDisabled; }
        void set_cache_disabled(bool b) { set_bit(CacheDisabled, b); }

        void set_bit(byte bit, bool value)
        {
            if (value)
                *m_pte |= bit;
            else
                *m_pte &= ~bit;
        }

        dword* m_pte;
    };

    static unsigned s_user_physical_pages_in_existence;
    static unsigned s_super_physical_pages_in_existence;

    PageTableEntry ensure_pte(PageDirectory&, LinearAddress);

    RetainPtr<PageDirectory> m_kernel_page_directory;
    dword* m_page_table_zero;

    LinearAddress m_quickmap_addr;

    Vector<Retained<PhysicalPage>> m_free_physical_pages;
    Vector<Retained<PhysicalPage>> m_free_supervisor_physical_pages;

    HashTable<VMObject*> m_vmos;
    HashTable<Region*> m_user_regions;
    HashTable<Region*> m_kernel_regions;

    size_t m_ram_size { 0 };
    bool m_quickmap_in_use { false };
};

struct ProcessPagingScope {
    ProcessPagingScope(Process&);
    ~ProcessPagingScope();
};

struct KernelPagingScope {
    KernelPagingScope();
    ~KernelPagingScope();
};
