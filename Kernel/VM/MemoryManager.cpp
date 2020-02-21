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

#include "CMOS.h"
#include "Process.h"
#include <AK/Assertions.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Multiboot.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/InodeVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/PhysicalRegion.h>
#include <Kernel/VM/PurgeableVMObject.h>
#include <LibBareMetal/StdLib.h>

//#define MM_DEBUG
//#define PAGE_FAULT_DEBUG

extern uintptr_t start_of_kernel_text;
extern uintptr_t start_of_kernel_data;
extern uintptr_t end_of_kernel_bss;

namespace Kernel {

static MemoryManager* s_the;

MemoryManager& MM
{
    return *s_the;
}

MemoryManager::MemoryManager()
{
    m_kernel_page_directory = PageDirectory::create_kernel_page_directory();
    parse_memory_map();
    write_cr3(kernel_page_directory().cr3());
    setup_low_identity_mapping();
    protect_kernel_image();

    m_shared_zero_page = allocate_user_physical_page();
}

MemoryManager::~MemoryManager()
{
}

void MemoryManager::protect_kernel_image()
{
    // Disable writing to the kernel text and rodata segments.
    for (size_t i = (uintptr_t)&start_of_kernel_text; i < (uintptr_t)&start_of_kernel_data; i += PAGE_SIZE) {
        auto& pte = ensure_pte(kernel_page_directory(), VirtualAddress(i));
        pte.set_writable(false);
    }

    if (g_cpu_supports_nx) {
        // Disable execution of the kernel data and bss segments.
        for (size_t i = (uintptr_t)&start_of_kernel_data; i < (uintptr_t)&end_of_kernel_bss; i += PAGE_SIZE) {
            auto& pte = ensure_pte(kernel_page_directory(), VirtualAddress(i));
            pte.set_execute_disabled(true);
        }
    }
}

void MemoryManager::setup_low_identity_mapping()
{
    m_low_page_table = allocate_user_physical_page(ShouldZeroFill::Yes);

    auto* pd_zero = quickmap_pd(kernel_page_directory(), 0);
    pd_zero[1].set_present(false);
    pd_zero[2].set_present(false);
    pd_zero[3].set_present(false);

    auto& pde_zero = pd_zero[0];
    pde_zero.set_page_table_base(m_low_page_table->paddr().get());
    pde_zero.set_present(true);
    pde_zero.set_huge(false);
    pde_zero.set_writable(true);
    pde_zero.set_user_allowed(false);
    if (g_cpu_supports_nx)
        pde_zero.set_execute_disabled(true);

    for (uintptr_t offset = (1 * MB); offset < (2 * MB); offset += PAGE_SIZE) {
        auto& page_table_page = m_low_page_table;
        auto& pte = quickmap_pt(page_table_page->paddr())[offset / PAGE_SIZE];
        pte.set_physical_page_base(offset);
        pte.set_user_allowed(false);
        pte.set_present(offset != 0);
        pte.set_writable(offset < (1 * MB));
    }
}

void MemoryManager::parse_memory_map()
{
    RefPtr<PhysicalRegion> region;
    bool region_is_super = false;

    auto* mmap = (multiboot_memory_map_t*)(low_physical_to_virtual(multiboot_info_ptr->mmap_addr));
    for (; (unsigned long)mmap < (low_physical_to_virtual(multiboot_info_ptr->mmap_addr)) + (multiboot_info_ptr->mmap_length); mmap = (multiboot_memory_map_t*)((unsigned long)mmap + mmap->size + sizeof(mmap->size))) {
        kprintf("MM: Multiboot mmap: base_addr = 0x%x%08x, length = 0x%x%08x, type = 0x%x\n",
            (uintptr_t)(mmap->addr >> 32),
            (uintptr_t)(mmap->addr & 0xffffffff),
            (uintptr_t)(mmap->len >> 32),
            (uintptr_t)(mmap->len & 0xffffffff),
            (uintptr_t)mmap->type);

        if (mmap->type != MULTIBOOT_MEMORY_AVAILABLE)
            continue;

        // FIXME: Maybe make use of stuff below the 1MB mark?
        if (mmap->addr < (1 * MB))
            continue;

        if ((mmap->addr + mmap->len) > 0xffffffff)
            continue;

        auto diff = (uintptr_t)mmap->addr % PAGE_SIZE;
        if (diff != 0) {
            kprintf("MM: got an unaligned region base from the bootloader; correcting %p by %d bytes\n", mmap->addr, diff);
            diff = PAGE_SIZE - diff;
            mmap->addr += diff;
            mmap->len -= diff;
        }
        if ((mmap->len % PAGE_SIZE) != 0) {
            kprintf("MM: got an unaligned region length from the bootloader; correcting %d by %d bytes\n", mmap->len, mmap->len % PAGE_SIZE);
            mmap->len -= mmap->len % PAGE_SIZE;
        }
        if (mmap->len < PAGE_SIZE) {
            kprintf("MM: memory region from bootloader is too small; we want >= %d bytes, but got %d bytes\n", PAGE_SIZE, mmap->len);
            continue;
        }

#ifdef MM_DEBUG
        kprintf("MM: considering memory at %p - %p\n",
            (uintptr_t)mmap->addr, (uintptr_t)(mmap->addr + mmap->len));
#endif

        for (size_t page_base = mmap->addr; page_base < (mmap->addr + mmap->len); page_base += PAGE_SIZE) {
            auto addr = PhysicalAddress(page_base);

            if (page_base < 7 * MB) {
                // nothing
            } else if (page_base >= 7 * MB && page_base < 8 * MB) {
                if (region.is_null() || !region_is_super || region->upper().offset(PAGE_SIZE) != addr) {
                    m_super_physical_regions.append(PhysicalRegion::create(addr, addr));
                    region = m_super_physical_regions.last();
                    region_is_super = true;
                } else {
                    region->expand(region->lower(), addr);
                }
            } else {
                if (region.is_null() || region_is_super || region->upper().offset(PAGE_SIZE) != addr) {
                    m_user_physical_regions.append(PhysicalRegion::create(addr, addr));
                    region = m_user_physical_regions.last();
                    region_is_super = false;
                } else {
                    region->expand(region->lower(), addr);
                }
            }
        }
    }

    for (auto& region : m_super_physical_regions)
        m_super_physical_pages += region.finalize_capacity();

    for (auto& region : m_user_physical_regions)
        m_user_physical_pages += region.finalize_capacity();
}

const PageTableEntry* MemoryManager::pte(const PageDirectory& page_directory, VirtualAddress vaddr)
{
    ASSERT_INTERRUPTS_DISABLED();
    u32 page_directory_table_index = (vaddr.get() >> 30) & 0x3;
    u32 page_directory_index = (vaddr.get() >> 21) & 0x1ff;
    u32 page_table_index = (vaddr.get() >> 12) & 0x1ff;

    auto* pd = quickmap_pd(const_cast<PageDirectory&>(page_directory), page_directory_table_index);
    const PageDirectoryEntry& pde = pd[page_directory_index];
    if (!pde.is_present())
        return nullptr;

    return &quickmap_pt(PhysicalAddress((uintptr_t)pde.page_table_base()))[page_table_index];
}

PageTableEntry& MemoryManager::ensure_pte(PageDirectory& page_directory, VirtualAddress vaddr)
{
    ASSERT_INTERRUPTS_DISABLED();
    u32 page_directory_table_index = (vaddr.get() >> 30) & 0x3;
    u32 page_directory_index = (vaddr.get() >> 21) & 0x1ff;
    u32 page_table_index = (vaddr.get() >> 12) & 0x1ff;

    auto* pd = quickmap_pd(page_directory, page_directory_table_index);
    PageDirectoryEntry& pde = pd[page_directory_index];
    if (!pde.is_present()) {
#ifdef MM_DEBUG
        dbgprintf("MM: PDE %u not present (requested for V%p), allocating\n", page_directory_index, vaddr.get());
#endif
        auto page_table = allocate_user_physical_page(ShouldZeroFill::Yes);
#ifdef MM_DEBUG
        dbgprintf("MM: PD K%p (%s) at P%p allocated page table #%u (for V%p) at P%p\n",
            &page_directory,
            &page_directory == m_kernel_page_directory ? "Kernel" : "User",
            page_directory.cr3(),
            page_directory_index,
            vaddr.get(),
            page_table->paddr().get());
#endif
        pde.set_page_table_base(page_table->paddr().get());
        pde.set_user_allowed(true);
        pde.set_present(true);
        pde.set_writable(true);
        pde.set_global(&page_directory == m_kernel_page_directory.ptr());
        page_directory.m_physical_pages.set(page_directory_index, move(page_table));
    }

    return quickmap_pt(PhysicalAddress((uintptr_t)pde.page_table_base()))[page_table_index];
}

void MemoryManager::initialize()
{
    s_the = new MemoryManager;
}

Region* MemoryManager::kernel_region_from_vaddr(VirtualAddress vaddr)
{
    if (vaddr.get() < 0xc0000000)
        return nullptr;
    for (auto& region : MM.m_kernel_regions) {
        if (region.contains(vaddr))
            return &region;
    }
    return nullptr;
}

Region* MemoryManager::user_region_from_vaddr(Process& process, VirtualAddress vaddr)
{
    // FIXME: Use a binary search tree (maybe red/black?) or some other more appropriate data structure!
    for (auto& region : process.m_regions) {
        if (region.contains(vaddr))
            return &region;
    }
    dbg() << process << " Couldn't find user region for " << vaddr;
    return nullptr;
}

Region* MemoryManager::region_from_vaddr(Process& process, VirtualAddress vaddr)
{
    if (auto* region = kernel_region_from_vaddr(vaddr))
        return region;
    return user_region_from_vaddr(process, vaddr);
}

const Region* MemoryManager::region_from_vaddr(const Process& process, VirtualAddress vaddr)
{
    if (auto* region = kernel_region_from_vaddr(vaddr))
        return region;
    return user_region_from_vaddr(const_cast<Process&>(process), vaddr);
}

Region* MemoryManager::region_from_vaddr(VirtualAddress vaddr)
{
    if (auto* region = kernel_region_from_vaddr(vaddr))
        return region;
    auto page_directory = PageDirectory::find_by_cr3(read_cr3());
    if (!page_directory)
        return nullptr;
    ASSERT(page_directory->process());
    return user_region_from_vaddr(*page_directory->process(), vaddr);
}

PageFaultResponse MemoryManager::handle_page_fault(const PageFault& fault)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(Thread::current);
    ASSERT(!g_in_irq);
#ifdef PAGE_FAULT_DEBUG
    dbgprintf("MM: handle_page_fault(%w) at V%p\n", fault.code(), fault.vaddr().get());
#endif
    auto* region = region_from_vaddr(fault.vaddr());
    if (!region) {
        kprintf("NP(error) fault at invalid address V%p\n", fault.vaddr().get());
        return PageFaultResponse::ShouldCrash;
    }

