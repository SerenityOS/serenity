/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Concepts.h>
#include <AK/HashTable.h>
#include <AK/IntrusiveRedBlackTree.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AllocationStrategy.h>
#include <Kernel/Memory/MemorySections.h>
#include <Kernel/Memory/PhysicalRAMPage.h>
#include <Kernel/Memory/PhysicalRegion.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/RegionTree.h>
#include <Kernel/Memory/VMObject.h>

struct KmallocGlobalData;

namespace Kernel::Memory {

class PageDirectoryEntry;
class PageTableEntry;

ErrorOr<FlatPtr> page_round_up(FlatPtr x);

constexpr FlatPtr page_round_down(FlatPtr x)
{
    return ((FlatPtr)(x)) & ~(PAGE_SIZE - 1);
}

inline FlatPtr virtual_to_low_physical(FlatPtr virtual_)
{
    return virtual_ - g_boot_info.physical_to_virtual_offset;
}

enum class UsedMemoryRangeType {
    LowMemory = 0,
    Kernel,
    BootModule,
    PhysicalPages,
    SMBIOS,
    __Count,
};

static constexpr StringView UserMemoryRangeTypeNames[] {
    "Low memory"sv,
    "Kernel"sv,
    "Boot module"sv,
    "Physical Pages"sv,
    "SMBIOS"sv,
};
static_assert(array_size(UserMemoryRangeTypeNames) == to_underlying(UsedMemoryRangeType::__Count));

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

    Spinlock<LockRank::None> m_quickmap_in_use {};
    InterruptsState m_quickmap_previous_interrupts_state;
};

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

    [[nodiscard]] NonnullRefPtr<PhysicalRAMPage> take_one();
    void uncommit_one();

    void operator=(CommittedPhysicalPageSet&&) = delete;

private:
    size_t m_page_count { 0 };
};

class MemoryManager {
    friend class PageDirectory;
    friend class AnonymousVMObject;
    friend class Region;
    friend class RegionTree;
    friend class VMObject;
    friend struct ::KmallocGlobalData;

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
    void unmap_prekernel();
    void unmap_text_after_init();
    void protect_ksyms_after_init();

    static void enter_process_address_space(Process&);
    static void enter_address_space(AddressSpace&);

    bool validate_user_stack(AddressSpace&, VirtualAddress) const;

    enum class ShouldZeroFill {
        No,
        Yes
    };

    ErrorOr<CommittedPhysicalPageSet> commit_physical_pages(size_t page_count);
    void uncommit_physical_pages(Badge<CommittedPhysicalPageSet>, size_t page_count);

    NonnullRefPtr<PhysicalRAMPage> allocate_committed_physical_page(Badge<CommittedPhysicalPageSet>, ShouldZeroFill = ShouldZeroFill::Yes);
    ErrorOr<NonnullRefPtr<PhysicalRAMPage>> allocate_physical_page(ShouldZeroFill = ShouldZeroFill::Yes, bool* did_purge = nullptr);
    ErrorOr<Vector<NonnullRefPtr<PhysicalRAMPage>>> allocate_contiguous_physical_pages(size_t size, MemoryType memory_type_for_zero_fill);
    void deallocate_physical_page(PhysicalAddress);

    ErrorOr<NonnullOwnPtr<Region>> allocate_contiguous_kernel_region(size_t, StringView name, Region::Access access, MemoryType = MemoryType::Normal);
    ErrorOr<NonnullOwnPtr<Region>> allocate_dma_buffer_page(StringView name, Region::Access access, RefPtr<PhysicalRAMPage>& dma_buffer_page, MemoryType = MemoryType::NonCacheable);
    ErrorOr<NonnullOwnPtr<Region>> allocate_dma_buffer_page(StringView name, Region::Access access, MemoryType = MemoryType::NonCacheable);
    ErrorOr<NonnullOwnPtr<Region>> allocate_dma_buffer_pages(size_t size, StringView name, Region::Access access, Vector<NonnullRefPtr<PhysicalRAMPage>>& dma_buffer_pages, MemoryType = MemoryType::NonCacheable);
    ErrorOr<NonnullOwnPtr<Region>> allocate_dma_buffer_pages(size_t size, StringView name, Region::Access access, MemoryType = MemoryType::NonCacheable);
    ErrorOr<NonnullOwnPtr<Region>> allocate_kernel_region(size_t, StringView name, Region::Access access, AllocationStrategy strategy = AllocationStrategy::Reserve, MemoryType = MemoryType::Normal);
    ErrorOr<NonnullOwnPtr<Region>> allocate_mmio_kernel_region(PhysicalAddress, size_t, StringView name, Region::Access access, MemoryType = MemoryType::IO);
    ErrorOr<NonnullOwnPtr<Region>> allocate_kernel_region_with_physical_pages(Span<NonnullRefPtr<PhysicalRAMPage>>, StringView name, Region::Access access, MemoryType = MemoryType::Normal);
    ErrorOr<NonnullOwnPtr<Region>> allocate_kernel_region_with_vmobject(VMObject&, size_t, StringView name, Region::Access access, MemoryType = MemoryType::Normal);
    ErrorOr<NonnullOwnPtr<Region>> allocate_unbacked_region_anywhere(size_t size, size_t alignment);
    ErrorOr<NonnullOwnPtr<Region>> create_identity_mapped_region(PhysicalAddress, size_t);

