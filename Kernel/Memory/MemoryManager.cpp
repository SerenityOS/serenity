/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/Arch/x86/PageFault.h>
#include <Kernel/BootInfo.h>
#include <Kernel/CMOS.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/KSyms.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PageDirectory.h>
#include <Kernel/Memory/PhysicalRegion.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/StdLib.h>

extern u8 start_of_kernel_image[];
extern u8 end_of_kernel_image[];
extern u8 start_of_kernel_text[];
extern u8 start_of_kernel_data[];
extern u8 end_of_kernel_bss[];
extern u8 start_of_ro_after_init[];
extern u8 end_of_ro_after_init[];
extern u8 start_of_unmap_after_init[];
extern u8 end_of_unmap_after_init[];
extern u8 start_of_kernel_ksyms[];
extern u8 end_of_kernel_ksyms[];

extern multiboot_module_entry_t multiboot_copy_boot_modules_array[16];
extern size_t multiboot_copy_boot_modules_count;

// Treat the super pages as logically separate from .bss
// FIXME: Find a solution so we don't need to expand this range each time
// we are in a situation too many drivers try to allocate super pages.
__attribute__((section(".super_pages"))) static u8 super_pages[4 * MiB];

namespace Kernel::Memory {

ErrorOr<FlatPtr> page_round_up(FlatPtr x)
{
    if (x > (explode_byte(0xFF) & ~0xFFF)) {
        return Error::from_errno(EINVAL);
    }
    return (((FlatPtr)(x)) + PAGE_SIZE - 1) & (~(PAGE_SIZE - 1));
}

// NOTE: We can NOT use Singleton for this class, because
// MemoryManager::initialize is called *before* global constructors are
// run. If we do, then Singleton would get re-initialized, causing
// the memory manager to be initialized twice!
static MemoryManager* s_the;
RecursiveSpinlock s_mm_lock { LockRank::MemoryManager };

MemoryManager& MemoryManager::the()
{
    return *s_the;
}

bool MemoryManager::is_initialized()
{
    return s_the != nullptr;
}

UNMAP_AFTER_INIT MemoryManager::MemoryManager()
{
    s_the = this;

    SpinlockLocker lock(s_mm_lock);
    parse_memory_map();
    write_cr3(kernel_page_directory().cr3());
    protect_kernel_image();

    // We're temporarily "committing" to two pages that we need to allocate below
    auto committed_pages = commit_user_physical_pages(2).release_value();

    m_shared_zero_page = committed_pages.take_one();

    // We're wasting a page here, we just need a special tag (physical
    // address) so that we know when we need to lazily allocate a page
    // that we should be drawing this page from the committed pool rather
    // than potentially failing if no pages are available anymore.
    // By using a tag we don't have to query the VMObject for every page
    // whether it was committed or not
    m_lazy_committed_page = committed_pages.take_one();
}

UNMAP_AFTER_INIT MemoryManager::~MemoryManager()
{
}

UNMAP_AFTER_INIT void MemoryManager::protect_kernel_image()
{
    SpinlockLocker page_lock(kernel_page_directory().get_lock());
    // Disable writing to the kernel text and rodata segments.
    for (auto const* i = start_of_kernel_text; i < start_of_kernel_data; i += PAGE_SIZE) {
        auto& pte = *ensure_pte(kernel_page_directory(), VirtualAddress(i));
        pte.set_writable(false);
    }
    if (Processor::current().has_feature(CPUFeature::NX)) {
        // Disable execution of the kernel data, bss and heap segments.
        for (auto const* i = start_of_kernel_data; i < end_of_kernel_image; i += PAGE_SIZE) {
            auto& pte = *ensure_pte(kernel_page_directory(), VirtualAddress(i));
            pte.set_execute_disabled(true);
        }
    }
}

UNMAP_AFTER_INIT void MemoryManager::unmap_prekernel()
{
    SpinlockLocker page_lock(kernel_page_directory().get_lock());
    SpinlockLocker mm_lock(s_mm_lock);

    auto start = start_of_prekernel_image.page_base().get();
    auto end = end_of_prekernel_image.page_base().get();

    for (auto i = start; i <= end; i += PAGE_SIZE)
        release_pte(kernel_page_directory(), VirtualAddress(i), i == end ? IsLastPTERelease::Yes : IsLastPTERelease::No);
    flush_tlb(&kernel_page_directory(), VirtualAddress(start), (end - start) / PAGE_SIZE);
}

UNMAP_AFTER_INIT void MemoryManager::protect_readonly_after_init_memory()
{
    SpinlockLocker page_lock(kernel_page_directory().get_lock());
    SpinlockLocker mm_lock(s_mm_lock);
    // Disable writing to the .ro_after_init section
    for (auto i = (FlatPtr)&start_of_ro_after_init; i < (FlatPtr)&end_of_ro_after_init; i += PAGE_SIZE) {
        auto& pte = *ensure_pte(kernel_page_directory(), VirtualAddress(i));
        pte.set_writable(false);
        flush_tlb(&kernel_page_directory(), VirtualAddress(i));
    }
}

void MemoryManager::unmap_text_after_init()
{
    SpinlockLocker page_lock(kernel_page_directory().get_lock());
    SpinlockLocker mm_lock(s_mm_lock);

    auto start = page_round_down((FlatPtr)&start_of_unmap_after_init);
    auto end = page_round_up((FlatPtr)&end_of_unmap_after_init).release_value_but_fixme_should_propagate_errors();

    // Unmap the entire .unmap_after_init section
    for (auto i = start; i < end; i += PAGE_SIZE) {
        auto& pte = *ensure_pte(kernel_page_directory(), VirtualAddress(i));
        pte.clear();
        flush_tlb(&kernel_page_directory(), VirtualAddress(i));
    }

    dmesgln("Unmapped {} KiB of kernel text after init! :^)", (end - start) / KiB);
}

UNMAP_AFTER_INIT void MemoryManager::protect_ksyms_after_init()
{
    SpinlockLocker mm_lock(s_mm_lock);
    SpinlockLocker page_lock(kernel_page_directory().get_lock());

    auto start = page_round_down((FlatPtr)start_of_kernel_ksyms);
    auto end = page_round_up((FlatPtr)end_of_kernel_ksyms).release_value_but_fixme_should_propagate_errors();

    for (auto i = start; i < end; i += PAGE_SIZE) {
        auto& pte = *ensure_pte(kernel_page_directory(), VirtualAddress(i));
        pte.set_writable(false);
        flush_tlb(&kernel_page_directory(), VirtualAddress(i));
    }

    dmesgln("Write-protected kernel symbols after init.");
}

IterationDecision MemoryManager::for_each_physical_memory_range(Function<IterationDecision(PhysicalMemoryRange const&)> callback)
{
    VERIFY(!m_physical_memory_ranges.is_empty());
    for (auto& current_range : m_physical_memory_ranges) {
        IterationDecision decision = callback(current_range);
        if (decision != IterationDecision::Continue)
            return decision;
    }
    return IterationDecision::Continue;
}

UNMAP_AFTER_INIT void MemoryManager::register_reserved_ranges()
{
    VERIFY(!m_physical_memory_ranges.is_empty());
    ContiguousReservedMemoryRange range;
    for (auto& current_range : m_physical_memory_ranges) {
        if (current_range.type != PhysicalMemoryRangeType::Reserved) {
            if (range.start.is_null())
                continue;
            m_reserved_memory_ranges.append(ContiguousReservedMemoryRange { range.start, current_range.start.get() - range.start.get() });
            range.start.set((FlatPtr) nullptr);
            continue;
        }
        if (!range.start.is_null()) {
            continue;
        }
        range.start = current_range.start;
    }
    if (m_physical_memory_ranges.last().type != PhysicalMemoryRangeType::Reserved)
        return;
    if (range.start.is_null())
        return;
    m_reserved_memory_ranges.append(ContiguousReservedMemoryRange { range.start, m_physical_memory_ranges.last().start.get() + m_physical_memory_ranges.last().length - range.start.get() });
}

bool MemoryManager::is_allowed_to_read_physical_memory_for_userspace(PhysicalAddress start_address, size_t read_length) const
{
    // Note: Guard against overflow in case someone tries to mmap on the edge of
    // the RAM
    if (start_address.offset_addition_would_overflow(read_length))
        return false;
    auto end_address = start_address.offset(read_length);
    for (auto const& current_range : m_reserved_memory_ranges) {
        if (current_range.start > start_address)
            continue;
        if (current_range.start.offset(current_range.length) < end_address)
            continue;
        return true;
    }
    return false;
}

UNMAP_AFTER_INIT void MemoryManager::parse_memory_map()
{
    // Register used memory regions that we know of.
    m_used_memory_ranges.ensure_capacity(4);
    m_used_memory_ranges.append(UsedMemoryRange { UsedMemoryRangeType::LowMemory, PhysicalAddress(0x00000000), PhysicalAddress(1 * MiB) });
    m_used_memory_ranges.append(UsedMemoryRange { UsedMemoryRangeType::Kernel, PhysicalAddress(virtual_to_low_physical((FlatPtr)start_of_kernel_image)), PhysicalAddress(page_round_up(virtual_to_low_physical((FlatPtr)end_of_kernel_image)).release_value_but_fixme_should_propagate_errors()) });

    if (multiboot_flags & 0x4) {
        auto* bootmods_start = multiboot_copy_boot_modules_array;
        auto* bootmods_end = bootmods_start + multiboot_copy_boot_modules_count;

        for (auto* bootmod = bootmods_start; bootmod < bootmods_end; bootmod++) {
            m_used_memory_ranges.append(UsedMemoryRange { UsedMemoryRangeType::BootModule, PhysicalAddress(bootmod->start), PhysicalAddress(bootmod->end) });
        }
    }

    auto* mmap_begin = multiboot_memory_map;
    auto* mmap_end = multiboot_memory_map + multiboot_memory_map_count;

    struct ContiguousPhysicalVirtualRange {
        PhysicalAddress lower;
        PhysicalAddress upper;
    };

    Vector<ContiguousPhysicalVirtualRange> contiguous_physical_ranges;

    for (auto* mmap = mmap_begin; mmap < mmap_end; mmap++) {
        // We have to copy these onto the stack, because we take a reference to these when printing them out,
        // and doing so on a packed struct field is UB.
        auto address = mmap->addr;
        auto length = mmap->len;
        ArmedScopeGuard write_back_guard = [&]() {
            mmap->addr = address;
            mmap->len = length;
        };

        dmesgln("MM: Multiboot mmap: address={:p}, length={}, type={}", address, length, mmap->type);

        auto start_address = PhysicalAddress(address);
        switch (mmap->type) {
        case (MULTIBOOT_MEMORY_AVAILABLE):
            m_physical_memory_ranges.append(PhysicalMemoryRange { PhysicalMemoryRangeType::Usable, start_address, length });
            break;
        case (MULTIBOOT_MEMORY_RESERVED):
            m_physical_memory_ranges.append(PhysicalMemoryRange { PhysicalMemoryRangeType::Reserved, start_address, length });
            break;
        case (MULTIBOOT_MEMORY_ACPI_RECLAIMABLE):
            m_physical_memory_ranges.append(PhysicalMemoryRange { PhysicalMemoryRangeType::ACPI_Reclaimable, start_address, length });
            break;
        case (MULTIBOOT_MEMORY_NVS):
            m_physical_memory_ranges.append(PhysicalMemoryRange { PhysicalMemoryRangeType::ACPI_NVS, start_address, length });
            break;
        case (MULTIBOOT_MEMORY_BADRAM):
            dmesgln("MM: Warning, detected bad memory range!");
            m_physical_memory_ranges.append(PhysicalMemoryRange { PhysicalMemoryRangeType::BadMemory, start_address, length });
            break;
        default:
            dbgln("MM: Unknown range!");
            m_physical_memory_ranges.append(PhysicalMemoryRange { PhysicalMemoryRangeType::Unknown, start_address, length });
            break;
        }

        if (mmap->type != MULTIBOOT_MEMORY_AVAILABLE)
            continue;

        // Fix up unaligned memory regions.
        auto diff = (FlatPtr)address % PAGE_SIZE;
        if (diff != 0) {
            dmesgln("MM: Got an unaligned physical_region from the bootloader; correcting {:p} by {} bytes", address, diff);
            diff = PAGE_SIZE - diff;
            address += diff;
            length -= diff;
        }
        if ((length % PAGE_SIZE) != 0) {
            dmesgln("MM: Got an unaligned physical_region from the bootloader; correcting length {} by {} bytes", length, length % PAGE_SIZE);
            length -= length % PAGE_SIZE;
        }
        if (length < PAGE_SIZE) {
            dmesgln("MM: Memory physical_region from bootloader is too small; we want >= {} bytes, but got {} bytes", PAGE_SIZE, length);
            continue;
        }

        for (PhysicalSize page_base = address; page_base <= (address + length); page_base += PAGE_SIZE) {
            auto addr = PhysicalAddress(page_base);

            // Skip used memory ranges.
            bool should_skip = false;
            for (auto& used_range : m_used_memory_ranges) {
                if (addr.get() >= used_range.start.get() && addr.get() <= used_range.end.get()) {
                    should_skip = true;
                    break;
                }
            }
            if (should_skip)
                continue;

            if (contiguous_physical_ranges.is_empty() || contiguous_physical_ranges.last().upper.offset(PAGE_SIZE) != addr) {
                contiguous_physical_ranges.append(ContiguousPhysicalVirtualRange {
                    .lower = addr,
                    .upper = addr,
                });
            } else {
                contiguous_physical_ranges.last().upper = addr;
            }
        }
    }

    for (auto& range : contiguous_physical_ranges) {
        m_user_physical_regions.append(PhysicalRegion::try_create(range.lower, range.upper).release_nonnull());
    }

    // Super pages are guaranteed to be in the first 16MB of physical memory
    VERIFY(virtual_to_low_physical((FlatPtr)super_pages) + sizeof(super_pages) < 0x1000000);

    // Append statically-allocated super physical physical_region.
    m_super_physical_region = PhysicalRegion::try_create(
        PhysicalAddress(virtual_to_low_physical(FlatPtr(super_pages))),
        PhysicalAddress(virtual_to_low_physical(FlatPtr(super_pages + sizeof(super_pages)))));
    VERIFY(m_super_physical_region);

    m_system_memory_info.super_physical_pages += m_super_physical_region->size();

    for (auto& region : m_user_physical_regions)
        m_system_memory_info.user_physical_pages += region.size();

    register_reserved_ranges();
    for (auto& range : m_reserved_memory_ranges) {
        dmesgln("MM: Contiguous reserved range from {}, length is {}", range.start, range.length);
    }

    initialize_physical_pages();

    VERIFY(m_system_memory_info.super_physical_pages > 0);
    VERIFY(m_system_memory_info.user_physical_pages > 0);

    // We start out with no committed pages
    m_system_memory_info.user_physical_pages_uncommitted = m_system_memory_info.user_physical_pages;

    for (auto& used_range : m_used_memory_ranges) {
        dmesgln("MM: {} range @ {} - {} (size {:#x})", UserMemoryRangeTypeNames[to_underlying(used_range.type)], used_range.start, used_range.end.offset(-1), used_range.end.as_ptr() - used_range.start.as_ptr());
    }

    dmesgln("MM: Super physical region: {} - {} (size {:#x})", m_super_physical_region->lower(), m_super_physical_region->upper().offset(-1), PAGE_SIZE * m_super_physical_region->size());
    m_super_physical_region->initialize_zones();

    for (auto& region : m_user_physical_regions) {
        dmesgln("MM: User physical region: {} - {} (size {:#x})", region.lower(), region.upper().offset(-1), PAGE_SIZE * region.size());
        region.initialize_zones();
    }
}

UNMAP_AFTER_INIT void MemoryManager::initialize_physical_pages()
{
    // We assume that the physical page range is contiguous and doesn't contain huge gaps!
    PhysicalAddress highest_physical_address;
    for (auto& range : m_used_memory_ranges) {
        if (range.end.get() > highest_physical_address.get())
            highest_physical_address = range.end;
    }
    for (auto& region : m_physical_memory_ranges) {
        auto range_end = PhysicalAddress(region.start).offset(region.length);
        if (range_end.get() > highest_physical_address.get())
            highest_physical_address = range_end;
    }

    // Calculate how many total physical pages the array will have
    m_physical_page_entries_count = PhysicalAddress::physical_page_index(highest_physical_address.get()) + 1;
    VERIFY(m_physical_page_entries_count != 0);
    VERIFY(!Checked<decltype(m_physical_page_entries_count)>::multiplication_would_overflow(m_physical_page_entries_count, sizeof(PhysicalPageEntry)));

    // Calculate how many bytes the array will consume
    auto physical_page_array_size = m_physical_page_entries_count * sizeof(PhysicalPageEntry);
    auto physical_page_array_pages = page_round_up(physical_page_array_size).release_value_but_fixme_should_propagate_errors() / PAGE_SIZE;
    VERIFY(physical_page_array_pages * PAGE_SIZE >= physical_page_array_size);

    // Calculate how many page tables we will need to be able to map them all
    auto needed_page_table_count = (physical_page_array_pages + 512 - 1) / 512;

    auto physical_page_array_pages_and_page_tables_count = physical_page_array_pages + needed_page_table_count;

    // Now that we know how much memory we need for a contiguous array of PhysicalPage instances, find a memory region that can fit it
    PhysicalRegion* found_region { nullptr };
    Optional<size_t> found_region_index;
    for (size_t i = 0; i < m_user_physical_regions.size(); ++i) {
        auto& region = m_user_physical_regions[i];
        if (region.size() >= physical_page_array_pages_and_page_tables_count) {
            found_region = &region;
            found_region_index = i;
            break;
        }
    }

    if (!found_region) {
        dmesgln("MM: Need {} bytes for physical page management, but no memory region is large enough!", physical_page_array_pages_and_page_tables_count);
        VERIFY_NOT_REACHED();
    }

    VERIFY(m_system_memory_info.user_physical_pages >= physical_page_array_pages_and_page_tables_count);
    m_system_memory_info.user_physical_pages -= physical_page_array_pages_and_page_tables_count;

    if (found_region->size() == physical_page_array_pages_and_page_tables_count) {
        // We're stealing the entire region
        m_physical_pages_region = m_user_physical_regions.take(*found_region_index);
    } else {
        m_physical_pages_region = found_region->try_take_pages_from_beginning(physical_page_array_pages_and_page_tables_count);
    }
    m_used_memory_ranges.append({ UsedMemoryRangeType::PhysicalPages, m_physical_pages_region->lower(), m_physical_pages_region->upper() });

    // Create the bare page directory. This is not a fully constructed page directory and merely contains the allocators!
    m_kernel_page_directory = PageDirectory::must_create_kernel_page_directory();

    // Allocate a virtual address range for our array
    auto range_or_error = m_kernel_page_directory->range_allocator().try_allocate_anywhere(physical_page_array_pages * PAGE_SIZE);
    if (range_or_error.is_error()) {
        dmesgln("MM: Could not allocate {} bytes to map physical page array!", physical_page_array_pages * PAGE_SIZE);
        VERIFY_NOT_REACHED();
    }
    auto range = range_or_error.release_value();

    // Now that we have our special m_physical_pages_region region with enough pages to hold the entire array
    // try to map the entire region into kernel space so we always have it
    // We can't use ensure_pte here because it would try to allocate a PhysicalPage and we don't have the array
    // mapped yet so we can't create them
    SpinlockLocker lock(s_mm_lock);

    // Create page tables at the beginning of m_physical_pages_region, followed by the PhysicalPageEntry array
    auto page_tables_base = m_physical_pages_region->lower();
    auto physical_page_array_base = page_tables_base.offset(needed_page_table_count * PAGE_SIZE);
    auto physical_page_array_current_page = physical_page_array_base.get();
    auto virtual_page_array_base = range.base().get();
    auto virtual_page_array_current_page = virtual_page_array_base;
    for (size_t pt_index = 0; pt_index < needed_page_table_count; pt_index++) {
        auto virtual_page_base_for_this_pt = virtual_page_array_current_page;
        auto pt_paddr = page_tables_base.offset(pt_index * PAGE_SIZE);
        auto* pt = reinterpret_cast<PageTableEntry*>(quickmap_page(pt_paddr));
        __builtin_memset(pt, 0, PAGE_SIZE);
        for (size_t pte_index = 0; pte_index < PAGE_SIZE / sizeof(PageTableEntry); pte_index++) {
            auto& pte = pt[pte_index];
            pte.set_physical_page_base(physical_page_array_current_page);
            pte.set_user_allowed(false);
            pte.set_writable(true);
            if (Processor::current().has_feature(CPUFeature::NX))
                pte.set_execute_disabled(false);
            pte.set_global(true);
            pte.set_present(true);

            physical_page_array_current_page += PAGE_SIZE;
            virtual_page_array_current_page += PAGE_SIZE;
        }
        unquickmap_page();

        // Hook the page table into the kernel page directory
        u32 page_directory_index = (virtual_page_base_for_this_pt >> 21) & 0x1ff;
        auto* pd = reinterpret_cast<PageDirectoryEntry*>(quickmap_page(boot_pd_kernel));
        PageDirectoryEntry& pde = pd[page_directory_index];

        VERIFY(!pde.is_present()); // Nothing should be using this PD yet

        // We can't use ensure_pte quite yet!
        pde.set_page_table_base(pt_paddr.get());
        pde.set_user_allowed(false);
        pde.set_present(true);
        pde.set_writable(true);
        pde.set_global(true);

        unquickmap_page();

        flush_tlb_local(VirtualAddress(virtual_page_base_for_this_pt));
    }

    // We now have the entire PhysicalPageEntry array mapped!
    m_physical_page_entries = (PhysicalPageEntry*)range.base().get();
    for (size_t i = 0; i < m_physical_page_entries_count; i++)
        new (&m_physical_page_entries[i]) PageTableEntry();

    // Now we should be able to allocate PhysicalPage instances,
    // so finish setting up the kernel page directory
    m_kernel_page_directory->allocate_kernel_directory();

    // Now create legit PhysicalPage objects for the page tables we created.
    virtual_page_array_current_page = virtual_page_array_base;
    for (size_t pt_index = 0; pt_index < needed_page_table_count; pt_index++) {
        VERIFY(virtual_page_array_current_page <= range.end().get());
        auto pt_paddr = page_tables_base.offset(pt_index * PAGE_SIZE);
        auto physical_page_index = PhysicalAddress::physical_page_index(pt_paddr.get());
        auto& physical_page_entry = m_physical_page_entries[physical_page_index];
        auto physical_page = adopt_ref(*new (&physical_page_entry.allocated.physical_page) PhysicalPage(MayReturnToFreeList::No));

        // NOTE: This leaked ref is matched by the unref in MemoryManager::release_pte()
        (void)physical_page.leak_ref();

        virtual_page_array_current_page += (PAGE_SIZE / sizeof(PageTableEntry)) * PAGE_SIZE;
    }

    dmesgln("MM: Physical page entries: {}", range);
}

PhysicalPageEntry& MemoryManager::get_physical_page_entry(PhysicalAddress physical_address)
{
    VERIFY(m_physical_page_entries);
    auto physical_page_entry_index = PhysicalAddress::physical_page_index(physical_address.get());
    VERIFY(physical_page_entry_index < m_physical_page_entries_count);
    return m_physical_page_entries[physical_page_entry_index];
}

PhysicalAddress MemoryManager::get_physical_address(PhysicalPage const& physical_page)
{
    PhysicalPageEntry const& physical_page_entry = *reinterpret_cast<PhysicalPageEntry const*>((u8 const*)&physical_page - __builtin_offsetof(PhysicalPageEntry, allocated.physical_page));
    VERIFY(m_physical_page_entries);
    size_t physical_page_entry_index = &physical_page_entry - m_physical_page_entries;
    VERIFY(physical_page_entry_index < m_physical_page_entries_count);
    return PhysicalAddress((PhysicalPtr)physical_page_entry_index * PAGE_SIZE);
}

PageTableEntry* MemoryManager::pte(PageDirectory& page_directory, VirtualAddress vaddr)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(s_mm_lock.is_locked_by_current_processor());
    VERIFY(page_directory.get_lock().is_locked_by_current_processor());
    u32 page_directory_table_index = (vaddr.get() >> 30) & 0x1ff;
    u32 page_directory_index = (vaddr.get() >> 21) & 0x1ff;
    u32 page_table_index = (vaddr.get() >> 12) & 0x1ff;