    return region->handle_fault(fault);
}

OwnPtr<Region> MemoryManager::allocate_kernel_region(size_t size, const StringView& name, u8 access, bool user_accessible, bool should_commit, bool cacheable)
{
    InterruptDisabler disabler;
    ASSERT(!(size % PAGE_SIZE));
    auto range = kernel_page_directory().range_allocator().allocate_anywhere(size);
    ASSERT(range.is_valid());
    OwnPtr<Region> region;
    if (user_accessible)
        region = Region::create_user_accessible(range, name, access, cacheable);
    else
        region = Region::create_kernel_only(range, name, access, cacheable);
    region->map(kernel_page_directory());
    if (should_commit)
        region->commit();
    return region;
}

OwnPtr<Region> MemoryManager::allocate_kernel_region(PhysicalAddress paddr, size_t size, const StringView& name, u8 access, bool user_accessible, bool cacheable)
{
    InterruptDisabler disabler;
    ASSERT(!(size % PAGE_SIZE));
    auto range = kernel_page_directory().range_allocator().allocate_anywhere(size);
    ASSERT(range.is_valid());
    auto vmobject = AnonymousVMObject::create_for_physical_range(paddr, size);
    if (!vmobject)
        return nullptr;
    OwnPtr<Region> region;
    if (user_accessible)
        region = Region::create_user_accessible(range, vmobject.release_nonnull(), 0, name, access, cacheable);
    else
        region = Region::create_kernel_only(range, vmobject.release_nonnull(), 0, name, access, cacheable);
    region->map(kernel_page_directory());
    return region;
}

