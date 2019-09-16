#include "CMOS.h"
#include "Process.h"
#include "StdLib.h"
#include <AK/Assertions.h>
#include <AK/kstdio.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Multiboot.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/InodeVMObject.h>
#include <Kernel/VM/MemoryManager.h>

//#define MM_DEBUG
//#define PAGE_FAULT_DEBUG

static MemoryManager* s_the;

MemoryManager& MM
{
    return *s_the;
}

MemoryManager::MemoryManager()
{
    m_kernel_page_directory = PageDirectory::create_at_fixed_address(PhysicalAddress(0x4000));
    m_page_table_zero = (PageTableEntry*)0x6000;
    m_page_table_one = (PageTableEntry*)0x7000;

    initialize_paging();

    kprintf("MM initialized.\n");
}

MemoryManager::~MemoryManager()
{
}

void MemoryManager::populate_page_directory(PageDirectory& page_directory)
{
    page_directory.m_directory_page = allocate_supervisor_physical_page();
    page_directory.entries()[0].copy_from({}, kernel_page_directory().entries()[0]);
    page_directory.entries()[1].copy_from({}, kernel_page_directory().entries()[1]);
    // Defer to the kernel page tables for 0xC0000000-0xFFFFFFFF
    for (int i = 768; i < 1024; ++i)
        page_directory.entries()[i].copy_from({}, kernel_page_directory().entries()[i]);
}