    auto* pd = quickmap_pd(const_cast<PageDirectory&>(page_directory), page_directory_table_index);
    PageDirectoryEntry const& pde = pd[page_directory_index];
    if (!pde.is_present())
        return nullptr;

    return &quickmap_pt(PhysicalAddress((FlatPtr)pde.page_table_base()))[page_table_index];
}

PageTableEntry* MemoryManager::ensure_pte(PageDirectory& page_directory, VirtualAddress vaddr)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(s_mm_lock.is_locked_by_current_processor());
    VERIFY(page_directory.get_lock().is_locked_by_current_processor());
    u32 page_directory_table_index = (vaddr.get() >> 30) & 0x1ff;
    u32 page_directory_index = (vaddr.get() >> 21) & 0x1ff;
    u32 page_table_index = (vaddr.get() >> 12) & 0x1ff;

    auto* pd = quickmap_pd(page_directory, page_directory_table_index);
    auto& pde = pd[page_directory_index];
    if (pde.is_present())
        return &quickmap_pt(PhysicalAddress(pde.page_table_base()))[page_table_index];

    bool did_purge = false;
    auto page_table_or_error = allocate_user_physical_page(ShouldZeroFill::Yes, &did_purge);
    if (page_table_or_error.is_error()) {
        dbgln("MM: Unable to allocate page table to map {}", vaddr);
        return nullptr;
    }
    auto page_table = page_table_or_error.release_value();
    if (did_purge) {
        // If any memory had to be purged, ensure_pte may have been called as part
        // of the purging process. So we need to re-map the pd in this case to ensure
        // we're writing to the correct underlying physical page
        pd = quickmap_pd(page_directory, page_directory_table_index);
        VERIFY(&pde == &pd[page_directory_index]); // Sanity check

        VERIFY(!pde.is_present()); // Should have not changed
    }
    pde.set_page_table_base(page_table->paddr().get());
    pde.set_user_allowed(true);
    pde.set_present(true);
    pde.set_writable(true);
    pde.set_global(&page_directory == m_kernel_page_directory.ptr());

    // NOTE: This leaked ref is matched by the unref in MemoryManager::release_pte()
    (void)page_table.leak_ref();

    return &quickmap_pt(PhysicalAddress(pde.page_table_base()))[page_table_index];
}