OwnPtr<Region> MemoryManager::allocate_user_accessible_kernel_region(size_t size, const StringView& name, u8 access, bool cacheable)
{
    return allocate_kernel_region(size, name, access, true, true, cacheable);
}

OwnPtr<Region> MemoryManager::allocate_kernel_region_with_vmobject(VMObject& vmobject, size_t size, const StringView& name, u8 access, bool user_accessible, bool cacheable)
{
    InterruptDisabler disabler;
    ASSERT(!(size % PAGE_SIZE));
    auto range = kernel_page_directory().range_allocator().allocate_anywhere(size);
    ASSERT(range.is_valid());
    OwnPtr<Region> region;
    if (user_accessible)
        region = Region::create_user_accessible(range, vmobject, 0, name, access, cacheable);
    else
        region = Region::create_kernel_only(range, vmobject, 0, name, access, cacheable);
    region->map(kernel_page_directory());
    return region;
}

void MemoryManager::deallocate_user_physical_page(PhysicalPage&& page)
{
    for (auto& region : m_user_physical_regions) {
        if (!region.contains(page)) {
            kprintf(
                "MM: deallocate_user_physical_page: %p not in %p -> %p\n",
                page.paddr().get(), region.lower().get(), region.upper().get());
            continue;
        }

        region.return_page(move(page));
        --m_user_physical_pages_used;

        return;
    }

    kprintf("MM: deallocate_user_physical_page couldn't figure out region for user page @ %p\n", page.paddr().get());
    ASSERT_NOT_REACHED();
}

