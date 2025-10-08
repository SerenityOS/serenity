/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/Registers.h>
#include <Kernel/Sections.h>

#include <Kernel/EFIPrekernel/Arch/Boot.h>
#include <Kernel/EFIPrekernel/Arch/MMU.h>
#include <Kernel/EFIPrekernel/Panic.h>
#include <Kernel/EFIPrekernel/Runtime.h>
#include <Kernel/EFIPrekernel/VirtualMemoryLayout.h>

namespace Kernel {

// This function has to fit into one page as it will be identity mapped.
[[gnu::aligned(PAGE_SIZE)]] [[noreturn]] NEVER_INLINE static void enter_kernel_helper(FlatPtr sctlr_el1, FlatPtr kernel_entry, FlatPtr kernel_sp, FlatPtr boot_info_vaddr) asm("enter_kernel_helper");
[[gnu::aligned(PAGE_SIZE)]] [[noreturn]] NEVER_INLINE static void enter_kernel_helper(FlatPtr sctlr_el1, FlatPtr kernel_entry, FlatPtr kernel_sp, FlatPtr boot_info_vaddr)
{
    register FlatPtr x0 asm("x0") = boot_info_vaddr;
    register FlatPtr sp asm("sp") = kernel_sp;
    asm volatile(R"(
        // Invalidate the TLBs before enabling the MMU, as the TLBs might still contain old values.
        tlbi vmalle1
        dsb ish
        isb

        msr sctlr_el1, %[sctlr_el1]
        isb

        mov lr, xzr
        mov fp, xzr

        br %[kernel_entry]
    )"
        :
        : "r"(x0), "r"(sp), [sctlr_el1] "r"(sctlr_el1), [kernel_sp] "r"(kernel_sp), [kernel_entry] "r"(kernel_entry)
        : "memory");

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
    if (boot_info.flattened_devicetree_paddr.is_null())
        PANIC("No devicetree configuration table was found. AArch64 systems without a devicetree UEFI configuration table are not supported.");

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
    // Current execution state (from https://uefi.org/specs/UEFI/2.11/02_Overview.html#aarch64-platforms):
    // * We are either in EL2 or EL1
    // * MMU is enabled and all RAM is identity mapped (other memory regions undefined)
    // * SCTLR_EL2/SCTLR_EL1 is set to:
    //     EE=0: Little endian
    //      I=1: Instruction cache on
    //     SA=1: Stack alignment check on
    //      C=1: Data cache on
    //      A=0: Alignment check off
    //      M=1: MMU on
    //   (other bits are undefined)
    // * TCR_EL2/TCR_EL1 is set to:
    //    TBI=0: Top byte ignore off
    //    (I)PS: Set to the valid (intermediate) physical address size
    //    TG0=0: 4K translation granule (aka page size)
    //   (other bits are undefined)
    // * Only TTBR0_EL2/TTBR0_EL1 must be used
    // * Interrupts are enabled
    // * CNTFRQ_EL0 is set to the correct timer frequency
    // * CNTHCTL_EL2.{EL1PCTEN,EL1PCEN} are set to 1 (physical timer is accessible in EL1 and EL0)

    // We should be in EL2 or EL1.
    auto current_el = Aarch64::Asm::get_current_exception_level();
    if (current_el == Aarch64::Asm::ExceptionLevel::EL3 || current_el == Aarch64::Asm::ExceptionLevel::EL0)
        halt();

    Aarch64::CPACR_EL1 cpacr_el1 = {};
    cpacr_el1.ZEN = 0;     // Trap SVE instructions at EL1 and EL0
    cpacr_el1.FPEN = 0b11; // Don't trap Advanced SIMD and floating-point instructions
    cpacr_el1.SMEN = 0;    // Trap SME instructions at EL1 and EL0
    cpacr_el1.TTA = 0;     // Don't trap access to trace registers
    Aarch64::CPACR_EL1::write(cpacr_el1);

    // Prepare some register values that will be used in the assembly code down below.

    // These register values will be used if we start in EL2:

    // Hypervisor Configuration
    // Stage 2 address translation is disabled, so intermediate physical address == physical address.
    Aarch64::HCR_EL2 hcr_el2 = {};
    hcr_el2.RW = 1; // EL1 to use AArch64

    // System Control Register for EL2
    auto sctlr_el2 = Aarch64::SCTLR_EL2::default_value();

    // Process state for entering EL1
    Aarch64::SPSR_EL2 spsr_el2 = {};

    // All interrupts masked
    spsr_el2.A = 1;
    spsr_el2.I = 1;
    spsr_el2.F = 1;

    spsr_el2.M = Aarch64::SPSR_EL2::Mode::EL1h; // Enter EL1

    // These register values are used to set up EL1:

    // System Control Register for EL1
    Aarch64::SCTLR_EL1 sctlr_el1 = Aarch64::SCTLR_EL1::default_value();

    // Memory attributes
    Aarch64::MAIR_EL1 mair_el1 = {};
    mair_el1.Attr[0] = 0xFF;       // Normal memory
    mair_el1.Attr[1] = 0b00000100; // Device-nGnRE memory (non-cacheable)
    mair_el1.Attr[2] = 0b01000100; // Normal Non-cacheable