void MemoryManager::initialize_paging()
{
    memset(m_page_table_zero, 0, PAGE_SIZE);
    memset(m_page_table_one, 0, PAGE_SIZE);

#ifdef MM_DEBUG
    dbgprintf("MM: Kernel page directory @ %p\n", kernel_page_directory().cr3());
#endif

#ifdef MM_DEBUG
    dbgprintf("MM: Protect against null dereferences\n");
#endif
    // Make null dereferences crash.
    map_protected(VirtualAddress(0), PAGE_SIZE);

#ifdef MM_DEBUG
    dbgprintf("MM: Identity map bottom 5MB\n");
#endif
    // The bottom 5 MB (except for the null page) are identity mapped & supervisor only.
    // Every process shares these mappings.
    create_identity_mapping(kernel_page_directory(), VirtualAddress(PAGE_SIZE), (5 * MB) - PAGE_SIZE);

    // Basic memory map:
    // 0      -> 512 kB         Kernel code. Root page directory & PDE 0.
    // (last page before 1MB)   Used by quickmap_page().
    // 1 MB   -> 3 MB           kmalloc_eternal() space.
    // 3 MB   -> 4 MB           kmalloc() space.
    // 4 MB   -> 5 MB           Supervisor physical pages (available for allocation!)
    // 5 MB   -> 0xc0000000     Userspace physical pages (available for allocation!)
    // 0xc0000000-0xffffffff    Kernel-only virtual address space

#ifdef MM_DEBUG
    dbgprintf("MM: Quickmap will use %p\n", m_quickmap_addr.get());
#endif
    m_quickmap_addr = VirtualAddress((1 * MB) - PAGE_SIZE);

    RefPtr<PhysicalRegion> region;
    bool region_is_super = false;

    for (auto* mmap = (multiboot_memory_map_t*)multiboot_info_ptr->mmap_addr; (unsigned long)mmap < multiboot_info_ptr->mmap_addr + multiboot_info_ptr->mmap_length; mmap = (multiboot_memory_map_t*)((unsigned long)mmap + mmap->size + sizeof(mmap->size))) {
        kprintf("MM: Multiboot mmap: base_addr = 0x%x%08x, length = 0x%x%08x, type = 0x%x\n",
            (u32)(mmap->addr >> 32),
            (u32)(mmap->addr & 0xffffffff),
            (u32)(mmap->len >> 32),
            (u32)(mmap->len & 0xffffffff),
            (u32)mmap->type);

        if (mmap->type != MULTIBOOT_MEMORY_AVAILABLE)
            continue;

        // FIXME: Maybe make use of stuff below the 1MB mark?
        if (mmap->addr < (1 * MB))
            continue;

        if ((mmap->addr + mmap->len) > 0xffffffff)
            continue;

#ifdef MM_DEBUG
        kprintf("MM: considering memory at %p - %p\n",
            (u32)mmap->addr, (u32)(mmap->addr + mmap->len));
#endif

        for (size_t page_base = mmap->addr; page_base < (mmap->addr + mmap->len); page_base += PAGE_SIZE) {
            auto addr = PhysicalAddress(page_base);

            if (page_base < 4 * MB) {
                // nothing
            } else if (page_base >= 4 * MB && page_base < 5 * MB) {
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
                    region = &m_user_physical_regions.last();
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

#ifdef MM_DEBUG
    dbgprintf("MM: Installing page directory\n");
#endif

    asm volatile("movl %%eax, %%cr3" ::"a"(kernel_page_directory().cr3()));
    asm volatile(
        "movl %%cr0, %%eax\n"
        "orl $0x80000001, %%eax\n"
        "movl %%eax, %%cr0\n" ::
            : "%eax", "memory");

#ifdef MM_DEBUG
    dbgprintf("MM: Paging initialized.\n");
#endif
}

PageTableEntry& MemoryManager::ensure_pte(PageDirectory& page_directory, VirtualAddress vaddr)
{
    ASSERT_INTERRUPTS_DISABLED();
    u32 page_directory_index = (vaddr.get() >> 22) & 0x3ff;
    u32 page_table_index = (vaddr.get() >> 12) & 0x3ff;

    PageDirectoryEntry& pde = page_directory.entries()[page_directory_index];
    if (!pde.is_present()) {
#ifdef MM_DEBUG
        dbgprintf("MM: PDE %u not present (requested for V%p), allocating\n", page_directory_index, vaddr.get());
#endif
        if (page_directory_index == 0) {
            ASSERT(&page_directory == m_kernel_page_directory);
            pde.set_page_table_base((u32)m_page_table_zero);
            pde.set_user_allowed(false);
            pde.set_present(true);
            pde.set_writable(true);
        } else if (page_directory_index == 1) {
            ASSERT(&page_directory == m_kernel_page_directory);
            pde.set_page_table_base((u32)m_page_table_one);
            pde.set_user_allowed(false);
            pde.set_present(true);
            pde.set_writable(true);
        } else {
            //ASSERT(&page_directory != m_kernel_page_directory.ptr());
            auto page_table = allocate_supervisor_physical_page();
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
            page_directory.m_physical_pages.set(page_directory_index, move(page_table));
        }
    }
    return pde.page_table_base()[page_table_index];
}

void MemoryManager::map_protected(VirtualAddress vaddr, size_t length)
{
    InterruptDisabler disabler;
    ASSERT(vaddr.is_page_aligned());
    for (u32 offset = 0; offset < length; offset += PAGE_SIZE) {
        auto pte_address = vaddr.offset(offset);
        auto& pte = ensure_pte(kernel_page_directory(), pte_address);
        pte.set_physical_page_base(pte_address.get());
        pte.set_user_allowed(false);
        pte.set_present(false);
        pte.set_writable(false);
        flush_tlb(pte_address);
    }
}

void MemoryManager::create_identity_mapping(PageDirectory& page_directory, VirtualAddress vaddr, size_t size)
{
    InterruptDisabler disabler;
    ASSERT((vaddr.get() & ~PAGE_MASK) == 0);
    for (u32 offset = 0; offset < size; offset += PAGE_SIZE) {
        auto pte_address = vaddr.offset(offset);
        auto& pte = ensure_pte(page_directory, pte_address);
        pte.set_physical_page_base(pte_address.get());
        pte.set_user_allowed(false);
        pte.set_present(true);
        pte.set_writable(true);
        page_directory.flush(pte_address);
    }
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
    ASSERT_INTERRUPTS_DISABLED();
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

bool MemoryManager::zero_page(Region& region, unsigned page_index_in_region)
{
    ASSERT_INTERRUPTS_DISABLED();
    auto& vmo = region.vmobject();
    auto& vmo_page = vmo.physical_pages()[region.first_page_index() + page_index_in_region];
    sti();
    LOCKER(vmo.m_paging_lock);
    cli();
    if (!vmo_page.is_null()) {
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("MM: zero_page() but page already present. Fine with me!\n");
#endif
        remap_region_page(region, page_index_in_region);
        return true;
    }
    auto physical_page = allocate_user_physical_page(ShouldZeroFill::Yes);
#ifdef PAGE_FAULT_DEBUG
    dbgprintf("      >> ZERO P%p\n", physical_page->paddr().get());
#endif
    region.set_should_cow(page_index_in_region, false);
    vmo.physical_pages()[page_index_in_region] = move(physical_page);
    remap_region_page(region, page_index_in_region);
    return true;
}

bool MemoryManager::copy_on_write(Region& region, unsigned page_index_in_region)
{
    ASSERT_INTERRUPTS_DISABLED();
    auto& vmo = region.vmobject();
    if (vmo.physical_pages()[page_index_in_region]->ref_count() == 1) {
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("    >> It's a COW page but nobody is sharing it anymore. Remap r/w\n");
#endif
        region.set_should_cow(page_index_in_region, false);
        remap_region_page(region, page_index_in_region);
        return true;
    }

#ifdef PAGE_FAULT_DEBUG
    dbgprintf("    >> It's a COW page and it's time to COW!\n");
#endif
    auto physical_page_to_copy = move(vmo.physical_pages()[page_index_in_region]);
    auto physical_page = allocate_user_physical_page(ShouldZeroFill::No);
    u8* dest_ptr = quickmap_page(*physical_page);
    const u8* src_ptr = region.vaddr().offset(page_index_in_region * PAGE_SIZE).as_ptr();
#ifdef PAGE_FAULT_DEBUG
    dbgprintf("      >> COW P%p <- P%p\n", physical_page->paddr().get(), physical_page_to_copy->paddr().get());
#endif
    memcpy(dest_ptr, src_ptr, PAGE_SIZE);
    vmo.physical_pages()[page_index_in_region] = move(physical_page);
    unquickmap_page();
    region.set_should_cow(page_index_in_region, false);
    remap_region_page(region, page_index_in_region);
    return true;
}

bool MemoryManager::page_in_from_inode(Region& region, unsigned page_index_in_region)
{
    ASSERT(region.page_directory());
    auto& vmo = region.vmobject();
    ASSERT(vmo.is_inode());

    auto& inode_vmobject = static_cast<InodeVMObject&>(vmo);

    auto& vmo_page = inode_vmobject.physical_pages()[region.first_page_index() + page_index_in_region];

    InterruptFlagSaver saver;

    sti();
    LOCKER(vmo.m_paging_lock);
    cli();

    if (!vmo_page.is_null()) {
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("MM: page_in_from_inode() but page already present. Fine with me!\n");
#endif
        remap_region_page(region, page_index_in_region);
        return true;
    }

#ifdef MM_DEBUG
    dbgprintf("MM: page_in_from_inode ready to read from inode\n");
#endif
    sti();
    u8 page_buffer[PAGE_SIZE];
    auto& inode = inode_vmobject.inode();
    auto nread = inode.read_bytes((region.first_page_index() + page_index_in_region) * PAGE_SIZE, PAGE_SIZE, page_buffer, nullptr);
    if (nread < 0) {
        kprintf("MM: page_in_from_inode had error (%d) while reading!\n", nread);
        return false;
    }
    if (nread < PAGE_SIZE) {
        // If we read less than a page, zero out the rest to avoid leaking uninitialized data.
        memset(page_buffer + nread, 0, PAGE_SIZE - nread);
    }
    cli();
    vmo_page = allocate_user_physical_page(ShouldZeroFill::No);
    if (vmo_page.is_null()) {
        kprintf("MM: page_in_from_inode was unable to allocate a physical page\n");
        return false;
    }
    remap_region_page(region, page_index_in_region);
    u8* dest_ptr = region.vaddr().offset(page_index_in_region * PAGE_SIZE).as_ptr();
    memcpy(dest_ptr, page_buffer, PAGE_SIZE);
    return true;
}

Region* MemoryManager::region_from_vaddr(VirtualAddress vaddr)
{
    if (auto* region = kernel_region_from_vaddr(vaddr))
        return region;
    auto page_directory = PageDirectory::find_by_pdb(cpu_cr3());
    if (!page_directory)
        return nullptr;
    ASSERT(page_directory->process());
    return user_region_from_vaddr(*page_directory->process(), vaddr);
}

PageFaultResponse MemoryManager::handle_page_fault(const PageFault& fault)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(current);
#ifdef PAGE_FAULT_DEBUG
    dbgprintf("MM: handle_page_fault(%w) at V%p\n", fault.code(), fault.vaddr().get());
#endif
    ASSERT(fault.vaddr() != m_quickmap_addr);
    if (fault.type() == PageFault::Type::PageNotPresent && fault.vaddr().get() >= 0xc0000000) {
        auto* current_page_directory = reinterpret_cast<PageDirectoryEntry*>(cpu_cr3());
        u32 page_directory_index = (fault.vaddr().get() >> 22) & 0x3ff;
        auto& kernel_pde = kernel_page_directory().entries()[page_directory_index];
        auto& current_pde = current_page_directory[page_directory_index];

        if (kernel_pde.is_present() && !current_pde.is_present()) {
            dbg() << "NP(kernel): Copying new kernel mapping for " << fault.vaddr() << " into current page directory";
            current_pde.copy_from({}, kernel_pde);
            flush_tlb(fault.vaddr().page_base());
            return PageFaultResponse::Continue;
        }
    }
    auto* region = region_from_vaddr(fault.vaddr());
    if (!region) {
        kprintf("NP(error) fault at invalid address V%p\n", fault.vaddr().get());
        return PageFaultResponse::ShouldCrash;
    }
    auto page_index_in_region = region->page_index_from_address(fault.vaddr());
    if (fault.type() == PageFault::Type::PageNotPresent) {
        if (region->vmobject().is_inode()) {
#ifdef PAGE_FAULT_DEBUG
            dbgprintf("NP(inode) fault in Region{%p}[%u]\n", region, page_index_in_region);
#endif
            page_in_from_inode(*region, page_index_in_region);
            return PageFaultResponse::Continue;
        }
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("NP(zero) fault in Region{%p}[%u]\n", region, page_index_in_region);
#endif
        zero_page(*region, page_index_in_region);
        return PageFaultResponse::Continue;
    }
    ASSERT(fault.type() == PageFault::Type::ProtectionViolation);
    if (fault.access() == PageFault::Access::Write && region->should_cow(page_index_in_region)) {
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("PV(cow) fault in Region{%p}[%u]\n", region, page_index_in_region);
#endif
        bool success = copy_on_write(*region, page_index_in_region);
        ASSERT(success);
        return PageFaultResponse::Continue;
    }
    kprintf("PV(error) fault in Region{%p}[%u] at V%p\n", region, page_index_in_region, fault.vaddr().get());
    return PageFaultResponse::ShouldCrash;
}

RefPtr<Region> MemoryManager::allocate_kernel_region(size_t size, const StringView& name, bool user_accessible, bool should_commit)
{
    InterruptDisabler disabler;
    ASSERT(!(size % PAGE_SIZE));
    auto range = kernel_page_directory().range_allocator().allocate_anywhere(size);
    ASSERT(range.is_valid());
    RefPtr<Region> region;
    if (user_accessible)
        region = Region::create_user_accessible(range, name, PROT_READ | PROT_WRITE | PROT_EXEC, false);
    else
        region = Region::create_kernel_only(range, name, PROT_READ | PROT_WRITE | PROT_EXEC, false);
    MM.map_region_at_address(*m_kernel_page_directory, *region, range.base());
    // FIXME: It would be cool if these could zero-fill on demand instead.
    if (should_commit)
        region->commit();
    return region;
}

RefPtr<Region> MemoryManager::allocate_user_accessible_kernel_region(size_t size, const StringView& name)
{
    return allocate_kernel_region(size, name, true);
}

void MemoryManager::deallocate_user_physical_page(PhysicalPage&& page)
{
    for (auto& region : m_user_physical_regions) {
        if (!region.contains(page)) {
            kprintf(
                "MM: deallocate_user_physical_page: %p not in %p -> %p\n",
                page.paddr(), region.lower().get(), region.upper().get());
            continue;
        }

        region.return_page(move(page));
        --m_user_physical_pages_used;

        return;
    }

    kprintf("MM: deallocate_user_physical_page couldn't figure out region for user page @ %p\n", page.paddr());
    ASSERT_NOT_REACHED();
}

RefPtr<PhysicalPage> MemoryManager::allocate_user_physical_page(ShouldZeroFill should_zero_fill)
{
    InterruptDisabler disabler;
    RefPtr<PhysicalPage> page;

    for (auto& region : m_user_physical_regions) {
        page = region.take_free_page(false);
        if (page.is_null())
            continue;
    }

    if (!page) {
        if (m_user_physical_regions.is_empty()) {
            kprintf("MM: no user physical regions available (?)\n");
        }

        kprintf("MM: no user physical pages available\n");
        ASSERT_NOT_REACHED();
        return {};
    }

#ifdef MM_DEBUG
    dbgprintf("MM: allocate_user_physical_page vending P%p\n", page->paddr().get());
#endif

    if (should_zero_fill == ShouldZeroFill::Yes) {
        auto* ptr = (u32*)quickmap_page(*page);
        fast_u32_fill(ptr, 0, PAGE_SIZE / sizeof(u32));
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
                page.paddr(), region.lower().get(), region.upper().get());
            continue;
        }

        region.return_page(move(page));
        --m_super_physical_pages_used;
        return;
    }

    kprintf("MM: deallocate_supervisor_physical_page couldn't figure out region for super page @ %p\n", page.paddr());
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

    fast_u32_fill((u32*)page->paddr().as_ptr(), 0, PAGE_SIZE / sizeof(u32));
    ++m_super_physical_pages_used;
    return page;
}

void MemoryManager::enter_process_paging_scope(Process& process)
{
    ASSERT(current);
    InterruptDisabler disabler;
    current->tss().cr3 = process.page_directory().cr3();
    asm volatile("movl %%eax, %%cr3" ::"a"(process.page_directory().cr3())
                 : "memory");
}

void MemoryManager::flush_entire_tlb()
{
    asm volatile(
        "mov %%cr3, %%eax\n"
        "mov %%eax, %%cr3\n" ::
            : "%eax", "memory");
}

void MemoryManager::flush_tlb(VirtualAddress vaddr)
{
    asm volatile("invlpg %0"
                 :
                 : "m"(*(char*)vaddr.get())
                 : "memory");
}

void MemoryManager::map_for_kernel(VirtualAddress vaddr, PhysicalAddress paddr)
{
    auto& pte = ensure_pte(kernel_page_directory(), vaddr);
    pte.set_physical_page_base(paddr.get());
    pte.set_present(true);
    pte.set_writable(true);
    pte.set_user_allowed(false);
    flush_tlb(vaddr);
}

u8* MemoryManager::quickmap_page(PhysicalPage& physical_page)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(!m_quickmap_in_use);
    m_quickmap_in_use = true;
    auto page_vaddr = m_quickmap_addr;
    auto& pte = ensure_pte(kernel_page_directory(), page_vaddr);
    pte.set_physical_page_base(physical_page.paddr().get());
    pte.set_present(true);
    pte.set_writable(true);
    pte.set_user_allowed(false);
    flush_tlb(page_vaddr);
    ASSERT((u32)pte.physical_page_base() == physical_page.paddr().get());
#ifdef MM_DEBUG
    dbg() << "MM: >> quickmap_page " << page_vaddr << " => " << physical_page.paddr() << " @ PTE=" << (void*)pte.raw() << " {" << &pte << "}";
#endif
    return page_vaddr.as_ptr();
}

