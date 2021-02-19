/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/CMOS.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Heap/kmalloc.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Process.h>
#include <Kernel/StdLib.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/ContiguousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/PhysicalRegion.h>
#include <Kernel/VM/SharedInodeVMObject.h>

extern u8* start_of_kernel_image;
extern u8* end_of_kernel_image;
extern FlatPtr start_of_kernel_text;
extern FlatPtr start_of_kernel_data;
extern FlatPtr end_of_kernel_bss;
extern FlatPtr start_of_ro_after_init;
extern FlatPtr end_of_ro_after_init;
extern FlatPtr start_of_unmap_after_init;
extern FlatPtr end_of_unmap_after_init;

extern multiboot_module_entry_t multiboot_copy_boot_modules_array[16];
extern size_t multiboot_copy_boot_modules_count;

// Treat the super pages as logically separate from .bss
__attribute__((section(".super_pages"))) static u8 super_pages[1 * MiB];

namespace Kernel {

// NOTE: We can NOT use AK::Singleton for this class, because
// MemoryManager::initialize is called *before* global constructors are
// run. If we do, then AK::Singleton would get re-initialized, causing
// the memory manager to be initialized twice!
static MemoryManager* s_the;
RecursiveSpinLock s_mm_lock;

const LogStream& operator<<(const LogStream& stream, const UsedMemoryRange& value)
{
    return stream << UserMemoryRangeTypeNames[static_cast<int>(value.type)] << " range @ " << value.start << " - " << value.end;
}

MemoryManager& MM
{
    return *s_the;
}

bool MemoryManager::is_initialized()
{
    return s_the != nullptr;
}

MemoryManager::MemoryManager()
{
    ScopedSpinLock lock(s_mm_lock);
    m_kernel_page_directory = PageDirectory::create_kernel_page_directory();
    parse_memory_map();
    write_cr3(kernel_page_directory().cr3());
    protect_kernel_image();

    // We're temporarily "committing" to two pages that we need to allocate below
    if (!commit_user_physical_pages(2))
        ASSERT_NOT_REACHED();

    m_shared_zero_page = allocate_committed_user_physical_page();

    // We're wasting a page here, we just need a special tag (physical
    // address) so that we know when we need to lazily allocate a page
    // that we should be drawing this page from the committed pool rather
    // than potentially failing if no pages are available anymore.
    // By using a tag we don't have to query the VMObject for every page
    // whether it was committed or not
    m_lazy_committed_page = allocate_committed_user_physical_page();
}

MemoryManager::~MemoryManager()
{
}

void MemoryManager::protect_kernel_image()
{
    ScopedSpinLock page_lock(kernel_page_directory().get_lock());
    // Disable writing to the kernel text and rodata segments.
    for (auto i = (FlatPtr)&start_of_kernel_text; i < (FlatPtr)&start_of_kernel_data; i += PAGE_SIZE) {
        auto& pte = *ensure_pte(kernel_page_directory(), VirtualAddress(i));
        pte.set_writable(false);
    }
    if (Processor::current().has_feature(CPUFeature::NX)) {
        // Disable execution of the kernel data, bss and heap segments.
        for (auto i = (FlatPtr)&start_of_kernel_data; i < (FlatPtr)&end_of_kernel_image; i += PAGE_SIZE) {
            auto& pte = *ensure_pte(kernel_page_directory(), VirtualAddress(i));
            pte.set_execute_disabled(true);
        }
    }
}

void MemoryManager::protect_readonly_after_init_memory()
{
    ScopedSpinLock mm_lock(s_mm_lock);
    ScopedSpinLock page_lock(kernel_page_directory().get_lock());
    // Disable writing to the .ro_after_init section
    for (auto i = (FlatPtr)&start_of_ro_after_init; i < (FlatPtr)&end_of_ro_after_init; i += PAGE_SIZE) {
        auto& pte = *ensure_pte(kernel_page_directory(), VirtualAddress(i));
        pte.set_writable(false);
        flush_tlb(&kernel_page_directory(), VirtualAddress(i));
    }
}

void MemoryManager::unmap_memory_after_init()
{
    ScopedSpinLock mm_lock(s_mm_lock);
    ScopedSpinLock page_lock(kernel_page_directory().get_lock());

    auto start = page_round_down((FlatPtr)&start_of_unmap_after_init);
    auto end = page_round_up((FlatPtr)&end_of_unmap_after_init);

    // Unmap the entire .unmap_after_init section
    for (auto i = start; i < end; i += PAGE_SIZE) {
        auto& pte = *ensure_pte(kernel_page_directory(), VirtualAddress(i));
        pte.clear();
        flush_tlb(&kernel_page_directory(), VirtualAddress(i));
    }

    dmesgln("Unmapped {} KiB of kernel text after init! :^)", (end - start) / KiB);
}

void MemoryManager::register_reserved_ranges()
{
    ASSERT(!m_physical_memory_ranges.is_empty());
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

bool MemoryManager::is_allowed_to_mmap_to_userspace(PhysicalAddress start_address, const Range& range) const
{
    ASSERT(!m_reserved_memory_ranges.is_empty());
    for (auto& current_range : m_reserved_memory_ranges) {
        if (!(current_range.start <= start_address))
            continue;
        if (!(current_range.start.offset(current_range.length) > start_address))
            continue;
        if (current_range.length < range.size())
            return false;
        return true;
    }
    return false;
}

void MemoryManager::parse_memory_map()
{
    RefPtr<PhysicalRegion> physical_region;

    // Register used memory regions that we know of.
    m_used_memory_ranges.ensure_capacity(4);
    m_used_memory_ranges.append(UsedMemoryRange { UsedMemoryRangeType::LowMemory, PhysicalAddress(0x00000000), PhysicalAddress(1 * MiB) });
    m_used_memory_ranges.append(UsedMemoryRange { UsedMemoryRangeType::Kernel, PhysicalAddress(virtual_to_low_physical(FlatPtr(&start_of_kernel_image))), PhysicalAddress(page_round_up(virtual_to_low_physical(FlatPtr(&end_of_kernel_image)))) });

    if (multiboot_info_ptr->flags & 0x4) {
        auto* bootmods_start = multiboot_copy_boot_modules_array;
        auto* bootmods_end = bootmods_start + multiboot_copy_boot_modules_count;

        for (auto* bootmod = bootmods_start; bootmod < bootmods_end; bootmod++) {
            m_used_memory_ranges.append(UsedMemoryRange { UsedMemoryRangeType::BootModule, PhysicalAddress(bootmod->start), PhysicalAddress(bootmod->end) });
        }
    }

    auto* mmap_begin = reinterpret_cast<multiboot_memory_map_t*>(low_physical_to_virtual(multiboot_info_ptr->mmap_addr));
    auto* mmap_end = reinterpret_cast<multiboot_memory_map_t*>(low_physical_to_virtual(multiboot_info_ptr->mmap_addr) + multiboot_info_ptr->mmap_length);

    for (auto& used_range : m_used_memory_ranges) {
        klog() << "MM: " << used_range;
    }

    for (auto* mmap = mmap_begin; mmap < mmap_end; mmap++) {
        dmesgln("MM: Multiboot mmap: address={:p}, length={}, type={}", mmap->addr, mmap->len, mmap->type);

        auto start_address = PhysicalAddress(mmap->addr);
        auto length = static_cast<size_t>(mmap->len);
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
            klog() << "MM: Warning, detected bad memory range!";
            m_physical_memory_ranges.append(PhysicalMemoryRange { PhysicalMemoryRangeType::BadMemory, start_address, length });
            break;
        default:
            dbgln("MM: Unknown range!");
            m_physical_memory_ranges.append(PhysicalMemoryRange { PhysicalMemoryRangeType::Unknown, start_address, length });
            break;
        }

        if (mmap->type != MULTIBOOT_MEMORY_AVAILABLE)
            continue;

        if ((mmap->addr + mmap->len) > 0xffffffff)
            continue;

        // Fix up unaligned memory regions.
        auto diff = (FlatPtr)mmap->addr % PAGE_SIZE;
        if (diff != 0) {
            dmesgln("MM: Got an unaligned physical_region from the bootloader; correcting {:p} by {} bytes", mmap->addr, diff);
            diff = PAGE_SIZE - diff;
            mmap->addr += diff;
            mmap->len -= diff;
        }
        if ((mmap->len % PAGE_SIZE) != 0) {
            dmesgln("MM: Got an unaligned physical_region from the bootloader; correcting length {} by {} bytes", mmap->len, mmap->len % PAGE_SIZE);
            mmap->len -= mmap->len % PAGE_SIZE;
        }
        if (mmap->len < PAGE_SIZE) {
            dmesgln("MM: Memory physical_region from bootloader is too small; we want >= {} bytes, but got {} bytes", PAGE_SIZE, mmap->len);
            continue;
        }

        for (size_t page_base = mmap->addr; page_base <= (mmap->addr + mmap->len); page_base += PAGE_SIZE) {
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

            // Assign page to user physical physical_region.
            if (physical_region.is_null() || physical_region->upper().offset(PAGE_SIZE) != addr) {
                m_user_physical_regions.append(PhysicalRegion::create(addr, addr));
                physical_region = m_user_physical_regions.last();
            } else {
                physical_region->expand(physical_region->lower(), addr);
            }
        }
    }

    // Append statically-allocated super physical physical_region.
    m_super_physical_regions.append(PhysicalRegion::create(
        PhysicalAddress(virtual_to_low_physical(FlatPtr(super_pages))),
        PhysicalAddress(virtual_to_low_physical(FlatPtr(super_pages + sizeof(super_pages))))));

    for (auto& region : m_super_physical_regions) {
        m_super_physical_pages += region.finalize_capacity();
        dmesgln("MM: Super physical region: {} - {}", region.lower(), region.upper());
    }

    for (auto& region : m_user_physical_regions) {
        m_user_physical_pages += region.finalize_capacity();
        dmesgln("MM: User physical region: {} - {}", region.lower(), region.upper());
    }

    ASSERT(m_super_physical_pages > 0);
    ASSERT(m_user_physical_pages > 0);

    // We start out with no committed pages
    m_user_physical_pages_uncommitted = m_user_physical_pages.load();
    register_reserved_ranges();
    for (auto& range : m_reserved_memory_ranges) {
        dmesgln("MM: Contiguous reserved range from {}, length is {}", range.start, range.length);
    }
}

PageTableEntry* MemoryManager::pte(PageDirectory& page_directory, VirtualAddress vaddr)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(s_mm_lock.own_lock());
    ASSERT(page_directory.get_lock().own_lock());
    u32 page_directory_table_index = (vaddr.get() >> 30) & 0x3;
    u32 page_directory_index = (vaddr.get() >> 21) & 0x1ff;
    u32 page_table_index = (vaddr.get() >> 12) & 0x1ff;