RefPtr<PhysicalPage> MemoryManager::find_free_user_physical_page()
{
    RefPtr<PhysicalPage> page;
    for (auto& region : m_user_physical_regions) {
        page = region.take_free_page(false);
        if (!page.is_null())
            break;
    }
    return page;
}

RefPtr<PhysicalPage> MemoryManager::allocate_user_physical_page(ShouldZeroFill should_zero_fill)
{
    InterruptDisabler disabler;
    RefPtr<PhysicalPage> page = find_free_user_physical_page();

    if (!page) {
        if (m_user_physical_regions.is_empty()) {
            kprintf("MM: no user physical regions available (?)\n");
        }

        for_each_vmobject([&](auto& vmobject) {
            if (vmobject.is_purgeable()) {
                auto& purgeable_vmobject = static_cast<PurgeableVMObject&>(vmobject);
                int purged_page_count = purgeable_vmobject.purge_with_interrupts_disabled({});
                if (purged_page_count) {
                    kprintf("MM: Purge saved the day! Purged %d pages from PurgeableVMObject{%p}\n", purged_page_count, &purgeable_vmobject);
                    page = find_free_user_physical_page();
                    ASSERT(page);
                    return IterationDecision::Break;
                }
            }
            return IterationDecision::Continue;
        });

        if (!page) {
            kprintf("MM: no user physical pages available\n");
            ASSERT_NOT_REACHED();
            return {};
        }
    }

#ifdef MM_DEBUG
    dbgprintf("MM: allocate_user_physical_page vending P%p\n", page->paddr().get());
#endif

    if (should_zero_fill == ShouldZeroFill::Yes) {
        auto* ptr = quickmap_page(*page);
        memset(ptr, 0, PAGE_SIZE);
        unquickmap_page();
    }

    ++m_user_physical_pages_used;
    return page;
}

void MemoryManager::deallocate_supervisor_physical_page(PhysicalPage&& page)
{
    for (auto& region : m_super_physical_regions) {
        if (!region.contains(page)) {
            kprintf(
                "MM: deallocate_supervisor_physical_page: %p not in %p -> %p\n",
                page.paddr().get(), region.lower().get(), region.upper().get());
            continue;
        }

        region.return_page(move(page));
        --m_super_physical_pages_used;
        return;
    }

    kprintf("MM: deallocate_supervisor_physical_page couldn't figure out region for super page @ %p\n", page.paddr().get());
    ASSERT_NOT_REACHED();
}

RefPtr<PhysicalPage> MemoryManager::allocate_supervisor_physical_page()
{
    InterruptDisabler disabler;
    RefPtr<PhysicalPage> page;

    for (auto& region : m_super_physical_regions) {
        page = region.take_free_page(true);
        if (page.is_null())
            continue;
    }

    if (!page) {
        if (m_super_physical_regions.is_empty()) {
            kprintf("MM: no super physical regions available (?)\n");
        }

        kprintf("MM: no super physical pages available\n");
        ASSERT_NOT_REACHED();
        return {};
    }

#ifdef MM_DEBUG
    dbgprintf("MM: allocate_supervisor_physical_page vending P%p\n", page->paddr().get());
#endif

    fast_u32_fill((u32*)page->paddr().offset(0xc0000000).as_ptr(), 0, PAGE_SIZE / sizeof(u32));
    ++m_super_physical_pages_used;
    return page;
}

