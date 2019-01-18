#pragma once

#include "types.h"
#include "i386.h"
#include <AK/Bitmap.h>
#include <AK/ByteBuffer.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/Vector.h>
#include <AK/HashTable.h>
#include <AK/AKString.h>
#include <VirtualFileSystem/VirtualFileSystem.h>

#define PAGE_ROUND_UP(x) ((((dword)(x)) + PAGE_SIZE-1) & (~(PAGE_SIZE-1)))

class Process;
extern Process* current;

enum class PageFaultResponse {
    ShouldCrash,
    Continue,
};

class PhysicalPage {
    AK_MAKE_ETERNAL
    friend class MemoryManager;
    friend class PageDirectory;
    friend class VMObject;
public:
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

    unsigned short retain_count() const { return m_retain_count; }

private:
    PhysicalPage(PhysicalAddress paddr, bool supervisor);
    ~PhysicalPage() = delete;

    void return_to_freelist();

    unsigned short m_retain_count { 1 };
    bool m_supervisor { false };
    PhysicalAddress m_paddr;
};

class PageDirectory {
    friend class MemoryManager;
public:
    PageDirectory();
    explicit PageDirectory(PhysicalAddress);
    ~PageDirectory();

    dword cr3() const { return m_directory_page->paddr().get(); }
    dword* entries() { return reinterpret_cast<dword*>(cr3()); }

    void flush(LinearAddress);

private:
    RetainPtr<PhysicalPage> m_directory_page;
    HashMap<unsigned, RetainPtr<PhysicalPage>> m_physical_pages;
};

class VMObject : public Retainable<VMObject> {
public:
    static RetainPtr<VMObject> create_file_backed(RetainPtr<Inode>&&, size_t);
    static RetainPtr<VMObject> create_anonymous(size_t);
    static RetainPtr<VMObject> create_framebuffer_wrapper(PhysicalAddress, size_t);
    RetainPtr<VMObject> clone();

    ~VMObject();
    bool is_anonymous() const { return m_anonymous; }

    Inode* inode() { return m_inode.ptr(); }
    const Inode* inode() const { return m_inode.ptr(); }
    size_t inode_offset() const { return m_inode_offset; }

    String name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

    size_t page_count() const { return m_size / PAGE_SIZE; }
    const Vector<RetainPtr<PhysicalPage>>& physical_pages() const { return m_physical_pages; }
    Vector<RetainPtr<PhysicalPage>>& physical_pages() { return m_physical_pages; }

private:
    VMObject(RetainPtr<Inode>&&, size_t);
    explicit VMObject(VMObject&);
    explicit VMObject(size_t);
    VMObject(PhysicalAddress, size_t);
    String m_name;
    bool m_anonymous { false };
    Unix::off_t m_inode_offset { 0 };
    size_t m_size { 0 };
    RetainPtr<Inode> m_inode;
    Vector<RetainPtr<PhysicalPage>> m_physical_pages;
};

class Region : public Retainable<Region> {
public:
    Region(LinearAddress, size_t, String&&, bool r, bool w, bool cow = false);
    Region(LinearAddress, size_t, RetainPtr<VMObject>&&, size_t offset_in_vmo, String&&, bool r, bool w, bool cow = false);
    Region(LinearAddress, size_t, RetainPtr<Inode>&&, String&&, bool r, bool w);
    ~Region();

    const VMObject& vmo() const { return *m_vmo; }
    VMObject& vmo() { return *m_vmo; }

    RetainPtr<Region> clone();
    bool contains(LinearAddress laddr) const
    {
        return laddr >= linearAddress && laddr < linearAddress.offset(size);
    }

    unsigned page_index_from_address(LinearAddress laddr) const
    {
        return (laddr - linearAddress).get() / PAGE_SIZE;
    }

    size_t first_page_index() const
    {
        return m_offset_in_vmo / PAGE_SIZE;
    }

    size_t last_page_index() const
    {
        return (first_page_index() + page_count()) - 1;
    }

    size_t page_count() const
    {
        return size / PAGE_SIZE;
    }

    bool page_in(PageDirectory&);
    int commit(Process&);
    int decommit(Process&);