    auto* pd = quickmap_pd(const_cast<PageDirectory&>(page_directory), page_directory_table_index);
    const PageDirectoryEntry& pde = pd[page_directory_index];
    if (!pde.is_present())
        return nullptr;

    return &quickmap_pt(PhysicalAddress((FlatPtr)pde.page_table_base()))[page_table_index];
}

PageTableEntry* MemoryManager::ensure_pte(PageDirectory& page_directory, VirtualAddress vaddr)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(s_mm_lock.own_lock());
    ASSERT(page_directory.get_lock().own_lock());
    u32 page_directory_table_index = (vaddr.get() >> 30) & 0x3;
    u32 page_directory_index = (vaddr.get() >> 21) & 0x1ff;
    u32 page_table_index = (vaddr.get() >> 12) & 0x1ff;

    auto* pd = quickmap_pd(page_directory, page_directory_table_index);
    PageDirectoryEntry& pde = pd[page_directory_index];
    if (!pde.is_present()) {
        bool did_purge = false;
        auto page_table = allocate_user_physical_page(ShouldZeroFill::Yes, &did_purge);
        if (!page_table) {
            dbgln("MM: Unable to allocate page table to map {}", vaddr);
            return nullptr;
        }
        if (did_purge) {
            // If any memory had to be purged, ensure_pte may have been called as part
            // of the purging process. So we need to re-map the pd in this case to ensure
            // we're writing to the correct underlying physical page
            pd = quickmap_pd(page_directory, page_directory_table_index);
            ASSERT(&pde == &pd[page_directory_index]); // Sanity check

            ASSERT(!pde.is_present()); // Should have not changed
        }
        pde.set_page_table_base(page_table->paddr().get());
        pde.set_user_allowed(true);
        pde.set_present(true);
        pde.set_writable(true);
        pde.set_global(&page_directory == m_kernel_page_directory.ptr());
        // Use page_directory_table_index and page_directory_index as key
        // This allows us to release the page table entry when no longer needed
        auto result = page_directory.m_page_tables.set(vaddr.get() & ~0x1fffff, move(page_table));
        ASSERT(result == AK::HashSetResult::InsertedNewEntry);
    }

    return &quickmap_pt(PhysicalAddress((FlatPtr)pde.page_table_base()))[page_table_index];
}