void MemoryManager::enter_process_paging_scope(Process& process)
{
    ASSERT(Thread::current);
    InterruptDisabler disabler;

    Thread::current->tss().cr3 = process.page_directory().cr3();
    write_cr3(process.page_directory().cr3());
}

void MemoryManager::flush_entire_tlb()
{
    write_cr3(read_cr3());
}

void MemoryManager::flush_tlb(VirtualAddress vaddr)
{
#ifdef MM_DEBUG
    dbgprintf("MM: Flush page V%p\n", vaddr.get());
#endif
    asm volatile("invlpg %0"
                 :
                 : "m"(*(char*)vaddr.get())
                 : "memory");
}

extern "C" PageTableEntry boot_pd3_pde1023_pt[1024];

PageDirectoryEntry* MemoryManager::quickmap_pd(PageDirectory& directory, size_t pdpt_index)
{
    auto& pte = boot_pd3_pde1023_pt[4];
    auto pd_paddr = directory.m_directory_pages[pdpt_index]->paddr();
    if (pte.physical_page_base() != pd_paddr.as_ptr()) {
#ifdef MM_DEBUG
        dbgprintf("quickmap_pd: Mapping P%p at 0xffe04000 in pte @ %p\n", directory.m_directory_pages[pdpt_index]->paddr().as_ptr(), &pte);
#endif
        pte.set_physical_page_base(pd_paddr.get());
        pte.set_present(true);
        pte.set_writable(true);
        pte.set_user_allowed(false);
        flush_tlb(VirtualAddress(0xffe04000));
    }
    return (PageDirectoryEntry*)0xffe04000;
}

PageTableEntry* MemoryManager::quickmap_pt(PhysicalAddress pt_paddr)
{
    auto& pte = boot_pd3_pde1023_pt[8];
    if (pte.physical_page_base() != pt_paddr.as_ptr()) {
#ifdef MM_DEBUG
        dbgprintf("quickmap_pt: Mapping P%p at 0xffe08000 in pte @ %p\n", pt_paddr.as_ptr(), &pte);
#endif
        pte.set_physical_page_base(pt_paddr.get());
        pte.set_present(true);
        pte.set_writable(true);
        pte.set_user_allowed(false);
        flush_tlb(VirtualAddress(0xffe08000));
    }
    return (PageTableEntry*)0xffe08000;
}

u8* MemoryManager::quickmap_page(PhysicalPage& physical_page)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(!m_quickmap_in_use);
    m_quickmap_in_use = true;

    auto& pte = boot_pd3_pde1023_pt[0];
    if (pte.physical_page_base() != physical_page.paddr().as_ptr()) {
#ifdef MM_DEBUG
        dbgprintf("quickmap_page: Mapping P%p at 0xffe00000 in pte @ %p\n", physical_page.paddr().as_ptr(), &pte);
#endif
        pte.set_physical_page_base(physical_page.paddr().get());
        pte.set_present(true);
        pte.set_writable(true);
        pte.set_user_allowed(false);
        flush_tlb(VirtualAddress(0xffe00000));
    }
    return (u8*)0xffe00000;
}

void MemoryManager::unquickmap_page()
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(m_quickmap_in_use);
    auto& pte = boot_pd3_pde1023_pt[0];
    pte.clear();
    flush_tlb(VirtualAddress(0xffe00000));
    m_quickmap_in_use = false;
}