void MemoryManager::unquickmap_page()
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(m_quickmap_in_use);
    auto page_vaddr = m_quickmap_addr;
    auto& pte = ensure_pte(kernel_page_directory(), page_vaddr);
#ifdef MM_DEBUG
    auto old_physical_address = pte.physical_page_base();
#endif
    pte.set_physical_page_base(0);
    pte.set_present(false);
    pte.set_writable(false);
    flush_tlb(page_vaddr);
#ifdef MM_DEBUG
    dbg() << "MM: >> unquickmap_page " << page_vaddr << " =/> " << old_physical_address;
#endif
    m_quickmap_in_use = false;
}

void MemoryManager::remap_region_page(Region& region, unsigned page_index_in_region)
{
    ASSERT(region.page_directory());
    InterruptDisabler disabler;
    auto page_vaddr = region.vaddr().offset(page_index_in_region * PAGE_SIZE);
    auto& pte = ensure_pte(*region.page_directory(), page_vaddr);
    auto& physical_page = region.vmobject().physical_pages()[page_index_in_region];
    ASSERT(physical_page);
    pte.set_physical_page_base(physical_page->paddr().get());
    pte.set_present(true); // FIXME: Maybe we should use the is_readable flag here?
    if (region.should_cow(page_index_in_region))
        pte.set_writable(false);
    else
        pte.set_writable(region.is_writable());
    pte.set_user_allowed(region.is_user_accessible());
    region.page_directory()->flush(page_vaddr);
#ifdef MM_DEBUG
    dbg() << "MM: >> remap_region_page (PD=" << region.page_directory()->cr3() << ", PTE=" << (void*)pte.raw() << "{" << &pte << "}) " << region.name() << " " << page_vaddr << " => " << physical_page->paddr() << " (@" << physical_page.ptr() << ")";
#endif
}