void MemoryManager::release_pte(PageDirectory& page_directory, VirtualAddress vaddr, bool is_last_release)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(s_mm_lock.own_lock());
    ASSERT(page_directory.get_lock().own_lock());
    u32 page_directory_table_index = (vaddr.get() >> 30) & 0x3;
    u32 page_directory_index = (vaddr.get() >> 21) & 0x1ff;
    u32 page_table_index = (vaddr.get() >> 12) & 0x1ff;

    auto* pd = quickmap_pd(page_directory, page_directory_table_index);
    PageDirectoryEntry& pde = pd[page_directory_index];
    if (pde.is_present()) {
        auto* page_table = quickmap_pt(PhysicalAddress((FlatPtr)pde.page_table_base()));
        auto& pte = page_table[page_table_index];
        pte.clear();

        if (is_last_release || page_table_index == 0x1ff) {
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
                pde.clear();

                auto result = page_directory.m_page_tables.remove(vaddr.get() & ~0x1fffff);
                ASSERT(result);
            }
        }
    }
}

void MemoryManager::initialize(u32 cpu)
{
    auto mm_data = new MemoryManagerData;
    Processor::current().set_mm_data(*mm_data);

    if (cpu == 0) {
        s_the = new MemoryManager;
        kmalloc_enable_expand();
    }
}