    size_t committed() const;

    LinearAddress linearAddress;
    size_t size { 0 };
    size_t m_offset_in_vmo { 0 };
    RetainPtr<VMObject> m_vmo;
    String name;
    bool is_readable { true };
    bool is_writable { true };
    Bitmap cow_map;
};

#define MM MemoryManager::the()

class MemoryManager {
    AK_MAKE_ETERNAL
    friend class PageDirectory;
    friend class PhysicalPage;
    friend class Region;
    friend class VMObject;
    friend ByteBuffer procfs$mm();
public:
    static MemoryManager& the() PURE;

    static void initialize();

    PageFaultResponse handle_page_fault(const PageFault&);

    bool map_region(Process&, Region&);
    bool unmap_region(Process&, Region&);

    void populate_page_directory(PageDirectory&);

    void enter_process_paging_scope(Process&);

    bool validate_user_read(const Process&, LinearAddress) const;
    bool validate_user_write(const Process&, LinearAddress) const;

    RetainPtr<PhysicalPage> allocate_physical_page();
    RetainPtr<PhysicalPage> allocate_supervisor_physical_page();

    void remap_region(Process&, Region&);

private:
    MemoryManager();
    ~MemoryManager();

    void register_vmo(VMObject&);
    void unregister_vmo(VMObject&);
    void register_region(Region&);
    void unregister_region(Region&);

    void map_region_at_address(PageDirectory&, Region&, LinearAddress, bool user_accessible);
    void unmap_range(PageDirectory&, LinearAddress, size_t);
    void remap_region_page(PageDirectory&, Region&, unsigned page_index_in_region, bool user_allowed);

    void initialize_paging();
    void flush_entire_tlb();
    void flush_tlb(LinearAddress);

    RetainPtr<PhysicalPage> allocate_page_table(PageDirectory&, unsigned index);

    void map_protected(LinearAddress, size_t length);

    void create_identity_mapping(PageDirectory&, LinearAddress, size_t length);
    void remove_identity_mapping(PageDirectory&, LinearAddress, size_t);

    static Region* region_from_laddr(Process&, LinearAddress);

    bool copy_on_write(Process&, Region&, unsigned page_index_in_region);
    bool page_in_from_inode(PageDirectory&, Region&, unsigned page_index_in_region);
    bool zero_page(PageDirectory&, Region& region, unsigned page_index_in_region);

    byte* quickmap_page(PhysicalPage&);
    void unquickmap_page();

    PageDirectory& kernel_page_directory() { return *m_kernel_page_directory; }

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

        bool is_present() const { return raw() & Present; }
        void set_present(bool b) { set_bit(Present, b); }

        bool is_user_allowed() const { return raw() & UserSupervisor; }
        void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

        bool is_writable() const { return raw() & ReadWrite; }
        void set_writable(bool b) { set_bit(ReadWrite, b); }

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
        explicit PageTableEntry(dword* pte) : m_pte(pte) { }

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
        };

        bool is_present() const { return raw() & Present; }
        void set_present(bool b) { set_bit(Present, b); }

        bool is_user_allowed() const { return raw() & UserSupervisor; }
        void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

        bool is_writable() const { return raw() & ReadWrite; }
        void set_writable(bool b) { set_bit(ReadWrite, b); }

        void set_bit(byte bit, bool value)
        {
            if (value)
                *m_pte |= bit;
            else
                *m_pte &= ~bit;
        }

        dword* m_pte;
    };

    PageTableEntry ensure_pte(PageDirectory&, LinearAddress);

    OwnPtr<PageDirectory> m_kernel_page_directory;
    dword* m_page_table_zero;

    LinearAddress m_quickmap_addr;

    Vector<RetainPtr<PhysicalPage>> m_free_physical_pages;
    Vector<RetainPtr<PhysicalPage>> m_free_supervisor_physical_pages;

    HashTable<VMObject*> m_vmos;
    HashTable<Region*> m_regions;
};

struct ProcessPagingScope {
    ProcessPagingScope(Process& process) { MM.enter_process_paging_scope(process); }
    ~ProcessPagingScope() { MM.enter_process_paging_scope(*current); }
};
