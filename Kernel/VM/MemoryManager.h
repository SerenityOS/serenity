/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/HashTable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>
#include <Kernel/Arch/x86/PageFault.h>
#include <Kernel/Arch/x86/TrapFrame.h>
#include <Kernel/Forward.h>
#include <Kernel/SpinLock.h>
#include <Kernel/VM/AllocationStrategy.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {

constexpr bool page_round_up_would_wrap(FlatPtr x)
{
    return x > (explode_byte(0xFF) & ~0xFFF);
}

constexpr FlatPtr page_round_up(FlatPtr x)
{
    FlatPtr rounded = (((FlatPtr)(x)) + PAGE_SIZE - 1) & (~(PAGE_SIZE - 1));
    // Rounding up >0xfffff000 wraps back to 0. That's never what we want.
    VERIFY(x == 0 || rounded != 0);
    return rounded;
}

constexpr FlatPtr page_round_down(FlatPtr x)
{
    return ((FlatPtr)(x)) & ~(PAGE_SIZE - 1);
}

inline FlatPtr low_physical_to_virtual(FlatPtr physical)
{
    return physical + KERNEL_BASE;
}

inline FlatPtr virtual_to_low_physical(FlatPtr virtual_)
{
    return virtual_ - KERNEL_BASE;
}

enum class UsedMemoryRangeType {
    LowMemory = 0,
    Kernel,
    BootModule,
    PhysicalPages,
};

static constexpr StringView UserMemoryRangeTypeNames[] {
    "Low memory",
    "Kernel",
    "Boot module",
    "Physical Pages"
};

struct UsedMemoryRange {
    UsedMemoryRangeType type {};
    PhysicalAddress start;
    PhysicalAddress end;
};

struct ContiguousReservedMemoryRange {
    PhysicalAddress start;
    PhysicalSize length {};
};

enum class PhysicalMemoryRangeType {
    Usable = 0,
    Reserved,
    ACPI_Reclaimable,
    ACPI_NVS,
    BadMemory,
    Unknown,
};

struct PhysicalMemoryRange {
    PhysicalMemoryRangeType type { PhysicalMemoryRangeType::Unknown };
    PhysicalAddress start;
    PhysicalSize length {};
};

#define MM Kernel::MemoryManager::the()

struct MemoryManagerData {
    SpinLock<u8> m_quickmap_in_use;
    u32 m_quickmap_prev_flags;

    PhysicalAddress m_last_quickmap_pd;
    PhysicalAddress m_last_quickmap_pt;
};

extern RecursiveSpinLock s_mm_lock;

class MemoryManager {
    AK_MAKE_ETERNAL
    friend class PageDirectory;
    friend class PhysicalPage;
    friend class PhysicalRegion;
    friend class AnonymousVMObject;
    friend class Region;
    friend class VMObject;

public:
    static MemoryManager& the();
    static bool is_initialized();

    static void initialize(u32 cpu);

    static inline MemoryManagerData& get_data()
    {
        return Processor::current().get_mm_data();
    }

    PageFaultResponse handle_page_fault(const PageFault&);

    void set_page_writable_direct(VirtualAddress, bool);

    void protect_readonly_after_init_memory();
    void unmap_memory_after_init();

    static void enter_process_paging_scope(Process&);
    static void enter_space(Space&);

    bool validate_user_stack(const Process&, VirtualAddress) const;

    enum class ShouldZeroFill {
        No,
        Yes
    };

    bool commit_user_physical_pages(size_t);
    void uncommit_user_physical_pages(size_t);
    NonnullRefPtr<PhysicalPage> allocate_committed_user_physical_page(ShouldZeroFill = ShouldZeroFill::Yes);
    RefPtr<PhysicalPage> allocate_user_physical_page(ShouldZeroFill = ShouldZeroFill::Yes, bool* did_purge = nullptr);
    RefPtr<PhysicalPage> allocate_supervisor_physical_page();
    NonnullRefPtrVector<PhysicalPage> allocate_contiguous_supervisor_physical_pages(size_t size, size_t physical_alignment = PAGE_SIZE);
    void deallocate_user_physical_page(PhysicalAddress);
    void deallocate_supervisor_physical_page(PhysicalAddress);

    OwnPtr<Region> allocate_contiguous_kernel_region(size_t, StringView name, Region::Access access, size_t physical_alignment = PAGE_SIZE, Region::Cacheable = Region::Cacheable::Yes);
    OwnPtr<Region> allocate_kernel_region(size_t, StringView name, Region::Access access, AllocationStrategy strategy = AllocationStrategy::Reserve, Region::Cacheable = Region::Cacheable::Yes);
    OwnPtr<Region> allocate_kernel_region(PhysicalAddress, size_t, StringView name, Region::Access access, Region::Cacheable = Region::Cacheable::Yes);
    OwnPtr<Region> allocate_kernel_region_identity(PhysicalAddress, size_t, StringView name, Region::Access access, Region::Cacheable = Region::Cacheable::Yes);
    OwnPtr<Region> allocate_kernel_region_with_vmobject(VMObject&, size_t, StringView name, Region::Access access, Region::Cacheable = Region::Cacheable::Yes);
    OwnPtr<Region> allocate_kernel_region_with_vmobject(const Range&, VMObject&, StringView name, Region::Access access, Region::Cacheable = Region::Cacheable::Yes);