Region* MemoryManager::kernel_region_from_vaddr(VirtualAddress vaddr)
{
    ScopedSpinLock lock(s_mm_lock);
    for (auto& region : MM.m_kernel_regions) {
        if (region.contains(vaddr))
            return &region;
    }
    return nullptr;
}

Region* MemoryManager::user_region_from_vaddr(Space& space, VirtualAddress vaddr)
{
    // FIXME: Use a binary search tree (maybe red/black?) or some other more appropriate data structure!
    ScopedSpinLock lock(space.get_lock());
    for (auto& region : space.regions()) {
        if (region.contains(vaddr))
            return &region;
    }
    return nullptr;
}

Region* MemoryManager::find_region_from_vaddr(Space& space, VirtualAddress vaddr)
{
    ScopedSpinLock lock(s_mm_lock);
    if (auto* region = user_region_from_vaddr(space, vaddr))
        return region;
    return kernel_region_from_vaddr(vaddr);
}

Region* MemoryManager::find_region_from_vaddr(VirtualAddress vaddr)
{
    ScopedSpinLock lock(s_mm_lock);
    if (auto* region = kernel_region_from_vaddr(vaddr))
        return region;
    auto page_directory = PageDirectory::find_by_cr3(read_cr3());
    if (!page_directory)
        return nullptr;
    ASSERT(page_directory->space());
    return user_region_from_vaddr(*page_directory->space(), vaddr);
}

PageFaultResponse MemoryManager::handle_page_fault(const PageFault& fault)
{
    ASSERT_INTERRUPTS_DISABLED();
    ScopedSpinLock lock(s_mm_lock);
    if (Processor::current().in_irq()) {
        dbgln("CPU[{}] BUG! Page fault while handling IRQ! code={}, vaddr={}, irq level: {}",
            Processor::id(), fault.code(), fault.vaddr(), Processor::current().in_irq());
        dump_kernel_regions();
        return PageFaultResponse::ShouldCrash;
    }
#if PAGE_FAULT_DEBUG
    dbgln("MM: CPU[{}] handle_page_fault({:#04x}) at {}", Processor::id(), fault.code(), fault.vaddr());
#endif
    auto* region = find_region_from_vaddr(fault.vaddr());
    if (!region) {
        dmesgln("CPU[{}] NP(error) fault at invalid address {}", Processor::id(), fault.vaddr());
        return PageFaultResponse::ShouldCrash;
    }

    return region->handle_fault(fault, lock);
}