void MemoryManager::release_pte(PageDirectory& page_directory, VirtualAddress vaddr, IsLastPTERelease is_last_pte_release)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(s_mm_lock.is_locked_by_current_processor());
    VERIFY(page_directory.get_lock().is_locked_by_current_processor());
    u32 page_directory_table_index = (vaddr.get() >> 30) & 0x1ff;
    u32 page_directory_index = (vaddr.get() >> 21) & 0x1ff;
    u32 page_table_index = (vaddr.get() >> 12) & 0x1ff;

    auto* pd = quickmap_pd(page_directory, page_directory_table_index);
    PageDirectoryEntry& pde = pd[page_directory_index];
    if (pde.is_present()) {
        auto* page_table = quickmap_pt(PhysicalAddress((FlatPtr)pde.page_table_base()));
        auto& pte = page_table[page_table_index];
        pte.clear();

        if (is_last_pte_release == IsLastPTERelease::Yes || page_table_index == 0x1ff) {
            // If this is the last PTE in a region or the last PTE in a page table then
            // check if we can also release the page table
            bool all_clear = true;
            for (u32 i = 0; i <= 0x1ff; i++) {
                if (!page_table[i].is_null()) {
                    all_clear = false;
                    break;
                }
            }
            if (all_clear) {
                get_physical_page_entry(PhysicalAddress { pde.page_table_base() }).allocated.physical_page.unref();
                pde.clear();
            }
        }
    }
}