void MemoryManager::remap_region(PageDirectory& page_directory, Region& region)
{
    InterruptDisabler disabler;
    ASSERT(region.page_directory() == &page_directory);
    map_region_at_address(page_directory, region, region.vaddr());
}

void MemoryManager::map_region_at_address(PageDirectory& page_directory, Region& region, VirtualAddress vaddr)
{
    InterruptDisabler disabler;
    region.set_page_directory(page_directory);
    auto& vmo = region.vmobject();
#ifdef MM_DEBUG
    dbgprintf("MM: map_region_at_address will map VMO pages %u - %u (VMO page count: %u)\n", region.first_page_index(), region.last_page_index(), vmo.page_count());
#endif
    for (size_t i = 0; i < region.page_count(); ++i) {
        auto page_vaddr = vaddr.offset(i * PAGE_SIZE);
        auto& pte = ensure_pte(page_directory, page_vaddr);
        auto& physical_page = vmo.physical_pages()[region.first_page_index() + i];
        if (physical_page) {
            pte.set_physical_page_base(physical_page->paddr().get());
            pte.set_present(true); // FIXME: Maybe we should use the is_readable flag here?
            // FIXME: It seems wrong that the *region* cow map is essentially using *VMO* relative indices.
            if (region.should_cow(region.first_page_index() + i))
                pte.set_writable(false);
            else
                pte.set_writable(region.is_writable());
        } else {
            pte.set_physical_page_base(0);
            pte.set_present(false);
            pte.set_writable(region.is_writable());
        }
        pte.set_user_allowed(region.is_user_accessible());
        page_directory.flush(page_vaddr);
#ifdef MM_DEBUG
        dbgprintf("MM: >> map_region_at_address (PD=%p) '%s' V%p => P%p (@%p)\n", &page_directory, region.name().characters(), page_vaddr, physical_page ? physical_page->paddr().get() : 0, physical_page.ptr());
#endif
    }
}