OwnPtr<Region> MemoryManager::allocate_contiguous_kernel_region(size_t size, String name, u8 access, size_t physical_alignment, Region::Cacheable cacheable)
{
    ASSERT(!(size % PAGE_SIZE));
    ScopedSpinLock lock(s_mm_lock);
    auto range = kernel_page_directory().range_allocator().allocate_anywhere(size);
    if (!range.has_value())
        return {};
    auto vmobject = ContiguousVMObject::create_with_size(size, physical_alignment);
    return allocate_kernel_region_with_vmobject(range.value(), vmobject, move(name), access, cacheable);
}

OwnPtr<Region> MemoryManager::allocate_kernel_region(size_t size, String name, u8 access, AllocationStrategy strategy, Region::Cacheable cacheable)
{
    ASSERT(!(size % PAGE_SIZE));
    ScopedSpinLock lock(s_mm_lock);
    auto range = kernel_page_directory().range_allocator().allocate_anywhere(size);
    if (!range.has_value())
        return {};
    auto vmobject = AnonymousVMObject::create_with_size(size, strategy);
    if (!vmobject)
        return {};
    return allocate_kernel_region_with_vmobject(range.value(), vmobject.release_nonnull(), move(name), access, cacheable);
}

OwnPtr<Region> MemoryManager::allocate_kernel_region(PhysicalAddress paddr, size_t size, String name, u8 access, Region::Cacheable cacheable)
{
    ASSERT(!(size % PAGE_SIZE));
    ScopedSpinLock lock(s_mm_lock);
    auto range = kernel_page_directory().range_allocator().allocate_anywhere(size);
    if (!range.has_value())
        return {};
    auto vmobject = AnonymousVMObject::create_for_physical_range(paddr, size);
    if (!vmobject)
        return {};
    return allocate_kernel_region_with_vmobject(range.value(), *vmobject, move(name), access, cacheable);
}

OwnPtr<Region> MemoryManager::allocate_kernel_region_identity(PhysicalAddress paddr, size_t size, String name, u8 access, Region::Cacheable cacheable)
{
    ASSERT(!(size % PAGE_SIZE));
    ScopedSpinLock lock(s_mm_lock);
    auto range = kernel_page_directory().identity_range_allocator().allocate_specific(VirtualAddress(paddr.get()), size);
    if (!range.has_value())
        return {};
    auto vmobject = AnonymousVMObject::create_for_physical_range(paddr, size);
    if (!vmobject)
        return {};
    return allocate_kernel_region_with_vmobject(range.value(), *vmobject, move(name), access, cacheable);
}

OwnPtr<Region> MemoryManager::allocate_kernel_region_with_vmobject(const Range& range, VMObject& vmobject, String name, u8 access, Region::Cacheable cacheable)
{
    ScopedSpinLock lock(s_mm_lock);
    auto region = Region::create_kernel_only(range, vmobject, 0, move(name), access, cacheable);
    if (region)
        region->map(kernel_page_directory());
    return region;
}

OwnPtr<Region> MemoryManager::allocate_kernel_region_with_vmobject(VMObject& vmobject, size_t size, String name, u8 access, Region::Cacheable cacheable)
{
    ASSERT(!(size % PAGE_SIZE));
    ScopedSpinLock lock(s_mm_lock);
    auto range = kernel_page_directory().range_allocator().allocate_anywhere(size);
    if (!range.has_value())
        return {};
    return allocate_kernel_region_with_vmobject(range.value(), vmobject, move(name), access, cacheable);
}

bool MemoryManager::commit_user_physical_pages(size_t page_count)
{
    ASSERT(page_count > 0);
    ScopedSpinLock lock(s_mm_lock);
    if (m_user_physical_pages_uncommitted < page_count)
        return false;

    m_user_physical_pages_uncommitted -= page_count;
    m_user_physical_pages_committed += page_count;
    return true;
}