UNMAP_AFTER_INIT void MemoryManager::initialize(u32 cpu)
{
    ProcessorSpecific<MemoryManagerData>::initialize();

    if (cpu == 0) {
        new MemoryManager;
        kmalloc_enable_expand();
    }
}

Region* MemoryManager::kernel_region_from_vaddr(VirtualAddress vaddr)
{
    if (is_user_address(vaddr))
        return nullptr;

    SpinlockLocker lock(s_mm_lock);
    auto* region = MM.m_kernel_regions.find_largest_not_above(vaddr.get());
    if (!region || !region->contains(vaddr))
        return nullptr;
    return region;
}

Region* MemoryManager::find_user_region_from_vaddr_no_lock(AddressSpace& space, VirtualAddress vaddr)
{
    VERIFY(space.get_lock().is_locked_by_current_processor());
    return space.find_region_containing({ vaddr, 1 });
}

Region* MemoryManager::find_user_region_from_vaddr(AddressSpace& space, VirtualAddress vaddr)
{
    SpinlockLocker lock(space.get_lock());
    return find_user_region_from_vaddr_no_lock(space, vaddr);
}

void MemoryManager::validate_syscall_preconditions(AddressSpace& space, RegisterState const& regs)
{
    // We take the space lock once here and then use the no_lock variants
    // to avoid excessive spinlock recursion in this extremely common path.
    SpinlockLocker lock(space.get_lock());

    auto unlock_and_handle_crash = [&lock, &regs](const char* description, int signal) {
        lock.unlock();
        handle_crash(regs, description, signal);
    };

    {
        VirtualAddress userspace_sp = VirtualAddress { regs.userspace_sp() };
        if (!MM.validate_user_stack_no_lock(space, userspace_sp)) {
            dbgln("Invalid stack pointer: {}", userspace_sp);
            return unlock_and_handle_crash("Bad stack on syscall entry", SIGSEGV);
        }
    }

    {
        VirtualAddress ip = VirtualAddress { regs.ip() };
        auto* calling_region = MM.find_user_region_from_vaddr_no_lock(space, ip);
        if (!calling_region) {
            dbgln("Syscall from {:p} which has no associated region", ip);
            return unlock_and_handle_crash("Syscall from unknown region", SIGSEGV);
        }

        if (calling_region->is_writable()) {
            dbgln("Syscall from writable memory at {:p}", ip);
            return unlock_and_handle_crash("Syscall from writable memory", SIGSEGV);
        }

        if (space.enforces_syscall_regions() && !calling_region->is_syscall_region()) {
            dbgln("Syscall from non-syscall region");
            return unlock_and_handle_crash("Syscall from non-syscall region", SIGSEGV);
        }
    }
}