bool MemoryManager::unmap_region(Region& region)
{
    ASSERT(region.page_directory());
    InterruptDisabler disabler;
    for (size_t i = 0; i < region.page_count(); ++i) {
        auto vaddr = region.vaddr().offset(i * PAGE_SIZE);
        auto& pte = ensure_pte(*region.page_directory(), vaddr);
        pte.set_physical_page_base(0);
        pte.set_present(false);
        pte.set_writable(false);
        pte.set_user_allowed(false);
        region.page_directory()->flush(vaddr);
#ifdef MM_DEBUG
        auto& physical_page = region.vmobject().physical_pages()[region.first_page_index() + i];
        dbgprintf("MM: >> Unmapped V%p => P%p <<\n", vaddr, physical_page ? physical_page->paddr().get() : 0);
#endif
    }
    region.page_directory()->range_allocator().deallocate({ region.vaddr(), region.size() });
    region.release_page_directory();
    return true;
}

bool MemoryManager::map_region(Process& process, Region& region)
{
    map_region_at_address(process.page_directory(), region, region.vaddr());
    return true;
}

bool MemoryManager::validate_user_read(const Process& process, VirtualAddress vaddr) const
{
    auto* region = region_from_vaddr(process, vaddr);
    return region && region->is_readable();
}

bool MemoryManager::validate_user_write(const Process& process, VirtualAddress vaddr) const
{
    auto* region = region_from_vaddr(process, vaddr);
    return region && region->is_writable();
}

void MemoryManager::register_vmo(VMObject& vmo)
{
    InterruptDisabler disabler;
    m_vmobjects.append(&vmo);
}

void MemoryManager::unregister_vmo(VMObject& vmo)
{
    InterruptDisabler disabler;
    m_vmobjects.remove(&vmo);
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

ProcessPagingScope::ProcessPagingScope(Process& process)
{
    ASSERT(current);
    MM.enter_process_paging_scope(process);
}

ProcessPagingScope::~ProcessPagingScope()
{
    MM.enter_process_paging_scope(current->process());
}