template<MemoryManager::AccessSpace space, MemoryManager::AccessType access_type>
bool MemoryManager::validate_range(const Process& process, VirtualAddress base_vaddr, size_t size) const
{
    ASSERT(size);
    if (base_vaddr > base_vaddr.offset(size)) {
        dbg() << "Shenanigans! Asked to validate wrappy " << base_vaddr << " size=" << size;
        return false;
    }

    VirtualAddress vaddr = base_vaddr.page_base();
    VirtualAddress end_vaddr = base_vaddr.offset(size - 1).page_base();
    if (end_vaddr < vaddr) {
        dbg() << "Shenanigans! Asked to validate " << base_vaddr << " size=" << size;
        return false;
    }
    const Region* region = nullptr;
    while (vaddr <= end_vaddr) {
        if (!region || !region->contains(vaddr)) {
            if (space == AccessSpace::Kernel)
                region = kernel_region_from_vaddr(vaddr);
            if (!region || !region->contains(vaddr))
                region = user_region_from_vaddr(const_cast<Process&>(process), vaddr);
            if (!region
                || (space == AccessSpace::User && !region->is_user_accessible())
                || (access_type == AccessType::Read && !region->is_readable())
                || (access_type == AccessType::Write && !region->is_writable())) {
                return false;
            }
        }
        vaddr = vaddr.offset(PAGE_SIZE);
    }
    return true;
}

bool MemoryManager::validate_user_stack(const Process& process, VirtualAddress vaddr) const
{
    if (!is_user_address(vaddr))
        return false;
    auto* region = user_region_from_vaddr(const_cast<Process&>(process), vaddr);
    return region && region->is_user_accessible() && region->is_stack();
}

bool MemoryManager::validate_kernel_read(const Process& process, VirtualAddress vaddr, size_t size) const
{
    return validate_range<AccessSpace::Kernel, AccessType::Read>(process, vaddr, size);
}

bool MemoryManager::can_read_without_faulting(const Process& process, VirtualAddress vaddr, size_t size) const
{
    // FIXME: Use the size argument!
    UNUSED_PARAM(size);
    auto* pte = const_cast<MemoryManager*>(this)->pte(process.page_directory(), vaddr);
    if (!pte)
        return false;
    return pte->is_present();
}


bool MemoryManager::validate_user_read(const Process& process, VirtualAddress vaddr, size_t size) const
{
    if (!is_user_address(vaddr))
        return false;
    return validate_range<AccessSpace::User, AccessType::Read>(process, vaddr, size);
}

bool MemoryManager::validate_user_write(const Process& process, VirtualAddress vaddr, size_t size) const
{
    if (!is_user_address(vaddr))
        return false;
    return validate_range<AccessSpace::User, AccessType::Write>(process, vaddr, size);
}

void MemoryManager::register_vmobject(VMObject& vmobject)
{
    InterruptDisabler disabler;
    m_vmobjects.append(&vmobject);
}

void MemoryManager::unregister_vmobject(VMObject& vmobject)
{
    InterruptDisabler disabler;
    m_vmobjects.remove(&vmobject);
}

void MemoryManager::register_region(Region& region)
{
    InterruptDisabler disabler;
    if (region.vaddr().get() >= 0xc0000000)
        m_kernel_regions.append(&region);
    else
        m_user_regions.append(&region);
}

void MemoryManager::unregister_region(Region& region)
{
    InterruptDisabler disabler;
    if (region.vaddr().get() >= 0xc0000000)
        m_kernel_regions.remove(&region);
    else
        m_user_regions.remove(&region);
}

void MemoryManager::dump_kernel_regions()
{
    kprintf("Kernel regions:\n");
    kprintf("BEGIN       END         SIZE        ACCESS  NAME\n");
    for (auto& region : MM.m_kernel_regions) {
        kprintf("%08x -- %08x    %08x    %c%c%c%c%c%c    %s\n",
            region.vaddr().get(),
            region.vaddr().offset(region.size() - 1).get(),
            region.size(),
            region.is_readable() ? 'R' : ' ',
            region.is_writable() ? 'W' : ' ',
            region.is_executable() ? 'X' : ' ',
            region.is_shared() ? 'S' : ' ',
            region.is_stack() ? 'T' : ' ',
            region.vmobject().is_purgeable() ? 'P' : ' ',
            region.name().characters());
    }
}

ProcessPagingScope::ProcessPagingScope(Process& process)
{
    ASSERT(Thread::current);
    m_previous_cr3 = read_cr3();
    MM.enter_process_paging_scope(process);
}

ProcessPagingScope::~ProcessPagingScope()
{
    InterruptDisabler disabler;
    Thread::current->tss().cr3 = m_previous_cr3;
    write_cr3(m_previous_cr3);
}

}
