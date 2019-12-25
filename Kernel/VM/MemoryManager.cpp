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

MemoryManager::MemoryManager(u32 physical_address_for_kernel_page_tables)
{
    m_kernel_page_directory = PageDirectory::create_at_fixed_address(PhysicalAddress(physical_address_for_kernel_page_tables));
    for (size_t i = 0; i < 4; ++i) {
        m_low_page_tables[i] = (PageTableEntry*)(physical_address_for_kernel_page_tables + PAGE_SIZE * (5 + i));
        memset(m_low_page_tables[i], 0, PAGE_SIZE);
    }

    initialize_paging();

    kprintf("MM initialized.\n");
}

MemoryManager::~MemoryManager()
{
}

void MemoryManager::initialize_paging()
{
#ifdef MM_DEBUG
    dbgprintf("MM: Kernel page directory @ %p\n", kernel_page_directory().cr3());
#endif

#ifdef MM_DEBUG
    dbgprintf("MM: Protect against null dereferences\n");
#endif
    // Make null dereferences crash.
    map_protected(VirtualAddress(0), PAGE_SIZE);

#ifdef MM_DEBUG
    dbgprintf("MM: Identity map bottom 8MB\n");
#endif
    // The bottom 8 MB (except for the null page) are identity mapped & supervisor only.
    // Every process shares these mappings.
    create_identity_mapping(kernel_page_directory(), VirtualAddress(PAGE_SIZE), (8 * MB) - PAGE_SIZE);

    // FIXME: We should move everything kernel-related above the 0xc0000000 virtual mark.

    // Basic physical memory map:
    // 0      -> 1 MB           We're just leaving this alone for now.
    // 1      -> 3 MB           Kernel image.
    // (last page before 2MB)   Used by quickmap_page().
    // 2 MB   -> 4 MB           kmalloc_eternal() space.
    // 4 MB   -> 7 MB           kmalloc() space.
    // 7 MB   -> 8 MB           Supervisor physical pages (available for allocation!)
    // 8 MB   -> MAX            Userspace physical pages (available for allocation!)

    // Basic virtual memory map:
    // 0 -> 4 KB                Null page (so nullptr dereferences crash!)
    // 4 KB -> 8 MB             Identity mapped.
    // 8 MB -> 3 GB             Available to userspace.
    // 3GB  -> 4 GB             Kernel-only virtual address space (>0xc0000000)

#ifdef MM_DEBUG
    dbgprintf("MM: Quickmap will use %p\n", m_quickmap_addr.get());
#endif
    m_quickmap_addr = VirtualAddress((2 * MB) - PAGE_SIZE);

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

        auto diff = (u32)mmap->addr % PAGE_SIZE;
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
            (u32)mmap->addr, (u32)(mmap->addr + mmap->len));
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

#ifdef MM_DEBUG
    dbgprintf("MM: Installing page directory\n");
#endif

    // Turn on CR4.PGE so the CPU will respect the G bit in page tables.
    asm volatile(
        "mov %cr4, %eax\n"
        "orl $0x80, %eax\n"
        "mov %eax, %cr4\n");

    // Turn on CR4.PAE
    asm volatile(
        "mov %cr4, %eax\n"
        "orl $0x20, %eax\n"
        "mov %eax, %cr4\n");

    // Turn on IA32_EFER.NXE
    asm volatile(
        "movl $0xc0000080, %ecx\n"
        "rdmsr\n"
        "orl $0x800, %eax\n"
        "wrmsr\n");

    asm volatile("movl %%eax, %%cr3" ::"a"(kernel_page_directory().cr3()));
    asm volatile(
        "movl %%cr0, %%eax\n"
        "orl $0x80010001, %%eax\n"
        "movl %%eax, %%cr0\n" ::
            : "%eax", "memory");

#ifdef MM_DEBUG
    dbgprintf("MM: Paging initialized.\n");
#endif
}