    struct SystemMemoryInfo {
        PhysicalSize physical_pages { 0 };
        PhysicalSize physical_pages_used { 0 };
        PhysicalSize physical_pages_committed { 0 };
        PhysicalSize physical_pages_uncommitted { 0 };
    };

    SystemMemoryInfo get_system_memory_info();

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
    static void validate_syscall_preconditions(Process&, RegisterState const&);

    void dump_kernel_regions();

    PhysicalRAMPage& shared_zero_page() { return *m_shared_zero_page; }
    PhysicalRAMPage& lazy_committed_page() { return *m_lazy_committed_page; }

    PageDirectory& kernel_page_directory() { return *m_kernel_page_directory; }

    template<typename Callback>
    void for_each_used_memory_range(Callback callback)
    {
        m_global_data.with([&](auto& global_data) {
            for (auto& range : global_data.used_memory_ranges)
                callback(range);
        });
    }
    bool is_allowed_to_read_physical_memory_for_userspace(PhysicalAddress, size_t read_length) const;

    PhysicalPageEntry& get_physical_page_entry(PhysicalAddress);
    PhysicalAddress get_physical_address(PhysicalRAMPage const&);

    void copy_physical_page(PhysicalRAMPage&, u8 page_buffer[PAGE_SIZE]);

    IterationDecision for_each_physical_memory_range(Function<IterationDecision(PhysicalMemoryRange const&)>);

private:
    MemoryManager();
    ~MemoryManager();

    struct GlobalData {
        GlobalData();

        SystemMemoryInfo system_memory_info;

        Vector<NonnullOwnPtr<PhysicalRegion>> physical_regions;
        OwnPtr<PhysicalRegion> physical_pages_region;

        RegionTree region_tree;

        Vector<UsedMemoryRange> used_memory_ranges;
        Vector<PhysicalMemoryRange> physical_memory_ranges;
        Vector<ContiguousReservedMemoryRange> reserved_memory_ranges;
    };

    void initialize_physical_pages();
    void register_reserved_ranges();

#ifdef HAS_ADDRESS_SANITIZER
    void initialize_kasan_shadow_memory();
#endif

    void unregister_kernel_region(Region&);

    void protect_kernel_image();
    void parse_memory_map();
    void parse_memory_map_efi(GlobalData&);
    void parse_memory_map_fdt(GlobalData&, u8 const* fdt_addr);
    void parse_memory_map_multiboot(GlobalData&);
    static void flush_tlb_local(VirtualAddress, size_t page_count = 1);
    static void flush_tlb(PageDirectory const*, VirtualAddress, size_t page_count = 1);

    RefPtr<PhysicalRAMPage> find_free_physical_page(bool);

    ALWAYS_INLINE u8* quickmap_page(PhysicalRAMPage& page)
    {
        return quickmap_page(page.paddr());
    }
    u8* quickmap_page(PhysicalAddress const&);
    void unquickmap_page();

    PageDirectoryEntry* quickmap_pd(PageDirectory&, size_t pdpt_index);
    PageTableEntry* quickmap_pt(PhysicalAddress);

    PageTableEntry* pte(PageDirectory&, VirtualAddress);
    PageTableEntry* ensure_pte(PageDirectory&, VirtualAddress);
    enum class IsLastPTERelease {
        Yes,
        No
    };
    void release_pte(PageDirectory&, VirtualAddress, IsLastPTERelease);

    // NOTE: These are outside of GlobalData as they are only assigned on startup,
    //       and then never change. Atomic ref-counting covers that case without
    //       the need for additional synchronization.
    LockRefPtr<PageDirectory> m_kernel_page_directory;
    RefPtr<PhysicalRAMPage> m_shared_zero_page;
    RefPtr<PhysicalRAMPage> m_lazy_committed_page;

    // NOTE: These are outside of GlobalData as they are initialized on startup,
    //       and then never change.
    PhysicalPageEntry* m_physical_page_entries { nullptr };
    size_t m_physical_page_entries_count { 0 };

    SpinlockProtected<GlobalData, LockRank::None> m_global_data;
};

inline bool PhysicalRAMPage::is_shared_zero_page() const
{
    return this == &MM.shared_zero_page();
}

inline bool PhysicalRAMPage::is_lazy_committed_page() const
{
    return this == &MM.lazy_committed_page();
}

inline ErrorOr<Memory::VirtualRange> expand_range_to_page_boundaries(FlatPtr address, size_t size)
{
    if ((address + size) < address)
        return EINVAL;

    auto base = VirtualAddress { address }.page_base();
    auto end = TRY(Memory::page_round_up(address + size));

    return Memory::VirtualRange { base, end - base.get() };
}

inline ErrorOr<Memory::VirtualRange> shrink_range_to_page_boundaries(FlatPtr address, size_t size)
{
    if ((address + size) < address)
        return EINVAL;

    auto base = TRY(Memory::page_round_up(address));
    auto end = Memory::page_round_down(address + size);

    if (end < base)
        return EINVAL;

    return Memory::VirtualRange { VirtualAddress(base), end - base };
}

}
