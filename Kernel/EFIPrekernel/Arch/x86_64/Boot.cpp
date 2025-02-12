/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/Sections.h>

#include <Kernel/Arch/x86_64/MSR.h>
#include <Kernel/EFIPrekernel/Arch/Boot.h>
#include <Kernel/EFIPrekernel/Arch/MMU.h>
#include <Kernel/EFIPrekernel/Arch/x86_64/CPUID.h>
#include <Kernel/EFIPrekernel/DebugOutput.h>
#include <Kernel/EFIPrekernel/EFIPrekernel.h>
#include <Kernel/EFIPrekernel/Globals.h>
#include <Kernel/EFIPrekernel/Panic.h>
#include <Kernel/EFIPrekernel/VirtualMemoryLayout.h>

namespace Kernel {

// This function has to fit into one page as it will be identity mapped.
[[gnu::aligned(PAGE_SIZE)]] [[noreturn]] static void enter_kernel_helper(FlatPtr cr3, FlatPtr kernel_entry, FlatPtr kernel_sp, FlatPtr boot_info_vaddr)
{
    register FlatPtr rdi asm("rdi") = boot_info_vaddr;
    asm volatile(R"(
        /* Disable interrupts while modifying the GDT and loading the segment registers. The kernel also expects them to be disabled on entry. */
        cli

        /* Set up a (temporary) GDT for booting the kernel. */
        lgdt .Lgdt_pseudo_descriptor(%%rip)

        /* Set all segment registers except cs. */
        xor %%ax, %%ax
        mov %%ax, %%ss
        mov %%ax, %%ds
        mov %%ax, %%es
        mov %%ax, %%fs
        mov %%ax, %%gs

        /* Set cs to 8 by performing a far return, referencing the 2nd GDT entry. The kernel expects cs to be 8. */
        pushq $8
        leaq 1f(%%rip), %%rax
        pushq %%rax
        lretq
    1:

        movq %[cr3], %%cr3

        movq %[kernel_sp], %%rsp
        pushq $0
        jmp *%[kernel_entry]

    /* Put the GDT into the bootstrap page, so the kernel can keep using it until it loads its own GDT,
     * even if it causes a descriptor cache flush. */
    .balign 8
    .Lgdt:
        .8byte 0                                      /* null descriptor */
        .8byte (1<<43) | (1<<44) | (1<<47) | (1<<53)  /* executable, code segment, present, 64-bit */
    .Lgdt_end:

    /* The GDT pseudo-descriptor must be in a writable section, so relocations can be applied.
     * This means it won't be in the identity mapped bootstrap page, but that isn't a problem, as
     * it won't be accessed after being read by the lgdt instruction. */
    .section .data

    /* Ensure the base address is properly aligned. */
    .balign 8
        .4byte 0
        .2byte 0

    .Lgdt_pseudo_descriptor:
        .2byte .Lgdt_end - .Lgdt - 1  /* limit */
        .8byte .Lgdt                  /* base address */
    .previous
    )"
                 :
                 : "r"(rdi), [cr3] "r"(cr3), [kernel_sp] "r"(kernel_sp), [kernel_entry] "r"(kernel_entry)
                 : "rax", "memory");

    __builtin_unreachable();
}

static void map_bootstrap_page(void* root_page_table, BootInfo& boot_info)
{
    // FIXME: This leaks < (page table levels) pages, since all active allocations after ExitBootServices are currently eternal.
    //        We could theoretically reclaim them in the kernel.
    // NOTE: If this map_pages ever fails, the kernel vaddr range is inside our (physical) prekernel range.
    if (auto result = map_pages(root_page_table, bit_cast<FlatPtr>(&enter_kernel_helper), bit_cast<PhysicalPtr>(&enter_kernel_helper), 1, Access::Read | Access::Execute); result.is_error())
        PANIC("Failed to identity map the enter_kernel_helper function: {}", result.release_error());

    auto maybe_bootstrap_page_page_directory = get_or_insert_page_table(root_page_table, bit_cast<PhysicalPtr>(&enter_kernel_helper), 1);
    if (maybe_bootstrap_page_page_directory.is_error())
        PANIC("Could not find the bootstrap page page directory: {}", maybe_bootstrap_page_page_directory.release_error());

    boot_info.boot_method_specific.efi.bootstrap_page_vaddr = VirtualAddress { bit_cast<FlatPtr>(&enter_kernel_helper) };
    boot_info.boot_method_specific.efi.bootstrap_page_page_directory_paddr = PhysicalAddress { bit_cast<PhysicalPtr>(maybe_bootstrap_page_page_directory.value()) };
}

static void set_up_quickmap_page_table(void* root_page_table, BootInfo& boot_info)
{
    auto kernel_pt1024_base = boot_info.kernel_mapping_base + KERNEL_PT1024_OFFSET;

    auto maybe_quickmap_page_table_paddr = get_or_insert_page_table(root_page_table, kernel_pt1024_base, 0, true);
    if (maybe_quickmap_page_table_paddr.is_error())
        PANIC("Failed to insert the quickmap page table: {}", maybe_quickmap_page_table_paddr.release_error());

    boot_info.boot_pd_kernel_pt1023 = bit_cast<Memory::PageTableEntry*>(QUICKMAP_PAGE_TABLE_VADDR);

    if (auto result = map_pages(root_page_table, bit_cast<FlatPtr>(boot_info.boot_pd_kernel_pt1023), bit_cast<PhysicalPtr>(maybe_quickmap_page_table_paddr.value()), 1, Access::Read | Access::Write); result.is_error())
        PANIC("Failed to map the quickmap page table: {}", result.release_error());
}

void arch_prepare_boot(void* root_page_table, BootInfo& boot_info)
{
    map_bootstrap_page(root_page_table, boot_info);
    set_up_quickmap_page_table(root_page_table, boot_info);

    auto maybe_kernel_page_directory = get_or_insert_page_table(root_page_table, boot_info.kernel_mapping_base, 1);
    if (maybe_kernel_page_directory.is_error())
        PANIC("Could not find the kernel page directory: {}", maybe_kernel_page_directory.release_error());

    auto maybe_kernel_pdpt = get_or_insert_page_table(root_page_table, boot_info.kernel_mapping_base, 2);
    if (maybe_kernel_pdpt.is_error())
        PANIC("Could not find the kernel page directory pointer table: {}", maybe_kernel_pdpt.release_error());

    boot_info.boot_pml4t = PhysicalAddress { bit_cast<PhysicalPtr>(root_page_table) };
    boot_info.boot_pdpt = PhysicalAddress { bit_cast<PhysicalPtr>(maybe_kernel_pdpt.value()) };
    boot_info.boot_pd_kernel = PhysicalAddress { bit_cast<PhysicalPtr>(maybe_kernel_page_directory.value()) };
}

[[noreturn]] void arch_enter_kernel(void* root_page_table, FlatPtr kernel_entry_vaddr, FlatPtr kernel_stack_pointer, FlatPtr boot_info_vaddr)
{
    if (has_nx()) {
        // Turn on IA32_EFER.NXE.
        MSR ia32_efer(0xc0000080);
        ia32_efer.set(ia32_efer.get() | (1 << 11));
    }

    FlatPtr cr3 = bit_cast<FlatPtr>(root_page_table);

    enter_kernel_helper(cr3, kernel_entry_vaddr, kernel_stack_pointer, boot_info_vaddr);
}

}
