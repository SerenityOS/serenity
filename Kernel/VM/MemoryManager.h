#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>
#include <AK/Bitmap.h>
#include <AK/ByteBuffer.h>
#include <AK/HashTable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/PhysicalRegion.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/VMObject.h>

#define PAGE_ROUND_UP(x) ((((u32)(x)) + PAGE_SIZE - 1) & (~(PAGE_SIZE - 1)))

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
    static MemoryManager& the();

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

    RefPtr<PhysicalPage> allocate_user_physical_page(ShouldZeroFill);
    RefPtr<PhysicalPage> allocate_supervisor_physical_page();
    void deallocate_user_physical_page(PhysicalPage&&);
    void deallocate_supervisor_physical_page(PhysicalPage&&);

    void remap_region(PageDirectory&, Region&);

    void map_for_kernel(VirtualAddress, PhysicalAddress);

    RefPtr<Region> allocate_kernel_region(size_t, const StringView& name, bool user_accessible = false);
    RefPtr<Region> allocate_user_accessible_kernel_region(size_t, const StringView& name);
    void map_region_at_address(PageDirectory&, Region&, VirtualAddress);

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

    void remap_region_page(Region&, unsigned page_index_in_region);

    void initialize_paging();
    void flush_entire_tlb();
    void flush_tlb(VirtualAddress);

    RefPtr<PhysicalPage> allocate_page_table(PageDirectory&, unsigned index);

    void map_protected(VirtualAddress, size_t length);

    void create_identity_mapping(PageDirectory&, VirtualAddress, size_t length);
    void remove_identity_mapping(PageDirectory&, VirtualAddress, size_t);

    static Region* region_from_vaddr(Process&, VirtualAddress);
    static const Region* region_from_vaddr(const Process&, VirtualAddress);

    bool copy_on_write(Region&, unsigned page_index_in_region);
    bool page_in_from_inode(Region&, unsigned page_index_in_region);
    bool zero_page(Region& region, unsigned page_index_in_region);

    u8* quickmap_page(PhysicalPage&);
    void unquickmap_page();

    PageDirectory& kernel_page_directory() { return *m_kernel_page_directory; }

    PageTableEntry& ensure_pte(PageDirectory&, VirtualAddress);

    RefPtr<PageDirectory> m_kernel_page_directory;
    PageTableEntry* m_page_table_zero { nullptr };
    PageTableEntry* m_page_table_one { nullptr };

    VirtualAddress m_quickmap_addr;

    unsigned m_user_physical_pages { 0 };
    unsigned m_user_physical_pages_used { 0 };
    unsigned m_super_physical_pages { 0 };
    unsigned m_super_physical_pages_used { 0 };

    NonnullRefPtrVector<PhysicalRegion> m_user_physical_regions;
    NonnullRefPtrVector<PhysicalRegion> m_super_physical_regions;

    HashTable<VMObject*> m_vmos;
    HashTable<Region*> m_user_regions;
    HashTable<Region*> m_kernel_regions;

    bool m_quickmap_in_use { false };
};

struct ProcessPagingScope {
    ProcessPagingScope(Process&);
    ~ProcessPagingScope();
};