Region* MemoryManager::find_region_from_vaddr(VirtualAddress vaddr)
{
    if (auto* region = kernel_region_from_vaddr(vaddr))
        return region;
    auto page_directory = PageDirectory::find_by_cr3(read_cr3());
    if (!page_directory)
        return nullptr;
    VERIFY(page_directory->address_space());
    return find_user_region_from_vaddr(*page_directory->address_space(), vaddr);
}

PageFaultResponse MemoryManager::handle_page_fault(PageFault const& fault)
{
    VERIFY_INTERRUPTS_DISABLED();

    auto faulted_in_range = [&fault](auto const* start, auto const* end) {
        return fault.vaddr() >= VirtualAddress { start } && fault.vaddr() < VirtualAddress { end };
    };

    if (faulted_in_range(&start_of_ro_after_init, &end_of_ro_after_init))
        PANIC("Attempt to write into READONLY_AFTER_INIT section");

    if (faulted_in_range(&start_of_unmap_after_init, &end_of_unmap_after_init)) {
        auto const* kernel_symbol = symbolicate_kernel_address(fault.vaddr().get());
        PANIC("Attempt to access UNMAP_AFTER_INIT section ({:p}: {})", fault.vaddr(), kernel_symbol ? kernel_symbol->name : "(Unknown)");
    }

    if (faulted_in_range(&start_of_kernel_ksyms, &end_of_kernel_ksyms))
        PANIC("Attempt to access KSYMS section");

    if (Processor::current_in_irq()) {
        dbgln("CPU[{}] BUG! Page fault while handling IRQ! code={}, vaddr={}, irq level: {}",
            Processor::current_id(), fault.code(), fault.vaddr(), Processor::current_in_irq());
        dump_kernel_regions();
        return PageFaultResponse::ShouldCrash;
    }
    dbgln_if(PAGE_FAULT_DEBUG, "MM: CPU[{}] handle_page_fault({:#04x}) at {}", Processor::current_id(), fault.code(), fault.vaddr());
    auto* region = find_region_from_vaddr(fault.vaddr());
    if (!region) {
        return PageFaultResponse::ShouldCrash;
    }
    return region->handle_fault(fault);
}

ErrorOr<NonnullOwnPtr<Region>> MemoryManager::allocate_contiguous_kernel_region(size_t size, StringView name, Region::Access access, Region::Cacheable cacheable)
{
    VERIFY(!(size % PAGE_SIZE));
    SpinlockLocker lock(kernel_page_directory().get_lock());
    auto vmobject = TRY(AnonymousVMObject::try_create_physically_contiguous_with_size(size));
    auto range = TRY(kernel_page_directory().range_allocator().try_allocate_anywhere(size));
    return allocate_kernel_region_with_vmobject(range, move(vmobject), name, access, cacheable);
}