    struct SystemMemoryInfo {
        PhysicalSize user_physical_pages { 0 };
        PhysicalSize user_physical_pages_used { 0 };
        PhysicalSize user_physical_pages_committed { 0 };
        PhysicalSize user_physical_pages_uncommitted { 0 };
        PhysicalSize super_physical_pages { 0 };
        PhysicalSize super_physical_pages_used { 0 };
    };

    SystemMemoryInfo get_system_memory_info()
    {
        ScopedSpinLock lock(s_mm_lock);
        return m_system_memory_info;
    }

    template<IteratorFunction<VMObject&> Callback>
    static void for_each_vmobject(Callback callback)
    {
        for (auto& vmobject : MM.m_vmobjects) {
            if (callback(vmobject) == IterationDecision::Break)
                break;
        }
    }

    template<VoidFunction<VMObject&> Callback>
    static void for_each_vmobject(Callback callback)
    {
        for (auto& vmobject : MM.m_vmobjects)
            callback(vmobject);
    }

    static Region* find_region_from_vaddr(Space&, VirtualAddress);
    static Region* find_user_region_from_vaddr(Space&, VirtualAddress);

    void dump_kernel_regions();

    PhysicalPage& shared_zero_page() { return *m_shared_zero_page; }
    PhysicalPage& lazy_committed_page() { return *m_lazy_committed_page; }

    PageDirectory& kernel_page_directory() { return *m_kernel_page_directory; }

    const Vector<UsedMemoryRange>& used_memory_ranges() { return m_used_memory_ranges; }
    bool is_allowed_to_mmap_to_userspace(PhysicalAddress, const Range&) const;

    PhysicalPageEntry& get_physical_page_entry(PhysicalAddress);
    PhysicalAddress get_physical_address(PhysicalPage const&);

private:
    MemoryManager();
    ~MemoryManager();

    void initialize_physical_pages();
    void register_reserved_ranges();

    void register_vmobject(VMObject&);
    void unregister_vmobject(VMObject&);
    void register_region(Region&);
    void unregister_region(Region&);

    void protect_kernel_image();
    void parse_memory_map();
    static void flush_tlb_local(VirtualAddress, size_t page_count = 1);
    static void flush_tlb(const PageDirectory*, VirtualAddress, size_t page_count = 1);

    static Region* kernel_region_from_vaddr(VirtualAddress);

    static Region* find_region_from_vaddr(VirtualAddress);

    RefPtr<PhysicalPage> find_free_user_physical_page(bool);

    ALWAYS_INLINE u8* quickmap_page(PhysicalPage& page)
    {
        return quickmap_page(page.paddr());
    }
    u8* quickmap_page(PhysicalAddress const&);
    void unquickmap_page();

    PageDirectoryEntry* quickmap_pd(PageDirectory&, size_t pdpt_index);
    PageTableEntry* quickmap_pt(PhysicalAddress);

    PageTableEntry* pte(PageDirectory&, VirtualAddress);
    PageTableEntry* ensure_pte(PageDirectory&, VirtualAddress);
    void release_pte(PageDirectory&, VirtualAddress, bool);

    RefPtr<PageDirectory> m_kernel_page_directory;

    RefPtr<PhysicalPage> m_shared_zero_page;
    RefPtr<PhysicalPage> m_lazy_committed_page;

    SystemMemoryInfo m_system_memory_info;

    NonnullRefPtrVector<PhysicalRegion> m_user_physical_regions;
    NonnullRefPtrVector<PhysicalRegion> m_super_physical_regions;
    RefPtr<PhysicalRegion> m_physical_pages_region;
    PhysicalPageEntry* m_physical_page_entries { nullptr };
    size_t m_physical_page_entries_free { 0 };
    size_t m_physical_page_entries_count { 0 };

    Region::List m_user_regions;
    Region::List m_kernel_regions;
    Vector<UsedMemoryRange> m_used_memory_ranges;
    Vector<PhysicalMemoryRange> m_physical_memory_ranges;
    Vector<ContiguousReservedMemoryRange> m_reserved_memory_ranges;

    VMObject::List m_vmobjects;
};

template<typename Callback>
void VMObject::for_each_region(Callback callback)
{
    ScopedSpinLock lock(s_mm_lock);
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
    return vaddr.get() < KERNEL_BASE;
}

inline bool is_user_range(VirtualAddress vaddr, size_t size)
{
    if (vaddr.offset(size) < vaddr)
        return false;
    return is_user_address(vaddr) && is_user_address(vaddr.offset(size));
}

inline bool is_user_range(const Range& range)
{
    return is_user_range(range.base(), range.size());
}

inline bool PhysicalPage::is_shared_zero_page() const
{
    return this == &MM.shared_zero_page();
}

inline bool PhysicalPage::is_lazy_committed_page() const
{
    return this == &MM.lazy_committed_page();
}

}
