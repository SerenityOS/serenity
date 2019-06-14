#pragma once

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
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/PhysicalRegion.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/VMObject.h>
#include <Kernel/VirtualAddress.h>

#define PAGE_ROUND_UP(x) ((((dword)(x)) + PAGE_SIZE - 1) & (~(PAGE_SIZE - 1)))

class SynthFSInode;

enum class PageFaultResponse {
    ShouldCrash,
    Continue,
};

#define MM MemoryManager::the()

class MemoryManager {
    AK_MAKE_ETERNAL
    friend class PageDirectory;
    friend class PhysicalPage;
    friend class PhysicalRegion;
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

    bool validate_user_read(const Process&, VirtualAddress) const;
    bool validate_user_write(const Process&, VirtualAddress) const;

    enum class ShouldZeroFill {
        No,
        Yes
    };

    RetainPtr<PhysicalPage> allocate_user_physical_page(ShouldZeroFill);
    RetainPtr<PhysicalPage> allocate_supervisor_physical_page();
    void deallocate_user_physical_page(PhysicalPage&&);
    void deallocate_supervisor_physical_page(PhysicalPage&&);

    void remap_region(PageDirectory&, Region&);

    void map_for_kernel(VirtualAddress, PhysicalAddress);

    RetainPtr<Region> allocate_kernel_region(size_t, String&& name);
    void map_region_at_address(PageDirectory&, Region&, VirtualAddress, bool user_accessible);

    unsigned user_physical_pages() const { return m_user_physical_pages; }
    unsigned user_physical_pages_used() const { return m_user_physical_pages_used; }
    unsigned super_physical_pages() const { return m_super_physical_pages; }
    unsigned super_physical_pages_used() const { return m_super_physical_pages_used; }

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
    void flush_tlb(VirtualAddress);

    RetainPtr<PhysicalPage> allocate_page_table(PageDirectory&, unsigned index);

    void map_protected(VirtualAddress, size_t length);

    void create_identity_mapping(PageDirectory&, VirtualAddress, size_t length);
    void remove_identity_mapping(PageDirectory&, VirtualAddress, size_t);

    static Region* region_from_vaddr(Process&, VirtualAddress);
    static const Region* region_from_vaddr(const Process&, VirtualAddress);

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

        enum Flags {
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

        enum Flags {
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

    PageTableEntry ensure_pte(PageDirectory&, VirtualAddress);

    RetainPtr<PageDirectory> m_kernel_page_directory;
    dword* m_page_table_zero { nullptr };
    dword* m_page_table_one { nullptr };

    VirtualAddress m_quickmap_addr;

    unsigned m_user_physical_pages { 0 };
    unsigned m_user_physical_pages_used { 0 };
    unsigned m_super_physical_pages { 0 };
    unsigned m_super_physical_pages_used { 0 };

    Vector<Retained<PhysicalRegion>> m_user_physical_regions {};
    Vector<Retained<PhysicalRegion>> m_super_physical_regions {};

    HashTable<VMObject*> m_vmos;
    HashTable<Region*> m_user_regions;
    HashTable<Region*> m_kernel_regions;

    bool m_quickmap_in_use { false };
};

struct ProcessPagingScope {
    ProcessPagingScope(Process&);
    ~ProcessPagingScope();
};