ErrorOr<NonnullOwnPtr<Memory::Region>> MemoryManager::allocate_dma_buffer_page(StringView name, Memory::Region::Access access, RefPtr<Memory::PhysicalPage>& dma_buffer_page)
{
    dma_buffer_page = TRY(allocate_supervisor_physical_page());
    // Do not enable Cache for this region as physical memory transfers are performed (Most architectures have this behaviour by default)
    return allocate_kernel_region(dma_buffer_page->paddr(), PAGE_SIZE, name, access, Region::Cacheable::No);
}

ErrorOr<NonnullOwnPtr<Memory::Region>> MemoryManager::allocate_dma_buffer_page(StringView name, Memory::Region::Access access)
{
    RefPtr<Memory::PhysicalPage> dma_buffer_page;

    return allocate_dma_buffer_page(name, access, dma_buffer_page);
}

ErrorOr<NonnullOwnPtr<Memory::Region>> MemoryManager::allocate_dma_buffer_pages(size_t size, StringView name, Memory::Region::Access access, NonnullRefPtrVector<Memory::PhysicalPage>& dma_buffer_pages)
{
    VERIFY(!(size % PAGE_SIZE));
    dma_buffer_pages = TRY(allocate_contiguous_supervisor_physical_pages(size));
    // Do not enable Cache for this region as physical memory transfers are performed (Most architectures have this behaviour by default)
    return allocate_kernel_region(dma_buffer_pages.first().paddr(), size, name, access, Region::Cacheable::No);
}

ErrorOr<NonnullOwnPtr<Memory::Region>> MemoryManager::allocate_dma_buffer_pages(size_t size, StringView name, Memory::Region::Access access)
{
    VERIFY(!(size % PAGE_SIZE));
    NonnullRefPtrVector<Memory::PhysicalPage> dma_buffer_pages;

    return allocate_dma_buffer_pages(size, name, access, dma_buffer_pages);
}

ErrorOr<NonnullOwnPtr<Region>> MemoryManager::allocate_kernel_region(size_t size, StringView name, Region::Access access, AllocationStrategy strategy, Region::Cacheable cacheable)
{
    VERIFY(!(size % PAGE_SIZE));
    auto vmobject = TRY(AnonymousVMObject::try_create_with_size(size, strategy));
    SpinlockLocker lock(kernel_page_directory().get_lock());
    auto range = TRY(kernel_page_directory().range_allocator().try_allocate_anywhere(size));
    return allocate_kernel_region_with_vmobject(range, move(vmobject), name, access, cacheable);
}

ErrorOr<NonnullOwnPtr<Region>> MemoryManager::allocate_kernel_region(PhysicalAddress paddr, size_t size, StringView name, Region::Access access, Region::Cacheable cacheable)
{
    VERIFY(!(size % PAGE_SIZE));
    auto vmobject = TRY(AnonymousVMObject::try_create_for_physical_range(paddr, size));
    SpinlockLocker lock(kernel_page_directory().get_lock());
    auto range = TRY(kernel_page_directory().range_allocator().try_allocate_anywhere(size));
    return allocate_kernel_region_with_vmobject(range, move(vmobject), name, access, cacheable);
}

ErrorOr<NonnullOwnPtr<Region>> MemoryManager::allocate_kernel_region_with_vmobject(VirtualRange const& range, VMObject& vmobject, StringView name, Region::Access access, Region::Cacheable cacheable)
{
    OwnPtr<KString> name_kstring;
    if (!name.is_null())
        name_kstring = TRY(KString::try_create(name));
    auto region = TRY(Region::try_create_kernel_only(range, vmobject, 0, move(name_kstring), access, cacheable));
    TRY(region->map(kernel_page_directory()));
    return region;
}

ErrorOr<NonnullOwnPtr<Region>> MemoryManager::allocate_kernel_region_with_vmobject(VMObject& vmobject, size_t size, StringView name, Region::Access access, Region::Cacheable cacheable)
{
    VERIFY(!(size % PAGE_SIZE));
    SpinlockLocker lock(kernel_page_directory().get_lock());
    auto range = TRY(kernel_page_directory().range_allocator().try_allocate_anywhere(size));
    return allocate_kernel_region_with_vmobject(range, vmobject, name, access, cacheable);
}

ErrorOr<CommittedPhysicalPageSet> MemoryManager::commit_user_physical_pages(size_t page_count)
{
    VERIFY(page_count > 0);
    SpinlockLocker lock(s_mm_lock);
    if (m_system_memory_info.user_physical_pages_uncommitted < page_count)
        return ENOMEM;

    m_system_memory_info.user_physical_pages_uncommitted -= page_count;
    m_system_memory_info.user_physical_pages_committed += page_count;
    return CommittedPhysicalPageSet { {}, page_count };
}

void MemoryManager::uncommit_user_physical_pages(Badge<CommittedPhysicalPageSet>, size_t page_count)
{
    VERIFY(page_count > 0);

    SpinlockLocker lock(s_mm_lock);
    VERIFY(m_system_memory_info.user_physical_pages_committed >= page_count);

    m_system_memory_info.user_physical_pages_uncommitted += page_count;
    m_system_memory_info.user_physical_pages_committed -= page_count;
}

void MemoryManager::deallocate_physical_page(PhysicalAddress paddr)
{
    SpinlockLocker lock(s_mm_lock);

    // Are we returning a user page?
    for (auto& region : m_user_physical_regions) {
        if (!region.contains(paddr))
            continue;

        region.return_page(paddr);
        --m_system_memory_info.user_physical_pages_used;

        // Always return pages to the uncommitted pool. Pages that were
        // committed and allocated are only freed upon request. Once
        // returned there is no guarantee being able to get them back.
        ++m_system_memory_info.user_physical_pages_uncommitted;
        return;
    }

    // If it's not a user page, it should be a supervisor page.
    if (!m_super_physical_region->contains(paddr))
        PANIC("MM: deallocate_user_physical_page couldn't figure out region for page @ {}", paddr);

    m_super_physical_region->return_page(paddr);
    --m_system_memory_info.super_physical_pages_used;
}

RefPtr<PhysicalPage> MemoryManager::find_free_user_physical_page(bool committed)
{
    VERIFY(s_mm_lock.is_locked());
    RefPtr<PhysicalPage> page;
    if (committed) {
        // Draw from the committed pages pool. We should always have these pages available
        VERIFY(m_system_memory_info.user_physical_pages_committed > 0);
        m_system_memory_info.user_physical_pages_committed--;
    } else {
        // We need to make sure we don't touch pages that we have committed to
        if (m_system_memory_info.user_physical_pages_uncommitted == 0)
            return {};
        m_system_memory_info.user_physical_pages_uncommitted--;
    }
    for (auto& region : m_user_physical_regions) {
        page = region.take_free_page();
        if (!page.is_null()) {
            ++m_system_memory_info.user_physical_pages_used;
            break;
        }
    }
    VERIFY(!committed || !page.is_null());
    return page;
}