void MemoryManager::uncommit_user_physical_pages(size_t page_count)
{
    ASSERT(page_count > 0);
    ScopedSpinLock lock(s_mm_lock);
    ASSERT(m_user_physical_pages_committed >= page_count);

    m_user_physical_pages_uncommitted += page_count;
    m_user_physical_pages_committed -= page_count;
}

void MemoryManager::deallocate_user_physical_page(const PhysicalPage& page)
{
    ScopedSpinLock lock(s_mm_lock);
    for (auto& region : m_user_physical_regions) {
        if (!region.contains(page))
            continue;

        region.return_page(page);
        --m_user_physical_pages_used;

        // Always return pages to the uncommitted pool. Pages that were
        // committed and allocated are only freed upon request. Once
        // returned there is no guarantee being able to get them back.
        ++m_user_physical_pages_uncommitted;
        return;
    }

    dmesgln("MM: deallocate_user_physical_page couldn't figure out region for user page @ {}", page.paddr());
    ASSERT_NOT_REACHED();
}

RefPtr<PhysicalPage> MemoryManager::find_free_user_physical_page(bool committed)
{
    ASSERT(s_mm_lock.is_locked());
    RefPtr<PhysicalPage> page;
    if (committed) {
        // Draw from the committed pages pool. We should always have these pages available
        ASSERT(m_user_physical_pages_committed > 0);
        m_user_physical_pages_committed--;
    } else {
        // We need to make sure we don't touch pages that we have committed to
        if (m_user_physical_pages_uncommitted == 0)
            return {};
        m_user_physical_pages_uncommitted--;
    }
    for (auto& region : m_user_physical_regions) {
        page = region.take_free_page(false);
        if (!page.is_null()) {
            ++m_user_physical_pages_used;
            break;
        }
    }
    ASSERT(!committed || !page.is_null());
    return page;
}

NonnullRefPtr<PhysicalPage> MemoryManager::allocate_committed_user_physical_page(ShouldZeroFill should_zero_fill)
{
    ScopedSpinLock lock(s_mm_lock);
    auto page = find_free_user_physical_page(true);
    if (should_zero_fill == ShouldZeroFill::Yes) {
        auto* ptr = quickmap_page(*page);
        memset(ptr, 0, PAGE_SIZE);
        unquickmap_page();
    }
    return page.release_nonnull();
}