PageTableEntry& MemoryManager::ensure_pte(PageDirectory& page_directory, VirtualAddress vaddr)
{
    ASSERT_INTERRUPTS_DISABLED();
    u32 page_directory_table_index = (vaddr.get() >> 30) & 0x3;
    u32 page_directory_index = (vaddr.get() >> 21) & 0x1ff;
    u32 page_table_index = (vaddr.get() >> 12) & 0x1ff;

    PageDirectoryEntry& pde = page_directory.table().directory(page_directory_table_index)[page_directory_index];
    if (!pde.is_present()) {
#ifdef MM_DEBUG
        dbgprintf("MM: PDE %u not present (requested for V%p), allocating\n", page_directory_index, vaddr.get());
#endif
        if (page_directory_table_index == 0 && page_directory_index < 4) {
            ASSERT(&page_directory == m_kernel_page_directory);
            pde.set_page_table_base((u32)m_low_page_tables[page_directory_index]);
            pde.set_user_allowed(false);
            pde.set_present(true);
            pde.set_writable(true);
            pde.set_global(true);
        } else {
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
            pde.set_global(&page_directory == m_kernel_page_directory.ptr());
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

void MemoryManager::initialize(u32 physical_address_for_kernel_page_tables)
{
    s_the = new MemoryManager(physical_address_for_kernel_page_tables);
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
    auto page_directory = PageDirectory::find_by_cr3(cpu_cr3());
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
    auto* region = region_from_vaddr(fault.vaddr());
    if (!region) {
        kprintf("NP(error) fault at invalid address V%p\n", fault.vaddr().get());
        return PageFaultResponse::ShouldCrash;
    }

    return region->handle_fault(fault);
}

OwnPtr<Region> MemoryManager::allocate_kernel_region(size_t size, const StringView& name, bool user_accessible, bool should_commit)
{
    InterruptDisabler disabler;
    ASSERT(!(size % PAGE_SIZE));
    auto range = kernel_page_directory().range_allocator().allocate_anywhere(size);
    ASSERT(range.is_valid());
    OwnPtr<Region> region;
    if (user_accessible)
        region = Region::create_user_accessible(range, name, PROT_READ | PROT_WRITE | PROT_EXEC);
    else
        region = Region::create_kernel_only(range, name, PROT_READ | PROT_WRITE | PROT_EXEC);
    region->map(kernel_page_directory());
    // FIXME: It would be cool if these could zero-fill on demand instead.
    if (should_commit)
        region->commit();
    return region;
}

OwnPtr<Region> MemoryManager::allocate_user_accessible_kernel_region(size_t size, const StringView& name)
{
    return allocate_kernel_region(size, name, true);
}

OwnPtr<Region> MemoryManager::allocate_kernel_region_with_vmobject(VMObject& vmobject, size_t size, const StringView& name)
{
    InterruptDisabler disabler;
    ASSERT(!(size % PAGE_SIZE));
    auto range = kernel_page_directory().range_allocator().allocate_anywhere(size);
    ASSERT(range.is_valid());
    auto region = make<Region>(range, vmobject, 0, name, PROT_READ | PROT_WRITE | PROT_EXEC);
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

void MemoryManager::map_for_kernel(VirtualAddress vaddr, PhysicalAddress paddr, bool cache_disabled)
{
    auto& pte = ensure_pte(kernel_page_directory(), vaddr);
    pte.set_physical_page_base(paddr.get());
    pte.set_present(true);
    pte.set_writable(true);
    pte.set_user_allowed(false);
    pte.set_cache_disabled(cache_disabled);
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

bool MemoryManager::validate_user_stack(const Process& process, VirtualAddress vaddr) const
{
    auto* region = region_from_vaddr(process, vaddr);
    return region && region->is_stack();
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

ProcessPagingScope::ProcessPagingScope(Process& process)
{
    ASSERT(current);
    MM.enter_process_paging_scope(process);
}

ProcessPagingScope::~ProcessPagingScope()
{
    MM.enter_process_paging_scope(current->process());
}