    // Address translation configuration
    Aarch64::TCR_EL1 tcr_el1 = {};

    tcr_el1.SH1 = Aarch64::TCR_EL1::InnerShareable;
    tcr_el1.ORGN1 = Aarch64::TCR_EL1::NormalMemory_Outer_WriteBack_ReadAllocate_WriteAllocateCacheable;
    tcr_el1.IRGN1 = Aarch64::TCR_EL1::NormalMemory_Inner_WriteBack_ReadAllocate_WriteAllocateCacheable;
    tcr_el1.T1SZ = 16;

    tcr_el1.SH0 = Aarch64::TCR_EL1::InnerShareable;
    tcr_el1.ORGN0 = Aarch64::TCR_EL1::NormalMemory_Outer_WriteBack_ReadAllocate_WriteAllocateCacheable;
    tcr_el1.IRGN0 = Aarch64::TCR_EL1::NormalMemory_Inner_WriteBack_ReadAllocate_WriteAllocateCacheable;
    tcr_el1.T0SZ = 16;

    tcr_el1.TG1 = Aarch64::TCR_EL1::TG1GranuleSize::Size_4KB;
    tcr_el1.TG0 = Aarch64::TCR_EL1::TG0GranuleSize::Size_4KB;

    // Auto-detect the Intermediate Physical Address Size.
    Aarch64::ID_AA64MMFR0_EL1 feature_register = Aarch64::ID_AA64MMFR0_EL1::read();
    tcr_el1.IPS = feature_register.PARange;

    Aarch64::TCR_EL1::write(tcr_el1);

    // System Control Register value for EL1 with MMU enabled
    Aarch64::SCTLR_EL1 sctlr_el1_mmu_on = Aarch64::SCTLR_EL1::default_value();
    sctlr_el1_mmu_on.M = 1; // Enable MMU
    sctlr_el1_mmu_on.C = 1; // Enable data cache
    sctlr_el1_mmu_on.I = 1; // Enable instruction cache

    // The following code needs to disable the MMU while we load our MMU configuration.
    // All memory accesses are Device-nGnRnE (uncacheable) while the MMU is disabled,
    // which means that the following code isn't allowed to access any memory that was previously written to,
    // as the firmware likely mapped it as cacheable memory.
    // Memory accesses with incompatible attributes can result in unexpected behavior.

    // Therefore, the following code is written in assembly to ensure that it doesn't access any memory (including the stack!).

    asm volatile(R"(
        cmp %w[current_el], #2
        b.ne 1f

        // We are in EL2, so we need to set up EL2 and enter EL1.

        // Intialize SCTLR_EL2 with our defaults.
        // This also disables the MMU for EL2.
        msr sctlr_el2, %[sctlr_el2]
        isb

        // Set HCR_EL2 to a known value.
        msr hcr_el2, %[hcr_el2]

        // Intialize SCTLR_EL1 with our defaults.
        msr sctlr_el1, %[sctlr_el1]

        // Enter EL1.

        // Configure SPSR_EL2 to enter EL1.
        msr spsr_el2, %[spsr_el2]
        // Copy the current stack pointer.
        mov x0, sp
        msr sp_el1, x0
        // Enter at label 1.
        adr x0, 1f
        msr elr_el2, x0

        eret

    1:
        // We are in EL1, so we need to set up EL1.

        // Initialize SCTLR_EL1 with our defaults.
        // This also ensures the MMU is disabled for EL1 while we load our MMU settings.
        msr sctlr_el1, %[sctlr_el1]
        isb

        // Set up paging.

        // Set TTBR*_EL1 to the root page table.
        msr ttbr0_el1, %[root_page_table]
        msr ttbr1_el1, %[root_page_table]

        // Set MAIR_EL1 to our memory attributes.
        msr mair_el1, %[mair_el1]

        // Set TCR_EL1 to our address translation configuration.
        msr tcr_el1, %[tcr_el1]

        // Call the enter_kernel_helper function, which will enable the MMU and jump to the kernel entry point.
        mov x0, %[sctlr_el1_mmu_on]
        mov x1, %[kernel_entry_vaddr]
        mov x2, %[kernel_stack_pointer]
        mov x3, %[boot_info_vaddr]
        b enter_kernel_helper
    )" ::[current_el] "r"(current_el),
        [sctlr_el2] "r"(sctlr_el2), [hcr_el2] "r"(hcr_el2), [spsr_el2] "r"(spsr_el2),
        [sctlr_el1] "r"(sctlr_el1), [root_page_table] "r"(root_page_table), [mair_el1] "r"(mair_el1), [tcr_el1] "r"(tcr_el1),
        [sctlr_el1_mmu_on] "r"(sctlr_el1_mmu_on), [kernel_entry_vaddr] "r"(kernel_entry_vaddr), [kernel_stack_pointer] "r"(kernel_stack_pointer), [boot_info_vaddr] "r"(boot_info_vaddr)
        : "memory", "cc", "x0", "x1", "x2", "x3");

    __builtin_unreachable();
}

}
