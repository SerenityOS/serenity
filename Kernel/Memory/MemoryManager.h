/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/HashTable.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtrVector.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AllocationStrategy.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/Memory/PhysicalRegion.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/VMObject.h>

namespace Kernel {
class PageDirectoryEntry;
}

namespace Kernel::Memory {

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

inline FlatPtr virtual_to_low_physical(FlatPtr virtual_)
{
    return virtual_ - physical_to_virtual_offset;
}

enum class UsedMemoryRangeType {
    LowMemory = 0,
    Prekernel,
    Kernel,
    BootModule,
    PhysicalPages,
};

static constexpr StringView UserMemoryRangeTypeNames[] {
    "Low memory",
    "Prekernel",
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

#define MM Kernel::Memory::MemoryManager::the()

struct MemoryManagerData {
    static ProcessorSpecificDataID processor_specific_data_id() { return ProcessorSpecificDataID::MemoryManager; }

    Spinlock m_quickmap_in_use;
    u32 m_quickmap_prev_flags;

    PhysicalAddress m_last_quickmap_pd;
    PhysicalAddress m_last_quickmap_pt;
};

// NOLINTNEXTLINE(readability-redundant-declaration) FIXME: Why do we declare this here *and* in Thread.h?
extern RecursiveSpinlock s_mm_lock;

// This class represents a set of committed physical pages.
// When you ask MemoryManager to commit pages for you, you get one of these in return.
// You can allocate pages from it via `take_one()`
// It will uncommit any (unallocated) remaining pages when destroyed.
class CommittedPhysicalPageSet {
    AK_MAKE_NONCOPYABLE(CommittedPhysicalPageSet);

public:
    CommittedPhysicalPageSet(Badge<MemoryManager>, size_t page_count)
        : m_page_count(page_count)
    {
    }

    CommittedPhysicalPageSet(CommittedPhysicalPageSet&& other)
        : m_page_count(exchange(other.m_page_count, 0))
    {
    }

    ~CommittedPhysicalPageSet();

    bool is_empty() const { return m_page_count == 0; }
    size_t page_count() const { return m_page_count; }

    [[nodiscard]] NonnullRefPtr<PhysicalPage> take_one();
    void uncommit_one();

    void operator=(CommittedPhysicalPageSet&&) = delete;

private:
    size_t m_page_count { 0 };
};

class MemoryManager {
    AK_MAKE_ETERNAL
    friend class PageDirectory;
    friend class AnonymousVMObject;
    friend class Region;
    friend class VMObject;

public:
    static MemoryManager& the();
    static bool is_initialized();

    static void initialize(u32 cpu);

    static inline MemoryManagerData& get_data()
    {
        return ProcessorSpecific<MemoryManagerData>::get();
    }

    PageFaultResponse handle_page_fault(PageFault const&);

    void set_page_writable_direct(VirtualAddress, bool);

    void protect_readonly_after_init_memory();
    void unmap_text_after_init();
    void unmap_ksyms_after_init();

    static void enter_process_address_space(Process&);
    static void enter_address_space(AddressSpace&);

    bool validate_user_stack_no_lock(AddressSpace&, VirtualAddress) const;
    bool validate_user_stack(AddressSpace&, VirtualAddress) const;

    enum class ShouldZeroFill {
        No,
        Yes
    };

    ErrorOr<CommittedPhysicalPageSet> commit_user_physical_pages(size_t page_count);
    void uncommit_user_physical_pages(Badge<CommittedPhysicalPageSet>, size_t page_count);

    NonnullRefPtr<PhysicalPage> allocate_committed_user_physical_page(Badge<CommittedPhysicalPageSet>, ShouldZeroFill = ShouldZeroFill::Yes);
    RefPtr<PhysicalPage> allocate_user_physical_page(ShouldZeroFill = ShouldZeroFill::Yes, bool* did_purge = nullptr);
    RefPtr<PhysicalPage> allocate_supervisor_physical_page();
    NonnullRefPtrVector<PhysicalPage> allocate_contiguous_supervisor_physical_pages(size_t size);
    void deallocate_physical_page(PhysicalAddress);

    ErrorOr<NonnullOwnPtr<Region>> allocate_contiguous_kernel_region(size_t, StringView name, Region::Access access, Region::Cacheable = Region::Cacheable::Yes);
    ErrorOr<NonnullOwnPtr<Region>> allocate_kernel_region(size_t, StringView name, Region::Access access, AllocationStrategy strategy = AllocationStrategy::Reserve, Region::Cacheable = Region::Cacheable::Yes);
    ErrorOr<NonnullOwnPtr<Region>> allocate_kernel_region(PhysicalAddress, size_t, StringView name, Region::Access access, Region::Cacheable = Region::Cacheable::Yes);
    ErrorOr<NonnullOwnPtr<Region>> allocate_kernel_region_with_vmobject(VMObject&, size_t, StringView name, Region::Access access, Region::Cacheable = Region::Cacheable::Yes);
    ErrorOr<NonnullOwnPtr<Region>> allocate_kernel_region_with_vmobject(VirtualRange const&, VMObject&, StringView name, Region::Access access, Region::Cacheable = Region::Cacheable::Yes);

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
        SpinlockLocker lock(s_mm_lock);
        return m_system_memory_info;
    }

    template<IteratorFunction<VMObject&> Callback>
    static void for_each_vmobject(Callback callback)
    {
        VMObject::all_instances().with([&](auto& list) {
            for (auto& vmobject : list) {
                if (callback(vmobject) == IterationDecision::Break)
                    break;
            }
        });
    }

    template<VoidFunction<VMObject&> Callback>
    static void for_each_vmobject(Callback callback)
    {
        VMObject::all_instances().with([&](auto& list) {
            for (auto& vmobject : list) {
                callback(vmobject);
            }
        });
    }

    static Region* find_user_region_from_vaddr(AddressSpace&, VirtualAddress);
    static Region* find_user_region_from_vaddr_no_lock(AddressSpace&, VirtualAddress);
    static void validate_syscall_preconditions(AddressSpace&, RegisterState const&);

    void dump_kernel_regions();

    PhysicalPage& shared_zero_page() { return *m_shared_zero_page; }
    PhysicalPage& lazy_committed_page() { return *m_lazy_committed_page; }

    PageDirectory& kernel_page_directory() { return *m_kernel_page_directory; }

    Vector<UsedMemoryRange> const& used_memory_ranges() { return m_used_memory_ranges; }
    bool is_allowed_to_mmap_to_userspace(PhysicalAddress, VirtualRange const&) const;

    PhysicalPageEntry& get_physical_page_entry(PhysicalAddress);
    PhysicalAddress get_physical_address(PhysicalPage const&);

    void copy_physical_page(PhysicalPage&, u8 page_buffer[PAGE_SIZE]);

private:
    MemoryManager();
    ~MemoryManager();

    void initialize_physical_pages();
    void register_reserved_ranges();

    void register_region(Region&);
    void unregister_region(Region&);

    void protect_kernel_image();
    void parse_memory_map();
    static void flush_tlb_local(VirtualAddress, size_t page_count = 1);
    static void flush_tlb(PageDirectory const*, VirtualAddress, size_t page_count = 1);

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

    NonnullOwnPtrVector<PhysicalRegion> m_user_physical_regions;
    OwnPtr<PhysicalRegion> m_super_physical_region;
    OwnPtr<PhysicalRegion> m_physical_pages_region;
    PhysicalPageEntry* m_physical_page_entries { nullptr };
    size_t m_physical_page_entries_count { 0 };

    RedBlackTree<FlatPtr, Region*> m_kernel_regions;

    Vector<UsedMemoryRange> m_used_memory_ranges;
    Vector<PhysicalMemoryRange> m_physical_memory_ranges;
    Vector<ContiguousReservedMemoryRange> m_reserved_memory_ranges;
};

inline bool is_user_address(VirtualAddress vaddr)
{
    return vaddr.get() < USER_RANGE_CEILING;
}

inline bool is_user_range(VirtualAddress vaddr, size_t size)
{
    if (vaddr.offset(size) < vaddr)
        return false;
    if (!is_user_address(vaddr))
        return false;
    if (size <= 1)
        return true;
    return is_user_address(vaddr.offset(size - 1));
}

inline bool is_user_range(VirtualRange const& range)
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

inline ErrorOr<Memory::VirtualRange> expand_range_to_page_boundaries(FlatPtr address, size_t size)
{
    if (Memory::page_round_up_would_wrap(size))
        return EINVAL;

    if ((address + size) < address)
        return EINVAL;

    if (Memory::page_round_up_would_wrap(address + size))
        return EINVAL;

    auto base = VirtualAddress { address }.page_base();
    auto end = Memory::page_round_up(address + size);

    return Memory::VirtualRange { base, end - base.get() };
}

}