NonnullRefPtr<PhysicalPage> MemoryManager::allocate_committed_user_physical_page(Badge<CommittedPhysicalPageSet>, ShouldZeroFill should_zero_fill)
{
    SpinlockLocker lock(s_mm_lock);
    auto page = find_free_user_physical_page(true);
    if (should_zero_fill == ShouldZeroFill::Yes) {
        auto* ptr = quickmap_page(*page);
        memset(ptr, 0, PAGE_SIZE);
        unquickmap_page();
    }
    return page.release_nonnull();
}

ErrorOr<NonnullRefPtr<PhysicalPage>> MemoryManager::allocate_user_physical_page(ShouldZeroFill should_zero_fill, bool* did_purge)
{
    SpinlockLocker lock(s_mm_lock);
    auto page = find_free_user_physical_page(false);
    bool purged_pages = false;

    if (!page) {
        // We didn't have a single free physical page. Let's try to free something up!
        // First, we look for a purgeable VMObject in the volatile state.
        for_each_vmobject([&](auto& vmobject) {
            if (!vmobject.is_anonymous())
                return IterationDecision::Continue;
            auto& anonymous_vmobject = static_cast<AnonymousVMObject&>(vmobject);
            if (!anonymous_vmobject.is_purgeable() || !anonymous_vmobject.is_volatile())
                return IterationDecision::Continue;
            if (auto purged_page_count = anonymous_vmobject.purge()) {
                dbgln("MM: Purge saved the day! Purged {} pages from AnonymousVMObject", purged_page_count);
                page = find_free_user_physical_page(false);
                purged_pages = true;
                VERIFY(page);
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (!page) {
            dmesgln("MM: no user physical pages available");
            return ENOMEM;
        }
    }

    if (should_zero_fill == ShouldZeroFill::Yes) {
        auto* ptr = quickmap_page(*page);
        memset(ptr, 0, PAGE_SIZE);
        unquickmap_page();
    }

    if (did_purge)
        *did_purge = purged_pages;
    return page.release_nonnull();
}

ErrorOr<NonnullRefPtrVector<PhysicalPage>> MemoryManager::allocate_contiguous_user_physical_pages(size_t size)
{
    VERIFY(!(size % PAGE_SIZE));
    SpinlockLocker lock(s_mm_lock);
    size_t page_count = ceil_div(size, static_cast<size_t>(PAGE_SIZE));

    for (auto& physical_region : m_user_physical_regions) {
        auto physical_pages = physical_region.take_contiguous_free_pages(page_count);
        if (!physical_pages.is_empty()) {
            {
                auto cleanup_region = TRY(MM.allocate_kernel_region(physical_pages[0].paddr(), PAGE_SIZE * page_count, "MemoryManager Allocation Sanitization", Region::Access::Read | Region::Access::Write));
                memset(cleanup_region->vaddr().as_ptr(), 0, PAGE_SIZE * page_count);
            }
            m_system_memory_info.user_physical_pages_used += page_count;
            return physical_pages;
        }
    }

    dmesgln("MM: no contiguous user physical pages available");
    return ENOMEM;
}

ErrorOr<NonnullRefPtrVector<PhysicalPage>> MemoryManager::allocate_contiguous_supervisor_physical_pages(size_t size)
{
    VERIFY(!(size % PAGE_SIZE));
    SpinlockLocker lock(s_mm_lock);
    size_t count = ceil_div(size, static_cast<size_t>(PAGE_SIZE));
    auto physical_pages = m_super_physical_region->take_contiguous_free_pages(count);

    if (physical_pages.is_empty()) {
        dmesgln("MM: no super physical pages available");
        return ENOMEM;
    }

    {
        auto cleanup_region = TRY(MM.allocate_kernel_region(physical_pages[0].paddr(), PAGE_SIZE * count, "MemoryManager Allocation Sanitization", Region::Access::Read | Region::Access::Write));
        memset(cleanup_region->vaddr().as_ptr(), 0, PAGE_SIZE * count);
    }
    m_system_memory_info.super_physical_pages_used += count;
    return physical_pages;
}

ErrorOr<NonnullRefPtr<PhysicalPage>> MemoryManager::allocate_supervisor_physical_page()
{
    SpinlockLocker lock(s_mm_lock);
    auto page = m_super_physical_region->take_free_page();

    if (!page) {
        dmesgln("MM: no super physical pages available");
        return ENOMEM;
    }

    auto* ptr = quickmap_page(*page);
    memset(ptr, 0, PAGE_SIZE);
    unquickmap_page();
    ++m_system_memory_info.super_physical_pages_used;
    return page.release_nonnull();
}

void MemoryManager::enter_process_address_space(Process& process)
{
    enter_address_space(process.address_space());
}

void MemoryManager::enter_address_space(AddressSpace& space)
{
    auto* current_thread = Thread::current();
    VERIFY(current_thread != nullptr);
    SpinlockLocker lock(s_mm_lock);

    current_thread->regs().cr3 = space.page_directory().cr3();
    write_cr3(space.page_directory().cr3());
}

void MemoryManager::flush_tlb_local(VirtualAddress vaddr, size_t page_count)
{
    Processor::flush_tlb_local(vaddr, page_count);
}

void MemoryManager::flush_tlb(PageDirectory const* page_directory, VirtualAddress vaddr, size_t page_count)
{
    Processor::flush_tlb(page_directory, vaddr, page_count);
}

PageDirectoryEntry* MemoryManager::quickmap_pd(PageDirectory& directory, size_t pdpt_index)
{
    VERIFY(s_mm_lock.is_locked_by_current_processor());
    auto& mm_data = get_data();
    auto& pte = boot_pd_kernel_pt1023[(KERNEL_QUICKMAP_PD - KERNEL_PT1024_BASE) / PAGE_SIZE];
    auto pd_paddr = directory.m_directory_pages[pdpt_index]->paddr();
    if (pte.physical_page_base() != pd_paddr.get()) {
        pte.set_physical_page_base(pd_paddr.get());
        pte.set_present(true);
        pte.set_writable(true);
        pte.set_user_allowed(false);
        // Because we must continue to hold the MM lock while we use this
        // mapping, it is sufficient to only flush on the current CPU. Other
        // CPUs trying to use this API must wait on the MM lock anyway
        flush_tlb_local(VirtualAddress(KERNEL_QUICKMAP_PD));
    } else {
        // Even though we don't allow this to be called concurrently, it's
        // possible that this PD was mapped on a different CPU and we don't
        // broadcast the flush. If so, we still need to flush the TLB.
        if (mm_data.m_last_quickmap_pd != pd_paddr)
            flush_tlb_local(VirtualAddress(KERNEL_QUICKMAP_PD));
    }
    mm_data.m_last_quickmap_pd = pd_paddr;
    return (PageDirectoryEntry*)KERNEL_QUICKMAP_PD;
}

PageTableEntry* MemoryManager::quickmap_pt(PhysicalAddress pt_paddr)
{
    VERIFY(s_mm_lock.is_locked_by_current_processor());
    auto& mm_data = get_data();
    auto& pte = ((PageTableEntry*)boot_pd_kernel_pt1023)[(KERNEL_QUICKMAP_PT - KERNEL_PT1024_BASE) / PAGE_SIZE];
    if (pte.physical_page_base() != pt_paddr.get()) {
        pte.set_physical_page_base(pt_paddr.get());
        pte.set_present(true);
        pte.set_writable(true);
        pte.set_user_allowed(false);
        // Because we must continue to hold the MM lock while we use this
        // mapping, it is sufficient to only flush on the current CPU. Other
        // CPUs trying to use this API must wait on the MM lock anyway
        flush_tlb_local(VirtualAddress(KERNEL_QUICKMAP_PT));
    } else {
        // Even though we don't allow this to be called concurrently, it's
        // possible that this PT was mapped on a different CPU and we don't
        // broadcast the flush. If so, we still need to flush the TLB.
        if (mm_data.m_last_quickmap_pt != pt_paddr)
            flush_tlb_local(VirtualAddress(KERNEL_QUICKMAP_PT));
    }
    mm_data.m_last_quickmap_pt = pt_paddr;
    return (PageTableEntry*)KERNEL_QUICKMAP_PT;
}

u8* MemoryManager::quickmap_page(PhysicalAddress const& physical_address)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(s_mm_lock.is_locked_by_current_processor());
    auto& mm_data = get_data();
    mm_data.m_quickmap_prev_flags = mm_data.m_quickmap_in_use.lock();

    VirtualAddress vaddr(KERNEL_QUICKMAP_PER_CPU_BASE + Processor::current_id() * PAGE_SIZE);
    u32 pte_idx = (vaddr.get() - KERNEL_PT1024_BASE) / PAGE_SIZE;

    auto& pte = ((PageTableEntry*)boot_pd_kernel_pt1023)[pte_idx];
    if (pte.physical_page_base() != physical_address.get()) {
        pte.set_physical_page_base(physical_address.get());
        pte.set_present(true);
        pte.set_writable(true);
        pte.set_user_allowed(false);
        flush_tlb_local(vaddr);
    }
    return vaddr.as_ptr();
}

void MemoryManager::unquickmap_page()
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(s_mm_lock.is_locked_by_current_processor());
    auto& mm_data = get_data();
    VERIFY(mm_data.m_quickmap_in_use.is_locked());
    VirtualAddress vaddr(KERNEL_QUICKMAP_PER_CPU_BASE + Processor::current_id() * PAGE_SIZE);
    u32 pte_idx = (vaddr.get() - KERNEL_PT1024_BASE) / PAGE_SIZE;
    auto& pte = ((PageTableEntry*)boot_pd_kernel_pt1023)[pte_idx];
    pte.clear();
    flush_tlb_local(vaddr);
    mm_data.m_quickmap_in_use.unlock(mm_data.m_quickmap_prev_flags);
}

bool MemoryManager::validate_user_stack_no_lock(AddressSpace& space, VirtualAddress vaddr) const
{
    VERIFY(space.get_lock().is_locked_by_current_processor());

    if (!is_user_address(vaddr))
        return false;

    auto* region = find_user_region_from_vaddr_no_lock(space, vaddr);
    return region && region->is_user() && region->is_stack();
}

bool MemoryManager::validate_user_stack(AddressSpace& space, VirtualAddress vaddr) const
{
    SpinlockLocker lock(space.get_lock());
    return validate_user_stack_no_lock(space, vaddr);
}

void MemoryManager::register_kernel_region(Region& region)
{
    VERIFY(region.is_kernel());
    SpinlockLocker lock(s_mm_lock);
    m_kernel_regions.insert(region.vaddr().get(), region);
}

void MemoryManager::unregister_kernel_region(Region& region)
{
    VERIFY(region.is_kernel());
    SpinlockLocker lock(s_mm_lock);
    m_kernel_regions.remove(region.vaddr().get());
}

void MemoryManager::dump_kernel_regions()
{
    dbgln("Kernel regions:");
#if ARCH(I386)
    char const* addr_padding = "";
#else
    char const* addr_padding = "        ";
#endif
    dbgln("BEGIN{}         END{}        SIZE{}       ACCESS NAME",
        addr_padding, addr_padding, addr_padding);
    SpinlockLocker lock(s_mm_lock);
    for (auto const& region : m_kernel_regions) {
        dbgln("{:p} -- {:p} {:p} {:c}{:c}{:c}{:c}{:c}{:c} {}",
            region.vaddr().get(),
            region.vaddr().offset(region.size() - 1).get(),
            region.size(),
            region.is_readable() ? 'R' : ' ',
            region.is_writable() ? 'W' : ' ',
            region.is_executable() ? 'X' : ' ',
            region.is_shared() ? 'S' : ' ',
            region.is_stack() ? 'T' : ' ',
            region.is_syscall_region() ? 'C' : ' ',
            region.name());
    }
}

void MemoryManager::set_page_writable_direct(VirtualAddress vaddr, bool writable)
{
    SpinlockLocker page_lock(kernel_page_directory().get_lock());
    SpinlockLocker lock(s_mm_lock);
    auto* pte = ensure_pte(kernel_page_directory(), vaddr);
    VERIFY(pte);
    if (pte->is_writable() == writable)
        return;
    pte->set_writable(writable);
    flush_tlb(&kernel_page_directory(), vaddr);
}

CommittedPhysicalPageSet::~CommittedPhysicalPageSet()
{
    if (m_page_count)
        MM.uncommit_user_physical_pages({}, m_page_count);
}

NonnullRefPtr<PhysicalPage> CommittedPhysicalPageSet::take_one()
{
    VERIFY(m_page_count > 0);
    --m_page_count;
    return MM.allocate_committed_user_physical_page({}, MemoryManager::ShouldZeroFill::Yes);
}

void CommittedPhysicalPageSet::uncommit_one()
{
    VERIFY(m_page_count > 0);
    --m_page_count;
    MM.uncommit_user_physical_pages({}, 1);
}

void MemoryManager::copy_physical_page(PhysicalPage& physical_page, u8 page_buffer[PAGE_SIZE])
{
    SpinlockLocker locker(s_mm_lock);
    auto* quickmapped_page = quickmap_page(physical_page);
    memcpy(page_buffer, quickmapped_page, PAGE_SIZE);
    unquickmap_page();
}

}