RefPtr<PhysicalPage> MemoryManager::allocate_user_physical_page(ShouldZeroFill should_zero_fill, bool* did_purge)
{
    ScopedSpinLock lock(s_mm_lock);
    auto page = find_free_user_physical_page(false);
    bool purged_pages = false;

    if (!page) {
        // We didn't have a single free physical page. Let's try to free something up!
        // First, we look for a purgeable VMObject in the volatile state.
        for_each_vmobject([&](auto& vmobject) {
            if (!vmobject.is_anonymous())
                return IterationDecision::Continue;
            int purged_page_count = static_cast<AnonymousVMObject&>(vmobject).purge_with_interrupts_disabled({});
            if (purged_page_count) {
                dbgln("MM: Purge saved the day! Purged {} pages from AnonymousVMObject", purged_page_count);
                page = find_free_user_physical_page(false);
                purged_pages = true;
                ASSERT(page);
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (!page) {
            dmesgln("MM: no user physical pages available");
            return {};
        }
    }

    if (should_zero_fill == ShouldZeroFill::Yes) {
        auto* ptr = quickmap_page(*page);
        memset(ptr, 0, PAGE_SIZE);
        unquickmap_page();
    }

    if (did_purge)
        *did_purge = purged_pages;
    return page;
}

void MemoryManager::deallocate_supervisor_physical_page(const PhysicalPage& page)
{
    ScopedSpinLock lock(s_mm_lock);
    for (auto& region : m_super_physical_regions) {
        if (!region.contains(page)) {
            dbgln("MM: deallocate_supervisor_physical_page: {} not in {} - {}", page.paddr(), region.lower(), region.upper());
            continue;
        }

        region.return_page(page);
        --m_super_physical_pages_used;
        return;
    }

    dbgln("MM: deallocate_supervisor_physical_page couldn't figure out region for super page @ {}", page.paddr());
    ASSERT_NOT_REACHED();
}

NonnullRefPtrVector<PhysicalPage> MemoryManager::allocate_contiguous_supervisor_physical_pages(size_t size, size_t physical_alignment)
{
    ASSERT(!(size % PAGE_SIZE));
    ScopedSpinLock lock(s_mm_lock);
    size_t count = ceil_div(size, PAGE_SIZE);
    NonnullRefPtrVector<PhysicalPage> physical_pages;

    for (auto& region : m_super_physical_regions) {
        physical_pages = region.take_contiguous_free_pages(count, true, physical_alignment);
        if (!physical_pages.is_empty())
            continue;
    }

    if (physical_pages.is_empty()) {
        if (m_super_physical_regions.is_empty()) {
            dmesgln("MM: no super physical regions available (?)");
        }

        dmesgln("MM: no super physical pages available");
        ASSERT_NOT_REACHED();
        return {};
    }

    auto cleanup_region = MM.allocate_kernel_region(physical_pages[0].paddr(), PAGE_SIZE * count, "MemoryManager Allocation Sanitization", Region::Access::Read | Region::Access::Write);
    fast_u32_fill((u32*)cleanup_region->vaddr().as_ptr(), 0, (PAGE_SIZE * count) / sizeof(u32));
    m_super_physical_pages_used += count;
    return physical_pages;
}

RefPtr<PhysicalPage> MemoryManager::allocate_supervisor_physical_page()
{
    ScopedSpinLock lock(s_mm_lock);
    RefPtr<PhysicalPage> page;

    for (auto& region : m_super_physical_regions) {
        page = region.take_free_page(true);
        if (!page.is_null())
            break;
    }

    if (!page) {
        if (m_super_physical_regions.is_empty()) {
            dmesgln("MM: no super physical regions available (?)");
        }

        dmesgln("MM: no super physical pages available");
        ASSERT_NOT_REACHED();
        return {};
    }

    fast_u32_fill((u32*)page->paddr().offset(0xc0000000).as_ptr(), 0, PAGE_SIZE / sizeof(u32));
    ++m_super_physical_pages_used;
    return page;
}

void MemoryManager::enter_process_paging_scope(Process& process)
{
    enter_space(process.space());
}

void MemoryManager::enter_space(Space& space)
{
    auto current_thread = Thread::current();
    ASSERT(current_thread != nullptr);
    ScopedSpinLock lock(s_mm_lock);

    current_thread->tss().cr3 = space.page_directory().cr3();
    write_cr3(space.page_directory().cr3());
}

void MemoryManager::flush_tlb_local(VirtualAddress vaddr, size_t page_count)
{
    Processor::flush_tlb_local(vaddr, page_count);
}

void MemoryManager::flush_tlb(const PageDirectory* page_directory, VirtualAddress vaddr, size_t page_count)
{
    Processor::flush_tlb(page_directory, vaddr, page_count);
}

extern "C" PageTableEntry boot_pd3_pt1023[1024];

PageDirectoryEntry* MemoryManager::quickmap_pd(PageDirectory& directory, size_t pdpt_index)
{
    ASSERT(s_mm_lock.own_lock());
    auto& mm_data = get_data();
    auto& pte = boot_pd3_pt1023[4];
    auto pd_paddr = directory.m_directory_pages[pdpt_index]->paddr();
    if (pte.physical_page_base() != pd_paddr.as_ptr()) {
        pte.set_physical_page_base(pd_paddr.get());
        pte.set_present(true);
        pte.set_writable(true);
        pte.set_user_allowed(false);
        // Because we must continue to hold the MM lock while we use this
        // mapping, it is sufficient to only flush on the current CPU. Other
        // CPUs trying to use this API must wait on the MM lock anyway
        flush_tlb_local(VirtualAddress(0xffe04000));
    } else {
        // Even though we don't allow this to be called concurrently, it's
        // possible that this PD was mapped on a different CPU and we don't
        // broadcast the flush. If so, we still need to flush the TLB.
        if (mm_data.m_last_quickmap_pd != pd_paddr)
            flush_tlb_local(VirtualAddress(0xffe04000));
    }
    mm_data.m_last_quickmap_pd = pd_paddr;
    return (PageDirectoryEntry*)0xffe04000;
}

PageTableEntry* MemoryManager::quickmap_pt(PhysicalAddress pt_paddr)
{
    ASSERT(s_mm_lock.own_lock());
    auto& mm_data = get_data();
    auto& pte = boot_pd3_pt1023[0];
    if (pte.physical_page_base() != pt_paddr.as_ptr()) {
        pte.set_physical_page_base(pt_paddr.get());
        pte.set_present(true);
        pte.set_writable(true);
        pte.set_user_allowed(false);
        // Because we must continue to hold the MM lock while we use this
        // mapping, it is sufficient to only flush on the current CPU. Other
        // CPUs trying to use this API must wait on the MM lock anyway
        flush_tlb_local(VirtualAddress(0xffe00000));
    } else {
        // Even though we don't allow this to be called concurrently, it's
        // possible that this PT was mapped on a different CPU and we don't
        // broadcast the flush. If so, we still need to flush the TLB.
        if (mm_data.m_last_quickmap_pt != pt_paddr)
            flush_tlb_local(VirtualAddress(0xffe00000));
    }
    mm_data.m_last_quickmap_pt = pt_paddr;
    return (PageTableEntry*)0xffe00000;
}

u8* MemoryManager::quickmap_page(PhysicalPage& physical_page)
{
    ASSERT_INTERRUPTS_DISABLED();
    auto& mm_data = get_data();
    mm_data.m_quickmap_prev_flags = mm_data.m_quickmap_in_use.lock();
    ScopedSpinLock lock(s_mm_lock);

    u32 pte_idx = 8 + Processor::id();
    VirtualAddress vaddr(0xffe00000 + pte_idx * PAGE_SIZE);

    auto& pte = boot_pd3_pt1023[pte_idx];
    if (pte.physical_page_base() != physical_page.paddr().as_ptr()) {
        pte.set_physical_page_base(physical_page.paddr().get());
        pte.set_present(true);
        pte.set_writable(true);
        pte.set_user_allowed(false);
        flush_tlb_local(vaddr);
    }
    return vaddr.as_ptr();
}

void MemoryManager::unquickmap_page()
{
    ASSERT_INTERRUPTS_DISABLED();
    ScopedSpinLock lock(s_mm_lock);
    auto& mm_data = get_data();
    ASSERT(mm_data.m_quickmap_in_use.is_locked());
    u32 pte_idx = 8 + Processor::id();
    VirtualAddress vaddr(0xffe00000 + pte_idx * PAGE_SIZE);
    auto& pte = boot_pd3_pt1023[pte_idx];
    pte.clear();
    flush_tlb_local(vaddr);
    mm_data.m_quickmap_in_use.unlock(mm_data.m_quickmap_prev_flags);
}

bool MemoryManager::validate_user_stack(const Process& process, VirtualAddress vaddr) const
{
    if (!is_user_address(vaddr))
        return false;
    ScopedSpinLock lock(s_mm_lock);
    auto* region = user_region_from_vaddr(const_cast<Process&>(process).space(), vaddr);
    return region && region->is_user() && region->is_stack();
}

void MemoryManager::register_vmobject(VMObject& vmobject)
{
    ScopedSpinLock lock(s_mm_lock);
    m_vmobjects.append(&vmobject);
}

void MemoryManager::unregister_vmobject(VMObject& vmobject)
{
    ScopedSpinLock lock(s_mm_lock);
    m_vmobjects.remove(&vmobject);
}

void MemoryManager::register_region(Region& region)
{
    ScopedSpinLock lock(s_mm_lock);
    if (region.is_kernel())
        m_kernel_regions.append(&region);
    else
        m_user_regions.append(&region);
}

void MemoryManager::unregister_region(Region& region)
{
    ScopedSpinLock lock(s_mm_lock);
    if (region.is_kernel())
        m_kernel_regions.remove(&region);
    else
        m_user_regions.remove(&region);
}

void MemoryManager::dump_kernel_regions()
{
    dbgln("Kernel regions:");
    dbgln("BEGIN       END         SIZE        ACCESS  NAME");
    ScopedSpinLock lock(s_mm_lock);
    for (auto& region : m_kernel_regions) {
        dbgln("{:08x} -- {:08x} {:08x} {:c}{:c}{:c}{:c}{:c}{:c} {}",
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

}
