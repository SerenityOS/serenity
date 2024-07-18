/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/Arch/riscv64/CSR.h>
#include <Kernel/Arch/riscv64/VirtualMemoryDefinitions.h>
#include <Kernel/Firmware/EFI/Protocols/RISCVBootProtocol.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Sections.h>

#include <Kernel/EFIPrekernel/Arch/Boot.h>
#include <Kernel/EFIPrekernel/Arch/MMU.h>
#include <Kernel/EFIPrekernel/DebugOutput.h>
#include <Kernel/EFIPrekernel/EFIPrekernel.h>
#include <Kernel/EFIPrekernel/Globals.h>
#include <Kernel/EFIPrekernel/Panic.h>
#include <Kernel/EFIPrekernel/VirtualMemoryLayout.h>

namespace Kernel {

// This function has to fit into one page as it will be identity mapped.
[[gnu::aligned(PAGE_SIZE)]] [[noreturn]] static void enter_kernel_helper(FlatPtr satp, FlatPtr kernel_entry, FlatPtr kernel_sp, FlatPtr boot_info_vaddr)
{
    // Switch the active root page table to argument 0.
    // This will immediately take effect, but we won't crash as this function is identity mapped.
    // Also set up a temporary trap handler to catch traps while switching page tables.
    register FlatPtr a0 asm("a0") = boot_info_vaddr;
    register FlatPtr sp asm("sp") = kernel_sp;
    asm volatile(R"(
        lla t0, 1f
        csrw stvec, t0

        csrw satp, %[satp]
        sfence.vma

        li ra, 0
        li fp, 0
        jr %[kernel_entry]

    .p2align 2
    1:
        csrw sie, zero
        wfi
        j 1b
    )"
                 :
                 : "r"(a0), "r"(sp), [satp] "r"(satp), [kernel_sp] "r"(kernel_sp), [kernel_entry] "r"(kernel_entry)
                 : "t0", "memory");

    __builtin_unreachable();
}

static FlatPtr get_boot_hart_id()
{
    auto riscv_boot_protocol_guid = EFI::RISCVBootProtocol::guid;
    EFI::RISCVBootProtocol* riscv_boot_protocol = nullptr;

    if (auto status = g_efi_system_table->boot_services->locate_protocol(&riscv_boot_protocol_guid, nullptr, reinterpret_cast<void**>(&riscv_boot_protocol)); status != EFI::Status::Success)
        PANIC("Failed to locate the RISC-V boot protocol: {}. RISC-V systems that don't support RISCV_EFI_BOOT_PROTOCOL are not supported.", status);

    FlatPtr boot_hart_id { 0 };
    if (auto status = riscv_boot_protocol->get_boot_hart_id(riscv_boot_protocol, &boot_hart_id); status != EFI::Status::Success)
        PANIC("Failed to get the RISC-V boot hart ID: {}", status);

    return boot_hart_id;
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
    if (boot_info.flattened_devicetree_paddr.is_null())
        PANIC("No devicetree configuration table was found. RISC-V systems without a devicetree UEFI configuration table are not supported.");

    boot_info.arch_specific.boot_hart_id = get_boot_hart_id();

    map_bootstrap_page(root_page_table, boot_info);
    set_up_quickmap_page_table(root_page_table, boot_info);

    auto maybe_kernel_page_directory = get_or_insert_page_table(root_page_table, boot_info.kernel_mapping_base, 1);
    if (maybe_kernel_page_directory.is_error())
        PANIC("Could not find the kernel page directory: {}", maybe_kernel_page_directory.release_error());

    // There is no level 4 table in Sv39.
    boot_info.boot_pml4t = PhysicalAddress { 0 };

    boot_info.boot_pdpt = PhysicalAddress { bit_cast<PhysicalPtr>(root_page_table) };
    boot_info.boot_pd_kernel = PhysicalAddress { bit_cast<PhysicalPtr>(maybe_kernel_page_directory.value()) };
}

[[noreturn]] void arch_enter_kernel(void* root_page_table, FlatPtr kernel_entry_vaddr, FlatPtr kernel_stack_pointer, FlatPtr boot_info_vaddr)
{
    RISCV64::CSR::SATP satp = {
        .PPN = bit_cast<u64>(root_page_table) >> PADDR_PPN_OFFSET,
        .ASID = 0,
        .MODE = RISCV64::CSR::SATP::Mode::Sv39,
    };

    enter_kernel_helper(bit_cast<FlatPtr>(satp), kernel_entry_vaddr, kernel_stack_pointer, boot_info_vaddr);
}

}
