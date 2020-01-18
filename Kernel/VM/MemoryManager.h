#pragma once

#include <AK/Badge.h>
#include <AK/Bitmap.h>
#include <AK/ByteBuffer.h>
#include <AK/HashTable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
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

template<typename T>
inline T* low_physical_to_virtual(T* physical)
{
    return (T*)(((u8*)physical) + 0xc0000000);
}

inline u32 low_physical_to_virtual(u32 physical)
{
    return physical + 0xc0000000;
}

template<typename T>
inline T* virtual_to_low_physical(T* physical)
{
    return (T*)(((u8*)physical) - 0xc0000000);
}

inline u32 virtual_to_low_physical(u32 physical)
{
    return physical - 0xc0000000;
}

class KBuffer;
class SynthFSInode;

#define MM MemoryManager::the()

class MemoryManager {
    AK_MAKE_ETERNAL
    friend class PageDirectory;
    friend class PhysicalPage;
    friend class PhysicalRegion;
    friend class Region;
    friend class VMObject;
    friend Optional<KBuffer> procfs$mm(InodeIdentifier);
    friend Optional<KBuffer> procfs$memstat(InodeIdentifier);

public:
    static MemoryManager& the();

    static void initialize();

    PageFaultResponse handle_page_fault(const PageFault&);

    void enter_process_paging_scope(Process&);

    bool validate_user_stack(const Process&, VirtualAddress) const;
    bool validate_user_read(const Process&, VirtualAddress, size_t) const;
    bool validate_user_write(const Process&, VirtualAddress, size_t) const;

    bool validate_kernel_read(const Process&, VirtualAddress, size_t) const;

    enum class ShouldZeroFill {
        No,
        Yes
    };

    RefPtr<PhysicalPage> allocate_user_physical_page(ShouldZeroFill = ShouldZeroFill::Yes);
    RefPtr<PhysicalPage> allocate_supervisor_physical_page();
    void deallocate_user_physical_page(PhysicalPage&&);
    void deallocate_supervisor_physical_page(PhysicalPage&&);

    void map_for_kernel(VirtualAddress, PhysicalAddress, bool cache_disabled = false);

    OwnPtr<Region> allocate_kernel_region(size_t, const StringView& name, u8 access, bool user_accessible = false, bool should_commit = true, bool cacheable = true);
    OwnPtr<Region> allocate_kernel_region(PhysicalAddress, size_t, const StringView& name, u8 access, bool user_accessible = false, bool cacheable = false);
    OwnPtr<Region> allocate_kernel_region_with_vmobject(VMObject&, size_t, const StringView& name, u8 access, bool user_accessible = false, bool cacheable = false);
    OwnPtr<Region> allocate_user_accessible_kernel_region(size_t, const StringView& name, u8 access, bool cacheable = false);

    unsigned user_physical_pages() const { return m_user_physical_pages; }
    unsigned user_physical_pages_used() const { return m_user_physical_pages_used; }
    unsigned super_physical_pages() const { return m_super_physical_pages; }
    unsigned super_physical_pages_used() const { return m_super_physical_pages_used; }

    template<typename Callback>
    static void for_each_vmobject(Callback callback)
    {
        for (auto& vmobject : MM.m_vmobjects) {
            if (callback(vmobject) == IterationDecision::Break)
                break;
        }
    }

    static Region* region_from_vaddr(Process&, VirtualAddress);
    static const Region* region_from_vaddr(const Process&, VirtualAddress);

    void dump_kernel_regions();

private:
    MemoryManager();
    ~MemoryManager();

    enum class AccessSpace { Kernel, User };
    enum class AccessType { Read, Write };
    template<AccessSpace, AccessType>
    bool validate_range(const Process&, VirtualAddress, size_t) const;

    void register_vmobject(VMObject&);
    void unregister_vmobject(VMObject&);
    void register_region(Region&);
    void unregister_region(Region&);

    void detect_cpu_features();
    void setup_low_1mb();
    void protect_kernel_image();
    void parse_memory_map();
    void flush_entire_tlb();
    void flush_tlb(VirtualAddress);

    static Region* user_region_from_vaddr(Process&, VirtualAddress);
    static Region* kernel_region_from_vaddr(VirtualAddress);

    static Region* region_from_vaddr(VirtualAddress);

    RefPtr<PhysicalPage> find_free_user_physical_page();
    u8* quickmap_page(PhysicalPage&);
    void unquickmap_page();

    PageDirectoryEntry* quickmap_pd(PageDirectory&, size_t pdpt_index);
    PageTableEntry* quickmap_pt(PhysicalAddress);

    PageDirectory& kernel_page_directory() { return *m_kernel_page_directory; }

    PageTableEntry& ensure_pte(PageDirectory&, VirtualAddress);

    RefPtr<PageDirectory> m_kernel_page_directory;
    RefPtr<PhysicalPage> m_low_page_table;

    unsigned m_user_physical_pages { 0 };
    unsigned m_user_physical_pages_used { 0 };
    unsigned m_super_physical_pages { 0 };
    unsigned m_super_physical_pages_used { 0 };

    NonnullRefPtrVector<PhysicalRegion> m_user_physical_regions;
    NonnullRefPtrVector<PhysicalRegion> m_super_physical_regions;

    InlineLinkedList<Region> m_user_regions;
    InlineLinkedList<Region> m_kernel_regions;

    InlineLinkedList<VMObject> m_vmobjects;

    bool m_quickmap_in_use { false };
};

struct ProcessPagingScope {
    ProcessPagingScope(Process&);
    ~ProcessPagingScope();
};

template<typename Callback>
void VMObject::for_each_region(Callback callback)
{
    // FIXME: Figure out a better data structure so we don't have to walk every single region every time an inode changes.
    //        Perhaps VMObject could have a Vector<Region*> with all of his mappers?
    for (auto& region : MM.m_user_regions) {
        if (&region.vmobject() == this)
            callback(region);
    }
    for (auto& region : MM.m_kernel_regions) {
        if (&region.vmobject() == this)
            callback(region);
    }
}

inline bool is_user_address(VirtualAddress vaddr)
{
    return vaddr.get() >= (8 * MB) && vaddr.get() < 0xc0000000;
}
